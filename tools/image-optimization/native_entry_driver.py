#!/usr/bin/env python3
"""Finite entry validation/build/verification; no reservation or measurement batches."""
import argparse
import base64
from hashlib import sha256
import json
import os
from pathlib import Path
import re
import resource
import selectors
import shutil
import signal
import stat
import subprocess
import sys
import threading
import time

from native_entry_config import (EntryError, HASH, ROOT, SCOPE, MAX_BACKEND_RECEIPT_BYTES,
    MAX_BACKEND_SOURCE_FILES, canonical_uid, exact_keys,
    frozen_json, load_config, load_manifest, no_symlink, open_nofollow, origin, private_file, private_output,
    require, strict_json, write_private)
from native_entry_crypto import certificate_identity, public_key
from native_entry_android_plugins import (inspect_android_plugins, generate_android_registrant,
    generated_source, SOURCE as ANDROID_REGISTRANT_SOURCE)
from native_entry_destination import destination_run, FIXTURE_TEST
from native_fixture_server import Broker, FirebaseVerifier, create_server

TOTAL_SECONDS = 180
CLEANUP_SECONDS = 15
MAX_TRACE_BYTES = 1024 * 1024
MOBILE = ROOT / "app/mobile"
MAX_SOURCE_FILES = 4096
MAX_SOURCE_BYTES = 512 * 1024 * 1024
MAX_SOURCE_FILE_BYTES = 64 * 1024 * 1024
SOURCE_SECONDS = 10
SOURCE_IGNORED_DIRECTORIES = frozenset(("build", ".dart_tool", ".gradle", "Pods", ".symlinks", "ephemeral",
    ".git", ".idea", "__pycache__", "coverage", ".pub-cache", ".pub", ".cxx", "DerivedData", "xcuserdata",
    ".generated", "Flutter.framework", "App.framework", "flutter_assets", ".swiftpm", ".build", ".buildlog",
    ".history", ".svn", ".atom", "migrate_working_dir", "captures"))
SOURCE_IGNORED_FILES = frozenset((".DS_Store", "Generated.xcconfig", "flutter_export_environment.sh",
    ".flutter-plugins-dependencies", ".last_build_id", "local.properties", "key.properties", "google-services.json",
    "GoogleService-Info.plist", "ServiceDefinitions.json", "Flutter.podspec", "app.flx", "app.zip",
    "GeneratedPluginRegistrant.java", "GeneratedPluginRegistrant.h", "GeneratedPluginRegistrant.m"))
SOURCE_IGNORED_SUFFIXES = frozenset((".pyc", ".class", ".log", ".swp", ".iml", ".ipr", ".iws",
    ".keystore", ".jks", ".p12", ".p8", ".mobileprovision", ".pbxuser", ".perspectivev3", ".mode1v3", ".mode2v3"))
SOURCE_IGNORED_RELATIVE_DIRECTORIES = frozenset(("android/app/debug", "android/app/profile", "android/app/release",
    "ios/profile", "doc/api"))


def tool_environment():
    # Explicitly exclude inherited account/token/service credentials from native tools.
    keys = ("PATH", "HOME", "TMPDIR", "LANG", "LC_ALL", "JAVA_HOME", "ANDROID_HOME", "ANDROID_SDK_ROOT")
    return {name: os.environ[name] for name in keys if name in os.environ}


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        self.exit(2, "cli_rejected\n")


def run_bounded(argv, output, alias, *, seconds=180, max_bytes=MAX_TRACE_BYTES,
                cwd=None, env=None, on_line=None, side_file=None, side_limit=0):
    """Argument arrays, capped private traces, and cleanup of the owned process group."""
    require(type(argv) is list and argv and all(type(item) is str and "\x00" not in item for item in argv), "command_rejected")
    require(re.fullmatch(r"[a-z][a-z0-9-]{0,63}", alias) and 0 < seconds <= TOTAL_SECONDS and
        type(max_bytes) is int and 1 <= max_bytes <= MAX_TRACE_BYTES, "command_bound_rejected")
    paths = [Path(output) / (alias + suffix) for suffix in (".stdout", ".stderr")]
    handles, process = [], None
    selector = selectors.DefaultSelector()
    started = time.monotonic()
    status, exit_code, total = "failed", None, 0
    pending = b""
    try:
        for path in paths:
            handles.append(os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600), "wb"))
        def child_limits():
            os.umask(0o077)
            if side_file is not None:
                resource.setrlimit(resource.RLIMIT_FSIZE, (side_limit, side_limit))
        process = subprocess.Popen(argv, cwd=cwd, env=env if env is not None else tool_environment(), stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, start_new_session=True, preexec_fn=child_limits)
        selector.register(process.stdout, selectors.EVENT_READ, (handles[0], True))
        selector.register(process.stderr, selectors.EVENT_READ, (handles[1], False))
        while selector.get_map() or process.poll() is None:
            if on_line is not None and hasattr(on_line, "poll"):
                on_line.poll()
            if time.monotonic() - started >= seconds:
                status = "timeout"
                break
            for key, _ in selector.select(min(0.05, max(0, seconds - (time.monotonic() - started)))):
                chunk = os.read(key.fileobj.fileno(), 8192)
                if not chunk:
                    selector.unregister(key.fileobj)
                    continue
                retained = chunk[:max(0, max_bytes - total)]
                key.data[0].write(retained)
                total += len(retained)
                if len(retained) != len(chunk):
                    status = "output_limit"
                    break
                if on_line is not None and key.data[1]:
                    pending += retained
                    require(len(pending) <= 65536, "launcher_line_rejected")
                    while b"\n" in pending:
                        line, pending = pending.split(b"\n", 1)
                        # Remove only the CR belonging to this consumed CRLF delimiter.
                        if line.endswith(b"\r"):
                            line = line[:-1]
                        on_line(line)
            if status == "output_limit":
                break
        else:
            status = "ok" if process.poll() == 0 else "failed"
        if status in ("timeout", "output_limit"):
            os.killpg(process.pid, signal.SIGKILL)
        exit_code = process.wait(timeout=2)
        if status not in ("timeout", "output_limit"):
            status = "ok" if exit_code == 0 else "failed"
        if side_file is not None and Path(side_file).exists():
            require(Path(side_file).stat().st_size <= side_limit, "side_output_limit")
            os.chmod(side_file, 0o600)
    except FileNotFoundError:
        status = "missing_executable"
    except EntryError:
        raise
    except (OSError, subprocess.TimeoutExpired):
        status = "failed"
    finally:
        cleanup_started = time.monotonic()
        if process is not None:
            # A tool can exit while leaving descendants or a pipe holder alive.
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=min(2, CLEANUP_SECONDS - (time.monotonic() - cleanup_started)))
            except subprocess.TimeoutExpired:
                pass
            for stream in (process.stdout, process.stderr):
                stream.close()
        for handle in handles:
            handle.close()
        selector.close()
    result = {"status": status, "exitCode": exit_code, "elapsedMillis": int((time.monotonic() - started) * 1000),
        "argumentSha256": sha256(frozen_json(argv)).hexdigest(), "traceBytes": total,
        "traceSha256": [sha256(path.read_bytes()).hexdigest() for path in paths]}
    write_private(Path(output) / (alias + ".json"), result)
    return result


def checked_run(argv, output, alias, *, deadline, **kwargs):
    remaining = deadline - time.monotonic()
    require(remaining > 0, "driver_expired")
    result = run_bounded(argv, output, alias, seconds=min(remaining, TOTAL_SECONDS), **kwargs)
    require(result["status"] == "ok", "tool_" + result["status"])
    return (Path(output) / (alias + ".stdout")).read_bytes()


def capture_observation(argv, *, seconds=10, max_bytes=16384):
    """Bound private process observations in memory; command lines are never logged."""
    process = None
    selector = selectors.DefaultSelector()
    retained = bytearray()
    started = time.monotonic()
    try:
        process = subprocess.Popen(argv, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, env=tool_environment(), start_new_session=True)
        selector.register(process.stdout, selectors.EVENT_READ)
        while selector.get_map() or process.poll() is None:
            require(time.monotonic() - started < seconds, "backend_observation_unavailable")
            for key, _ in selector.select(0.05):
                data = os.read(key.fileobj.fileno(), min(8192, max_bytes - len(retained) + 1))
                if not data:
                    selector.unregister(key.fileobj)
                    continue
                retained.extend(data)
                require(len(retained) <= max_bytes, "backend_observation_rejected")
        require(process.wait(timeout=1) == 0, "backend_observation_unavailable")
        return bytes(retained)
    except EntryError:
        raise
    except (OSError, subprocess.SubprocessError):
        raise EntryError("backend_observation_unavailable") from None
    finally:
        if process is not None:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            try:
                process.wait(timeout=1)
            except subprocess.TimeoutExpired:
                pass
            process.stdout.close()
        selector.close()


def selected_mobile_sources(root=None):
    """One deterministic bounded selection for both source binding and private copying."""
    root = root or MOBILE
    selected, total, scanned = [], 0, 0
    started = time.monotonic()
    try:
        require(root.is_dir() and not root.is_symlink(), "source_unavailable")
        def failed_walk(error):
            raise EntryError("source_unavailable")
        for directory, names, files in os.walk(root, followlinks=False, onerror=failed_walk):
            scanned += len(names) + len(files)
            require(scanned <= MAX_SOURCE_FILES * 4 and time.monotonic() - started < SOURCE_SECONDS,
                "source_selection_bound_rejected")
            parent = Path(directory)
            names[:] = sorted(name for name in names if name not in SOURCE_IGNORED_DIRECTORIES and
                str((parent / name).relative_to(root)) not in SOURCE_IGNORED_RELATIVE_DIRECTORIES)
            for name in names:
                require(not (parent / name).is_symlink(), "source_symlink_rejected")
            for name in sorted(files):
                if name in SOURCE_IGNORED_FILES or Path(name).suffix in SOURCE_IGNORED_SUFFIXES:
                    continue
                path = parent / name
                require(path.is_file() and not path.is_symlink(), "source_symlink_rejected")
                size = path.stat().st_size
                total += size
                require(size <= MAX_SOURCE_FILE_BYTES and total <= MAX_SOURCE_BYTES and
                    len(selected) < MAX_SOURCE_FILES, "source_copy_capacity_rejected")
                selected.append(path)
        return sorted(selected)
    except OSError:
        raise EntryError("source_unavailable") from None


def read_source(path):
    """Read a stable no-follow descriptor; source files need not have private modes."""
    parent, descriptor = None, None
    try:
        require(path.is_absolute() and ".." not in path.parts, "source_unavailable")
        parent = os.open("/", os.O_RDONLY | os.O_DIRECTORY)
        for component in path.parts[1:-1]:
            following = os.open(component, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent)
            os.close(parent)
            parent = following
        descriptor = os.open(path.name, os.O_RDONLY | os.O_NONBLOCK | os.O_NOFOLLOW, dir_fd=parent)
        info = os.fstat(descriptor)
        require(stat.S_ISREG(info.st_mode) and info.st_size <= MAX_SOURCE_FILE_BYTES, "source_unavailable")
        with os.fdopen(descriptor, "rb") as handle:
            descriptor = None
            data = handle.read(MAX_SOURCE_FILE_BYTES + 1)
        require(len(data) == info.st_size and len(data) <= MAX_SOURCE_FILE_BYTES, "source_changed_during_copy")
        return data
    except OSError:
        raise EntryError("source_unavailable") from None
    finally:
        if descriptor is not None:
            os.close(descriptor)
        if parent is not None:
            os.close(parent)


def source_snapshot():
    paths = [ROOT / "AGENTS.md", ROOT / "tools/mobile-test-account.json"]
    paths += list((ROOT / "tools/image-optimization").glob("native_entry_*.py"))
    paths += [ROOT / "tools/image-optimization/native_fixture_server.py",
        ROOT / "tools/image-optimization/pyproject.toml", ROOT / "tools/image-optimization/uv.lock"]
    paths += selected_mobile_sources()
    result = {}
    started, total = time.monotonic(), 0
    try:
        for path in sorted(set(paths)):
            require(time.monotonic() - started < SOURCE_SECONDS and len(result) < MAX_SOURCE_FILES,
                "source_selection_bound_rejected")
            data = read_source(path)
            total += len(data)
            require(total <= MAX_SOURCE_BYTES, "source_copy_capacity_rejected")
            result[str(path.relative_to(ROOT))] = sha256(data).hexdigest()
        return result, sha256(frozen_json(result)).hexdigest()
    except OSError:
        raise EntryError("source_unavailable") from None


def artifact_observation(path, *, deadline=None):
    """Hash every regular byte with UTF-8 path ordering and bounded no-follow reads."""
    path = no_symlink(path)
    deadline = min(deadline if deadline is not None else float("inf"), time.monotonic() + 30)
    root = None
    def signature(info):
        return (info.st_dev, info.st_ino, info.st_mode, info.st_size, info.st_mtime_ns, info.st_ctime_ns)
    def check_deadline():
        require(time.monotonic() < deadline, "artifact_deadline_exceeded")
    def hash_file(descriptor, expected, budget=256 * 1024 * 1024):
        check_deadline()
        before = os.fstat(descriptor)
        require(stat.S_ISREG(before.st_mode) and signature(before) == expected, "artifact_changed")
        require(before.st_size <= min(256 * 1024 * 1024, budget), "artifact_size_rejected")
        digest, count = sha256(), 0
        while True:
            check_deadline()
            data = os.read(descriptor, 1024 * 1024)
            if not data:
                break
            count += len(data)
            require(count <= min(256 * 1024 * 1024, budget), "artifact_size_rejected")
            digest.update(data)
        require(count == before.st_size and signature(os.fstat(descriptor)) == expected, "artifact_changed")
        return digest.digest(), count
    try:
        initial = os.lstat(path)
        require(stat.S_ISREG(initial.st_mode) or stat.S_ISDIR(initial.st_mode), "artifact_type_rejected")
        root = open_nofollow(path, os.O_RDONLY | os.O_NONBLOCK |
            (os.O_DIRECTORY if stat.S_ISDIR(initial.st_mode) else 0))
        require(signature(os.fstat(root)) == signature(initial), "artifact_changed")
        if stat.S_ISREG(initial.st_mode):
            digest, size = hash_file(root, signature(initial))
            require(signature(os.lstat(path)) == signature(initial), "artifact_changed")
            return {"sha256": digest.hex(), "fileCount": 1, "byteCount": size}
        selected, scanned = [], 0
        directories = {(): signature(initial)}
        def visit(descriptor, parts, depth):
            nonlocal scanned
            check_deadline()
            require(depth <= 128, "artifact_scan_bound_rejected")
            # Descriptor scandir propagates traversal/read errors; Path.rglob can hide them.
            with os.scandir(descriptor) as entries:
                for entry in entries:
                    check_deadline()
                    scanned += 1
                    require(scanned <= 65536, "artifact_scan_bound_rejected")
                    components = parts + (entry.name,)
                    relative = "/".join(components).encode("utf-8", "strict")
                    require(len(relative) <= 4096, "artifact_scan_bound_rejected")
                    info = os.stat(entry.name, dir_fd=descriptor, follow_symlinks=False)
                    expected = signature(info)
                    require(not stat.S_ISLNK(info.st_mode), "artifact_symlink_rejected")
                    if stat.S_ISREG(info.st_mode):
                        selected.append((relative, components, expected))
                    elif stat.S_ISDIR(info.st_mode):
                        child = os.open(entry.name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=descriptor)
                        try:
                            require(signature(os.fstat(child)) == expected, "artifact_changed")
                            directories[components] = expected
                            visit(child, components, depth + 1)
                        finally:
                            os.close(child)
                    else:
                        raise EntryError("artifact_type_rejected")
        def open_relative(parts, *, directory=False):
            descriptor = os.dup(root)
            try:
                require(signature(os.fstat(descriptor)) == directories[()], "artifact_changed")
                for index, component in enumerate(parts):
                    check_deadline()
                    prefix = parts[:index + 1]
                    is_directory = directory or index < len(parts) - 1
                    child = os.open(component, os.O_RDONLY | os.O_NONBLOCK | os.O_NOFOLLOW |
                        (os.O_DIRECTORY if is_directory else 0), dir_fd=descriptor)
                    os.close(descriptor)
                    descriptor = child
                    if is_directory:
                        require(signature(os.fstat(descriptor)) == directories[prefix], "artifact_changed")
                return descriptor
            except BaseException:
                os.close(descriptor)
                raise
        visit(root, (), 0)
        digest, total = sha256(), 0
        for relative, parts, expected in sorted(selected, key=lambda row: row[0]):
            descriptor = open_relative(parts)
            try:
                file_digest, size = hash_file(descriptor, expected, 512 * 1024 * 1024 - total)
            finally:
                os.close(descriptor)
            total += size
            digest.update(len(relative).to_bytes(4, "big") + relative + file_digest)
        # Earlier bytes may change while later files are hashed without altering a directory.
        for _, parts, expected in selected:
            descriptor = open_relative(parts)
            try:
                check_deadline()
                require(stat.S_ISREG(os.fstat(descriptor).st_mode) and signature(os.fstat(descriptor)) == expected,
                    "artifact_changed")
            finally:
                os.close(descriptor)
        for parts in directories:
            descriptor = open_relative(parts, directory=True)
            os.close(descriptor)
        current_root = open_nofollow(path, os.O_RDONLY | os.O_DIRECTORY)
        try:
            require(signature(os.fstat(current_root)) == directories[()], "artifact_changed")
        finally:
            os.close(current_root)
        return {"sha256": digest.hexdigest(), "fileCount": len(selected), "byteCount": total}
    except (OSError, UnicodeError):
        raise EntryError("artifact_unavailable") from None
    finally:
        if root is not None:
            os.close(root)


def artifact_hash(path):
    return artifact_observation(path)["sha256"]


def private_mobile_snapshot(output, expected_sources=None):
    """Prepare native build state outside the checkout; preserve pinned lock bytes."""
    destination = output / "source/repository/app/mobile"
    destination.mkdir(parents=True, mode=0o700)
    total = 0
    try:
        copied, selected = {}, selected_mobile_sources()
        started = time.monotonic()
        for path in selected:
            require(time.monotonic() - started < SOURCE_SECONDS, "source_selection_bound_rejected")
            data = read_source(path)
            total += len(data)
            require(total <= MAX_SOURCE_BYTES, "source_copy_capacity_rejected")
            relative = path.relative_to(MOBILE)
            digest = sha256(data).hexdigest()
            name = str(path.relative_to(ROOT)) if expected_sources is not None else str(relative)
            if expected_sources is not None:
                require(expected_sources.get(name) == digest, "source_changed_during_copy")
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
            fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW,
                0o700 if os.access(path, os.X_OK) else 0o600)
            with os.fdopen(fd, "wb") as handle:
                handle.write(data)
            require(sha256(read_source(target)).hexdigest() == digest, "source_changed_during_copy")
            copied[name] = digest
        if expected_sources is not None:
            prefix = str(MOBILE.relative_to(ROOT)) + "/"
            expected = {name: digest for name, digest in expected_sources.items() if name.startswith(prefix)}
            require(copied == expected, "source_changed_during_copy")
        return destination
    except OSError:
        raise EntryError("source_copy_failed") from None


def validate_copied_sources(mobile, sources):
    prefix = str(MOBILE.relative_to(ROOT)) + "/"
    expected = {name[len(prefix):]: digest for name, digest in sources.items() if name.startswith(prefix)}
    actual = {str(path.relative_to(mobile)): sha256(read_source(path)).hexdigest()
        for path in selected_mobile_sources(mobile)}
    require(actual == expected, "copied_source_changed")
    return sha256(frozen_json(actual)).hexdigest()


def prepare_android_tool_settings(mobile):
    """Generate private SDK locations instead of copying local developer properties."""
    flutter = shutil.which("flutter")
    sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    require(flutter and sdk, "build_tools_unavailable")
    flutter_root = Path(flutter).resolve().parent.parent
    android_root = Path(sdk).resolve()
    require((flutter_root / "packages/flutter_tools/gradle").is_dir() and android_root.is_dir(), "build_tools_unavailable")
    def property_value(path):
        value = str(path)
        require("\n" not in value and "\r" not in value, "build_tools_unavailable")
        return value.replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=").replace(" ", "\\ ")
    write_private(mobile / "android/local.properties",
        ("flutter.sdk=" + property_value(flutter_root) + "\nsdk.dir=" + property_value(android_root) + "\n").encode())


def validate_backend_startup(config, inspector=None):
    """Bind live PID/start/command/listener to rehashed source, artifact and config."""
    receipt = strict_json(private_file(config.values["backendStartupReceiptPath"],
        MAX_BACKEND_RECEIPT_BYTES), MAX_BACKEND_RECEIPT_BYTES)
    exact_keys(receipt, ("schemaVersion", "backendEnvironment", "backendOrigin", "pid", "processStartTime",
        "commandSha256", "sourceSha256", "sourceFiles", "artifactPath", "artifactSha256", "configPath", "configSha256"))
    require(type(receipt["schemaVersion"]) is int and receipt["schemaVersion"] == 1 and
        receipt["backendEnvironment"] == config.values["backendEnvironment"] and
        receipt["backendOrigin"] == config.values["backendOrigin"] and
        type(receipt["pid"]) is int and receipt["pid"] > 0 and
        type(receipt["processStartTime"]) is str and len(receipt["processStartTime"]) <= 64, "backend_receipt_rejected")
    for name in ("commandSha256", "sourceSha256", "artifactSha256", "configSha256"):
        require(type(receipt[name]) is str and HASH.fullmatch(receipt[name]), "backend_receipt_rejected")
    files = receipt["sourceFiles"]
    require(type(files) is dict and 1 <= len(files) <= MAX_BACKEND_SOURCE_FILES, "backend_receipt_rejected")
    for name, digest in files.items():
        require(type(name) is str and name.startswith("be/") and ".." not in Path(name).parts and
            not Path(name).is_absolute() and type(digest) is str and HASH.fullmatch(digest), "backend_receipt_rejected")
        path = ROOT / name
        require(not path.is_symlink() and path.is_file() and path.stat().st_size <= 4 * 1024 * 1024 and
            sha256(path.read_bytes()).hexdigest() == digest, "backend_source_changed")
    require(sha256(frozen_json(files)).hexdigest() == receipt["sourceSha256"] and
        artifact_hash(receipt["artifactPath"]) == receipt["artifactSha256"] and
        sha256(private_file(receipt["configPath"])).hexdigest() == receipt["configSha256"], "backend_binding_changed")
    if inspector is None:
        def inspector(pid, port):
            start = capture_observation(["ps", "-p", str(pid), "-o", "lstart="]).strip().decode("ascii")
            command = capture_observation(["ps", "-p", str(pid), "-o", "command="]).strip()
            listener = capture_observation(["lsof", "-nP", "-a", "-p", str(pid), "-iTCP:" + str(port), "-sTCP:LISTEN", "-Fn"])
            return start, command, listener
    parsed = origin(config.values["backendOrigin"])
    started, command, listeners = inspector(receipt["pid"], parsed.port)
    require(started == receipt["processStartTime"] and sha256(command).hexdigest() == receipt["commandSha256"], "backend_process_changed")
    profile = receipt["backendEnvironment"].encode()
    require(re.search(rb"(?:--spring\.profiles\.active=|-Dspring\.profiles\.active=)" + profile + rb"(?:\s|$)", command),
        "backend_profile_unverified")
    ports = [line[1:] for line in listeners.splitlines() if line.startswith(b"n")]
    require(ports and all(item in (b"127.0.0.1:" + str(parsed.port).encode(),
        b"[::1]:" + str(parsed.port).encode(), b"::1:" + str(parsed.port).encode()) for item in ports), "backend_listener_unverified")
    return sha256(frozen_json(receipt)).hexdigest()


def validate_ios_native_test_linkage(mobile, runner, output, deadline):
    """Reject application runtimes in the native XCTest runner before accepting a build."""
    helper = Path(mobile) / "ios/ImageExperimentUITests/verify_native_linkage.py"
    result = checked_run([sys.executable, "-B", str(helper), "--runner", str(runner)],
        output, "native-test-linkage", deadline=deadline)
    require(result == b"NATIVE_TEST_LINKAGE_PASS\n", "native_test_linkage_rejected")
    return "apple_native_only"


def build_entry(config, manifest, output, deadline):
    values = config.values
    snapshot, snapshot_hash = source_snapshot()
    certificate_identity(config)
    mobile = private_mobile_snapshot(output, snapshot)
    if values["platform"] == "android":
        prepare_android_tool_settings(mobile)
    original_pub_lock = sha256((mobile / "pubspec.lock").read_bytes()).hexdigest()
    checked_run(["flutter", "pub", "get", "--offline"], output, "flutter-packages", deadline=deadline, cwd=mobile)
    require(sha256((mobile / "pubspec.lock").read_bytes()).hexdigest() == original_pub_lock, "mobile_lock_changed")
    if values["platform"] == "android":
        android_generation = generate_android_registrant(mobile, output, values["mode"], deadline, checked_run)
        require(sha256((mobile / "pubspec.lock").read_bytes()).hexdigest() == original_pub_lock, "mobile_lock_changed")
        generated_registrant_hash = generated_source(mobile / ANDROID_REGISTRANT_SOURCE)
        mode = values["mode"].capitalize()
        checked_run([str(mobile / "android/gradlew"), "--no-daemon", "-PimageExperiment=true",
            "-PexperimentMode=" + values["mode"], "-Ptarget=lib/main_image_experiment.dart",
            "-PimageExperimentSigningConfig=" + values["signingConfigFile"],
            "-PimageExperimentOutputRoot=" + str(output), ":app:assemble" + mode,
            ":app:assemble" + mode + "AndroidTest"], output, "build", deadline=deadline, cwd=mobile / "android")
        apks = sorted((output / "gradle/app/outputs/apk").rglob("*.apk"))
        require(len(apks) == 2, "artifact_count_rejected")
        app = next((path for path in apks if "androidTest" not in str(path) and "androidtest" not in str(path).lower()), None)
        test = next((path for path in apks if path != app), None)
        require(app is not None and test is not None, "artifact_count_rejected")
        app_id = checked_run(["apkanalyzer", "manifest", "application-id", str(app)], output, "app-id", deadline=deadline).decode().strip()
        test_id = checked_run(["apkanalyzer", "manifest", "application-id", str(test)], output, "test-id", deadline=deadline).decode().strip()
        certificates = []
        for name, artifact in (("app-signing", app), ("test-signing", test)):
            text = checked_run(["apksigner", "verify", "--print-certs", str(artifact)], output, name, deadline=deadline).decode()
            rows = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})", text)
            require(len(rows) == 1, "signing_identity_rejected")
            certificates.append(rows[0])
        require(certificates[0] == certificates[1], "signing_mismatch")
        debuggable_text = checked_run(["apkanalyzer", "manifest", "debuggable", str(app)], output, "debuggable", deadline=deadline).decode().strip()
        require(debuggable_text in ("true", "false"), "debuggable_unverified")
        debuggable = debuggable_text == "true"
        android_plugins = inspect_android_plugins(mobile, app, output, values["mode"], deadline,
            checked_run, artifact_hash)
        require(android_plugins["generatedSourceSha256"] == generated_registrant_hash, "android_plugin_binding_changed")
        android_plugins.update(android_generation)
    else:
        derived = output / "derived-data"
        checked_run(["pod", "install", "--deployment"], output, "pods", deadline=deadline, cwd=mobile / "ios")
        checked_run(["xcodebuild", "-workspace", str(mobile / "ios/Runner.xcworkspace"),
            "-scheme", "imageexperiment", "-configuration", values["mode"].capitalize() + "-imageexperiment",
            "-sdk", "iphoneos", "-derivedDataPath", str(derived), "-xcconfig", values["signingConfigFile"],
            "FLUTTER_APPLICATION_PATH=" + str(mobile), "build-for-testing"], output, "build", deadline=deadline, cwd=mobile / "ios")
        products = derived / "Build/Products" / (values["mode"].capitalize() + "-imageexperiment-iphoneos")
        app, test = products / "ImageExperiment.app", products / "ImageExperimentUITests-Runner.app"
        require(app.is_dir() and test.is_dir(), "artifact_unavailable")
        run_files = sorted((derived / "Build/Products").glob("*.xctestrun"))
        require(len(run_files) == 1, "xctestrun_binding_rejected")
        xctestrun = no_symlink(run_files[0])
        xctestrun_hash = sha256(private_file(xctestrun, 256 * 1024)).hexdigest()
        app_id = checked_run(["plutil", "-extract", "CFBundleIdentifier", "raw", "-o", "-", str(app / "Info.plist")], output, "app-id", deadline=deadline).decode().strip()
        test_id = checked_run(["plutil", "-extract", "CFBundleIdentifier", "raw", "-o", "-", str(test / "Info.plist")], output, "test-id", deadline=deadline).decode().strip()
        native_test_linkage = validate_ios_native_test_linkage(mobile, test, output, deadline)
        certificates = []
        for name, artifact in (("app-signing", app), ("test-signing", test)):
            prefix = output / name
            checked_run(["codesign", "--verify", "--strict", str(artifact)], output, name + "-verify", deadline=deadline)
            checked_run(["codesign", "--display", "--extract-certificates=" + str(prefix), str(artifact)], output, name, deadline=deadline)
            certificates.append(sha256(private_file(str(prefix) + "0", exact_mode=False)).hexdigest())
        require(certificates[0] == certificates[1], "signing_mismatch")
        debuggable = None
    require(app_id == values["expectedAppId"] and test_id.startswith(app_id + ".") and
        (test_id == app_id + ".test" if values["platform"] == "android" else test_id == app_id + ".uitests.xctrunner"), "app_identity_rejected")
    version_bytes = checked_run(["flutter", "--version", "--machine"], output, "flutter-version", deadline=deadline)
    version = strict_json(version_bytes)
    require(type(version.get("engineRevision")) is str, "engine_identity_unavailable")
    require(source_snapshot()[1] == snapshot_hash, "source_changed_during_build")
    copied_hash = validate_copied_sources(mobile, snapshot)
    receipt = {"schemaVersion": 1, "scope": SCOPE, "platform": values["platform"], "mode": values["mode"],
        "expectedAppId": app_id, "testAppId": test_id, "artifactPath": str(app), "testArtifactPath": str(test),
        "artifactSha256": artifact_hash(app), "testArtifactSha256": artifact_hash(test),
        "certificateSha256": certificates[0], "signingConfigSha256": sha256(private_file(values["signingConfigFile"])).hexdigest(),
        "sourceSha256": snapshot_hash, "sourceFiles": snapshot,
        "sourceCopyPath": str(mobile), "copiedSourceSha256": copied_hash,
        "lockSha256": sha256((Path(__file__).parent / "uv.lock").read_bytes()).hexdigest(),
        "dartTarget": "lib/main_image_experiment.dart", "nativeTarget": "imageExperiment" if values["platform"] == "android" else "ImageExperiment",
        "workspacePath": str(mobile / "ios/Runner.xcworkspace") if values["platform"] == "ios" else None,
        "xctestrunPath": str(xctestrun) if values["platform"] == "ios" else None,
        "xctestrunSha256": xctestrun_hash if values["platform"] == "ios" else None,
        "nativeTestLinkage": native_test_linkage if values["platform"] == "ios" else None,
        "engineRevision": version["engineRevision"], "renderer": None, "rendererReason": "runtime_not_observed",
        "debuggable": debuggable, "debuggableReason": None if debuggable is not None else "runtime_not_observed",
        "physicalVerification": "not_run", "nativeEntryGate": "open"}
    if values["platform"] == "android":
        require(receipt["artifactSha256"] == android_plugins["artifactSha256"], "android_plugin_binding_changed")
        receipt["androidPluginRegistration"] = android_plugins
    write_private(output / "build-receipt.json", receipt)
    return receipt


class LauncherPairing:
    """Parse exactly one public key report; raw traces never contain credentials."""
    def __init__(self, broker, platform):
        self.broker, self.platform = broker, platform
        self.rows = {}
        self.count = 0
        self.native_result = None
        self.native_result_count = 0
        self.native_reason = None
        self.pending = None
        self.error = None
        self.worker = None
        self.closed = threading.Event()
        self.ready = threading.Event()

    def bind_when_reachable(self):
        try:
            while not self.closed.is_set():
                remaining = min(30, self.broker.deadline / 1000 - self.broker.clock(),
                    30 - (self.broker.monotonic() - self.broker.started))
                require(remaining > 0, "reachability_deadline_exceeded")
                if self.broker.reachability_ready.wait(timeout=min(0.05, remaining)):
                    require(not self.closed.is_set(), "launcher_closed")
                    self.broker.pair_launcher(self.pending)
                    self.pending = None
                    return
        except EntryError as error:
            self.error = str(error)
        finally:
            self.ready.set()

    def poll(self):
        if self.error is not None:
            raise EntryError(self.error)

    def close(self):
        self.closed.set()
        if self.worker is not None:
            self.worker.join(timeout=1)
        self.pending = None

    def record_result(self, result):
        require(result in ("settled", "refused") and self.native_result_count == 0,
            "native_result_rejected")
        self.native_result = result
        self.native_result_count = 1

    def __call__(self, line):
        require(len(line) <= 8192, "launcher_line_rejected")
        payload = None
        if self.platform == "ios" and line.startswith(b"IMAGE_EXPERIMENT_RESULT "):
            require(line in (b"IMAGE_EXPERIMENT_RESULT settled", b"IMAGE_EXPERIMENT_RESULT refused"), "native_result_rejected")
            self.record_result(line.split(b" ", 1)[1].decode("ascii"))
            return
        if self.platform == "ios" and line.startswith(b"IMAGE_EXPERIMENT_PAIRING "):
            try:
                payload = strict_json(base64.b64decode(line.split(b" ", 1)[1], validate=True), 2048)
            except ValueError:
                raise EntryError("launcher_report_rejected") from None
        if self.platform == "android":
            result_prefix = b"INSTRUMENTATION_RESULT: reason="
            if line.startswith(result_prefix):
                require(self.native_reason is None and line[len(result_prefix):] in (
                    b"NATIVE_LAUNCHER_SETTLED", b"NATIVE_LAUNCHER_REFUSED"), "native_result_rejected")
                self.native_reason = line[len(result_prefix):]
                return
            if line.startswith(b"INSTRUMENTATION_CODE:"):
                if self.native_reason == b"NATIVE_LAUNCHER_SETTLED" and line.strip() == b"INSTRUMENTATION_CODE: -1":
                    self.record_result("settled")
                elif self.native_reason == b"NATIVE_LAUNCHER_REFUSED" and line.strip() == b"INSTRUMENTATION_CODE: 0":
                    self.record_result("refused")
                else:
                    raise EntryError("native_result_rejected")
                return
            prefix = b"INSTRUMENTATION_STATUS: "
            if line.startswith(prefix):
                field, separator, value = line[len(prefix):].partition(b"=")
                if separator and field in (b"event", b"sessionId", b"launchNonce", b"publicKeySpkiBase64"):
                    require(field not in self.rows, "launcher_duplicate_rejected")
                    self.rows[field] = value
            elif line.startswith(b"INSTRUMENTATION_STATUS_CODE:"):
                if self.rows.get(b"event") == b"pairing-key":
                    require(line.strip() == b"INSTRUMENTATION_STATUS_CODE: 1", "launcher_report_rejected")
                    try:
                        payload = {name.decode(): value.decode("ascii") for name, value in self.rows.items() if name != b"event"}
                    except UnicodeError:
                        raise EntryError("launcher_report_rejected") from None
                self.rows.clear()
        if payload is not None:
            self.count += 1
            require(self.count == 1, "launcher_replacement_rejected")
            exact_keys(payload, ("sessionId", "launchNonce", "publicKeySpkiBase64"))
            self.broker.launch_binding(payload["sessionId"], payload["launchNonce"])
            public_key(payload["publicKeySpkiBase64"])
            self.pending = payload
            self.worker = threading.Thread(target=self.bind_when_reachable, daemon=True)
            self.worker.start()


def validate_entry_outcome(broker, pairing):
    """Successful entry, full fixture transfer and explicit app teardown are distinct."""
    pairing.poll()
    require(pairing.ready.is_set() and pairing.pending is None and pairing.count == 1,
        "launcher_pairing_unsettled")
    require(pairing.native_result_count == 1, "native_result_unverified")
    require(pairing.native_result == "settled", "native_entry_refused")
    with broker.lock:
        broker.state()
        require(broker.admission_successful, "entry_admission_unverified")
        entry = next(item for item in broker.manifest.entries if item["caseId"] == broker.case_id)
        require(broker.fixture_delivery_count > 0 and broker.fixture_delivery_bytes == entry["byteSize"] and
            broker.fixture_delivery_sha256 == entry["outputSha256"], "fixture_delivery_unverified")
        require(broker.explicit_app_revoked and broker.reason == "app_revoked" and
            not broker.active and broker.token is None, "entry_teardown_unverified")
        return {"admission": "observed", "fixtureDelivery": "observed",
            "fixtureDeliveryCount": broker.fixture_delivery_count,
            "fixtureBodyBytes": broker.fixture_delivery_bytes,
            "fixtureOutputSha256": broker.fixture_delivery_sha256,
            "nativeResult": "settled", "teardown": "authenticated_app_revoke_observed"}


def validate_physical_prerequisites(config, build_path, prerequisite_path):
    build = strict_json(private_file(build_path, 2 * 1024 * 1024), 2 * 1024 * 1024)
    require(type(build) is dict, "build_binding_rejected")
    proof = strict_json(private_file(prerequisite_path))
    exact_keys(proof, ("schemaVersion", "scope", "batchId", "platform", "mode", "deviceAlias", "deviceSelectorSha256",
        "expectedAppId", "firebaseProjectId", "artifactSha256", "testArtifactSha256", "certificateSha256",
        "physical", "connected", "deviceModel", "provider", "observedAt", "projectAccepted", "iamVerified",
        "quotaVerified", "reservationExpiresAt", "reservationReceiptSha256", "installedArtifactSha256",
        "installedTestArtifactSha256", "installationReceiptSha256", "installationAuthorized", "authenticationAuthorized"))
    values = config.values
    require(type(proof["schemaVersion"]) is int and proof["schemaVersion"] == 1 and proof["scope"] == SCOPE, "physical_receipt_rejected")
    for name in ("batchId", "platform", "mode", "deviceAlias", "expectedAppId", "firebaseProjectId"):
        require(proof[name] == values[name], "physical_binding_rejected")
    require(proof["deviceSelectorSha256"] == sha256(values["deviceSelector"].encode()).hexdigest(), "device_binding_rejected")
    for name in ("physical", "connected", "projectAccepted", "iamVerified", "installationAuthorized", "authenticationAuthorized"):
        require(proof[name] is True, "physical_prerequisite_unavailable")
    require(type(proof["observedAt"]) is int and int(time.time() * 1000) - 300000 <= proof["observedAt"] <= int(time.time() * 1000), "device_receipt_stale")
    if values["platform"] == "ios":
        require(proof["deviceModel"] == "iPhone 12" and proof["provider"] == "local" and
            proof["reservationExpiresAt"] is None and proof["reservationReceiptSha256"] is None, "device_selection_rejected")
    else:
        require(proof["provider"] == "android_device_streaming" and proof["quotaVerified"] is True and
            type(proof["reservationExpiresAt"]) is int and proof["reservationExpiresAt"] > int((time.time() + 195) * 1000) and
            type(proof["reservationReceiptSha256"]) is str and HASH.fullmatch(proof["reservationReceiptSha256"]), "reservation_unavailable")
    for name in ("artifactSha256", "testArtifactSha256", "certificateSha256"):
        require(type(proof[name]) is str and HASH.fullmatch(proof[name]) and proof[name] == build.get(name), "artifact_binding_rejected")
    sources, source_hash = source_snapshot()
    require(build.get("scope") == SCOPE and build.get("platform") == values["platform"] and build.get("mode") == values["mode"] and
        build.get("expectedAppId") == values["expectedAppId"] and build.get("dartTarget") == "lib/main_image_experiment.dart" and
        artifact_hash(build["artifactPath"]) == build["artifactSha256"] and
        artifact_hash(build["testArtifactPath"]) == build["testArtifactSha256"] and
        build.get("sourceSha256") == source_hash and build.get("sourceFiles") == sources and
        build.get("signingConfigSha256") == sha256(private_file(values["signingConfigFile"])).hexdigest(), "build_binding_rejected")
    require(type(build.get("sourceCopyPath")) is str, "build_binding_rejected")
    copy_root = no_symlink(build["sourceCopyPath"])
    require(copy_root.is_dir() and
        validate_copied_sources(copy_root, sources) == build.get("copiedSourceSha256"), "build_binding_rejected")
    require(proof["installedArtifactSha256"] == build["artifactSha256"] and
        proof["installedTestArtifactSha256"] == build["testArtifactSha256"] and
        type(proof["installationReceiptSha256"]) is str and HASH.fullmatch(proof["installationReceiptSha256"]), "installed_binding_unavailable")
    return build, proof


def verify_entry(config, manifest, output, deadline, *, case_id, build_path, prerequisite_path, quality_capture=False):
    """Launch only already-installed bound artifacts after private prerequisite admission."""
    require(case_id in manifest.objects, "unknown_case")
    require(type(quality_capture) is bool and (not quality_capture or config.values["batchId"] == "native_quality_r1"), "capture_selection_rejected")
    build, proof = validate_physical_prerequisites(config, build_path, prerequisite_path)
    backend_hash = validate_backend_startup(config)
    verifier = FirebaseVerifier(config)
    broker, server, thread, pairing = None, None, None, None
    try:
        broker = Broker(config, manifest, verifier, case_id=case_id)
        server = create_server(config, broker)
        thread = threading.Thread(target=server.serve_forever, kwargs={"poll_interval": 0.05}, daemon=True)
        thread.start()
        metadata = base64.b64encode(frozen_json(broker.metadata())).decode("ascii")
        pairing = LauncherPairing(broker, config.values["platform"])
        values = config.values
        if values["platform"] == "android":
            state = checked_run(["adb", "-s", values["deviceSelector"], "get-state"], output, "device-state", deadline=deadline)
            require(state.strip() == b"device", "device_unavailable")
            # Installation is a prior operator-owned action, never performed by this driver.
            command = ["adb", "-s", values["deviceSelector"], "shell", "am", "instrument", "-w", "-r",
                "-e", "imageExperimentMetadata", metadata, build["testAppId"] + "/com.tellpal.imageexperiment.ImageExperimentInstrumentation"]
            if quality_capture:
                command[-1:-1] = ["-e", "imageExperimentQualityCapture", "1"]
            launch_env = None
        else:
            selected = checked_run(["xcrun", "devicectl", "device", "info", "details", "--device", values["deviceSelector"]],
                output, "device-state", deadline=deadline)
            require(selected and b"disconnected" not in selected.lower(), "device_unavailable")
            run_file = destination_run(build, output, metadata, audit=False, quality_capture=quality_capture)
            command = ["xcodebuild", "-xctestrun", str(run_file), "-destination", "id=" + values["deviceSelector"],
                "test-without-building", "-only-testing:ImageExperimentUITests/" + FIXTURE_TEST]
            launch_env = tool_environment()
        checked_run(command, output, "launcher", deadline=deadline, on_line=pairing, env=launch_env)
        outcome = validate_entry_outcome(broker, pairing)
        receipt = {"schemaVersion": 1, "scope": SCOPE, "deviceAlias": values["deviceAlias"], "platform": values["platform"],
            "mode": values["mode"], "caseSha256": sha256(case_id.encode()).hexdigest(),
            "sessionSha256": sha256(broker.session_id.encode()).hexdigest(), "generation": 0,
            "artifactSha256": build["artifactSha256"], "testArtifactSha256": build["testArtifactSha256"],
            "backendStartupSha256": backend_hash, "paired": True, "reachability": "proxied", **outcome,
            "decodedAdmission": None, "decodedAdmissionReason": "private_device_ledger_not_collected",
            "physicalPresentation": None, "physicalPresentationReason": "not_measured",
            "nativeEntryGate": "open", "negativePhysicalMatrix": "not_run"}
        write_private(output / "verification-receipt.json", receipt)
        return receipt
    finally:
        cleanup_deadline = time.monotonic() + CLEANUP_SECONDS
        if pairing is not None:
            pairing.close()
        if broker is not None:
            broker.close()
        else:
            verifier.close()
        if server is not None:
            server.shutdown()
            server.owned_cleanup_deadline = cleanup_deadline
            server.server_close()
        if thread is not None:
            thread.join(timeout=max(0, cleanup_deadline - time.monotonic()))


def main(argv=None):
    parser = SafeParser(description="Private isolated fixture entry")
    parser.add_argument("mode", choices=("validate-config", "build-entry", "verify-entry"))
    parser.add_argument("--config", required=True)
    parser.add_argument("--case-id")
    parser.add_argument("--build-receipt")
    parser.add_argument("--prerequisite-receipt")
    args = parser.parse_args(argv)
    output = None
    try:
        config = load_config(args.config)
        manifest = load_manifest(config)
        certificate_identity(config)
        if args.mode == "validate-config":
            require(args.case_id is None and args.build_receipt is None and args.prerequisite_receipt is None, "cli_rejected")
            result = {"scope": SCOPE, "status": "validated", "manifestSha256": manifest.private_sha256,
                "publicManifestSha256": manifest.public_sha256, "nativeEntryGate": "open", "physicalVerification": "not_run"}
        else:
            if args.mode == "verify-entry":
                require(args.case_id is not None and args.build_receipt is not None and args.prerequisite_receipt is not None,
                    "physical_prerequisite_unavailable")
            else:
                require(args.case_id is None and args.build_receipt is None and args.prerequisite_receipt is None, "cli_rejected")
            output = private_output(config.values["privateOutputRoot"], [args.config, *[config.values[name]
                for name in ("manifestPath", "idTokenFile", "firebaseAdminCredentialFile", "tlsCertificateFile", "tlsPrivateKeyFile", "signingConfigFile")]], create=True)
            deadline = time.monotonic() + TOTAL_SECONDS
            receipt = build_entry(config, manifest, output, deadline) if args.mode == "build-entry" else verify_entry(
                config, manifest, output, deadline, case_id=args.case_id,
                build_path=args.build_receipt, prerequisite_path=args.prerequisite_receipt)
            result = {"scope": SCOPE, "status": "completed", "mode": args.mode,
                "receiptSha256": sha256(frozen_json(receipt)).hexdigest(), "nativeEntryGate": "open"}
        print(frozen_json(result).decode("ascii"))
        return 0
    except EntryError as error:
        if output is not None:
            write_private(output / "failure-receipt.json", {"scope": SCOPE, "status": "failed", "reason": str(error), "nativeEntryGate": "open"})
        print(frozen_json({"scope": SCOPE, "status": "refused", "reason": str(error), "nativeEntryGate": "open"}).decode("ascii"))
        return 4
    except (OSError, ValueError, KeyError, TypeError):
        print(frozen_json({"scope": SCOPE, "status": "refused", "reason": "operation_failed", "nativeEntryGate": "open"}).decode("ascii"))
        return 4


if __name__ == "__main__":
    sys.exit(main())
