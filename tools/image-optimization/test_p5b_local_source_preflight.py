"""Ağ ve gerçek kaynak kullanmadan P5b girdi/okuma sınırlarını sınar."""
import copy
import csv
from hashlib import sha256
from io import BytesIO
import json
import os
from pathlib import Path
import tempfile
import unittest

from PIL import Image

import p5b_local_source_preflight as preflight
from p5b_image_probe import inspect_image
from p5b_inventory import FIELDS, PILOTS
from p5b_source_io import MAX_FILE_BYTES, PreflightError, read_bounded


def image_bytes(fmt="PNG", mode="RGB", color=None, **options):
    output = BytesIO()
    with Image.new(mode, (8, 6), color or (16, 48, 96)) as image:
        image.save(output, format=fmt, **options)
    return output.getvalue()


class PilotPreflightTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="p5b-synthetic-", dir="/private/tmp")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.csv = self.root / "pilot.csv"
        self.mapping_file = self.root / "mapping.json"
        self.rows, self.mapping = [], {}
        for pilot, (key, pages, _, _) in PILOTS.items():
            for slot in range(pages + 1):
                asset = pilot * 100 + slot
                data = image_bytes("JPEG" if pilot == 18 else "PNG", color=(slot, pilot, 96))
                path = self.root / f"{asset}.original"
                path.write_bytes(data)
                self.mapping[str(asset)] = str(path)
                base = dict(observed_at="2026-10-21 13:00:00+03:00", pilot_content_id=pilot,
                            expected_external_key=key, language_code="tr", content_id=pilot,
                            external_key=key, content_type="STORY", declared_page_count=pages,
                            localization_id=pilot, page_number=slot or None,
                            page_id=asset if slot else None, page_localization_id=asset if slot else None,
                            source_asset_id=asset, asset_id=asset, provider="SYNTHETIC",
                            object_path=f"synthetic/{asset}", mime_type="image/jpeg" if pilot == 18 else "image/png",
                            byte_size=len(data), checksum_sha256=sha256(data).hexdigest())
                for role in (["READING_PAGE"] if slot else ["READING_COVER", "READING_DETAIL"]):
                    self.rows.append({**base, "role": role})

    def run_report(self):
        with self.csv.open("w", newline="", encoding="utf-8-sig") as handle:
            writer = csv.DictWriter(handle, fieldnames=FIELDS)
            writer.writeheader()
            writer.writerows(self.rows)
        self.mapping_file.write_text(json.dumps(self.mapping), encoding="utf-8")
        return preflight.run_preflight(self.csv, self.mapping_file)

    def assert_blocked(self, report):
        self.assertEqual("BLOCKED", report["status"])
        self.assertEqual("NOT_EVALUATED", report["p2Result"])

    def test_complete_synthetic_sources_preserve_bytes_and_separate_roles(self):
        before = {key: sha256(Path(path).read_bytes()).hexdigest() for key, path in self.mapping.items()}
        report = self.run_report()
        self.assertEqual("LOCAL_OBSERVATIONS_COMPLETE", report["status"])
        observed = report["baselineComparison"]["observedCsv"]
        self.assertEqual((43, 43, 41), tuple(observed[key] for key in ("roleRows", "boundRoleLinks", "uniqueSources")))
        self.assertEqual(sum(Path(path).stat().st_size for path in self.mapping.values()), observed["recordedUniqueBytes"])
        self.assertEqual(0, report["baselineComparison"]["deltaFromP5a"]["uniqueSources"])
        self.assertNotEqual(0, report["baselineComparison"]["deltaFromP5a"]["recordedUniqueBytes"])
        self.assertTrue(all(source["observed"]["image"]["fullDecode"] for source in report["sources"]))
        for source in report["sources"]:
            for match in ("byteMatch", "sha256Match", "mimeMatch"):
                self.assertIs(True, source[match])
        summaries = {summary["pilotContentId"]: summary for summary in report["pilots"]}
        for pilot, (_, pages, count, expected_bytes) in PILOTS.items():
            size = sum(Path(self.mapping[str(pilot * 100 + slot)]).stat().st_size for slot in range(pages + 1))
            self.assertEqual({"roleRows": pages + 2, "boundRoleLinks": pages + 2, "uniqueSources": count,
                              "recordedUniqueBytes": size}, summaries[pilot]["observedCsv"])
            self.assertEqual({"roleRows": pages + 2, "boundRoleLinks": pages + 2, "uniqueSources": count,
                              "recordedUniqueBytes": expected_bytes}, summaries[pilot]["expectedP5a"])
            self.assertEqual({"roleRows": 0, "boundRoleLinks": 0, "uniqueSources": 0,
                              "recordedUniqueBytes": size - expected_bytes}, summaries[pilot]["deltaFromP5a"])
        self.assertEqual("NOT_ESTABLISHED", report["realPilotValidation"])
        self.assertNotIn("synthetic/", json.dumps(report))
        self.assertNotIn(str(self.root), json.dumps(report))
        self.assertEqual(before, {key: sha256(Path(path).read_bytes()).hexdigest() for key, path in self.mapping.items()})

    def test_missing_source_and_missing_sha_are_unverified(self):
        self.mapping.pop("1800")
        self.rows[2]["checksum_sha256"] = None
        report = self.run_report()
        self.assert_blocked(report)
        self.assertIn("ORIGINAL_NOT_PROVIDED", report["sources"][0]["errors"])
        self.assertIsNone(report["sources"][1]["sha256Match"])

    def test_missing_bytes_are_not_zero(self):
        self.rows[0]["byte_size"] = self.rows[1]["byte_size"] = None
        report = self.run_report()
        self.assert_blocked(report)
        self.assertIsNone(report["baselineComparison"]["observedCsv"]["recordedUniqueBytes"])
        self.assertIsNone(report["baselineComparison"]["deltaFromP5a"]["recordedUniqueBytes"])

    def test_byte_hash_mime_mismatch_have_expected_and_observed(self):
        for row in self.rows[:2]:
            row.update(byte_size=1, checksum_sha256="0" * 64, mime_type="image/png")
        report = self.run_report()
        self.assert_blocked(report)
        source = report["sources"][0]
        self.assertEqual(1, source["expected"]["bytes"])
        self.assertGreater(source["observed"]["bytes"], 1)
        for match in ("byteMatch", "sha256Match", "mimeMatch"):
            self.assertIs(False, source[match])

    def test_only_sha_mismatch_blocks_report_and_cli(self):
        for row in self.rows[:2]:
            row["checksum_sha256"] = "0" * 64
        report = self.run_report()
        self.assert_blocked(report)
        source = report["sources"][0]
        self.assertIs(True, source["byteMatch"])
        self.assertIs(True, source["mimeMatch"])
        self.assertIs(False, source["sha256Match"])
        self.assertEqual(["SHA256MATCH_MISMATCH"], source["errors"])
        output = self.root / "sha-blocked.json"
        self.assertEqual(1, preflight.main(["--csv", str(self.csv), "--mapping", str(self.mapping_file),
                                            "--report", str(output)]))
        self.assert_blocked(json.loads(output.read_text()))

    def test_csv_missing_role_and_changed_unique_count_are_separate(self):
        before = {summary["pilotContentId"]: summary for summary in self.run_report()["pilots"]}
        removed_bytes = Path(self.mapping["1801"]).stat().st_size
        self.rows = [row for row in self.rows if row["role"] != "READING_DETAIL" or row["pilot_content_id"] != 18]
        self.rows[1]["asset_id"] = self.rows[1]["source_asset_id"] = 1800
        self.rows[1].update({key: self.rows[0][key] for key in ("provider", "object_path", "mime_type", "byte_size", "checksum_sha256")})
        self.mapping.pop("1801")
        report = self.run_report()
        self.assert_blocked(report)
        self.assertEqual(-1, report["baselineComparison"]["deltaFromP5a"]["roleRows"])
        self.assertEqual(-1, report["baselineComparison"]["deltaFromP5a"]["uniqueSources"])
        after = {summary["pilotContentId"]: summary for summary in report["pilots"]}
        self.assertEqual(before[62], after[62])
        size = before[18]["observedCsv"]["recordedUniqueBytes"] - removed_bytes
        self.assertEqual({"roleRows": 17, "boundRoleLinks": 17, "uniqueSources": 16,
                          "recordedUniqueBytes": size}, after[18]["observedCsv"])
        self.assertEqual({"roleRows": -1, "boundRoleLinks": -1, "uniqueSources": -1,
                          "recordedUniqueBytes": size - 6_191_423}, after[18]["deltaFromP5a"])

    def test_cross_pilot_conflicting_metadata_has_no_invented_total(self):
        before = {summary["pilotContentId"]: summary for summary in self.run_report()["pilots"]}
        metadata = {key: self.rows[0][key] for key in
                    ("asset_id", "source_asset_id", "provider", "object_path", "mime_type", "byte_size", "checksum_sha256")}
        metadata["byte_size"] += 1
        for row in self.rows:
            if row["pilot_content_id"] == 62 and row["role"] in ("READING_COVER", "READING_DETAIL"):
                row.update(metadata)
        self.mapping.pop("6200")
        report = self.run_report()
        self.assert_blocked(report)
        self.assertIn("CONFLICTING_ASSET_METADATA", {error["code"] for error in report["errors"]})
        after = {summary["pilotContentId"]: summary for summary in report["pilots"]}
        self.assertEqual(before[18], after[18])
        self.assertIsNone(after[62]["observedCsv"]["recordedUniqueBytes"])
        self.assertIsNone(after[62]["deltaFromP5a"]["recordedUniqueBytes"])
        self.assertIsNone(report["baselineComparison"]["observedCsv"]["recordedUniqueBytes"])

    def test_missing_scope_page_and_media_bindings_are_blockers(self):
        for field in ("content_id", "localization_id", "page_id", "page_localization_id", "asset_id"):
            with self.subTest(field=field):
                saved = copy.deepcopy(self.rows)
                self.rows[2][field] = None
                self.assert_blocked(self.run_report())
                self.rows = saved
        self.rows = [row for row in self.rows if row["pilot_content_id"] == 18]
        self.assert_blocked(self.run_report())

    def test_page_gap_duplicate_and_conflicting_metadata(self):
        self.rows.pop(3)
        self.rows.append(copy.deepcopy(self.rows[0]))
        self.rows[1]["byte_size"] = 1
        codes = {error["code"] for error in self.run_report()["errors"]}
        self.assertTrue({"PAGE_SET_MISMATCH", "DUPLICATE_ROLE_SLOT", "CONFLICTING_ASSET_METADATA"} <= codes)

    def test_multiple_statement_timestamps_and_pilot_scope_rejected(self):
        self.rows[0]["observed_at"] = "2026-10-21 14:00:00+03:00"
        self.assert_blocked(self.run_report())
        self.rows[0]["pilot_content_id"] = 99
        self.assert_blocked(self.run_report())

    def test_missing_bad_extra_csv_columns_and_invalid_sha(self):
        self.run_report()
        for data in (b"no,header\n1,2\n", self.csv.read_bytes().replace(b"byte_size", b"wrong_size"),
                     self.csv.read_bytes() + b"too,few\n"):
            self.csv.write_bytes(data)
            self.assert_blocked(preflight.run_preflight(self.csv, self.mapping_file))
        self.rows[0]["checksum_sha256"] = "bad"
        self.assert_blocked(self.run_report())

    def test_mapping_invalid_duplicate_and_extra_id(self):
        self.run_report()
        for data in ('[]', '{"1800":"/private/tmp/a","1800":"/private/tmp/b"}',
                     '{"01800":"/private/tmp/a"}', '{"1800":"relative.png"}'):
            self.mapping_file.write_text(data)
            self.assert_blocked(preflight.run_preflight(self.csv, self.mapping_file))
        self.mapping["9999"] = str(self.root / "extra")
        self.assert_blocked(self.run_report())

    def test_unencodable_mapping_path_produces_cli_report(self):
        self.mapping["1800"] = str(self.root / "\ud800")
        self.run_report()
        output = self.root / "invalid-path.json"
        self.assertEqual(1, preflight.main(["--csv", str(self.csv), "--mapping", str(self.mapping_file),
                                            "--report", str(output)]))
        report = json.loads(output.read_text())
        self.assert_blocked(report)
        self.assertEqual([{"code": "MAPPING_PATH_INVALID"}], report["errors"])
        self.assertNotIn("\\ud800", output.read_text())

    def test_large_declared_page_count_is_explicitly_blocked(self):
        for row in self.rows:
            if row["pilot_content_id"] == 18:
                row["declared_page_count"] = 2_147_483_647
        report = self.run_report()
        self.assert_blocked(report)
        self.assertEqual([{"code": "DECLARED_PAGE_COUNT_LIMIT"}], report["errors"])

    def test_corrupt_empty_and_truncated_sources(self):
        for data in (b"broken", b"", image_bytes("JPEG")[:-12], image_bytes()[:-15]):
            with self.subTest(length=len(data)):
                Path(self.mapping["1800"]).write_bytes(data)
                self.assert_blocked(self.run_report())

    def test_symlink_ancestor_directory_fifo_missing_and_byte_limit_rejected(self):
        target = Path(self.mapping["1800"])
        link = self.root / "symlink"
        link.symlink_to(target)
        ancestor = self.root / "linked-directory"
        ancestor.symlink_to(self.root, target_is_directory=True)
        fifo = self.root / "fifo"
        os.mkfifo(fifo)
        sparse = self.root / "large"
        with sparse.open("wb") as handle:
            handle.truncate(MAX_FILE_BYTES + 1)
        for path in (link, ancestor / target.name, self.root, fifo, self.root / "absent", sparse):
            with self.subTest(path=path.name), self.assertRaises(PreflightError):
                read_bounded(path)

    def test_cli_writes_new_private_report_and_never_overwrites_inputs(self):
        self.run_report()
        output = self.root / "report.json"
        argv = ["--csv", str(self.csv), "--mapping", str(self.mapping_file), "--report", str(output)]
        self.assertEqual(0, preflight.main(argv))
        self.assertEqual(0o600, output.stat().st_mode & 0o777)
        before = self.csv.read_bytes()
        argv[-1] = str(self.csv)
        self.assertEqual(2, preflight.main(argv))
        self.assertEqual(before, self.csv.read_bytes())
        self.mapping.pop("1800")
        self.run_report()
        argv[-1] = str(self.root / "blocked.json")
        self.assertEqual(1, preflight.main(argv))


class ImageProbeTest(unittest.TestCase):
    def test_empty_exif_block_presence_is_separate_from_tag_count(self):
        exif = b"Exif\x00\x00II\x2a\x00\x08\x00\x00\x00\x00\x00\x00\x00\x00\x00"
        result = inspect_image(image_bytes("JPEG", exif=exif))
        self.assertIs(True, result["exifPresent"])
        self.assertEqual(0, result["exifEntryCount"])
        self.assertIsNone(result["orientation"])
        self.assertIn("EXIF_PRESENT", result["reviewSignals"])
        self.assertTrue(result["fullDecode"])
        self.assertEqual("NOT_EVALUATED", result["p2Result"])

    def test_orientation_icc_and_alpha_are_observed_without_p2_acceptance(self):
        exif = Image.Exif()
        exif[274] = 6
        result = inspect_image(image_bytes("JPEG", exif=exif, icc_profile=b"synthetic-icc"))
        self.assertEqual((6, True, True), (result["orientation"], result["exifPresent"], result["iccPresent"]))
        self.assertTrue(result["fullDecode"])
        self.assertEqual("NOT_EVALUATED", result["p2Result"])
        self.assertTrue({"EXIF_PRESENT", "NONIDENTITY_ORIENTATION", "ICC_PRESENT"} <= set(result["reviewSignals"]))
        alpha = inspect_image(image_bytes(mode="RGBA", color=(0, 10, 20, 127)))
        self.assertTrue(alpha["alphaChannel"] and alpha["transparentPixels"])
        opaque = inspect_image(image_bytes(mode="RGBA", color=(0, 10, 20, 255)))
        self.assertTrue(opaque["alphaChannel"])
        self.assertFalse(opaque["transparentPixels"])

    def test_palette_transparency_metadata(self):
        data = image_bytes(mode="P", color=0, transparency=0)
        result = inspect_image(data)
        self.assertTrue(result["transparencyMetadata"] and result["transparentPixels"])

    def test_unsupported_format_and_frames_are_explicit(self):
        result = inspect_image(image_bytes("GIF"))
        self.assertEqual("GIF", result["format"])
        self.assertIn("UNSUPPORTED_FORMAT", result["errors"])
        self.assertFalse(result["fullDecode"])
        output = BytesIO()
        with Image.new("RGB", (8, 6), "red") as first, Image.new("RGB", (8, 6), "blue") as second:
            first.save(output, format="PNG", save_all=True, append_images=[second])
        result = inspect_image(output.getvalue())
        self.assertEqual(2, result["frames"])
        self.assertIn("MULTIFRAME_UNSUPPORTED", result["errors"])

    def test_pixel_limit_before_decode(self):
        import struct
        import zlib
        data = bytearray(image_bytes())
        data[16:24] = struct.pack(">II", 8000, 5001)
        data[29:33] = struct.pack(">I", zlib.crc32(data[12:29]))
        result = inspect_image(bytes(data))
        self.assertIn("PIXEL_LIMIT", result["errors"])
        self.assertFalse(result["fullDecode"])


if __name__ == "__main__":
    unittest.main()
