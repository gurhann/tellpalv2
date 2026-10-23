#!/usr/bin/env python3
"""Sabit altı yerel görseli ayrı iPhone profile paketinde doğrular."""
import argparse
import base64
from datetime import datetime, timezone
from hashlib import sha256
import json
import math
import os
from pathlib import Path
import plistlib
import re
import secrets
import selectors
import shutil
import signal
import stat
import subprocess
import sys
import time

from native_entry_config import EntryError, frozen_json, require, strict_json
from native_entry_driver import (MOBILE, ROOT, private_mobile_snapshot, read_source,
    selected_mobile_sources, source_snapshot, tool_environment)

EVIDENCE = ROOT / "be/docs/evidence/local-lossless-formats/verification.json"
EVIDENCE_SHA = "16adae98244f6821cdb15f81dcca4a7640c2075cbeaa2cbecfc34786a1e78100"
PREFLIGHT = Path("/private/tmp/tellpal-offline-reading-iphone-preflight-c8913e4241094a43a7bf080f866dc9a0.json")
PREFLIGHT_SHA = "fe02cb8638ff620fa23d1578fec991c4d66f9c66d1cf41124fd29d895c7f6faf"
PUBLIC = ROOT / "be/docs/evidence/local-lossless-mobile"
SCOPE = "local-lossless-mobile-r1"
APP_ID = "com.tellpal.tellpalMobilePoc.imageexperiment.profile"
FLAG = "TELLPAL_LOCAL_LOSSLESS_QA=true"
MANIFEST = "assets/local_lossless/manifest.json"
CASE_IDS = tuple(f"{asset}-{kind}" for asset in (9778, 9816, 9818) for kind in ("png", "webp"))
WIDTH, HEIGHT, RGB_BYTES, RGBA_BYTES = 822, 1334, 3289644, 4386192
NETWORK_POLICY = "(version 1)(allow default)(deny network*)"
COUNTERS = ("activeBuffers", "activeDescriptors", "activeCodecs", "activeImages")
SAFE_REASONS = frozenset(("DEVICE_UNAVAILABLE", "SIGNING_UNAVAILABLE", "OFFLINE_CACHE_MISSING",
    "BUILD_FAILED", "TOOLS_UNAVAILABLE", "DRIVER_NOT_RUN", "DRIVER_FAILED", "VERIFICATION_REJECTED"))
EXTERNAL_QA_ROOT = Path("/Volumes/MAC_EK/tellpal-qa-private")
RUNTIME_ENV_KEYS = ("TMPDIR", "JAVA_TOOL_OPTIONS", "GRADLE_USER_HOME", "PUB_CACHE")


def digest(data):
    return sha256(data).hexdigest()


def write_new(path, value):
    """Yeni makbuz mevcut dosyanın üzerine yazamaz."""
    data = value if isinstance(value, bytes) else (json.dumps(value, ensure_ascii=False,
        indent=2, allow_nan=False) + "\n").encode()
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600), "wb") as handle:
        handle.write(data)
        handle.flush()
        os.fsync(handle.fileno())


def private_path(path, *, directory=False):
    path = Path(path)
    require(path.is_absolute() and path.resolve(strict=True) == path and
        not path.is_relative_to(ROOT), "private_path_rejected")
    if path.is_relative_to(EXTERNAL_QA_ROOT):
        approved_external_root()
    else:
        require(path.is_relative_to(Path("/private/tmp")), "private_path_rejected")
    info = path.lstat()
    require(info.st_uid == os.getuid() and stat.S_IMODE(info.st_mode) == (0o700 if directory else 0o600) and
        (stat.S_ISDIR(info.st_mode) if directory else stat.S_ISREG(info.st_mode)), "private_path_rejected")
    return path


def approved_external_root():
    """Yalnız kullanıcının onayladığı gerçek ve özel dış dizini kabul eder."""
    root = EXTERNAL_QA_ROOT
    require(root.is_absolute() and root.resolve(strict=True) == root and not root.is_relative_to(ROOT),
        "external_qa_root_rejected")
    info = root.lstat()
    require(stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid() and
        stat.S_IMODE(info.st_mode) == 0o700, "external_qa_root_rejected")
    return root


def new_private_directory(path):
    path = Path(path)
    require(path.is_absolute() and path.parent.resolve(strict=True) == path.parent and
        path.is_relative_to(approved_external_root()) and path != EXTERNAL_QA_ROOT and
        not path.exists() and not path.is_symlink(),
        "output_not_unique")
    private_path(path.parent, directory=True)
    return path


def runtime_environment_binding(private_dir):
    """Geçici yazımları run köküne, cache'leri mevcut canonical hedeflere bağlar."""
    root = private_path(private_dir, directory=True)
    require(not any(char.isspace() for char in str(root)), "runtime_environment_rejected")
    caches = {}
    for name, relative in (("GRADLE_USER_HOME", ".gradle"), ("PUB_CACHE", ".pub-cache")):
        path = (Path.home() / relative).resolve(strict=True)
        require(path.is_dir() and path.stat().st_uid == os.getuid() and not path.is_relative_to(ROOT),
            "runtime_cache_rejected")
        require(path.is_relative_to(EXTERNAL_QA_ROOT.parent), "runtime_cache_not_external")
        caches[name] = str(path)
    return {"TMPDIR": str(root), "JAVA_TOOL_OPTIONS": "-Djava.io.tmpdir=" + str(root), **caches}


def qa_tool_environment(private_dir=None):
    env = tool_environment()
    env.update({"CI": "true", "FLUTTER_SUPPRESS_ANALYTICS": "true", "DART_SUPPRESS_ANALYTICS": "true",
        "COCOAPODS_DISABLE_STATS": "true"})
    if private_dir is not None:
        env.update(runtime_environment_binding(private_dir))
    return env


def private_json(path, maximum=4 * 1024 * 1024):
    path = private_path(path)
    require(path.stat().st_size <= maximum, "private_json_too_large")
    return strict_json(read_source(path), limit=maximum)


def file_hash(path):
    """Büyük eski build çıktısını değiştirmeden, sınırlı bloklarla özetler."""
    require(not path.is_symlink() and path.is_file(), "preserved_file_rejected")
    info = path.stat()
    require(info.st_size <= 1024 * 1024 * 1024, "preservation_bound_rejected")
    value = sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            value.update(block)
    after = path.stat()
    require((info.st_ino, info.st_size, info.st_mtime_ns) ==
        (after.st_ino, after.st_size, after.st_mtime_ns), "preserved_file_changed")
    return value.hexdigest()


def tree_hashes(root):
    """Sembolik bağların hedef metni de korunur; hedefleri takip edilmez."""
    require(root.is_dir() and not root.is_symlink(), "preserved_tree_unavailable")
    result = {}
    for directory, names, files in os.walk(root, followlinks=False):
        require(len(result) < 50000, "preservation_bound_rejected")
        parent = Path(directory)
        for name in sorted(names + files):
            path = parent / name
            key = str(path.relative_to(root))
            if path.is_symlink():
                result[key] = "symlink:" + os.readlink(path)
            elif path.is_file():
                result[key] = file_hash(path)
    return result


def fixtures():
    raw = read_source(EVIDENCE)
    require(digest(raw) == EVIDENCE_SHA, "fixture_evidence_changed")
    evidence = strict_json(raw, limit=2 * 1024 * 1024)
    require(evidence.get("status") == "PASS" and len(evidence.get("images", [])) == 3, "fixture_evidence_rejected")
    cases = []
    for image in evidence["images"]:
        asset_id = image["assetId"]
        require(image["dimensions"] == [WIDTH, HEIGHT] and image["rgbBytesCompared"] == RGB_BYTES and
            image["rgbMatchesGoldenPin"] is True and image["allRgbBytesEqual"] is True,
            "fixture_pixels_rejected")
        for kind in ("png", "webp"):
            pin = image[kind]
            source = ROOT / pin["path"]
            require(source.resolve(strict=True) == source and source.is_relative_to(ROOT / "be/var"),
                "fixture_path_rejected")
            data = read_source(source)
            require(len(data) == pin["bytes"] and digest(data) == pin["sha256"], "fixture_bytes_changed")
            validate_header(data, kind.upper())
            cases.append({"caseId": f"{asset_id}-{kind}", "assetId": asset_id,
                "asset": f"assets/local_lossless/{asset_id}.{kind}", "format": kind.upper(),
                "bytes": pin["bytes"], "sha256": pin["sha256"], "width": WIDTH, "height": HEIGHT,
                "rgbBytes": RGB_BYTES, "rgbSha256": image["rgbSha256"], "sourcePath": str(source)})
    require(tuple(row["caseId"] for row in cases) == CASE_IDS, "fixture_order_rejected")
    return evidence, cases


def validate_header(data, kind):
    if kind == "PNG":
        require(len(data) >= 33 and data[:8] == b"\x89PNG\r\n\x1a\n" and data[12:16] == b"IHDR" and
            int.from_bytes(data[16:20], "big") == WIDTH and int.from_bytes(data[20:24], "big") == HEIGHT and
            data[24:29] == bytes((8, 2, 0, 0, 0)), "fixture_header_rejected")
    else:
        require(kind == "WEBP" and len(data) >= 25 and data[:4] == b"RIFF" and data[8:16] == b"WEBPVP8L" and
            int.from_bytes(data[4:8], "little") + 8 == len(data) and data[20] == 0x2f,
            "fixture_header_rejected")
        bits = int.from_bytes(data[21:25], "little")
        require((bits & 0x3fff) + 1 == WIDTH and ((bits >> 14) & 0x3fff) + 1 == HEIGHT and
            bits >> 28 == 0, "fixture_header_rejected")


def preflight_selection(path=None, expected_sha=None):
    """Yeni seçim yalnız yol ve SHA birlikte verilirse varsayılanı değiştirir."""
    require((path is None) == (expected_sha is None), "preflight_pair_required")
    if path is None:
        return PREFLIGHT, PREFLIGHT_SHA
    require(type(expected_sha) is str and re.fullmatch(r"[0-9a-f]{64}", expected_sha), "preflight_sha_rejected")
    return private_path(path), expected_sha


def pinned_private_bytes(path, expected_sha):
    require(type(expected_sha) is str and re.fullmatch(r"[0-9a-f]{64}", expected_sha), "device_proof_sha_rejected")
    require(isinstance(path, (str, Path)), "device_proof_path_rejected")
    path = private_path(Path(path))
    require(path.stat().st_size <= 4 * 1024 * 1024, "device_proof_too_large")
    raw = read_source(path)
    require(digest(raw) == expected_sha, "device_proof_changed")
    return raw


def validate_flutter_device(preflight, settings):
    """CoreDevice kimliği Flutter selector'ı olamaz; pinned fiziksel UDID aranır."""
    device, discovery = preflight.get("device", {}), preflight.get("flutterDiscovery", {})
    udid = device.get("hardwareUdid")
    require(type(udid) is str and udid and udid.strip() == udid and device.get("selector") == udid and
        device.get("physical") is True and device.get("transportType") == "wired", "flutter_device_identity_rejected")
    evidence = strict_json(pinned_private_bytes(device.get("hardwareEvidencePath"),
        device.get("hardwareEvidenceSha256")))
    require(type(evidence) is dict and type(evidence.get("result")) is dict, "flutter_device_hardware_rejected")
    hardware = evidence["result"]
    properties = hardware.get("hardwareProperties", {})
    require(properties.get("udid") == udid and properties.get("reality") == "physical" and
        properties.get("platform") == "iOS" and properties.get("marketingName") == "iPhone 12",
        "flutter_device_hardware_rejected")
    raw = pinned_private_bytes(discovery.get("sourcePath"), discovery.get("sha256"))
    receipt = strict_json(pinned_private_bytes(discovery.get("toolReceiptPath"), discovery.get("toolReceiptSha256")))
    require(type(receipt) is dict, "flutter_device_command_rejected")
    executable = Path(discovery.get("flutterExecutable", ""))
    require(executable.is_absolute() and executable.resolve(strict=True) == executable and executable.is_file() and
        os.access(executable, os.X_OK) and executable == Path(settings["FLUTTER_ROOT"]) / "bin/flutter",
        "flutter_device_tool_rejected")
    command = [str(executable), "devices", "--machine", "--device-connection=attached", "--device-timeout=15"]
    traces = receipt.get("traceSha256")
    require(discovery.get("deviceConnectionInterface") == "attached" and receipt.get("status") == "ok" and
        receipt.get("argumentSha256") == digest(frozen_json(command)) and type(traces) is list and len(traces) == 2 and
        traces[0] == discovery["sha256"], "flutter_device_command_rejected")
    devices = strict_json(raw)
    require(type(devices) is list and len(devices) <= 64 and all(type(row) is dict for row in devices),
        "flutter_device_list_rejected")
    matches = [row for row in devices if row.get("id") == udid]
    require(len(matches) == 1 and matches[0].get("isSupported") is True and matches[0].get("emulator") is False and
        matches[0].get("targetPlatform") == "ios", "flutter_device_match_rejected")


def signing_preflight(path=None, expected_sha=None):
    path, expected_sha = preflight_selection(path, expected_sha)
    raw = read_source(private_path(path))
    require(digest(raw) == expected_sha, "preflight_changed")
    preflight = strict_json(raw)
    require(preflight["mode"] == "profile" and preflight["expectedAppId"] == APP_ID and
        preflight["device"]["model"] == "iPhone 12", "preflight_scope_rejected")
    source = Path(preflight["signingConfig"]["sourcePath"])
    require(source.resolve(strict=True) == source and not source.is_relative_to(ROOT), "signing_path_rejected")
    info = source.lstat()
    require(stat.S_ISREG(info.st_mode) and stat.S_IMODE(info.st_mode) == 0o600 and info.st_uid == os.getuid(),
        "signing_file_rejected")
    data = read_source(source)
    require(digest(data) == preflight["signingConfig"]["sha256"], "signing_changed")
    settings = {}
    for line in data.decode().splitlines():
        if not line.strip() or line.lstrip().startswith("//"):
            continue
        require("=" in line and not line.lstrip().startswith("#"), "signing_config_rejected")
        key, value = (part.strip() for part in line.split("=", 1))
        require(key not in settings and value, "signing_config_rejected")
        settings[key] = value
    required = {"DEVELOPMENT_TEAM", "CODE_SIGN_IDENTITY", "CODE_SIGN_STYLE",
        "IMAGE_EXPERIMENT_APP_PROVISIONING_PROFILE_SPECIFIER", "IMAGE_EXPERIMENT_TEST_PROVISIONING_PROFILE_SPECIFIER",
        "IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT", "IMAGE_EXPERIMENT_SIGNING_CONFIG", "FLUTTER_ROOT", "FLUTTER_BUILD_DIR"}
    require(set(settings) == required and settings["CODE_SIGN_STYLE"] == "Manual" and settings["FLUTTER_BUILD_DIR"] == "build" and
        settings["IMAGE_EXPERIMENT_SIGNING_CONFIG"] == str(source), "signing_config_rejected")
    if path != PREFLIGHT or expected_sha != PREFLIGHT_SHA:
        validate_flutter_device(preflight, settings)
    return preflight, source, settings


def preservation(evidence, signing_source, settings, preflight_path=PREFLIGHT, extra=()):
    paths = {ROOT / name for name in evidence["preservation"]["afterSha256"]}
    paths.update((EVIDENCE, EVIDENCE.with_suffix(".md"), preflight_path, signing_source))
    directories = {ROOT / "be/docs/evidence/png-lossless-compression",
        ROOT / "be/docs/evidence/p4-reading-images", ROOT / evidence["privateRun"]}
    previous = evidence.get("measurementCorrection", {}).get("previousRun")
    if previous:
        directories.add(ROOT / previous)
    directories.update(path for path in (ROOT / "be/var").glob("png-lossless*") if path.is_dir())
    for directory in directories:
        require(directory.is_dir() and not directory.is_symlink(), "preserved_tree_unavailable")
        paths.update(path for path in directory.rglob("*") if path.is_file())
    result = {str(path): file_hash(path) for path in sorted(paths)}
    original_output = Path(settings["IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT"])
    trees = {str(path): tree_hashes(path) for path in extra}
    if PUBLIC.exists():
        trees[str(PUBLIC)] = tree_hashes(PUBLIC)
    return {"files": result, "signingOutputPath": str(original_output),
        "signingOutputFiles": tree_hashes(original_output), "extraTrees": trees}


def assert_preserved(before):
    after = {name: file_hash(Path(name)) for name in before["files"]}
    output = tree_hashes(Path(before["signingOutputPath"]))
    trees = {name: tree_hashes(Path(name)) for name in before.get("extraTrees", {})}
    require(after == before["files"] and output == before["signingOutputFiles"] and
        trees == before.get("extraTrees", {}), "preservation_changed")
    return {"unchanged": True, "fileCount": len(after), "sha256": digest(frozen_json(after)),
        "signingOutputFileCount": len(output), "signingOutputSha256": digest(frozen_json(output)),
        "extraTreeFileCount": sum(len(row) for row in trees.values()), "extraTreesSha256": digest(frozen_json(trees))}


def local_sources():
    sources, _ = source_snapshot()
    for relative in ("tools/image-optimization/local_lossless_mobile.py", "tools/image-optimization/test_local_lossless_mobile.py",
        "_bmad-output/implementation-artifacts/spec-local-lossless-mobile.md"):
        sources[relative] = digest(read_source(ROOT / relative))
    return sources, digest(frozen_json(sources))


def prepare(private_dir, preflight=None, preflight_sha256=None, preserve_private=()):
    """Yalnız yeni private snapshot'ta asset listesi ve signing yolunu hazırlar."""
    preflight_path, expected_sha = preflight_selection(preflight, preflight_sha256)
    private_dir = new_private_directory(private_dir)
    extra = tuple(private_path(path, directory=True) for path in preserve_private)
    require(all(not private_dir.is_relative_to(path) and not path.is_relative_to(private_dir) for path in extra),
        "preservation_scope_rejected")
    preflight, signing_source, settings = signing_preflight(preflight_path, expected_sha)
    evidence, cases = fixtures()
    protected = preservation(evidence, signing_source, settings, preflight_path, extra)
    if "flutterDiscovery" in preflight:
        for name in (preflight["device"]["hardwareEvidencePath"], preflight["flutterDiscovery"]["sourcePath"],
                preflight["flutterDiscovery"]["toolReceiptPath"]):
            protected["files"][name] = file_hash(private_path(Path(name)))
    sources, source_sha = local_sources()
    private_dir.mkdir(mode=0o700)
    output = private_dir / "build-output"
    output.mkdir(mode=0o700)
    write_new(private_dir / "preservation-before.private.json", protected)
    mobile = private_mobile_snapshot(output, sources)
    asset_dir = mobile / "assets/local_lossless"
    asset_dir.mkdir(mode=0o700)
    for row in cases:
        data = read_source(Path(row["sourcePath"]))
        require(digest(data) == row["sha256"], "fixture_changed_during_copy")
        write_new(mobile / row["asset"], data)
    manifest = {"schemaVersion": 1, "evidenceSha256": EVIDENCE_SHA,
        "cases": [{key: value for key, value in row.items() if key != "sourcePath"} for row in cases]}
    write_new(mobile / MANIFEST, manifest)
    pubspec = mobile / "pubspec.yaml"
    original = read_source(pubspec)
    text = original.decode()
    require(text.count("  assets:\n") == 1 and "assets/local_lossless" not in text, "private_pubspec_rejected")
    replacement = text.replace("  assets:\n", "  assets:\n    - assets/local_lossless/\n", 1).encode()
    # Bu dosya yalnız yeni snapshot'a aittir; checkout pubspec'i hiç yazılmaz.
    pubspec.write_bytes(replacement)
    new_settings = dict(settings)
    new_settings.update({"IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT": str(output),
        "IMAGE_EXPERIMENT_SIGNING_CONFIG": str(private_dir / "signing.xcconfig")})
    write_new(private_dir / "signing.xcconfig", "\n".join(f"{key} = {value}" for key, value in new_settings.items()).encode() + b"\n")
    run_nonce = secrets.token_hex(32)
    receipt = {"schemaVersion": 1, "scope": SCOPE, "mode": "profile", "appId": APP_ID, "runNonce": run_nonce,
        "dartTarget": "lib/main_image_experiment.dart", "dartDefine": FLAG, "sourceFiles": sources,
        "sourceSha256": source_sha, "sourceCopyPath": str(mobile), "buildOutputPath": str(output),
        "manifestSha256": file_hash(mobile / MANIFEST), "privatePubspecSha256": digest(replacement),
        "originalPubspecSha256": digest(original), "pubspecLockSha256": file_hash(mobile / "pubspec.lock"),
        "podLockSha256": file_hash(mobile / "ios/Podfile.lock"), "signingConfigSha256": file_hash(private_dir / "signing.xcconfig"),
        "preflightPath": str(preflight_path), "preflightSha256": expected_sha,
        "preflightSigningConfigPath": str(signing_source),
        "preflightSigningConfigSha256": file_hash(signing_source), "fixtureEvidenceSha256": EVIDENCE_SHA,
        "deviceSelector": preflight["device"]["selector"], "physicalVerification": "NOT_RUN"}
    receipt["runtimeEnvironment"] = runtime_environment_binding(private_dir)
    require(local_sources()[1] == source_sha, "source_changed_during_prepare")
    receipt["preservation"] = assert_preserved(protected)
    write_new(private_dir / "prepare-receipt.json", receipt)
    return receipt


def validate_snapshot(receipt):
    if "preflightPath" in receipt:
        preflight, signing, _ = signing_preflight(Path(receipt["preflightPath"]), receipt["preflightSha256"])
        require(str(signing) == receipt["preflightSigningConfigPath"] and
            file_hash(signing) == receipt["preflightSigningConfigSha256"] and
            preflight["device"]["selector"] == receipt["deviceSelector"], "preflight_binding_rejected")
    mobile = Path(receipt["sourceCopyPath"])
    require(local_sources()[1] == receipt["sourceSha256"], "source_changed")
    if "runtimeEnvironment" in receipt:
        require(receipt["runtimeEnvironment"] == runtime_environment_binding(Path(receipt["buildOutputPath"]).parent),
            "runtime_environment_changed")
    expected = {name[len("app/mobile/"):]: value for name, value in receipt["sourceFiles"].items()
        if name.startswith("app/mobile/")}
    expected["pubspec.yaml"] = receipt["privatePubspecSha256"]
    actual = {str(path.relative_to(mobile)): file_hash(path) for path in selected_mobile_sources(mobile)
        if not path.is_relative_to(mobile / "assets/local_lossless")}
    require(actual == expected and file_hash(mobile / MANIFEST) == receipt["manifestSha256"] and
        file_hash(mobile / "pubspec.lock") == receipt["pubspecLockSha256"] and
        file_hash(mobile / "ios/Podfile.lock") == receipt["podLockSha256"], "private_snapshot_changed")
    _, cases = fixtures()
    for row in cases:
        require(file_hash(mobile / row["asset"]) == row["sha256"], "copied_fixture_changed")


def dart_defines(run_nonce):
    require(type(run_nonce) is str and re.fullmatch(r"[0-9a-f]{64}", run_nonce), "run_nonce_rejected")
    return ",".join(base64.b64encode(value.encode()).decode() for value in
        (FLAG, "TELLPAL_LOCAL_LOSSLESS_RUN=" + run_nonce))


def preflight_binding(receipt):
    return digest(frozen_json({key: receipt[key] for key in ("preflightPath", "preflightSha256",
        "preflightSigningConfigPath", "preflightSigningConfigSha256")}))


def build_command(mobile, output, signing, run_nonce):
    return ["xcodebuild", "-quiet", "-workspace", str(mobile / "ios/Runner.xcworkspace"),
        "-scheme", "imageexperiment", "-configuration", "Profile-imageexperiment", "-sdk", "iphoneos", "-jobs", "2",
        "-destination", "generic/platform=iOS", "-derivedDataPath", str(output / "derived-data"),
        "-xcconfig", str(signing), "-disableAutomaticPackageResolution", "-skipPackageUpdates",
        "FLUTTER_APPLICATION_PATH=" + str(mobile), "FLUTTER_TARGET=lib/main_image_experiment.dart",
        "FLUTTER_BUILD_MODE=profile", "DART_DEFINES=" + dart_defines(run_nonce), "build"]


def bounded_tool(argv, output, alias, *, cwd=None, env=None, seconds=180):
    """Özel log, toplam byte ve süreç grubu süresi birlikte sınırlıdır."""
    require(0 < seconds <= 600 and re.fullmatch(r"[a-z][a-z0-9-]{0,63}", alias), "tool_bound_rejected")
    process, total, status, handles = None, 0, "failed", []
    selector = selectors.DefaultSelector()
    started = time.monotonic()
    try:
        for suffix in (".stdout", ".stderr"):
            descriptor = os.open(output / (alias + suffix), os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
            handles.append(os.fdopen(descriptor, "wb"))
        process = subprocess.Popen(argv, cwd=cwd, env=env, stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, start_new_session=True, umask=0o077)
        selector.register(process.stdout, selectors.EVENT_READ, handles[0])
        selector.register(process.stderr, selectors.EVENT_READ, handles[1])
        while selector.get_map() or process.poll() is None:
            if time.monotonic() - started >= seconds:
                status = "timeout"
                break
            for key, _ in selector.select(.05):
                chunk = os.read(key.fileobj.fileno(), 8192)
                if not chunk:
                    selector.unregister(key.fileobj)
                    continue
                require(total + len(chunk) <= 8 * 1024 * 1024, "tool_output_limit")
                key.data.write(chunk)
                total += len(chunk)
        else:
            status = "ok" if process.wait(timeout=2) == 0 else "failed"
    except OSError:
        status = "unavailable"
    finally:
        if process is not None:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait(timeout=5)
            process.stdout.close()
            process.stderr.close()
        for handle in handles:
            handle.close()
        selector.close()
        write_new(output / (alias + ".json"), {"status": status,
            "elapsedMillis": int((time.monotonic() - started) * 1000), "traceBytes": total,
            "argumentSha256": digest(frozen_json(argv)),
            "traceSha256": [file_hash(output / (alias + suffix)) for suffix in (".stdout", ".stderr")],
            "runtimeEnvironmentSha256": digest(frozen_json({key: env[key] for key in RUNTIME_ENV_KEYS
                if env is not None and key in env}))})
    return status


def checked_tool(argv, output, alias, *, cwd=None, offline=False, seconds=180, private_dir=None):
    env = qa_tool_environment(private_dir)
    if offline:
        require(shutil.which("sandbox-exec") is not None, "offline_guard_unavailable")
        argv = ["sandbox-exec", "-p", NETWORK_POLICY, *argv]
    status = bounded_tool(argv, output, alias, seconds=seconds, cwd=cwd, env=env)
    require(status == "ok", "tool_" + status)
    return read_source(output / (alias + ".stdout"))


def build(private_dir):
    private_dir = private_path(private_dir, directory=True)
    receipt = private_json(private_dir / "prepare-receipt.json")
    validate_snapshot(receipt)
    protected = private_json(private_dir / "preservation-before.private.json", 20 * 1024 * 1024)
    output, mobile = Path(receipt["buildOutputPath"]), Path(receipt["sourceCopyPath"])
    signing = private_dir / "signing.xcconfig"
    require(file_hash(signing) == receipt["signingConfigSha256"], "private_signing_changed")
    require(not (private_dir / "build-attempt.json").exists(), "build_attempt_exists")
    write_new(private_dir / "build-attempt.json", {"startedAtUtc": datetime.now(timezone.utc).isoformat(),
        "networkPolicy": NETWORK_POLICY, "status": "STARTED"})
    try:
        checked_tool(["flutter", "pub", "get", "--offline"], output, "flutter-packages", cwd=mobile, offline=True, private_dir=private_dir)
        checked_tool(["flutter", "build", "ios", "--config-only", "--no-pub", "--no-codesign", "--profile",
            "--flavor", "imageexperiment", "--target", "lib/main_image_experiment.dart", "--dart-define=" + FLAG,
            "--dart-define=TELLPAL_LOCAL_LOSSLESS_RUN=" + receipt["runNonce"]],
            output, "flutter-config", cwd=mobile, offline=True, private_dir=private_dir)
        checked_tool(["pod", "install", "--deployment", "--no-repo-update"], output, "pods", cwd=mobile / "ios", offline=True, private_dir=private_dir)
        validate_snapshot(receipt)
        checked_tool(build_command(mobile, output, signing, receipt["runNonce"]), output, "xcode-build", cwd=mobile / "ios", offline=True, seconds=600, private_dir=private_dir)
        app = output / "derived-data/Build/Products/Profile-imageexperiment-iphoneos/ImageExperiment.app"
        require(app.is_dir(), "artifact_unavailable")
        info = plistlib.loads(read_source(app / "Info.plist"))
        require(info.get("CFBundleIdentifier") == APP_ID and info.get("DTPlatformName") == "iphoneos" and
            info.get("CFBundleSupportedPlatforms") == ["iPhoneOS"], "artifact_identity_rejected")
        checked_tool(["codesign", "--verify", "--strict", str(app)], output, "codesign-verify", offline=True, private_dir=private_dir)
        version = strict_json(checked_tool(["flutter", "--version", "--machine"], output, "flutter-version", offline=True, private_dir=private_dir))
        validate_snapshot(receipt)
        result = {"schemaVersion": 1, "scope": SCOPE, "mode": "profile", "appId": APP_ID, "runNonce": receipt["runNonce"],
            "runtimeEnvironmentSha256": digest(frozen_json(receipt["runtimeEnvironment"])),
            "compiledDartDefinesSha256": digest(dart_defines(receipt["runNonce"]).encode()),
            "artifactPath": str(app), "artifactFiles": tree_hashes(app), "sourceSha256": receipt["sourceSha256"],
            "manifestSha256": receipt["manifestSha256"], "signingConfigSha256": receipt["signingConfigSha256"],
            "preflightBindingSha256": preflight_binding(receipt),
            "flutterVersion": version.get("frameworkVersion"), "engineRevision": version.get("engineRevision"),
            "offlineBuild": True, "physicalVerification": "NOT_RUN", "preservation": assert_preserved(protected)}
        write_new(private_dir / "build-receipt.json", result)
        return result
    except Exception:
        write_new(private_dir / "build-failure.json", {"status": "NOT_RUN", "reason": "BUILD_FAILED",
            "preservation": assert_preserved(protected)})
        raise


def physical_device(private_dir, selector, stage):
    require(stage in ("run", "verify"), "device_stage_rejected")
    output = private_dir / "build-output"
    path = private_dir / (stage + "-device-evidence.private.json")
    require(not path.exists(), "device_evidence_exists")
    checked_tool(["xcrun", "devicectl", "device", "info", "details", "--device", selector,
        "--json-output", str(path)], output, stage + "-device-info", private_dir=private_dir)
    os.chmod(path, 0o600)
    raw = private_json(path)
    result = raw.get("result", {})
    hardware, properties = result.get("hardwareProperties", {}), result.get("deviceProperties", {})
    require(hardware.get("reality") == "physical" and hardware.get("platform") == "iOS" and
        hardware.get("marketingName") == "iPhone 12" and properties.get("developerModeStatus") == "enabled",
        "physical_device_unverified")
    transport = result.get("connectionProperties", {}).get("transportType")
    require(transport == "wired", "wired_device_required")
    return {"physicalDevice": True, "model": hardware["marketingName"],
        "osVersion": properties.get("osVersionNumber"),
        "transportType": transport,
        "evidenceSha256": file_hash(path)}


def validate_build(prepared, built, private_dir):
    expected_app = Path(prepared["buildOutputPath"]) / "derived-data/Build/Products/Profile-imageexperiment-iphoneos/ImageExperiment.app"
    require(built.get("scope") == SCOPE and built.get("mode") == "profile" and built.get("appId") == APP_ID and
        built.get("sourceSha256") == prepared["sourceSha256"] and built.get("runNonce") == prepared["runNonce"] and
        built.get("manifestSha256") == prepared["manifestSha256"] and built.get("offlineBuild") is True and
        built.get("compiledDartDefinesSha256") == digest(dart_defines(prepared["runNonce"]).encode()) and
        built.get("artifactPath") == str(expected_app) and expected_app.is_relative_to(private_dir / "build-output") and
        built.get("artifactFiles") == tree_hashes(expected_app), "build_binding_rejected")
    dart_defines(prepared["runNonce"])
    if "runtimeEnvironment" in prepared:
        require(built.get("runtimeEnvironmentSha256") == digest(frozen_json(prepared["runtimeEnvironment"])),
            "runtime_environment_changed")
    if "preflightPath" in prepared:
        require(built.get("preflightBindingSha256") == preflight_binding(prepared) and
            built.get("signingConfigSha256") == prepared["signingConfigSha256"], "preflight_build_binding_rejected")
    return digest(frozen_json(built["artifactFiles"]))


def launch_command(prepared, built):
    return ["flutter", "drive", "--profile", "--no-pub", "--device-connection=attached", "--no-publish-port",
        "--flavor", "imageexperiment",
        "--target", "lib/main_image_experiment.dart", "--driver", "test_driver/local_lossless_driver.dart",
        "--use-application-binary=" + built["artifactPath"], "--device-id", prepared["deviceSelector"],
        "--no-keep-app-running"]


def run(private_dir):
    """Aynı private build'i, aynı fiziksel selector'da tek sonlu driver ile açar."""
    private_dir = private_path(private_dir, directory=True)
    prepared = private_json(private_dir / "prepare-receipt.json")
    built = private_json(private_dir / "build-receipt.json")
    validate_snapshot(prepared)
    artifact_sha = validate_build(prepared, built, private_dir)
    protected = private_json(private_dir / "preservation-before.private.json", 20 * 1024 * 1024)
    assert_preserved(protected)
    require(not (private_dir / "launch-attempt.json").exists() and not (private_dir / "device-run").exists(),
        "launch_attempt_exists")
    device = physical_device(private_dir, prepared["deviceSelector"], "run")
    argv = launch_command(prepared, built)
    output = private_dir / "device-run"
    env = qa_tool_environment(private_dir)
    env.update({"CI": "true", "FLUTTER_SUPPRESS_ANALYTICS": "true", "DART_SUPPRESS_ANALYTICS": "true",
        "COCOAPODS_DISABLE_STATS": "true", "LOCAL_LOSSLESS_RUN_NONCE": prepared["runNonce"],
        "LOCAL_LOSSLESS_EVIDENCE_DIR": str(output)})
    binding = {"schemaVersion": 1, "scope": SCOPE, "runNonce": prepared["runNonce"], "mode": "profile", "appId": APP_ID,
        "sourceSha256": prepared["sourceSha256"], "deviceSelector": prepared["deviceSelector"],
        "sourceCopyPath": prepared["sourceCopyPath"], "artifactPath": built["artifactPath"],
        "artifactSha256": artifact_sha, "argumentSha256": digest(frozen_json(argv)),
        "buildReceiptSha256": file_hash(private_dir / "build-receipt.json"), "device": device}
    if "runtimeEnvironment" in prepared:
        binding["runtimeEnvironmentSha256"] = digest(frozen_json(prepared["runtimeEnvironment"]))
    write_new(private_dir / "launch-attempt.json", {**binding, "status": "STARTED"})
    status = bounded_tool(argv, private_dir / "build-output", "flutter-drive", cwd=prepared["sourceCopyPath"], env=env, seconds=600)
    launch = {**binding, "toolStatus": status, "status": "FAIL"}
    try:
        require(status == "ok", "driver_launch_failed")
        validate_snapshot(prepared)
        validate_build(prepared, built, private_dir)
        driver_result = output / "evidence.json"
        evidence = private_json(driver_result)
        _, cases = fixtures()
        validate_driver(evidence, cases, output, prepared["runNonce"])
        vm_sha = evidence.get("vmServiceUrlSha256")
        require(type(vm_sha) is str and re.fullmatch(r"[0-9a-f]{64}", vm_sha) and
            evidence.get("vmServiceUrlSource") == "flutter_drive_environment", "vm_endpoint_binding_rejected")
        launch.update({"status": "PASS", "driverResultPath": str(driver_result),
            "driverResultSha256": file_hash(driver_result), "vmServiceUrlSha256": vm_sha,
            "vmServiceUrlSource": "flutter_drive_environment",
            "launchAttemptSha256": file_hash(private_dir / "launch-attempt.json"),
            "toolReceiptSha256": file_hash(private_dir / "build-output/flutter-drive.json")})
    except Exception:
        launch["reason"] = "DRIVER_FAILED"
    finally:
        launch["preservation"] = assert_preserved(protected)
        write_new(private_dir / "launch-receipt.json", launch)
    require(launch["status"] == "PASS", "driver_launch_failed")
    return launch


def validate_launch(prepared, built, launch, result, result_path, private_dir):
    artifact_sha = validate_build(prepared, built, private_dir)
    expected_result = private_dir / "device-run/evidence.json"
    expected_command = launch_command(prepared, built)
    tool_receipt = private_json(private_dir / "build-output/flutter-drive.json")
    if "runtimeEnvironment" in prepared:
        expected_runtime = digest(frozen_json(prepared["runtimeEnvironment"]))
        require(launch.get("runtimeEnvironmentSha256") == expected_runtime and
            tool_receipt.get("runtimeEnvironmentSha256") == expected_runtime, "runtime_environment_changed")
    require(launch.get("status") == "PASS" and launch.get("toolStatus") == "ok" and launch.get("scope") == SCOPE and
        launch.get("mode") == "profile" and launch.get("appId") == APP_ID and
        launch.get("runNonce") == prepared["runNonce"] and result.get("runNonce") == prepared["runNonce"] and
        launch.get("sourceSha256") == prepared["sourceSha256"] and launch.get("sourceCopyPath") == prepared["sourceCopyPath"] and
        launch.get("deviceSelector") == prepared["deviceSelector"] and launch.get("artifactPath") == built["artifactPath"] and
        launch.get("artifactSha256") == artifact_sha and launch.get("argumentSha256") == digest(frozen_json(expected_command)) and
        launch.get("buildReceiptSha256") == file_hash(private_dir / "build-receipt.json") and result_path == expected_result and
        launch.get("launchAttemptSha256") == file_hash(private_dir / "launch-attempt.json") and
        launch.get("toolReceiptSha256") == file_hash(private_dir / "build-output/flutter-drive.json") and
        tool_receipt.get("status") == "ok" and tool_receipt.get("argumentSha256") == digest(frozen_json(expected_command)) and
        launch.get("driverResultPath") == str(expected_result) and launch.get("driverResultSha256") == file_hash(result_path) and
        type(launch.get("vmServiceUrlSha256")) is str and re.fullmatch(r"[0-9a-f]{64}", launch["vmServiceUrlSha256"]) and
        launch.get("vmServiceUrlSha256") == result.get("vmServiceUrlSha256") and
        launch.get("vmServiceUrlSource") == result.get("vmServiceUrlSource") == "flutter_drive_environment", "launch_binding_rejected")
    return artifact_sha


def number(value):
    return type(value) in (int, float) and math.isfinite(value)


def rss_sample(value):
    require(type(value) is dict and type(value.get("available")) is bool and
        (set(value) == {"available", "bytes"} and type(value["bytes"]) is int and value["bytes"] > 0
        if value["available"] else set(value) == {"available"}), "rss_evidence_rejected")
    return dict(value)


def validate_driver(result, cases, directory, run_nonce):
    dart_defines(run_nonce)
    initial = result.get("initial", {})
    require(result.get("status") == "PASS" and result.get("passed") is True and initial.get("profileMode") is True and
        initial.get("enabled") is True and initial.get("ready") is True and initial.get("stopped") is False and
        initial.get("completedCases") == 0 and initial.get("cases") == [] and
        all(type(initial.get(key)) is int and initial[key] == 0 for key in COUNTERS), "driver_mode_rejected")
    require(result.get("runNonce") == run_nonce and initial.get("runNonce") == run_nonce, "runtime_nonce_rejected")
    observations = result.get("observations")
    require(type(observations) is list and len(observations) == 6, "six_cases_required")
    screens = result.get("captures")
    require(type(screens) is list and len(screens) == 6, "six_screenshots_required")
    stopped = result.get("stopped", {})
    measured = stopped.get("cases")
    require(type(measured) is list and len(measured) == 6, "six_cases_required")
    screenshots, safe_cases = [], []
    for index, (row, pin, screen, observation) in enumerate(zip(measured, cases, screens, observations)):
        snapshot = observation.get("painted", {})
        require(observation.get("caseIndex") == index and snapshot.get("enabled") is True and
            snapshot.get("profileMode") is True and snapshot.get("runNonce") == run_nonce and
            snapshot.get("stopped") is False and snapshot.get("status") == "loaded" and
            snapshot.get("caseIndex") == index and type(snapshot.get("epoch")) is int and snapshot["epoch"] > 0 and
            snapshot.get("paintedEpoch") == snapshot["epoch"] and snapshot.get("completedCases") == index + 1 and
            snapshot.get("cases") == measured[:index + 1] and snapshot.get("activeImages") == 1 and
            all(type(snapshot.get(key)) is int and snapshot[key] == 0 for key in COUNTERS[:3]), "paint_sequence_rejected")
        require(type(row) is dict and all(row.get(key) == pin[key] for key in ("caseId", "assetId", "format", "asset")) and
            row.get("encodedBytes") == pin["bytes"] and row.get("encodedSha256") == pin["sha256"] and
            row.get("decodedWidth") == WIDTH and row.get("decodedHeight") == HEIGHT and
            row.get("rgbBytes") == RGB_BYTES and row.get("rgbSha256") == pin["rgbSha256"] and
            row.get("rgbMatchesPin") is True and row.get("paintCompleted") is True, "case_evidence_rejected")
        widget = row.get("widget", {})
        require(all(number(widget.get(key)) and widget[key] > 0 for key in
            ("logicalWidth", "logicalHeight", "dpr", "physicalWidth", "physicalHeight")) and
            abs(widget["physicalWidth"] - WIDTH) < 1e-5 and abs(widget["physicalHeight"] - HEIGHT) < 1e-5 and
            abs(widget["logicalWidth"] * widget["dpr"] - WIDTH) < 1e-5 and
            abs(widget["logicalHeight"] * widget["dpr"] - HEIGHT) < 1e-5 and widget.get("pixelAligned") is True and
            all(number(widget.get(key)) and abs(widget[key] - round(widget[key])) < 1e-5
                for key in ("physicalLeft", "physicalTop")) and
            all(number(widget.get(logical)) and abs(widget[logical] * widget["dpr"] - widget[physical]) < 1e-5
                for logical, physical in (("logicalLeft", "physicalLeft"), ("logicalTop", "physicalTop"))),
            "display_geometry_rejected")
        require(widget.get("fit") == "contain", "display_fit_rejected")
        for field in ("assetReadMicros", "checksumMicros", "decodeMicros", "rgbReadbackMicros"):
            require(type(row.get(field)) is int and row[field] >= 0, "timing_evidence_rejected")
        for field in ("rssBefore", "rssLoaded"):
            rss_sample(row.get(field))
        require(screen.get("caseIndex") == index and screen.get("afterPaintedEpoch") == snapshot["paintedEpoch"],
            "screenshot_order_rejected")
        name = screen.get("screenshotName")
        require(type(name) is str and name == f"{index + 1}-{pin['caseId']}.png", "screenshot_path_rejected")
        path = directory / name
        data = read_source(private_path(path))
        require(24 <= len(data) <= 16 * 1024 * 1024 and data.startswith(b"\x89PNG\r\n\x1a\n") and
            len(data) == screen.get("byteSize") and digest(data) == screen.get("sha256"), "screenshot_changed")
        screenshots.append({"caseId": pin["caseId"], "path": name, "sha256": digest(data)})
        allowed = ("caseId", "assetId", "format", "asset", "encodedBytes", "encodedSha256", "decodedWidth",
            "decodedHeight", "rgbBytes", "rgbSha256", "rgbMatchesPin", "assetReadMicros", "checksumMicros",
            "decodeMicros", "rgbReadbackMicros", "paintCompleted", "widget", "rssBefore", "rssLoaded")
        safe = {key: row.get(key) for key in allowed}
        safe["widget"] = {key: widget[key] for key in ("logicalWidth", "logicalHeight", "dpr", "physicalWidth",
            "physicalHeight", "logicalLeft", "logicalTop", "physicalLeft", "physicalTop", "pixelAligned", "fit")}
        safe_cases.append(safe)
    require(stopped.get("ok") is True and stopped.get("stopped") is True and stopped.get("runNonce") == run_nonce and
        stopped.get("allCasesPassed") is True and
        stopped.get("completedCases") == 6 and
        all(type(stopped.get(key)) is int and stopped[key] == 0 for key in COUNTERS), "stop_resources_retained")
    rss_sample(stopped.get("rssStop"))
    return {"cases": safe_cases, "screenshots": screenshots,
        "stopped": {key: stopped.get(key) for key in ("stopped", *COUNTERS, "rssStop")}}


def public_destination(public=None):
    """Yeni deneme yalnız kanıt kökünün attempts altına yazılabilir."""
    selected = PUBLIC if public is None else Path(public)
    require(selected.is_absolute() and selected.resolve(strict=False) == selected and
        PUBLIC.resolve(strict=False) == PUBLIC, "public_path_rejected")
    if selected != PUBLIC:
        require(selected.is_relative_to(PUBLIC), "public_path_rejected")
        relative = selected.relative_to(PUBLIC).parts
        require(len(relative) == 2 and relative[0] == "attempts" and
            re.fullmatch(r"[a-z0-9][a-z0-9-]{0,95}", relative[1]) and
            PUBLIC.is_dir() and not PUBLIC.is_symlink(), "public_path_rejected")
        require(not selected.parent.exists() or selected.parent.is_dir() and
            not selected.parent.is_symlink(), "public_path_rejected")
    require(not selected.exists() and not selected.is_symlink(), "public_evidence_exists")
    require(selected.parent.is_dir() or selected.parent == PUBLIC / "attempts", "public_path_rejected")
    return selected


def private_report_destination(path):
    """Yeni rapor yalnız onaylı dış kökteki bir run'ın report dizinidir."""
    selected = new_private_directory(path)
    require(selected.name == "report" and selected.parent != EXTERNAL_QA_ROOT, "private_report_path_rejected")
    return selected


def publish(report, screenshots=(), screenshot_directory=None, public=None, private_report=None):
    """Başarı ve hata kanıtları yalnız yeni dizine exclusive yazılır."""
    require(private_report is None or public is None, "report_destination_conflict")
    if private_report is None:
        public = public_destination(public)
        if public.parent == PUBLIC / "attempts" and not public.parent.exists():
            public.parent.mkdir(mode=0o755)
        public = public_destination(public)
    else:
        public = private_report_destination(private_report)
    public.mkdir(mode=0o700 if private_report is not None else 0o755)
    for screen in screenshots:
        data = read_source(screenshot_directory / screen["path"])
        require(digest(data) == screen["sha256"], "screenshot_changed_during_publish")
        write_new(public / screen["path"], data)
    write_new(public / "verification.json", report)
    status = report["status"]
    lines = ["# Fiziksel iPhone yerel kayıpsız format karşılaştırması", "", f"Sonuç: **{status}**.", ""]
    lines += ["Plan: her asset ve format için bir seri örnek, toplam altı durum. "
        f"Kabul edilen runtime ölçümü: **{report.get('acceptedCases', 0)}/6 durum**; "
        f"kabul edilen screenshot: **{report.get('acceptedScreenshots', 0)}/6**. "
        "Planlanan örnek sayısı ölçümün tamamlandığını göstermez.", ""]
    if status == "PASS":
        lines += ["Fiziksel iPhone 12 üzerinde ayrı profile paketinde altı sabit durum tamamlandı. Her durumda 822×1334 tam decode, tüm 3.289.644 RGB byte'ın pin eşitliği, 1:1 fiziksel piksel gösterimi, paint ve ekran kaydı doğrulandı.", "",
            "| Durum | Asset okuma µs | SHA µs | Decode µs | RGB readback µs |", "|---|---:|---:|---:|---:|"]
        for row in report["cases"]:
            lines.append(f"| {row['caseId']} | {row['assetReadMicros']} | {row['checksumMicros']} | {row['decodeMicros']} | {row['rgbReadbackMicros']} |")
        lines += ["", "Stop sonrası owned buffer/descriptor/codec/image sayaçları sıfırdır.", ""]
    else:
        lines += ["Fiziksel cihaz kabulü tamamlanmadı. Gerekçe: `" + report["reason"] + "`. Özel deneme ve hata kayıtları korundu.", ""]
    lines += ["Decode süresine RGB readback veya screenshot dahil değildir. RGB readback süresi native readback, alpha tarama, RGB kopyası ve SHA doğrulamasının toplamıdır. Plan her format için tek seri örnekle sınırlıdır; format hızı genellenemez. RSS değerleri anlık süreç örnekleridir; peak, GPU belleği veya phys_footprint değildir. Loaded RSS, RGB readback ve checksum sonrasıdır; doğrulama ek yükünü içerir. Saf decoder veya RGBA resident farkı çıkarılamaz. Teorik RGBA tutarı görsel başına 4.386.192 byte'tır. Renderer gözlenmedi (`unobserved`).", "",
        "HTTP transferi ölçülmedi. Gerçek OS background testi yapılmadı. Tam genişlikte reader uygunluğu ve insan görsel kalite kabulü ayrıca değerlendirilmelidir. P0/P4b ve üretim kapıları açıktır.", ""]
    write_new(public / "verification.md", "\n".join(lines).encode())
    return report


def verify(private_dir, driver_result=None, not_run_reason=None, public=None, private_report=None):
    require(private_report is None or public is None, "report_destination_conflict")
    private_dir = private_path(private_dir, directory=True)
    if private_report is None:
        public = public_destination(public)
    else:
        private_report = private_report_destination(private_report)
        require(private_report.parent == private_dir, "private_report_binding_rejected")
    prepared = private_json(private_dir / "prepare-receipt.json")
    report = {"schemaVersion": 1, "scope": SCOPE, "observedAtUtc": datetime.now(timezone.utc).isoformat(),
        "status": "NOT_RUN", "reason": not_run_reason or "DRIVER_NOT_RUN", "mode": "profile", "bundleId": APP_ID,
        "fixtureEvidenceSha256": EVIDENCE_SHA, "sourceSha256": prepared["sourceSha256"],
        "pubspecLockSha256": prepared["pubspecLockSha256"], "podLockSha256": prepared["podLockSha256"],
        "theoreticalRgbaBytes": RGBA_BYTES, "rssKind": "INSTANTANEOUS_PROCESS_RSS_NOT_PEAK_OR_GPU",
        "rssLoadedScope": "after_rgb_readback_and_checksum_includes_validation_overhead",
        "plannedSamplesPerFormatPerAsset": 1, "acceptedSamplesPerFormatPerAsset": 0,
        "acceptedCases": 0, "acceptedScreenshots": 0,
        "rgbReadbackScope": "native_readback_alpha_scan_rgb_copy_and_sha_validation", "renderer": "unobserved",
        "httpTransferMeasured": False, "realOsBackgroundTested": False, "humanQualityAccepted": False,
        "fullWidthReaderAccepted": False, "productionGate": "OPEN", "p0Gate": "OPEN", "p4bGate": "OPEN"}
    report["codeVerification"] = "SEE_TASK_TEST_RESULTS"
    screenshots, screenshot_directory = [], None
    try:
        validate_snapshot(prepared)
        protected = private_json(private_dir / "preservation-before.private.json", 20 * 1024 * 1024)
        report["preservation"] = assert_preserved(protected)
        if driver_result is not None:
            report["status"] = "FAIL"
            build_result = private_json(private_dir / "build-receipt.json")
            _, cases = fixtures()
            driver_result = private_path(driver_result)
            result = private_json(driver_result)
            launch = private_json(private_dir / "launch-receipt.json")
            artifact_sha = validate_launch(prepared, build_result, launch, result, driver_result, private_dir)
            checked = validate_driver(result, cases, driver_result.parent, prepared["runNonce"])
            report["device"] = physical_device(private_dir, prepared["deviceSelector"], "verify")
            screenshots, screenshot_directory = checked["screenshots"], driver_result.parent
            report.update(checked)
            report.update({"status": "PASS", "reason": None, "driverResultSha256": file_hash(driver_result),
                "artifactSha256": artifact_sha, "runNonceSha256": digest(prepared["runNonce"].encode()),
                "launchReceiptSha256": file_hash(private_dir / "launch-receipt.json"),
                "vmServiceUrlSha256": launch["vmServiceUrlSha256"],
                "engineRevision": build_result["engineRevision"], "flutterVersion": build_result["flutterVersion"],
                "acceptedSamplesPerFormatPerAsset": 1, "acceptedCases": len(checked["cases"]),
                "acceptedScreenshots": len(screenshots)})
        else:
            require(report["reason"] in SAFE_REASONS, "reason_rejected")
            if (private_dir / "launch-attempt.json").exists():
                report.update({"status": "FAIL", "reason": "DRIVER_FAILED"})
    except Exception:
        report.update({"status": "FAIL", "reason": "VERIFICATION_REJECTED"})
        screenshots = []
    if private_report is not None:
        return publish(report, screenshots, screenshot_directory, private_report=private_report)
    return publish(report, screenshots, screenshot_directory, public=public)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--prepare", action="store_true")
    modes.add_argument("--build", action="store_true")
    modes.add_argument("--run", action="store_true")
    modes.add_argument("--verify", action="store_true")
    parser.add_argument("--private-dir", type=Path, required=True)
    parser.add_argument("--driver-result", type=Path)
    parser.add_argument("--not-run-reason", choices=sorted(SAFE_REASONS))
    parser.add_argument("--public-dir", type=Path)
    parser.add_argument("--preflight", type=Path)
    parser.add_argument("--preflight-sha256")
    parser.add_argument("--preserve-private-dir", type=Path, action="append", default=[])
    parser.add_argument("--report-dir", type=Path)
    args = parser.parse_args(argv)
    try:
        require(args.public_dir is None or args.verify, "public_dir_requires_verify")
        require(args.report_dir is None or args.verify, "report_dir_requires_verify")
        require(args.public_dir is None or args.report_dir is None, "report_destination_conflict")
        require(args.prepare or (args.preflight is None and args.preflight_sha256 is None and
            not args.preserve_private_dir), "prepare_arguments_rejected")
        require(args.verify or (args.driver_result is None and args.not_run_reason is None), "verify_arguments_rejected")
        if args.prepare:
            preflight_selection(args.preflight, args.preflight_sha256)
            prepare(args.private_dir, args.preflight, args.preflight_sha256, args.preserve_private_dir)
            print("Özel snapshot hazır; fiziksel cihaz doğrulaması NOT_RUN.")
        elif args.build:
            build(args.private_dir)
            print("Ağsız ayrı profile paket hazır; fiziksel cihaz doğrulaması NOT_RUN.")
        elif args.run:
            run(args.private_dir)
            print("Aynı profile paket ve fiziksel cihazda altı durum doğrulandı; private launch kanıtı hazır.")
        else:
            result = verify(args.private_dir, args.driver_result, args.not_run_reason, public=args.public_dir,
                private_report=args.report_dir)
            print("Fiziksel cihaz doğrulaması: " + result["status"])
            return 0 if result["status"] == "PASS" else 2
        return 0
    except Exception:
        print("İşlem reddedildi; özel kanıtlar korundu. Public kayıt otomatik oluşturulmadı.")
        return 2


if __name__ == "__main__":
    sys.exit(main())
