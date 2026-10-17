"""P3 launcher'ın yan etki ve kapsam korumaları."""
import contextlib
import io
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import p3_local_cms as tool


class P3LocalCmsTest(unittest.TestCase):
    def test_dry_run_reads_no_environment_or_credentials_and_starts_no_process(self):
        with patch.object(tool, "load_environment", side_effect=AssertionError("read")), \
                patch.object(tool.subprocess, "run", side_effect=AssertionError("process")), \
                patch.object(Path, "read_text", side_effect=AssertionError("file")), \
                patch.object(tool.socket, "socket", side_effect=AssertionError("network")), \
                contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(tool.main(["--env-file", "/missing/secret"]), 0)
        self.assertIn("ZERO_CREDENTIAL_READS_ZERO_NETWORK_ZERO_DB_WRITES", output.getvalue())

    def test_remote_database_production_bucket_and_ambiguous_configuration_are_rejected(self):
        for environment in ({"TELLPAL_DB_URL": "jdbc:postgresql://remote/db"},
                            {"TELLPAL_DB_URL": "jdbc:postgresql://localhost/db?host=remote"},
                            {"SPRING_PROFILES_ACTIVE": "local,prod"},
                            {"SPRING_PROFILES_INCLUDE": "prod"},
                            {"TELLPAL_ASSET_STORAGE_FIREBASE_BUCKET_NAME": "other"},
                            {"SPRING_APPLICATION_JSON": "{}"},
                            {"JAVA_TOOL_OPTIONS": "-Dspring.profiles.active=prod"}):
            with self.subTest(environment=environment), self.assertRaises(ValueError):
                tool.validate_environment(environment)

    def test_overlay_contains_only_explicit_synthetic_limits_and_all_three_roles(self):
        result = tool.overlay("local/p3-cms-12345678-1234-4234-8234-123456789abc")
        for role in ("READING_COVER", "READING_DETAIL", "READING_PAGE"):
            self.assertIn(f"profiles.{role}.width=40", result)
            self.assertIn(f"profiles.{role}.max-output-bytes=1000000", result)
            self.assertIn(f"profiles.{role}.allow-growth=true", result)
        self.assertIn("tellpal.asset.processing.poller.enabled=false", result)
        self.assertNotIn("datasource", result)
        self.assertNotIn("jwt", result)
        self.assertNotIn("cors", result)

    def test_prefix_scope_rejects_other_and_nonrandom_identity(self):
        for prefix in ("prod/p3-cms-12345678-1234-4234-8234-123456789abc", "local/other",
                       "local/p3-cms-12345678-1234-1234-8234-123456789abc"):
            with self.assertRaises(ValueError):
                tool.overlay(prefix)

    def test_machine_evidence_is_ascii_and_matches_java_and_each_overlay_role(self):
        self.assertTrue(tool.EVIDENCE.isascii())
        java_source = (tool.BE / "src/test/java/com/tellpal/v2/support/P3LocalCmsSetup.java").read_text()
        self.assertIn('static final String EVIDENCE = "' + tool.EVIDENCE + '";', java_source)
        result = tool.overlay("local/p3-cms-12345678-1234-4234-8234-123456789abc")
        self.assertTrue(result.isascii())
        for role in ("READING_COVER", "READING_DETAIL", "READING_PAGE"):
            self.assertIn(f"profiles.{role}.growth-evidence={tool.EVIDENCE}\n", result)

    def test_existing_attempt_cannot_be_overwritten_or_restarted(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary).resolve()
            (directory / "attempt.properties").write_text("state=FAILED\n")
            with patch.object(tool, "DIRECTORY", directory), patch.object(tool, "java_command", return_value=["java"]), \
                    patch.object(tool, "require_free_port"), \
                    patch.object(tool.subprocess, "run", side_effect=AssertionError("process")):
                with self.assertRaises(FileExistsError):
                    tool.setup({}, None)
            self.assertEqual((directory / "attempt.properties").read_text(), "state=FAILED\n")

    def test_dotenv_is_parsed_without_shell_execution_and_inherited_values_win(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / ".env"
            path.write_text("TELLPAL_DB_PASSWORD='$(touch never)'\nPORT=8080\n")
            with patch.dict(os.environ, {"PORT": "8081"}, clear=True):
                environment = tool.load_environment(path)
            self.assertEqual(environment["TELLPAL_DB_PASSWORD"], "$(touch never)")
            self.assertEqual(environment["PORT"], "8081")

    def test_busy_port_rejects_start_without_killing_or_starting_process(self):
        with patch.object(tool, "private_directory"), patch.object(tool, "receipt_values", return_value={"databaseUrl": tool.DEFAULT_DATABASE_URL}), \
                patch.object(tool, "require_free_port", side_effect=ValueError("PORT_BUSY_IDENTIFY_EXISTING_PROCESS")), \
                patch.object(tool.subprocess, "Popen", side_effect=AssertionError("new process")):
            with self.assertRaisesRegex(ValueError, "PORT_BUSY_IDENTIFY_EXISTING_PROCESS"):
                tool.start({}, None)

    def test_scoped_child_environment_disables_legacy_poller_and_pins_unique_prefix(self):
        environment = {"TELLPAL_ASSET_PROCESSING_POLLER_ENABLED": "true",
                       "TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX": "local",
                       "TELLPAL_DB_URL": "jdbc:postgresql://localhost/tellpal_v2",
                       "TELLPAL_ADMIN_JWT_SECRET": "synthetic-test-secret",
                       "TELLPAL_ADMIN_CORS_ALLOWED_ORIGINS": "http://localhost:5173"}
        prefix = "local/p3-cms-12345678-1234-4234-8234-123456789abc"
        scoped = tool.scoped_environment(environment, prefix)
        self.assertEqual(scoped["TELLPAL_ASSET_PROCESSING_POLLER_ENABLED"], "false")
        self.assertEqual(scoped["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"], prefix)
        self.assertEqual(environment["TELLPAL_ASSET_PROCESSING_POLLER_ENABLED"], "true")
        self.assertEqual(environment["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"], "local")
        for name in ("TELLPAL_DB_URL", "TELLPAL_ADMIN_JWT_SECRET", "TELLPAL_ADMIN_CORS_ALLOWED_ORIGINS"):
            self.assertEqual(scoped[name], environment[name])

    def test_preflight_forwards_scoped_environment_to_real_child_command(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary).resolve()
            prefix = "local/p3-cms-12345678-1234-4234-8234-123456789abc"
            (directory / "application-p3.properties").write_text(tool.overlay(prefix))
            environment = {"TELLPAL_ASSET_PROCESSING_POLLER_ENABLED": "true",
                           "TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX": "prod"}
            with patch.object(tool, "DIRECTORY", directory), patch.object(tool, "java_command", return_value=["java"]), \
                    patch.object(tool.subprocess, "run") as run, contextlib.redirect_stdout(io.StringIO()):
                run.return_value.returncode = 0
                self.assertEqual(tool.preflight(environment, None), 0)
            self.assertEqual(run.call_args.kwargs["env"]["TELLPAL_ASSET_PROCESSING_POLLER_ENABLED"], "false")
            self.assertEqual(run.call_args.kwargs["env"]["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"], prefix)


    def test_setup_and_start_forward_scope_without_changing_original_environment(self):
        prefix = "local/p3-cms-12345678-1234-4234-8234-123456789abc"
        environment = {"TELLPAL_ASSET_PROCESSING_POLLER_ENABLED": "true",
                       "TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX": "local"}
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary).resolve()
            (directory / "application-p3.properties").write_text(tool.overlay(prefix))
            with patch.object(tool, "DIRECTORY", directory), patch.object(tool, "java_command", return_value=["java"]), \
                    patch.object(tool, "preflight", return_value=0), patch.object(tool, "require_free_port"), \
                    patch.object(tool.subprocess, "run") as run, patch.object(tool.subprocess, "Popen") as popen, \
                    patch.object(tool, "receipt_values", return_value={"prefix": prefix, "databaseUrl": tool.DEFAULT_DATABASE_URL}), \
                    contextlib.redirect_stdout(io.StringIO()):
                run.return_value.returncode = 0
                popen.return_value.pid = 1234
                self.assertEqual(tool.setup(environment, None), 0)
                self.assertEqual(tool.start(environment, None), 0)
            for process in (run, popen):
                self.assertEqual(process.call_args.kwargs["env"]["TELLPAL_ASSET_PROCESSING_POLLER_ENABLED"], "false")
                self.assertEqual(process.call_args.kwargs["env"]["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"], prefix)
            self.assertEqual(environment["TELLPAL_ASSET_PROCESSING_POLLER_ENABLED"], "true")
            self.assertEqual(environment["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"], "local")


    def test_foreground_start_waits_for_owned_child_and_keeps_private_log(self):
        prefix = "local/p3-cms-12345678-1234-4234-8234-123456789abc"
        environment = {"TELLPAL_ASSET_PROCESSING_POLLER_ENABLED": "true"}
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary).resolve()
            with patch.object(tool, "DIRECTORY", directory), patch.object(tool, "java_command", return_value=["java"]), \
                    patch.object(tool, "require_free_port"), patch.object(tool, "receipt_values", return_value={"prefix": prefix, "databaseUrl": tool.DEFAULT_DATABASE_URL}), \
                    patch.object(tool.subprocess, "run") as run, \
                    patch.object(tool.subprocess, "Popen", side_effect=AssertionError("detached process")), \
                    contextlib.redirect_stdout(io.StringIO()) as console:
                run.return_value.returncode = 0
                self.assertEqual(tool.start(environment, None, foreground=True), 0)
            self.assertEqual(run.call_args.args[0][0], "java")
            self.assertIn("--spring.datasource.url=" + tool.DEFAULT_DATABASE_URL, run.call_args.args[0])
            self.assertNotIn("start_new_session", run.call_args.kwargs)
            self.assertEqual(run.call_args.kwargs["env"]["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"], prefix)
            self.assertEqual(Path(run.call_args.kwargs["stdout"].name), directory / "backend.log")
            self.assertEqual((directory / "backend.log").stat().st_mode & 0o777, 0o600)
            self.assertFalse((directory / "backend.pid").exists())
            self.assertIn("BACKEND_FOREGROUND_START_REQUESTED", console.getvalue())

    def test_foreground_option_routes_only_to_start_without_real_process(self):
        with patch.object(tool, "load_environment", return_value={}), patch.object(tool, "start", return_value=0) as start:
            self.assertEqual(tool.main(["--start", "--foreground"]), 0)
        start.assert_called_once_with({}, None, True)
        with patch.object(tool, "load_environment", side_effect=AssertionError("environment read")), \
                contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            tool.main(["--foreground"])


    def test_missing_env_file_fails_closed_before_any_process_or_default_fallback(self):
        with tempfile.TemporaryDirectory() as temporary, patch.dict(os.environ, {}, clear=True):
            missing = Path(temporary) / "missing.env"
            with self.assertRaisesRegex(ValueError, "ENV_FILE_REQUIRED"):
                tool.load_environment(missing)
            with patch.object(tool, "BE", Path(temporary)), patch.object(tool, "prepare", side_effect=AssertionError("fallback")), \
                    contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(tool.main(["--prepare"]), 2)
                self.assertEqual(tool.main(["--prepare", "--env-file", str(missing)]), 2)

    def test_embedded_and_quoted_hash_values_are_preserved_and_inline_comments_rejected(self):
        with tempfile.TemporaryDirectory() as temporary, patch.dict(os.environ, {}, clear=True):
            path = Path(temporary) / ".env"
            path.write_text("# tam satır yorum\nBARE=abc#def\nSINGLE='abc#def'\nDOUBLE=\"abc # def\"\n")
            values = tool.load_environment(path)
            self.assertEqual(values["BARE"], "abc#def")
            self.assertEqual(values["SINGLE"], "abc#def")
            self.assertEqual(values["DOUBLE"], "abc # def")
            for raw in ("VALUE=abc # yorum\n", "VALUE=   # yorum\n", "VALUE='abc' # yorum\n"):
                path.write_text(raw)
                with self.assertRaises(ValueError):
                    tool.load_environment(path)

    def test_start_rejects_other_local_database_or_unbound_receipt_before_jvm(self):
        for receipt, environment in (({"databaseUrl": "jdbc:postgresql://localhost:5432/other_db"}, {}),
                                     ({"databaseUrl": tool.DEFAULT_DATABASE_URL}, {"TELLPAL_DB_URL": "jdbc:postgresql://localhost:5432/other_db"}),
                                     ({}, {})):
            with patch.object(tool, "private_directory"), patch.object(tool, "receipt_values", return_value=receipt), \
                    patch.object(tool.subprocess, "Popen", side_effect=AssertionError("JVM started")), \
                    patch.object(tool.subprocess, "run", side_effect=AssertionError("JVM started")), \
                    patch.object(tool, "require_free_port", side_effect=AssertionError("port probed")):
                with self.assertRaisesRegex(ValueError, "RECEIPT_DATABASE_MISMATCH"):
                    tool.start(environment, None)


if __name__ == "__main__":
    unittest.main()
