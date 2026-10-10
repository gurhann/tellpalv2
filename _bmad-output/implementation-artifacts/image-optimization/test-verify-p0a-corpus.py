#!/usr/bin/env python3
"""Focused negative coverage for the Story 1.2 verifier.

Run with --source-root pointing at the private {sha256}.source directory.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import importlib.util
import io
import json
import struct
import shutil
import subprocess
import tempfile
import unittest
import zlib
from contextlib import contextmanager
from pathlib import Path
from unittest import mock

from PIL import Image

HERE = Path(__file__).resolve().parent
VERIFY = HERE / "verify-p0a-corpus.py"
CORPUS = HERE / "p0a-corpus.json"


class CorpusVerifierTests(unittest.TestCase):
    source_root: Path
    reference_root: Path | None = None
    preparation_receipt: Path | None = None
    original: dict

    @classmethod
    def setUpClass(cls):
        if not cls.source_root:
            raise unittest.SkipTest("--source-root is required")
        cls.original = json.loads(CORPUS.read_text(encoding="utf-8"))

    def detached_manifest(self, data, corpus_bytes):
        """Bind isolated fixture bytes without reusing the production freeze."""
        freeze = data["freeze"]
        reference_set = [{key: ref.get(key) for key in (
            "id", "source_hash", "pixel_sha256", "width", "height", "implementation_sha256",
            "destination_profile_sha256", "recipe_version")}
            for ref in data["normalization"]["correctness"]["prepared_references"]]
        reference_set.sort(key=lambda ref: str(ref["id"]))
        serialized = json.dumps(reference_set, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode()
        manifest = {key: freeze[key] for key in (
            "revision_id", "corpus_snapshot_id", "protocol_snapshot_id", "protocol_sha256")}
        manifest.update(schema_version=1, corpus_sha256=hashlib.sha256(corpus_bytes).hexdigest(),
            normalization_implementation_sha256=data["normalization"]["recipe"]["implementation"]["script_sha256"],
            reference_set_sha256=hashlib.sha256(serialized).hexdigest())
        return manifest

    def run_case(self, mutate, verify_receipt=True, with_manifest=False, mutate_manifest=None,
                 protocol_bytes=None, preparation_receipt=None):
        with tempfile.TemporaryDirectory(prefix="p0a-verifier-") as tmp:
            path = Path(tmp) / "corpus.json"
            data = copy.deepcopy(self.original)
            mutate(data)
            path.write_text(json.dumps(data), encoding="utf-8")
            command = ["python3", str(VERIFY), "--source-root", str(self.source_root), "--corpus", str(path)]
            if self.reference_root: command += ["--reference-root", str(self.reference_root)]
            receipt = preparation_receipt if preparation_receipt is not None else self.preparation_receipt
            if verify_receipt and receipt: command += ["--preparation-receipt", str(receipt)]
            if protocol_bytes is not None:
                protocol = Path(tmp) / "p0a-trial-protocol.md"
                protocol.write_bytes(protocol_bytes)
                command += ["--protocol", str(protocol)]
            if with_manifest:
                manifest = self.detached_manifest(data, path.read_bytes())
                if mutate_manifest: mutate_manifest(manifest)
                manifest_path = Path(tmp) / "freeze.json"
                manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
                command += ["--freeze-manifest", str(manifest_path)]
            completed = subprocess.run(command, text=True, capture_output=True)
            self.assertIn(completed.returncode, (0, 1, 2), completed.stdout + completed.stderr)
            report = json.loads(completed.stdout)
            status_codes = {"PASS": 0, "INCOMPLETE": 1, "INVALID": 2}
            self.assertIn(report.get("status"), status_codes, report)
            self.assertEqual(completed.returncode, status_codes[report["status"]], report)
            return report

    @contextmanager
    def privacy_probe(self):
        """Own a uniquely named scratch artifact and remove only that artifact."""
        fake_url = "https://storage.googleapis.com/bucket/object?" + "si" + "g=" + "constructed"
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", prefix="p0a-privacy-probe-",
                                         suffix=".json", dir=HERE, delete=False) as stream:
            target = Path(stream.name)
            json.dump({"nested": {"value": fake_url}}, stream)
        try:
            yield target
        finally:
            target.unlink(missing_ok=True)

    def verifier_module(self):
        spec = importlib.util.spec_from_file_location("p0a_probe_verify", VERIFY)
        verifier = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(verifier)
        return verifier

    def run_validation_case(self, mutate, unreadable_path=None, protocol_bytes=None):
        """Combine a malformed contract with independent source/privacy/freeze probes."""
        verifier = self.verifier_module()
        data = copy.deepcopy(self.original)
        mutate(data)
        source_hash = self.original["specimens"][1]["integrity"]["sha256"]
        source = self.source_root.resolve() / f"{source_hash}.source"
        unreadable = unreadable_path.resolve() if unreadable_path is not None else None
        raw = bytearray(source.read_bytes())
        raw[-1] ^= 1
        with tempfile.TemporaryDirectory(prefix="p0a-independent-probes-") as tmp, self.privacy_probe() as privacy:
            corrupted = Path(tmp) / source.name
            corrupted.write_bytes(raw)
            protocol = HERE / "p0a-trial-protocol.md"
            if protocol_bytes is not None:
                protocol = Path(tmp) / protocol.name
                protocol.write_bytes(protocol_bytes)
            original_open = Path.open
            def read_probe(path, *args, **kwargs):
                mode = args[0] if args else kwargs.get("mode", "r")
                if "r" in mode:
                    if unreadable is not None and path.resolve() == unreadable:
                        raise PermissionError("constructed read denial")
                    if path.resolve() == source:
                        return original_open(corrupted, *args, **kwargs)
                return original_open(path, *args, **kwargs)
            report = verifier.Report()
            with mock.patch.object(Path, "open", autospec=True, side_effect=read_probe):
                try:
                    verifier.validate(data, self.source_root.resolve(), protocol,
                        self.reference_root.resolve() if self.reference_root else None,
                        self.preparation_receipt if self.preparation_receipt else None,
                        report, None, hashlib.sha256(json.dumps(data).encode()).hexdigest())
                except Exception as exc:
                    report.bad(f"verifier internal validation failure: {type(exc).__name__}")
            rows = ([{"severity": "invalid", "message": message} for message in report.invalid] +
                    [{"severity": "incomplete", "message": message} for message in report.incomplete] +
                    [{"severity": "note", "message": message} for message in getattr(report, "notes", [])])
            return {"status": report.status(), "diagnostics": rows, "privacy_probe_name": privacy.name}

    def assert_mixed_invalid(self, report, expected=None):
        self.assertEqual(report["status"], "INVALID", report["diagnostics"])
        messages = [item["message"] for item in report["diagnostics"]]
        self.assertFalse(any("internal validation failure" in message for message in messages), messages)
        self.assertTrue(any("rehash mismatch" in message for message in messages), messages)
        self.assertTrue(any(f"private access material in {report['privacy_probe_name']}" in message for message in messages), messages)
        self.assertTrue(any(item["severity"] == "incomplete" and "detached immutable freeze manifest" in item["message"]
                            for item in report["diagnostics"]), messages)
        if expected:
            self.assertTrue(any(expected in message for message in messages), messages)

    def test_missing_coverage_cell_is_incomplete(self):
        report = self.run_case(lambda data: data["coverage_matrix"].pop(0))
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("mandatory coverage cell missing" in x["message"] for x in report["diagnostics"]))

    def test_missing_trial_plan_is_incomplete(self):
        report = self.run_case(lambda data: data.pop("trial_plan"))
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("predeclared trial plan is missing" in x["message"] for x in report["diagnostics"]))

    def test_wrong_scheduled_counts_are_invalid(self):
        report = self.run_case(lambda data: data["trial_plan"]["counts"].__setitem__("base_native_observations", 1))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("scheduled observation counts mismatch" in x["message"] for x in report["diagnostics"]))

    def test_missing_outputs_cannot_be_zero_byte_successes(self):
        report = self.run_case(lambda data: data["trial_plan"]["byte_eligibility"].__setitem__("missing_output_is_zero", True))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("byte eligibility rules mismatch" in x["message"] for x in report["diagnostics"]))

    def test_seed_change_requires_protocol_revision(self):
        report = self.run_case(lambda data: data["trial_plan"].__setitem__("seed", 1))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("trial rule mismatch: seed" in x["message"] for x in report["diagnostics"]))

    def test_comparison_reference_must_belong_to_bound_source(self):
        def mutate(data):
            binding = data["bindings"][0]
            specimen = next(x for x in data["specimens"] if x["id"] == binding["specimen_id"])
            source_hash = specimen["integrity"]["sha256"]
            reference = next(x for x in data["normalization"]["correctness"]["prepared_references"] if x["source_hash"] != source_hash)
            data["trial_plan"].update(status="PREREGISTERED", native_environments=[{"id": "isolated-test-environment"}],
                counts={"pairs": 1, "environments": 1, "base_native_observations": 30, "maximum_native_attempts": 60},
                planned_pairs=[{"id": "wrong-source-reference", "binding_id": binding["id"],
                    "source_sha256": source_hash, "reference_id": reference["id"],
                    "reference_pixel_sha256": reference["pixel_sha256"],
                    "geometry": {"width": reference["width"], "height": reference["height"]},
                    "role": binding["role"], "control_settings": {"format": "JPEG", "quality": 85},
                    "candidate_settings": {"format": "JPEG", "quality": 80},
                    "baseline_denominator": "same-role-jpeg-q85", "arm_type": "same_geometry_codec",
                    "freeze_revision_id": data["freeze"]["revision_id"], "state": "not_run"}])
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("reference belongs to another source" in x["message"] for x in report["diagnostics"]))

    def test_preregistered_comparison_requires_admitted_reference(self):
        def mutate(data):
            self.draft_current_prerequisites(data, unadmit=True)
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertFalse(any(x["severity"] == "invalid" for x in report["diagnostics"]))
        self.assertTrue(any("comparison pair reference admission unresolved" in x["message"] for x in report["diagnostics"]))

    def test_duplicate_binding_tuple_is_invalid(self):
        def mutate(data):
            data["bindings"].append(copy.deepcopy(data["bindings"][0]))
            data["bindings"][-1]["id"] = "duplicate-binding"
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("duplicate binding tuple" in x["message"] for x in report["diagnostics"]))

    def test_shared_hash_distinct_binding_remains_distinct(self):
        source_hash = "78ea1f1d05beefa3a2b35c2327a9ef7358c8894bcc5374c97a9399ac7a8982fc"
        specimens = [row for row in self.original["specimens"] if row["integrity"]["sha256"] == source_hash]
        self.assertEqual({row["id"] for row in specimens}, {
            "cms-content71-cover-source-9888", "cms-content71-asset9905-page17", "cms-content71-asset9906-page18"})
        bindings = [row for row in self.original["bindings"] if row["specimen_id"] in {specimen["id"] for specimen in specimens}]
        self.assertEqual(len(bindings), 3)
        self.assertEqual(len({row["id"] for row in bindings}), 3)
        self.assertEqual(len({(row["content_id"], row["asset_id"], row["page_number"], row["source_owner_role"]) for row in bindings}), 3)
        self.assertEqual(self.original["counts"]["cms_unique_retained_originals"], 53)
        self.assertEqual(self.original["counts"]["cms_binding_records"], 55)
        report = self.run_case(lambda data: data)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertFalse(any(x["severity"] == "invalid" for x in report["diagnostics"]))
        self.assertFalse(any("duplicate binding tuple" in x["message"] for x in report["diagnostics"]))

    def test_retained_identity_and_backlink_conservation_cannot_be_forged(self):
        def drop_source(data):
            specimen = data["specimens"].pop(3)
            data["bindings"] = [row for row in data["bindings"] if row["specimen_id"] != specimen["id"]]
            data["byte_observations"] = [row for row in data["byte_observations"] if row["id"] != specimen["id"]]
            data["counts"].update(cms_unique_retained_originals=52, total_unique_retained_originals=59,
                                  cms_binding_records=54)

        def rename_binding(data):
            binding = data["bindings"][3]
            previous = binding["id"]
            binding["id"] = "renamed-retained-binding"
            for specimen in data["specimens"]:
                specimen["bindings"] = [binding["id"] if ident == previous else ident for ident in specimen["bindings"]]
            for ref in data["normalization"]["correctness"]["prepared_references"]:
                ref["binding_ids"] = [binding["id"] if ident == previous else ident for ident in ref["binding_ids"]]

        def swap_source_labels(data):
            first, second = data["specimens"][2:4]
            replacements = {first["id"]: second["id"], second["id"]: first["id"]}
            first["id"], second["id"] = second["id"], first["id"]
            for binding in data["bindings"]:
                binding["specimen_id"] = replacements.get(binding["specimen_id"], binding["specimen_id"])
            for observation in data["byte_observations"]:
                observation["id"] = replacements.get(observation["id"], observation["id"])
            for cell in data["coverage_matrix"]:
                cell["specimen_ids"] = [replacements.get(ident, ident) for ident in cell["specimen_ids"]]

        def drop_history(data):
            historical = data["historical_controls"].pop(0)
            data["byte_observations"] = [row for row in data["byte_observations"] if row["id"] != historical["id"]]
            data["counts"].update(historical_regression_controls=5, total_unique_retained_originals=59)

        def drop_binding(data, hide_count=False):
            data["bindings"] = [row for row in data["bindings"] if row["id"] != "binding-content10-asset11219"]
            if hide_count:
                data["counts"]["cms_binding_records"] = 54

        mutations = [("source deletion", drop_source, "retained specimen identities/ownership/links differ"),
                     ("binding rename", rename_binding, "retained binding identities/ownership/links differ"),
                     ("backlink removal", lambda data: data["specimens"][3].__setitem__("bindings", []), "ownership backlink mismatch"),
                     ("source identity swap", swap_source_labels, "retained specimen identities/ownership/links differ"),
                     ("binding ownership", lambda data: data["bindings"][3].__setitem__("asset_id", 9778), "retained binding identities/ownership/links differ"),
                     ("binding deletion", drop_binding, "retained binding identities/ownership/links differ"),
                     ("binding deletion hidden by counts", lambda data: drop_binding(data, hide_count=True), "retained binding identities/ownership/links differ"),
                     ("historical deletion", drop_history, "retained historical identity set differs")]
        for label, mutate, expected in mutations:
            with self.subTest(case=label):
                self.assert_mixed_invalid(self.run_validation_case(mutate), expected)

    def test_public_counts_are_derived_from_retained_records(self):
        keys = ("cms_unique_retained_originals", "historical_regression_controls", "historical_color_diagnostics",
                "total_unique_retained_originals", "cms_binding_records", "source_story_count",
                "prepared_unadmitted_references", "admitted_pilot_references", "synthetic_rejection_fixtures",
                "source_story_ids")
        for key in keys:
            def mutate(data):
                value = data["counts"][key]
                data["counts"][key] = value[:-1] if isinstance(value, list) else value + 1
            with self.subTest(field=key):
                self.assert_mixed_invalid(self.run_validation_case(mutate), f"counts.{key} differs from derived retained records")

    def test_unselected_unknown_metadata_does_not_change_retained_identity(self):
        for field in ("frames", "icc"):
            def mutate(data):
                specimen = next(row for row in data["specimens"] if row["id"] == "cms-content71-cover-source-9888")
                specimen["metadata"][field] = None
            with self.subTest(field=field):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INCOMPLETE", report["diagnostics"])
                messages = [item["message"] for item in report["diagnostics"]]
                self.assertFalse(any(item["severity"] == "invalid" for item in report["diagnostics"]), messages)
                self.assertFalse(any("internal validation failure" in message for message in messages), messages)
                self.assertTrue(any("cms-content71-cover-source-9888" in message and "unavailable" in message for message in messages), messages)

    def test_all_observation_identities_are_retained_and_missing_evidence_stays_open(self):
        expected_ids = {row["id"] for key in ("specimens", "historical_controls") for row in self.original[key]}
        self.assertEqual(len(expected_ids), 62)
        self.assertEqual({row["id"] for row in self.original["byte_observations"]}, expected_ids)
        self.assertEqual(len(self.original["byte_observations"]), 62)
        ident = self.original["specimens"][3]["id"]
        mutations = [("missing row", lambda data: data.__setitem__("byte_observations", [row for row in data["byte_observations"] if row["id"] != ident]), "full byte metadata observation missing")]
        for field in ("sha256", "bytes", "width", "frames", "exif", "icc", "decode_status"):
            def mutate(data, field=field):
                observation = next(row for row in data["byte_observations"] if row["id"] == ident)
                if field == "frames":
                    observation.pop(field)
                else:
                    observation[field] = None
            expected = ("observation source hash unavailable" if field == "sha256" else
                        "observation full decode evidence unavailable" if field == "decode_status" else
                        f"observation {field} unavailable")
            mutations.append((field, mutate, expected))
        for label, mutate, expected in mutations:
            with self.subTest(field=label):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INCOMPLETE", report["diagnostics"])
                messages = [item["message"] for item in report["diagnostics"]]
                self.assertFalse(any(item["severity"] == "invalid" for item in report["diagnostics"]), messages)
                self.assertFalse(any("internal validation failure" in message for message in messages), messages)
                self.assertTrue(any(ident in message and expected in message for message in messages), messages)

    def test_forged_observation_metadata_and_duplicate_identities_are_invalid(self):
        ident = self.original["specimens"][3]["id"]
        def row(data):
            return next(item for item in data["byte_observations"] if item["id"] == ident)
        def forge_tagged_hash(data):
            next(item for item in data["byte_observations"] if item["icc"]["present"])["icc"]["sha256"] = "0" * 64
        def tagged_icc(data):
            return next(item for item in data["byte_observations"] if item["icc"]["present"])["icc"]
        def duplicate(data):
            data["byte_observations"].append(copy.deepcopy(row(data)))
        def add_unknown(data):
            observation = copy.deepcopy(row(data))
            observation["id"] = "unknown-observation"
            data["byte_observations"].append(observation)
        mutations = [("byte count", lambda data: row(data).__setitem__("bytes", row(data)["bytes"] + 1), "bytes differs from private source"),
                     ("axis value", lambda data: row(data).__setitem__("width", 1), "width differs from private source"),
                     ("axis type", lambda data: row(data).__setitem__("width", float(row(data)["width"])), "width differs from private source"),
                     ("MIME", lambda data: row(data).__setitem__("actual_mime", "image/png"), "actual_mime differs from private source"),
                     ("alpha", lambda data: row(data).__setitem__("has_alpha", True), "has_alpha differs from private source"),
                     ("frame count", lambda data: row(data).__setitem__("frames", 2), "frames differs from private source"),
                     ("EXIF", lambda data: row(data)["exif"].__setitem__("present", False), "exif metadata differs from private source"),
                     ("ICC hash", forge_tagged_hash, "icc metadata differs from private source"),
                     ("ICC validity", lambda data: tagged_icc(data).__setitem__("valid", False), "icc valid differs from private source"),
                     ("ICC color space", lambda data: tagged_icc(data).__setitem__("profile_color_space", "CMYK"), "icc profile_color_space differs from private source"),
                     ("sRGB assumption", lambda data: row(data)["icc"].__setitem__("assumed_srgb", False), "icc assumed_srgb differs from private source"),
                     ("absent ICC hash", lambda data: row(data)["icc"].__setitem__("sha256", "0" * 64), "absent ICC metadata has a contradictory profile hash"),
                     ("source hash", lambda data: row(data).__setitem__("sha256", "0" * 64), "observation source hash/link differs"),
                     ("decode status", lambda data: row(data).__setitem__("decode_status", "not_decoded"), "observation full decode evidence differs"),
                     ("duplicate ID", duplicate, "duplicate byte observation ID"),
                     ("unknown ID", add_unknown, "malformed or unknown retained identity"),
                     ("renamed ID", lambda data: row(data).__setitem__("id", "renamed-observation"), "malformed or unknown retained identity")]
        for label, mutate, expected in mutations:
            with self.subTest(case=label):
                self.assert_mixed_invalid(self.run_validation_case(mutate), expected)

    def test_role_only_binding_change_remains_duplicate(self):
        def mutate(data):
            duplicate = copy.deepcopy(data["bindings"][0])
            duplicate["id"] = "role-only-duplicate"
            duplicate["role"] = "story_cover_source"
            data["bindings"].append(duplicate)
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("duplicate binding tuple" in x["message"] for x in report["diagnostics"]))

    def test_rgb_specimen_cannot_satisfy_feature_cells(self):
        for cell_id, message in (("static-listening-alpha", "alpha"), ("reading-page-icc", "ICC"), ("reading-page-exif-rotation", "EXIF"), ("reading-page-exif-mirroring", "EXIF")):
            def mutate(data, cell_id=cell_id):
                cell = next(x for x in data["coverage_matrix"] if x["id"] == cell_id)
                cell.update(status="satisfied", specimen_ids=[data["specimens"][0]["id"]], hashes=[data["specimens"][0]["integrity"]["sha256"]], suitability_evidence="forged feature claim")
            report = self.run_case(mutate)
            self.assertEqual(report["status"], "INVALID", cell_id)
            self.assertTrue(any(message.lower() in x["message"].lower() for x in report["diagnostics"]), cell_id)

    def test_fully_opaque_rgba_original_cannot_satisfy_real_alpha(self):
        def mutate(data):
            specimen = next(x for x in data["specimens"] if
                            x["metadata"].get("has_alpha") is True and
                            x["metadata"].get("alpha_extrema") == [255, 255])
            cell = next(x for x in data["coverage_matrix"] if x["class"] == "real_alpha")
            cell.update(status="satisfied", specimen_ids=[specimen["id"]],
                        hashes=[specimen["integrity"]["sha256"]],
                        suitability_evidence="Alpha channel incorrectly claimed as transparency")
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("does not demonstrate required class" in x["message"]
                            for x in report["diagnostics"]))

    def test_retained_original_bound_rejects_sixty_first_unique_hash(self):
        def mutate(data):
            template = copy.deepcopy(data["specimens"][0])
            for index in range(13):
                specimen = copy.deepcopy(template)
                specimen["id"] = f"bound-overflow-{index}"
                specimen["integrity"]["sha256"] = f"{index + 1:064x}"
                specimen["bindings"] = []
                data["specimens"].append(specimen)
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("retained original count exceeds 60" in x["message"]
                            for x in report["diagnostics"]))

    def test_story_bound_rejects_fifth_unique_content(self):
        def mutate(data):
            template = copy.deepcopy(data["specimens"][0])
            for index, content_id in enumerate((99, 100)):
                specimen = copy.deepcopy(template)
                specimen["id"] = f"story-bound-overflow-{index}"
                specimen["content_id"] = content_id
                specimen["asset_id"] = 90000 + index
                specimen["page_number"] = None
                specimen["bindings"] = []
                data["specimens"].append(specimen)
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("source acquisition exceeds content 71 plus 3 story bound" in x["message"]
                            for x in report["diagnostics"]))

    def test_tampered_bound_metadata_is_invalid(self):
        report = self.run_case(lambda data: data["source_policy"].__setitem__("maximum_retained_originals", 61))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("maximum_retained_originals must be 60" in x["message"]
                            for x in report["diagnostics"]))

    def test_indexed_png_palette_expands_to_exact_rgba(self):
        spec = importlib.util.spec_from_file_location("p0a_prepare", HERE / "prepare-p0a-normalization.py")
        helper = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(helper)
        image = Image.new("P", (2, 2))
        image.putpalette([17, 101, 233, 80, 120, 160, 0, 255, 13, 33, 44, 55])
        image.putdata([0, 1, 2, 3])
        image.info["transparency"] = bytes([0, 128, 255, 255])
        payload = io.BytesIO()
        image.save(payload, format="PNG")
        normalized, _ = helper.normalize(payload.getvalue(), b"")
        self.assertEqual(normalized.size, (2, 2))
        self.assertEqual(list(normalized.getdata()), [
            (17, 101, 233, 0), (80, 120, 160, 128),
            (0, 255, 13, 255), (33, 44, 55, 255),
        ])

    def test_packed_four_bit_palette_png_normalizes_exactly(self):
        spec = importlib.util.spec_from_file_location("p0a_prepare", HERE / "prepare-p0a-normalization.py")
        helper = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(helper)
        image = Image.new("P", (2, 2))
        image.putpalette([17, 101, 233, 80, 120, 160, 0, 255, 13, 33, 44, 55])
        image.putdata([0, 1, 2, 3])
        image.info["transparency"] = bytes([0, 128, 255, 255])
        payload = io.BytesIO(); image.save(payload, format="PNG", bits=4)
        encoded = payload.getvalue()
        self.assertEqual((encoded[24], encoded[25]), (4, 3))
        normalized, _ = helper.normalize(encoded, b"")
        self.assertEqual(list(normalized.getdata()), [
            (17, 101, 233, 0), (80, 120, 160, 128),
            (0, 255, 13, 255), (33, 44, 55, 255),
        ])

    def test_sixteen_bit_grayscale_and_truecolor_pngs_remain_rejected(self):
        spec = importlib.util.spec_from_file_location("p0a_prepare", HERE / "prepare-p0a-normalization.py")
        helper = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(helper)
        image = Image.new("I", (1, 1)); image.putdata([65535])
        payload = io.BytesIO(); image.save(payload, format="PNG")
        encoded = payload.getvalue()
        self.assertEqual((encoded[24], encoded[25]), (16, 0))
        with self.assertRaisesRegex(ValueError, "unsupported PNG bit depth"):
            helper.normalize(encoded, b"")
        def chunk(kind, payload):
            return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff)
        rgb16 = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 16, 2, 0, 0, 0)) +
                 chunk(b"IDAT", zlib.compress(b"\x00\xff\xff\x00\x00\x00\x00")) + chunk(b"IEND", b""))
        self.assertEqual((rgb16[24], rgb16[25]), (16, 2))
        with self.assertRaisesRegex(ValueError, "unsupported PNG bit depth"):
            helper.normalize(rgb16, b"")

    def test_actual_retained_palette_source_has_no_false_depth_or_srgb_gap(self):
        palette = next(specimen for specimen in self.original["specimens"]
                        if specimen.get("asset_id") == 11219)
        self.assertEqual(palette["metadata"]["mode"], "P")
        self.assertTrue(palette["metadata"]["icc"].get("assumed_srgb"))
        report = self.run_case(lambda data: data, verify_receipt=False)
        messages = [item["message"] for item in report["diagnostics"]]
        self.assertFalse(any(palette["id"] in message and "unsupported PNG" in message for message in messages))
        self.assertFalse(any(palette["id"] in message and "untagged sRGB assumption missing" in message for message in messages))

    def test_altered_source_is_invalid_and_incomplete_is_preserved(self):
        with tempfile.TemporaryDirectory(prefix="p0a-source-") as tmp:
            root = Path(tmp)
            for source in self.source_root.glob("*.source"):
                shutil.copyfile(source, root / source.name)
            target = next(root.glob("24d27b*.source"))
            data = bytearray(target.read_bytes()); data[-1] ^= 1; target.write_bytes(data)
            path = root / "corpus.json"; payload = copy.deepcopy(self.original); payload["coverage_matrix"].pop(); path.write_text(json.dumps(payload), encoding="utf-8")
            completed = subprocess.run(["python3", str(VERIFY), "--source-root", str(root), "--corpus", str(path)], text=True, capture_output=True)
            report = json.loads(completed.stdout)
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any(x["severity"] == "incomplete" for x in report["diagnostics"]))
            self.assertTrue(any("rehash mismatch" in x["message"] for x in report["diagnostics"]))

    def test_freeze_mismatch_is_invalid(self):
        report = self.run_case(lambda data: data["freeze"].__setitem__("protocol_sha256", "0" * 64))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("protocol hash mismatch" in x["message"] for x in report["diagnostics"]))

    def test_missing_detached_manifest_prevents_handoff(self):
        report = self.run_case(lambda data: data)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("detached immutable freeze manifest is not supplied" in x["message"]
                            for x in report["diagnostics"]))

    def test_detached_freeze_binds_actual_corpus_protocol_and_reference_set(self):
        for field in ("corpus_sha256", "protocol_sha256", "normalization_implementation_sha256",
                      "reference_set_sha256", "revision_id", "corpus_snapshot_id", "protocol_snapshot_id"):
            with self.subTest(field=field):
                report = self.run_case(lambda data: data, with_manifest=True,
                    mutate_manifest=lambda manifest: manifest.__setitem__(field, "changed"))
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any(f"detached freeze manifest mismatch: {field}" in x["message"]
                                    for x in report["diagnostics"]))

    def test_valid_r3_preparation_passes_only_with_matching_detached_freeze(self):
        if not self.reference_root or not self.preparation_receipt:
            self.skipTest("independent reference/receipt inputs are required for PASS")
        report = self.run_case(lambda data: data, with_manifest=True)
        self.assertEqual(report["status"], "PASS", report["diagnostics"])

    def test_failed_unadmitted_correctness_is_incomplete(self):
        def mutate(data):
            self.draft_current_prerequisites(data, unadmit=True)
            data["normalization"]["correctness"]["checks"] = [{"id": "exif-1", "result": "failed"}]
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INCOMPLETE")

    def test_failed_admitted_reference_is_invalid(self):
        def mutate(data):
            data["normalization"]["correctness"]["admitted_references"] = [{"id": "bad-ref", "correctness_result": "failed", "pixel_sha256": "0" * 64}]
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("admitted reference is not independently correct" in x["message"] for x in report["diagnostics"]))

    def test_empty_correctness_checks_is_incomplete(self):
        def mutate(data):
            self.draft_current_prerequisites(data, unadmit=True)
            data["normalization"]["correctness"]["checks"] = []
        report = self.run_case(mutate, verify_receipt=False)
        self.assertEqual(report["status"], "INCOMPLETE")

    def test_missing_metadata_remains_open(self):
        report = self.run_case(lambda data: data.__setitem__("byte_observations", []), verify_receipt=False)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("metadata observation missing" in x["message"] for x in report["diagnostics"]))

    def test_nested_private_url_is_rejected(self):
        with self.privacy_probe() as target:
            report = self.run_case(lambda data: data)
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any(f"private access material in {target.name}" in x["message"] for x in report["diagnostics"]))
            self.assertFalse(any("internal validation failure" in x["message"] for x in report["diagnostics"]))
        self.assertFalse(target.exists())

    def test_protocol_freeze_hash_uses_exact_crlf_bytes(self):
        original = (HERE / "p0a-trial-protocol.md").read_bytes()
        self.assertNotIn(b"\r\n", original)
        crlf = original.replace(b"\n", b"\r\n")
        self.assertNotEqual(hashlib.sha256(original).digest(), hashlib.sha256(crlf).digest())
        self.assert_mixed_invalid(self.run_validation_case(lambda data: data, protocol_bytes=crlf), "protocol hash mismatch")
        if not self.reference_root or not self.preparation_receipt:
            self.skipTest("private prepared references and receipt are required for the matching-hash positive control")
        def mutate(data):
            data["freeze"]["protocol_sha256"] = hashlib.sha256(crlf).hexdigest()
        report = self.run_case(mutate, protocol_bytes=crlf, with_manifest=True)
        self.assertEqual(report["status"], "PASS", report["diagnostics"])
        self.assertFalse(any("internal validation failure" in item["message"] for item in report["diagnostics"]))

    def test_preparation_receipt_cli_symlink_preserves_later_diagnostics(self):
        if not self.preparation_receipt:
            self.skipTest("private preparation receipt is required")
        with tempfile.TemporaryDirectory(prefix="p0a-receipt-link-") as tmp, self.privacy_probe() as target:
            receipt_link = Path(tmp) / "receipt.json"
            receipt_link.symlink_to(self.preparation_receipt)
            report = self.run_case(lambda data: data["specimens"][0]["integrity"].__setitem__(
                "bytes", data["specimens"][0]["integrity"]["bytes"] + 1), preparation_receipt=receipt_link)
            self.assertEqual(report["status"], "INVALID", report["diagnostics"])
            messages = [item["message"] for item in report["diagnostics"]]
            self.assertFalse(any("internal validation failure" in message for message in messages), messages)
            self.assertTrue(any("preparation receipt" in message and "without symlinks" in message for message in messages), messages)
            self.assertTrue(any("recorded byte count differs" in message for message in messages), messages)
            self.assertTrue(any(f"private access material in {target.name}" in message for message in messages), messages)
            self.assertTrue(any(item["severity"] == "incomplete" and "detached immutable freeze manifest" in item["message"]
                                for item in report["diagnostics"]), messages)
        self.assertFalse(target.exists())

    def test_privacy_probe_cleanup_owns_only_its_unique_path(self):
        with self.privacy_probe() as first:
            with self.privacy_probe() as second:
                self.assertNotEqual(first, second)
                self.assertTrue(first.exists())
                self.assertTrue(second.exists())
            self.assertFalse(second.exists())
            self.assertTrue(first.exists())
        self.assertFalse(first.exists())

    def test_failed_check_with_pass_admission_is_invalid(self):
        def mutate(data):
            data["normalization"]["correctness"]["checks"] = [{"id": "failed-check", "result": "failed"}]
            data["normalization"]["correctness"]["admitted_references"] = [{"id": "bad-admission", "check_id": "failed-check", "correctness_result": "pass", "pixel_sha256": "0" * 64}]
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")

    def test_unsafe_prepared_reference_path_is_invalid(self):
        def mutate(data):
            data["normalization"]["correctness"]["prepared_references"][0]["id"] = "../outside"
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")

    def test_prepared_reference_symlink_is_invalid(self):
        with tempfile.TemporaryDirectory(prefix="p0a-ref-") as tmp:
            refroot = Path(tmp)
            ref = self.original["normalization"]["correctness"]["prepared_references"][0]
            (refroot / f"{ref['id']}.pixels").symlink_to(self.source_root / next(self.source_root.glob("*.source")).name)
            data = copy.deepcopy(self.original); path = refroot / "corpus.json"; path.write_text(json.dumps(data), encoding="utf-8")
            completed = subprocess.run(["python3", str(VERIFY), "--source-root", str(self.source_root), "--reference-root", str(refroot), "--corpus", str(path)], text=True, capture_output=True)
            report = json.loads(completed.stdout)
            self.assertEqual(report["status"], "INVALID")

    def test_altered_normalized_reference_hash_is_invalid(self):
        def mutate(data):
            data["normalization"]["correctness"]["prepared_references"][0]["pixel_sha256"] = "0" * 64
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")

    def test_forged_feature_metadata_is_invalid(self):
        def mutate(data):
            metadata = data["specimens"][0]["metadata"]
            metadata["has_alpha"] = True; metadata["alpha_extrema"] = [0, 255]
            metadata["icc"] = {"present": True, "sha256": "0" * 64}
            metadata["exif"] = {"present": True, "orientation": 7}
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("alpha metadata differs" in x["message"] for x in report["diagnostics"]))

    def test_required_alpha_cannot_be_deferred(self):
        def mutate(data):
            cell = next(x for x in data["coverage_matrix"] if x["id"] == "static-listening-alpha")
            cell["applicable"] = False; cell["status"] = "deferred"
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")

    def test_ready_pilot_requires_links_and_admitted_references(self):
        def mutate(data): data["pilot_subset"] = {"status":"READY","bindings":[],"references":[{"id":"missing"}],"coverage_matrix":[]}
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")

    def test_freeze_ids_must_be_bound_by_protocol(self):
        def mutate(data):
            for key in ("revision_id", "corpus_snapshot_id", "protocol_snapshot_id"):
                data["freeze"][key] = "changed-" + key
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")

    def test_source_symlink_is_invalid(self):
        with tempfile.TemporaryDirectory(prefix="p0a-symlink-") as tmp:
            root = Path(tmp)
            for source in self.source_root.glob("*.source"): shutil.copyfile(source, root / source.name)
            (root / "24d27b3384c5ecf706f1c4458bc3bc869a2e55e85e4ca46e449a4e5dbdcbe075.source").unlink()
            (root / "24d27b3384c5ecf706f1c4458bc3bc869a2e55e85e4ca46e449a4e5dbdcbe075.source").symlink_to(next(self.source_root.glob("24d27b*.source")))
            path = root / "corpus.json"; path.write_text(json.dumps(self.original), encoding="utf-8")
            completed = subprocess.run(["python3", str(VERIFY), "--source-root", str(root), "--corpus", str(path)], text=True, capture_output=True)
            report = json.loads(completed.stdout)
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any("must not be a symlink" in x["message"] for x in report["diagnostics"]))

    def test_missing_top_level_and_source_corruption_retain_all_diagnostics(self):
        with tempfile.TemporaryDirectory(prefix="p0a-mixed-") as tmp:
            root = Path(tmp)
            for source in self.source_root.glob("*.source"): shutil.copyfile(source, root / source.name)
            target = next(root.glob("24d27b*.source")); data = bytearray(target.read_bytes()); data[-1] ^= 1; target.write_bytes(data)
            path = root / "corpus.json"; payload = copy.deepcopy(self.original); del payload["coverage_matrix"]; path.write_text(json.dumps(payload), encoding="utf-8")
            completed = subprocess.run(["python3", str(VERIFY), "--source-root", str(root), "--corpus", str(path)], text=True, capture_output=True)
            report = json.loads(completed.stdout)
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any("missing top-level field" in x["message"] for x in report["diagnostics"]))
            self.assertTrue(any("rehash mismatch" in x["message"] for x in report["diagnostics"]))

    def test_malformed_gate_status_retains_later_integrity_diagnostics(self):
        def mutate(data):
            data["gates"][0]["status"] = []
            data["normalization"]["correctness"]["prepared_references"][0]["pixel_sha256"] = "0" * 64
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("invalid gate status" in x["message"] for x in report["diagnostics"]))
        self.assertTrue(any("preparation reference identity differs" in x["message"] for x in report["diagnostics"]))
        self.assertTrue(any(x["severity"] == "incomplete" for x in report["diagnostics"]))

    def pilot_specimen(self, data):
        binding_id = data["pilot_subset"]["bindings"][0]
        binding = next(x for x in data["bindings"] if x["id"] == binding_id)
        return next(x for x in data["specimens"] if x["id"] == binding["specimen_id"])

    def draft_current_prerequisites(self, data, unadmit=False):
        """Represent an unfinished preparation without retaining a false READY claim."""
        data["pilot_subset"]["status"] = "DRAFT"
        data["freeze"]["freeze_ready"] = False
        if unadmit:
            data["normalization"]["correctness"]["admitted_references"] = []
            data["counts"]["prepared_unadmitted_references"] = 60
            data["counts"]["admitted_pilot_references"] = 0
            selected = {ref["id"] for ref in data["pilot_subset"]["references"]}
            for ref in data["normalization"]["correctness"]["prepared_references"]:
                if ref["id"] in selected: ref["status"] = "unadmitted_preflight"
            for ref in data["pilot_subset"]["references"]: ref["status"] = "unadmitted_preflight"
            for cell in data["pilot_subset"]["coverage_matrix"]:
                if cell["required_for_current_pilot"]:
                    cell["status"] = "pending_admission"

    def add_draft_pair(self, data, binding=None):
        binding = binding or next(x for x in data["bindings"] if x["id"] == data["pilot_subset"]["bindings"][0])
        specimen = next(x for x in data["specimens"] if x["id"] == binding["specimen_id"])
        source_hash = specimen["integrity"]["sha256"]
        reference = next(x for x in data["normalization"]["correctness"]["prepared_references"] if x["source_hash"] == source_hash)
        pair = copy.deepcopy(data["trial_plan"]["planned_pairs"][0])
        pair.update(id="draft-scope-probe", binding_id=binding["id"], source_sha256=source_hash,
                    reference_id=reference["id"], reference_pixel_sha256=reference["pixel_sha256"],
                    geometry={"width": reference["width"], "height": reference["height"]},
                    role=binding["role"], source_owner_role=binding["role"],
                    cms_binding_source_owner_role=binding["source_owner_role"])
        data["trial_plan"].update(planned_pairs=[pair],
            counts={"pairs": 1, "environments": 0, "base_native_observations": 0, "maximum_native_attempts": 0})
        return pair

    def test_narrow_scope_keeps_current_coverage_and_future_gates_separate(self):
        report = self.run_case(lambda data: data)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertFalse(any(x["severity"] == "invalid" for x in report["diagnostics"]))
        messages = [x["message"] for x in report["diagnostics"]]
        self.assertFalse(any("required coverage unresolved" in x or "G-COVERAGE: unresolved" in x for x in messages))
        self.assertFalse(any("pilot role geometry unavailable" in x or "pilot coverage unresolved" in x for x in messages))
        self.assertEqual(self.original["pilot_subset"]["status"], "READY")
        self.assertTrue(all(x["status"] == "unavailable" for x in self.original["pilot_subset"]["geometry_evidence"]))
        self.assertTrue(all(x["status"] == "open" for x in self.original["future_qualification"]["gates"]))

    def test_opaque_rgba_is_eligible_without_qualifying_real_alpha(self):
        # The predicate accepts opaque RGBA, but r3 does not authorize a third binding.
        spec = importlib.util.spec_from_file_location("p0a_verify", VERIFY)
        verifier = importlib.util.module_from_spec(spec)
        assert spec.loader is not None
        spec.loader.exec_module(verifier)
        specimen = next(x for x in self.original["specimens"] if x["metadata"].get("alpha_extrema") == [255, 255])
        binding = next(x for x in self.original["bindings"] if x["specimen_id"] == specimen["id"])
        reference = next(x for x in self.original["normalization"]["correctness"]["prepared_references"]
                         if x["source_hash"] == specimen["integrity"]["sha256"])
        report = verifier.Report()
        verifier.validate_pilot_binding(binding, specimen, reference, "isolated-opaque-rgba-predicate", report)
        self.assertEqual(report.status(), "PASS", report.invalid)
        self.assertFalse(verifier.feature_matches(specimen, "real_alpha"))

    def test_draft_pilot_rejects_unknown_transparent_or_exif_tagged_sources(self):
        cases = [({"frames": None}, "exactly one frame"), ({"frames": True}, "exactly one frame"),
                 ({"frames": 1.0}, "exactly one frame"), ({"frames": 2}, "exactly one frame"),
                 ({"has_alpha": None}, "alpha predicate is unknown"),
                 ({"has_alpha": True, "alpha_extrema": [0, 255]}, "not opaque"),
                 ({"has_alpha": True, "alpha_extrema": [255.0, 255.0]}, "metadata is incomplete"),
                 ({"exif": {"present": True, "orientation": 1}}, "explicitly absent EXIF"),
                 ({"exif": {"present": True, "orientation": 6}}, "explicitly absent EXIF"),
                 ({"exif": {"present": False}}, "explicitly absent EXIF"),
                 ({"exif": {"present": None, "orientation": None}}, "metadata is incomplete"),
                 ({"icc": None}, "metadata is incomplete")]
        for fields, expected in cases:
            with self.subTest(fields=fields):
                report = self.run_case(lambda data: self.pilot_specimen(data)["metadata"].update(fields))
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("pilot binding" in x["message"] and expected in x["message"] for x in report["diagnostics"]))

    def test_every_draft_comparison_checks_source_scope(self):
        def mutate(data):
            self.add_draft_pair(data)
            self.pilot_specimen(data)["metadata"]["exif"] = {"present": True, "orientation": 1}
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("comparison pair draft-scope-probe: pilot source requires explicitly absent EXIF" in x["message"] for x in report["diagnostics"]))

    def test_future_gates_cannot_be_removed_closed_or_stripped(self):
        mutations = [lambda data: data.pop("future_qualification"),
                     lambda data: data["future_qualification"]["gates"].pop(),
                     lambda data: data["future_qualification"]["gates"][0].__setitem__("status", "satisfied"),
                     lambda data: data["future_qualification"]["gates"][0].pop("requirement"),
                     lambda data: data["future_qualification"]["gates"][0].pop("next_action"),
                     lambda data: data["future_qualification"]["gates"][0].pop("evidence"),
                     lambda data: data["future_qualification"].__setitem__("synthetic_checks_do_not_close_gates", False)]
        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("future qualification" in x["message"] for x in report["diagnostics"]))

    def test_future_coverage_gate_links_and_visibility_are_mandatory(self):
        for location in ("coverage_matrix", "pilot_subset"):
            def mutate(data):
                cells = data[location] if location == "coverage_matrix" else data[location]["coverage_matrix"]
                next(x for x in cells if x["class"] == "real_alpha").pop("future_qualification_gate_id")
            report = self.run_case(mutate)
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any("gate linkage mismatch" in x["message"] for x in report["diagnostics"]))
        report = self.run_case(lambda data: data["coverage_matrix"].remove(next(x for x in data["coverage_matrix"] if x["class"] == "real_alpha")))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("future qualification coverage cell missing" in x["message"] for x in report["diagnostics"]))

    def test_missing_current_text_or_icc_pilot_coverage_is_incomplete(self):
        for cls in ("real_text_diacritics", "icc_color_conversion"):
            def mutate(data):
                self.draft_current_prerequisites(data)
                cells = data["pilot_subset"]["coverage_matrix"]
                cells.remove(next(x for x in cells if x["class"] == cls))
            report = self.run_case(mutate)
            self.assertEqual(report["status"], "INCOMPLETE")
            self.assertTrue(any(f"pilot coverage missing: {cls}" in x["message"] for x in report["diagnostics"]))

    def test_draft_missing_pilot_cell_with_null_links_is_incomplete(self):
        def mutate(data):
            self.draft_current_prerequisites(data)
            cell = data["pilot_subset"]["coverage_matrix"][0]
            cell["status"] = "missing"
            cell["next_action"] = "Supply the missing source and independently admitted reference."
            for key in ("source_binding_id", "source_sha256", "reference_id", "reference_pixel_sha256", "suitability_evidence"):
                cell[key] = None
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("pilot coverage unresolved: real_text_diacritics (missing)" in x["message"] for x in report["diagnostics"]))

    def test_ready_pilot_cannot_claim_missing_current_coverage(self):
        def mutate(data):
            data["pilot_subset"]["status"] = "READY"
            data["pilot_subset"]["coverage_matrix"][0]["status"] = "missing"
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("READY pilot has unresolved coverage" in x["message"] for x in report["diagnostics"]))

    def test_pilot_reference_projection_is_exact(self):
        for field, value in (("source_hash", "0" * 64), ("pixel_sha256", "0" * 64), ("width", 1), ("status", "unadmitted_preflight")):
            report = self.run_case(lambda data: data["pilot_subset"]["references"][0].__setitem__(field, value))
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any("pilot reference projection differs" in x["message"] for x in report["diagnostics"]))

    def test_comparison_role_and_pilot_membership_are_bound(self):
        report = self.run_case(lambda data: self.add_draft_pair(data).__setitem__("role", "listening_art"))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("role differs from source binding" in x["message"] for x in report["diagnostics"]))
        def mutate(data):
            binding = next(x for x in data["bindings"] if x["id"] not in data["pilot_subset"]["bindings"])
            self.add_draft_pair(data, binding)
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("outside selected pilot bindings/references" in x["message"] for x in report["diagnostics"]))

    def test_malformed_r2_contract_retains_later_integrity_and_incomplete_diagnostics(self):
        mutations = [lambda data: data["pilot_scope"].__setitem__("required_for_current_pilot", [{}]),
                     lambda data: data["future_qualification"]["gates"][0].__setitem__("id", []),
                     lambda data: data["pilot_subset"].__setitem__("bindings", [{}]),
                     lambda data: data["pilot_subset"]["references"][0].__setitem__("id", []),
                     lambda data: data["pilot_subset"]["coverage_matrix"][0].__setitem__("class", []),
                     lambda data: self.add_draft_pair(data).__setitem__("id", [])]
        for index, mutation in enumerate(mutations):
            def mutate(data):
                mutation(data)
                data["normalization"]["correctness"]["prepared_references"][0]["pixel_sha256"] = "0" * 64
            with self.subTest(index=index):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                messages = [x["message"] for x in report["diagnostics"]]
                self.assertTrue(any("preparation reference identity differs" in x for x in messages))
                self.assertTrue(any(x["severity"] == "incomplete" for x in report["diagnostics"]))
                self.assertFalse(any("internal validation failure" in x for x in messages))

    def test_missing_geometry_cannot_be_hidden_by_removing_unavailable_roles(self):
        def mutate(data):
            self.draft_current_prerequisites(data)
            data["pilot_subset"].pop("geometry_evidence")
            data["demand_baseline"].pop("current_source_assessment")
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("pilot role geometry evidence missing" in x["message"] for x in report["diagnostics"]))

    def test_ready_pilot_requires_geometry_for_every_selected_binding(self):
        def mutate(data):
            data["pilot_subset"]["status"] = "READY"
            data["pilot_subset"]["geometry_evidence"] = []
            data["demand_baseline"]["current_source_assessment"]["unavailable_roles"] = []
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("READY pilot role geometry evidence missing" in x["message"] for x in report["diagnostics"]))

    def test_insufficient_pixels_keep_geometry_incomplete_and_cannot_claim_verification(self):
        def mutate(data):
            specimen = self.pilot_specimen(data)
            ref = data["pilot_subset"]["references"][0]
            row = data["pilot_subset"]["geometry_evidence"][0]
            row.update(status="insufficient_pixels", evidence_id="isolated-demand-shortage-test",
                unclamped_required_full_image=[specimen["encoded_axes"]["width"] + 1, specimen["encoded_axes"]["height"] + 1],
                source_pixels_sufficient=False, reference_id=ref["id"], reference_pixel_sha256=ref["pixel_sha256"],
                reference_geometry={"width":ref["width"],"height":ref["height"]})
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("(insufficient_pixels)" in x["message"] for x in report["diagnostics"]))
        def forged(data):
            mutate(data)
            data["pilot_subset"]["geometry_evidence"][0].update(status="verified", source_pixels_sufficient=True)
        report = self.run_case(forged)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("source adequacy mismatch" in x["message"] for x in report["diagnostics"]))

    def test_rejection_fixtures_remain_separate_with_frozen_categories(self):
        fixtures = self.original["synthetic_rejection_fixtures"]
        self.assertEqual({x["category"] for x in fixtures}, {"truncated", "mime_mismatch", "excessive_bytes", "excessive_pixels"})
        self.assertTrue(all(x["valid_quality_specimen"] is False for x in fixtures))
        report = self.run_case(lambda data: data)
        self.assertFalse(any(x["severity"] == "invalid" for x in report["diagnostics"]))
        report = self.run_case(lambda data: data["synthetic_rejection_fixtures"][0].__setitem__("expected_rejection", "success"))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("expected rejection does not match fixture category" in x["message"] for x in report["diagnostics"]))

    def test_full_source_reference_cannot_claim_unavailable_role_geometry(self):
        def mutate(data):
            ref = data["pilot_subset"]["references"][0]
            data["pilot_subset"]["geometry_evidence"][0].update(status="verified",
                evidence_id="isolated-false-role-proof", unclamped_required_full_image=[1, 1],
                source_pixels_sufficient=True, reference_id=ref["id"], reference_pixel_sha256=ref["pixel_sha256"],
                reference_geometry={"width": ref["width"], "height": ref["height"]})
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("claims verification for unavailable role demand" in x["message"] for x in report["diagnostics"]))

    def test_r3_approval_and_experimental_purpose_are_bound(self):
        mutations = [lambda data: data["pilot_scope"]["source_resolution_approval"].__setitem__("approval_text", "changed"),
                     lambda data: data["pilot_scope"]["source_resolution_approval"].__setitem__("reader_qualification_claimed", True),
                     lambda data: data["pilot_subset"].pop("experimental_purpose"),
                     lambda data: data["pilot_subset"].__setitem__("experimental_purpose", "reader_demand_qualification"),
                     lambda data: data["pilot_subset"].__setitem__("reader_qualification_claimed", True),
                     lambda data: data["trial_plan"]["planned_pairs"][0].__setitem__("experimental_purpose", "reader_demand_qualification")]
        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                self.assertFalse(any("internal validation failure" in item["message"] for item in report["diagnostics"]))

    def test_r3_selection_cannot_omit_or_add_an_approved_binding(self):
        def added(data):
            binding = next(binding for binding in data["bindings"] if binding["id"] not in data["pilot_subset"]["bindings"])
            data["pilot_subset"]["bindings"].append(binding["id"])
        for mutate in (lambda data: data["pilot_subset"]["bindings"].pop(), added):
            report = self.run_case(mutate)
            self.assertEqual(report["status"], "INVALID")
            self.assertTrue(any("exactly the two approved bindings" in item["message"] for item in report["diagnostics"]))

    def test_current_coverage_is_bound_to_offline_purpose_and_r3_revision(self):
        for field, value in (("experimental_purpose", "reader_demand_qualification"),
                             ("freeze_revision_id", "changed-r3")):
            with self.subTest(field=field):
                report = self.run_case(lambda data: data["pilot_subset"]["coverage_matrix"][0].__setitem__(field, value))
                self.assertEqual(report["status"], "INVALID")

    def test_experimental_geometry_is_bound_to_exact_source_reference_and_revision(self):
        fields = [("source_sha256", "0" * 64), ("source_owner_role", "listening_art"),
                  ("cms_binding_source_owner_role", "story_cover"), ("freeze_revision_id", "changed-r3"),
                  ("experimental_purpose", "reader_demand_qualification"),
                  ("source_geometry", {"width": 1, "height": 1}),
                  ("reference_geometry", {"width": 1712, "height": 2777}),
                  ("reference_pixel_sha256", "0" * 64), ("reader_qualification_claimed", True),
                  ("resize", True), ("crop", True), ("upscale", True)]
        for field, value in fields:
            with self.subTest(field=field):
                report = self.run_case(lambda data: data["pilot_subset"]["experimental_geometry_evidence"][0].__setitem__(field, value))
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("experimental" in item["message"] and "geometry" in item["message"]
                                    for item in report["diagnostics"]))

    def test_missing_experimental_geometry_is_an_unfinished_current_prerequisite(self):
        def mutate(data):
            self.draft_current_prerequisites(data)
            data["pilot_subset"]["experimental_geometry_evidence"].pop()
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INCOMPLETE")
        self.assertTrue(any("experimental geometry" in item["message"] and "missing" in item["message"]
                            for item in report["diagnostics"]))

    def test_reader_gate_and_unavailable_fields_cannot_be_removed_or_fabricated(self):
        fields = ["logical_slot", "dpr", "fit", "unclamped_required_full_image",
                  "source_pixels_sufficient", "native_viewing_quality"]
        for location in ("gate", "reader_row"):
            for field in fields:
                def mutate(data):
                    row = (next(gate for gate in data["future_qualification"]["gates"] if gate["id"] == "FQ-READER")
                           if location == "gate" else data["pilot_subset"]["geometry_evidence"][0])
                    row[field] = "fabricated"
                with self.subTest(location=location, field=field):
                    report = self.run_case(mutate)
                    self.assertEqual(report["status"], "INVALID")
        def removed(data):
            data["future_qualification"]["gates"] = [gate for gate in data["future_qualification"]["gates"] if gate["id"] != "FQ-READER"]
        report = self.run_case(removed)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("FQ-READER" in item["message"] for item in report["diagnostics"]))
        for location in ("gate", "reader_row"):
            def stripped(data):
                row = (next(gate for gate in data["future_qualification"]["gates"] if gate["id"] == "FQ-READER")
                       if location == "gate" else data["pilot_subset"]["geometry_evidence"][0])
                row.pop("dpr")
            with self.subTest(location=location, missing="dpr"):
                report = self.run_case(stripped)
                self.assertEqual(report["status"], "INVALID")

    def test_admission_is_limited_to_two_selected_purpose_bound_references(self):
        correctness = self.original["normalization"]["correctness"]
        self.assertEqual(len(correctness["prepared_references"]), 60)
        self.assertEqual(len(correctness["admitted_references"]), 2)
        self.assertEqual(sum(ref["status"] == "admitted" for ref in correctness["prepared_references"]), 2)
        self.assertEqual({ref["id"] for ref in correctness["admitted_references"]},
                         {ref["id"] for ref in self.original["pilot_subset"]["references"]})
        for field, value in (("experimental_purpose", "reader_demand_qualification"),
                             ("freeze_revision_id", "changed-r3"), ("source_owner_role", "listening_art"),
                             ("binding_id", self.original["pilot_subset"]["bindings"][1]),
                             ("pixel_sha256", "0" * 64)):
            with self.subTest(field=field):
                report = self.run_case(lambda data: data["normalization"]["correctness"]["admitted_references"][0].__setitem__(field, value))
                self.assertEqual(report["status"], "INVALID")
        def third(data):
            correctness = data["normalization"]["correctness"]
            admission = copy.deepcopy(correctness["admitted_references"][0])
            ref = next(ref for ref in correctness["prepared_references"] if ref["status"] != "admitted")
            admission.update({key: ref[key] for key in ("id", "source_hash", "pixel_sha256", "width", "height")})
            correctness["admitted_references"].append(admission)
        report = self.run_case(third)
        self.assertEqual(report["status"], "INVALID")

    def test_v7_receipt_recipe_version_and_normalization_policy_remain_frozen(self):
        def relabeled_receipt(data):
            norm = data["normalization"]
            norm["recipe"]["preparation_output_sha256"] = "0" * 64
            norm["correctness"]["preparation_output_sha256"] = "0" * 64
            for ref in norm["correctness"]["admitted_references"]:
                ref["preparation_output_sha256"] = "0" * 64
        mutations = [relabeled_receipt,
                     lambda data: data["normalization"]["decision"].__setitem__("recipe_version", "changed-recipe"),
                     lambda data: data["normalization"]["recipe"].__setitem__("exif_transformations", [1]),
                     lambda data: data["normalization"]["recipe"].__setitem__("tagged_color", "different color intent"),
                     lambda data: data["normalization"]["recipe"].__setitem__("untagged_color", "unknown color assumption"),
                     lambda data: data["normalization"]["recipe"].__setitem__("alpha", "premultiplied alpha"),
                     lambda data: data["normalization"]["recipe"].__setitem__("jpeg_matte", "unlabeled black matte"),
                     lambda data: data["normalization"]["recipe"].__setitem__("canonical_pixel_serialization", "changed serialization")]
        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(mutate, verify_receipt=False)
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("normalization" in item["message"] or "recipe" in item["message"] or "v7" in item["message"]
                                    for item in report["diagnostics"]))

    def test_prepared_reference_membership_cannot_be_omitted_or_duplicated(self):
        def duplicate(data):
            refs = data["normalization"]["correctness"]["prepared_references"]
            refs.append(copy.deepcopy(refs[0]))
        for mutate in (lambda data: data["normalization"]["correctness"]["prepared_references"].pop(), duplicate):
            report = self.run_case(mutate)
            self.assertEqual(report["status"], "INVALID")

    def test_preregistered_ledger_has_exact_seven_candidates_for_each_binding(self):
        pairs = self.original["trial_plan"]["planned_pairs"]
        self.assertEqual(len(pairs), 14)
        expected = {("JPEG", 80, None), ("JPEG", 90, None),
                    ("WEBP", 80, False), ("WEBP", 85, False), ("WEBP", 90, False),
                    ("WEBP", None, True), ("PNG", None, None)}
        for binding_id in self.original["pilot_subset"]["bindings"]:
            selected = [pair for pair in pairs if pair["binding_id"] == binding_id]
            self.assertEqual(len(selected), 7)
            self.assertEqual({(pair["candidate_settings"]["format"], pair["candidate_settings"].get("quality"),
                               pair["candidate_settings"].get("lossless")) for pair in selected}, expected)
        report = self.run_case(lambda data: data["trial_plan"]["planned_pairs"].append(copy.deepcopy(data["trial_plan"]["planned_pairs"][0])))
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("duplicate" in item["message"] for item in report["diagnostics"]))

    def test_ledger_missing_pair_cannot_be_hidden_by_forging_counts(self):
        def mutate(data):
            data["trial_plan"]["planned_pairs"].pop()
            data["trial_plan"]["counts"]["pairs"] = 13
        report = self.run_case(mutate)
        self.assertEqual(report["status"], "INVALID")
        self.assertTrue(any("14" in item["message"] or "candidate" in item["message"] for item in report["diagnostics"]))

    def test_full_codec_settings_and_profile_policy_are_frozen(self):
        cases = [("jpeg-q80", "quality", 81), ("jpeg-q90", "subsampling", 2),
                 ("webp-lossy-q80", "method", 5), ("webp-lossy-q85", "lossless", True),
                 ("webp-lossy-q90", "mode", "RGBA"), ("webp-lossless", "exact", False),
                 ("png-opaque", "compress_level", 9)]
        for suffix, field, value in cases:
            def mutate(data):
                pair = next(pair for pair in data["trial_plan"]["planned_pairs"] if pair["id"].endswith(f"-{suffix}-r3"))
                pair["candidate_settings"][field] = value
            with self.subTest(suffix=suffix):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("settings" in item["message"] for item in report["diagnostics"]))
        for arm, field in (("control_settings", "progressive"), ("candidate_settings", "profile_handling")):
            with self.subTest(arm=arm, missing=field):
                report = self.run_case(lambda data: data["trial_plan"]["planned_pairs"][0][arm].pop(field))
                self.assertEqual(report["status"], "INVALID")

    def test_runtime_discovery_cannot_grant_provider_or_execution_qualification(self):
        mutations = [lambda data: data["trial_plan"]["provider_discovery"]["runtime"].__setitem__("pillow", "changed"),
                     lambda data: data["trial_plan"]["provider_discovery"]["binaries"][0].__setitem__("sha256", "0" * 64),
                     lambda data: data["trial_plan"]["provider_discovery"].__setitem__("encoding_performed", True),
                     lambda data: data["trial_plan"]["provider_qualification"].__setitem__("status", "qualified"),
                     lambda data: data["trial_plan"]["provider_qualification"].__setitem__("comparison_execution_allowed", True),
                     lambda data: data["trial_plan"]["planned_pairs"][0].__setitem__("provider_discovery_sha256", "0" * 64)]
        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("provider" in item["message"] for item in report["diagnostics"]))

    def test_pair_outputs_and_quality_savings_remain_unavailable_until_execution(self):
        mutations = [lambda pair: pair.__setitem__("state", "eligible"),
                     lambda pair: pair["outputs"]["candidate"].__setitem__("bytes", 0),
                     lambda pair: pair["outputs"]["control"].__setitem__("verified", True),
                     lambda pair: pair["visual_assessment"].__setitem__("verdict", "pass"),
                     lambda pair: pair.__setitem__("reader_verdict", "pass"),
                     lambda pair: pair.__setitem__("native_verdict", "pass"),
                     lambda pair: pair.__setitem__("raw_byte_delta", 0),
                     lambda pair: pair.__setitem__("quality_qualified_savings", 0),
                     lambda pair: pair.__setitem__("exclusion_reasons", [])]
        for index, mutation in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(lambda data: mutation(data["trial_plan"]["planned_pairs"][0]))
                self.assertEqual(report["status"], "INVALID")

    def test_original_byte_denominator_cannot_replace_q85_or_be_forged(self):
        mutations = [lambda pair: pair.__setitem__("baseline_denominator", "original_source_bytes"),
                     lambda pair: pair["original_byte_denominator"].__setitem__("bytes", 0),
                     lambda pair: pair["original_byte_denominator"].__setitem__("source_sha256", "0" * 64),
                     lambda pair: pair["original_byte_denominator"].__setitem__("verified", False)]
        for index, mutation in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(lambda data: mutation(data["trial_plan"]["planned_pairs"][0]))
                self.assertEqual(report["status"], "INVALID")
                self.assertTrue(any("denominator" in item["message"] for item in report["diagnostics"]))

    def test_offline_order_hashes_adjacency_parity_and_tie_break_are_bound(self):
        mutations = [lambda plan: plan["planned_pairs"][0].__setitem__("pair_key_sha256", "0" * 64),
                     lambda plan: plan["offline_order"][0].__setitem__("context_sha256", "0" * 64),
                     lambda plan: plan["offline_order"][0]["arms"].reverse(),
                     lambda plan: plan["offline_order"].reverse(),
                     lambda plan: plan["order_rules"].__setitem__("sort", "pair_id_only"),
                     lambda plan: plan["offline_order_context"].__setitem__("repeat", 6)]
        for index, mutation in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(lambda data: mutation(data["trial_plan"]))
                self.assertEqual(report["status"], "INVALID")

    def test_attempt_ledgers_and_offline_native_counts_are_truthful(self):
        plan = self.original["trial_plan"]
        self.assertEqual(plan["counts"], {"pairs": 14, "environments": 0, "base_native_observations": 0, "maximum_native_attempts": 0})
        self.assertEqual(plan["planned_counts"]["scheduled_offline_arm_observations"], 28)
        self.assertEqual(plan["planned_counts"]["maximum_offline_attempts"], 56)
        self.assertEqual(plan["planned_counts"]["actual_attempts"], 0)
        mutations = [lambda plan: plan["native_environments"].append({"id": "invented-native-environment"}),
                     lambda plan: plan.__setitem__("native_status", "passed"),
                     lambda plan: plan.__setitem__("worker_status", "passed"),
                     lambda plan: plan["planned_counts"].__setitem__("native_qualified", 1),
                     lambda plan: plan["planned_pairs"][0]["attempt_limits"].__setitem__("retry_limit", 2),
                     lambda plan: plan["planned_pairs"][0]["attempts"].append({"id": "invented-attempt", "result": "pass"}),
                     lambda plan: plan["planned_pairs"][0]["metric_counts"].__setitem__("metric_valid", 1),
                     lambda plan: plan["planned_pairs"][0]["planned_attempt_ids"].__setitem__("candidate", plan["planned_pairs"][0]["planned_attempt_ids"]["control"])]
        for index, mutation in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(lambda data: mutation(data["trial_plan"]))
                self.assertEqual(report["status"], "INVALID")

    def test_frozen_numeric_types_and_nonfinite_values_cannot_be_coerced(self):
        def prepared_width(data):
            ref = data["normalization"]["correctness"]["prepared_references"][0]
            ref["width"] = float(ref["width"])
        mutations = [prepared_width,
                     lambda data: data["trial_plan"]["counts"].__setitem__("pairs", 14.0),
                     lambda data: data["trial_plan"]["counts"].__setitem__("base_native_observations", False),
                     lambda data: data["pilot_subset"]["experimental_geometry_evidence"][0]["source_geometry"].__setitem__("width", 1712.0),
                     lambda data: data["trial_plan"]["planned_pairs"][0]["candidate_settings"].__setitem__("quality", float("nan"))]
        for index, mutate in enumerate(mutations):
            with self.subTest(index=index):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                self.assertFalse(any("internal validation failure" in item["message"] for item in report["diagnostics"]))

    def test_malformed_r3_fields_retain_integrity_and_incomplete_diagnostics(self):
        mutations = [lambda data: data["pilot_subset"]["experimental_geometry_evidence"][0].__setitem__("binding_id", []),
                     lambda data: data["normalization"]["correctness"]["admitted_references"][0].__setitem__("id", []),
                     lambda data: data["trial_plan"]["planned_pairs"][0].__setitem__("candidate_settings", []),
                     lambda data: data["trial_plan"]["provider_discovery"].__setitem__("binaries", [{}]),
                     lambda data: data["trial_plan"]["offline_order"][0].__setitem__("pair_id", []),
                     lambda data: data["trial_plan"].__setitem__("planned_pairs", None)]
        for index, mutation in enumerate(mutations):
            def mutate(data):
                mutation(data)
                data["normalization"]["correctness"]["prepared_references"][0]["pixel_sha256"] = "0" * 64
            with self.subTest(index=index):
                report = self.run_case(mutate)
                self.assertEqual(report["status"], "INVALID")
                messages = [item["message"] for item in report["diagnostics"]]
                self.assertTrue(any("preparation reference identity differs" in message for message in messages))
                self.assertTrue(any(item["severity"] == "incomplete" for item in report["diagnostics"]))
                self.assertFalse(any("internal validation failure" in message for message in messages))

    def test_malformed_containers_and_ids_retain_later_source_and_privacy_diagnostics(self):
        mutations = [lambda data: data.__setitem__("normalization", []),
                     lambda data: data.__setitem__("normalization", {}),
                     lambda data: data["normalization"]["correctness"]["prepared_references"][0].__setitem__("id", []),
                     lambda data: data["normalization"]["correctness"]["admitted_references"][0].__setitem__("id", []),
                     lambda data: data["normalization"]["correctness"]["admitted_references"][0].__setitem__("check_id", []),
                     lambda data: data["trial_plan"].__setitem__("planned_pairs", None)]
        with self.privacy_probe() as target:
            for index, mutation in enumerate(mutations):
                def mutate(data):
                    mutation(data)
                    data["specimens"][0]["integrity"]["bytes"] += 1
                with self.subTest(index=index):
                    report = self.run_case(mutate)
                    self.assertEqual(report["status"], "INVALID")
                    messages = [item["message"] for item in report["diagnostics"]]
                    self.assertTrue(any("recorded byte count differs" in message for message in messages))
                    self.assertTrue(any(f"private access material in {target.name}" in message for message in messages))
                    self.assertTrue(any(item["severity"] == "incomplete" for item in report["diagnostics"]))
                    self.assertFalse(any("internal validation failure" in message for message in messages))
        self.assertFalse(target.exists())

    def test_malformed_retained_record_shapes_keep_independent_diagnostics(self):
        mutations = [("observation ID", lambda data: data["byte_observations"][0].__setitem__("id", [])),
                     ("observation hash", lambda data: data["byte_observations"][0].__setitem__("sha256", [])),
                     ("observation EXIF", lambda data: data["byte_observations"][0].__setitem__("exif", [])),
                     ("observation array", lambda data: data.__setitem__("byte_observations", {})),
                     ("source hash", lambda data: data["specimens"][0]["integrity"].__setitem__("sha256", [])),
                     ("integrity object", lambda data: data["specimens"][0].__setitem__("integrity", [1])),
                     ("source content ID", lambda data: data["specimens"][0].__setitem__("content_id", [])),
                     ("binding content ID", lambda data: data["bindings"][0].__setitem__("content_id", {})),
                     ("source roles", lambda data: data["specimens"][0].__setitem__("roles", None)),
                     ("source backlinks", lambda data: data["specimens"][0].__setitem__("bindings", None)),
                     ("coverage status", lambda data: data["coverage_matrix"][0].__setitem__("status", [])),
                     ("coverage class", lambda data: next(row for row in data["coverage_matrix"] if row["id"] == "static-listening-alpha").__setitem__("class", [])),
                     ("reference status", lambda data: data["normalization"]["correctness"]["prepared_references"][0].__setitem__("status", [])),
                     ("fixture category", lambda data: data["synthetic_rejection_fixtures"][0].__setitem__("category", []))]
        for label, mutate in mutations:
            with self.subTest(field=label):
                self.assert_mixed_invalid(self.run_validation_case(mutate))

    def test_each_excluded_future_coverage_row_remains_visible(self):
        classes = {"real_alpha", "exif_rotation", "exif_mirroring"}
        for location in ("global", "pilot"):
            for removed in ({cls} for cls in sorted(classes)):
                def mutate(data):
                    owner = data if location == "global" else data["pilot_subset"]
                    owner["coverage_matrix"] = [row for row in owner["coverage_matrix"] if row["class"] not in removed]
                with self.subTest(location=location, removed=sorted(removed)):
                    expected = "future qualification coverage cell missing" if location == "global" else "pilot future qualification coverage missing"
                    self.assert_mixed_invalid(self.run_validation_case(mutate), expected)
            def remove_all(data):
                owner = data if location == "global" else data["pilot_subset"]
                owner["coverage_matrix"] = [row for row in owner["coverage_matrix"] if row["class"] not in classes]
            with self.subTest(location=location, removed="all three"):
                report = self.run_validation_case(remove_all)
                self.assert_mixed_invalid(report)
                messages = [item["message"] for item in report["diagnostics"]]
                expected = "future qualification coverage cell missing" if location == "global" else "pilot future qualification coverage missing"
                self.assertEqual(sum(expected in message for message in messages), 3, messages)

    def test_unreadable_historical_and_reference_files_keep_later_diagnostics(self):
        historical = self.original["historical_controls"][0]
        cases = [("historical source", self.source_root / f"{historical['sha256']}.source", "historical source read failed")]
        if self.reference_root:
            reference = self.original["normalization"]["correctness"]["prepared_references"][0]
            cases.append(("prepared reference", self.reference_root / f"{reference['id']}.pixels", "prepared reference read/stat failed"))
        for label, path, expected in cases:
            with self.subTest(file=label):
                self.assertTrue(path.is_file(), path.name)
                self.assert_mixed_invalid(self.run_validation_case(lambda data: data, unreadable_path=path), expected)


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--source-root", type=Path, required=True); parser.add_argument("--reference-root", type=Path); parser.add_argument("--preparation-receipt", type=Path); args, rest = parser.parse_known_args()
    CorpusVerifierTests.source_root = args.source_root.resolve()
    CorpusVerifierTests.reference_root = args.reference_root.resolve() if args.reference_root else None
    CorpusVerifierTests.preparation_receipt = args.preparation_receipt.resolve() if args.preparation_receipt else None
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(CorpusVerifierTests)
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__": raise SystemExit(main())
