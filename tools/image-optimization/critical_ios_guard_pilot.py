#!/usr/bin/env python3
"""Optional bounded iOS guard observations; never a native-entry gate receipt.

Run one case with the existing installed release artifacts, then independently
qualify a device ledger for corruption/normal teardown. Runtime validators and
signed build inputs are imported unchanged. All outputs remain private.
"""
import base64
from contextlib import contextmanager
from datetime import datetime, timezone
from decimal import Decimal
from hashlib import sha256
import ipaddress
import math
from pathlib import Path
import re
import sys
import threading
import time

from cryptography import x509

import native_entry_driver as host
from native_entry_config import (EntryError, HASH, INPUT_KEYS, SCOPE,
    exact_keys, finite_number, frozen_json, load_config, load_manifest,
    origin, private_file, private_output, require, strict_json, write_private)
from native_fixture_server import Broker

PILOT_SCOPE = "image-optimization-critical-ios-guards-r1"
FAULTS = ("alternate-trust", "expired-envelope", "wrong-uid-envelope", "corrupt-bytes", "normal-teardown")
EXPECTED_STAGES = {"alternate-trust": "NATIVE_PROBE", "expired-envelope": "NATIVE_ENVELOPE_EXPIRED",
    "wrong-uid-envelope": "NATIVE_ENVELOPE_BINDING", "corrupt-bytes": "DIAGNOSTIC_UNAVAILABLE"}
MAX_LEDGER_BYTES = 262144
OBSERVER_LOCK = threading.Lock()
HASH_FIELDS = frozenset(("caseSha256", "providerSha256", "attemptSha256", "sessionSha256", "sourceSha256", "referenceSha256", "settingsSha256", "scopeSha256"))
INT_FIELDS = frozenset(("generation", "monotonicMicroseconds", "requestCount", "retryCount", "bodyBytes",
    "sourceWidth", "sourceHeight", "requiredWidth", "requiredHeight", "decodeWidth", "decodeHeight",
    "actualDecodeWidth", "actualDecodeHeight", "frameNumber", "dartHeapBytes", "dartExternalBytes",
    "decodedLruBytes", "decodedLiveCount", "decodedPendingCount", "processBytes", "nativeBytes", "graphicsBytes",
    "diskBytes", "transferBytes", "firstRasterizedTargetFrame", "observerOverheadMicroseconds", "memoryBytes", "nativeReadMicroseconds", "nativeResultCode", "nativeResultCount", "sampleStartMicroseconds", "sampleEndMicroseconds", "sampleCadenceMicroseconds"))
DOUBLE_FIELDS = frozenset(("logicalSlotWidth", "logicalSlotHeight", "dpr", "alignmentX", "alignmentY", "observedSlotWidth", "observedSlotHeight", "observedDpr"))
BOOL_FIELDS = frozenset(("encodedVerified", "sourceShortage", "physicalPresentationVerified"))
STRING_FIELDS = {
    "phase": {"idle", "baseline", "load", "hold", "releasing", "post_release", "complete"},
    "memoryCounter": {"ios_phys_footprint", "android_total_pss", "unavailable"},
    "memoryReason": {"none", "mach_result", "mach_count", "missing_counter", "sample_failed", "session_inactive", "sample_timeout"},
    "thermalState": {"nominal", "fair", "serious", "critical", "unavailable"},
    "clockDomain": {"dart_monotonic"}, "clockUnit": {"microseconds"}, "fit": {"contain", "cover"},
    "role": {"isolated_raster_fixture"}, "event": {"request", "body", "encoded_verified", "decoded", "draw",
        "failure", "revoke", "cache_state", "status", "status_request", "status_body", "ownership", "phase", "memory_sample", "release_complete", "layout", "dispatch"},
    "ownership": {"owned", "settling", "settled", "released", "unavailable"},
    "reason": {"none", "invalid_fixture", "session_inactive", "status_unavailable", "status_revoked", "missing_counter",
        "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity",
        "route_departed", "stale_attempt", "internal_settlement_unavailable", "export_capacity", "pin_refused",
        "transport_refused", "decode_refused", "cleanup_unavailable", "phase_budget_refused", "load_timeout", "layout_unavailable"},
    "counterReason": {"missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable",
        "clock_unmapped", "retained_layer_ambiguity", "internal_settlement_unavailable"},
    "cacheState": {"namespace_keys_absent", "network-cold", "disk-warm/decoded-cold", "decoded-warm", "unavailable"},
}


def digest_fields(fields):
    values = [str(field).encode("utf-8") for field in fields]
    return sha256(b"".join(len(value).to_bytes(4, "big") + value for value in values)).hexdigest()


def dart_double(value):
    """Match Dart's shortest double spelling for the admitted finite geometry."""
    number = float(value)
    require(math.isfinite(number), "provider_geometry_rejected")
    if number == 0:
        return "-0.0" if math.copysign(1, number) < 0 else "0.0"
    if 1e-6 <= abs(number) < 1e21:
        text = format(Decimal(str(number)), "f")
        return text if "." in text else text + ".0"
    mantissa, exponent = str(number).split("e")
    return mantissa.removesuffix(".0") + "e" + ("+" if int(exponent) >= 0 else "-") + str(abs(int(exponent)))


def provider_binding(manifest, envelope, case_id):
    """Reproduce the current cache identity, including its fresh session namespace."""
    entries = manifest.entries
    namespace = digest_fields([SCOPE, envelope["batchId"], envelope["sessionId"], envelope["generation"],
        envelope["accessAlias"], *[value for item in entries for value in
            (item["role"], item["sourceSha256"], item["profileRevision"])]])
    entry = next(item for item in entries if item["caseId"] == case_id)
    pw, ph = (math.ceil(entry[key] * entry["dpr"]) for key in ("logicalSlotWidth", "logicalSlotHeight"))
    scale = (min if entry["fit"] == "contain" else max)(pw / entry["width"], ph / entry["height"])
    dimensions = [min(entry[key], math.ceil(entry[key] * min(1.0, scale))) for key in ("width", "height")]
    fields = ("caseId", "role", "arm", "sourceSha256", "referenceSha256", "outputSha256", "mime", "byteSize",
        "width", "height", "encoderRevision", "profileRevision", "logicalSlotWidth", "logicalSlotHeight", "dpr",
        "fit", "alignmentX", "alignmentY", "purposeAcceptanceReceipt")
    provider = digest_fields([namespace, *[dart_double(entry[key]) if key in DOUBLE_FIELDS else entry[key]
        for key in fields], *dimensions])
    return {"expectedProviderSha256": provider,
        "expectedAttemptSha256": sha256((provider + "::library-transfer::1").encode()).hexdigest(),
        "expectedDecodeWidth": dimensions[0], "expectedDecodeHeight": dimensions[1]}


def alternate_certificate(path, config):
    der = private_file(path, 8192)
    try:
        certificate = x509.load_der_x509_certificate(der)
        now = datetime.now(timezone.utc)
        require(certificate.not_valid_before_utc <= now < certificate.not_valid_after_utc, "alternate_certificate_expired")
        host_name = origin(config.values["brokerOrigin"], broker=True).hostname
        san = certificate.extensions.get_extension_for_class(x509.SubjectAlternativeName).value
        try:
            match = ipaddress.ip_address(host_name) in san.get_values_for_type(x509.IPAddress)
        except ValueError:
            match = host_name in san.get_values_for_type(x509.DNSName)
        require(match and sha256(der).hexdigest() != config.values["brokerCertificateSha256"], "alternate_certificate_rejected")
        return der
    except EntryError:
        raise
    except Exception:
        raise EntryError("alternate_certificate_rejected") from None


class ObservedBroker(Broker):
    """Mutate only original successfully admitted return values, outside runtime inputs."""
    def __init__(self, *args, fault, alternate_der=None, **kwargs):
        require(fault in FAULTS and (alternate_der is not None) == (fault == "alternate-trust"), "fault_rejected")
        self.fault, self.alternate_der = fault, alternate_der
        self.counts = {name: 0 for name in ("brokerProbe", "backendProbe", "pairing", "claim", "fixtureDispatch",
            "fixtureFlushed", "originalDeliveryGuardRefused")}
        self.identity = {}
        self.mutation = {}
        super().__init__(*args, **kwargs)

    def metadata(self):
        value = super().metadata()
        if self.fault == "alternate-trust":
            value = {**value, "brokerCertificateDerBase64": base64.b64encode(self.alternate_der).decode("ascii"),
                "brokerCertificateSha256": sha256(self.alternate_der).hexdigest()}
        return value

    def probe(self, session_id, nonce, backend=False):
        value = super().probe(session_id, nonce, backend)
        with self.lock:
            self.counts["backendProbe" if backend else "brokerProbe"] += 1
        return value

    def pair_launcher(self, payload):
        super().pair_launcher(payload)
        with self.lock:
            self.counts["pairing"] += 1

    def claim(self, value):
        data = super().claim(value)
        envelope = strict_json(data)
        with self.lock:
            self.counts["claim"] += 1
            self.identity = provider_binding(self.manifest, envelope, self.case_id)
        if self.fault == "expired-envelope":
            envelope["expiresAt"] = envelope["issuedAt"] - 1
        elif self.fault == "wrong-uid-envelope":
            envelope["expectedUid"] = "synthetic_mismatching_uid"
        else:
            return data
        return frozen_json(envelope)

    def fixture(self, case_id, session_id, authorization):
        entry, data = super().fixture(case_id, session_id, authorization)
        with self.lock:
            self.counts["fixtureDispatch"] += 1
        if self.fault == "corrupt-bytes":
            offset = len(data) // 2
            data = data[:offset] + bytes((data[offset] ^ 1,)) + data[offset + 1:]
            self.mutation = {"changedByteCount": 1, "wireBytes": len(data), "wireSha256": sha256(data).hexdigest()}
        return entry, data

    def completed_fixture(self, case_id, byte_count, digest):
        try:
            super().completed_fixture(case_id, byte_count, digest)
        except EntryError as error:
            if self.fault == "corrupt-bytes" and str(error) == "fixture_delivery_unverified" and \
                    byte_count == self.mutation.get("wireBytes") and digest == self.mutation.get("wireSha256"):
                with self.lock:
                    self.counts["fixtureFlushed"] += 1
                    self.counts["originalDeliveryGuardRefused"] += 1
            raise
        with self.lock:
            self.counts["fixtureFlushed"] += 1


class GuardPairing(host.LauncherPairing):
    def __init__(self, broker, platform="ios"):
        require(platform == "ios", "guard_selection_rejected")
        super().__init__(broker, platform)
        self.stage = None
        native_source = host.read_source(host.MOBILE / "ios/ImageExperiment/ImageExperimentBootstrap.swift")
        self.allowed_stages = {value.encode("ascii") for value in re.findall(
            r'case [A-Za-z]+ = "([A-Z_]{1,48})"', native_source.decode("utf-8"))}
        require(0 < len(self.allowed_stages) <= 128 and all(value.encode("ascii") in self.allowed_stages
            for value in EXPECTED_STAGES.values()), "guard_stage_schema_rejected")

    def __call__(self, line):
        prefix = b"IMAGE_EXPERIMENT_FIXTURE_STAGE "
        if line.startswith(prefix):
            require(self.stage is None and line[len(prefix):] in self.allowed_stages, "guard_stage_rejected")
            self.stage = line[len(prefix):].decode("ascii")
            return
        super().__call__(line)


@contextmanager
def observe_original_driver(state, fault, alternate):
    """Bind construction seams for one original verify_entry call; restore on every exit."""
    with OBSERVER_LOCK:
        originals = host.Broker, host.LauncherPairing, host.create_server
        def broker_factory(*args, **kwargs):
            require("broker" not in state, "guard_dispatch_repeated")
            value = ObservedBroker(*args, fault=fault, alternate_der=alternate, **kwargs)
            state["broker"] = value
            return value
        def pairing_factory(*args, **kwargs):
            value = GuardPairing(*args, **kwargs)
            state["pairing"] = value
            return value
        def server_factory(*args, **kwargs):
            value = originals[2](*args, **kwargs)
            state["server"] = value
            return value
        try:
            host.Broker, host.LauncherPairing, host.create_server = broker_factory, pairing_factory, server_factory
            yield
        finally:
            host.Broker, host.LauncherPairing, host.create_server = originals


def validate_audit(path, build_path, build, proof):
    data = private_file(path)
    audit = strict_json(data)
    require(type(audit) is dict and type(audit.get("schemaVersion")) is int and audit["schemaVersion"] == 1 and
        audit.get("scope") == "image-optimization-installation-audit-r1" and audit.get("platform") == "ios" and
        audit.get("mode") == "release" and audit.get("certificateBinding") == "exact_bundle_equivalence" and
        audit.get("buildReceiptSha256") == sha256(private_file(build_path, 2 * 1024 * 1024)).hexdigest() and
        proof["installationReceiptSha256"] == sha256(data).hexdigest(), "guard_audit_binding_rejected")
    for key in ("sourceSha256", "copiedSourceSha256", "artifactSha256", "testArtifactSha256",
        "expectedAppId", "testAppId", "certificateSha256"):
        require(audit.get(key) == build.get(key), "guard_audit_binding_rejected")
    for key in ("deviceSelectorSha256", "installedArtifactSha256", "installedTestArtifactSha256"):
        require(audit.get(key) == proof[key], "guard_audit_binding_rejected")
    observed = audit.get("observedAt")
    require(type(observed) is int and int(time.time() * 1000) - 300000 <= observed <= int(time.time() * 1000),
        "guard_audit_stale")
    return sha256(data).hexdigest()


def validate_observation(receipt):
    """A failed subprocess alone proves no guard; require exact native/boundary facts."""
    fault = receipt["fault"]
    require(fault in FAULTS, "fault_rejected")
    require(receipt.get("sourceUnchanged") is True and receipt.get("ownedCleanup") is True and
        receipt.get("observationFailure") is None, "guard_observation_unqualified")
    result, counts = receipt["launcher"], receipt["boundaryCounts"]
    exact_keys(counts, ("brokerProbe", "backendProbe", "pairing", "claim", "fixtureDispatch",
        "fixtureFlushed", "originalDeliveryGuardRefused"))
    require(all(type(value) is int and 0 <= value <= 4 for value in counts.values()) and
        type(receipt["nativeResultCount"]) is int and type(receipt["pairingReportCount"]) is int,
        "guard_boundary_rejected")
    require(result["status"] in ("ok", "failed") and type(result["exitCode"]) is int and
        receipt["nativeResultCount"] == 1, "guard_native_result_unverified")
    if fault == "normal-teardown":
        require(result["status"] == "ok" and result["exitCode"] == 0 and receipt["nativeResult"] == "settled" and
            receipt["nativeStage"] is None and type(receipt.get("originalEntryOutcome")) is dict and
            receipt["originalEntryOutcome"].get("teardown") ==
            "authenticated_app_revoke_observed", "guard_teardown_unverified")
    else:
        require(result["status"] == "failed" and result["exitCode"] != 0 and receipt["nativeResult"] == "refused" and
            receipt["nativeStage"] == EXPECTED_STAGES[fault], "guard_refusal_unverified")
    if fault == "alternate-trust":
        require(all(type(value) is int and value == 0 for value in counts.values()) and
            receipt["pairingReportCount"] == 0 and receipt["admissionSuccessful"] is False,
            "guard_boundary_crossed")
        return
    require(counts["brokerProbe"] == counts["backendProbe"] == counts["pairing"] == counts["claim"] == 1 and
        receipt["pairingReportCount"] == 1 and receipt["admissionSuccessful"] is True,
        "guard_admission_unverified")
    if fault in ("expired-envelope", "wrong-uid-envelope"):
        require(counts["fixtureDispatch"] == counts["fixtureFlushed"] == counts["originalDeliveryGuardRefused"] == 0,
            "guard_boundary_crossed")
    elif fault == "corrupt-bytes":
        mutation = receipt["mutation"]
        require(counts["fixtureDispatch"] > 0 and counts["fixtureDispatch"] == counts["fixtureFlushed"] ==
            counts["originalDeliveryGuardRefused"] and mutation["changedByteCount"] == 1 and
            mutation["wireBytes"] == receipt["fixtureByteSize"] and mutation["wireSha256"] != receipt["fixtureOutputSha256"],
            "guard_corruption_unverified")
    else:
        require(counts["fixtureDispatch"] == counts["fixtureFlushed"] == 1 and
            counts["originalDeliveryGuardRefused"] == 0, "guard_normal_delivery_unverified")


def validate_ledger(data, receipt):
    value = strict_json(data, MAX_LEDGER_BYTES)
    exact_keys(value, ("schemaVersion", "records", "droppedCount"))
    require(type(value["schemaVersion"]) is int and value["schemaVersion"] == 1 and
        type(value["droppedCount"]) is int and value["droppedCount"] == 0 and type(value["records"]) is list and
        1 <= len(value["records"]) <= 512, "guard_ledger_rejected")
    for row in value["records"]:
        require(type(row) is dict, "guard_ledger_rejected")
        for field, item in row.items():
            valid = (type(item) is str and HASH.fullmatch(item)) if field in HASH_FIELDS else \
                (item is None or type(item) is int and item >= (-1 if field == "frameNumber" else 0)) if field in INT_FIELDS else \
                finite_number(item) if field in DOUBLE_FIELDS else type(item) is bool if field in BOOL_FIELDS else \
                (type(item) is str and item in STRING_FIELDS[field]) if field in STRING_FIELDS else False
            require(valid, "guard_ledger_rejected")
    rows = value["records"]
    identity = receipt["providerBinding"]
    require(type(receipt["generation"]) is int and receipt["generation"] >= 0 and
        all(type(identity.get(key)) is str and HASH.fullmatch(identity[key]) for key in
            ("expectedProviderSha256", "expectedAttemptSha256")) and
        type(receipt["caseSha256"]) is str and HASH.fullmatch(receipt["caseSha256"]), "guard_ledger_binding_rejected")
    require(sha256(data).hexdigest() != receipt.get("priorLedgerSha256") and all(row.get("generation",
        receipt["generation"]) == receipt["generation"] for row in rows), "guard_ledger_stale")
    require(any(row.get("caseSha256") == receipt["caseSha256"] and row.get("event") == "ownership" and
        row.get("ownership") == "owned" and row.get("providerSha256") == identity["expectedProviderSha256"] and
        row.get("attemptSha256") == identity["expectedAttemptSha256"] and
        row.get("generation") == receipt["generation"] for row in rows), "guard_ledger_binding_rejected")
    for row in rows:
        for key in ("caseSha256", "providerSha256", "attemptSha256"):
            expected = receipt["caseSha256"] if key == "caseSha256" else identity["expected" + key[0].upper() + key[1:]]
            require(key not in row or row[key] == expected, "guard_ledger_binding_rejected")
        if any(key in row for key in HASH_FIELDS):
            require(row.get("generation") == receipt["generation"], "guard_ledger_binding_rejected")
    require(any(row.get("event") == "ownership" and row.get("ownership") == "released" and
        row.get("providerSha256") == identity["expectedProviderSha256"] and
        row.get("attemptSha256") == identity["expectedAttemptSha256"] and
        row.get("generation") == receipt["generation"] for row in rows) and
        any(row.get("event") == "revoke" and row.get("generation") == receipt["generation"] for row in rows) and
        not any(row.get("reason") == "cleanup_unavailable" for row in rows), "guard_ledger_cleanup_unverified")
    events = [row.get("event") for row in rows]
    if receipt["fault"] == "corrupt-bytes":
        require(not any(event in events for event in ("encoded_verified", "decoded", "draw")) and
            not any(row.get("encodedVerified") is True for row in rows) and any(row.get("event") == "failure" and
                row.get("reason") in ("decode_refused", "invalid_fixture") and
                row.get("providerSha256") == identity["expectedProviderSha256"] and
                row.get("attemptSha256") == identity["expectedAttemptSha256"] for row in rows), "guard_ledger_corruption_unverified")
    else:
        require(all(event in events for event in ("encoded_verified", "decoded", "draw", "revoke")) and "failure" not in events,
            "guard_ledger_draw_unverified")
        require(events.index("encoded_verified") < events.index("decoded") < events.index("draw") < events.index("revoke") and
            all(row.get("encodedVerified") is True for row in rows if row.get("event") == "encoded_verified"),
            "guard_ledger_draw_unverified")
        event_fields = {"encoded_verified": ("caseSha256", "providerSha256", "generation"),
            "decoded": ("caseSha256", "providerSha256", "attemptSha256", "generation"),
            "draw": ("providerSha256", "attemptSha256", "generation")}
        require(all(all(key in row for key in event_fields[row["event"]]) for row in rows
            if row.get("event") in event_fields), "guard_ledger_binding_rejected")
        decoded = [row for row in rows if row.get("event") == "decoded"]
        require(all(row.get("actualDecodeWidth") == identity["expectedDecodeWidth"] and
            row.get("actualDecodeHeight") == identity["expectedDecodeHeight"] for row in decoded), "guard_ledger_decode_unverified")
    return {"ledgerSha256": sha256(data).hexdigest(), "recordCount": len(rows), "droppedCount": 0,
        "freshSessionProviderBinding": "observed", "events": sorted(set(event for event in events if event is not None))}


def observe(config, manifest, output, deadline, *, fault, case_id, build_path, prerequisite_path,
            audit_path, alternate_path=None, prior_ledger_path=None):
    require(config.values["platform"] == "ios" and config.values["mode"] == "release" and
        fault in FAULTS and case_id in manifest.objects, "guard_selection_rejected")
    build, proof = host.validate_physical_prerequisites(config, build_path, prerequisite_path)
    audit_hash = validate_audit(audit_path, build_path, build, proof)
    alternate = alternate_certificate(alternate_path, config) if fault == "alternate-trust" else None
    require((alternate_path is not None) == (fault == "alternate-trust"), "fault_rejected")
    prior_hash = sha256(private_file(prior_ledger_path, MAX_LEDGER_BYTES)).hexdigest() if prior_ledger_path else None
    original_source = host.source_snapshot()[1]
    state, outcome, original_failure = {}, None, None
    with observe_original_driver(state, fault, alternate):
        try:
            outcome = host.verify_entry(config, manifest, output, deadline, case_id=case_id,
                build_path=build_path, prerequisite_path=prerequisite_path)
        except EntryError as error:
            original_failure = str(error)
    require("broker" in state and "pairing" in state and "server" in state, original_failure or "guard_dispatch_unavailable")
    broker, pairing, server = state["broker"], state["pairing"], state["server"]
    launcher_path = Path(output) / "launcher.json"
    launcher = strict_json(private_file(launcher_path)) if launcher_path.exists() else {"status": "failed", "exitCode": None}
    entry = next(item for item in manifest.entries if item["caseId"] == case_id)
    with broker.lock, server.connection_lock:
        receipt = {"schemaVersion": 1, "scope": PILOT_SCOPE, "fault": fault, "nativeEntryGate": "open",
            "deviceAlias": config.values["deviceAlias"], "platform": "ios", "mode": "release",
            "sourceSha256": original_source, "artifactSha256": build["artifactSha256"],
            "testArtifactSha256": build["testArtifactSha256"], "installationReceiptSha256": audit_hash,
            "prerequisiteReceiptSha256": sha256(private_file(prerequisite_path)).hexdigest(),
            "backendStartupSha256": sha256(private_file(config.values["backendStartupReceiptPath"], 262144)).hexdigest(),
            "observedAt": int(time.time() * 1000), "originalDriverReason": original_failure,
            "caseSha256": sha256(case_id.encode()).hexdigest(), "sessionSha256": sha256(broker.session_id.encode()).hexdigest(),
            "generation": broker.generation, "manifestSha256": manifest.private_sha256,
            "publicManifestSha256": manifest.public_sha256, "fixtureOutputSha256": entry["outputSha256"],
            "fixtureByteSize": entry["byteSize"], "nativeResult": pairing.native_result,
            "nativeResultCount": pairing.native_result_count, "nativeStage": pairing.stage,
            "pairingReportCount": pairing.count, "admissionSuccessful": broker.admission_successful,
            "boundaryCounts": dict(broker.counts), "mutation": dict(broker.mutation), "providerBinding": dict(broker.identity),
            "launcher": launcher, "observationFailure": None if original_failure == "tool_failed" else original_failure,
            "originalEntryOutcome": outcome, "priorLedgerSha256": prior_hash,
            "sourceUnchanged": host.source_snapshot()[1] == original_source,
            "ownedCleanup": server.socket.fileno() == -1 and not server.workers and not server.connections and
                not broker.active and broker.token is None and not broker.timer.is_alive() and
                (pairing.worker is None or not pairing.worker.is_alive()),
            "status": "unqualified", "reason": "guard_observation_unqualified",
            "quality": "human_verdict_pending", "latency": "not_qualified", "memory": "not_qualified"}
    try:
        validate_observation(receipt)
        receipt["status"] = "awaiting_ledger" if fault in ("corrupt-bytes", "normal-teardown") else "observed"
        receipt["reason"] = "fresh_device_ledger_required" if receipt["status"] == "awaiting_ledger" else "native_boundary_observed"
    except EntryError as error:
        receipt["reason"] = str(error)
    write_private(Path(output) / "observation-receipt.json", receipt)
    return receipt


def qualify(receipt_path, ledger_path, output):
    raw = private_file(receipt_path)
    receipt = strict_json(raw)
    require(type(receipt) is dict and receipt.get("scope") == PILOT_SCOPE and receipt.get("schemaVersion") == 1 and
        receipt.get("fault") in ("corrupt-bytes", "normal-teardown") and receipt.get("status") == "awaiting_ledger",
        "guard_receipt_rejected")
    validate_observation(receipt)
    ledger = validate_ledger(private_file(ledger_path, MAX_LEDGER_BYTES), receipt)
    result = {"schemaVersion": 1, "scope": PILOT_SCOPE, "status": "observed", "fault": receipt["fault"],
        "observationReceiptSha256": sha256(raw).hexdigest(), "sourceSha256": receipt["sourceSha256"],
        "artifactSha256": receipt["artifactSha256"], "caseSha256": receipt["caseSha256"],
        "sessionSha256": receipt["sessionSha256"], **ledger, "nativeEntryGate": "open",
        "quality": "human_verdict_pending", "latency": "not_qualified", "memory": "not_qualified",
        "physicalPresentation": "not_measured", "firstRasterizedTargetFrame": None}
    write_private(Path(output) / "qualification-receipt.json", result)
    return result


def main(argv=None):
    parser = host.SafeParser(description="Optional bounded critical iOS guard observer")
    sub = parser.add_subparsers(dest="mode", required=True)
    run = sub.add_parser("run")
    for name in ("config", "case-id", "build-receipt", "prerequisite-receipt", "audit-receipt"):
        run.add_argument("--" + name, required=True)
    run.add_argument("--fault", required=True, choices=FAULTS)
    run.add_argument("--alternate-certificate")
    run.add_argument("--prior-ledger")
    check = sub.add_parser("qualify")
    for name in ("observation-receipt", "device-ledger", "private-output"):
        check.add_argument("--" + name, required=True)
    args = parser.parse_args(argv)
    output = None
    try:
        if args.mode == "run":
            config = load_config(args.config)
            manifest = load_manifest(config)
            inputs = [args.config, args.build_receipt, args.prerequisite_receipt, args.audit_receipt,
                *[config.values[key] for key in INPUT_KEYS], *[item["filePath"] for item in strict_json(
                    private_file(config.values["manifestPath"], 100 * 1024 * 1024))["entries"]]]
            inputs += [path for path in (args.alternate_certificate, args.prior_ledger) if path is not None]
            output = private_output(config.values["privateOutputRoot"], inputs, create=True)
            result = observe(config, manifest, output, time.monotonic() + host.TOTAL_SECONDS, fault=args.fault,
                case_id=args.case_id, build_path=args.build_receipt, prerequisite_path=args.prerequisite_receipt,
                audit_path=args.audit_receipt, alternate_path=args.alternate_certificate, prior_ledger_path=args.prior_ledger)
        else:
            output = private_output(args.private_output, [args.observation_receipt, args.device_ledger], create=True)
            result = qualify(args.observation_receipt, args.device_ledger, output)
        print(frozen_json({"scope": PILOT_SCOPE, "status": result["status"], "fault": result["fault"],
            "reason": result.get("reason", "fresh_device_ledger_observed"), "nativeEntryGate": "open"}).decode("ascii"))
        return 0 if result["status"] in ("observed", "awaiting_ledger") else 4
    except (EntryError, OSError, ValueError, TypeError, KeyError) as error:
        reason = str(error) if isinstance(error, EntryError) else "guard_operation_failed"
        if output is not None:
            write_private(Path(output) / "failure-receipt.json", {"scope": PILOT_SCOPE, "status": "unqualified",
                "reason": reason, "nativeEntryGate": "open"})
        print(frozen_json({"scope": PILOT_SCOPE, "status": "unqualified", "reason": reason,
            "nativeEntryGate": "open"}).decode("ascii"))
        return 4


if __name__ == "__main__":
    sys.exit(main())
