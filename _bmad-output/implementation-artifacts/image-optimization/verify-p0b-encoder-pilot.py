#!/usr/bin/env python3
"""Independently reread the local r3 encoder evidence; never import the encoder."""

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
from io import BytesIO

from PIL import Image, ImageFile, JpegImagePlugin

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
CORPUS = HERE / "p0a-corpus.json"
PROTOCOL = HERE / "p0a-trial-protocol.md"
LIMITS = {"concurrency": 1, "encode_seconds": 120, "readback_seconds": 120,
          "preflight_seconds": 300, "output_bytes": 64 * 1024 * 1024,
          "input_bytes": 20 * 1024 * 1024, "input_pixels": 40_000_000,
          "initial_arms": 28, "maximum_attempts": 56}
DEFAULTS = {"webp_lossless_quality": {"supplied": False, "effective": 80,
            "basis": "installed Pillow 12.3.0 WebPImagePlugin._save default"}}
LIMITATIONS = {
    "scope": "local_installed_python_pillow_source_resolution_codec_feasibility",
    "shared_pillow_decoder": True,
    "independent_reread_is_not_decoder_diversity": True,
    "visual_verdict": "pending", "reader_verdict": "pending", "native_verdict": "pending",
    "quality_qualified_savings": None,
    "java_railway_mobile_qualification": False, "resource_qualification": False,
    "worker_measurement": "deferred_story_1_4",
    "open_gates": ["FQ-READER", "FQ-REAL-ALPHA", "FQ-EXIF-ROTATION", "FQ-EXIF-MIRRORING",
                   "OQ-2", "OQ-3", "OQ-4", "OQ-6"],
}
IJG_LUMA = (16, 11, 10, 16, 24, 40, 51, 61, 12, 12, 14, 19, 26, 58, 60, 55,
            14, 13, 16, 24, 40, 57, 69, 56, 14, 17, 22, 29, 51, 87, 80, 62,
            18, 22, 37, 56, 68, 109, 103, 77, 24, 35, 55, 64, 81, 104, 113, 92,
            49, 64, 78, 87, 103, 121, 120, 101, 72, 92, 95, 98, 112, 100, 103, 99)
IJG_CHROMA = (17, 18, 24, 47, 99, 99, 99, 99, 18, 21, 26, 66, 99, 99, 99, 99,
              24, 26, 56, 99, 99, 99, 99, 99, 47, 66, 99, 99, 99, 99, 99, 99) + (99,) * 32
ImageFile.LOAD_TRUNCATED_IMAGES = False
Image.MAX_IMAGE_PIXELS = LIMITS["input_pixels"]


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    loaded = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded)
    return loaded


P0A = module("p0a_unchanged_contract", HERE / "verify-p0a-corpus.py")
canonical, same, stream_hash = P0A.canonical, P0A.same, P0A.stream_hash
sha = P0A.sha


class GateFailure(ValueError):
    """A sanitized category; exception messages never expose private inputs."""


def load_json(path):
    no_symlinks(path)
    def pairs(rows):
        result = {}
        for key, value in rows:
            if key in result:
                raise GateFailure("duplicate_json_key")
            result[key] = value
        return result
    if path.stat().st_size > 8 * 1024 * 1024:
        raise GateFailure("json_byte_limit")
    return json.loads(path.read_bytes(), object_pairs_hook=pairs,
                      parse_constant=lambda _: (_ for _ in ()).throw(GateFailure("nonfinite_json")))


def no_symlinks(path):
    if ".." in Path(path).parts:
        raise GateFailure("unsafe_path")
    path = Path(os.path.abspath(path))
    for part in [*reversed(path.parents), path]:
        if part.is_symlink():
            raise GateFailure("symlink_path")
    return path


def private_path(path):
    path = no_symlinks(path)
    if path.resolve().is_relative_to(REPO.resolve()):
        raise GateFailure("private_path_inside_repository")
    return path


def safe_batch(path, inputs, *, new=False):
    path = private_path(path)
    for source in inputs:
        source = no_symlinks(source).resolve()
        resolved = path.resolve()
        if resolved.is_relative_to(source) or source.is_relative_to(resolved):
            raise GateFailure("output_input_overlap")
    if new and path.exists():
        raise GateFailure("batch_already_exists")
    return path


def intake(path, declared_mime):
    """Use the same bounded byte/decode gate for sources and rejection fixtures."""
    no_symlinks(path)
    if path.stat().st_size > LIMITS["input_bytes"]:
        return "source_byte_limit"
    with path.open("rb") as stream:
        raw = stream.read(LIMITS["input_bytes"] + 1)
    if len(raw) > LIMITS["input_bytes"]:
        return "source_byte_limit"
    try:
        with Image.open(BytesIO(raw)) as image:
            if image.width * image.height > LIMITS["input_pixels"]:
                return "source_pixel_limit"
            if P0A.PIL_MIME.get(image.format) != declared_mime:
                return "declared_actual_mime_mismatch"
            image.load()
    except Image.DecompressionBombError:
        return "source_pixel_limit"
    except (OSError, ValueError, SyntaxError):
        return "decode_truncated"
    return None


def reference_rgb(path, pair):
    """Check the canonical stream and exact opacity before projecting to RGB."""
    no_symlinks(path)
    width, height = pair["geometry"]["width"], pair["geometry"]["height"]
    if type(width) is not int or type(height) is not int or width <= 0 or height <= 0:
        raise GateFailure("reference_geometry")
    if width * height > LIMITS["input_pixels"]:
        raise GateFailure("reference_pixel_limit")
    expected_bytes = len(P0A.MAGIC) + 8 + width * height * 4
    if path.stat().st_size != expected_bytes:
        raise GateFailure("reference_length")
    raw = path.read_bytes()
    if sha(raw) != pair["reference_pixel_sha256"]:
        raise GateFailure("reference_hash")
    if raw[:len(P0A.MAGIC)] != P0A.MAGIC:
        raise GateFailure("reference_magic")
    if struct.unpack(">II", raw[len(P0A.MAGIC):len(P0A.MAGIC) + 8]) != (width, height):
        raise GateFailure("reference_geometry")
    pixels = raw[len(P0A.MAGIC) + 8:]
    if any(alpha != 255 for alpha in memoryview(pixels)[3::4]):
        raise GateFailure("reference_not_opaque")
    return Image.frombytes("RGBA", (width, height), pixels).convert("RGB").tobytes()


def make_schedule(corpus):
    """Validate the finite structural/order contract before creating a batch."""
    try:
        plan = corpus["trial_plan"]
        labels = ("jpeg-q80", "jpeg-q90", "webp-lossy-q80", "webp-lossy-q85", "webp-lossy-q90", "webp-lossless", "png-opaque")
        expected_ids = {binding + "-" + label + "-r3" for binding in P0A.SELECTED_BINDINGS for label in labels}
        rows = plan["planned_pairs"]
        if not isinstance(rows, list) or len(rows) != 14: raise ValueError()
        pairs = {pair["id"]: pair for pair in rows}
        if set(pairs) != expected_ids: raise ValueError()
        context = {"seed": 20261010, "environment_id": "offline-source-resolution-codec-r3", "cache_mode": "not_applicable_offline", "repeat": 1}
        if not same(plan["seed"], 20261010) or not same(plan["offline_order_context"], context): raise ValueError()
        expected_order = []
        for ident, pair in pairs.items():
            ids = {arm: [ident + "-" + arm + "-attempt-1", ident + "-" + arm + "-retry-1"] for arm in ("control", "candidate")}
            if not same(pair["planned_attempt_ids"], ids): raise ValueError()
            key = {field: plan["seed"] if field == "seed" else pair[field] for field in P0A.PAIR_KEY_FIELDS}
            key_hash = sha(canonical(key))
            if pair["pair_key_sha256"] != key_hash: raise ValueError()
            digest = sha(canonical({**context, "pair_key_sha256": key_hash}))
            first = "control" if int(digest[:2], 16) % 2 == 0 else "candidate"
            expected_order.append({"pair_id": ident, "context_sha256": digest, "arms": [first, "candidate" if first == "control" else "control"]})
        expected_order.sort(key=lambda row: (row["context_sha256"], row["pair_id"]))
        if not same(plan["offline_order"], expected_order): raise ValueError()
        result = []
        for ordered in expected_order:
            pair = pairs[ordered["pair_id"]]
            for arm in ordered["arms"]:
                initial, retry = pair["planned_attempt_ids"][arm]
                result.append({"pair_id": pair["id"], "arm": arm, "initial_id": initial, "retry_id": retry,
                               "settings_sha256": sha(canonical(pair[arm + "_settings"]))})
        if len(result) != 28 or len({r[k] for r in result for k in ("initial_id", "retry_id")}) != 56: raise ValueError()
        return result
    except (KeyError, TypeError, ValueError, AttributeError):
        raise GateFailure("invalid_frozen_schedule") from None


def input_paths(args):
    return [args.source_root, args.reference_root, args.preparation_receipt, args.freeze_manifest,
            args.corpus, args.protocol]


def file_identity(path, ident):
    no_symlinks(path)
    digest, size = stream_hash(path)
    return {"id": ident, "sha256": digest, "bytes": size}


def input_snapshot(args, corpus, *, selected=False):
    rows = [file_identity(args.corpus, "corpus"), file_identity(args.protocol, "protocol"),
            file_identity(args.preparation_receipt, "preparation_receipt"),
            file_identity(args.freeze_manifest, "freeze_manifest")]
    refs = corpus["normalization"]["correctness"]["prepared_references"]
    sources = {ref["source_hash"] for ref in refs}
    if selected:
        sources = {p["source_sha256"] for p in corpus["trial_plan"]["planned_pairs"]}
        reference_ids = {p["reference_id"] for p in corpus["trial_plan"]["planned_pairs"]}
        refs = [r for r in refs if r["id"] in reference_ids]
    for digest in sorted(sources):
        rows.append(file_identity(args.source_root / (digest + ".source"), "source-" + digest))
    for ref in sorted(refs, key=lambda r: r["id"]):
        rows.append(file_identity(args.reference_root / (ref["id"] + ".pixels"), "reference-" + ref["id"]))
    for fixture in corpus["synthetic_rejection_fixtures"]:
        rows.append(file_identity(args.source_root / "fixtures" / fixture["private_file"], fixture["id"]))
    return rows


def code_identity():
    return [file_identity(HERE / name, name) for name in
            ("run-p0b-encoder-pilot.py", "verify-p0b-encoder-pilot.py", "verify-p0a-corpus.py",
             "prepare-p0a-normalization.py")]


def check_integrity(args, scope):
    try:
        corpus = load_json(args.corpus)
        if not same(input_snapshot(args, corpus, selected=True), scope["selected_input_snapshot"]):
            raise GateFailure("prerequisite_integrity_failed")
        if not same(code_identity(), scope["code"]):
            raise GateFailure("prerequisite_integrity_failed")
        if not same(P0A.installed_provider_discovery(), scope["provider_discovery"]):
            raise GateFailure("prerequisite_integrity_failed")
    except Exception:
        # Missing/unreadable inputs or discovery failure are integrity failures too.
        raise GateFailure("prerequisite_integrity_failed") from None


def bounded_integrity(args, scope_path):
    """Only the child calls provider discovery; the parent enforces the deadline."""
    try:
        observed = subprocess.run([sys.executable, str(HERE / "verify-p0b-encoder-pilot.py"),
            "--integrity-scope", str(scope_path), *cli_inputs(args)], capture_output=True,
            timeout=LIMITS["preflight_seconds"])
        if observed.returncode != 0 or json.loads(observed.stdout).get("status") != "PASS":
            raise GateFailure("prerequisite_integrity_failed")
    except (OSError, ValueError, subprocess.TimeoutExpired):
        raise GateFailure("prerequisite_integrity_failed") from None
def preflight(args):
    for path in (args.source_root, args.reference_root, args.preparation_receipt, args.freeze_manifest):
        private_path(path)
    for path in (args.corpus, args.protocol):
        no_symlinks(path)
    corpus = load_json(args.corpus)
    report = P0A.Report()
    P0A.validate(corpus, args.source_root, args.protocol, args.reference_root,
                 args.preparation_receipt, report, args.freeze_manifest, stream_hash(args.corpus)[0])
    if report.status() != "PASS":
        return {"status": report.status(), "diagnostics": report.invalid + report.incomplete}
    fixtures = []
    for fixture in corpus["synthetic_rejection_fixtures"]:
        actual = intake(args.source_root / "fixtures" / fixture["private_file"],
                        fixture["metadata"]["declared_mime"])
        fixtures.append({"id": fixture["id"], "expected_rejection": fixture["expected_rejection"],
                         "actual_rejection": actual, "synthetic": True,
                         "passed": same(actual, fixture["expected_rejection"])})
    for pair in corpus["trial_plan"]["planned_pairs"]:
        mime = next(s["declared_mime"] for s in corpus["specimens"]
                    if s["integrity"]["sha256"] == pair["source_sha256"])
        if intake(args.source_root / (pair["source_sha256"] + ".source"), mime) is not None:
            raise GateFailure("selected_source_intake_failed")
        reference_rgb(args.reference_root / (pair["reference_id"] + ".pixels"), pair)
    if len(fixtures) != 4 or not all(row["passed"] for row in fixtures):
        raise GateFailure("fixture_gate_failed")
    return {"status": "PASS", "input_snapshot": input_snapshot(args, corpus),
            "selected_input_snapshot": input_snapshot(args, corpus, selected=True),
            "provider_discovery": P0A.installed_provider_discovery(), "fixtures": fixtures}


def make_scope(corpus, evidence, batch_id):
    return {"schema_version": 1, "artifact": "p0b-execution-scope", "batch_id": batch_id,
            "freeze_revision_id": corpus["freeze"]["revision_id"],
            "experimental_purpose": P0A.EXPERIMENTAL_PURPOSE,
            "parent_freeze": corpus["freeze"],
            "reference_set_sha256": sha(canonical([{k: row[k] for k in
                ("id", "source_hash", "pixel_sha256", "width", "height", "implementation_sha256",
                 "destination_profile_sha256", "recipe_version")}
                for row in sorted(corpus["normalization"]["correctness"]["prepared_references"],
                                  key=lambda row: row["id"])])),
            "input_snapshot": evidence["input_snapshot"],
            "selected_input_snapshot": evidence["selected_input_snapshot"],
            "provider_discovery": evidence["provider_discovery"],
            "provider_discovery_sha256": corpus["trial_plan"]["provider_discovery_sha256"],
            "code": code_identity(), "limits": LIMITS, "effective_defaults": DEFAULTS,
            "planned_pairs": corpus["trial_plan"]["planned_pairs"],
            "schedule": make_schedule(corpus), "fixtures": evidence["fixtures"],
            "outputs_qualified": False, "limitations": LIMITATIONS}


def jpeg_tables(quality):
    if type(quality) is not int or quality not in (80, 85, 90):
        raise GateFailure("output_jpeg_quality")
    scale = 200 - 2 * quality
    return {number: [max(1, min(255, (value * scale + 50) // 100)) for value in base]
            for number, base in enumerate((IJG_LUMA, IJG_CHROMA))}


def inspect_output(path, reference, pair, arm):
    no_symlinks(path)
    size = path.stat().st_size
    if size <= 0 or size > LIMITS["output_bytes"]:
        raise GateFailure("output_byte_limit")
    with path.open("rb") as stream:
        raw = stream.read(LIMITS["output_bytes"] + 1)
    if len(raw) != size or len(raw) > LIMITS["output_bytes"]:
        raise GateFailure("output_byte_limit")
    settings = pair[arm + "_settings"]
    expected_mime = P0A.PIL_MIME[settings["format"]]
    with Image.open(BytesIO(raw)) as image:
        if P0A.PIL_MIME.get(image.format) != expected_mime:
            raise GateFailure("output_mime")
        if image.size != (pair["geometry"]["width"], pair["geometry"]["height"]):
            raise GateFailure("output_geometry")
        if type(getattr(image, "n_frames", 1)) is not int or getattr(image, "n_frames", 1) != 1:
            raise GateFailure("output_frames")
        if image.width * image.height > LIMITS["input_pixels"]:
            raise GateFailure("output_pixel_limit")
        image.load()
        if image.mode != "RGB" or "transparency" in image.info:
            raise GateFailure("output_mode_or_alpha")
        if any(image.info.get(key) for key in ("icc_profile", "exif", "xmp", "XML:com.adobe.xmp")) or len(image.getexif()):
            raise GateFailure("output_metadata")
        if settings["format"] == "JPEG" and (JpegImagePlugin.get_sampling(image) != 0 or image.info.get("progressive") or image.info.get("progression")):
            raise GateFailure("output_jpeg_settings")
        if settings["format"] == "JPEG" and not same(image.quantization, jpeg_tables(settings["quality"])):
            raise GateFailure("output_jpeg_quality_tables")
        rgb = image.tobytes()
    lossless = settings["format"] == "PNG" or settings.get("lossless") is True
    # Reference checks also run for lossy arms: their input identity remains mandatory.
    reference_pixels = reference_rgb(reference, pair)
    if lossless and rgb != reference_pixels:
        raise GateFailure("lossless_pixel_mismatch")
    return {"status": "available", "sha256": sha(raw), "bytes": len(raw),
            "actual_mime": expected_mime, "round_trip_decode": True, "verified": True,
            "width": pair["geometry"]["width"], "height": pair["geometry"]["height"],
            "mode": "RGB", "frames": 1, "opaque": True, "icc_present": False,
            "exif_present": False, "xmp_present": False, "decoded_rgb_sha256": sha(rgb),
            "jpeg_quality_tables_verified": True if settings["format"] == "JPEG" else None,
            "lossless_exact": True if lossless else None}


def derive_counts(arms):
    attempts = [a for row in arms for a in row.get("attempts", [])]
    return {"pairs": len({r.get("pair_id") for r in arms}), "scheduled_arms": len(arms),
            "actual_attempts": len(attempts), "successful_attempts": sum(a.get("status") == "success" for a in attempts),
            "failed_attempts": sum(a.get("status") == "failed" for a in attempts),
            "retries": sum(a.get("kind") == "retry" for a in attempts),
            "verified_arms": sum(r.get("status") == "success" for r in arms),
            "failed_arms": sum(r.get("execution_status", r.get("status")) == "failed" for r in arms),
            "not_run_arms": sum(r.get("execution_status", r.get("status")) == "not_run" for r in arms),
            "metric_valid": sum(r.get("status") == "success" for r in arms),
            "visual_passed": 0, "reader_qualified": 0, "native_qualified": 0,
            "quality_qualified_savings": None}


def started_receipt(row, attempt_id, kind, scope_hash):
    return {"id": attempt_id, "pair_id": row["pair_id"], "arm": row["arm"],
            "kind": kind, "settings_sha256": row["settings_sha256"], "scope_sha256": scope_hash}


def expected_request(args, batch, directory, pair, arm):
    return {"inputs": {key: str(getattr(args, key)) for key in
                ("source_root", "reference_root", "preparation_receipt", "freeze_manifest", "corpus", "protocol")},
            "scope": str(batch / "scope.json"), "pair": pair, "arm": arm,
            "output": str(directory / "output.bin"),
            "reference": str(args.reference_root / (pair["reference_id"] + ".pixels"))}


def scan_public(value):
    """Reject access material and machine paths in recursively nested public evidence."""
    report = P0A.Report()
    P0A.scan_private_values(value, report)
    def visit(item):
        if isinstance(item, dict):
            for child in item.values(): visit(child)
        elif isinstance(item, list):
            for child in item: visit(child)
        elif isinstance(item, str) and (re.search(r"(?:/Users/|/private/|/tmp/|file://|https?://|eyJ[a-zA-Z0-9_-]{12,}\.)", item) or "-----BEGIN " in item):
            report.bad("private_path_or_access_material")
    visit(value)
    if report.invalid:
        raise GateFailure("public_privacy_failed")


def cli_inputs(args):
    return ["--source-root", str(args.source_root), "--reference-root", str(args.reference_root),
            "--preparation-receipt", str(args.preparation_receipt), "--freeze-manifest", str(args.freeze_manifest),
            "--corpus", str(args.corpus), "--protocol", str(args.protocol)]


def verify_result(scope, result, batch, args, evidence, inspect=None):
    """Derive completeness from frozen rows and actual attempt files, never counters."""
    report = P0A.Report()
    inventory = []
    corpus = load_json(args.corpus)
    expected_scope = make_scope(corpus, evidence, scope.get("batch_id"))
    if not same(scope, expected_scope): report.bad("execution_scope_mismatch")
    scope_hash = stream_hash(batch / "scope.json")[0]
    if result.get("scope_sha256") != scope_hash: report.bad("execution_scope_hash_mismatch")
    if not same(result.get("limitations"), LIMITATIONS): report.bad("qualification_boundaries_changed")
    if result.get("artifact") != "p0b-encoder-pilot" or type(result.get("schema_version")) is not int or result["schema_version"] != 1:
        report.bad("execution_result_schema")
    if result.get("batch_id") != scope.get("batch_id"): report.bad("batch_identity_mismatch")
    if result.get("prerequisites") != "passed": report.open("prerequisites_not_passed")
    if result.get("stop_reason") is not None: report.open("batch_stopped")
    if not same(result.get("input_snapshot_after"), evidence["input_snapshot"]): report.bad("input_hashes_changed_or_missing")
    if not same(result.get("fixtures"), evidence["fixtures"]): report.bad("rejection_fixture_evidence_changed")
    expected_fixtures = [{"id": row["id"], "expected_rejection": row["expected_rejection"],
                          "actual_rejection": row["expected_rejection"], "synthetic": True, "passed": True}
                         for row in corpus["synthetic_rejection_fixtures"]]
    if not same(evidence["fixtures"], expected_fixtures): report.bad("fixture_categories_or_identities_mismatch")
    fixture_passed = sum(row.get("passed") is True and row.get("actual_rejection") == row.get("expected_rejection")
                         for row in evidence["fixtures"])
    if fixture_passed != 4: report.bad("four_rejection_categories_required")
    rows = result.get("arms")
    if not isinstance(rows, list): report.bad("arms_must_be_array"); rows = []
    schedule = make_schedule(corpus)
    identities = [(r["pair_id"], r["arm"]) for r in schedule]
    actual_identities = [(r.get("pair_id"), r.get("arm")) if isinstance(r, dict) else (None, None) for r in rows]
    if actual_identities != identities: report.bad("missing_duplicate_or_reordered_arms")
    pairs = {p["id"]: p for p in corpus["trial_plan"]["planned_pairs"]}
    seen_attempts = set()
    verified = 0
    expected_attempt_dirs = set()
    integrity_stop_seen = False
    failures = {"encode_failure", "encode_timeout", "readback_failure", "readback_timeout", "prerequisite_integrity_failed"}
    encode_logs = {"encode.stdout", "encode.stderr"}
    all_logs = encode_logs | {"readback.stdout", "readback.stderr"}
    for index, scheduled in enumerate(schedule):
        row = rows[index] if index < len(rows) and isinstance(rows[index], dict) else {}
        pair = pairs[scheduled["pair_id"]]
        attempts = row.get("attempts", [])
        execution_status = row.get("status") if row.get("status") in ("not_run", "success", "failed") else "unverified"
        public = {**scheduled, "status": "not_run", "execution_status": execution_status, "attempts": [], "output": None}
        inventory.append(public)
        if not isinstance(attempts, list): report.bad("attempts_must_be_array"); continue
        if len(attempts) > 2: report.bad("attempt_ceiling_exceeded")
        succeeded = False
        for number, attempt in enumerate(attempts):
            if not isinstance(attempt, dict): report.bad("attempt_must_be_object"); continue
            expected_id = scheduled["initial_id"] if number == 0 else scheduled["retry_id"]
            kind = "initial" if number == 0 else "retry"
            ident = attempt.get("id")
            if ident != expected_id or attempt.get("kind") != kind or ident in seen_attempts:
                report.bad("attempt_identity_or_order_mismatch")
                continue
            seen_attempts.add(ident)
            expected_attempt_dirs.add(ident)
            execution_success = any(a["status"] == "success" for a in public["attempts"])
            if execution_success: report.bad("retry_after_success")
            stopped_before_attempt = integrity_stop_seen
            if stopped_before_attempt: report.bad("attempt_after_prerequisite_integrity_stop")
            status = attempt.get("status") if attempt.get("status") in ("success", "failed") else "unverified"
            raw_failure = attempt.get("failure")
            failure = raw_failure if raw_failure is None or isinstance(raw_failure, str) and raw_failure in failures else "unrecognized_failure"
            # Retain execution history before opening any receipts, logs or outputs.
            # Untrusted observations and failure text never enter public evidence.
            history = {"id": ident, "kind": kind, "status": status, "failure": failure,
                       "observation": None, "logs": {}, "receipts_verified": False,
                       "verification_status": "failed", "verification_failure": "attempt_evidence_unverified"}
            public["attempts"].append(history)
            if status == "failed" and failure == "prerequisite_integrity_failed":
                integrity_stop_seen = True
                if result.get("stop_reason") != "prerequisite_integrity_failed":
                    report.bad("integrity_stop_reason_missing_or_changed")
            directory = batch / "attempts" / ident
            try:
                no_symlinks(directory)
                if not same(load_json(directory / "started.json"), started_receipt(scheduled, ident, kind, scope_hash)):
                    raise GateFailure("attempt_start_receipt_mismatch")
                if not same(load_json(directory / "completed.json"), attempt):
                    raise GateFailure("attempt_completion_receipt_mismatch")
                history["receipts_verified"] = True
                request = directory / "request.json"
                if not same(load_json(request), expected_request(args, batch, directory, pair, scheduled["arm"])):
                    raise GateFailure("attempt_request_mismatch")
                logs = attempt.get("logs")
                if not isinstance(logs, dict):
                    raise GateFailure("attempt_logs_missing")
                required_logs = all_logs if status == "success" or failure in ("readback_failure", "readback_timeout") else encode_logs
                if set(logs) != required_logs:
                    raise GateFailure("attempt_required_logs_missing_or_extra")
                for log, digest in logs.items():
                    if log not in all_logs or not isinstance(digest, str) or not P0A.SHA.fullmatch(digest):
                        raise GateFailure("unsafe_log_identity")
                    if file_identity(directory / log, log)["sha256"] != digest:
                        raise GateFailure("attempt_log_hash")
                    history["logs"][log] = digest
                if stopped_before_attempt or execution_success:
                    raise GateFailure("attempt_outside_frozen_stop_rules")
                if status == "success":
                    if attempt.get("failure") is not None: raise GateFailure("successful_attempt_has_failure")
                    if inspect is None:
                        observed = subprocess.run([sys.executable, str(HERE / "verify-p0b-encoder-pilot.py"),
                            "--inspect-request", str(request)], capture_output=True, timeout=LIMITS["readback_seconds"])
                        if observed.returncode != 0: raise GateFailure("output_reread_failed")
                        observed = json.loads(observed.stdout)["observation"]
                    else:
                        observed = inspect(directory / "output.bin", args.reference_root / (pair["reference_id"] + ".pixels"), pair, scheduled["arm"])
                    if not same(observed, attempt.get("observation")):
                        raise GateFailure("output_observation_changed")
                    public["output"] = observed
                    history["observation"] = observed
                    succeeded = True
                elif status == "failed":
                    if failure not in failures: raise GateFailure("unknown_attempt_failure")
                    if attempt.get("observation") is not None: raise GateFailure("failed_attempt_cannot_qualify_output")
                else: raise GateFailure("invalid_attempt_status")
                history.update(verification_status="verified", verification_failure=None)
            except (OSError, ValueError, KeyError, subprocess.TimeoutExpired) as exc:
                history["verification_failure"] = str(exc) if isinstance(exc, GateFailure) else "attempt_evidence_read_failed"
                report.bad("attempt_receipt_or_output_verification_failed")
        expected_status = "success" if succeeded else "failed" if attempts else "not_run"
        public["status"] = expected_status
        expected_execution = "success" if any(a["status"] == "success" for a in public["attempts"]) else "failed" if attempts else "not_run"
        if row.get("status") != expected_execution: report.bad("arm_execution_status_mismatch")
        if not same(row.get("output"), public["output"]): report.bad("arm_output_mismatch")
        if succeeded: verified += 1
        else: report.open("arm_not_verified:" + scheduled["pair_id"] + ":" + scheduled["arm"])
    if (batch / "attempts").is_dir():
        actual_dirs = {p.name for p in (batch / "attempts").iterdir()}
        if actual_dirs != expected_attempt_dirs: report.bad("unrecorded_or_missing_attempt_directory")
    counts = derive_counts(inventory)
    execution_counts = derive_counts([{**row, "status": row["execution_status"]} for row in inventory])
    if not same(result.get("counts"), execution_counts): report.bad("forged_or_incorrect_counts")
    if len(seen_attempts) > LIMITS["maximum_attempts"]: report.bad("batch_attempt_ceiling_exceeded")
    if verified != 28: report.open("28_verified_arms_required")
    comparisons = []
    by_arm = {(r["pair_id"], r["arm"]): r for r in inventory}
    for pair in corpus["trial_plan"]["planned_pairs"]:
        control, candidate = [by_arm[(pair["id"], arm)]["output"] for arm in ("control", "candidate")]
        delta = None
        if control and candidate and control["bytes"] > 0:
            delta = {"denominator": "normalized-same-geometry-jpeg-q85", "control_bytes": control["bytes"],
                     "candidate_bytes": candidate["bytes"], "candidate_minus_control_bytes": candidate["bytes"] - control["bytes"]}
        comparisons.append({"id": pair["id"], "binding_id": pair["binding_id"], "source_sha256": pair["source_sha256"],
            "reference_id": pair["reference_id"], "reference_pixel_sha256": pair["reference_pixel_sha256"],
            "geometry": pair["geometry"], "role": pair["role"], "source_owner_role": pair["source_owner_role"],
            "cms_binding_source_owner_role": pair["cms_binding_source_owner_role"],
            "experimental_purpose": pair["experimental_purpose"], "control_settings": pair["control_settings"],
            "candidate_settings": pair["candidate_settings"], "original_byte_denominator": pair["original_byte_denominator"],
            "control": control, "candidate": candidate, "raw_byte_delta": delta,
            "visual_verdict": "pending", "reader_verdict": "pending", "native_verdict": "pending",
            "quality_qualified_savings": None})
    output = {"schema_version": 1, "artifact": "p0b-encoder-pilot", "status": report.status(),
        "batch_id": scope.get("batch_id"), "scope_sha256": scope_hash,
        "private_result_sha256": stream_hash(batch / "execution-result.json")[0],
        "provider_discovery_sha256": corpus["trial_plan"]["provider_discovery_sha256"],
        "qualified_codecs": sorted({r["output"]["actual_mime"] for r in inventory if r["output"]}) if not report.invalid else [],
        "counts": counts, "fixture_rejections_verified": fixture_passed, "fixtures": evidence["fixtures"],
        "arms": inventory, "pairs": comparisons, "limitations": LIMITATIONS,
        "diagnostics": [{"severity": "invalid", "category": x} for x in report.invalid] +
                       [{"severity": "incomplete", "category": x} for x in report.incomplete]}
    try: scan_public(output)
    except GateFailure:
        output = {"status": "INVALID", "diagnostics": [{"severity": "invalid", "category": "public_privacy_failed"}]}
    return output


def parser():
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--source-root", type=Path)
    result.add_argument("--reference-root", type=Path)
    result.add_argument("--preparation-receipt", type=Path)
    result.add_argument("--freeze-manifest", type=Path)
    result.add_argument("--corpus", type=Path, default=CORPUS)
    result.add_argument("--protocol", type=Path, default=PROTOCOL)
    result.add_argument("--batch-root", type=Path)
    result.add_argument("--preflight", action="store_true")
    result.add_argument("--inspect-request", type=Path)
    result.add_argument("--integrity-scope", type=Path)
    return result


def retained_failure(batch, args, category):
    """An admission failure still exposes every frozen not-run/failed ledger row."""
    result = {"status": "INVALID", "artifact": "p0b-encoder-pilot", "qualified_codecs": [],
              "limitations": LIMITATIONS,
              "diagnostics": [{"severity": "invalid", "category": category}]}
    try:
        stored = load_json(batch / "execution-result.json")
        recorded = {(row.get("pair_id"), row.get("arm")): row for row in stored.get("arms", [])
                    if isinstance(row, dict)}
        rows = []
        for scheduled in make_schedule(load_json(args.corpus)):
            row = recorded.get((scheduled["pair_id"], scheduled["arm"]), {})
            attempts = []
            for attempt in row.get("attempts", [])[:2]:
                if isinstance(attempt, dict):
                    attempts.append({key: attempt.get(key) for key in ("id", "kind", "status", "failure")})
            execution_status = row.get("status", "not_run")
            rows.append({**scheduled, "status": "pending_verification" if execution_status == "success" else execution_status,
                         "attempts": attempts, "output": None})
        result.update(batch_id=stored.get("batch_id"), arms=rows, counts=derive_counts(rows),
                      scope_sha256=stored.get("scope_sha256"), stop_reason=stored.get("stop_reason"))
        scan_public(result)
    except Exception:
        return {"status": "INVALID", "artifact": "p0b-encoder-pilot", "qualified_codecs": [],
                "limitations": LIMITATIONS,
                "diagnostics": [{"severity": "invalid", "category": category}]}
    return result


def main():
    args = parser().parse_args()
    batch = None
    try:
        if args.integrity_scope:
            check_integrity(args, load_json(private_path(args.integrity_scope)))
            result = {"status": "PASS"}
        elif args.inspect_request:
            request_path = private_path(args.inspect_request)
            request = load_json(request_path)
            private_path(Path(request["output"]))
            private_path(Path(request["reference"]))
            if Path(request["output"]) != request_path.parent / "output.bin" or request["arm"] not in ("control", "candidate"):
                raise GateFailure("attempt_request_mismatch")
            observation = inspect_output(Path(request["output"]), Path(request["reference"]), request["pair"], request["arm"])
            result = {"status": "PASS", "observation": observation}
        elif args.preflight:
            result = preflight(args)
        else:
            batch = safe_batch(args.batch_root, input_paths(args))
            observed = subprocess.run([sys.executable, str(Path(__file__).resolve()), "--preflight", *cli_inputs(args)],
                                      capture_output=True, timeout=LIMITS["preflight_seconds"])
            if observed.returncode != 0: raise GateFailure("prerequisite_reverification_failed")
            evidence = json.loads(observed.stdout)
            result = verify_result(load_json(batch / "scope.json"), load_json(batch / "execution-result.json"), batch, args, evidence)
            scan_public(result)
    except Exception as exc:
        category = str(exc) if isinstance(exc, GateFailure) else "verification_failure:" + type(exc).__name__
        result = retained_failure(batch, args, category) if batch else {
            "status": "INVALID", "diagnostics": [{"severity": "invalid", "category": category}]}
    print(json.dumps(result, indent=2, ensure_ascii=True, allow_nan=False))
    return {"PASS": 0, "INCOMPLETE": 1, "INVALID": 2}[result["status"]]


if __name__ == "__main__":
    raise SystemExit(main())
