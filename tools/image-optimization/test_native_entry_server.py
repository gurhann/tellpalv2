"""Synthetic pairing, single-use admission, immutable delivery and lifecycle checks."""
import base64
from concurrent.futures import ThreadPoolExecutor
from hashlib import sha256
import http.client
import json
import os
from pathlib import Path
import socket
import threading
import time
import unittest
from unittest.mock import patch

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

from native_entry_config import EntryError, SCOPE, canonical_uid, frozen_json, strict_json
from native_entry_crypto import certificate_identity, signed_payload
import native_fixture_server as fixture_server
from native_fixture_server import Broker, FirebaseVerifier, create_server
from test_native_entry_config import PrivateInputs


def synthetic_admission_worker(credentials, project, uid, token, output):
    os.setsid()
    output.send_bytes(frozen_json({"exp": int(time.time()) + 3600}))
    output.close()


def synthetic_stalled_worker(credentials, project, uid, token, output):
    os.setsid()
    time.sleep(30)


class BrokerInputs(PrivateInputs):
    def setUp(self):
        super().setUp()
        self.config = self.configuration()
        self.snapshot = self.admitted_manifest()
        self.calls = []
        self.disabled = False
        def verifier(token):
            self.calls.append(token)
            if self.disabled:
                raise EntryError("qa_disabled")
            return {"exp": int(time.time()) + 3600}
        self.verifier = verifier
        self.backend_calls = []
        def backend(config, token=None):
            self.backend_calls.append(token)
            return True
        self.broker = Broker(self.config, self.snapshot, verifier, backend=backend)
        self.addCleanup(self.broker.close)
        self.key = ec.generate_private_key(ec.SECP256R1())
        self.der_key = self.key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        self.public_key = base64.b64encode(self.der_key).decode()
        self.pairing = {"sessionId": self.broker.session_id, "launchNonce": self.broker.nonce, "publicKeySpkiBase64": self.public_key}

    def pair(self):
        self.broker.probe(self.broker.session_id, self.broker.nonce)
        self.broker.probe(self.broker.session_id, self.broker.nonce, backend=True)
        self.broker.pair_launcher(self.pairing)

    def proof(self):
        self.pair()
        body = {"scope": SCOPE, "batchId": self.values["batchId"], **self.pairing}
        challenge = self.broker.make_challenge(body)
        payload = signed_payload(SCOPE, body["batchId"], body["sessionId"], body["launchNonce"], challenge["challenge"], self.der_key)
        return {**body, "challenge": challenge["challenge"], "signatureBase64": base64.b64encode(self.key.sign(payload, ec.ECDSA(hashes.SHA256()))).decode()}

    def admit(self):
        envelope = strict_json(self.broker.claim(self.proof()))
        self.auth = "Bearer " + envelope["idToken"]
        return envelope


class PairingTests(BrokerInputs):
    def test_public_metadata_pairing_challenge_never_expose_token(self):
        self.assertNotIn("synthetic.id.token", json.dumps(self.broker.metadata()))
        self.assertEqual(self.broker.metadata()["caseId"], "case_a")
        proof = self.proof()
        self.assertEqual(self.broker.paired_status(self.broker.session_id, self.broker.nonce)["publicKeySha256"], sha256(self.der_key).hexdigest())
        self.assertNotIn("synthetic.id.token", json.dumps(proof))
        self.assertEqual(self.backend_calls, [None])
        self.assertEqual(self.calls, [])

    def test_network_hello_cannot_pair_and_both_probes_are_required(self):
        body = {"scope": SCOPE, "batchId": self.values["batchId"], **self.pairing}
        with self.assertRaisesRegex(EntryError, "unpaired_key"):
            self.broker.make_challenge(body)
        with self.assertRaisesRegex(EntryError, "reachability_not_qualified"):
            self.broker.pair_launcher(self.pairing)
        self.broker.probe(self.broker.session_id, self.broker.nonce)
        with self.assertRaisesRegex(EntryError, "reachability_not_qualified"):
            self.broker.pair_launcher(self.pairing)
        self.assertEqual(self.calls, [])

    def test_launcher_key_replacement_wrong_nonce_scope_batch_and_expiry_reject(self):
        self.pair()
        with self.assertRaisesRegex(EntryError, "launcher_replacement_rejected"):
            self.broker.pair_launcher(self.pairing)
        body = {"scope": SCOPE, "batchId": self.values["batchId"], **self.pairing}
        for field, value in (("sessionId", "other"), ("launchNonce", "0" * 64), ("scope", "other"),
            ("batchId", "other"), ("publicKeySpkiBase64", "AAAA")):
            with self.subTest(field=field), self.assertRaises(EntryError):
                self.broker.make_challenge({**body, field: value})
        self.broker.deadline = 0
        with self.assertRaisesRegex(EntryError, "bootstrap_expired"):
            self.broker.make_challenge(body)

    def test_single_claim_concurrent_race_has_one_envelope_and_no_retry(self):
        proof = self.proof()
        def claim():
            try:
                return strict_json(self.broker.claim(proof))
            except EntryError as error:
                return str(error)
        with ThreadPoolExecutor(max_workers=2) as executor:
            results = list(executor.map(lambda _: claim(), range(2)))
        self.assertEqual(sum(type(result) is dict for result in results), 1)
        self.assertEqual(sum(result == "claim_replayed" for result in results), 1)
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(self.backend_calls, [None, "synthetic.id.token"])

    def test_wrong_signature_or_missing_fields_cannot_admit_fixture(self):
        proof = self.proof()
        for body in ({**proof, "signatureBase64": "AAAA"}, {key: value for key, value in proof.items() if key != "challenge"}):
            with self.assertRaises(EntryError):
                self.broker.claim(body)
        with self.assertRaises(EntryError):
            self.broker.fixture("case_a", self.broker.session_id, "Bearer synthetic.id.token")
        self.assertEqual(self.calls, [])

    def test_firebase_project_mismatch_rejects_before_sdk_or_network(self):
        Path(self.values["firebaseAdminCredentialFile"]).write_bytes(b'{"project_id":"different-project"}')
        with self.assertRaisesRegex(EntryError, "admin_project_rejected"):
            FirebaseVerifier(self.config)

    def test_admin_credential_root_requires_object_before_sdk_or_network(self):
        for value in (None, [], "private-value", 42, True):
            Path(self.values["firebaseAdminCredentialFile"]).write_bytes(frozen_json(value))
            with self.subTest(value=value), patch.object(fixture_server.multiprocessing, "get_context") as context:
                with self.assertRaisesRegex(EntryError, "^admin_credential_rejected$"):
                    FirebaseVerifier(self.config)
                context.assert_not_called()

    def test_private_ipc_worker_settles_after_synthetic_verification_without_token_export(self):
        verifier = FirebaseVerifier(self.config)
        self.addCleanup(verifier.close)
        with patch.object(fixture_server, "_firebase_worker", synthetic_admission_worker):
            claims = verifier("synthetic.id.token")
        self.assertGreater(claims["exp"], int(time.time()))
        self.assertEqual(verifier.workers, set())
        self.assertNotIn("synthetic.id.token", repr(verifier))

    def test_stalled_private_worker_is_killed_under_total_deadline_and_owned_cleanup(self):
        verifier = FirebaseVerifier(self.config)
        self.addCleanup(verifier.close)
        started = time.monotonic()
        with patch.object(fixture_server, "_firebase_worker", synthetic_stalled_worker), patch.object(fixture_server, "REQUEST_SECONDS", 0.15):
            with self.assertRaisesRegex(EntryError, "token_verification_timeout"):
                verifier("synthetic.id.token")
        self.assertLess(time.monotonic() - started, 2)
        self.assertEqual(verifier.workers, set())


class SessionTests(BrokerInputs):
    def test_partial_wrong_hash_and_unadmitted_delivery_never_record_full_transfer(self):
        with self.assertRaisesRegex(EntryError, "fixture_delivery_unverified"):
            self.broker.completed_fixture("case_a", len(self.image), sha256(self.image).hexdigest())
        self.admit()
        for case, size, digest in (("case_a", len(self.image) - 1, sha256(self.image).hexdigest()),
            ("case_a", len(self.image), "0" * 64), ("unknown", len(self.image), sha256(self.image).hexdigest())):
            with self.subTest(case=case, size=size), self.assertRaisesRegex(EntryError, "fixture_delivery_unverified"):
                self.broker.completed_fixture(case, size, digest)
        self.assertEqual(self.broker.fixture_delivery_count, 0)

    def test_expiry_during_fresh_status_verification_returns_terminal_status(self):
        self.admit()
        def expired_during_verification(token):
            self.broker.expires_at = 0
        self.broker.verifier = expired_during_verification
        status = self.broker.revoke_from_app(self.broker.session_id, self.auth)
        self.assertEqual(status["status"], "expired")
        self.assertFalse(self.broker.explicit_app_revoked)

    def test_exact_envelope_hashes_uid_scope_ttl_and_size(self):
        envelope = self.admit()
        self.assertEqual(envelope["expectedUid"], canonical_uid())
        self.assertEqual(envelope["scope"], SCOPE)
        self.assertLessEqual(envelope["expiresAt"] - envelope["issuedAt"], 180000)
        self.assertLessEqual(len(frozen_json(envelope)), 65536)
        public = base64.b64decode(envelope["publicManifestBase64"], validate=True)
        self.assertEqual(sha256(public).hexdigest(), envelope["publicManifestSha256"])
        self.assertNotIn(b"filePath", public)
        self.assertNotIn("synthetic", repr(self.broker))

    def test_fresh_verification_each_object_and_disabled_status_revokes(self):
        self.admit()
        for _ in range(2):
            self.broker.fixture("case_a", self.broker.session_id, self.auth)
        self.assertEqual(len(self.calls), 3)
        self.disabled = True
        self.assertEqual(self.broker.authorize(self.broker.session_id, self.auth)["status"], "revoked")
        self.assertIsNone(self.broker.token)
        with self.assertRaises(EntryError):
            self.broker.fixture("case_a", self.broker.session_id, self.auth)

    def test_immutable_snapshot_unknown_ids_and_wrong_auth_refuse(self):
        self.admit()
        Path(self.entry["filePath"]).write_bytes(b"changed")
        self.assertEqual(self.broker.fixture("case_a", self.broker.session_id, self.auth)[1], self.image)
        for case, session, auth in (("../escape", self.broker.session_id, self.auth), ("unknown", self.broker.session_id, self.auth),
            ("case_a", "wrong", self.auth), ("case_a", self.broker.session_id, "Bearer wrong")):
            with self.assertRaises(EntryError):
                self.broker.fixture(case, session, auth)

    def test_expiry_revocation_owned_sockets_and_idempotent_teardown(self):
        self.admit()
        sockets = [socket.socket() for _ in range(5)]
        for connection in sockets:
            self.addCleanup(connection.close)
        for connection in sockets[:4]:
            self.broker.own(connection)
        with self.assertRaisesRegex(EntryError, "transfer_capacity_rejected"):
            self.broker.own(sockets[4])
        self.broker.expires_at = 0
        self.assertEqual(self.broker.authorize(self.broker.session_id, self.auth)["status"], "expired")
        self.assertEqual(self.broker.active, set())
        self.assertTrue(all(connection.fileno() == -1 for connection in sockets[:4]))
        self.broker.close()
        self.broker.close()
        self.assertEqual(self.snapshot.objects, {})
        self.assertIsNone(self.broker.paired)

    def test_late_network_completion_after_revoke_never_retains_token(self):
        proof = self.proof()
        def backend(config, token=None):
            self.broker.revoke("host_revoked")
        self.broker.backend = backend
        with self.assertRaises(EntryError):
            self.broker.claim(proof)
        self.assertIsNone(self.broker.token)
        self.assertEqual(self.broker.state()["status"], "revoked")

    def test_network_work_does_not_hold_admission_lock_or_delay_owned_revoke(self):
        proof = self.proof()
        started, complete = threading.Event(), threading.Event()
        def verifier(token):
            started.set()
            complete.wait(timeout=1)
            return {"exp": int(time.time()) + 3600}
        self.broker.verifier = verifier
        results = []
        def claim():
            try:
                self.broker.claim(proof)
            except EntryError as error:
                results.append(str(error))
        worker = threading.Thread(target=claim)
        worker.start()
        self.assertTrue(started.wait(timeout=1))
        before = time.monotonic()
        self.broker.revoke()
        self.assertLess(time.monotonic() - before, 0.1)
        complete.set()
        worker.join(timeout=2)
        self.assertFalse(worker.is_alive())
        self.assertIsNone(self.broker.token)
        self.assertTrue(results)


class HTTPSDeliveryTests(BrokerInputs):
    def setUp(self):
        super().setUp()
        self.server = create_server(self.config, self.broker)
        self.thread = threading.Thread(target=self.server.serve_forever, kwargs={"poll_interval": 0.01}, daemon=True)
        self.thread.start()
        self.addCleanup(self.finish)

    def finish(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def request(self, method, path, body=None, headers=None):
        _, context = certificate_identity(self.config)
        connection = http.client.HTTPSConnection("127.0.0.1", self.server.server_port, context=context, timeout=2)
        try:
            connection.request(method, path, body=body, headers=headers or {})
            response = connection.getresponse()
            return response.status, response.getheader("Content-Type"), response.read()
        finally:
            connection.close()

    def test_fixed_token_free_probes_and_no_arbitrary_proxy_paths(self):
        headers = {"X-Experiment-Session": self.broker.session_id, "X-Experiment-Launch-Nonce": self.broker.nonce}
        self.assertEqual(self.request("GET", "/experiment/v1/probe", headers=headers)[0], 200)
        result = strict_json(self.request("GET", "/experiment/v1/backend-probe", headers=headers)[2])
        self.assertEqual(result["reachability"], "proxied")
        for path in ("/experiment/v1/backend-probe?url=https://example.com", "/api/test/library", "/experiment/v1/probe#fragment"):
            status, _, data = self.request("GET", path, headers=headers)
            self.assertEqual(status, 403)
            self.assertNotIn(b"example.com", data)

    def test_auth_exact_bytes_no_redirect_and_status_revocation(self):
        self.admit()
        headers = {"Authorization": self.auth, "X-Experiment-Session": self.broker.session_id}
        status, mime, data = self.request("GET", "/experiment/v1/batches/test_batch/objects/case_a", headers=headers)
        self.assertEqual((status, mime, data), (200, "image/png", self.image))
        self.assertEqual(self.request("GET", "/experiment/v1/batches/test_batch/objects/case_a")[0], 403)
        self.assertEqual(self.request("GET", "/experiment/v1/session/status", headers=headers)[0], 200)
        status, _, data = self.request("POST", "/experiment/v1/session/revoke", body=b"{}", headers={**headers, "Content-Type": "application/json"})
        self.assertEqual(status, 200)
        self.assertEqual(strict_json(data)["status"], "revoked")
        self.assertTrue(self.broker.admission_successful)
        self.assertTrue(self.broker.explicit_app_revoked)
        self.assertEqual(self.broker.reason, "app_revoked")
        self.assertEqual(self.broker.fixture_delivery_count, 1)
        self.assertEqual(self.broker.fixture_delivery_bytes, len(self.image))
        self.assertEqual(self.broker.fixture_delivery_sha256, sha256(self.image).hexdigest())

    def test_body_bound_duplicate_fields_and_method_refuse_without_raw_error(self):
        for body in (b'{"secret":"raw-secret","secret":1}', b" " * 65537):
            status, _, data = self.request("POST", "/experiment/v1/bootstrap/challenge", body=body, headers={"Content-Type": "application/json"})
            self.assertEqual(status, 403)
            self.assertNotIn(b"raw-secret", data)
        self.assertEqual(self.request("PUT", "/experiment/v1/probe", body=b"")[0], 501)


if __name__ == "__main__":
    unittest.main()
