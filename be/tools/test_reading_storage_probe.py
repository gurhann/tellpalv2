"""Credential-free tests launch real disposable processes; no storage calls."""
import contextlib
import io
import json
import os
from pathlib import Path
import shlex
import signal
import subprocess
import sys
import tempfile
import time
import unittest
import uuid
import reading_storage_probe as probe
from reading_storage_probe import ChildResult, clean_environment, main, supervise, supervisor_receipt


class SupervisorTest(unittest.TestCase):
    def run_child(self, code, **kwargs):
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            result = supervise([sys.executable, "-c", code], cwd=Path.cwd(), **kwargs)
        self.assertEqual("", output.getvalue())
        self.assertTrue(result.observed_exit)
        return result

    def test_success_is_reaped_without_forwarding_sensitive_output(self):
        result = self.run_child("import sys; print('SECRET_TOKEN raw SDK URL'); sys.stderr.write('PRIVATE_KEY')")
        self.assertEqual(("CHILD_EXIT", 0), (result.status, result.returncode))

    def test_timeout_kills_and_reaps_real_child(self):
        with tempfile.TemporaryDirectory() as directory:
            pid_file = Path(directory) / "pid"
            result = self.run_child(f"import os,time; open({str(pid_file)!r},'w').write(str(os.getpid())); time.sleep(30)", seconds=0.3)
            self.assertEqual("WATCHDOG_TIMEOUT", result.status)
            self.assertEqual(-signal.SIGKILL, result.returncode)
            with self.assertRaises(ProcessLookupError):
                os.kill(int(pid_file.read_text()), 0)

    def test_output_overflow_stops_and_reaps_real_child(self):
        result = self.run_child("import os,time; os.write(1,b'x'*1000000); time.sleep(30)", output_limit=1024)
        self.assertEqual("OUTPUT_OVERFLOW", result.status)
        self.assertEqual(-signal.SIGKILL, result.returncode)

    def test_descendant_holding_pipes_is_terminated(self):
        result = self.run_child("import os,time; pid=os.fork(); time.sleep(30) if pid==0 else None", seconds=0.3)
        self.assertEqual("WATCHDOG_TIMEOUT", result.status)

    def test_launch_environment_cannot_inject_credentials_or_java_options(self):
        from unittest.mock import patch
        with patch.dict(os.environ, {"GOOGLE_APPLICATION_CREDENTIALS": "/never/open", "JAVA_TOOL_OPTIONS": "secret"}):
            environment = clean_environment()
        self.assertNotIn("GOOGLE_APPLICATION_CREDENTIALS", environment)
        self.assertNotIn("JAVA_TOOL_OPTIONS", environment)

    def test_supervisor_receipt_is_private_exclusive_and_retained(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory).resolve() / "receipt"
            with supervisor_receipt(path) as ledger:
                ledger.write("PLANNED")
            target = Path(str(path) + ".supervisor")
            self.assertEqual(0o600, target.stat().st_mode & 0o777)
            with self.assertRaises(FileExistsError):
                supervisor_receipt(path)
            self.assertEqual("PLANNED", target.read_text())

    def test_default_dry_run_cannot_start_maven_preparation(self):
        from unittest.mock import patch
        arguments = ["probe", "--project", "probe-local", "--bucket", "probe-bucket",
                     "--credential", "/nonexistent/private/credential", "--receipt", "/nonexistent/private/receipt"]
        with patch.object(sys, "argv", arguments), patch("reading_storage_probe.launch", return_value=0) as launch, \
                patch("reading_storage_probe.supervise") as build, patch.object(Path, "is_file", return_value=True):
            self.assertEqual(0, main())
            launch.assert_called_once()
            build.assert_not_called()

    def test_explicit_preparation_never_launches_storage_child(self):
        from unittest.mock import patch
        with patch.object(sys, "argv", ["probe", "--prepare"]), \
                patch("reading_storage_probe.launch") as launch, \
                patch("reading_storage_probe.supervise", return_value=ChildResult("CHILD_EXIT", 0, True)), \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(0, main())
            launch.assert_not_called()

    def test_selector_registration_failure_kills_and_reaps_and_restores_handler(self):
        from unittest.mock import patch
        actual_popen = subprocess.Popen
        children = []
        previous_handler = signal.getsignal(signal.SIGTERM)

        def start(*args, **kwargs):
            child = actual_popen(*args, **kwargs)
            children.append(child)
            return child

        class BrokenSelector:
            closed = False
            def register(self, *args):
                raise OSError("INJECTED_REGISTRATION_FAILURE")
            def close(self):
                self.closed = True

        selector = BrokenSelector()
        with patch("reading_storage_probe.subprocess.Popen", side_effect=start), \
                patch("reading_storage_probe.selectors.DefaultSelector", return_value=selector):
            with self.assertRaises(OSError):
                supervise([sys.executable, "-c", "import time; time.sleep(30)"], cwd=Path.cwd())
        self.assertTrue(selector.closed)
        self.assertEqual(-signal.SIGKILL, children[0].returncode)
        with self.assertRaises(ProcessLookupError):
            os.kill(children[0].pid, 0)
        self.assertIs(previous_handler, signal.getsignal(signal.SIGTERM))

    def test_signal_handler_is_restored_after_success(self):
        handler = signal.getsignal(signal.SIGTERM)
        self.run_child("pass")
        self.assertIs(handler, signal.getsignal(signal.SIGTERM))


class LauncherIntegrationTest(unittest.TestCase):
    """Run the actual main→launch→supervise path with a controlled executable, never a JVM/cloud."""
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.directory = Path(self.temporary.name).resolve()
        self.be = self.directory / "workspace" / "be"
        (self.be / "target").mkdir(parents=True)
        (self.be / "target" / "reading-storage-probe.classpath").write_text("/unavailable/classpath")
        self.private = self.directory / "private"
        self.private.mkdir(mode=0o700)
        self.credential = self.private / "never-open-credential"
        self.receipt = self.private / "worker-receipt"
        self.child_pid = self.directory / "child-pid"
        self.arguments_file = self.directory / "child-arguments"
        self.audit_file = self.directory / "subprocess-audit"
        self.executable = self.directory / "controlled-java"
        self.runner = self.directory / "run-launcher.py"

    def tearDown(self):
        self.temporary.cleanup()

    def command(self, *, live=False, module=None, forbid_receipt=False):
        module = module or Path(probe.__file__).resolve()
        forbidden = [str(self.credential)]
        if forbid_receipt:
            forbidden += [str(self.receipt), str(self.receipt) + ".supervisor"]
        self.runner.write_text(f"""
import importlib.util, os, sys
from pathlib import Path
spec = importlib.util.spec_from_file_location('probe_under_test', {str(module)!r})
probe = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = probe
spec.loader.exec_module(probe)
probe.BE = Path({str(self.be)!r})
def audit(event, arguments):
    if event == 'open' and isinstance(arguments[0], (str, bytes, os.PathLike)) and os.fsdecode(arguments[0]) in {forbidden!r}:
        raise RuntimeError('FORBIDDEN_PRIVATE_FILE_ACCESS')
    if event == 'subprocess.Popen':
        command = arguments[1]
        if any('mvnw' in str(part) or str(part) == 'bash' for part in command):
            raise RuntimeError('FORBIDDEN_MAVEN_EXECUTION')
        with open({str(self.audit_file)!r}, 'a') as ledger:
            ledger.write(str(command[0]) + '\\n')
sys.addaudithook(audit)
sys.exit(probe.main())
""")
        command = [sys.executable, str(self.runner), "--project", "probe-local", "--bucket", "probe-bucket",
                   "--credential", str(self.credential), "--receipt", str(self.receipt), "--java", str(self.executable)]
        return command + (["--execute-live"] if live else [])

    def child(self, body="", *, expect_live=False):
        self.executable.write_text(f"""#!{sys.executable}
import json, os, sys, time
from pathlib import Path
assert ('--execute-live' in sys.argv) == {expect_live!r}
Path({str(self.child_pid)!r}).write_text(str(os.getpid()))
Path({str(self.arguments_file)!r}).write_text(json.dumps(sys.argv[1:]))
print('RAW_PRIVATE_CHILD_OUTPUT_MUST_STAY_DISCARDED')
{body}
""")
        self.executable.chmod(0o700)

    def run_launcher(self, **kwargs):
        result = subprocess.run(self.command(**kwargs), capture_output=True, text=True, timeout=10)
        self.assertNotIn("RAW_PRIVATE_CHILD_OUTPUT", result.stdout + result.stderr)
        return result

    def complete_receipt(self):
        return "\n".join(["EVIDENCE LIVE_GCS", "WORKER " + probe.MAIN, "PROJECT probe-local", "BUCKET probe-bucket",
                         "OWNED_KEY local/reading/p2-storage-probe/" + str(uuid.uuid4()) + "/fixture.png",
                         "PLANNED_CREATE_ATTEMPTS 4", "RETAINED_OBJECTS_MAX 1", "STATE PLANNED_NO_CLEANUP_NO_NEW_KEY_RETRY",
                         "CREATE_MAY_HAVE_WRITTEN", "INITIAL_CREATE_AND_FRESH_PINNED_DECODE_PASS generation=17 bytes=83 sha256=" + "a" * 64,
                         "IDENTICAL_CREATE_MAY_HAVE_WRITTEN", "IDENTICAL_CONFLICT_PASS", "SAME_SIZE_CREATE_MAY_HAVE_WRITTEN",
                         "SAME_SIZE_DIFFERENT_BYTES_CONFLICT_PASS", "WRONG_MIME_CREATE_MAY_HAVE_WRITTEN", "WRONG_MIME_CONFLICT_PASS",
                         "IMPOSSIBLE_GENERATION_AND_FORGED_METADATA_REJECTED_PASS", "LIVE_GCS_PASS generation=17"]) + "\n"

    def receipt_child(self, text):
        self.child(f"descriptor = os.open({str(self.receipt)!r}, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)\n"
                   f"with os.fdopen(descriptor, 'w') as receipt:\n    receipt.write({text!r})", expect_live=True)

    def test_default_real_main_dry_run_has_no_live_flag_maven_or_private_file_reads(self):
        self.child()
        result = self.run_launcher(forbid_receipt=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("DRY_RUN_ZERO_CREDENTIAL_READS_ZERO_NETWORK", result.stdout)
        self.assertFalse(self.credential.exists())
        self.assertFalse(self.receipt.exists())
        self.assertFalse(Path(str(self.receipt) + ".supervisor").exists())
        self.assertNotIn("--execute-live", json.loads(self.arguments_file.read_text()))
        self.assertEqual([str(self.executable)], self.audit_file.read_text().splitlines())
        rendered = next(line.removeprefix("REVIEW_LIVE_COMMAND ") for line in result.stdout.splitlines()
                        if line.startswith("REVIEW_LIVE_COMMAND "))
        review = shlex.split(rendered)
        self.assertEqual(str(self.executable), review[review.index("--java") + 1])
        self.assertIn("--execute-live", review)

    def test_default_dry_run_integration_rejects_mutation_that_adds_execute_flag(self):
        self.child()
        source = Path(probe.__file__).read_text()
        original = "result = supervise(command, cwd=BE, environment=environment)"
        self.assertEqual(1, source.count(original))
        mutant = self.directory / "mutant.py"
        mutant.write_text(source.replace(original, "result = supervise(command + ['--execute-live'], cwd=BE, environment=environment)"))
        result = self.run_launcher(module=mutant, forbid_receipt=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("DRY_RUN_REFUSED", result.stdout)

    def test_sigterm_actual_launcher_kills_and_reaps_new_session_child(self):
        self.child("time.sleep(30)")
        launcher = subprocess.Popen(self.command(forbid_receipt=True), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        try:
            deadline = time.monotonic() + 5
            while not self.child_pid.exists() and time.monotonic() < deadline:
                time.sleep(0.01)
            self.assertTrue(self.child_pid.exists())
            pid = int(self.child_pid.read_text())
            launcher.terminate()
            stdout, stderr = launcher.communicate(timeout=5)
            self.assertEqual(2, launcher.returncode)
            self.assertIn("SIGNAL_TERMINATED", stdout)
            self.assertIn("CHILD_EXIT_OBSERVED True", stdout)
            self.assertNotIn("RAW_PRIVATE_CHILD_OUTPUT", stdout + stderr)
            with self.assertRaises(ProcessLookupError):
                os.kill(pid, 0)
        finally:
            if launcher.poll() is None:
                launcher.kill()
                launcher.wait()
            if self.child_pid.exists():
                try:
                    os.kill(int(self.child_pid.read_text()), signal.SIGKILL)
                except ProcessLookupError:
                    pass

    def test_exit_zero_without_worker_receipt_cannot_claim_live_pass(self):
        self.child(expect_live=True)
        result = self.run_launcher(live=True)
        self.assertEqual(1, result.returncode)
        self.assertIn("LIVE_GCS FAILED", result.stdout)
        self.assertNotIn("LIVE_GCS PASS", result.stdout)

    def test_partial_worker_receipt_cannot_claim_live_pass(self):
        self.receipt_child(self.complete_receipt().split("IDENTICAL_CONFLICT_PASS")[0])
        result = self.run_launcher(live=True)
        self.assertEqual(1, result.returncode)
        self.assertNotIn("LIVE_GCS PASS", result.stdout)

    def test_forged_worker_identity_or_key_scope_cannot_claim_live_pass(self):
        complete = self.complete_receipt()
        for changed in [complete.replace("WORKER " + probe.MAIN, "WORKER forged"),
                        complete.replace("PROJECT probe-local", "PROJECT other-project"),
                        complete.replace("BUCKET probe-bucket", "BUCKET other-bucket"),
                        complete.replace("local/reading/p2-storage-probe/", "prod/reading/p2-storage-probe/")]:
            with self.subTest(identity=changed.splitlines()[1:4]):
                self.receipt_child(changed)
                result = self.run_launcher(live=True)
                self.assertEqual(1, result.returncode)
                self.assertNotIn("LIVE_GCS PASS", result.stdout)
                self.assertNotIn("WORKER forged", result.stdout + result.stderr)
                self.receipt.unlink()
                Path(str(self.receipt) + ".supervisor").unlink()

    def test_complete_private_synthetic_worker_receipt_accepts_exit_zero(self):
        self.receipt_child(self.complete_receipt())
        result = self.run_launcher(live=True)
        self.assertEqual(0, result.returncode)
        self.assertIn("LIVE_GCS PASS", result.stdout)
        self.assertIn("WORKER_RECEIPT_VERIFIED", Path(str(self.receipt) + ".supervisor").read_text())

    def test_existing_worker_receipt_cannot_be_reused_by_exit_zero_child(self):
        self.receipt.write_text(self.complete_receipt())
        self.receipt.chmod(0o600)
        self.child(expect_live=True)
        result = self.run_launcher(live=True)
        self.assertEqual(2, result.returncode)
        self.assertFalse(self.child_pid.exists())


if __name__ == "__main__":
    unittest.main()
