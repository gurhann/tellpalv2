"""Synthetic operator-login admission, transport, privacy and cleanup checks."""
from contextlib import redirect_stderr, redirect_stdout
from copy import deepcopy
from io import StringIO
import json
import multiprocessing
import os
from pathlib import Path
import ssl
import tempfile
import time
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

import native_entry_config as config
import native_test_login as login


def synthetic_success_worker(inputs, sender):
    os.setsid()
    sender.send_bytes(config.frozen_json({"idToken": "synthetic.id.token"}))
    sender.close()


def synthetic_waiting_worker(inputs, sender):
    os.setsid()
    time.sleep(30)


class LoginTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="native-operator-login-test-")
        self.addCleanup(temporary.cleanup)
        self.base = Path(temporary.name).resolve()
        self.base.chmod(0o700)
        self.uid = config.canonical_uid()
        self.admin = {"type": "service_account", "project_id": "synthetic-project",
            "client_email": "synthetic-service@synthetic-project.iam.gserviceaccount.com",
            "private_key": "synthetic-private-key-never-used", "token_uri": login.TOKEN_URI,
            "auth_uri": login.AUTH_URI, "universe_domain": "googleapis.com"}
        self.api = {"schemaVersion": 1, "firebaseProjectId": "synthetic-project",
            "webApiKey": "synthetic_api_key_not_real"}
        self.admin_path = self.file("admin.json", config.frozen_json(self.admin))
        self.api_path = self.file("api.json", config.frozen_json(self.api))
        self.output = self.base / "verified-token"
        self.token = "synthetic.id.token"
        self.refresh = "synthetic-refresh-SECRET"
        self.user = SimpleNamespace(uid=self.uid, disabled=False)
        self.claims = {"uid": self.uid, "sub": self.uid, "aud": "synthetic-project",
            "iss": "https://securetoken.google.com/synthetic-project", "exp": int(time.time()) + 3600}
        self.client = Mock()
        self.client.get_user.return_value = self.user
        self.client.create_custom_token.return_value = b"synthetic.custom.token"
        self.client.verify_id_token.return_value = self.claims
        self.factory = Mock(return_value=self.client)
        self.exchange = Mock(side_effect=lambda *args: self.response())

    def file(self, name, data):
        path = self.base / name
        path.write_bytes(data)
        path.chmod(0o600)
        return path

    def response(self):
        return {"idToken": self.token, "refreshToken": self.refresh, "expiresIn": "3600"}

    def inputs(self):
        return login.load_inputs(self.admin_path, self.api_path, self.output)

    def obtain(self):
        return login.obtain_verified_token(self.inputs(), auth_factory=self.factory, exchange=self.exchange)

    def test_existing_canonical_identity_is_checked_before_claimless_exchange_and_fresh_verification(self):
        self.assertEqual(self.obtain(), self.token)
        self.client.get_user.assert_called_once_with(self.uid)
        self.client.create_custom_token.assert_called_once_with(self.uid)
        self.client.verify_id_token.assert_called_once_with(self.token, check_revoked=True)
        self.client.close.assert_called_once_with()
        self.assertEqual(self.exchange.call_args.args[:2], (self.api["webApiKey"], "synthetic.custom.token"))
        self.assertEqual([call[0] for call in self.client.method_calls],
            ["get_user", "create_custom_token", "verify_id_token", "close"])
        self.assertFalse(self.output.exists())

    def test_missing_account_refuses_before_mint_or_exchange_and_sanitizes_sdk_error(self):
        self.client.get_user.side_effect = RuntimeError("SECRET account missing")
        with self.assertRaisesRegex(config.EntryError, "^login_account_unavailable$"):
            self.obtain()
        self.client.create_custom_token.assert_not_called()
        self.exchange.assert_not_called()
        self.client.close.assert_called_once_with()

    def test_disabled_or_wrong_account_refuses_before_mint_or_exchange(self):
        for user, reason in [(SimpleNamespace(uid=self.uid, disabled=True), "login_account_disabled"),
            (SimpleNamespace(uid="different-synthetic-user", disabled=False), "login_identity_rejected")]:
            with self.subTest(reason=reason):
                self.client.get_user.return_value = user
                with self.assertRaisesRegex(config.EntryError, "^" + reason + "$"):
                    self.obtain()
        self.client.create_custom_token.assert_not_called()
        self.exchange.assert_not_called()

    def test_project_mismatch_refuses_before_any_auth_call(self):
        self.admin["project_id"] = "other-project"
        self.admin_path.write_bytes(config.frozen_json(self.admin))
        with self.assertRaisesRegex(config.EntryError, "^login_project_rejected$"):
            self.obtain()
        self.factory.assert_not_called()

    def test_service_account_type_google_token_endpoint_universe_and_selected_email_reject(self):
        for field, value in [("type", "authorized_user"), ("token_uri", "https://example.invalid/token"),
            ("auth_uri", "https://example.invalid/auth"), ("universe_domain", "example.invalid"),
            ("client_email", "other@other-project.iam.gserviceaccount.com")]:
            candidate = dict(self.admin, **{field: value})
            self.admin_path.write_bytes(config.frozen_json(candidate))
            with self.subTest(field=field), self.assertRaisesRegex(config.EntryError, "^login_credential_rejected$"):
                self.inputs()

    def test_api_duplicate_unknown_trailing_nonfinite_and_boolean_schema_reject(self):
        for data in (b'{"schemaVersion":1,"schemaVersion":1}', b"{} {}", b'{"schemaVersion":NaN}',
            config.frozen_json(dict(self.api, extra="SECRET")), config.frozen_json(dict(self.api, schemaVersion=True))):
            self.api_path.write_bytes(data)
            with self.subTest(data=data), self.assertRaises(config.EntryError):
                self.inputs()

    def test_private_input_rejects_public_permissions_directory_repository_and_symlink(self):
        for path in (self.admin_path, self.api_path):
            path.chmod(0o644)
            with self.assertRaisesRegex(config.EntryError, "^file_not_private$"):
                self.inputs()
            path.chmod(0o600)
        link = self.base / "admin-link"
        link.symlink_to(self.admin_path)
        with self.assertRaisesRegex(config.EntryError, "^symlink_rejected$"):
            login.load_inputs(link, self.api_path, self.output)
        with self.assertRaises(config.EntryError):
            login.load_inputs(self.base, self.api_path, self.output)
        with self.assertRaisesRegex(config.EntryError, "^repository_path_rejected$"):
            login.load_inputs(config.ROOT / "tools/mobile-test-account.json", self.api_path, self.output)

    def test_output_refuses_existing_input_overlap_symlink_and_nonprivate_parent(self):
        original = self.admin_path.read_bytes()
        with self.assertRaises(config.EntryError):
            login.load_inputs(self.admin_path, self.api_path, self.admin_path)
        self.assertEqual(self.admin_path.read_bytes(), original)
        with self.assertRaisesRegex(config.EntryError, "^repository_path_rejected$"):
            login.load_inputs(self.admin_path, self.api_path, config.ROOT / "never-create-token")
        link = self.base / "output-link"
        link.symlink_to(self.output)
        with self.assertRaisesRegex(config.EntryError, "^symlink_rejected$"):
            login.load_inputs(self.admin_path, self.api_path, link)
        self.base.chmod(0o755)
        with self.assertRaisesRegex(config.EntryError, "^output_parent_not_private$"):
            self.inputs()

    def test_ancestor_symlinks_are_refused_for_inputs_and_output(self):
        linked = self.base / "ancestor"
        linked.symlink_to(self.base, target_is_directory=True)
        with self.assertRaisesRegex(config.EntryError, "^symlink_rejected$"):
            login.load_inputs(linked / self.admin_path.name, self.api_path, self.output)
        with self.assertRaisesRegex(config.EntryError, "^symlink_rejected$"):
            login.load_inputs(self.admin_path, self.api_path, linked / "output")

    def test_all_verified_identity_fields_and_expiry_are_authoritative(self):
        for field, value in [("uid", "wrong"), ("sub", "wrong"), ("aud", "wrong-project"),
            ("iss", "https://example.invalid"), ("exp", 1), ("exp", True)]:
            self.client.verify_id_token.return_value = dict(self.claims, **{field: value})
            with self.subTest(field=field, value=value), self.assertRaises(config.EntryError):
                self.obtain()
        self.assertFalse(self.output.exists())

    def test_verification_exception_is_sanitized_and_does_not_write_token(self):
        self.client.verify_id_token.side_effect = RuntimeError("SECRET token body")
        with self.assertRaisesRegex(config.EntryError, "^login_auth_refused$"):
            self.obtain()
        self.assertFalse(self.output.exists())

    def test_rest_wrong_identity_or_new_account_indicator_refuses_before_verification(self):
        for changed in ({"localId": "wrong"}, {"isNewUser": True}, {"isNewUser": "false"}):
            self.exchange.side_effect = lambda *args: dict(self.response(), **changed)
            with self.subTest(changed=changed), self.assertRaisesRegex(config.EntryError, "^login_identity_rejected$"):
                self.obtain()
        self.client.verify_id_token.assert_not_called()

    def test_optional_rest_identity_fields_may_be_absent_or_exact(self):
        self.exchange.side_effect = lambda *args: dict(self.response(), localId=self.uid, isNewUser=False)
        self.assertEqual(self.obtain(), self.token)

    def test_invalid_tokens_and_rest_response_shapes_refuse(self):
        for token in ("", "bad\ntoken", "nonascii-ç", "x" * (login.MAX_TOKEN_BYTES + 1), True):
            self.exchange.side_effect = lambda *args: dict(self.response(), idToken=token)
            with self.subTest(token=type(token).__name__), self.assertRaises(config.EntryError):
                self.obtain()
        for response in ([], {"idToken": self.token}, dict(self.response(), unknown="SECRET"),
            dict(self.response(), expiresIn=True), dict(self.response(), refreshToken=[])):
            self.exchange.side_effect = lambda *args: deepcopy(response)
            with self.subTest(response=type(response).__name__), self.assertRaises(config.EntryError):
                self.obtain()

    def test_private_output_contains_only_id_token_and_never_overwrites(self):
        inputs = self.inputs()
        token = self.obtain()
        login.write_token(inputs, token)
        self.assertEqual(config.private_file(self.output), (self.token + "\n").encode())
        self.assertEqual(self.output.stat().st_mode & 0o777, 0o600)
        self.assertNotIn(self.refresh.encode(), self.output.read_bytes())
        with self.assertRaisesRegex(config.EntryError, "^output_not_unique$"):
            login.write_token(inputs, "replacement")
        self.assertEqual(self.output.read_bytes(), (self.token + "\n").encode())

    def test_failed_write_removes_only_owned_partial_output_and_preserves_inputs(self):
        inputs = self.inputs()
        admin_before, api_before = self.admin_path.read_bytes(), self.api_path.read_bytes()
        with patch.object(login.os, "fsync", side_effect=OSError("SECRET disk failure")):
            with self.assertRaisesRegex(config.EntryError, "^login_output_failed$"):
                login.write_token(inputs, self.token)
        self.assertFalse(self.output.exists())
        self.assertEqual(self.admin_path.read_bytes(), admin_before)
        self.assertEqual(self.api_path.read_bytes(), api_before)

    def test_failed_output_permission_setup_removes_created_inode_and_preserves_inputs(self):
        inputs = self.inputs()
        admin_before, api_before = self.admin_path.read_bytes(), self.api_path.read_bytes()
        with patch.object(login.os, "fchmod", side_effect=OSError("SECRET permission failure")):
            with self.assertRaisesRegex(config.EntryError, "^login_output_failed$"):
                login.write_token(inputs, self.token)
        self.assertFalse(self.output.exists())
        self.assertEqual(self.admin_path.read_bytes(), admin_before)
        self.assertEqual(self.api_path.read_bytes(), api_before)

    def test_write_failure_does_not_remove_a_replacement_inode(self):
        def replace_then_fail(descriptor):
            self.output.unlink()
            self.output.write_bytes(b"synthetic independent replacement")
            raise OSError("SECRET write failure")
        with patch.object(login.os, "fsync", side_effect=replace_then_fail):
            with self.assertRaisesRegex(config.EntryError, "^login_output_failed$"):
                login.write_token(self.inputs(), self.token)
        self.assertEqual(self.output.read_bytes(), b"synthetic independent replacement")

    def test_repr_and_cli_success_do_not_export_identity_project_keys_or_tokens(self):
        self.assertNotIn("synthetic", repr(self.inputs()))
        stdout = StringIO()
        with patch.object(login, "login_in_worker", return_value=self.token), redirect_stdout(stdout):
            result = login.main(["--admin-credential", str(self.admin_path), "--web-api-config", str(self.api_path),
                "--id-token-output", str(self.output)])
        self.assertEqual(result, 0)
        body = json.loads(stdout.getvalue())
        self.assertEqual(body["status"], "completed")
        self.assertEqual(body["nativeEntryGate"], "open")
        for secret in (self.token, self.refresh, self.api["webApiKey"], self.uid, self.api["firebaseProjectId"], str(self.base)):
            self.assertNotIn(secret, stdout.getvalue())

    def test_cli_failure_and_parser_do_not_echo_private_input_or_sdk_message(self):
        stdout = StringIO()
        with patch.object(login, "login_in_worker", side_effect=RuntimeError("SECRET credentials")), redirect_stdout(stdout):
            result = login.main(["--admin-credential", str(self.admin_path), "--web-api-config", str(self.api_path),
                "--id-token-output", str(self.output)])
        self.assertEqual(result, 4)
        self.assertNotIn("SECRET", stdout.getvalue())
        self.assertFalse(self.output.exists())
        stderr = StringIO()
        with redirect_stderr(stderr), self.assertRaises(SystemExit):
            login.main(["--SECRET-PRIVATE-INPUT"])
        self.assertEqual(stderr.getvalue(), "login_cli_rejected\n")


class TransportTests(unittest.TestCase):
    def connection(self, response):
        connection = Mock()
        connection.getresponse.return_value = response
        factory = Mock(return_value=connection)
        return connection, factory

    def response(self, *, status=200, data=None, headers=None):
        data = b'{"idToken":"synthetic.id.token","refreshToken":"synthetic.refresh","expiresIn":"3600"}' if data is None else data
        response = Mock(status=status)
        response.getheaders.return_value = [("Content-Type", "application/json"), ("Content-Length", str(len(data)))] if headers is None else headers
        response.read.return_value = data
        return response

    def exchange(self, factory):
        return login.exchange_custom_token("synthetic_api_key", "synthetic.custom.token",
            time.monotonic() + login.REQUEST_SECONDS, connection_factory=factory)

    def test_fixed_google_https_standard_tls_and_body_only_credentials(self):
        connection, factory = self.connection(self.response())
        result = self.exchange(factory)
        self.assertEqual(result["idToken"], "synthetic.id.token")
        self.assertEqual(factory.call_args.args, (login.API_HOST,))
        context = factory.call_args.kwargs["context"]
        self.assertTrue(context.check_hostname)
        self.assertEqual(context.verify_mode, ssl.CERT_REQUIRED)
        self.assertLessEqual(factory.call_args.kwargs["timeout"], 10)
        call = connection.request.call_args
        self.assertEqual(call.args, ("POST", "/v1/accounts:signInWithCustomToken?key=synthetic_api_key"))
        self.assertEqual(json.loads(call.kwargs["body"]), {"token": "synthetic.custom.token", "returnSecureToken": True})
        self.assertNotIn("Authorization", call.kwargs["headers"])
        connection.close.assert_called_once_with()

    def test_all_redirects_refuse_without_followup_or_body_read(self):
        for status in (301, 302, 303, 307, 308):
            response = self.response(status=status, headers=[("Location", "https://example.invalid/secret")])
            connection, factory = self.connection(response)
            with self.subTest(status=status), self.assertRaisesRegex(config.EntryError, "^login_redirect_refused$"):
                self.exchange(factory)
            response.read.assert_not_called()
            connection.request.assert_called_once()
            connection.close.assert_called_once_with()

    def test_http_errors_never_parse_or_echo_server_body(self):
        response = self.response(status=400, data=b"SECRET token error")
        connection, factory = self.connection(response)
        with self.assertRaisesRegex(config.EntryError, "^login_exchange_refused$"):
            self.exchange(factory)
        response.read.assert_not_called()
        connection.close.assert_called_once_with()

    def test_response_type_duplicate_headers_length_encoding_size_and_json_bounds(self):
        responses = [self.response(headers=[("Content-Type", "text/html")]),
            self.response(headers=[("Content-Type", "application/json"), ("Content-Type", "application/json")]),
            self.response(headers=[("Content-Type", "application/json"), ("Content-Length", "999999")]),
            self.response(headers=[("Content-Type", "application/json"), ("Content-Length", "1")]),
            self.response(headers=[("Content-Type", "application/json"), ("Content-Encoding", "gzip")]),
            self.response(data=b"x" * (login.MAX_RESPONSE_BYTES + 1), headers=[("Content-Type", "application/json")]),
            self.response(data=b'{"idToken":"a","idToken":"b"}'), self.response(data=b"{} {}")]
        for response in responses:
            connection, factory = self.connection(response)
            with self.subTest(response=response), self.assertRaises(config.EntryError):
                self.exchange(factory)
            connection.close.assert_called_once_with()

    def test_elapsed_deadline_refuses_before_connection(self):
        factory = Mock()
        with self.assertRaisesRegex(config.EntryError, "^login_timeout$"):
            login.exchange_custom_token("synthetic", "synthetic", time.monotonic() - 1, connection_factory=factory)
        factory.assert_not_called()


class WorkerTests(unittest.TestCase):
    def tracked_spawn_context(self):
        context = multiprocessing.get_context("spawn")
        children = []
        def process(*args, **kwargs):
            child = context.Process(*args, **kwargs)
            children.append(child)
            return child
        return SimpleNamespace(Pipe=context.Pipe, Process=process), children

    def fake_context(self, payload, *, ready=True):
        receiver, sender, process = Mock(), Mock(), Mock()
        receiver.poll.return_value = ready
        receiver.recv_bytes.return_value = config.frozen_json(payload)
        context = Mock()
        context.Pipe.return_value = (receiver, sender)
        context.Process.return_value = process
        return context, receiver, sender, process

    def test_parent_only_accepts_bounded_token_payload_and_closes_owned_worker(self):
        context, receiver, sender, process = self.fake_context({"idToken": "synthetic.id.token"})
        with patch.object(login.multiprocessing, "get_context", return_value=context), patch.object(login, "stop_worker") as stop:
            self.assertEqual(login.login_in_worker(None), "synthetic.id.token")
        receiver.poll.assert_called_once_with(10)
        receiver.recv_bytes.assert_called_once_with(login.MAX_TOKEN_BYTES + 256)
        sender.close.assert_called()
        receiver.close.assert_called_once_with()
        stop.assert_called_once_with(process)

    def test_timeout_and_unknown_worker_errors_never_become_success(self):
        for payload, ready, reason in [({"reason": "SECRET sdk text"}, True, "login_worker_failed"),
            ({"idToken": "synthetic"}, False, "login_timeout"), ({"idToken": "", "extra": True}, True, "login_worker_failed")]:
            context, receiver, sender, process = self.fake_context(payload, ready=ready)
            with patch.object(login.multiprocessing, "get_context", return_value=context), patch.object(login, "stop_worker") as stop:
                with self.subTest(reason=reason), self.assertRaisesRegex(config.EntryError, "^" + reason + "$"):
                    login.login_in_worker(None)
            stop.assert_called_once_with(process)

    def test_cleanup_failure_withholds_successful_worker_token_from_output(self):
        context, receiver, sender, process = self.fake_context({"idToken": "synthetic.id.token"})
        stdout = StringIO()
        with patch.object(login.multiprocessing, "get_context", return_value=context), \
            patch.object(login, "stop_worker", side_effect=config.EntryError("login_cleanup_failed")), \
            patch.object(login, "load_inputs", return_value=None), \
            patch.object(login, "write_token") as write, redirect_stdout(stdout):
            result = login.main(["--admin-credential", "/synthetic/admin.json", "--web-api-config",
                "/synthetic/api.json", "--id-token-output", "/synthetic/token"])
        self.assertEqual(result, 4)
        write.assert_not_called()
        self.assertEqual(json.loads(stdout.getvalue())["reason"], "login_cleanup_failed")
        self.assertNotIn("synthetic.id.token", stdout.getvalue())

    def test_setsid_race_uses_pid_kill_fallback_and_bounded_join(self):
        process = Mock(pid=314159)
        process.is_alive.side_effect = [True, False]
        with patch.object(login.os, "killpg", side_effect=ProcessLookupError):
            login.stop_worker(process)
        process.kill.assert_called_once_with()
        process.join.assert_called_once_with(timeout=2)

    def test_worker_discards_emulator_adc_proxy_and_secret_environment_before_auth(self):
        sender = Mock()
        def observe(inputs):
            for name in ("FIREBASE_AUTH_EMULATOR_HOST", "GOOGLE_APPLICATION_CREDENTIALS", "HTTPS_PROXY", "SYNTHETIC_SECRET"):
                self.assertNotIn(name, os.environ)
            return "synthetic.id.token"
        with patch.dict(os.environ, {"FIREBASE_AUTH_EMULATOR_HOST": "localhost:9099",
            "GOOGLE_APPLICATION_CREDENTIALS": "/synthetic/private.json", "HTTPS_PROXY": "https://example.invalid",
            "SYNTHETIC_SECRET": "never-export"}), patch.object(login.os, "setsid"), patch.object(login.os, "dup2"), \
            patch.object(login, "obtain_verified_token", side_effect=observe):
            login._login_worker(None, sender)
        self.assertEqual(json.loads(sender.send_bytes.call_args.args[0]), {"idToken": "synthetic.id.token"})

    def test_worker_exception_details_are_fixed_before_ipc(self):
        sender = Mock()
        with patch.dict(os.environ), patch.object(login.os, "setsid"), patch.object(login.os, "dup2"), \
            patch.object(login, "obtain_verified_token", side_effect=config.EntryError("SECRET-PRIVATE-DETAIL")):
            login._login_worker(None, sender)
        self.assertEqual(json.loads(sender.send_bytes.call_args.args[0]), {"reason": "login_auth_refused"})

    def test_real_spawn_ipc_accepts_synthetic_token_and_joins_owned_child(self):
        context, children = self.tracked_spawn_context()
        with patch.object(login.multiprocessing, "get_context", return_value=context), \
            patch.object(login, "_login_worker", synthetic_success_worker):
            self.assertEqual(login.login_in_worker(None), "synthetic.id.token")
        self.assertEqual(len(children), 1)
        self.assertFalse(children[0].is_alive())
        children[0].close()

    def test_real_spawn_timeout_kills_owned_child_and_never_writes_or_reports_success(self):
        context, children = self.tracked_spawn_context()
        stdout = StringIO()
        started = time.monotonic()
        with patch.object(login.multiprocessing, "get_context", return_value=context), \
            patch.object(login, "_login_worker", synthetic_waiting_worker), \
            patch.object(login, "REQUEST_SECONDS", 0.3), \
            patch.object(login, "load_inputs", return_value=None), \
            patch.object(login, "write_token") as write, redirect_stdout(stdout):
            result = login.main(["--admin-credential", "/synthetic/admin.json", "--web-api-config",
                "/synthetic/api.json", "--id-token-output", "/synthetic/token"])
        self.assertEqual(result, 4)
        write.assert_not_called()
        self.assertEqual(json.loads(stdout.getvalue())["reason"], "login_timeout")
        self.assertNotIn("completed", stdout.getvalue())
        self.assertLess(time.monotonic() - started, 5)
        self.assertEqual(len(children), 1)
        self.assertFalse(children[0].is_alive())
        children[0].close()


if __name__ == "__main__":
    unittest.main()
