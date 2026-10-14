#!/usr/bin/env python3
"""Explicit, byte-bound Kotlin linkage checks; never discover private SDK inputs."""
from hashlib import sha256
from pathlib import Path
import os
import re
import time

import native_entry_android_plugins as plugins
import native_entry_driver as driver
from native_entry_config import (EntryError, exact_keys, frozen_json, no_symlink,
    private_file, private_output, require, strict_json, write_private)

SCOPE = "android-instrumentation-runtime-integration-r1"
OLD_APP = "ee3771a9a0c9be56430f559c0e1622abf2d1a175dffaf4144fe923c4b3ed210f"
OLD_TEST = "6a3bafac265e80b8f2f62735d6954b94792403c8b3554255a4fc4b2dbb0242bf"
ROOT_CLASSES = tuple("com.tellpal.imageexperiment." + name for name in
    ("ImageExperimentHandshakeTests", "ImageExperimentInstrumentation"))
REQUIRED = {"kotlin.text.StringsKt", "kotlin.ranges.RangesKt", "kotlin.ResultKt"}
CLASS_LIMIT = 128
INVOKE = re.compile(r"^\s+invoke-static(?:/range)? \{[^}\n]*\}, (Lkotlin/[^;\s]+;)->([^\s]+)$", re.M)


def runtime_definition(raw, name):
    """Raw declarations and executable method bodies, never mapping/reference text."""
    text = plugins.defined_class(raw, name)
    supers = re.findall(r"^\.super (L[^;\s]+;)$", text, re.M)
    require(len(supers) == 1, "runtime_definition_rejected")
    methods = {}
    for header, body in re.findall(r"^\.method ([^\n]+)\n(.*?)^\.end method$", text, re.M | re.S):
        tokens = header.split()
        require(tokens and tokens[-1] not in methods, "runtime_definition_rejected")
        methods[tokens[-1]] = (set(tokens[:-1]), body)
    return text, supers[0][1:-1].replace("/", "."), methods


def static_references(raw, name):
    text, _, _ = runtime_definition(raw, name)
    return {(owner[1:-1].replace("/", "."), method) for owner, method in INVOKE.findall(text)}


def resolve_static(owner, signature, read_class):
    """DEX static resolution follows the actual superclass chain of the facade."""
    seen, chain = set(), []
    current = owner
    while current.startswith("kotlin."):
        require(current not in seen and len(seen) < 32, "runtime_inheritance_rejected")
        seen.add(current)
        chain.append(current)
        text, parent, methods = runtime_definition(read_class(current), current)
        if current == owner:
            flags = re.search(r"^\.class ([^\n]+)$", text, re.M)[1].split()[:-1]
            require("public" in flags, "runtime_class_not_callable")
        if signature in methods:
            flags, body = methods[signature]
            require({"public", "static"}.issubset(flags) and not {"abstract", "native"}.intersection(flags)
                and re.search(r"^\s+return(?:-void|-object|-wide)?(?:\s+[vp]\d+)?$", body, re.M),
                "runtime_method_not_callable")
            return {"owner": owner, "signature": signature, "definedBy": current,
                "inheritance": chain}
        current = parent
    raise EntryError("runtime_method_missing")


def defined_inventory(raw):
    try:
        text = raw.decode("utf-8", "strict")
    except UnicodeError:
        raise EntryError("runtime_inventory_rejected") from None
    names = re.findall(r"^C d \d+\s+\d+\s+\d+\s+(" + plugins.NAME + r")$", text, re.M)
    require(names and len(names) <= CLASS_LIMIT and len(set(names)) == len(names)
        and set(ROOT_CLASSES).issubset(names), "runtime_inventory_rejected")
    return sorted(names)


def inspection(command, output, alias, deadline):
    raw = driver.checked_run(command, output, alias, deadline=deadline)
    return raw, {"stdoutSha256": sha256(raw).hexdigest(),
        "stderrSha256": sha256(private_file(output / (alias + ".stderr"), driver.MAX_TRACE_BYTES)).hexdigest(),
        "commandReceiptSha256": sha256(private_file(output / (alias + ".json"))).hexdigest()}


def check_runtime(app, test, analyzer, output, deadline):
    before = {"app": driver.artifact_hash(app), "test": driver.artifact_hash(test)}
    traces, definitions, references = {}, {}, set()
    raw, traces["inventory"] = inspection([str(analyzer), "dex", "packages", "--defined-only", str(test)],
        output, "runtime-test-inventory", deadline)
    for index, name in enumerate(defined_inventory(raw)):
        raw, traces[name] = inspection([str(analyzer), "dex", "code", "--class", name, str(test)],
            output, "runtime-test-" + str(index), deadline)
        references.update(static_references(raw, name))
    require(REQUIRED.issubset({owner for owner, _ in references}), "runtime_references_incomplete")
    def read_class(name):
        if name not in definitions:
            require(len(definitions) < CLASS_LIMIT, "runtime_definition_bound_rejected")
            raw, trace = inspection([str(analyzer), "dex", "code", "--class", name, str(app)],
                output, "runtime-app-" + str(len(definitions)), deadline)
            runtime_definition(raw, name)
            definitions[name], traces[name] = raw, trace
        return definitions[name]
    calls = [resolve_static(owner, method, read_class) for owner, method in sorted(references)]
    require(before == {"app": driver.artifact_hash(app), "test": driver.artifact_hash(test)},
        "runtime_apk_binding_changed")
    return {"status": "callable_runtime_verified", "artifactSha256": before,
        "calls": calls, "inspections": traces}


def load_receipt(path, mode, old=False):
    raw = private_file(path, 2 * 1024 * 1024)
    receipt = strict_json(raw, 2 * 1024 * 1024)
    require(type(receipt) is dict, "runtime_build_receipt_rejected")
    require(receipt.get("platform") == "android" and receipt.get("mode") == mode,
        "runtime_build_receipt_rejected")
    expected = "com.tellpal.tellpal_mobile_poc.imageexperiment." + mode
    require(receipt.get("expectedAppId") == expected and receipt.get("testAppId") == expected + ".test",
        "runtime_build_receipt_rejected")
    for field in ("artifact", "testArtifact"):
        require(driver.artifact_hash(receipt[field + "Path"]) == receipt.get(field + "Sha256"),
            "runtime_archive_binding_rejected")
    if old:
        require(receipt["artifactSha256"] == OLD_APP and receipt["testArtifactSha256"] == OLD_TEST,
            "runtime_r233_binding_rejected")
    return receipt, sha256(raw).hexdigest()


def load_inputs(path):
    raw = private_file(path, 16384)
    values = strict_json(raw, 16384)
    keys = ("apkanalyzer", "javaHome", "profileBuildReceipt", "releaseBuildReceipt", "oldReleaseBuildReceipt")
    exact_keys(values, ("scope", "schemaVersion", *keys, "privateOutputRoot"))
    require(values["scope"] == SCOPE and type(values["schemaVersion"]) is int and values["schemaVersion"] == 1,
        "runtime_inputs_rejected")
    paths = {key: no_symlink(values[key]) for key in keys}
    require(paths["javaHome"].is_dir() and (paths["javaHome"] / "bin/java").is_file()
        and all(paths[key].is_file() for key in keys if key != "javaHome")
        and os.access(paths["apkanalyzer"], os.X_OK), "runtime_inputs_rejected")
    receipts = {"profile": load_receipt(paths["profileBuildReceipt"], "profile"),
        "release": load_receipt(paths["releaseBuildReceipt"], "release"),
        "old": load_receipt(paths["oldReleaseBuildReceipt"], "release", old=True)}
    require(receipts["profile"][0]["certificateSha256"] == receipts["release"][0]["certificateSha256"] ==
        receipts["old"][0]["certificateSha256"] and receipts["profile"][0]["sourceSha256"] ==
        receipts["release"][0]["sourceSha256"], "runtime_product_binding_rejected")
    output = private_output(values["privateOutputRoot"], [path, *paths.values(),
        *(receipt[field] for receipt, _ in receipts.values() for field in ("artifactPath", "testArtifactPath"))], create=True)
    os.environ["JAVA_HOME"] = str(paths["javaHome"])
    return paths, receipts, output, sha256(raw).hexdigest()


def tool_digest(path):
    # Reading a public SDK executable can update atime without changing its bytes.
    require(Path(path).stat().st_size <= 1024 * 1024, "runtime_tool_size_rejected")
    observation = driver.artifact_observation(path)
    require(observation["fileCount"] == 1 and observation["byteCount"] <= 1024 * 1024,
        "runtime_tool_size_rejected")
    return observation["sha256"]


def old_negative(receipt, analyzer, output, deadline):
    traces = {}
    for field in ("artifact", "testArtifact"):
        apk = receipt[field + "Path"]
        for index, name in enumerate(sorted(REQUIRED)):
            alias = "old-" + field.lower() + "-" + str(index)
            remaining = deadline - time.monotonic()
            require(remaining > 0, "driver_expired")
            result = driver.run_bounded([str(analyzer), "dex", "code", "--class", name, apk], output,
                alias, seconds=min(20, remaining))
            require(result["status"] == "failed" and private_file(output / (alias + ".stdout")) == b""
                and private_file(output / (alias + ".stderr")).strip() ==
                ("ERROR: The given class (" + name + ") not found").encode(), "runtime_negative_unverified")
            traces[alias] = sha256(private_file(output / (alias + ".json"))).hexdigest()
        require(driver.artifact_hash(apk) == receipt[field + "Sha256"], "runtime_apk_binding_changed")
    return {"status": "missing_runtime_refused", "appSha256": OLD_APP, "testSha256": OLD_TEST, "inspections": traces}


def check_plugins(receipt, analyzer, output, deadline):
    recorded = receipt.get("androidPluginRegistration", {})
    require(type(recorded) is dict, "runtime_plugin_provenance_rejected")
    require(recorded.get("status") == "defined_registration_verified" and recorded.get("artifactSha256") ==
        receipt["artifactSha256"], "runtime_plugin_provenance_rejected")
    try:
        mobile = no_symlink(receipt.get("sourceCopyPath"))
        require(mobile == mobile.resolve() and len(mobile.parents) >= 4, "runtime_plugin_provenance_rejected")
    except (EntryError, OSError, ValueError, TypeError):
        raise EntryError("runtime_plugin_provenance_rejected") from None
    build = mobile.parents[3]
    source = plugins.generated_source(mobile / plugins.SOURCE)
    copied = plugins.generated_source(build / "gradle/app" / plugins.COPIED)
    require(source == copied == recorded.get("generatedSourceSha256") == recorded.get("compiledSourceSha256"),
        "runtime_plugin_provenance_rejected")
    classes, add, mapping_path, mapping_hash = {}, "add", None, None
    if receipt["mode"] == "release":
        mapping_path = build / "gradle/app/outputs/mapping/release/mapping.txt"
        raw = private_file(mapping_path, plugins.MAX_MAPPING_BYTES, exact_mode=False)
        mapping_hash = sha256(raw).hexdigest()
        require(mapping_hash == recorded.get("mappingSha256"), "runtime_plugin_provenance_rejected")
        classes, add = plugins.mapping_names(raw)
    traces = {}
    for index, name in enumerate((plugins.REGISTRANT, *plugins.JNI_PLUGINS)):
        raw, traces[name] = inspection([str(analyzer), "dex", "code", "--class", name, receipt["artifactPath"]],
            output, "plugin-" + str(index), deadline)
        plugins.defined_class(raw, name)
        if name == plugins.REGISTRANT:
            plugins.registration_code(raw, classes, add)
    require(driver.artifact_hash(receipt["artifactPath"]) == receipt["artifactSha256"], "runtime_apk_binding_changed")
    require(plugins.generated_source(mobile / plugins.SOURCE) == source and
        plugins.generated_source(build / "gradle/app" / plugins.COPIED) == copied and
        (mapping_path is None or sha256(private_file(mapping_path, plugins.MAX_MAPPING_BYTES, exact_mode=False)).hexdigest() == mapping_hash),
        "runtime_plugin_provenance_rejected")
    return {"status": "defined_registration_verified", "generatedSourceSha256": source, "inspections": traces}


def main(argv=None):
    parser = driver.SafeParser(description="Explicit private Android instrumentation Kotlin runtime checks")
    parser.add_argument("--config", required=True)
    args = parser.parse_args(argv)
    output = None
    os.umask(0o077)
    try:
        paths, receipts, output, config_hash = load_inputs(args.config)
        tool_hash = tool_digest(paths["apkanalyzer"])
        deadline = time.monotonic() + driver.TOTAL_SECONDS
        negative = old_negative(receipts["old"][0], paths["apkanalyzer"], output, deadline)
        modes = {}
        for mode in ("profile", "release"):
            receipt, receipt_hash = receipts[mode]
            selected = output / mode
            selected.mkdir(mode=0o700)
            runtime = check_runtime(receipt["artifactPath"], receipt["testArtifactPath"], paths["apkanalyzer"], selected, deadline)
            modes[mode] = {"buildReceiptSha256": receipt_hash, "runtime": runtime,
                "plugins": check_plugins(receipt, paths["apkanalyzer"], selected, deadline)}
        require(tool_digest(paths["apkanalyzer"]) == tool_hash,
            "runtime_tool_binding_changed")
        for key, mode in (("profileBuildReceipt", "profile"), ("releaseBuildReceipt", "release"), ("oldReleaseBuildReceipt", "old")):
            require(load_receipt(paths[key], "release" if mode == "old" else mode, old=mode == "old")[1] == receipts[mode][1],
                "runtime_receipt_binding_changed")
        require(time.monotonic() < deadline, "driver_expired")
        write_private(output / "integration-receipt.json", {"scope": SCOPE, "status": "passed",
            "configSha256": config_hash, "apkanalyzerSha256": tool_hash, "oldRelease": negative, "modes": modes,
            "cloudSessionCreated": False, "physicalQualification": False, "authenticatedEntry": "not_run"})
        print(frozen_json({"scope": SCOPE, "status": "passed", "skipCount": 0}).decode())
        return 0
    except (EntryError, OSError, ValueError, KeyError, TypeError) as error:
        reason = str(error) if isinstance(error, EntryError) else "runtime_operation_failed"
        if output is not None:
            try:
                write_private(output / "integration-failure.json", {"scope": SCOPE, "status": "refused", "reason": reason})
            except (EntryError, OSError, ValueError, KeyError, TypeError):
                # Diagnostic storage failure cannot erase the original public refusal.
                pass
        print(frozen_json({"scope": SCOPE, "status": "refused", "reason": reason}).decode())
        return 4


if __name__ == "__main__":
    raise SystemExit(main())
