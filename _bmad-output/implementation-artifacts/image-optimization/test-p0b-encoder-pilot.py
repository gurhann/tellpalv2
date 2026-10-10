#!/usr/bin/env python3
"""Synthetic regression checks only: no corpus admission or real codec qualification."""

import argparse
import copy
import importlib.util
import json
import os
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image

HERE = Path(__file__).resolve().parent


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


R = load("synthetic_encoder_runner", HERE / "run-p0b-encoder-pilot.py")
V = R.V
JPEG_EXPECTED_TABLES = {
    80: {0: [6,4,4,6,10,16,20,24,5,5,6,8,10,23,24,22,6,5,6,10,16,23,28,22,6,7,9,12,20,35,32,25,
             7,9,15,22,27,44,41,31,10,14,22,26,32,42,45,37,20,26,31,35,41,48,48,40,29,37,38,39,45,40,41,40],
         1: [7,7,10,19,40,40,40,40,7,8,10,26,40,40,40,40,10,10,22,40,40,40,40,40,19,26,40,40,40,40,40,40] + [40]*32},
    85: {0: [5,3,3,5,7,12,15,18,4,4,4,6,8,17,18,17,4,4,5,7,12,17,21,17,4,5,7,9,15,26,24,19,
             5,7,11,17,20,33,31,23,7,11,17,19,24,31,34,28,15,19,23,26,31,36,36,30,22,28,29,29,34,30,31,30],
         1: [5,5,7,14,30,30,30,30,5,6,8,20,30,30,30,30,7,8,17,30,30,30,30,30,14,20,30,30,30,30,30,30] + [30]*32},
    90: {0: [3,2,2,3,5,8,10,12,2,2,3,4,5,12,12,11,3,3,3,5,8,11,14,11,3,3,4,6,10,17,16,12,
             4,4,7,11,14,22,21,15,5,7,11,13,16,21,23,18,10,13,16,17,21,24,24,20,14,18,19,20,22,20,21,20],
         1: [3,4,5,9,20,20,20,20,4,4,5,13,20,20,20,20,5,5,11,20,20,20,20,20,9,13,20,20,20,20,20,20] + [20]*32},
}


class SyntheticPilotTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="tellpal-p0b-synthetic-", dir=Path(tempfile.gettempdir()).resolve())
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.batch = self.root / "batch"
        R.mkdir(self.batch)
        R.mkdir(self.batch / "attempts")
        self.source = self.root / "sources"
        self.reference = self.root / "references"
        R.mkdir(self.source)
        R.mkdir(self.reference)
        self.corpus = copy.deepcopy(V.load_json(V.CORPUS))
        self.pixels = bytes([10, 20, 30, 255, 40, 50, 60, 255, 70, 80, 90, 255,
                             1, 2, 3, 255, 252, 253, 254, 255, 99, 77, 55, 255])
        raw = V.P0A.MAGIC + struct.pack(">II", 2, 3) + self.pixels
        self.pixel_hash = V.sha(raw)
        self.selected = {p["reference_id"] for p in self.corpus["trial_plan"]["planned_pairs"]}
        for pair in self.corpus["trial_plan"]["planned_pairs"]:
            pair["geometry"] = {"width": 2, "height": 3}
            pair["reference_pixel_sha256"] = self.pixel_hash
            path = self.reference / (pair["reference_id"] + ".pixels")
            if not path.exists(): path.write_bytes(raw)
        for ref in self.corpus["normalization"]["correctness"]["prepared_references"]:
            if ref["id"] in self.selected:
                ref.update(width=2, height=3, pixel_sha256=self.pixel_hash)
        plan = self.corpus["trial_plan"]
        synthetic_order = []
        for pair in plan["planned_pairs"]:
            key = {field: plan["seed"] if field == "seed" else pair[field] for field in V.P0A.PAIR_KEY_FIELDS}
            pair["pair_key_sha256"] = V.sha(V.canonical(key))
            digest = V.sha(V.canonical({**plan["offline_order_context"], "pair_key_sha256": pair["pair_key_sha256"]}))
            first = "control" if int(digest[:2], 16) % 2 == 0 else "candidate"
            synthetic_order.append({"pair_id": pair["id"], "context_sha256": digest,
                                    "arms": [first, "candidate" if first == "control" else "control"]})
        plan["offline_order"] = sorted(synthetic_order, key=lambda row: (row["context_sha256"], row["pair_id"]))
        corpus_path = self.root / "synthetic-corpus.json"
        corpus_path.write_bytes(V.canonical(self.corpus))
        self.args = argparse.Namespace(source_root=self.source, reference_root=self.reference,
            preparation_receipt=self.root / "preparation.json", freeze_manifest=self.root / "freeze.json",
            corpus=corpus_path, protocol=V.PROTOCOL, output_root=self.root / "new-batch", batch_id="synthetic-only")
        self.evidence = {"status": "PASS", "input_snapshot": [], "selected_input_snapshot": [],
            "provider_discovery": self.corpus["trial_plan"]["provider_discovery"],
            "fixtures": [{"id": f["id"], "expected_rejection": f["expected_rejection"],
                          "actual_rejection": f["expected_rejection"], "passed": True, "synthetic": True}
                         for f in self.corpus["synthetic_rejection_fixtures"]]}
        self.scope = V.make_scope(self.corpus, self.evidence, "synthetic-only")
        R.exclusive_json(self.batch / "scope.json", self.scope)
        self.schedule = V.make_schedule(self.corpus)
        self.calls = []

    def write_json(self, path, value):
        if path.exists(): os.chmod(path, 0o600)
        path.write_bytes(V.canonical(value) + b"\n")

    def fake_invoke(self, outcomes=None):
        outcomes = list(outcomes or [])
        def invoke(command, timeout, directory, stage):
            self.assertTrue((directory / "started.json").is_file())
            self.calls.append((directory.name, stage))
            expected = V.LIMITS["encode_seconds" if stage == "encode" else "readback_seconds"]
            self.assertEqual(timeout, expected)
            status = outcomes.pop(0) if outcomes else "success"
            request = V.load_json(directory / "request.json")
            stdout = b"{}"
            if status == "integrity":
                status = "failure"
                stdout = V.canonical({"category": "prerequisite_integrity_failed"})
            elif status == "success" and stage == "encode":
                rgb = V.reference_rgb(Path(request["reference"]), request["pair"])
                image = Image.frombytes("RGB", (2, 3), rgb)
                settings = {k: v for k, v in request["pair"][request["arm"] + "_settings"].items()
                            if k not in ("mode", "profile_handling")}
                with Path(request["output"]).open("xb") as stream:
                    image.save(stream, **settings)
                stdout = V.canonical({"status": "PASS"})
            elif status == "success":
                observation = V.inspect_output(Path(request["output"]), Path(request["reference"]), request["pair"], request["arm"])
                stdout = V.canonical({"status": "PASS", "observation": observation})
            logs = {}
            for suffix, raw in (("stdout", stdout), ("stderr", b"synthetic-only")):
                name = stage + "." + suffix
                R.exclusive_bytes(directory / name, raw)
                logs[name] = V.sha(raw)
            return {"status": status, "stdout": stdout, "logs": logs}
        return invoke

    def execute(self, index=0, outcomes=None, integrity=lambda *_: None):
        return R.execute_arm(self.scope, self.schedule[index], self.batch, self.args,
                             invoke=self.fake_invoke(outcomes), integrity=integrity)

    def valid_result(self):
        arms = []
        for row in self.schedule:
            arm, stopped = R.execute_arm(self.scope, row, self.batch, self.args,
                                        invoke=self.fake_invoke(), integrity=lambda *_: None)
            self.assertFalse(stopped)
            arms.append(arm)
        result = {"schema_version": 1, "artifact": "p0b-encoder-pilot", "batch_id": "synthetic-only",
            "scope_sha256": V.stream_hash(self.batch / "scope.json")[0], "prerequisites": "passed",
            "fixtures": self.evidence["fixtures"], "arms": arms, "stop_reason": None,
            "input_snapshot_after": [], "limitations": V.LIMITATIONS, "counts": V.derive_counts(arms)}
        R.exclusive_json(self.batch / "execution-result.json", result)
        return result

    def verify(self, result, scope=None):
        return V.verify_result(scope or self.scope, result, self.batch, self.args, self.evidence, inspect=V.inspect_output)

    def assert_invalid(self, result, scope=None):
        observed = self.verify(result, scope)
        self.assertEqual(observed["status"], "INVALID", observed.get("diagnostics"))
        self.assertEqual(observed["qualified_codecs"], [])
        return observed

    def pair_for(self, codec):
        return next(p for p in self.corpus["trial_plan"]["planned_pairs"] if p["candidate_settings"]["format"] == codec)

    def encode_synthetic(self, pair, *, image=None, extra=None):
        image = image or Image.frombytes("RGBA", (2, 3), self.pixels).convert("RGB")
        path = self.root / "output.bin"
        settings = {k: v for k, v in pair["candidate_settings"].items() if k not in ("mode", "profile_handling")}
        image.save(path, **settings, **(extra or {}))
        return path, self.reference / (pair["reference_id"] + ".pixels")

    def test_frozen_schedule_28_unique_arms_and_56_unique_attempt_ids(self):
        actual = V.make_schedule(V.load_json(V.CORPUS))
        self.assertEqual(len(actual), 28)
        self.assertEqual(len({(r["pair_id"], r["arm"]) for r in actual}), 28)
        self.assertEqual(len({r[k] for r in actual for k in ("initial_id", "retry_id")}), 56)
        for left, right in zip(actual[::2], actual[1::2]):
            self.assertEqual(left["pair_id"], right["pair_id"])
            self.assertEqual({left["arm"], right["arm"]}, {"control", "candidate"})

    def test_success_persists_start_before_encode_and_no_retry(self):
        row, stopped = self.execute()
        self.assertFalse(stopped)
        self.assertEqual(row["status"], "success")
        self.assertEqual(len(row["attempts"]), 1)
        self.assertEqual(row["attempts"][0]["id"], self.schedule[0]["initial_id"])

    def test_encode_failure_retry_retains_both_attempts(self):
        row, stopped = self.execute(outcomes=["failure"])
        self.assertFalse(stopped)
        self.assertEqual([a["status"] for a in row["attempts"]], ["failed", "success"])
        self.assertEqual(row["attempts"][1]["id"], self.schedule[0]["retry_id"])

    def test_readback_failure_retries_once(self):
        row, _ = self.execute(outcomes=["success", "failure"])
        self.assertEqual(row["attempts"][0]["failure"], "readback_failure")
        self.assertEqual(len(row["attempts"]), 2)
        self.assertEqual(row["status"], "success")

    def test_encode_timeout_retries_once(self):
        row, _ = self.execute(outcomes=["timeout"])
        self.assertEqual(row["attempts"][0]["failure"], "encode_timeout")
        self.assertEqual(row["status"], "success")

    def test_readback_timeout_retries_once(self):
        row, _ = self.execute(outcomes=["success", "timeout"])
        self.assertEqual(row["attempts"][0]["failure"], "readback_timeout")
        self.assertEqual(row["status"], "success")

    def test_exhaustion_retains_two_failures_and_next_arm_runs(self):
        row, stopped = self.execute(outcomes=["failure", "timeout"])
        self.assertFalse(stopped)
        self.assertEqual(row["status"], "failed")
        self.assertIsNone(row["output"])
        self.assertEqual(len(row["attempts"]), 2)
        next_row, stopped = self.execute(1)
        self.assertFalse(stopped)
        self.assertEqual(next_row["status"], "success")

    def test_integrity_failure_does_not_retry(self):
        row, stopped = self.execute(outcomes=["integrity"])
        self.assertTrue(stopped)
        self.assertEqual(len(row["attempts"]), 1)
        self.assertEqual(row["attempts"][0]["failure"], "prerequisite_integrity_failed")

    def test_drift_before_retry_retains_initial_failure(self):
        calls = []
        def integrity(*_):
            calls.append(True)
            if len(calls) == 2: raise V.GateFailure("prerequisite_integrity_failed")
        row, stopped = self.execute(outcomes=["failure"], integrity=integrity)
        self.assertTrue(stopped)
        self.assertEqual(len(row["attempts"]), 1)
        self.assertEqual(row["status"], "failed")

    def test_prerequisite_failure_retains_all_not_run_and_no_encode(self):
        def failed(command, timeout, directory, stage):
            self.assertEqual(stage, "preflight")
            self.assertEqual(timeout, 300)
            return {"status": "failure", "stdout": b"{}", "logs": {}}
        with patch("builtins.print"):
            self.assertEqual(R.run_batch(self.args, invoke=failed), 2)
        result = V.load_json(self.args.output_root / "execution-result.json")
        self.assertEqual(len(result["arms"]), 28)
        self.assertTrue(all(a["status"] == "not_run" and a["output"] is None for a in result["arms"]))
        self.assertEqual(result["counts"]["actual_attempts"], 0)
        self.assertFalse((self.args.output_root / "scope.json").exists())
        observed = V.retained_failure(self.args.output_root, self.args, "prerequisite_failure")
        self.assertEqual(len(observed["arms"]), 28)
        self.assertEqual(observed["qualified_codecs"], [])

    def test_preflight_timeout_stops_without_an_encoder_call(self):
        def timeout(command, limit, directory, stage):
            self.assertEqual(stage, "preflight")
            return {"status": "timeout", "stdout": b"", "logs": {}}
        with patch("builtins.print"):
            self.assertEqual(R.run_batch(self.args, invoke=timeout), 2)
        self.assertEqual(V.load_json(self.args.output_root / "execution-result.json")["counts"]["actual_attempts"], 0)

    def test_supervisor_timeout_retains_stdout_stderr_as_private_immutable_logs(self):
        import subprocess
        expired = subprocess.TimeoutExpired(["synthetic"], 120, output=b"partial", stderr=b"failure")
        with patch.object(R.subprocess, "run", side_effect=expired) as run:
            observed = R.supervised(["synthetic"], 120, self.batch, "encode")
        self.assertEqual(run.call_args.kwargs["timeout"], 120)
        self.assertEqual(observed["status"], "timeout")
        self.assertEqual((self.batch / "encode.stdout").read_bytes(), b"partial")
        self.assertEqual((self.batch / "encode.stderr").stat().st_mode & 0o777, 0o400)

    def test_unexpected_fixture_acceptance_prevents_admission(self):
        def intake(path, _):
            if path.parent.name == "fixtures":
                return None
            return None
        with patch.object(V.P0A, "validate"), patch.object(V, "intake", side_effect=intake), \
             patch.object(V, "reference_rgb", return_value=b"synthetic"):
            with self.assertRaisesRegex(V.GateFailure, "fixture_gate_failed"): V.preflight(self.args)

    def test_snapshot_includes_all_60_originals_not_only_cms_specimens(self):
        def identity(path, ident): return {"id": ident, "sha256": "a" * 64, "bytes": 1}
        with patch.object(V, "file_identity", side_effect=identity):
            observed = V.input_snapshot(self.args, self.corpus)
        self.assertEqual(sum(row["id"].startswith("source-") for row in observed), 60)
        self.assertEqual(sum(row["id"].startswith("reference-") for row in observed), 60)

    def test_synthetic_valid_inventory_only_and_14_separate_controls(self):
        observed = self.verify(self.valid_result())
        self.assertEqual(observed["status"], "PASS", observed.get("diagnostics"))
        self.assertEqual(observed["counts"]["verified_arms"], 28)
        self.assertEqual(len([name for name, stage in self.calls if "-control-" in name and stage == "encode"]), 14)
        self.assertIsNone(observed["limitations"]["quality_qualified_savings"])
        self.assertEqual(observed["limitations"]["reader_verdict"], "pending")

    def test_identical_control_hashes_are_allowed_with_distinct_attempts(self):
        observed = self.verify(self.valid_result())
        controls = [r for r in observed["arms"] if r["arm"] == "control"]
        self.assertEqual(len({r["initial_id"] for r in controls}), 14)
        self.assertEqual(len({r["output"]["sha256"] for r in controls}), 1)

    def test_missing_arm_cannot_be_hidden_with_forged_counts(self):
        result = self.valid_result()
        result["arms"].pop()
        self.assert_invalid(result)

    def test_duplicate_arm_is_invalid(self):
        result = self.valid_result()
        result["arms"][1] = copy.deepcopy(result["arms"][0])
        self.assert_invalid(result)

    def test_reordered_arms_are_invalid(self):
        result = self.valid_result()
        result["arms"][0], result["arms"][1] = result["arms"][1], result["arms"][0]
        self.assert_invalid(result)

    def test_counts_cannot_claim_zero_failed_or_extra_verified_arms(self):
        result = self.valid_result()
        for field, value in (("actual_attempts", 0), ("verified_arms", 29), ("retries", True)):
            with self.subTest(field=field):
                changed = copy.deepcopy(result)
                changed["counts"][field] = value
                self.assert_invalid(changed)

    def test_missing_or_forged_fixture_rejection_is_invalid(self):
        result = self.valid_result()
        result["fixtures"][0]["actual_rejection"] = None
        self.assert_invalid(result)

    def test_scope_settings_order_defaults_runtime_and_ids_cannot_change(self):
        result = self.valid_result()
        mutations = [lambda s: s["schedule"].reverse(),
                     lambda s: s["schedule"][0].update(initial_id="forged"),
                     lambda s: s["planned_pairs"][0]["control_settings"].update(quality=80),
                     lambda s: s["provider_discovery"]["runtime"].update(python="wrong"),
                     lambda s: s["effective_defaults"]["webp_lossless_quality"].update(effective=100),
                     lambda s: s["limits"].update(output_bytes=1),
                     lambda s: s.update(outputs_qualified=True)]
        for mutate in mutations:
            scope = copy.deepcopy(self.scope)
            mutate(scope)
            self.assert_invalid(result, scope)

    def test_output_request_cannot_reuse_another_control(self):
        result = self.valid_result()
        controls = [r for r in result["arms"] if r["arm"] == "control"]
        victim = self.batch / "attempts" / controls[1]["attempts"][0]["id"] / "request.json"
        request = V.load_json(victim)
        request["output"] = str(self.batch / "attempts" / controls[0]["attempts"][0]["id"] / "output.bin")
        self.write_json(victim, request)
        self.assert_invalid(result)

    def test_request_pair_reference_or_arm_tamper_is_invalid(self):
        result = self.valid_result()
        victim = self.batch / "attempts" / result["arms"][0]["attempts"][0]["id"] / "request.json"
        original = V.load_json(victim)
        for key, value in (("arm", "candidate" if original["arm"] == "control" else "control"), ("reference", str(self.root / "other.pixels")), ("pair", {})):
            changed = copy.deepcopy(original)
            changed[key] = value
            self.write_json(victim, changed)
            self.assert_invalid(result)
        self.write_json(victim, original)

    def test_corruption_after_runner_readback_fails_independent_verification(self):
        result = self.valid_result()
        path = self.batch / "attempts" / result["arms"][0]["attempts"][0]["id"] / "output.bin"
        self.assertEqual(path.stat().st_mode & 0o777, 0o400)
        os.chmod(path, 0o600)
        path.write_bytes(b"corrupted")
        observed = self.assert_invalid(result)
        self.assertEqual(observed["counts"]["actual_attempts"], 28)
        self.assertEqual(observed["counts"]["successful_attempts"], 28)
        self.assertEqual(observed["counts"]["verified_arms"], 27)
        self.assertEqual(len(observed["arms"][0]["attempts"]), 1)
        self.assertIsNone(observed["arms"][0]["output"])
        self.assertTrue(observed["arms"][0]["attempts"][0]["receipts_verified"])

    def test_missing_log_or_completed_receipt_retains_execution_history(self):
        result = self.valid_result()
        directory = self.batch / "attempts" / result["arms"][0]["attempts"][0]["id"]
        for name in ("encode.stderr", "completed.json"):
            path = directory / name
            raw = path.read_bytes()
            path.unlink()
            observed = self.assert_invalid(result)
            self.assertEqual(observed["counts"]["actual_attempts"], 28)
            self.assertEqual(observed["counts"]["verified_arms"], 27)
            self.assertEqual(len(observed["arms"][0]["attempts"]), 1)
            self.assertIsNone(observed["arms"][0]["attempts"][0]["observation"])
            R.exclusive_bytes(path, raw)

    def test_required_log_pairs_and_hashes_cannot_be_omitted_or_forged(self):
        result = self.valid_result()
        completed = self.batch / "attempts" / result["arms"][0]["attempts"][0]["id"] / "completed.json"
        for logs in ({"encode.stdout": result["arms"][0]["attempts"][0]["logs"]["encode.stdout"]},
                     {key: "0" * 64 for key in result["arms"][0]["attempts"][0]["logs"]}):
            changed = copy.deepcopy(result)
            changed["arms"][0]["attempts"][0]["logs"] = logs
            self.write_json(completed, changed["arms"][0]["attempts"][0])
            observed = self.assert_invalid(changed)
            self.assertEqual(observed["counts"]["actual_attempts"], 28)
            self.assertEqual(observed["counts"]["verified_arms"], 27)

    def test_failed_attempt_requires_complete_log_pairs_for_its_stage(self):
        result = self.valid_result()
        completed = self.batch / "attempts" / result["arms"][0]["attempts"][0]["id"] / "completed.json"
        for category, removed in (("encode_failure", "encode.stderr"), ("readback_failure", "readback.stderr")):
            changed = copy.deepcopy(result)
            row = changed["arms"][0]
            attempt = row["attempts"][0]
            attempt.update(status="failed", failure=category, observation=None)
            if category == "encode_failure":
                attempt["logs"] = {key: value for key, value in attempt["logs"].items() if key.startswith("encode.")}
            attempt["logs"].pop(removed)
            row.update(status="failed", output=None)
            changed["counts"] = V.derive_counts(changed["arms"])
            self.write_json(completed, attempt)
            observed = self.assert_invalid(changed)
            self.assertEqual(observed["counts"]["actual_attempts"], 28)
            self.assertEqual(observed["arms"][0]["attempts"][0]["verification_failure"], "attempt_required_logs_missing_or_extra")

    def test_truthful_integrity_stop_preserves_failed_and_not_run_rows(self):
        first, stopped = self.execute(outcomes=["integrity"])
        self.assertTrue(stopped)
        arms = [first] + [{"pair_id": row["pair_id"], "arm": row["arm"], "status": "not_run", "attempts": [], "output": None}
                          for row in self.schedule[1:]]
        result = {"schema_version": 1, "artifact": "p0b-encoder-pilot", "batch_id": "synthetic-only",
                  "scope_sha256": V.stream_hash(self.batch / "scope.json")[0], "prerequisites": "passed",
                  "fixtures": self.evidence["fixtures"], "arms": arms, "stop_reason": "prerequisite_integrity_failed",
                  "input_snapshot_after": [], "limitations": V.LIMITATIONS, "counts": V.derive_counts(arms)}
        R.exclusive_json(self.batch / "execution-result.json", result)
        observed = self.verify(result)
        self.assertEqual(observed["status"], "INCOMPLETE", observed["diagnostics"])
        self.assertEqual(observed["counts"]["actual_attempts"], 1)
        self.assertEqual(observed["counts"]["not_run_arms"], 27)
        self.assertEqual(observed["qualified_codecs"], [])

    def test_untrusted_observation_or_failure_text_is_not_published(self):
        result = self.valid_result()
        private_value = "/" + "Users" + "/private/example"
        result["arms"][0]["attempts"][0]["observation"]["other"] = private_value
        completed = self.batch / "attempts" / result["arms"][0]["attempts"][0]["id"] / "completed.json"
        self.write_json(completed, result["arms"][0]["attempts"][0])
        observed = self.assert_invalid(result)
        self.assertEqual(observed["counts"]["actual_attempts"], 28)
        self.assertNotIn(private_value, json.dumps(observed))
        V.scan_public(observed)

    def mark_integrity_failure(self, result):
        row = result["arms"][0]
        attempt = row["attempts"][0]
        attempt.update(status="failed", failure="prerequisite_integrity_failed", observation=None,
                       logs={k: v for k, v in attempt["logs"].items() if k.startswith("encode.")})
        row.update(status="failed", output=None)
        result["stop_reason"] = "prerequisite_integrity_failed"
        directory = self.batch / "attempts" / attempt["id"]
        self.write_json(directory / "completed.json", attempt)
        result["counts"] = V.derive_counts(result["arms"])

    def test_later_arms_after_integrity_stop_cannot_qualify(self):
        result = self.valid_result()
        self.mark_integrity_failure(result)
        observed = self.assert_invalid(result)
        self.assertEqual(observed["counts"]["actual_attempts"], 28)
        self.assertEqual(observed["counts"]["verified_arms"], 0)
        self.assertTrue(any(d["category"] == "attempt_after_prerequisite_integrity_stop" for d in observed["diagnostics"]))
        result["stop_reason"] = None
        observed = self.assert_invalid(result)
        self.assertTrue(any(d["category"] == "integrity_stop_reason_missing_or_changed" for d in observed["diagnostics"]))

    def test_retry_after_integrity_stop_cannot_qualify(self):
        result = self.valid_result()
        original = copy.deepcopy(result["arms"][0]["attempts"][0])
        self.mark_integrity_failure(result)
        scheduled = self.schedule[0]
        directory = self.batch / "attempts" / scheduled["retry_id"]
        R.mkdir(directory)
        pair = next(p for p in self.corpus["trial_plan"]["planned_pairs"] if p["id"] == scheduled["pair_id"])
        original_directory = self.batch / "attempts" / scheduled["initial_id"]
        R.exclusive_json(directory / "started.json", V.started_receipt(scheduled, scheduled["retry_id"], "retry", result["scope_sha256"]))
        R.exclusive_json(directory / "request.json", V.expected_request(self.args, self.batch, directory, pair, scheduled["arm"]))
        for name in [*original["logs"], "output.bin"]:
            R.exclusive_bytes(directory / name, (original_directory / name).read_bytes())
        original.update(id=scheduled["retry_id"], kind="retry")
        R.exclusive_json(directory / "completed.json", original)
        result["arms"][0]["attempts"].append(original)
        result["arms"][0].update(status="success", output=original["observation"])
        result["counts"] = V.derive_counts(result["arms"])
        observed = self.assert_invalid(result)
        self.assertEqual(observed["counts"]["actual_attempts"], 29)
        self.assertEqual(observed["counts"]["verified_arms"], 0)

    def test_null_bool_or_wrong_actual_bytes_never_qualify(self):
        result = self.valid_result()
        for value in (None, False, 0, 1):
            changed = copy.deepcopy(result)
            changed["arms"][0]["output"]["bytes"] = value
            changed["arms"][0]["attempts"][0]["observation"]["bytes"] = value
            self.assert_invalid(changed)

    def test_third_attempt_or_retry_before_initial_is_invalid(self):
        result = self.valid_result()
        changed = copy.deepcopy(result)
        changed["arms"][0]["attempts"] *= 3
        self.assert_invalid(changed)
        changed = copy.deepcopy(result)
        changed["arms"][0]["attempts"][0]["id"] = self.schedule[0]["retry_id"]
        self.assert_invalid(changed)

    def test_future_claims_and_quality_savings_are_rejected(self):
        result = self.valid_result()
        for field, value in (("quality_qualified_savings", 0), ("visual_verdict", "pass"),
                             ("reader_verdict", "pass"), ("native_verdict", "pass"),
                             ("java_railway_mobile_qualification", True), ("resource_qualification", True)):
            changed = copy.deepcopy(result)
            changed["limitations"][field] = value
            self.assert_invalid(changed)

    def test_runtime_mismatch_fails_before_encode(self):
        with patch.object(V, "input_snapshot", return_value=[]), patch.object(V.P0A, "installed_provider_discovery", return_value={}):
            with self.assertRaises(V.GateFailure): V.check_integrity(self.args, self.scope)
        self.assertEqual(self.calls, [])

    def test_changed_code_or_input_stops_integrity_gate(self):
        for changed in ("code", "input"):
            with patch.object(V, "input_snapshot", return_value=[] if changed == "code" else [{}]), \
                 patch.object(V, "code_identity", return_value=[]), \
                 patch.object(V.P0A, "installed_provider_discovery", return_value=self.scope["provider_discovery"]):
                with self.assertRaises(V.GateFailure): V.check_integrity(self.args, self.scope)

    def test_truncated_intake_rejected(self):
        path = self.root / "truncated.bin"
        path.write_bytes(b"\xff\xd8\xff")
        self.assertEqual(V.intake(path, "image/jpeg"), "decode_truncated")

    def test_mime_mismatch_intake_rejected(self):
        path, _ = self.encode_synthetic(self.pair_for("PNG"))
        self.assertEqual(V.intake(path, "image/jpeg"), "declared_actual_mime_mismatch")

    def test_excessive_bytes_rejected_before_read(self):
        path = self.root / "large.bin"
        with path.open("wb") as stream: stream.truncate(V.LIMITS["input_bytes"] + 1)
        with patch.object(Path, "open", side_effect=AssertionError("must not read")):
            self.assertEqual(V.intake(path, "image/jpeg"), "source_byte_limit")

    def test_excessive_pixels_rejected_before_decode(self):
        path = self.root / "pixels.bin"
        path.write_bytes(b"synthetic header")
        class Header:
            width, height, format = 5001, 8000, "PNG"
            def __enter__(self): return self
            def __exit__(self, *_): pass
            def load(self): raise AssertionError("must not decode")
        with patch.object(V.Image, "open", return_value=Header()):
            self.assertEqual(V.intake(path, "image/png"), "source_pixel_limit")

    def test_over_twice_pixel_cap_bomb_error_is_pixel_category(self):
        path = self.root / "bomb.bin"
        path.write_bytes(b"synthetic header")
        with patch.object(V.Image, "open", side_effect=Image.DecompressionBombError("synthetic >80M")):
            self.assertEqual(V.intake(path, "image/png"), "source_pixel_limit")

    def test_reference_opacity_hash_magic_axes_and_length_gates(self):
        pair = self.pair_for("PNG")
        path = self.reference / (pair["reference_id"] + ".pixels")
        original = path.read_bytes()
        mutations = [original[:-1], b"X" + original[1:],
                     original[:len(V.P0A.MAGIC)] + struct.pack(">II", 3, 2) + original[len(V.P0A.MAGIC) + 8:],
                     original[:-1] + b"\xfe"]
        for changed in mutations:
            path.write_bytes(changed)
            copied = copy.deepcopy(pair)
            copied["reference_pixel_sha256"] = V.sha(changed)
            with self.assertRaises(V.GateFailure): V.reference_rgb(path, copied)
        path.write_bytes(original)
        copied = copy.deepcopy(pair)
        copied["reference_pixel_sha256"] = "0" * 64
        with self.assertRaises(V.GateFailure): V.reference_rgb(path, copied)

    def test_png_lossless_exact_equality(self):
        pair = self.pair_for("PNG")
        output, reference = self.encode_synthetic(pair)
        self.assertTrue(V.inspect_output(output, reference, pair, "candidate")["lossless_exact"])
        image = Image.new("RGB", (2, 3), (1, 2, 3))
        output, reference = self.encode_synthetic(pair, image=image)
        with self.assertRaisesRegex(V.GateFailure, "lossless_pixel_mismatch"):
            V.inspect_output(output, reference, pair, "candidate")

    def test_webp_lossless_exact_equality(self):
        pair = next(p for p in self.corpus["trial_plan"]["planned_pairs"] if p["candidate_settings"].get("lossless") is True)
        output, reference = self.encode_synthetic(pair)
        self.assertTrue(V.inspect_output(output, reference, pair, "candidate")["lossless_exact"])
        output, reference = self.encode_synthetic(pair, image=Image.new("RGB", (2, 3), (5, 6, 7)))
        with self.assertRaisesRegex(V.GateFailure, "lossless_pixel_mismatch"):
            V.inspect_output(output, reference, pair, "candidate")

    def test_lossy_decoded_difference_is_not_lossless_failure(self):
        pair = self.pair_for("JPEG")
        output, reference = self.encode_synthetic(pair, image=Image.new("RGB", (2, 3), (1, 2, 3)))
        self.assertIsNone(V.inspect_output(output, reference, pair, "candidate")["lossless_exact"])

    def test_output_wrong_mime_geometry_alpha_and_metadata_rejected(self):
        pair = self.pair_for("PNG")
        tests = [(Image.new("RGB", (3, 2)), {}), (Image.new("RGBA", (2, 3)), {}),
                 (Image.new("RGB", (2, 3)), {"icc_profile": b"synthetic-profile"})]
        exif = Image.Exif(); exif[274] = 1
        tests.append((Image.new("RGB", (2, 3)), {"exif": exif.tobytes()}))
        for image, extra in tests:
            output, reference = self.encode_synthetic(pair, image=image, extra=extra)
            with self.assertRaises(V.GateFailure): V.inspect_output(output, reference, pair, "candidate")
        output, reference = self.encode_synthetic(pair)
        Image.new("RGB", (2, 3)).save(output, format="JPEG")
        with self.assertRaises(V.GateFailure): V.inspect_output(output, reference, pair, "candidate")

    def test_nonstatic_and_xmp_outputs_rejected(self):
        pair = self.pair_for("PNG")
        output, reference = self.encode_synthetic(pair)
        Image.new("RGB", (2, 3), "red").save(output, format="PNG", save_all=True,
            append_images=[Image.new("RGB", (2, 3), "blue")], duration=100, loop=0)
        with self.assertRaises(V.GateFailure): V.inspect_output(output, reference, pair, "candidate")
        webp = self.pair_for("WEBP")
        output, reference = self.encode_synthetic(webp, extra={"xmp": b"synthetic-xmp"})
        with self.assertRaises(V.GateFailure): V.inspect_output(output, reference, webp, "candidate")

    def test_missing_empty_and_oversize_outputs_rejected(self):
        pair = self.pair_for("PNG")
        reference = self.reference / (pair["reference_id"] + ".pixels")
        output = self.root / "empty.bin"
        with self.assertRaises(OSError): V.inspect_output(output, reference, pair, "candidate")
        output.touch()
        with self.assertRaises(V.GateFailure): V.inspect_output(output, reference, pair, "candidate")
        with output.open("wb") as stream: stream.truncate(V.LIMITS["output_bytes"] + 1)
        with self.assertRaises(V.GateFailure): V.inspect_output(output, reference, pair, "candidate")

    def test_limited_writer_retains_partial_and_refuses_over_cap(self):
        from io import BytesIO
        sink = BytesIO(); writer = R.LimitedWriter(sink)
        with patch.dict(V.LIMITS, output_bytes=4):
            writer.write(b"abc")
            with self.assertRaises(V.GateFailure): writer.write(b"de")
        self.assertEqual(sink.getvalue(), b"abc")

    def test_wrong_or_omitted_jpeg_quality_is_detected_independently(self):
        pair = self.pair_for("JPEG")
        output, reference = self.encode_synthetic(pair)
        Image.new("RGB", (2, 3)).save(output, format="JPEG", subsampling=0)
        with self.assertRaisesRegex(V.GateFailure, "output_jpeg_quality_tables"):
            V.inspect_output(output, reference, pair, "candidate")
        for quality in (80, 85, 90):
            self.assertEqual(V.jpeg_tables(quality), JPEG_EXPECTED_TABLES[quality])

    def test_real_save_worker_path_on_tiny_synthetic_all_frozen_settings(self):
        for number, pair in enumerate(self.corpus["trial_plan"]["planned_pairs"]):
            for arm in ("control", "candidate"):
                output = self.root / ("synthetic-" + str(number) + "-" + arm + ".bin")
                request = {"inputs": R.input_request(self.args), "scope": str(self.batch / "scope.json"),
                           "pair": pair, "arm": arm, "output": str(output),
                           "reference": str(self.reference / (pair["reference_id"] + ".pixels"))}
                with patch.object(V, "check_integrity"), patch("builtins.print"):
                    R.encode_worker(request)
                observed = V.inspect_output(output, Path(request["reference"]), pair, arm)
                self.assertTrue(observed["verified"])
                self.assertEqual(output.stat().st_mode & 0o777, 0o400)
                settings = pair[arm + "_settings"]
                if settings["format"] == "JPEG":
                    with Image.open(output) as image:
                        self.assertEqual(image.quantization, JPEG_EXPECTED_TABLES[settings["quality"]])

    def test_existing_batch_or_receipt_never_overwritten(self):
        sentinel = self.batch / "sentinel.txt"
        sentinel.write_bytes(b"preserved")
        with self.assertRaises(V.GateFailure): V.safe_batch(self.batch, [], new=True)
        with self.assertRaises(FileExistsError): R.exclusive_json(self.batch / "scope.json", {})
        self.assertEqual(sentinel.read_bytes(), b"preserved")

    def test_root_repo_overlap_ancestor_descendant_and_symlinks_rejected(self):
        for output, inputs in ((V.REPO / "new-private-batch", []), (self.root, [self.source]),
                               (self.source / "nested", [self.source])):
            with self.assertRaises(V.GateFailure): V.safe_batch(output, inputs, new=True)
        link = self.root / "symlink"
        link.symlink_to(self.source, target_is_directory=True)
        with self.assertRaises(V.GateFailure): V.safe_batch(link / "new", [], new=True)
        file_link = self.root / "linked.json"
        file_link.symlink_to(self.batch / "scope.json")
        with self.assertRaises(V.GateFailure): V.load_json(file_link)

    def test_raw_symlink_parent_traversal_rejected_before_normalization(self):
        link = self.root / "linked-dir"
        link.symlink_to(self.source, target_is_directory=True)
        with self.assertRaisesRegex(V.GateFailure, "unsafe_path"):
            V.no_symlinks(link / ".." / "escaped.bin")

    def test_worker_rejects_repo_or_input_descendant_output_before_save(self):
        pair = self.corpus["trial_plan"]["planned_pairs"][0]
        for output in (V.REPO / "synthetic-worker-output.bin", self.source / "unsafe.bin", self.reference / "unsafe.bin"):
            request = {"inputs": R.input_request(self.args), "scope": str(self.batch / "scope.json"),
                       "pair": pair, "arm": "control", "output": str(output),
                       "reference": str(self.reference / (pair["reference_id"] + ".pixels"))}
            with patch.object(V, "check_integrity"), patch.object(Image.Image, "save") as save:
                with self.assertRaises(V.GateFailure): R.encode_worker(request)
            save.assert_not_called()
            self.assertFalse(output.exists())

    def test_malformed_corpus_or_schedule_creates_no_partial_batch(self):
        original = copy.deepcopy(self.corpus)
        mutations = [lambda c: c["trial_plan"]["planned_pairs"].pop(),
                     lambda c: c["trial_plan"]["planned_pairs"].append(c["trial_plan"]["planned_pairs"][0]),
                     lambda c: c["trial_plan"]["offline_order"].reverse(),
                     lambda c: c["trial_plan"]["planned_pairs"][0]["planned_attempt_ids"]["control"].__setitem__(0, "forged-id")]
        for mutate in mutations:
            changed = copy.deepcopy(original); mutate(changed)
            self.write_json(self.args.corpus, changed)
            with self.assertRaises(V.GateFailure): R.run_batch(self.args)
            self.assertFalse(self.args.output_root.exists())
        self.args.corpus.write_bytes(b"malformed JSON")
        with self.assertRaises(ValueError): R.run_batch(self.args)
        self.assertFalse(self.args.output_root.exists())
        self.args.corpus = self.root / "missing-corpus.json"
        with self.assertRaises(OSError): R.run_batch(self.args)
        self.assertFalse(self.args.output_root.exists())

    def test_real_supervisor_spawn_failure_retries_and_continues(self):
        with patch.object(R.subprocess, "run", side_effect=OSError("synthetic spawn failure")):
            for index in (0, 1):
                row, stopped = R.execute_arm(self.scope, self.schedule[index], self.batch, self.args,
                    invoke=R.supervised, integrity=lambda *_: None)
                self.assertFalse(stopped)
                self.assertEqual(len(row["attempts"]), 2)
                self.assertEqual([a["failure"] for a in row["attempts"]], ["encode_failure", "encode_failure"])
                for attempt in row["attempts"]:
                    directory = self.batch / "attempts" / attempt["id"]
                    self.assertTrue((directory / "completed.json").is_file())
                    self.assertEqual(set(attempt["logs"]), {"encode.stdout", "encode.stderr"})

    def test_parent_integrity_timeout_is_bounded_and_retains_prior_history(self):
        import subprocess
        from types import SimpleNamespace
        with patch.object(V.subprocess, "run", return_value=SimpleNamespace(returncode=0, stdout=b'{"status":"PASS"}')):
            first, stopped = R.execute_arm(self.scope, self.schedule[0], self.batch, self.args, invoke=self.fake_invoke())
        self.assertFalse(stopped)
        with patch.object(V.P0A, "installed_provider_discovery", side_effect=AssertionError("parent discovery forbidden")), \
             patch.object(V.subprocess, "run", side_effect=subprocess.TimeoutExpired(["synthetic"], 300)) as run:
            second, stopped = R.execute_arm(self.scope, self.schedule[1], self.batch, self.args, invoke=self.fake_invoke())
        self.assertTrue(stopped)
        self.assertEqual(run.call_args.kwargs["timeout"], 300)
        self.assertIn("--integrity-scope", run.call_args.args[0])
        self.assertEqual(len(first["attempts"]), 1)
        self.assertEqual(second["attempts"], [])

    def test_parent_integrity_runtime_mismatch_stops_without_retry(self):
        from types import SimpleNamespace
        with patch.object(V.subprocess, "run", return_value=SimpleNamespace(returncode=2, stdout=b'{"status":"INVALID"}')):
            row, stopped = R.execute_arm(self.scope, self.schedule[0], self.batch, self.args, invoke=self.fake_invoke())
        self.assertTrue(stopped)
        self.assertEqual(row["attempts"], [])
        self.assertEqual(self.calls, [])

    def test_missing_input_or_discovery_error_is_terminal_integrity_category(self):
        with patch.object(V, "input_snapshot", side_effect=FileNotFoundError("synthetic missing input")):
            with self.assertRaisesRegex(V.GateFailure, "prerequisite_integrity_failed"):
                V.check_integrity(self.args, self.scope)
        with patch.object(V, "input_snapshot", return_value=[]), \
             patch.object(V.P0A, "installed_provider_discovery", side_effect=OSError("synthetic missing provider")):
            with self.assertRaisesRegex(V.GateFailure, "prerequisite_integrity_failed"):
                V.check_integrity(self.args, self.scope)

    def test_duplicate_json_and_nonfinite_values_rejected(self):
        path = self.root / "malformed.json"
        for raw in (b'{"id":1,"id":2}', b'{"value":NaN}'):
            path.write_bytes(raw)
            with self.assertRaises(ValueError): V.load_json(path)

    def test_nested_privacy_rejected_opaque_metadata_allowed(self):
        V.scan_public({"opaque_id": "synthetic-1", "sha256": "a" * 64, "bytes": 123})
        fields = ["access" + "_url", "to" + "ken", "creden" + "tial", "source" + "_path"]
        values = ["https" + "://host.invalid/?to" + "ken=abcdefgh",
                  "/" + "Users" + "/private/example", "-----BE" + "GIN PRIVATE KEY-----"]
        for field in fields:
            with self.assertRaises(V.GateFailure): V.scan_public({"nested": [{field: "private-value"}]})
        for value in values:
            with self.assertRaises(V.GateFailure): V.scan_public({"nested": [{"other": value}]})

    def test_verifier_does_not_import_encoder(self):
        source = (HERE / "verify-p0b-encoder-pilot.py").read_text()
        self.assertNotIn('module("encoder', source)
        self.assertNotIn("import run_p0b", source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
