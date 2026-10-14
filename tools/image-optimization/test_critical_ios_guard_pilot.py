"""Offline synthetic mutations and qualification; no device or network credentials."""
import base64
from datetime import datetime, timedelta, timezone
from hashlib import sha256
import ipaddress
from pathlib import Path
import sys
import threading
import time
import unittest
from unittest.mock import patch

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID

import critical_ios_guard_pilot as pilot
import native_entry_driver as host
from native_entry_config import EntryError, Manifest, SCOPE, canonical_uid, frozen_json, strict_json
from native_entry_crypto import signed_payload
from test_native_entry_config import PrivateInputs


class GuardInputs(PrivateInputs):
    def setUp(self):
        # These tests exercise broker methods directly, with no listener/socket.
        with patch("test_native_entry_config.socket.socket") as socket:
            socket.return_value.__enter__.return_value.getsockname.return_value = ("127.0.0.1", 18000)
            super().setUp()
        self.values.update(platform="ios", expectedAppId="com.tellpal.tellpalMobilePoc.imageexperiment.release")
        self.config = self.configuration()
        self.snapshot = self.admitted_manifest()
        self.backend_calls, self.verifier_calls = [], []
        self.key = ec.generate_private_key(ec.SECP256R1())
        self.der_key = self.key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
        self.brokers = []
        self.addCleanup(lambda: [broker.close() for broker in self.brokers])

    def backend(self, config, token=None):
        self.backend_calls.append(token)

    def verifier(self, token):
        self.verifier_calls.append(token)
        return {"exp": int(time.time()) + 3600}

    def broker(self, fault, alternate_der=None):
        broker = pilot.ObservedBroker(self.config, self.snapshot, self.verifier,
            backend=self.backend, fault=fault, alternate_der=alternate_der)
        self.brokers.append(broker)
        return broker

    def proof(self, broker):
        broker.probe(broker.session_id, broker.nonce)
        broker.probe(broker.session_id, broker.nonce, backend=True)
        pairing = {"sessionId": broker.session_id, "launchNonce": broker.nonce,
            "publicKeySpkiBase64": base64.b64encode(self.der_key).decode()}
        broker.pair_launcher(pairing)
        body = {"scope": SCOPE, "batchId": self.values["batchId"], **pairing}
        challenge = broker.make_challenge(body)
        payload = signed_payload(SCOPE, body["batchId"], broker.session_id, broker.nonce, challenge["challenge"], self.der_key)
        return {**body, "challenge": challenge["challenge"],
            "signatureBase64": base64.b64encode(self.key.sign(payload, ec.ECDSA(hashes.SHA256()))).decode()}

    def admitted(self, broker):
        return strict_json(broker.claim(self.proof(broker)))

    def alternate(self, *, hostname="127.0.0.1", expired=False):
        key = ec.generate_private_key(ec.SECP256R1())
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "synthetic alternate")])
        now = datetime.now(timezone.utc)
        cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
            .serial_number(x509.random_serial_number()).not_valid_before(now - timedelta(days=2))
            .not_valid_after(now + timedelta(days=-1 if expired else 1))
            .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address(hostname))]), critical=False)
            .sign(key, hashes.SHA256()))
        return self.file("alternate-" + str(len(list(self.base.iterdir()))) + ".der", cert.public_bytes(serialization.Encoding.DER))


class MutationTests(GuardInputs):
    def test_alternate_trust_changes_only_launcher_certificate_and_pin(self):
        original_certificate = Path(self.values["tlsCertificateFile"]).read_bytes()
        alternate = pilot.alternate_certificate(self.alternate(), self.config)
        broker = self.broker("alternate-trust", alternate)
        metadata = broker.metadata()
        self.assertEqual(base64.b64decode(metadata["brokerCertificateDerBase64"]), alternate)
        self.assertEqual(metadata["brokerCertificateSha256"], sha256(alternate).hexdigest())
        self.assertEqual(self.config.values["brokerCertificateSha256"], sha256(self.der).hexdigest())
        self.assertEqual(Path(self.values["tlsCertificateFile"]).read_bytes(), original_certificate)
        self.assertEqual(self.verifier_calls, [])
        self.assertTrue(all(value == 0 for value in broker.counts.values()))

    def test_alternate_certificate_invalid_expired_wrong_host_and_original_refuse(self):
        cases = [self.file("original.der", self.der), self.alternate(hostname="127.0.0.2"),
            self.alternate(expired=True), self.file("invalid.der", b"invalid")]
        for case in cases:
            with self.subTest(case=case.name), self.assertRaises(EntryError):
                pilot.alternate_certificate(case, self.config)

    def test_expired_and_wrong_uid_envelopes_follow_original_canonical_admission(self):
        for fault in ("expired-envelope", "wrong-uid-envelope"):
            with self.subTest(fault=fault):
                broker = self.broker(fault)
                envelope = self.admitted(broker)
                self.assertTrue(broker.admission_successful)
                self.assertEqual(broker.counts["claim"], 1)
                self.assertEqual(broker.counts["fixtureDispatch"], 0)
                self.assertEqual(envelope["idToken"], "synthetic.id.token")
                self.assertEqual(self.backend_calls[-1], "synthetic.id.token")
                self.assertGreater(broker.expires_at, envelope["issuedAt"])
                if fault == "expired-envelope":
                    self.assertLess(envelope["expiresAt"], envelope["issuedAt"])
                    self.assertEqual(envelope["expectedUid"], canonical_uid())
                else:
                    self.assertNotEqual(envelope["expectedUid"], canonical_uid())
                    self.assertEqual(broker.metadata()["expectedUid"], canonical_uid())
                    self.assertEqual(envelope["expiresAt"], broker.expires_at)

    def test_mutation_cannot_skip_invalid_proof_or_failed_original_admission(self):
        for fault in ("expired-envelope", "wrong-uid-envelope", "corrupt-bytes", "normal-teardown"):
            with self.subTest(fault=fault):
                broker = self.broker(fault)
                proof = self.proof(broker)
                proof["signatureBase64"] = "AA=="
                with self.assertRaises(EntryError):
                    broker.claim(proof)
                self.assertFalse(broker.admission_successful)
                self.assertEqual(broker.counts["claim"], 0)
                self.assertEqual(broker.identity, {})
        broker = self.broker("expired-envelope")
        proof = self.proof(broker)
        with patch.object(broker, "verifier", side_effect=EntryError("qa_disabled")):
            with self.assertRaisesRegex(EntryError, "^qa_disabled$"):
                broker.claim(proof)
        self.assertEqual(broker.reason, "admission_failed")
        self.assertIsNone(broker.token)

    def test_corruption_changes_one_snapshot_byte_and_retains_original_delivery_guard(self):
        broker = self.broker("corrupt-bytes")
        envelope = self.admitted(broker)
        entry, data = broker.fixture("case_a", broker.session_id, "Bearer " + envelope["idToken"])
        self.assertEqual(len(data), len(self.image))
        self.assertEqual(sum(left != right for left, right in zip(data, self.image)), 1)
        self.assertEqual(self.snapshot.objects["case_a"], self.image)
        self.assertEqual(entry["outputSha256"], sha256(self.image).hexdigest())
        self.assertEqual(Path(self.entry["filePath"]).read_bytes(), self.image)
        with self.assertRaisesRegex(EntryError, "^fixture_delivery_unverified$"):
            broker.completed_fixture("case_a", len(data), sha256(data).hexdigest())
        self.assertEqual(broker.fixture_delivery_count, 0)
        self.assertEqual(broker.counts["fixtureFlushed"], 1)
        self.assertEqual(broker.counts["originalDeliveryGuardRefused"], 1)
        with self.assertRaisesRegex(EntryError, "^session_auth_rejected$"):
            broker.fixture("case_a", broker.session_id, "Bearer wrong")
        self.assertEqual(broker.counts["fixtureDispatch"], 1)

    def test_normal_retains_original_exact_delivery_and_authenticated_teardown(self):
        broker = self.broker("normal-teardown")
        envelope = self.admitted(broker)
        auth = "Bearer " + envelope["idToken"]
        entry, data = broker.fixture("case_a", broker.session_id, auth)
        self.assertEqual(data, self.image)
        broker.completed_fixture("case_a", len(data), sha256(data).hexdigest())
        self.assertEqual(broker.revoke_from_app(broker.session_id, auth)["reason"], "app_revoked")
        self.assertTrue(broker.explicit_app_revoked)
        self.assertIsNone(broker.token)
        self.assertEqual(entry["outputSha256"], broker.fixture_delivery_sha256)
        self.assertEqual(broker.counts["originalDeliveryGuardRefused"], 0)

    def test_provider_hashes_bind_fresh_session_and_canonical_double_geometry(self):
        broker = self.broker("normal-teardown")
        envelope = self.admitted(broker)
        binding = pilot.provider_binding(self.snapshot, envelope, "case_a")
        self.assertEqual((binding["expectedDecodeWidth"], binding["expectedDecodeHeight"]), (2, 3))
        self.assertEqual(binding, broker.identity)
        for key in ("sessionId", "accessAlias"):
            changed = pilot.provider_binding(self.snapshot, {**envelope, key: "new_value"}, "case_a")
            self.assertNotEqual(changed["expectedProviderSha256"], binding["expectedProviderSha256"])
        self.assertEqual(pilot.dart_double(40), "40.0")
        self.assertEqual(pilot.dart_double(0), "0.0")
        self.assertEqual(pilot.dart_double(-0.0), "-0.0")
        self.assertEqual(pilot.dart_double(1e-6), "0.000001")
        self.assertEqual(pilot.dart_double(1e-7), "1e-7")
        self.assertEqual(pilot.dart_double(1e21), "1e+21")


def observation(fault):
    before_fixture = fault in ("alternate-trust", "expired-envelope", "wrong-uid-envelope")
    admitted = fault != "alternate-trust"
    return {"schemaVersion": 1, "scope": pilot.PILOT_SCOPE, "status": "awaiting_ledger", "fault": fault,
        "sourceUnchanged": True, "ownedCleanup": True, "observationFailure": None,
        "sourceSha256": "1" * 64, "artifactSha256": "2" * 64, "sessionSha256": "3" * 64,
        "nativeResultCount": 1, "nativeResult": "settled" if fault == "normal-teardown" else "refused",
        "nativeStage": pilot.EXPECTED_STAGES.get(fault), "pairingReportCount": int(admitted),
        "admissionSuccessful": admitted, "launcher": {"status": "ok" if fault == "normal-teardown" else "failed",
            "exitCode": 0 if fault == "normal-teardown" else 65},
        "boundaryCounts": {"brokerProbe": int(admitted), "backendProbe": int(admitted), "pairing": int(admitted),
            "claim": int(admitted), "fixtureDispatch": int(not before_fixture), "fixtureFlushed": int(not before_fixture),
            "originalDeliveryGuardRefused": int(fault == "corrupt-bytes")},
        "originalEntryOutcome": {"teardown": "authenticated_app_revoke_observed"},
        "fixtureByteSize": 64, "fixtureOutputSha256": "4" * 64,
        "mutation": {"changedByteCount": 1, "wireBytes": 64, "wireSha256": "5" * 64},
        "generation": 0, "caseSha256": "6" * 64, "priorLedgerSha256": None,
        "providerBinding": {"expectedProviderSha256": "7" * 64, "expectedAttemptSha256": "8" * 64,
            "expectedDecodeWidth": 2, "expectedDecodeHeight": 3}}


def ledger(fault):
    common = {"providerSha256": "7" * 64, "attemptSha256": "8" * 64, "generation": 0}
    rows = [{"event": "ownership", "ownership": "owned", "caseSha256": "6" * 64, **common}]
    if fault == "corrupt-bytes":
        rows += [{"event": "failure", "reason": "decode_refused", **common}]
    else:
        rows += [{"event": "encoded_verified", "encodedVerified": True, "caseSha256": "6" * 64,
                "providerSha256": "7" * 64, "generation": 0},
            {"event": "decoded", "actualDecodeWidth": 2, "actualDecodeHeight": 3, "caseSha256": "6" * 64, **common},
            {"event": "draw", "frameNumber": -1, "firstRasterizedTargetFrame": None,
                "physicalPresentationVerified": False, **common}]
    rows += [{"event": "ownership", "ownership": "released", **common}, {"event": "revoke", "generation": 0}]
    return {"schemaVersion": 1, "droppedCount": 0, "records": rows}


class QualificationTests(unittest.TestCase):
    def test_provider_binding_matches_golden_vectors_from_actual_dart_cache_producer(self):
        # The root operator generated these expected hashes with one successful
        # Flutter test invoking actual ImageExperimentImageCache.provider and
        # RasterCase.geometry. Python never generated the expected values.
        path = Path(__file__).parent / "fixtures/critical-ios-provider-vector-r1.json"
        data = path.read_bytes()
        self.assertEqual(sha256(data).hexdigest(), "4b47ef2a679987c09537d57857ae61c9c4381cbc2b5dc012e30a429911a5b529")
        vector = strict_json(data)
        self.assertEqual(vector["scope"], "synthetic-dart-cache-identity-r1")
        self.assertEqual(vector["producer"], "actual ImageExperimentImageCache.provider and RasterCase.geometry")
        self.assertEqual([item["name"] for item in vector["vectors"]], ["contain", "cover"])
        for item in vector["vectors"]:
            with self.subTest(fit=item["name"]):
                manifest = Manifest(tuple(item["manifest"]["entries"]), {}, "0" * 64, b"{}")
                self.assertEqual(pilot.provider_binding(manifest, item["envelopeIdentity"], item["caseId"]), item["expected"])
                reordered = Manifest(tuple(reversed(manifest.entries)), {}, "0" * 64, b"{}")
                self.assertNotEqual(pilot.provider_binding(reordered, item["envelopeIdentity"], item["caseId"]), item["expected"])

    def test_every_case_requires_specific_native_marker_stage_and_boundary(self):
        for fault in pilot.FAULTS:
            receipt = observation(fault)
            pilot.validate_observation(receipt)
            for key, value in (("nativeResultCount", 0), ("nativeResultCount", True), ("ownedCleanup", False),
                ("sourceUnchanged", False), ("nativeResult", None), ("observationFailure", "tool_failed")):
                with self.subTest(fault=fault, key=key), self.assertRaises(EntryError):
                    pilot.validate_observation({**receipt, key: value})
            for result in ({"status": "timeout", "exitCode": -9}, {"status": "failed", "exitCode": None}):
                with self.subTest(fault=fault, result=result), self.assertRaises(EntryError):
                    pilot.validate_observation({**receipt, "launcher": result})
            if fault != "normal-teardown":
                with self.assertRaises(EntryError):
                    pilot.validate_observation({**receipt, "nativeStage": None})

    def test_failure_native_boundaries_cannot_be_crossed_or_claims_omitted(self):
        for fault in ("alternate-trust", "expired-envelope", "wrong-uid-envelope"):
            receipt = observation(fault)
            with self.subTest(fault=fault), self.assertRaises(EntryError):
                pilot.validate_observation({**receipt, "boundaryCounts": {**receipt["boundaryCounts"], "fixtureDispatch": 1}})
        receipt = observation("corrupt-bytes")
        for name, value in (("claim", 0), ("claim", True), ("fixtureDispatch", 0), ("fixtureFlushed", 0),
            ("originalDeliveryGuardRefused", 0)):
            with self.subTest(name=name), self.assertRaises(EntryError):
                pilot.validate_observation({**receipt, "boundaryCounts": {**receipt["boundaryCounts"], name: value}})

    def test_normal_contradictory_delivery_and_original_refusal_counts_reject(self):
        receipt = observation("normal-teardown")
        for key, values in (("fixtureDispatch", (0, 2)), ("fixtureFlushed", (0, 2)),
            ("originalDeliveryGuardRefused", (1, 2))):
            for value in values:
                with self.subTest(key=key, value=value), self.assertRaisesRegex(EntryError, "^guard_normal_delivery_unverified$"):
                    pilot.validate_observation({**receipt, "boundaryCounts": {**receipt["boundaryCounts"], key: value}})

    def test_fresh_corrupt_and_normal_ledgers_qualify_without_quality_or_performance_claim(self):
        for fault in ("corrupt-bytes", "normal-teardown"):
            receipt, data = observation(fault), frozen_json(ledger(fault))
            result = pilot.validate_ledger(data, receipt)
            self.assertEqual(result["freshSessionProviderBinding"], "observed")
            self.assertEqual(result["ledgerSha256"], sha256(data).hexdigest())

    def test_corruption_requires_fresh_failure_and_forbids_any_success_event(self):
        receipt = observation("corrupt-bytes")
        for mutation in ("remove_failure", "encoded_verified", "decoded", "draw", "encodedVerified"):
            value = ledger("corrupt-bytes")
            if mutation == "remove_failure":
                value["records"] = [row for row in value["records"] if row["event"] != "failure"]
            elif mutation == "encodedVerified":
                value["records"][0]["encodedVerified"] = True
            else:
                value["records"].append({"event": mutation})
            with self.subTest(mutation=mutation), self.assertRaisesRegex(EntryError, "^guard_ledger_corruption_unverified$"):
                pilot.validate_ledger(frozen_json(value), receipt)

    def test_same_case_generation_zero_old_session_hash_or_prior_bytes_refuse(self):
        receipt = observation("corrupt-bytes")
        data = frozen_json(ledger("corrupt-bytes"))
        with self.assertRaisesRegex(EntryError, "^guard_ledger_stale$"):
            pilot.validate_ledger(data, {**receipt, "priorLedgerSha256": sha256(data).hexdigest()})
        for field, changed in (("providerSha256", "a" * 64), ("attemptSha256", "a" * 64),
            ("caseSha256", "a" * 64), ("generation", 1), ("generation", None)):
            value = ledger("corrupt-bytes")
            value["records"][0][field] = changed
            with self.subTest(field=field, changed=changed), self.assertRaises(EntryError):
                pilot.validate_ledger(frozen_json(value), receipt)

    def test_ledger_unknown_secret_duplicate_nonfinite_bool_numbers_capacity_and_drops_refuse(self):
        receipt = observation("corrupt-bytes")
        raw_cases = [b'{"schemaVersion":1,"schemaVersion":1}', b'{"records":NaN}', b'{} trailing', b'"\xff"',
            b" " * (pilot.MAX_LEDGER_BYTES + 1)]
        for raw in raw_cases:
            with self.subTest(size=len(raw)), self.assertRaises(EntryError):
                pilot.validate_ledger(raw, receipt)
        for key, item in (("idToken", "must_never_export"), ("event", "private_secret"), ("generation", True),
            ("dpr", True), ("bodyBytes", -1), ("frameNumber", -2), ("providerSha256", "invalid")):
            value = ledger("corrupt-bytes")
            value["records"][0][key] = item
            with self.subTest(key=key), self.assertRaises(EntryError):
                pilot.validate_ledger(frozen_json(value), receipt)
        for key, item in (("droppedCount", 1), ("droppedCount", True), ("schemaVersion", True),
            ("records", []), ("records", [{}] * 513)):
            value = ledger("corrupt-bytes")
            value[key] = item
            with self.subTest(key=key), self.assertRaises(EntryError):
                pilot.validate_ledger(frozen_json(value), receipt)

    def test_normal_missing_decode_draw_revoke_wrong_geometry_and_order_refuse(self):
        receipt = observation("normal-teardown")
        for event in ("encoded_verified", "decoded", "draw", "revoke"):
            value = ledger("normal-teardown")
            value["records"] = [row for row in value["records"] if row["event"] != event]
            with self.subTest(event=event), self.assertRaises(EntryError):
                pilot.validate_ledger(frozen_json(value), receipt)
        value = ledger("normal-teardown")
        value["records"][2]["actualDecodeWidth"] = 1
        with self.assertRaisesRegex(EntryError, "^guard_ledger_decode_unverified$"):
            pilot.validate_ledger(frozen_json(value), receipt)
        value = ledger("normal-teardown")
        value["records"][1], value["records"][3] = value["records"][3], value["records"][1]
        with self.assertRaisesRegex(EntryError, "^guard_ledger_draw_unverified$"):
            pilot.validate_ledger(frozen_json(value), receipt)

    def test_normal_success_events_require_their_actual_producer_binding_fields(self):
        required = {"encoded_verified": ("caseSha256", "providerSha256", "generation"),
            "decoded": ("caseSha256", "providerSha256", "attemptSha256", "generation"),
            "draw": ("providerSha256", "attemptSha256", "generation")}
        receipt = observation("normal-teardown")
        for event, keys in required.items():
            for key in keys:
                value = ledger("normal-teardown")
                row = next(row for row in value["records"] if row["event"] == event)
                row.pop(key)
                with self.subTest(event=event, key=key), self.assertRaisesRegex(EntryError, "^guard_ledger_binding_rejected$"):
                    pilot.validate_ledger(frozen_json(value), receipt)
        value = ledger("normal-teardown")
        for row in value["records"]:
            if row["event"] in required:
                for key in pilot.HASH_FIELDS:
                    row.pop(key, None)
        with self.assertRaisesRegex(EntryError, "^guard_ledger_binding_rejected$"):
            pilot.validate_ledger(frozen_json(value), receipt)
        # The exact producers omit attempt on encoded verification and case on draw.
        value = ledger("normal-teardown")
        self.assertNotIn("attemptSha256", next(row for row in value["records"] if row["event"] == "encoded_verified"))
        self.assertNotIn("caseSha256", next(row for row in value["records"] if row["event"] == "draw"))
        pilot.validate_ledger(frozen_json(value), receipt)

    def test_both_cases_require_session_bound_release_revoke_and_successful_cleanup(self):
        for fault in ("corrupt-bytes", "normal-teardown"):
            receipt = observation(fault)
            for event in ("release", "revoke"):
                value = ledger(fault)
                value["records"] = [row for row in value["records"] if not
                    (row.get("ownership") == "released" if event == "release" else row["event"] == "revoke")]
                with self.subTest(fault=fault, missing=event), self.assertRaisesRegex(EntryError, "^guard_ledger_cleanup_unverified$"):
                    pilot.validate_ledger(frozen_json(value), receipt)
            for key in ("providerSha256", "attemptSha256", "generation"):
                value = ledger(fault)
                next(row for row in value["records"] if row.get("ownership") == "released").pop(key)
                with self.subTest(fault=fault, key=key), self.assertRaises(EntryError):
                    pilot.validate_ledger(frozen_json(value), receipt)
            value = ledger(fault)
            next(row for row in value["records"] if row["event"] == "revoke").pop("generation")
            with self.subTest(fault=fault, event="unbound_revoke"), self.assertRaisesRegex(EntryError, "^guard_ledger_cleanup_unverified$"):
                pilot.validate_ledger(frozen_json(value), receipt)
            value = ledger(fault)
            value["records"].append({"event": "failure", "reason": "cleanup_unavailable", "generation": 0})
            with self.subTest(fault=fault, cleanup="unavailable"), self.assertRaisesRegex(EntryError, "^guard_ledger_cleanup_unverified$"):
                pilot.validate_ledger(frozen_json(value), receipt)

    def test_stage_parser_rejects_duplicate_unknown_whitespace_and_tool_error_is_not_result(self):
        pairing = pilot.GuardPairing(None)
        pairing(b"tool_failed")
        self.assertEqual(pairing.native_result_count, 0)
        pairing(b"IMAGE_EXPERIMENT_FIXTURE_STAGE NATIVE_PROBE")
        for raw in (b"IMAGE_EXPERIMENT_FIXTURE_STAGE NATIVE_PROBE", b"IMAGE_EXPERIMENT_FIXTURE_STAGE token",
            b"IMAGE_EXPERIMENT_FIXTURE_STAGE NATIVE_PROBE\r", b"IMAGE_EXPERIMENT_RESULT refused "):
            with self.subTest(raw=raw), self.assertRaises(EntryError):
                pairing(raw)


class AuditAndSourceTests(GuardInputs):
    def test_audit_requires_same_installed_build_proof_and_five_minute_freshness(self):
        build = {key: "a" * 64 for key in ("sourceSha256", "copiedSourceSha256", "artifactSha256",
            "testArtifactSha256", "certificateSha256")}
        build.update(expectedAppId=self.values["expectedAppId"], testAppId=self.values["expectedAppId"] + ".uitests.xctrunner")
        build_path = self.file("build.json", frozen_json(build))
        audit = {"schemaVersion": 1, "scope": "image-optimization-installation-audit-r1", "platform": "ios", "mode": "release",
            "certificateBinding": "exact_bundle_equivalence", "buildReceiptSha256": sha256(build_path.read_bytes()).hexdigest(),
            "observedAt": int(time.time() * 1000), **build, "deviceSelectorSha256": "b" * 64,
            "installedArtifactSha256": "a" * 64, "installedTestArtifactSha256": "a" * 64}
        path = self.file("audit.json", frozen_json(audit))
        proof = {key: audit[key] for key in ("deviceSelectorSha256", "installedArtifactSha256", "installedTestArtifactSha256")}
        proof["installationReceiptSha256"] = sha256(path.read_bytes()).hexdigest()
        self.assertEqual(pilot.validate_audit(path, build_path, build, proof), proof["installationReceiptSha256"])
        for key, value in (("observedAt", int(time.time() * 1000) - 300001), ("observedAt", True),
            ("sourceSha256", "c" * 64), ("installedArtifactSha256", "c" * 64), ("certificateBinding", "declared")):
            changed = {**audit, key: value}
            path.write_bytes(frozen_json(changed))
            changed_proof = {**proof, "installationReceiptSha256": sha256(path.read_bytes()).hexdigest()}
            with self.subTest(key=key), self.assertRaises(EntryError):
                pilot.validate_audit(path, build_path, build, changed_proof)

    def test_new_observer_and_tests_are_outside_unchanged_292_signed_inputs(self):
        sources, digest = host.source_snapshot()
        self.assertEqual(len(sources), 292)
        self.assertEqual(digest, "d386e0e62b2029f57653ee5729a3ef49612c8c2ceeae0e3c31f6f3f22b098935")
        self.assertNotIn("tools/image-optimization/critical_ios_guard_pilot.py", sources)
        self.assertNotIn("tools/image-optimization/test_critical_ios_guard_pilot.py", sources)
        self.assertNotIn("tools/image-optimization/fixtures/critical-ios-provider-vector-r1.json", sources)

    def test_qualification_writes_private_separate_receipt_and_preserves_original(self):
        receipt = self.file("observation.json", frozen_json(observation("corrupt-bytes")))
        device = self.file("ledger.json", frozen_json(ledger("corrupt-bytes")))
        output = self.base / "qualification"
        output.mkdir(mode=0o700)
        original = receipt.read_bytes()
        result = pilot.qualify(receipt, device, output)
        self.assertEqual(receipt.read_bytes(), original)
        self.assertEqual((output / "qualification-receipt.json").stat().st_mode & 0o777, 0o600)
        self.assertEqual(result["nativeEntryGate"], "open")
        self.assertEqual(result["quality"], "human_verdict_pending")
        self.assertEqual(result["latency"], "not_qualified")
        self.assertEqual(result["memory"], "not_qualified")
        self.assertIsNone(result["firstRasterizedTargetFrame"])
        self.assertNotIn("synthetic.id.token", frozen_json(result).decode())
        self.assertNotIn(str(self.base), frozen_json(result).decode())


class OriginalDriverTests(GuardInputs):
    class Server:
        class Socket:
            def __init__(self):
                self.closed = False

            def fileno(self):
                return -1 if self.closed else 99

        def __init__(self):
            self.connection_lock = threading.Lock()
            self.connections, self.workers = set(), set()
            self.socket = self.Socket()
            self.stop = threading.Event()

        def serve_forever(self, **kwargs):
            self.stop.wait()

        def shutdown(self):
            self.stop.set()

        def server_close(self):
            self.socket.closed = True

    def run_original(self, fault):
        output = self.base / ("original-" + fault)
        output.mkdir(mode=0o700)
        prerequisite = self.file("proof-" + fault, b"{}")
        build = {"artifactSha256": "a" * 64, "testArtifactSha256": "b" * 64, "testAppId": "synthetic.runner"}
        state = {}
        original_checked = host.checked_run
        def synthetic_run(argv, selected_output, alias, *, deadline, **kwargs):
            if alias == "device-state":
                return b"synthetic connected device"
            pairing = kwargs["on_line"]
            broker = pairing.broker
            state["broker"] = broker
            broker.backend = self.backend
            lines = []
            if fault != "alternate-trust":
                broker.probe(broker.session_id, broker.nonce)
                broker.probe(broker.session_id, broker.nonce, backend=True)
                payload = {"sessionId": broker.session_id, "launchNonce": broker.nonce,
                    "publicKeySpkiBase64": base64.b64encode(self.der_key).decode()}
                lines.append("IMAGE_EXPERIMENT_PAIRING " + base64.b64encode(frozen_json(payload)).decode())
            if fault == "normal-teardown":
                def admit_and_deliver(line):
                    pairing(line)
                    if line.startswith(b"IMAGE_EXPERIMENT_PAIRING "):
                        self.assertTrue(pairing.ready.wait(timeout=1))
                        pairing.poll()
                        body = {"scope": SCOPE, "batchId": self.values["batchId"], **payload}
                        challenge = broker.make_challenge(body)
                        signed = signed_payload(SCOPE, body["batchId"], broker.session_id, broker.nonce,
                            challenge["challenge"], self.der_key)
                        envelope = strict_json(broker.claim({**body, "challenge": challenge["challenge"],
                            "signatureBase64": base64.b64encode(self.key.sign(signed, ec.ECDSA(hashes.SHA256()))).decode()}))
                        auth = "Bearer " + envelope["idToken"]
                        _, data = broker.fixture("case_a", broker.session_id, auth)
                        broker.completed_fixture("case_a", len(data), sha256(data).hexdigest())
                        broker.revoke_from_app(broker.session_id, auth)
                kwargs["on_line"] = admit_and_deliver
                lines.append("IMAGE_EXPERIMENT_RESULT settled")
            else:
                lines.extend(["IMAGE_EXPERIMENT_FIXTURE_STAGE NATIVE_PROBE", "IMAGE_EXPERIMENT_RESULT refused"])
            code = "import sys\n" + "\n".join("print(" + repr(line) + ", flush=True)" for line in lines) + \
                "\nsys.exit(" + ("0" if fault == "normal-teardown" else "65") + ")"
            return original_checked([sys.executable, "-B", "-c", code], selected_output, alias,
                deadline=deadline, **kwargs)
        return output, prerequisite, build, synthetic_run, state

    def test_original_verify_entry_child_process_trace_and_cleanup_survive_expected_refusal(self):
        output, prerequisite, build, synthetic_run, state = self.run_original("alternate-trust")
        originals = host.Broker, host.LauncherPairing
        # Only prerequisites and external transport/tools are synthetic boundaries.
        with patch.object(host, "validate_physical_prerequisites", return_value=(build, {})), \
                patch.object(pilot, "validate_audit", return_value="c" * 64), \
                patch.object(host, "validate_backend_startup", return_value="d" * 64), \
                patch.object(host, "FirebaseVerifier", return_value=self.verifier), \
                patch.object(host, "create_server", side_effect=lambda *args: self.Server()), \
                patch.object(host, "destination_run", return_value=self.base / "synthetic.xctestrun"), \
                patch.object(host, "checked_run", side_effect=synthetic_run):
            receipt = pilot.observe(self.config, self.snapshot, output, time.monotonic() + 5, fault="alternate-trust",
                case_id="case_a", build_path=prerequisite, prerequisite_path=prerequisite,
                audit_path=prerequisite, alternate_path=self.alternate())
        self.assertEqual(receipt["status"], "observed")
        self.assertEqual(receipt["originalDriverReason"], "tool_failed")
        self.assertEqual(receipt["launcher"]["exitCode"], 65)
        self.assertTrue(receipt["ownedCleanup"])
        self.assertEqual((host.Broker, host.LauncherPairing), originals)
        self.assertIn(b"IMAGE_EXPERIMENT_RESULT refused", (output / "launcher.stdout").read_bytes())
        self.assertEqual((output / "launcher.stdout").stat().st_mode & 0o777, 0o600)
        self.assertEqual(receipt["launcher"]["traceSha256"][0], sha256((output / "launcher.stdout").read_bytes()).hexdigest())

    def test_original_positive_entry_outcome_and_verification_receipt_are_preserved(self):
        output, prerequisite, build, synthetic_run, state = self.run_original("normal-teardown")
        with patch.object(host, "validate_physical_prerequisites", return_value=(build, {})), \
                patch.object(pilot, "validate_audit", return_value="c" * 64), \
                patch.object(host, "validate_backend_startup", return_value="d" * 64), \
                patch.object(host, "FirebaseVerifier", return_value=self.verifier), \
                patch.object(host, "create_server", side_effect=lambda *args: self.Server()), \
                patch.object(host, "destination_run", return_value=self.base / "synthetic.xctestrun"), \
                patch.object(host, "checked_run", side_effect=synthetic_run):
            receipt = pilot.observe(self.config, self.snapshot, output, time.monotonic() + 5, fault="normal-teardown",
                case_id="case_a", build_path=prerequisite, prerequisite_path=prerequisite, audit_path=prerequisite)
        self.assertEqual(receipt["status"], "awaiting_ledger")
        self.assertIsNone(receipt["originalDriverReason"])
        self.assertEqual(receipt["originalEntryOutcome"]["teardown"], "authenticated_app_revoke_observed")
        self.assertEqual(strict_json((output / "verification-receipt.json").read_bytes()), receipt["originalEntryOutcome"])
        self.assertTrue(receipt["ownedCleanup"])

    def test_constructor_seams_restore_after_exception_without_changing_validators(self):
        originals = host.Broker, host.LauncherPairing, host.create_server, host.verify_entry, host.validate_entry_outcome
        with self.assertRaisesRegex(EntryError, "^synthetic_failure$"):
            with pilot.observe_original_driver({}, "normal-teardown", None):
                raise EntryError("synthetic_failure")
        self.assertEqual((host.Broker, host.LauncherPairing, host.create_server, host.verify_entry,
            host.validate_entry_outcome), originals)


if __name__ == "__main__":
    unittest.main()
