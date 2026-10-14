#!/usr/bin/env python3
"""Explicit SDK/Gradle/archive checks; never discovered as default host tests."""
from hashlib import sha256
from pathlib import Path
import json
import os
import re
import time
from unittest.mock import patch

import native_entry_android_plugins as plugins
import native_entry_driver as driver
from native_entry_config import (EntryError, exact_keys, frozen_json, no_symlink, private_file,
    private_output, require, strict_json, write_private)

SCOPE = "android-plugin-registration-integration-r1"
INPUTS = ("flutterExecutable", "androidSdkRoot", "javaHome", "apkanalyzer", "signingConfigFile",
    "oldProfileBuildReceipt", "oldReleaseBuildReceipt")
DEV_PLUGIN = "dev.flutter.plugins.integration_test.IntegrationTestPlugin"


def load_inputs(config_path):
    raw = private_file(config_path, 16384)
    values = strict_json(raw, 16384)
    exact_keys(values, ("scope", "schemaVersion", *INPUTS, "privateOutputRoot"))
    require(values["scope"] == SCOPE and type(values["schemaVersion"]) is int and
        values["schemaVersion"] == 1, "integration_inputs_rejected")
    paths = {key: no_symlink(values[key]) for key in INPUTS}
    require(all(paths[key].is_dir() for key in ("androidSdkRoot", "javaHome")) and
        all(paths[key].is_file() for key in INPUTS if key not in ("androidSdkRoot", "javaHome")),
        "integration_inputs_rejected")
    require(os.access(paths["flutterExecutable"], os.X_OK) and os.access(paths["apkanalyzer"], os.X_OK) and
        (paths["javaHome"] / "bin/java").is_file(), "integration_inputs_rejected")
    private_file(paths["signingConfigFile"])
    for key, mode in (("oldProfileBuildReceipt", "profile"), ("oldReleaseBuildReceipt", "release")):
        receipt = strict_json(private_file(paths[key], 2 * 1024 * 1024), 2 * 1024 * 1024)
        require(receipt.get("platform") == "android" and receipt.get("mode") == mode and
            driver.artifact_hash(receipt["artifactPath"]) == receipt.get("artifactSha256"),
            "integration_archive_binding_rejected")
    output = private_output(values["privateOutputRoot"], [config_path, *paths.values()], create=True)
    os.environ["JAVA_HOME"] = str(paths["javaHome"])
    os.environ["ANDROID_HOME"] = os.environ["ANDROID_SDK_ROOT"] = str(paths["androidSdkRoot"])
    os.environ["PATH"] = os.pathsep.join((str(paths["flutterExecutable"].parent),
        str(paths["javaHome"] / "bin"), os.environ.get("PATH", "")))
    require(Path(driver.shutil.which("flutter")).resolve() == paths["flutterExecutable"], "integration_sdk_binding_rejected")
    return paths, output, sha256(raw).hexdigest()


def check_plugin_content(raw, mode):
    require(mode in ("profile", "release"), "integration_sdk_mode_mismatch")
    statements = set(re.findall(rb"flutterEngine\.getPlugins\(\)\.add\(new ([A-Za-z0-9_.$]+)\(\)\);", raw))
    require(all(name.encode() in statements for name in plugins.JNI_PLUGINS) and
        ((DEV_PLUGIN.encode() in statements) == (mode == "profile")), "integration_sdk_mode_mismatch")
    return {"jniPluginsPresent": True, "integrationTestPresent": mode == "profile",
        "generatedSourceSha256": sha256(raw).hexdigest()}


def sdk_modes(mobile, output, sources, deadline):
    require(b"integration_test:" in (mobile / "pubspec.yaml").read_bytes(), "integration_project_fixture_rejected")
    lock = sha256((mobile / "pubspec.lock").read_bytes()).hexdigest()
    driver.checked_run(["flutter", "pub", "get", "--offline"], output, "sdk-offline-packages", deadline=deadline, cwd=mobile)
    require(sha256((mobile / "pubspec.lock").read_bytes()).hexdigest() == lock, "mobile_lock_changed")
    results = {}
    for mode in ("profile", "release"):
        selected = output / ("sdk-" + mode)
        selected.mkdir(mode=0o700)
        provenance = plugins.generate_android_registrant(mobile, selected, mode, deadline, driver.checked_run)
        results[mode] = check_plugin_content(private_file(mobile / plugins.SOURCE, 65536, exact_mode=False), mode) | provenance
    mutant = plugins.SDK_GENERATOR.replace(b"releaseMode: args[2] == 'release'", b"releaseMode: false")
    require(mutant != plugins.SDK_GENERATOR, "integration_mutant_unavailable")
    selected = output / "sdk-release-mode-mutant"
    selected.mkdir(mode=0o700)
    with patch.object(plugins, "SDK_GENERATOR", mutant):
        provenance = plugins.generate_android_registrant(mobile, selected, "release", deadline, driver.checked_run)
    try:
        check_plugin_content(private_file(mobile / plugins.SOURCE, 65536, exact_mode=False), "release")
    except EntryError as error:
        require(str(error) == "integration_sdk_mode_mismatch", "integration_mutant_unverified")
    else:
        raise EntryError("integration_mutant_survived")
    results["releaseModeFalseMutant"] = {"contentCheck": "refused", **provenance}
    require(sha256((mobile / "pubspec.lock").read_bytes()).hexdigest() == lock and
        driver.validate_copied_sources(mobile, sources), "integration_source_changed")
    return results


def gradle_refusals(mobile, output, signer, deadline):
    generated = mobile / plugins.SOURCE
    results = []
    for index, content in enumerate((None, b"", b"incompatible cached source", b"\xff")):
        if content is None:
            generated.unlink(missing_ok=True)
        else:
            generated.write_bytes(content)
        destination = output / ("gradle-case-" + str(index))
        destination.mkdir(mode=0o700)
        stale = destination / "gradle/app" / plugins.COPIED
        init = output / ("stale-injection-" + str(index) + ".gradle")
        # Test fixture only: root exclusivity must pass before injecting owned stale output.
        write_private(init, ("gradle.projectsEvaluated { def app = rootProject.findProject(':app'); if (app != null) { app.tasks.named('prepareImageExperimentRegistrant').configure { doFirst { def stale = new File(" +
            json.dumps(str(stale)) + "); stale.parentFile.mkdirs(); stale.text = 'stale owned Java source' } } } }\n").encode())
        argv = [str(mobile / "android/gradlew"), "--no-daemon", "--init-script", str(init), "-PimageExperiment=true",
            "-PexperimentMode=profile", "-Ptarget=lib/main_image_experiment.dart",
            "-PimageExperimentSigningConfig=" + str(signer), "-PimageExperimentOutputRoot=" + str(destination), ":app:assembleProfile"]
        alias = "generation-refusal-" + str(index)
        remaining = deadline - time.monotonic()
        require(remaining > 0, "driver_expired")
        result = driver.run_bounded(argv, output, alias, seconds=min(45, remaining), cwd=mobile / "android")
        require(result["status"] == "failed" and b"EXPERIMENT_REGISTRANT_REFUSED" in
            private_file(output / (alias + ".stderr"), driver.MAX_TRACE_BYTES) and not stale.exists() and
            not (destination / "build-receipt.json").exists(), "integration_gradle_refusal_unverified")
        results.append({"case": ("missing", "empty", "incompatible", "invalid_utf8")[index],
            "status": "refused", "staleCopyRemoved": True,
            "inspectionReceiptSha256": sha256(private_file(output / (alias + ".json"))).hexdigest()})
    return results


def old_apks(paths, output, deadline):
    results = []
    for key, mode in (("oldProfileBuildReceipt", "profile"), ("oldReleaseBuildReceipt", "release")):
        receipt = strict_json(private_file(paths[key], 2 * 1024 * 1024), 2 * 1024 * 1024)
        apk = Path(receipt["artifactPath"])
        before = driver.artifact_hash(apk)
        require(before == receipt["artifactSha256"], "integration_archive_binding_rejected")
        alias = "old-" + mode + "-dex"
        remaining = deadline - time.monotonic()
        require(remaining > 0, "driver_expired")
        result = driver.run_bounded([str(paths["apkanalyzer"]), "dex", "code", "--class", plugins.REGISTRANT, str(apk)],
            output, alias, seconds=min(20, remaining))
        require(result["status"] == "failed" and private_file(output / (alias + ".stdout")) == b"" and
            private_file(output / (alias + ".stderr")).strip() ==
            b"ERROR: The given class (io.flutter.plugins.GeneratedPluginRegistrant) not found" and
            driver.artifact_hash(apk) == before, "integration_negative_apk_unverified")
        results.append({"mode": mode, "artifactSha256": before, "registrantDefinition": "absent",
            "admission": "refused", "inspectionReceiptSha256": sha256(private_file(output / (alias + ".json"))).hexdigest()})
    return results


def main(argv=None):
    parser = driver.SafeParser(description="Explicit private Android plugin integration checks")
    parser.add_argument("--config", required=True)
    args = parser.parse_args(argv)
    output = None
    os.umask(0o077)
    try:
        paths, output, config_hash = load_inputs(args.config)
        deadline = time.monotonic() + driver.TOTAL_SECONDS
        sources, source_hash = driver.source_snapshot()
        mobile = driver.private_mobile_snapshot(output, sources)
        driver.prepare_android_tool_settings(mobile)
        sdk = sdk_modes(mobile, output, sources, deadline)
        gradle = gradle_refusals(mobile, output, paths["signingConfigFile"], deadline)
        negatives = old_apks(paths, output, deadline)
        require(driver.source_snapshot()[1] == source_hash and
            driver.validate_copied_sources(mobile, sources), "integration_source_changed")
        write_private(output / "integration-receipt.json", {"scope": SCOPE, "status": "passed",
            "configSha256": config_hash, "sourceSha256": source_hash, "sdkModes": sdk,
            "gradleRefusals": gradle, "oldApks": negatives, "cloudSessionCreated": False,
            "physicalQualification": False, "acceptedBuildReceiptCreated": False})
        print(frozen_json({"scope": SCOPE, "status": "passed", "skipCount": 0}).decode())
        return 0
    except (EntryError, OSError, ValueError, KeyError, TypeError) as error:
        reason = str(error) if isinstance(error, EntryError) else "integration_operation_failed"
        if output is not None:
            write_private(output / "integration-failure.json", {"scope": SCOPE, "status": "refused", "reason": reason})
        print(frozen_json({"scope": SCOPE, "status": "refused", "reason": reason}).decode())
        return 4


if __name__ == "__main__":
    raise SystemExit(main())
