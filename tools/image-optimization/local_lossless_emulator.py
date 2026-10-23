#!/usr/bin/env python3
"""Altı sabit görsel için ayrı Android emülatör profile denemesini yönetir."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import stat
import sys
from zipfile import ZipFile

import local_lossless_mobile as base
from native_entry_config import EntryError, frozen_json, require, strict_json
from native_entry_driver import (MOBILE, ROOT, private_mobile_snapshot, read_source,
    selected_mobile_sources, source_snapshot, tool_environment)

SCOPE = "local-lossless-android-emulator-r1"
ENTRY = "test_driver/local_lossless_emulator_entry.dart"
DRIVER = "test_driver/local_lossless_driver.dart"
APP_ID = "com.tellpal.tellpal_mobile_poc.locallossless.emulator.profile"
ACTIVITY = "com.tellpal.tellpal_mobile_poc.MainActivity"
SELECTOR = "emulator-5554"
ABI = "arm64-v8a"
API = 36
MANIFEST = base.MANIFEST
PUBLIC = base.PUBLIC
REASONS = frozenset(("TOOLS_UNAVAILABLE", "OFFLINE_CACHE_MISSING", "BUILD_FAILED",
    "EMULATOR_UNAVAILABLE", "DRIVER_NOT_RUN", "DRIVER_FAILED", "VERIFICATION_REJECTED"))
INIT_NAME = "local-lossless-emulator.init.gradle"
JAR_ENGINE = "lib/arm64-v8a/libflutter.so"
MAX_APK = 512 * 1024 * 1024
PROFILE_ENGINE_MAX = 256 * 1024 * 1024
# Gradle, ADB ve VM yalnız yerel TCP bağlantısını kullanır.
LOCAL_NETWORK_POLICY = ("(version 1)(allow default)(deny network*)"
    '(allow network-inbound (local ip "localhost:*"))'
    '(allow network-outbound (remote ip "localhost:*"))'
    '(allow network-bind (local ip "localhost:*"))')


def stamp():
    return datetime.now(timezone.utc).isoformat()


def inside(path, root):
    path = Path(path)
    require(path.is_absolute() and path.resolve(strict=True) == path and
        path.is_relative_to(root), "private_binding_path_rejected")
    return path


def local_sources():
    sources, _ = source_snapshot()
    for name in ("local_lossless_mobile.py", "test_local_lossless_mobile.py",
        "local_lossless_emulator.py", "test_local_lossless_emulator.py"):
        relative = "tools/image-optimization/" + name
        sources[relative] = base.digest(read_source(ROOT / relative))
    relative = "_bmad-output/implementation-artifacts/spec-local-lossless-mobile.md"
    sources[relative] = base.digest(read_source(ROOT / relative))
    return sources, base.digest(frozen_json(sources))


def preserved_private_directory(path):
    """Eski private çıktı başka bir geçici kökte olabilir; yalnız okunur."""
    path = Path(path)
    require(path.is_absolute() and path.resolve(strict=True) == path and
        not path.is_relative_to(ROOT), "preserved_private_directory_rejected")
    info = path.lstat()
    require(stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid() and
        stat.S_IMODE(info.st_mode) == 0o700, "preserved_private_directory_rejected")
    return path


def protection(evidence, extra=()):
    """Yalnız bu görsel işine ait eski kanıt ve checkout çıktısını korur."""
    paths = {ROOT / name for name in evidence["preservation"]["afterSha256"]}
    paths.update((base.EVIDENCE, base.EVIDENCE.with_suffix(".md"), MOBILE / "pubspec.yaml", MOBILE / "pubspec.lock"))
    paths.update(ROOT / name for name in ("_bmad-output/party-mode/memories/installed/.memlog.md",
        "_bmad-output/specs/spec-publish-time-image-optimization/.memlog.md",
        "_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/.memlog.md"))
    directories = {ROOT / "be/docs/evidence/png-lossless-compression", ROOT / "be/docs/evidence/p4-reading-images",
        ROOT / "be/docs/evidence/local-lossless-formats", ROOT / evidence["privateRun"],
        ROOT / "_bmad-output/implementation-artifacts/image-optimization"}
    previous = evidence.get("measurementCorrection", {}).get("previousRun")
    if previous:
        directories.add(ROOT / previous)
    directories.update(path for path in (ROOT / "be/var").glob("png-lossless*") if path.is_dir())
    directories.update(path for path in (MOBILE / "build", MOBILE / "android/app/build") if path.exists())
    # Seçilen native kaynaklar guard/manifest/Gradle dosyalarını kapsar; canlı cache sabitlenmez.
    paths.update(path for path in selected_mobile_sources() if path.is_relative_to(MOBILE / "android") or
        path.is_relative_to(MOBILE / "ios"))
    generated = (MOBILE / "android/local.properties",
        MOBILE / "android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java")
    absent = [str(path) for path in (*generated, MOBILE / "build", MOBILE / "android/app/build") if not path.exists()]
    for path in generated:
        if path.exists():
            paths.add(path)
    # Signing yalnız eski dosya korumasıdır; yokluğu Android build girdisi değildir.
    if base.PREFLIGHT.exists():
        paths.add(base.PREFLIGHT)
        preflight = base.private_json(base.PREFLIGHT)
        require(base.file_hash(base.PREFLIGHT) == base.PREFLIGHT_SHA, "preflight_changed")
        signing = Path(preflight["signingConfig"]["sourcePath"])
        if signing.exists():
            require(signing.is_absolute() and signing.resolve(strict=True) == signing and
                not signing.is_relative_to(ROOT), "preserved_signing_rejected")
            paths.add(signing)
            values = [line.split("=", 1)[1].strip() for line in read_source(signing).decode().splitlines()
                if line.split("=", 1)[0].strip() == "IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT" and "=" in line]
            require(len(values) == 1, "preserved_signing_rejected")
            old_output = Path(values[0])
            if old_output.exists():
                directories.add(preserved_private_directory(old_output))
    for path in extra:
        directories.add(base.private_path(path, directory=True))
    files = {}
    for path in sorted(paths):
        require(path.is_absolute() and path.resolve(strict=True) == path, "preservation_scope_rejected")
        files[str(path)] = base.file_hash(path)
    trees = {str(path): base.tree_hashes(path) for path in sorted(directories)}
    return {"files": files, "trees": trees, "absent": absent,
        "publicTree": {"path": str(PUBLIC), "files": base.tree_hashes(PUBLIC)}}


def assert_protected(before, authorized_public=None):
    files = {name: base.file_hash(Path(name)) for name in before["files"]}
    trees = {name: base.tree_hashes(Path(name)) for name in before["trees"]}
    require(files == before["files"] and trees == before["trees"] and
        all(not Path(name).exists() and not Path(name).is_symlink() for name in before.get("absent", [])),
        "preservation_changed")
    public = before.get("publicTree")
    if public is not None:
        require(public["path"] == str(PUBLIC), "preservation_scope_rejected")
        actual = base.tree_hashes(PUBLIC)
        require(all(actual.get(name) == value for name, value in public["files"].items()), "preservation_changed")
        additions = set(actual) - set(public["files"])
        if authorized_public is None:
            require(not additions, "preservation_changed")
        else:
            target = Path(authorized_public)
            require(target.is_absolute() and target.resolve(strict=True) == target and
                target.parent == PUBLIC / "attempts" and
                re.fullmatch(r"[a-z0-9][a-z0-9-]{0,95}", target.name), "preservation_scope_rejected")
            prefix = str(target.relative_to(PUBLIC)) + "/"
            require(all(name.startswith(prefix) for name in additions) and
                not any(name.startswith(prefix) for name in public["files"]), "preservation_changed")
    return {"unchanged": True, "fileCount": len(files) + sum(len(row) for row in trees.values()) +
        (len(public["files"]) if public is not None else 0),
        "sha256": base.digest(frozen_json({"files": files, "trees": trees,
            "absent": before.get("absent", []), "publicTree": public}))}


def preservation_result(before):
    try:
        return assert_protected(before)
    except Exception:
        return {"unchanged": False, "reason": "VERIFICATION_REJECTED"}


def init_script(nonce):
    defines = base.dart_defines(nonce)
    return f'''// Yalnız bu private profile girişi için ayrı package oluşturur.
gradle.beforeProject {{ project ->
    project.pluginManager.withPlugin('com.android.application') {{
        if (project.path == ':app') {{
            if (project.findProperty('target') != '{ENTRY}' ||
                project.findProperty('target-platform') != 'android-arm64' ||
                project.findProperty('dart-defines') != '{defines}' ||
                ['imageExperiment', 'experimentMode', 'imageExperimentSigningConfig',
                 'imageExperimentOutputRoot'].any {{ project.findProperty(it) != null }} ||
                gradle.startParameter.taskNames != [':app:assembleProfile'] ||
                !gradle.startParameter.offline) {{
                throw new GradleException('LOCAL_LOSSLESS_EMULATOR_TARGET_REFUSED')
            }}
            project.extensions.getByName('androidComponents').finalizeDsl {{ android ->
                android.defaultConfig.applicationId = '{APP_ID}'
                android.defaultConfig.ndk.abiFilters.clear()
                android.defaultConfig.ndk.abiFilters.add('{ABI}')
                android.buildTypes.getByName('profile').ndk.abiFilters.clear()
                android.buildTypes.getByName('profile').ndk.abiFilters.add('{ABI}')
                android.packaging.jniLibs.keepDebugSymbols.add('**/libflutter.so')
            }}
        }}
    }}
}}
'''.encode()


def prepare(private_dir, preserve_private=()):
    private_dir = base.new_private_directory(private_dir)
    for old in preserve_private:
        old = base.private_path(old, directory=True)
        require(not private_dir.is_relative_to(old) and not old.is_relative_to(private_dir), "preservation_scope_rejected")
    evidence, cases = base.fixtures()
    sources, source_sha = local_sources()
    require("app/mobile/" + ENTRY in sources, "emulator_entry_missing")
    private_dir.mkdir(mode=0o700)
    try:
        protected = protection(evidence, preserve_private)
        base.write_new(private_dir / "preservation-before.private.json", protected)
        output = private_dir / "build-output"
        output.mkdir(mode=0o700)
        mobile = private_mobile_snapshot(output, sources)
        (mobile / "assets/local_lossless").mkdir(mode=0o700)
        for row in cases:
            data = read_source(Path(row["sourcePath"]))
            require(base.digest(data) == row["sha256"] and len(data) == row["bytes"], "fixture_changed_during_copy")
            base.write_new(mobile / row["asset"], data)
        manifest = {"schemaVersion": 1, "evidenceSha256": base.EVIDENCE_SHA,
            "cases": [{key: value for key, value in row.items() if key != "sourcePath"} for row in cases]}
        base.write_new(mobile / MANIFEST, manifest)
        pubspec = mobile / "pubspec.yaml"
        original = read_source(pubspec)
        text = original.decode()
        require(text.count("  assets:\n") == 1 and "assets/local_lossless" not in text, "private_pubspec_rejected")
        replacement = text.replace("  assets:\n", "  assets:\n    - assets/local_lossless/\n", 1).encode()
        pubspec.write_bytes(replacement)
        nonce = base.secrets.token_hex(32)
        base.write_new(private_dir / INIT_NAME, init_script(nonce))
        result = {"schemaVersion": 1, "scope": SCOPE, "mode": "profile", "appId": APP_ID,
            "dartTarget": ENTRY, "driver": DRIVER, "targetPlatform": "android-arm64", "abi": ABI,
            "deviceSelector": SELECTOR, "physicalDevice": False, "runNonce": nonce,
            "sourceFiles": sources, "sourceSha256": source_sha, "sourceCopyPath": str(mobile),
            "buildOutputPath": str(output), "manifestSha256": base.file_hash(mobile / MANIFEST),
            "originalPubspecSha256": base.digest(original), "privatePubspecSha256": base.digest(replacement),
            "pubspecLockSha256": base.file_hash(mobile / "pubspec.lock"),
            "entrySha256": base.file_hash(mobile / ENTRY), "driverSha256": base.file_hash(mobile / DRIVER),
            "initScriptSha256": base.file_hash(private_dir / INIT_NAME), "fixtureEvidenceSha256": base.EVIDENCE_SHA,
            "preservation": assert_protected(protected), "emulatorVerification": "NOT_RUN"}
        result["runtimeEnvironment"] = base.runtime_environment_binding(private_dir)
        require(local_sources()[1] == source_sha, "source_changed_during_prepare")
        base.write_new(private_dir / "prepare-receipt.json", result)
        return result
    except Exception as error:
        base.write_new(private_dir / "prepare-failure.json", {"status": "FAIL", "reason": "VERIFICATION_REJECTED",
            "failureType": type(error).__name__, "failureCode": str(error) if isinstance(error, EntryError) and
                re.fullmatch(r"[a-z0-9_]{1,96}", str(error)) else "prepare_failed"})
        raise


def validate_snapshot(prepared, private_dir):
    require(prepared.get("schemaVersion") == 1 and prepared.get("scope") == SCOPE and
        prepared.get("mode") == "profile" and prepared.get("appId") == APP_ID and
        prepared.get("dartTarget") == ENTRY and prepared.get("driver") == DRIVER and
        prepared.get("abi") == ABI and prepared.get("targetPlatform") == "android-arm64" and
        prepared.get("deviceSelector") == SELECTOR and prepared.get("physicalDevice") is False and
        prepared.get("fixtureEvidenceSha256") == base.EVIDENCE_SHA, "prepare_scope_rejected")
    base.dart_defines(prepared["runNonce"])
    require(prepared.get("runtimeEnvironment") == base.runtime_environment_binding(private_dir),
        "runtime_environment_changed")
    output = inside(prepared["buildOutputPath"], private_dir)
    require(output == private_dir / "build-output", "private_binding_path_rejected")
    mobile = inside(prepared["sourceCopyPath"], output)
    require(mobile == output / "source/repository/app/mobile", "private_binding_path_rejected")
    sources, source_sha = local_sources()
    require(source_sha == prepared["sourceSha256"] and sources == prepared["sourceFiles"], "source_changed")
    expected = {name[len("app/mobile/"):]: value for name, value in sources.items() if name.startswith("app/mobile/")}
    expected["pubspec.yaml"] = prepared["privatePubspecSha256"]
    actual = {str(path.relative_to(mobile)): base.file_hash(path) for path in selected_mobile_sources(mobile)
        if not path.is_relative_to(mobile / "assets/local_lossless")}
    require(actual == expected and base.file_hash(mobile / MANIFEST) == prepared["manifestSha256"] and
        base.file_hash(mobile / "pubspec.lock") == prepared["pubspecLockSha256"] and
        base.file_hash(mobile / ENTRY) == prepared["entrySha256"] and
        base.file_hash(mobile / DRIVER) == prepared["driverSha256"] and
        base.file_hash(private_dir / INIT_NAME) == prepared["initScriptSha256"] and
        read_source(private_dir / INIT_NAME) == init_script(prepared["runNonce"]), "private_snapshot_changed")
    _, cases = base.fixtures()
    expected_manifest = {"schemaVersion": 1, "evidenceSha256": base.EVIDENCE_SHA,
        "cases": [{key: value for key, value in row.items() if key != "sourcePath"} for row in cases]}
    require(strict_json(read_source(mobile / MANIFEST)) == expected_manifest, "fixture_manifest_changed")
    for row in cases:
        require(base.file_hash(mobile / row["asset"]) == row["sha256"] and
            (mobile / row["asset"]).stat().st_size == row["bytes"], "copied_fixture_changed")
    require({path.name for path in (mobile / "assets/local_lossless").iterdir()} ==
        {"manifest.json", *(Path(row["asset"]).name for row in cases)}, "fixture_manifest_changed")


def environment(private_dir=None):
    env = base.qa_tool_environment(private_dir)
    env.update({"CI": "true", "FLUTTER_SUPPRESS_ANALYTICS": "true", "DART_SUPPRESS_ANALYTICS": "true",
        "PUB_ENVIRONMENT": "tellpal_local_lossless_emulator"})
    return env


def offline_command(argv):
    require(shutil.which("sandbox-exec") is not None, "offline_guard_unavailable")
    return ["sandbox-exec", "-p", LOCAL_NETWORK_POLICY, *argv]


def checked(argv, output, alias, *, cwd=None, offline=False, seconds=30, private_dir=None):
    argv = offline_command(argv) if offline else argv
    status = base.bounded_tool(argv, output, alias, cwd=cwd, env=environment(private_dir), seconds=seconds)
    require(status == "ok", "tool_" + status)
    return read_source(output / (alias + ".stdout"))


def tools():
    flutter = shutil.which("flutter")
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    java = os.environ.get("JAVA_HOME")
    require(flutter and sdk and java, "build_tools_unavailable")
    flutter = Path(flutter).resolve(strict=True)
    sdk = Path(sdk).resolve(strict=True)
    java = Path(java).resolve(strict=True)
    require((java / "bin/java").is_file(), "build_tools_unavailable")
    java_release = java / "release"
    java_metadata = read_source(java_release).decode()
    java_version = re.findall(r'^JAVA_VERSION="([0-9]+(?:\.[0-9]+)*(?:[+_-][^"\r\n]+)?)"$', java_metadata, re.M)
    require(len(java_version) == 1 and java_version[0].split(".")[0] in ("17", "21"),
        "java_runtime_incompatible")
    candidates = sorted((sdk / "build-tools").glob("*/aapt"), key=lambda path:
        tuple(int(part) for part in path.parent.name.split(".")) if
        re.fullmatch(r"\d+(\.\d+)*", path.parent.name) else (), reverse=True)
    platform = sdk / "platforms/android-36/android.jar"
    require(candidates and (sdk / "platform-tools/adb").is_file() and platform.is_file() and
        (candidates[0].parent / "source.properties").is_file() and
        (candidates[0].parent / "lib/d8.jar").is_file(), "build_tools_unavailable")
    sdk_sources = {}
    for relative in ("android/android_device.dart", "android/android_sdk.dart", "drive/drive_service.dart"):
        path = flutter.parent.parent / "packages/flutter_tools/lib/src" / relative
        sdk_sources[relative] = base.file_hash(path)
        text = read_source(path).decode()
        if relative == "android/android_device.dart":
            require("_androidSdk.licensesAvailable && _androidSdk.latestVersion == null" in text and
                "if (!prebuiltApplication ||" in text, "sdk_prebuilt_guard_unverified")
        if relative == "drive/drive_service.dart":
            require("'VM_SERVICE_URL': _vmServiceUri" in text and "Application failed to start on attempt:" in text,
                "sdk_drive_contract_unverified")
    engine = flutter.parent.parent / "bin/cache/artifacts/engine/android-arm64-profile/flutter.jar"
    require(engine.is_file(), "offline_cache_missing")
    return {"flutter": str(flutter), "sdk": str(sdk), "java": str(java),
        "javaVersion": java_version[0], "javaReleaseSha256": base.file_hash(java_release),
        "aapt": str(candidates[0].resolve(strict=True)), "adb": str((sdk / "platform-tools/adb").resolve(strict=True)),
        "profileEngineJar": str(engine), "androidJarSha256": base.file_hash(platform),
        "sdkSourcesSha256": base.digest(frozen_json(sdk_sources))}


def settings(mobile, selected):
    def quote(value):
        require("\n" not in value and "\r" not in value, "build_tools_unavailable")
        return value.replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=").replace(" ", "\\ ")
    value = "flutter.sdk=" + quote(str(Path(selected["flutter"]).parent.parent)) + "\nsdk.dir=" + quote(selected["sdk"]) + "\n"
    base.write_new(mobile / "android/local.properties", value.encode())
    return base.file_hash(mobile / "android/local.properties")


def build_command(mobile, private_dir, nonce):
    return [str(mobile / "android/gradlew"), "--offline", "--no-daemon", "--console=plain", "--max-workers=2",
        "-p", str(mobile / "android"), "-I", str(private_dir / INIT_NAME), ":app:assembleProfile",
        "-Ptarget=" + ENTRY, "-Ptarget-platform=android-arm64", "-Pdart-defines=" + base.dart_defines(nonce)]


def archive_bytes(archive, name, maximum=128 * 1024 * 1024):
    require(archive.namelist().count(name) == 1, "apk_archive_rejected")
    item = archive.getinfo(name)
    require(not item.is_dir() and item.file_size <= maximum and
        stat.S_IFMT(item.external_attr >> 16) != stat.S_IFLNK, "apk_archive_rejected")
    data = archive.read(name)
    require(len(data) == item.file_size, "apk_archive_rejected")
    return data


def profile_engine(selected):
    with ZipFile(selected["profileEngineJar"]) as archive:
        return base.digest(archive_bytes(archive, JAR_ENGINE, PROFILE_ENGINE_MAX))


def inspect_apk(apk, badging, prepared, engine_sha):
    require(apk.stat().st_size <= MAX_APK, "apk_archive_rejected")
    text = badging.decode("utf-8")
    require(re.search(r"^package: name='" + re.escape(APP_ID) + r"' ", text, re.M) is not None and
        re.search(r"^launchable-activity: name='" + re.escape(ACTIVITY) + r"' ", text, re.M) is not None,
        "apk_identity_rejected")
    native = re.findall(r"^native-code: (.+)$", text, re.M)
    require(len(native) == 1 and re.findall(r"'([^']+)'", native[0]) == [ABI], "apk_abi_rejected")
    _, cases = base.fixtures()
    with ZipFile(apk) as archive:
        names = archive.namelist()
        require(len(names) <= 10000 and len(names) == len(set(names)) and
            all(not name.startswith("/") and ".." not in Path(name).parts for name in names), "apk_archive_rejected")
        abis = {name.split("/")[1] for name in names if name.startswith("lib/") and len(name.split("/")) > 2}
        require(abis == {ABI}, "apk_abi_rejected")
        libapp = archive_bytes(archive, "lib/arm64-v8a/libapp.so")
        require(len(libapp) > 64 and libapp[:6] == b"\x7fELF\x02\x01" and
            int.from_bytes(libapp[18:20], "little") == 183, "apk_aot_rejected")
        require(base.digest(archive_bytes(archive, JAR_ENGINE, PROFILE_ENGINE_MAX)) == engine_sha, "apk_profile_engine_rejected")
        manifest = archive_bytes(archive, "assets/flutter_assets/" + MANIFEST, 1024 * 1024)
        require(base.digest(manifest) == prepared["manifestSha256"], "apk_fixture_rejected")
        for row in cases:
            data = archive_bytes(archive, "assets/flutter_assets/" + row["asset"], 16 * 1024 * 1024)
            require(len(data) == row["bytes"] and base.digest(data) == row["sha256"], "apk_fixture_rejected")
    return {"package": APP_ID, "launcher": ACTIVITY, "abi": ABI, "mode": "profile",
        "profileEngineSha256": engine_sha, "libappSha256": base.digest(libapp),
        "packagedManifestSha256": base.digest(manifest)}


def validate_tool_trace(output, alias, argv, runtime_binding=None):
    receipt = base.private_json(output / (alias + ".json"))
    require(receipt.get("status") == "ok" and receipt.get("argumentSha256") == base.digest(frozen_json(argv)) and
        receipt.get("traceSha256") == [base.file_hash(output / (alias + suffix)) for suffix in (".stdout", ".stderr")],
        "tool_receipt_rejected")
    if runtime_binding is not None:
        require(receipt.get("runtimeEnvironmentSha256") == base.digest(frozen_json(runtime_binding)),
            "runtime_environment_receipt_rejected")
    return base.file_hash(output / (alias + ".json"))


def build(private_dir):
    private_dir = base.private_path(private_dir, directory=True)
    prepared = base.private_json(private_dir / "prepare-receipt.json")
    validate_snapshot(prepared, private_dir)
    protected = base.private_json(private_dir / "preservation-before.private.json", 20 * 1024 * 1024)
    assert_protected(protected)
    base.write_new(private_dir / "build-attempt.json", {"status": "STARTED", "startedAtUtc": stamp(), "scope": SCOPE})
    output, mobile = Path(prepared["buildOutputPath"]), Path(prepared["sourceCopyPath"])
    try:
        selected = tools()
        local_sha = settings(mobile, selected)
        checked([selected["flutter"], "pub", "get", "--offline"], output, "flutter-packages", cwd=mobile, offline=True, seconds=180, private_dir=private_dir)
        validate_snapshot(prepared, private_dir)
        argv = build_command(mobile, private_dir, prepared["runNonce"])
        checked(argv, output, "gradle-profile", cwd=mobile / "android", offline=True, seconds=600, private_dir=private_dir)
        apk = mobile / "build/app/outputs/apk/profile/app-profile.apk"
        inside(apk, output)
        badging = checked([selected["aapt"], "dump", "badging", str(apk)], output, "apk-badging", offline=True, private_dir=private_dir)
        identity = inspect_apk(apk, badging, prepared, profile_engine(selected))
        version = strict_json(checked([selected["flutter"], "--version", "--machine"], output, "flutter-version", offline=True, private_dir=private_dir))
        require(type(version.get("frameworkVersion")) is str and re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9]+\.[0-9]+\.pre)?", version["frameworkVersion"]) and
            type(version.get("engineRevision")) is str and re.fullmatch(r"[a-f0-9]{40}", version["engineRevision"]),
            "flutter_version_rejected")
        validate_snapshot(prepared, private_dir)
        result = {"schemaVersion": 1, "scope": SCOPE, "mode": "profile", "appId": APP_ID, "dartTarget": ENTRY,
            "abi": ABI, "runNonce": prepared["runNonce"], "sourceSha256": prepared["sourceSha256"],
            "manifestSha256": prepared["manifestSha256"], "entrySha256": prepared["entrySha256"],
            "initScriptSha256": prepared["initScriptSha256"], "pubspecLockSha256": prepared["pubspecLockSha256"],
            "compiledDartDefinesSha256": base.digest(base.dart_defines(prepared["runNonce"]).encode()),
            "runtimeEnvironmentSha256": base.digest(frozen_json(prepared["runtimeEnvironment"])),
            "artifactPath": str(apk), "artifactSha256": base.file_hash(apk), "artifactIdentity": identity,
            "buildArgumentSha256": base.digest(frozen_json(argv)), "tools": selected,
            "localPropertiesSha256": local_sha, "offlineBuild": True,
            "buildToolReceiptSha256": validate_tool_trace(output, "gradle-profile", offline_command(argv), prepared["runtimeEnvironment"]),
            "badgingToolReceiptSha256": validate_tool_trace(output, "apk-badging",
                offline_command([selected["aapt"], "dump", "badging", str(apk)]), prepared["runtimeEnvironment"]),
            "flutterVersion": version.get("frameworkVersion"), "engineRevision": version.get("engineRevision"),
            "preservation": assert_protected(protected), "emulatorVerification": "NOT_RUN"}
        base.write_new(private_dir / "build-receipt.json", result)
        return result
    except Exception as error:
        reason = "BUILD_FAILED"
        if isinstance(error, EntryError) and str(error) == "offline_cache_missing":
            reason = "OFFLINE_CACHE_MISSING"
        elif isinstance(error, (FileNotFoundError, OSError)) or isinstance(error, EntryError) and str(error) in (
                "build_tools_unavailable", "offline_guard_unavailable", "tool_unavailable", "java_runtime_incompatible"):
            reason = "TOOLS_UNAVAILABLE"
        base.write_new(private_dir / "build-failure.json", {"status": "NOT_RUN", "reason": reason,
            "failureType": type(error).__name__, "preservation": preservation_result(protected)})
        raise


def validate_build(prepared, built, private_dir):
    apk = Path(prepared["sourceCopyPath"]) / "build/app/outputs/apk/profile/app-profile.apk"
    inside(apk, private_dir / "build-output")
    require(built.get("schemaVersion") == 1 and built.get("scope") == SCOPE and built.get("mode") == "profile" and
        built.get("appId") == APP_ID and built.get("dartTarget") == ENTRY and built.get("abi") == ABI and
        all(built.get(key) == prepared[key] for key in ("runNonce", "sourceSha256", "manifestSha256", "entrySha256",
            "initScriptSha256", "pubspecLockSha256")) and built.get("offlineBuild") is True and
        built.get("compiledDartDefinesSha256") == base.digest(base.dart_defines(prepared["runNonce"]).encode()) and
        built.get("runtimeEnvironmentSha256") == base.digest(frozen_json(prepared["runtimeEnvironment"])) and
        built.get("artifactPath") == str(apk) and built.get("artifactSha256") == base.file_hash(apk) and
        built.get("buildArgumentSha256") == base.digest(frozen_json(build_command(Path(prepared["sourceCopyPath"]),
            private_dir, prepared["runNonce"]))), "build_binding_rejected")
    require(type(built.get("flutterVersion")) is str and
        re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9]+\.[0-9]+\.pre)?", built["flutterVersion"]) and
        type(built.get("engineRevision")) is str and re.fullmatch(r"[a-f0-9]{40}", built["engineRevision"]),
        "flutter_version_rejected")
    selected = built["tools"]
    require(selected == tools() and base.file_hash(Path(prepared["sourceCopyPath"]) / "android/local.properties") ==
        built.get("localPropertiesSha256"), "build_tools_changed")
    output = private_dir / "build-output"
    argv = build_command(Path(prepared["sourceCopyPath"]), private_dir, prepared["runNonce"])
    require(built.get("buildToolReceiptSha256") == validate_tool_trace(output, "gradle-profile", offline_command(argv), prepared["runtimeEnvironment"]) and
        built.get("badgingToolReceiptSha256") == validate_tool_trace(output, "apk-badging", offline_command(
            [selected["aapt"], "dump", "badging", str(apk)]), prepared["runtimeEnvironment"]), "build_binding_rejected")
    identity = inspect_apk(apk, read_source(output / "apk-badging.stdout"), prepared, profile_engine(selected))
    require(identity == built.get("artifactIdentity"), "build_binding_rejected")
    return base.file_hash(apk)


def validate_device(observed):
    require(type(observed) is dict and observed.get("selector") == SELECTOR and observed.get("state") == "device" and
        observed.get("qemu") == "1" and observed.get("abi") == ABI and observed.get("api") == str(API) and
        type(observed.get("fingerprint")) is str and 1 <= len(observed["fingerprint"]) <= 2048 and
        type(observed.get("avd")) is str and 1 <= len(observed["avd"]) <= 256 and
        observed.get("wmSize") == "Physical size: 1080x2400\nOverride size: 822x1828" and
        observed.get("wmDensity") == "Physical density: 420\nOverride density: 320", "emulator_identity_rejected")
    return base.digest(frozen_json(observed))


def observe_device(private_dir, selected, stage):
    require(stage in ("run", "verify"), "device_stage_rejected")
    commands = {"state": ["get-state"], "qemu": ["shell", "getprop", "ro.kernel.qemu"],
        "abi": ["shell", "getprop", "ro.product.cpu.abi"], "api": ["shell", "getprop", "ro.build.version.sdk"],
        "fingerprint": ["shell", "getprop", "ro.build.fingerprint"],
        "avd": ["shell", "getprop", "ro.boot.qemu.avd_name"], "wmSize": ["shell", "wm", "size"],
        "wmDensity": ["shell", "wm", "density"]}
    observed = {"selector": SELECTOR}
    for key, args in commands.items():
        raw = checked([selected["adb"], "-s", SELECTOR, *args], private_dir / "build-output",
            stage + "-device-" + key.lower(), offline=True, seconds=15, private_dir=private_dir)
        require(len(raw) <= 4096, "emulator_observation_rejected")
        observed[key] = raw.decode("utf-8").replace("\r\n", "\n").strip()
    validate_device(observed)
    base.write_new(private_dir / (stage + "-device.private.json"), observed)
    return observed


def launch_command(prepared, built):
    require(prepared["deviceSelector"] == SELECTOR, "emulator_identity_rejected")
    return offline_command([built["tools"]["flutter"], "drive", "--profile", "--no-pub",
        "--use-application-binary=" + built["artifactPath"], "--device-id", SELECTOR,
        "--target", ENTRY, "--driver", DRIVER, "--no-keep-app-running"])


def launch_binding(prepared, built, private_dir):
    return {"schemaVersion": 1, "scope": SCOPE, "mode": "profile", "appId": APP_ID, "dartTarget": ENTRY,
        "runNonce": prepared["runNonce"], "sourceSha256": prepared["sourceSha256"],
        "sourceCopyPath": prepared["sourceCopyPath"], "deviceSelector": SELECTOR,
        "artifactPath": built["artifactPath"], "artifactSha256": validate_build(prepared, built, private_dir),
        "runtimeEnvironmentSha256": base.digest(frozen_json(prepared["runtimeEnvironment"])),
        "argumentSha256": base.digest(frozen_json(launch_command(prepared, built))),
        "buildReceiptSha256": base.file_hash(private_dir / "build-receipt.json")}


def run(private_dir):
    private_dir = base.private_path(private_dir, directory=True)
    prepared = base.private_json(private_dir / "prepare-receipt.json")
    built = base.private_json(private_dir / "build-receipt.json")
    validate_snapshot(prepared, private_dir)
    protected = base.private_json(private_dir / "preservation-before.private.json", 20 * 1024 * 1024)
    assert_protected(protected)
    require(not (private_dir / "device-run").exists() and not (private_dir / "device-run").is_symlink(), "launch_attempt_exists")
    binding = launch_binding(prepared, built, private_dir)
    base.write_new(private_dir / "launch-attempt.json", {**binding, "status": "STARTED", "startedAtUtc": stamp()})
    launch = {**binding, "status": "FAIL", "toolStatus": "NOT_RUN", "reason": "EMULATOR_UNAVAILABLE"}
    try:
        observed = observe_device(private_dir, built["tools"], "run")
        launch["deviceEvidenceSha256"] = base.file_hash(private_dir / "run-device.private.json")
        launch["deviceIdentitySha256"] = validate_device(observed)
        env = environment(private_dir)
        env.update({"LOCAL_LOSSLESS_RUN_NONCE": prepared["runNonce"],
            "LOCAL_LOSSLESS_EVIDENCE_DIR": str(private_dir / "device-run")})
        launch["reason"] = "DRIVER_FAILED"
        status = base.bounded_tool(launch_command(prepared, built), private_dir / "build-output", "flutter-drive",
            cwd=prepared["sourceCopyPath"], env=env, seconds=600)
        launch["toolStatus"] = status
        require(status == "ok", "driver_launch_failed")
        for suffix in (".stdout", ".stderr"):
            trace = read_source(private_dir / "build-output" / ("flutter-drive" + suffix))
            require(b"Application failed to start on attempt:" not in trace,
                "sdk_launch_retry_rejected")
        result_path = private_dir / "device-run/evidence.json"
        result = base.private_json(result_path)
        _, cases = base.fixtures()
        base.validate_driver(result, cases, result_path.parent, prepared["runNonce"])
        require(type(result.get("vmServiceUrlSha256")) is str and
            re.fullmatch(r"[a-f0-9]{64}", result["vmServiceUrlSha256"]) and
            result.get("vmServiceUrlSource") == "flutter_drive_environment", "vm_endpoint_binding_rejected")
        validate_snapshot(prepared, private_dir)
        validate_build(prepared, built, private_dir)
        launch.update({"status": "PASS", "reason": None, "driverResultPath": str(result_path),
            "driverResultSha256": base.file_hash(result_path), "vmServiceUrlSha256": result["vmServiceUrlSha256"],
            "vmServiceUrlSource": "flutter_drive_environment", "launchAttemptSha256": base.file_hash(private_dir / "launch-attempt.json"),
            "toolReceiptSha256": validate_tool_trace(private_dir / "build-output", "flutter-drive", launch_command(prepared, built), prepared["runtimeEnvironment"])})
    except Exception as error:
        launch["failureType"] = type(error).__name__
    finally:
        launch["preservation"] = preservation_result(protected)
        if not launch["preservation"]["unchanged"]:
            launch.update({"status": "FAIL", "reason": "VERIFICATION_REJECTED"})
        base.write_new(private_dir / "launch-receipt.json", launch)
    require(launch["status"] == "PASS", "driver_launch_failed")
    return launch


def validate_launch(prepared, built, launch, result, result_path, private_dir):
    binding = launch_binding(prepared, built, private_dir)
    attempt = base.private_json(private_dir / "launch-attempt.json")
    observed = base.private_json(private_dir / "run-device.private.json")
    for suffix in (".stdout", ".stderr"):
        require(b"Application failed to start on attempt:" not in read_source(private_dir / "build-output" /
            ("flutter-drive" + suffix)), "sdk_launch_retry_rejected")
    require(launch.get("status") == "PASS" and launch.get("toolStatus") == "ok" and
        all(launch.get(key) == value and attempt.get(key) == value for key, value in binding.items()) and
        attempt.get("status") == "STARTED" and result_path == private_dir / "device-run/evidence.json" and
        launch.get("driverResultPath") == str(result_path) and launch.get("driverResultSha256") == base.file_hash(result_path) and
        launch.get("launchAttemptSha256") == base.file_hash(private_dir / "launch-attempt.json") and
        launch.get("toolReceiptSha256") == validate_tool_trace(private_dir / "build-output", "flutter-drive",
            launch_command(prepared, built), prepared["runtimeEnvironment"]) and launch.get("deviceEvidenceSha256") == base.file_hash(private_dir / "run-device.private.json") and
        launch.get("deviceIdentitySha256") == validate_device(observed) and
        type(launch.get("vmServiceUrlSha256")) is str and re.fullmatch(r"[a-f0-9]{64}", launch["vmServiceUrlSha256"]) and
        launch["vmServiceUrlSha256"] == result.get("vmServiceUrlSha256") and
        launch.get("vmServiceUrlSource") == result.get("vmServiceUrlSource") == "flutter_drive_environment" and
        result.get("runNonce") == prepared["runNonce"], "launch_binding_rejected")
    return binding["artifactSha256"]


def destination(public):
    require(public is not None, "public_attempt_required")
    selected = Path(public)
    require(selected != PUBLIC and selected.is_absolute() and selected.resolve(strict=False) == selected and
        PUBLIC.resolve(strict=False) == PUBLIC and PUBLIC.is_dir() and not PUBLIC.is_symlink() and
        selected.is_relative_to(PUBLIC), "public_path_rejected")
    relative = selected.relative_to(PUBLIC).parts
    require(len(relative) == 2 and relative[0] == "attempts" and
        re.fullmatch(r"[a-z0-9][a-z0-9-]{0,95}", relative[1]) and
        (not selected.parent.exists() or selected.parent.is_dir() and not selected.parent.is_symlink()), "public_path_rejected")
    require(not selected.exists() and not selected.is_symlink(), "public_evidence_exists")
    return selected


def publish(report, screenshots, source, public, protected=None):
    public = destination(public)
    if not public.parent.exists():
        public.parent.mkdir(mode=0o755)
    public = destination(public)
    public.mkdir(mode=0o755)
    for screen in screenshots:
        data = read_source(source / screen["path"])
        require(base.digest(data) == screen["sha256"], "screenshot_changed_during_publish")
        base.write_new(public / screen["path"], data)
    lines = ["# Android emülatöründe yerel kayıpsız format karşılaştırması", "", f"Sonuç: **{report['status']}**.", "",
        "Ortam Android emülatörüdür. Fiziksel cihaz, üretim performansı ve insan kalite kabulü sağlanmadı.", ""]
    if report["status"] == "PASS":
        lines += ["Ayrı profile paketinde altı sabit durumun encoded byte/SHA, 822×1334 tam decode, tüm 3.289.644 RGB byte eşitliği, gerçek DPR ve 1:1 contain paint bilgileri ile altı screenshot doğrulandı.", "",
            "| Durum | Asset okuma µs | SHA µs | Decode µs | RGB readback µs |", "|---|---:|---:|---:|---:|"]
        for row in report["cases"]:
            lines.append(f"| {row['caseId']} | {row['assetReadMicros']} | {row['checksumMicros']} | {row['decodeMicros']} | {row['rgbReadbackMicros']} |")
        lines += ["", "Stop sonrası owned buffer/descriptor/codec/image sayaçları sıfırdır.", ""]
    else:
        lines += ["Emülatör doğrulaması tamamlanmadı. Gerekçe: `" + report["reason"] + "`. Private hata makbuzları korundu.", ""]
    lines += ["Decode süresine RGB readback veya screenshot dahil değildir. RGB readback, alpha tarama, RGB kopyası ve SHA doğrulamasını içerir. Plan her format ve asset için tek seri örnektir. Kabul edilen örnek sayısı JSON kaydında ayrıca verilir. Hız ve RSS farklarından üretim üstünlüğü çıkarılamaz. RSS anlık süreç örneğidir; peak/GPU değildir. Loaded RSS doğrulama yükünü içerir. Teorik RGBA görsel başına 4.386.192 byte'tır. Renderer gözlenmedi (`unobserved`).", "",
        "HTTP transferi ölçülmedi; asset boyutları HTTP byte ölçümü değildir. Gerçek OS background testi NOT_RUN. Fiziksel Android ve gerçek iPhone kabulü, tam reader ve insan kalite kabulü ayrı kalır. P0/P4b ve üretim kapıları açıktır.", ""]
    base.write_new(public / "verification.md", "\n".join(lines).encode())
    if protected is not None:
        assert_protected(protected, authorized_public=public)
    base.write_new(public / "verification.json", report)
    if protected is not None:
        assert_protected(protected, authorized_public=public)
    return report


def verify(private_dir, driver_result=None, not_run_reason=None, public=None):
    public = destination(public)
    private_dir = base.private_path(private_dir, directory=True)
    prepared = {}
    report = {"schemaVersion": 1, "scope": SCOPE, "observedAtUtc": stamp(), "status": "NOT_RUN",
        "reason": not_run_reason or "DRIVER_NOT_RUN", "environment": "android_emulator", "physicalDevice": False,
        "performanceAccepted": False, "humanQualityAccepted": False, "mode": "profile", "package": APP_ID,
        "fixtureEvidenceSha256": base.EVIDENCE_SHA,
        "theoreticalRgbaBytes": base.RGBA_BYTES, "plannedSamplesPerFormatPerAsset": 1,
        "acceptedSamplesPerFormatPerAsset": 0, "acceptedCaseCount": 0, "renderer": "unobserved",
        "rssKind": "INSTANTANEOUS_PROCESS_RSS_NOT_PEAK_OR_GPU",
        "rssLoadedScope": "after_rgb_readback_and_checksum_includes_validation_overhead",
        "rgbReadbackScope": "native_readback_alpha_scan_rgb_copy_and_sha_validation", "httpTransferMeasured": False,
        "realOsBackgroundTest": "NOT_RUN", "fullWidthReaderAccepted": False, "productionGate": "OPEN", "p0Gate": "OPEN",
        "p4bGate": "OPEN", "physicalAndroidAccepted": False, "physicalIphoneAccepted": False,
        "cases": [], "screenshots": [], "stopped": None}
    screenshots, source, protected = [], None, None
    try:
        prepare_path = private_dir / "prepare-receipt.json"
        if not prepare_path.exists():
            failure = base.private_json(private_dir / "prepare-failure.json")
            require(failure.get("status") == "FAIL" and failure.get("reason") in REASONS,
                "prepare_failure_rejected")
            report.update({"status": "FAIL", "reason": failure["reason"], "failureStage": "prepare",
                "prepareFailureReceiptSha256": base.file_hash(private_dir / "prepare-failure.json"),
                "build": {"status": "NOT_RUN"}, "launch": {"status": "NOT_RUN"}})
            return publish(report, [], None, public)
        prepared = base.private_json(prepare_path)
        validate_snapshot(prepared, private_dir)
        for key in ("sourceSha256", "pubspecLockSha256", "manifestSha256", "entrySha256", "initScriptSha256"):
            require(type(prepared.get(key)) is str and re.fullmatch(r"[a-f0-9]{64}", prepared[key]), "public_digest_rejected")
        report.update({key: prepared[key] for key in ("sourceSha256", "pubspecLockSha256", "manifestSha256", "entrySha256", "initScriptSha256")})
        protected = base.private_json(private_dir / "preservation-before.private.json", 20 * 1024 * 1024)
        report["preservation"] = assert_protected(protected)
        if driver_result is not None:
            report["status"] = "FAIL"
            result_path = base.private_path(driver_result)
            result = base.private_json(result_path)
            built = base.private_json(private_dir / "build-receipt.json")
            launch = base.private_json(private_dir / "launch-receipt.json")
            artifact_sha = validate_launch(prepared, built, launch, result, result_path, private_dir)
            _, cases = base.fixtures()
            checked_result = base.validate_driver(result, cases, result_path.parent, prepared["runNonce"])
            observed = observe_device(private_dir, built["tools"], "verify")
            require(validate_device(observed) == launch["deviceIdentitySha256"], "emulator_changed_since_launch")
            report.update(checked_result)
            screenshots, source = checked_result["screenshots"], result_path.parent
            report.update({"status": "PASS", "reason": None, "acceptedSamplesPerFormatPerAsset": 1,
                "acceptedCaseCount": 6, "artifactSha256": artifact_sha,
                "buildReceiptSha256": base.file_hash(private_dir / "build-receipt.json"),
                "launchReceiptSha256": base.file_hash(private_dir / "launch-receipt.json"),
                "driverResultSha256": base.file_hash(result_path), "runNonceSha256": base.digest(prepared["runNonce"].encode()),
                "driverSha256": prepared["driverSha256"], "vmServiceUrlSha256": launch["vmServiceUrlSha256"],
                "device": {"physicalDevice": False, "api": API, "abi": ABI,
                    "launchEvidenceSha256": launch["deviceEvidenceSha256"],
                    "verifyEvidenceSha256": base.file_hash(private_dir / "verify-device.private.json")},
                "artifactIdentity": built["artifactIdentity"], "flutterVersion": built.get("flutterVersion"),
                "engineRevision": built.get("engineRevision")})
        else:
            require(report["reason"] in REASONS, "reason_rejected")
            if (private_dir / "launch-attempt.json").exists():
                report.update({"status": "FAIL", "reason": "DRIVER_FAILED"})
            for name in ("build-failure.json", "launch-receipt.json"):
                if (private_dir / name).exists():
                    receipt = base.private_json(private_dir / name)
                    report[name.replace(".json", "Sha256")] = base.file_hash(private_dir / name)
                    if receipt.get("reason") in REASONS:
                        report["reason"] = receipt["reason"]
    except Exception:
        report.update({"status": "FAIL", "reason": "VERIFICATION_REJECTED", "acceptedSamplesPerFormatPerAsset": 0,
            "acceptedCaseCount": 0, "cases": [], "screenshots": [], "stopped": None})
        screenshots = []
    return publish(report, screenshots, source, public, protected=protected)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    for name in ("prepare", "build", "run", "verify"):
        modes.add_argument("--" + name, action="store_true")
    parser.add_argument("--private-dir", type=Path, required=True)
    parser.add_argument("--preserve-private-dir", type=Path, action="append", default=[])
    parser.add_argument("--driver-result", type=Path)
    parser.add_argument("--not-run-reason", choices=sorted(REASONS))
    parser.add_argument("--public-dir", type=Path)
    args = parser.parse_args(argv)
    try:
        require(args.verify or args.public_dir is None and args.driver_result is None and args.not_run_reason is None,
            "verification_arguments_rejected")
        require(args.prepare or not args.preserve_private_dir, "preservation_arguments_rejected")
        require(not (args.driver_result is not None and args.not_run_reason is not None), "verification_arguments_rejected")
        if args.prepare:
            prepare(args.private_dir, args.preserve_private_dir)
        elif args.build:
            build(args.private_dir)
        elif args.run:
            run(args.private_dir)
        else:
            result = verify(args.private_dir, args.driver_result, args.not_run_reason, args.public_dir)
            print("Android emülatör doğrulaması: " + result["status"])
            return 0 if result["status"] == "PASS" else 2
        print("Emülatör adımı tamamlandı; fiziksel cihaz ve üretim kabulü sağlanmadı.")
        return 0
    except Exception:
        print("İşlem reddedildi; private kanıtlar korundu. Yeni public kayıt için --verify kullanılır.")
        return 2


if __name__ == "__main__":
    sys.exit(main())
