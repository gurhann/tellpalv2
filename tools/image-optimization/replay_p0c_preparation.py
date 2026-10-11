#!/usr/bin/env python3
"""Replay immutable P0c preparation privately; this never qualifies native readiness."""

import argparse
from datetime import datetime, timezone
from hashlib import sha256
import json
import os
from pathlib import Path, PurePosixPath
import re
import selectors
import signal
import stat
import subprocess
import sys
import tarfile
import time

from native_pilot_preflight import SetupError, create_private_output, run_bounded


ROOT = Path(__file__).resolve().parents[2]
PREPARATION_COMMIT = "3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd"
SOURCE_BASELINE = "7e3ee8f58a210196ae6988dfa38481d5bd9a7957"
ARTIFACT_DIRECTORY = Path("_bmad-output/implementation-artifacts/image-optimization")
ORIGINAL_HASHES = {
    "verify-p0c-readiness.py": "7415fb2bf9888ca933ef3dc5f1d092788f199962457c9e78db0d80d46e8d3a13",
    "test_p0c_readiness.py": "983318f8964eb058112000bad7cd00caaf9075cce5a8663f08f28a76ef8ca663",
    "p0c-device-matrix.json": "2477a34ae32429258dfadcde78601eed87079bfae2e978a847e67b3735366ed3",
}
GATES = (
    "NATIVE-ENTRY", "NATIVE-DEVICE", "NATIVE-CANDIDATE", "NATIVE-PROTOCOL", "NATIVE-DECISIONS",
)
MAX_ARCHIVE_BYTES = 640 * 1024 * 1024
MAX_EXTRACTED_BYTES = 620 * 1024 * 1024
MAX_FILE_BYTES = 32 * 1024 * 1024
MAX_ENTRIES = 10000
MAX_RECEIPT_BYTES = 4 * 1024 * 1024
MAX_RECEIPT_TOTAL_BYTES = 16 * 1024 * 1024
ARCHIVE_TIMEOUT_SECONDS = 45
VERIFIER_TIMEOUT_SECONDS = 30
TEST_TIMEOUT_SECONDS = 90


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def _require(condition, reason):
    if not condition:
        raise SetupError(reason)


def _new_private_file(path):
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
    return os.fdopen(os.open(path, flags, 0o600), "wb")


def _write_json(path, value):
    data = (json.dumps(value, indent=2, allow_nan=False) + "\n").encode("utf-8")
    with _new_private_file(path) as stream:
        stream.write(data)


def _private_directory(path):
    path.mkdir(mode=0o700)
    os.chmod(path, 0o700)


def _contains(parent, child):
    return parent == child or parent in child.parents


def _no_symlink_components(path):
    """Refuse explicit symlink components, including roots of private receipt inputs."""
    return all(not component.is_symlink() for component in (path, *path.parents))


def safe_extract_archive(archive_path, destination, *, max_entries=MAX_ENTRIES,
                         max_total_bytes=MAX_EXTRACTED_BYTES, max_file_bytes=MAX_FILE_BYTES):
    """Manually extract bounded regular files/directories; never apply tar metadata."""
    destination = Path(destination)
    archive_path = Path(archive_path)
    _require(archive_path.is_file() and not archive_path.is_symlink()
             and archive_path.stat().st_size <= MAX_ARCHIVE_BYTES, "unsafe_archive_input")
    _require(destination.is_absolute() and _no_symlink_components(destination), "unsafe_archive_destination")
    _require(not _contains(ROOT.resolve(), destination.resolve()), "archive_destination_inside_repository")
    _require(destination.is_dir() and not any(destination.iterdir()), "archive_destination_not_empty")
    _require(stat.S_IMODE(destination.stat().st_mode) == 0o700, "archive_destination_not_private")
    for value, upper in ((max_entries, MAX_ENTRIES), (max_total_bytes, MAX_EXTRACTED_BYTES),
                         (max_file_bytes, MAX_FILE_BYTES)):
        _require(type(value) is int and 0 < value <= upper, "invalid_archive_limit")
    total = 0
    seen = set()
    count = 0
    try:
        with tarfile.open(archive_path, mode="r|") as archive:
            for member in archive:
                count += 1
                _require(count <= max_entries, "archive_entry_limit")
                name = member.name
                relative = PurePosixPath(name)
                _require(bool(name) and not relative.is_absolute() and not re.match(r"^[A-Za-z]:", name)
                         and "\\" not in name
                         and "\x00" not in name and ".." not in relative.parts
                         and relative.parts and relative.parts[0] not in {".", ".git"},
                         "unsafe_archive_path")
                _require(member.isdir() or member.isreg(), "unsafe_archive_member_type")
                _require(type(member.size) is int and 0 <= member.size <= max_file_bytes,
                         "archive_file_limit")
                _require(not member.isdir() or member.size == 0, "unsafe_archive_directory")
                parts = relative.parts
                _require(parts not in seen, "duplicate_archive_member")
                seen.add(parts)
                total += member.size
                _require(total <= max_total_bytes, "archive_total_limit")
                target = destination.joinpath(*parts)
                _require(_contains(destination, target) and _no_symlink_components(target),
                         "unsafe_archive_path")
                parent = destination
                for part in parts[:-1]:
                    parent /= part
                    if not parent.exists():
                        _private_directory(parent)
                    _require(parent.is_dir() and not parent.is_symlink(), "archive_path_conflict")
                if member.isdir():
                    if target.exists():
                        _require(target.is_dir() and not target.is_symlink(), "archive_path_conflict")
                    else:
                        _private_directory(target)
                    continue
                _require(not target.exists(), "archive_path_conflict")
                source = archive.extractfile(member)
                _require(source is not None, "unreadable_archive_member")
                remaining = member.size
                with source, _new_private_file(target) as output:
                    while remaining:
                        chunk = source.read(min(65536, remaining))
                        _require(bool(chunk), "truncated_archive_member")
                        output.write(chunk)
                        remaining -= len(chunk)
    except (tarfile.TarError, EOFError, OSError):
        raise SetupError("archive_extraction_failed") from None
    return {"entries": count, "extracted_bytes": total}


def _kill_process_group(process):
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except ProcessLookupError:
        pass


def archive_preparation(repo_root, receipt_dir, env, *, timeout_seconds=ARCHIVE_TIMEOUT_SECONDS,
                        max_bytes=MAX_ARCHIVE_BYTES):
    """Capture Git's uncompressed archive with finite disk/output/time bounds."""
    _require(type(timeout_seconds) in (int, float) and 0 < timeout_seconds <= 60,
             "invalid_archive_timeout")
    _require(type(max_bytes) is int and 0 < max_bytes <= MAX_ARCHIVE_BYTES, "invalid_archive_limit")
    receipt_dir = Path(receipt_dir)
    stdout_path = receipt_dir / "git-archive.stdout.tar"
    stderr_path = receipt_dir / "git-archive.stderr.bin"
    argv = ["git", "-c", "core.fsmonitor=false", "archive", "--format=tar", PREPARATION_COMMIT]
    started_at = utc_now()
    status, reason, exit_code = "ok", None, None
    sizes = {"stdout": 0, "stderr": 0}
    digests = {key: sha256() for key in sizes}
    process = None
    deadline = time.monotonic() + timeout_seconds
    try:
        with _new_private_file(stdout_path) as stdout_file, _new_private_file(stderr_path) as stderr_file:
            try:
                process = subprocess.Popen(argv, cwd=repo_root, env=env, stdin=subprocess.DEVNULL,
                                           stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                           start_new_session=True)
            except FileNotFoundError:
                status, reason = "missing_executable", "git_unavailable"
            except OSError:
                status, reason = "failed", "git_archive_start_failed"
            if process is not None:
                with selectors.DefaultSelector() as selector:
                    selector.register(process.stdout, selectors.EVENT_READ, ("stdout", stdout_file))
                    selector.register(process.stderr, selectors.EVENT_READ, ("stderr", stderr_file))
                    while selector.get_map():
                        remaining = deadline - time.monotonic()
                        if remaining <= 0:
                            status, reason = "timeout", "git_archive_timeout"
                            break
                        for key, _ in selector.select(min(remaining, 0.1)):
                            channel, output = key.data
                            chunk = os.read(key.fd, 65536)
                            if not chunk:
                                selector.unregister(key.fileobj)
                                continue
                            limit = max_bytes if channel == "stdout" else 262144
                            retained = chunk[:max(0, limit - sizes[channel])]
                            output.write(retained)
                            sizes[channel] += len(retained)
                            digests[channel].update(retained)
                            if len(retained) != len(chunk):
                                status, reason = "output_limit", "git_archive_output_limit"
                                break
                        if status != "ok":
                            break
                if status != "ok":
                    _kill_process_group(process)
                try:
                    exit_code = process.wait(timeout=max(0.01, deadline - time.monotonic()) if status == "ok" else 1)
                except subprocess.TimeoutExpired:
                    _kill_process_group(process)
                    process.wait(timeout=1)
                    status, reason = "timeout", "git_archive_timeout"
                    exit_code = process.returncode
                if status == "ok" and exit_code != 0:
                    status, reason = "failed", "git_archive_failed"
    finally:
        if process is not None:
            if process.poll() is None:
                _kill_process_group(process)
                process.wait(timeout=1)
            for stream in (process.stdout, process.stderr):
                if stream is not None:
                    stream.close()
    return {
        "status": status, "exit_code": exit_code, "reason": reason,
        "provenance": {"probe_id": "git-archive", "started_at": started_at,
                       "finished_at": utc_now(), "argv": argv},
        "stdout_file": stdout_path.name, "stderr_file": stderr_path.name,
        "stdout_sha256": digests["stdout"].hexdigest(), "stderr_sha256": digests["stderr"].hexdigest(),
        "stdout_bytes": sizes["stdout"], "stderr_bytes": sizes["stderr"],
    }


def historical_environment(repo_root):
    """Permit only local Git reads and disable inherited Git/Python injection settings."""
    repo_root = Path(repo_root).resolve()
    env = {key: value for key, value in os.environ.items()
           if not key.startswith(("GIT_", "PYTHON"))}
    env.update({"GIT_OPTIONAL_LOCKS": "0", "GIT_NO_REPLACE_OBJECTS": "1", "GIT_TERMINAL_PROMPT": "0",
                "GIT_CONFIG_NOSYSTEM": "1", "GIT_CONFIG_GLOBAL": os.devnull,
                "GIT_CONFIG_COUNT": "2", "GIT_CONFIG_KEY_0": "core.fsmonitor", "GIT_CONFIG_VALUE_0": "false",
                "GIT_CONFIG_KEY_1": "safe.directory", "GIT_CONFIG_VALUE_1": str(repo_root),
                "PYTHONDONTWRITEBYTECODE": "1"})
    return env


def validate_private_receipts(matrix, private_root, *, repo_root=ROOT, snapshot_dir=None):
    """Hash one bounded read per fixed receipt and optionally retain that exact snapshot."""
    private_root = Path(private_root).absolute()
    _require(_no_symlink_components(private_root) and private_root.is_dir(), "unsafe_private_receipts_root")
    private_root = private_root.resolve()
    _require(not _contains(Path(repo_root).resolve(), private_root), "private_receipts_inside_repository")
    root_stat = private_root.stat()
    _require(stat.S_IMODE(root_stat.st_mode) == 0o700 and root_stat.st_uid == os.getuid(),
             "private_receipts_root_not_private")
    rows = matrix.get("evidence_sources")
    _require(isinstance(rows, list) and len(rows) == 13, "invalid_historical_receipt_bindings")
    if snapshot_dir is not None:
        snapshot_dir = Path(snapshot_dir)
        _require(snapshot_dir.is_absolute() and _no_symlink_components(snapshot_dir)
                 and not _contains(Path(repo_root).resolve(), snapshot_dir.resolve())
                 and snapshot_dir.is_dir() and not any(snapshot_dir.iterdir())
                 and stat.S_IMODE(snapshot_dir.stat().st_mode) == 0o700, "unsafe_receipt_snapshot_directory")
    result, names, identifiers, total = [], set(), set(), 0
    for row in rows:
        _require(isinstance(row, dict), "invalid_historical_receipt_bindings")
        name, identifier, expected = row.get("receipt_file"), row.get("id"), row.get("receipt_sha256")
        _require(isinstance(name, str) and re.fullmatch(r"[a-z0-9-]+\.json", name)
                 and name not in names and isinstance(identifier, str) and re.fullmatch(r"E-[A-Z0-9-]+", identifier)
                 and identifier not in identifiers and isinstance(expected, str)
                 and re.fullmatch(r"[0-9a-f]{64}", expected), "invalid_historical_receipt_bindings")
        names.add(name)
        identifiers.add(identifier)
        path = private_root / name
        flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0) | getattr(os, "O_NONBLOCK", 0)
        try:
            with os.fdopen(os.open(path, flags), "rb") as stream:
                metadata = os.fstat(stream.fileno())
                _require(stat.S_ISREG(metadata.st_mode) and metadata.st_uid == os.getuid()
                         and stat.S_IMODE(metadata.st_mode) in {0o400, 0o600}, "private_receipt_not_private")
                _require(metadata.st_size <= MAX_RECEIPT_BYTES, "private_receipt_size_limit")
                data = stream.read(MAX_RECEIPT_BYTES + 1)
        except OSError:
            raise SetupError("private_receipt_unreadable") from None
        total += len(data)
        _require(len(data) <= MAX_RECEIPT_BYTES and total <= MAX_RECEIPT_TOTAL_BYTES,
                 "private_receipt_size_limit")
        digest = sha256(data).hexdigest()
        _require(digest == expected, "private_receipt_hash_mismatch")
        if snapshot_dir is not None:
            with _new_private_file(snapshot_dir / name) as output:
                output.write(data)
        result.append({"id": identifier, "receipt_file": name, "sha256": digest, "bytes": len(data)})
    return result


def check_original_files(snapshot_root):
    here = Path(snapshot_root) / ARTIFACT_DIRECTORY
    result = {}
    for name, expected in ORIGINAL_HASHES.items():
        path = here / name
        _require(path.is_file() and not path.is_symlink(), "missing_original_preparation_file")
        digest = sha256(path.read_bytes()).hexdigest()
        _require(digest == expected, "original_preparation_file_changed")
        result[name] = digest
    return result


def validate_verifier_result(result, *, receipt_count=0):
    """Accept only the fixed historical preparation scope from the child verifier."""
    _require(isinstance(result, dict) and result.get("preparation_checks") == "PASS"
             and result.get("preparation_complete") is True and result.get("native_execution_ready") is False
             and type(result.get("source_hashes_checked")) is int and result["source_hashes_checked"] == 43
             and type(result.get("physical_devices_qualified")) is int and result["physical_devices_qualified"] == 0
             and type(result.get("private_receipts_rehashed")) is int
             and result["private_receipts_rehashed"] == receipt_count
             and result.get("open_native_gates") == sorted(GATES), "historical_verifier_scope_mismatch")
    return {
        "preparation_checks": "PASS", "preparation_complete": True, "source_hashes_checked": 43,
        "private_receipts_rehashed": receipt_count, "physical_devices_qualified": 0,
        "native_execution_ready": False, "open_native_gates": sorted(GATES),
    }


def validate_test_output(output):
    _require(isinstance(output, bytes) and re.search(rb"(?m)^Ran 42 tests in [0-9.]+s\r?$", output)
             and re.search(rb"(?m)^OK\r?$", output) and b"skipped=" not in output,
             "historical_test_count_or_result_mismatch")
    return {"status": "passed", "tests_run": 42, "skips": 0}


def replay(output_parent=None, private_receipts=None, *, repo_root=ROOT):
    """Return a private report path and a sanitized historical-only summary."""
    repo_root = Path(repo_root).resolve()
    output_dir = create_private_output(output_parent, prefix="tellpal-p0c-historical-")
    _require(not _contains(repo_root, output_dir.resolve()), "output_inside_repository")
    receipt_dir, snapshot_root = output_dir / "receipts", output_dir / "archive"
    _private_directory(receipt_dir)
    _private_directory(snapshot_root)
    report = {
        "schema_version": 1, "scope": "historical_p0c_preparation_replay",
        "preparation_commit": PREPARATION_COMMIT, "source_baseline": SOURCE_BASELINE,
        "started_at": utc_now(), "finished_at": None, "status": "failed", "reason": None,
        "current_native_readiness_verified": False, "native_execution_ready": False,
        "gates": [{"id": gate, "status": "open"} for gate in GATES],
        "archive_root": str(snapshot_root), "probes": [], "original_file_hashes": {},
        "source_hashes_checked": 0, "original_tests_run": 0, "private_receipts_rehashed": 0,
        "limitations": ["This result applies only to immutable historical preparation.",
                        "Current native devices, entry, account access, candidates and counters were not verified."],
    }
    try:
        env = historical_environment(repo_root)
        git_dir_query = run_bounded(["git", "rev-parse", "--absolute-git-dir"], receipt_dir,
                                    "git-directory", env=env, cwd=repo_root)
        report["probes"].append(git_dir_query)
        _require(git_dir_query["status"] == "ok", "repository_git_directory_unavailable")
        git_dir_bytes = (receipt_dir / git_dir_query["stdout_file"]).read_bytes()
        _require(0 < len(git_dir_bytes) <= 8192 and b"\x00" not in git_dir_bytes
                 and git_dir_bytes.count(b"\n") <= 1,
                 "invalid_repository_git_directory")
        git_dir = Path(os.fsdecode(git_dir_bytes.rstrip(b"\r\n")))
        _require(git_dir.is_absolute() and git_dir.is_dir(), "invalid_repository_git_directory")
        env["GIT_DIR"] = str(git_dir)
        archive = archive_preparation(repo_root, receipt_dir, env)
        report["probes"].append(archive)
        _require(archive["status"] == "ok", "historical_archive_unavailable")
        report["archive"] = safe_extract_archive(receipt_dir / archive["stdout_file"], snapshot_root)
        report["original_file_hashes"] = check_original_files(snapshot_root)
        here = snapshot_root / ARTIFACT_DIRECTORY
        matrix = json.loads((here / "p0c-device-matrix.json").read_bytes())
        command = [sys.executable, "-B", str(here / "verify-p0c-readiness.py")]
        if private_receipts is not None:
            private_snapshot = output_dir / "original-private-receipts"
            _private_directory(private_snapshot)
            rehashed = validate_private_receipts(matrix, private_receipts, repo_root=repo_root,
                                                  snapshot_dir=private_snapshot)
            report["private_receipts"] = rehashed
            command += ["--private-evidence-root", str(private_snapshot)]
        else:
            report["limitations"].append("Original private raw receipt bytes were not supplied or rehashed.")
        env["TMPDIR"] = str(output_dir)
        env["GIT_WORK_TREE"] = str(snapshot_root)
        verifier = run_bounded(command, receipt_dir, "original-verifier", env=env,
                               cwd=snapshot_root, timeout_seconds=VERIFIER_TIMEOUT_SECONDS)
        report["probes"].append(verifier)
        _require(verifier["status"] == "ok", "historical_verifier_failed")
        try:
            verifier_result = json.loads((receipt_dir / verifier["stdout_file"]).read_bytes())
        except (ValueError, UnicodeError):
            raise SetupError("historical_verifier_result_invalid") from None
        report["verifier"] = validate_verifier_result(verifier_result, receipt_count=13 if private_receipts else 0)
        report["source_hashes_checked"] = 43
        report["private_receipts_rehashed"] = 13 if private_receipts else 0
        tests = run_bounded([sys.executable, "-B", str(here / "test_p0c_readiness.py")],
                            receipt_dir, "original-tests", env=env, cwd=snapshot_root,
                            timeout_seconds=TEST_TIMEOUT_SECONDS)
        report["probes"].append(tests)
        _require(tests["status"] == "ok", "historical_tests_failed")
        report["tests"] = validate_test_output((receipt_dir / tests["stderr_file"]).read_bytes())
        report["original_tests_run"] = 42
        report["status"] = "passed"
    except SetupError as error:
        report["reason"] = str(error)
    except (OSError, ValueError, TypeError, KeyError, UnicodeError, subprocess.SubprocessError):
        report["reason"] = "historical_replay_failed"
    report["finished_at"] = utc_now()
    report_path = output_dir / "historical-replay.json"
    _write_json(report_path, report)
    summary = {key: report[key] for key in (
        "scope", "preparation_commit", "status", "reason", "source_hashes_checked",
        "original_tests_run", "private_receipts_rehashed", "native_execution_ready",
        "current_native_readiness_verified",
    )}
    summary["open_native_gates"] = sorted(GATES)
    return report_path, summary


class SanitizedParser(argparse.ArgumentParser):
    def error(self, message):
        raise SetupError("invalid_cli_arguments")


def main(argv=None):
    parser = SanitizedParser(description=__doc__)
    parser.add_argument("--output-parent", type=Path, help="External parent for a unique private replay directory.")
    parser.add_argument("--private-receipts", type=Path,
                        help="Optional private directory containing the original 13 fixed raw receipts.")
    try:
        args = parser.parse_args(argv)
        _, summary = replay(args.output_parent, args.private_receipts)
    except SetupError:
        summary = {"scope": "historical_p0c_preparation_replay", "status": "error",
                   "reason": "invalid_or_unsafe_replay_input", "native_execution_ready": False,
                   "current_native_readiness_verified": False, "open_native_gates": sorted(GATES)}
        print(json.dumps(summary, sort_keys=True), file=sys.stderr)
        return 2
    except (OSError, ValueError, TypeError):
        summary = {"scope": "historical_p0c_preparation_replay", "status": "error",
                   "reason": "private_replay_output_unavailable", "native_execution_ready": False,
                   "current_native_readiness_verified": False, "open_native_gates": sorted(GATES)}
        print(json.dumps(summary, sort_keys=True), file=sys.stderr)
        return 2
    print(json.dumps(summary, sort_keys=True))
    return 0 if summary["status"] == "passed" else 4


if __name__ == "__main__":
    raise SystemExit(main())
