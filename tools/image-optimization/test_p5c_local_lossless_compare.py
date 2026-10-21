"""P5c matrisini yalnız sentetik raster ve özel geçici dosyalarla sınar."""
from contextlib import ExitStack
from io import BytesIO
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zlib

from PIL import Image

import p5c_local_lossless_compare as compare
import p5c_lossless_codec as codec
from p5b_inventory import validate_inventory
from p5b_source_io import MAX_FILE_BYTES, PreflightError, ROOT, read_bounded
from p5c_lossless_output import create_private_directory, exclusive_bytes, gallery, totals
import test_p5b_local_source_preflight as p5b_fixtures

SIZE = (8, 6)
ICC = b"synthetic-icc" + b"\0" * (536 - len(b"synthetic-icc"))
EMPTY_META = {"icc": None, "exif": None, "xmp": None}


def raster(mode="RGB", format_name="PNG", **options):
    with Image.new(mode, SIZE, (16, 48, 96, 255) if mode == "RGBA" else (16, 48, 96)) as image:
        output = BytesIO()
        image.save(output, format=format_name, **options)
        return output.getvalue()


class CodecTest(unittest.TestCase):
    def decode(self, data, **options):
        return codec.decode_source(data, dimensions=SIZE, **options)

    def test_rgb_and_icc_equal_for_png_and_vp8x_webp_with_metadata(self):
        exif = Image.Exif()
        exif[282] = 132
        data = raster("RGBA", icc_profile=ICC, exif=exif)
        pixels, values, source = self.decode(data)
        values["xmp"] = b"<synthetic>sentetik</synthetic>"
        self.assertTrue(source["alphaRemoved"])
        self.assertEqual("ICC_PRESERVED", source["colorInterpretation"])
        for format_name in ("PNG", "WEBP"):
            with self.subTest(format=format_name):
                payload, elapsed = codec.encode_candidate(pixels, values, format_name, 100_000, SIZE)
                result = codec.validate_candidate(payload, format_name, pixels, values, SIZE)
                self.assertTrue(result["allRgbBytesEqual"] and result["allIccBytesEqual"])
                self.assertTrue(result["metadataEqual"]["xmp"])
                self.assertGreater(elapsed, 0)
                if format_name == "WEBP":
                    self.assertEqual(["VP8X", "ICCP", "VP8L", "EXIF", "XMP "], result["webpChunks"])

    def test_wrong_icc_fails_separately_even_when_every_rgb_byte_matches(self):
        pixels, values, _ = self.decode(raster(icc_profile=ICC))
        for format_name in ("PNG", "WEBP"):
            with self.subTest(format=format_name):
                payload, _ = codec.encode_candidate(pixels, {**values, "icc": b"wrong-profile"}, format_name, 100_000, SIZE)
                with self.assertRaisesRegex(PreflightError, "CANDIDATE_ICC_MISMATCH"):
                    codec.validate_candidate(payload, format_name, pixels, values, SIZE)

    def test_one_pixel_difference_fails_separately(self):
        pixels, values, _ = self.decode(raster(icc_profile=ICC))
        changed = bytes([pixels[0] ^ 1]) + pixels[1:]
        for format_name in ("PNG", "WEBP"):
            with self.subTest(format=format_name):
                payload, _ = codec.encode_candidate(changed, values, format_name, 100_000, SIZE)
                with self.assertRaisesRegex(PreflightError, "CANDIDATE_RGB_MISMATCH"):
                    codec.validate_candidate(payload, format_name, pixels, values, SIZE)

    def test_unprofiled_jpeg_stays_unspecified(self):
        pixels, values, source = self.decode(raster(format_name="JPEG"), pilot=18)
        self.assertIsNone(values["icc"])
        self.assertEqual("UNSPECIFIED", source["colorInterpretation"])
        payload, _ = codec.encode_candidate(pixels, values, "WEBP", 100_000, SIZE)
        self.assertEqual(["VP8L"], codec.webp_chunks(payload, SIZE))

    def test_profile_changes_in_either_pilot_are_blocked(self):
        for data, pilot in ((raster(format_name="JPEG", icc_profile=ICC), 18),
                            (raster("RGBA", icc_profile=ICC), 62), (raster("RGBA"), 62)):
            with self.subTest(pilot=pilot, bytes=len(data)):
                with self.assertRaisesRegex(PreflightError, "SOURCE_ICC_MISMATCH"):
                    self.decode(data, pilot=pilot)

    def test_orientation_is_blocked_including_identity_tag(self):
        for orientation in (1, 6):
            exif = Image.Exif()
            exif[274] = orientation
            with self.subTest(orientation=orientation):
                with self.assertRaisesRegex(PreflightError, "SOURCE_ORIENTATION_PRESENT"):
                    self.decode(raster(exif=exif))

    def test_transparent_alpha_and_transparency_metadata_are_blocked(self):
        image = Image.new("RGBA", SIZE, (1, 2, 3, 255))
        image.putpixel((0, 0), (1, 2, 3, 254))
        output = BytesIO()
        image.save(output, format="PNG")
        for data in (output.getvalue(), raster(transparency=(16, 48, 96))):
            with self.assertRaisesRegex(PreflightError, "SOURCE_TRANSPARENT_PIXELS"):
                self.decode(data)

    def test_source_geometry_and_large_raster_are_blocked_before_decode(self):
        with self.assertRaisesRegex(PreflightError, "SOURCE_DIMENSIONS_MISMATCH"):
            codec.decode_source(raster())
        payload = bytearray(raster())
        payload[16:24] = (8001).to_bytes(4, "big") + (5000).to_bytes(4, "big")
        payload[29:33] = (zlib.crc32(payload[12:29]) & 0xffffffff).to_bytes(4, "big")
        with self.assertRaisesRegex(PreflightError, "PIXEL_LIMIT"):
            self.decode(bytes(payload))

    def test_multiframe_and_unsupported_mode_are_blocked(self):
        output = BytesIO()
        with Image.new("RGB", SIZE, "red") as image, Image.new("RGB", SIZE, "blue") as second:
            image.save(output, format="PNG", save_all=True, append_images=[second])
        with self.assertRaisesRegex(PreflightError, "SOURCE_MULTIFRAME"):
            self.decode(output.getvalue())
        output = BytesIO()
        Image.new("L", SIZE).save(output, format="PNG")
        with self.assertRaisesRegex(PreflightError, "SOURCE_MODE_INVALID"):
            self.decode(output.getvalue())

    def test_lossy_and_corrupt_candidates_never_pass(self):
        pixels, values, _ = self.decode(raster())
        lossy = BytesIO()
        Image.frombytes("RGB", SIZE, pixels).save(lossy, format="WEBP", lossless=False)
        with self.assertRaisesRegex(PreflightError, "WEBP_VP8L_REQUIRED"):
            codec.validate_candidate(lossy.getvalue(), "WEBP", pixels, values, SIZE)
        payload, _ = codec.encode_candidate(pixels, values, "WEBP", 100_000, SIZE)
        with self.assertRaisesRegex(PreflightError, "WEBP_RIFF_INVALID"):
            codec.validate_candidate(payload[:-1], "WEBP", pixels, values, SIZE)
        with self.assertRaises(Exception):
            codec.validate_candidate(b"not-a-png", "PNG", pixels, values, SIZE)

    def test_encoder_stops_at_output_budget(self):
        pixels, values, _ = self.decode(raster())
        for format_name in ("PNG", "WEBP"):
            with self.subTest(format=format_name):
                with self.assertRaisesRegex(PreflightError, "OUTPUT_BUDGET_EXCEEDED"):
                    codec.encode_candidate(pixels, values, format_name, 8, SIZE)


class LocalRunTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="p5c-synthetic-", dir="/private/tmp")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def test_existing_output_and_symlinks_never_overwrite(self):
        marker = self.root / "marker"
        marker.write_bytes(b"unchanged")
        with self.assertRaises(FileExistsError):
            create_private_directory(self.root)
        with self.assertRaises(PreflightError):
            exclusive_bytes(marker, b"changed", 100)
        link = self.root / "link"
        link.symlink_to(self.root, target_is_directory=True)
        with self.assertRaises(OSError):
            create_private_directory(link / "run")
        self.assertEqual(b"unchanged", marker.read_bytes())
        with self.assertRaisesRegex(PreflightError, "PRIVATE_PATH_REQUIRED"):
            create_private_directory(ROOT / "unsafe-p5c-output")

    def test_source_byte_limit_and_symlink_are_blocked(self):
        source = self.root / "large"
        with source.open("wb") as output:
            output.truncate(MAX_FILE_BYTES + 1)
        with self.assertRaisesRegex(PreflightError, "FILE_BYTE_LIMIT"):
            read_bounded(source)
        link = self.root / "link"
        link.symlink_to(source)
        with self.assertRaisesRegex(PreflightError, "FILE_UNAVAILABLE_OR_SYMLINK"):
            read_bounded(link)

    def test_source_byte_hash_and_role_changes_fail_before_encoding(self):
        data = raster()
        for expected, code in (({"byte_size": 1}, "SOURCE_BYTES_MISMATCH"),
                               ({"byte_size": len(data), "checksum_sha256": "0" * 64}, "SOURCE_SHA256_MISMATCH")):
            with self.subTest(code=code):
                with self.assertRaisesRegex(PreflightError, code), patch.object(compare, "encode_candidate") as encode:
                    compare.check_record(data, expected)
                encode.assert_not_called()
        fixture = p5b_fixtures.PilotPreflightTest()
        fixture.setUp()
        self.addCleanup(fixture.doCleanups)
        fixture.rows.pop(0)
        inventory = validate_inventory(fixture.rows)
        self.assertIn("MISSING_OR_DUPLICATE_ROLE", {error["code"] for error in inventory["errors"]})

    def test_csv_pin_and_invalid_inputs_produce_only_failure_evidence(self):
        csv = self.root / "pilot.csv"
        csv.write_bytes(b"changed csv")
        mapping = self.root / "mapping.json"
        mapping.write_bytes(b"{}")
        output = self.root / "run"
        with patch.object(compare, "protected_snapshot", return_value={}), patch.object(compare, "run_child") as child:
            report = compare.run_compare(csv, mapping, output)
        self.assertEqual("BLOCKED", report["status"])
        self.assertIn("CSV_BASELINE_SHA256_MISMATCH", report["errors"])
        child.assert_not_called()
        self.assertEqual(["report.json"], [path.name for path in output.iterdir()])

    def test_worker_timeout_is_a_blocking_error(self):
        with patch.object(compare.subprocess, "run", side_effect=subprocess.TimeoutExpired("synthetic", 0.01)):
            with self.assertRaisesRegex(PreflightError, "SOURCE_TIMEOUT"):
                compare.run_child({"mode": "check"}, timeout=0.01)

    def test_preflight_failure_prevents_all_encode_and_preserves_inputs(self):
        source, csv, mapping = (self.root / name for name in ("source", "pilot.csv", "mapping.json"))
        for path in (source, csv, mapping):
            path.write_bytes(b"synthetic")
        expected = {"source_asset_id": 1800, "checksum_sha256": codec.digest(source.read_bytes())}
        request = {"source": str(source), "expected": expected, "mode": "check"}
        hashes = {"csv": codec.digest(csv.read_bytes()), "mapping": codec.digest(mapping.read_bytes())}
        with patch.object(compare, "load_inputs", return_value=([request, request], hashes)), \
                patch.object(compare, "protected_snapshot", return_value={}), \
                patch.object(compare, "run_child", side_effect=[{"wallProcessMillis": 1},
                            PreflightError("SOURCE_TRANSPARENT_PIXELS")]) as child:
            report = compare.run_compare(csv, mapping, self.root / "run")
        self.assertEqual("BLOCKED", report["status"])
        self.assertEqual(0, report["candidateCount"])
        self.assertTrue(all(call.args[0]["mode"] == "check" for call in child.call_args_list))
        self.assertTrue(report["preservation"]["allSourcesUnchanged"])

    def test_failed_candidate_file_remains_as_failure_evidence(self):
        source, csv, mapping = (self.root / name for name in ("source", "pilot.csv", "mapping.json"))
        for path in (source, csv, mapping):
            path.write_bytes(b"synthetic")
        expected = {"source_asset_id": 1800, "checksum_sha256": codec.digest(source.read_bytes())}
        request = {"source": str(source), "expected": expected, "mode": "check"}
        hashes = {"csv": codec.digest(csv.read_bytes()), "mapping": codec.digest(mapping.read_bytes())}
        output = self.root / "run"

        def child(request, **options):
            if request["mode"] == "check":
                return {"wallProcessMillis": 1}
            exclusive_bytes(output / "1800.webp", b"failed-candidate", 100)
            raise PreflightError("CANDIDATE_RGB_MISMATCH")

        with patch.object(compare, "load_inputs", return_value=([request], hashes)), \
                patch.object(compare, "protected_snapshot", return_value={}), \
                patch.object(compare, "run_child", side_effect=child):
            report = compare.run_compare(csv, mapping, output)
        self.assertEqual("BLOCKED", report["status"])
        self.assertEqual(["CANDIDATE_RGB_MISMATCH"], report["errors"])
        self.assertEqual(1800, report["activeAssetId"])
        self.assertEqual("ENCODE", report["stage"])
        self.assertEqual(b"failed-candidate", (output / "1800.webp").read_bytes())
        self.assertEqual(0, report["candidateCount"])

    def assert_post_encode_change_is_blocked(self, changed_input):
        source, csv, mapping = (self.root / name for name in ("source.jpg", "pilot.csv", "mapping.json"))
        source.write_bytes(raster(format_name="JPEG"))
        csv.write_bytes(b"synthetic csv")
        mapping.write_bytes(b"synthetic mapping")
        protected_root = self.root / "protected-root"
        evidence = protected_root / "be/docs/evidence/earlier.json"
        evidence.parent.mkdir(parents=True)
        evidence.write_bytes(b'{"synthetic":"before"}')
        paths = {"source": source, "csv": csv, "mapping": mapping, "evidence": evidence}
        before = {key: codec.digest(path.read_bytes()) for key, path in paths.items()}
        expected = {"source_asset_id": 1800, "pilot_content_id": 18,
                    "byte_size": source.stat().st_size, "checksum_sha256": before["source"],
                    "mime_type": "image/jpeg"}
        roles = [{"role": "READING_COVER", "pageNumber": None},
                 {"role": "READING_DETAIL", "pageNumber": None}]
        request = {"source": str(source), "expected": expected, "roles": roles, "mode": "check"}
        input_hashes = {"csv": before["csv"], "mapping": before["mapping"]}

        def child(request, **options):
            record = {**compare.worker(request), "wallProcessMillis": 1}
            if request["mode"] == "encode":
                self.assertEqual({"PNG", "WEBP"}, set(record["candidates"]))
                self.assertTrue(all(candidate["allRgbBytesEqual"] and candidate["allIccBytesEqual"]
                                    for candidate in record["candidates"].values()))
                # Değişiklik ancak iki aday da başarıyla yazılıp doğrulandıktan sonra oluşur.
                target = paths[changed_input]
                target.write_bytes(target.read_bytes() + b"\nsynthetic change")
            return record

        with ExitStack() as stack:
            stack.enter_context(patch.object(compare, "ROOT", protected_root))
            stack.enter_context(patch.object(compare, "load_inputs", return_value=([request], input_hashes)))
            stack.enter_context(patch.object(compare, "decode_source", side_effect=lambda data, pilot:
                                            codec.decode_source(data, pilot, SIZE)))
            stack.enter_context(patch.object(compare, "encode_candidate", side_effect=lambda pixels, values, fmt, budget:
                                            codec.encode_candidate(pixels, values, fmt, budget, SIZE)))
            stack.enter_context(patch.object(compare, "validate_candidate", side_effect=lambda data, fmt, pixels, values:
                                            codec.validate_candidate(data, fmt, pixels, values, SIZE)))
            child_mock = stack.enter_context(patch.object(compare, "run_child", side_effect=child))
            stack.enter_context(patch("sys.stderr"))
            report = compare.run_compare(csv, mapping, self.root / "run")
        self.assertEqual(2, child_mock.call_count)
        disk_report = json.loads((self.root / "run/report.json").read_bytes())
        self.assertEqual(report, disk_report)
        for observed in (report, disk_report):
            self.assertEqual("COMPLETE", observed["stage"])
            self.assertEqual(2, observed["candidateCount"])
            self.assertEqual("BLOCKED", observed["status"])
            self.assertEqual(["PROTECTED_INPUT_CHANGED"], observed["errors"])
            protection = observed["preservation"]
            self.assertEqual(changed_input != "source", protection["allSourcesUnchanged"])
            self.assertEqual({key: changed_input != key for key in ("csv", "mapping")},
                             protection["inputHashesUnchanged"])
            self.assertEqual(changed_input != "evidence", protection["previousEvidenceUnchanged"])
            self.assertEqual(1, protection["previousEvidenceFiles"])
            key = "be/docs/evidence/earlier.json"
            self.assertEqual({key: before["evidence"]}, protection["previousEvidenceBefore"])
            self.assertEqual({key: codec.digest(evidence.read_bytes())}, protection["previousEvidenceAfter"])

    def test_source_change_after_successful_encode_blocks_returned_and_disk_report(self):
        self.assert_post_encode_change_is_blocked("source")

    def test_csv_change_after_successful_encode_blocks_returned_and_disk_report(self):
        self.assert_post_encode_change_is_blocked("csv")

    def test_mapping_change_after_successful_encode_blocks_returned_and_disk_report(self):
        self.assert_post_encode_change_is_blocked("mapping")

    def test_previous_evidence_change_after_successful_encode_blocks_returned_and_disk_report(self):
        self.assert_post_encode_change_is_blocked("evidence")

    def test_totals_match_explicit_growth_savings_bytes_percent_and_role_counts(self):
        cover_roles = [{"role": "READING_COVER", "pageNumber": None},
                       {"role": "READING_DETAIL", "pageNumber": None}]
        page_roles = [{"role": "READING_PAGE", "pageNumber": 1}]
        records = [
            {"assetId": 1800, "pilotContentId": 18, "sourceBytes": 100, "roles": cover_roles,
             "candidates": {"PNG": {"bytes": 200}, "WEBP": {"bytes": 50}}},
            {"assetId": 6200, "pilotContentId": 62, "sourceBytes": 200, "roles": cover_roles,
             "candidates": {"PNG": {"bytes": 100}, "WEBP": {"bytes": 250}}},
            {"assetId": 1801, "pilotContentId": 18, "sourceBytes": 300, "roles": page_roles,
             "candidates": {"PNG": {"bytes": 400}, "WEBP": {"bytes": 250}}},
            {"assetId": 6201, "pilotContentId": 62, "sourceBytes": 300, "roles": page_roles,
             "candidates": {"PNG": {"bytes": 200}, "WEBP": {"bytes": 150}}},
        ]
        expected = {
            18: {"sourceBytes": 400, "PNG": (600, -200, -50.0, [1800, 1801]),
                 "WEBP": (300, 100, 25.0, [])},
            62: {"sourceBytes": 500, "PNG": (300, 200, 40.0, []),
                 "WEBP": (400, 100, 20.0, [6200])},
        }
        observed = totals(records)
        self.assertEqual([18, 62], [pilot["pilotContentId"] for pilot in observed])
        for pilot in observed:
            expected_pilot = expected[pilot["pilotContentId"]]
            self.assertEqual("tr", pilot["languageCode"])
            self.assertEqual(2, pilot["uniqueSources"])
            self.assertEqual(3, pilot["roleBindings"])
            self.assertEqual(expected_pilot["sourceBytes"], pilot["sourceBytes"])
            self.assertEqual({"PNG", "WEBP"}, set(pilot["formats"]))
            for fmt in ("PNG", "WEBP"):
                with self.subTest(pilot=pilot["pilotContentId"], format=fmt):
                    size, saved, percent, growing = expected_pilot[fmt]
                    candidate = pilot["formats"][fmt]
                    self.assertEqual(2, candidate["files"])
                    self.assertEqual(size, candidate["bytes"])
                    self.assertEqual(saved, candidate["savedBytes"])
                    self.assertAlmostEqual(percent, candidate["savedPercent"])
                    self.assertEqual(growing, candidate["growingFiles"])

    def test_real_child_rejects_wrong_geometry(self):
        source = self.root / "source.png"
        data = raster()
        source.write_bytes(data)
        request = {"source": str(source), "expected": {"byte_size": len(data),
                   "checksum_sha256": codec.digest(data), "pilot_content_id": 18}, "mode": "check"}
        with self.assertRaisesRegex(PreflightError, "SOURCE_DIMENSIONS_MISMATCH"):
            compare.run_child(request)

    def test_41_sources_create_82_candidates_43_roles_and_no_external_gallery(self):
        requests, hashes = [], {}
        for pilot, count in ((18, 17), (62, 24)):
            for slot in range(count):
                asset = pilot * 100 + slot
                data = raster(format_name="JPEG") if pilot == 18 else raster("RGBA", icc_profile=ICC)
                source = self.root / f"{asset}.source"
                source.write_bytes(data)
                roles = [{"role": "READING_PAGE", "pageNumber": slot}] if slot else [
                    {"role": "READING_COVER", "pageNumber": None}, {"role": "READING_DETAIL", "pageNumber": None}]
                expected = {"source_asset_id": asset, "pilot_content_id": pilot, "byte_size": len(data),
                            "checksum_sha256": codec.digest(data), "mime_type": "image/jpeg" if pilot == 18 else "image/png"}
                requests.append({"source": str(source), "expected": expected, "roles": roles, "mode": "check"})
                hashes[source] = codec.digest(data)
        output = self.root / "run"
        csv, mapping = self.root / "pilot.csv", self.root / "mapping.json"
        csv.write_bytes(b"synthetic csv")
        mapping.write_bytes(b"synthetic mapping")
        input_hashes = {"csv": codec.digest(csv.read_bytes()), "mapping": codec.digest(mapping.read_bytes())}
        with ExitStack() as stack:
            stack.enter_context(patch.object(codec, "ICC_SHA256", codec.digest(ICC)))
            stack.enter_context(patch.object(compare, "decode_source", side_effect=lambda data, pilot: codec.decode_source(data, pilot, SIZE)))
            stack.enter_context(patch.object(compare, "encode_candidate", side_effect=lambda pixels, values, fmt, budget: codec.encode_candidate(pixels, values, fmt, budget, SIZE)))
            stack.enter_context(patch.object(compare, "validate_candidate", side_effect=lambda data, fmt, pixels, values: codec.validate_candidate(data, fmt, pixels, values, SIZE)))
            stack.enter_context(patch.object(compare, "load_inputs", return_value=(requests, input_hashes)))
            stack.enter_context(patch.object(compare, "run_child", side_effect=lambda request, **kwargs: {
                **compare.worker(request), "wallProcessMillis": 1}))
            stack.enter_context(patch.object(compare, "protected_snapshot", return_value={}))
            with patch("sys.stderr"):
                report = compare.run_compare(csv, mapping, output)
        records = report["sources"]
        self.assertEqual("LOCAL_LOSSLESS_COMPLETE", report["status"])
        self.assertEqual(82, report["candidateCount"])
        self.assertTrue(report["preservation"]["allSourcesUnchanged"])
        self.assertEqual({"csv": True, "mapping": True}, report["preservation"]["inputHashesUnchanged"])
        self.assertEqual(41, len(records))
        self.assertEqual(82, sum(len(record["candidates"]) for record in records))
        summaries = totals(records)
        self.assertEqual(43, sum(pilot["roleBindings"] for pilot in summaries))
        self.assertEqual(sum(record["sourceBytes"] for record in records), sum(pilot["sourceBytes"] for pilot in summaries))
        self.assertEqual(hashes, {source: codec.digest(source.read_bytes()) for source in hashes})
        page = gallery(records).decode()
        self.assertEqual(123, page.count("<img "))
        self.assertNotIn("https:", page)
        self.assertNotIn(str(self.root), page)
        self.assertTrue(all(record["source"]["dimensions"] == list(SIZE) for record in records))
        self.assertTrue(all(path.stat().st_mode & 0o777 == 0o600 for path in output.iterdir()))


if __name__ == "__main__":
    unittest.main()
