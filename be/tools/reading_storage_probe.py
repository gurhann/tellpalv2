#!/usr/bin/env python3
"""Dry-run-first P2 diagnostic launcher. Never forwards child output or SDK errors."""
import argparse
import dataclasses
import os
from pathlib import Path
import selectors
import shlex
import signal
import stat
import subprocess
import sys
import time
import re
import uuid

BE = Path(__file__).resolve().parents[1]
MAIN = "com.tellpal.v2.asset.infrastructure.storage.ReadingStorageLiveProbe"
OUTPUT_LIMIT = 65_536
RECEIPT_LIMIT = 16_384


@dataclasses.dataclass(frozen=True)
class ChildResult:
    status: str
    returncode: int
    observed_exit: bool


def supervise(command, *, cwd, seconds=60, output_limit=OUTPUT_LIMIT, environment=None):
    """Bound a real process group, count both pipes, and always reap the owned child."""
    child = None
    selector = None
    terminated = False
    status = "CHILD_EXIT"
    previous_handler = signal.getsignal(signal.SIGTERM)

    def request_termination(signum, frame):
        nonlocal terminated
        terminated = True

    signal.signal(signal.SIGTERM, request_termination)
    try:
        child = subprocess.Popen(command, cwd=cwd, env=environment, stdout=subprocess.PIPE,
                                 stderr=subprocess.PIPE, start_new_session=True)
        deadline = time.monotonic() + seconds
        selector = selectors.DefaultSelector()
        for pipe in (child.stdout, child.stderr):
            os.set_blocking(pipe.fileno(), False)
            selector.register(pipe, selectors.EVENT_READ)
        count = 0
        while selector.get_map() or child.poll() is None:
            if terminated:
                status = "SIGNAL_TERMINATED"
                break
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                status = "WATCHDOG_TIMEOUT"
                break
            for key, _ in selector.select(min(remaining, 0.05)):
                data = os.read(key.fileobj.fileno(), 8192)
                if not data:
                    selector.unregister(key.fileobj)
                count += len(data)
            if count > output_limit:
                status = "OUTPUT_OVERFLOW"
                break
        # Descendants may hold pipes after the direct child exits. Kill the entire
        # owned group even on success; no process in this diagnostic is persistent.
    finally:
        try:
            if child is not None:
                try:
                    os.killpg(child.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                child.wait()  # Cancellation is never treated as observed exit.
        finally:
            try:
                if selector is not None:
                    selector.close()
            finally:
                try:
                    if child is not None:
                        child.stdout.close()
                        child.stderr.close()
                finally:
                    signal.signal(signal.SIGTERM, previous_handler)
    if terminated:
        status = "SIGNAL_TERMINATED"
    return ChildResult(status, child.returncode, child.poll() is not None)


def clean_environment():
    environment = os.environ.copy()
    for name in tuple(environment):
        if name.startswith(("GOOGLE_", "GCLOUD_", "CLOUDSDK_")) or name in {
                "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "MAVEN_OPTS"}:
            environment.pop(name)
    return environment


def supervisor_receipt(path):
    """Create a separate exclusive private launcher ledger before the live child."""
    target = Path(str(path) + ".supervisor")
    parent = target.parent
    mode = parent.stat().st_mode
    if not parent.is_absolute() or parent.resolve() != parent or parent.resolve().is_relative_to(BE.parent.resolve()):
        raise ValueError("PRIVATE_LOCATION")
    if mode & 0o077 or parent.stat().st_uid != os.getuid():
        raise ValueError("PRIVATE_PERMISSIONS")
    if os.path.lexists(path):
        raise ValueError("WORKER_RECEIPT_ALREADY_EXISTS")
    descriptor = os.open(target, os.O_CREAT | os.O_EXCL | os.O_WRONLY | os.O_NOFOLLOW, 0o600)
    return os.fdopen(descriptor, "w", encoding="utf-8")


def verified_worker_receipt(args):
    """Accept only a private bounded complete worker ledger bound to this target."""
    try:
        descriptor = os.open(args.receipt, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
        with os.fdopen(descriptor, "rb") as source:
            info = os.fstat(source.fileno())
            if not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or info.st_mode & 0o077 \
                    or not 0 < info.st_size <= RECEIPT_LIMIT:
                return False
            raw = source.read(RECEIPT_LIMIT + 1)
        if len(raw) > RECEIPT_LIMIT:
            return False
        lines = raw.decode("ascii").splitlines()
        if len(lines) != 18:
            return False
        key = re.fullmatch(r"OWNED_KEY local/reading/p2-storage-probe/([0-9a-f-]{36})/fixture\.png", lines[4])
        if key is None:
            return False
        identity = uuid.UUID(key[1])
        if str(identity) != key[1] or identity.version != 4 or identity.variant != uuid.RFC_4122:
            return False
        initial = re.fullmatch(r"INITIAL_CREATE_AND_FRESH_PINNED_DECODE_PASS generation=([1-9][0-9]{0,18}) bytes=([1-9][0-9]{0,4}) sha256=([0-9a-f]{64})", lines[9])
        if initial is None or int(initial[1]) > 9_223_372_036_854_775_807 or int(initial[2]) > 65_536:
            return False
        return lines == ["EVIDENCE LIVE_GCS", "WORKER " + MAIN, "PROJECT " + args.project, "BUCKET " + args.bucket,
                         lines[4], "PLANNED_CREATE_ATTEMPTS 4", "RETAINED_OBJECTS_MAX 1",
                         "STATE PLANNED_NO_CLEANUP_NO_NEW_KEY_RETRY", "CREATE_MAY_HAVE_WRITTEN", lines[9],
                         "IDENTICAL_CREATE_MAY_HAVE_WRITTEN", "IDENTICAL_CONFLICT_PASS",
                         "SAME_SIZE_CREATE_MAY_HAVE_WRITTEN", "SAME_SIZE_DIFFERENT_BYTES_CONFLICT_PASS",
                         "WRONG_MIME_CREATE_MAY_HAVE_WRITTEN", "WRONG_MIME_CONFLICT_PASS",
                         "IMPOSSIBLE_GENERATION_AND_FORGED_METADATA_REJECTED_PASS", "LIVE_GCS_PASS generation=" + initial[1]]
    except (OSError, UnicodeError, ValueError):
        return False


def launch(args):
    java = Path(args.java).resolve() if args.java else Path(shutil_which("java"))
    command = [str(java), "-Xmx128m", "-XX:MaxDirectMemorySize=32m", "-Djava.awt.headless=true",
               "-cp", str(BE / "target/test-classes") + os.pathsep + str(BE / "target/classes")
               + os.pathsep + (BE / "target/reading-storage-probe.classpath").read_text().strip(), MAIN,
               "--project", args.project, "--bucket", args.bucket, "--credential", str(args.credential),
               "--receipt", str(args.receipt), "--repository", str(BE.parent)]
    environment = clean_environment()
    if not args.execute_live:
        result = supervise(command, cwd=BE, environment=environment)
        if result.status != "CHILD_EXIT" or result.returncode:
            print("DRY_RUN_REFUSED", result.status, "CHILD_EXIT_OBSERVED", result.observed_exit)
            return 2
        print("DRY_RUN_ZERO_CREDENTIAL_READS_ZERO_NETWORK LIVE_GCS_NOT_EXECUTED")
        print("PROPOSED_KEY local/reading/p2-storage-probe/<generated-UUID>/fixture.png")
        print("CREATE_ATTEMPTS 4 RETAINED_OBJECTS_MAX 1 OPERATION_MILLIS 3000 RUN_SECONDS 60")
        review_command = [sys.executable, str(Path(__file__).resolve()),
              "--project", args.project, "--bucket", args.bucket, "--credential", str(args.credential),
              "--receipt", str(args.receipt), "--execute-live"]
        if args.java:
            review_command += ["--java", str(java)]
        print("REVIEW_LIVE_COMMAND " + shlex.join(review_command))
        return 0
    with supervisor_receipt(args.receipt) as ledger:
        ledger.write("LIVE_GCS PLANNED; one retained synthetic object maximum; no cleanup/new-key retry\n")
        ledger.flush()
        os.fsync(ledger.fileno())
        result = supervise(command + ["--execute-live"], cwd=BE, environment=environment)
        ledger.write(f"{result.status} EXIT_CODE {result.returncode} CHILD_EXIT_OBSERVED {result.observed_exit}\n")
        verified = result.status == "CHILD_EXIT" and result.returncode == 0 and verified_worker_receipt(args)
        ledger.write("WORKER_RECEIPT_VERIFIED\n" if verified else "WORKER_RECEIPT_UNVERIFIED\n")
        ledger.flush()
        os.fsync(ledger.fileno())
    print("LIVE_GCS", "PASS" if verified else "FAILED",
          result.status, "CHILD_EXIT_OBSERVED", result.observed_exit, "NO_CLEANUP_NO_RETRY")
    return 0 if verified else 1


def shutil_which(name):
    import shutil
    value = shutil.which(name)
    if value is None:
        raise ValueError("RUNTIME_UNAVAILABLE")
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project")
    parser.add_argument("--bucket")
    parser.add_argument("--credential", type=Path)
    parser.add_argument("--receipt", type=Path)
    parser.add_argument("--java", help="Explicit Java 25 executable; defaults to PATH java")
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--execute-live", action="store_true")
    modes.add_argument("--prepare", action="store_true", help="Only build classes/classpath; may resolve Maven dependencies")
    args = parser.parse_args()
    try:
        if args.prepare:
            # Explicit preparation may resolve dependencies. Dry run invokes no
            # Maven process and never opens credential/receipt paths or endpoints.
            build = supervise(["bash", str(BE / "mvnw"), "-q", "-DskipTests", "test-compile",
                               "dependency:build-classpath", "-Dmdep.includeScope=test",
                               "-Dmdep.outputFile=target/reading-storage-probe.classpath"],
                              cwd=BE, seconds=180, environment=clean_environment())
            print("PREPARATION_PASS" if build.status == "CHILD_EXIT" and build.returncode == 0 else "PREPARATION_FAILED",
                  build.status, "CHILD_EXIT_OBSERVED", build.observed_exit, "LIVE_GCS_NOT_EXECUTED")
            return 0 if build.status == "CHILD_EXIT" and build.returncode == 0 else 2
        if not all((args.project, args.bucket, args.credential, args.receipt)):
            print("REFUSED_MISSING_TARGET_ARGUMENTS")
            return 2
        if not (BE / "target/reading-storage-probe.classpath").is_file():
            print("PREPARATION_REQUIRED_USE_EXPLICIT_PREPARE_MODE")
            return 2
        return launch(args)
    except (OSError, ValueError):
        print("REFUSED_LOCAL_CONFIGURATION_OR_PROCESS_START")
        return 2


if __name__ == "__main__":
    sys.exit(main())
