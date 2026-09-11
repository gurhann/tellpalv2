from __future__ import annotations

import io
import json
import os
import sys
import tempfile
import threading
import unittest
import zipfile
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest.mock import ANY, MagicMock, call, patch
from urllib.parse import urlparse

SCRIPT_DIR = Path(__file__).resolve().parents[1]
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import import_lullabies
from lullaby_import_workflow import execute_import, remote_preflight
from lullaby_manifest import StoryValidationError, _extract_single_audio, _mp3_duration_seconds, _read_rows, build_lullaby_plan, parse_summary
from story_import_models import ContributorResolution
from tellpal_admin_client import TellPalAdminClient


class _WorkflowReport:
    def mark_running(self): pass
    def set_phase(self, _phase): pass
    def add_content_id(self, _content_id): pass
    def record_step(self, _step): pass
    def record_asset(self, **_asset): pass
    def mark_success(self, _summary): pass


class _WorkflowClient:
    def __init__(self):
        self.calls = []
        self.content = {"contentId": 1, "type": "LULLABY", "localizations": []}
        self.assets = {}
        self.instruments = []
        self.contributors = []

    def create_content(self, body):
        self.calls.append("create")
        self.content.update(body)
        return self.content

    def upload_media(self, path, kind, checksum):
        self.calls.append("media")
        asset_id = len(self.assets) + 1
        payload = {"assetId": asset_id, "byteSize": Path(path).stat().st_size, "checksumSha256": checksum, "kind": kind}
        self.assets[asset_id] = payload
        return payload

    def update_content(self, _content_id, body):
        self.calls.append("cover")
        self.content.update(body)
        return self.content

    def update_lullaby_playback(self, _content_id, audio_media_id, duration_minutes):
        self.calls.append("playback")
        self.content["playback"] = {"audioMediaId": audio_media_id, "durationMinutes": duration_minutes, "processingStatus": "PENDING"}
        return self.content["playback"]

    def replace_lullaby_instruments(self, _content_id, codes):
        self.calls.append("instruments")
        self.instruments = [{"code": code, "displayOrder": index} for index, code in enumerate(codes)]
        return self.instruments

    def create_localization(self, _content_id, language_code, body):
        self.calls.append("localization")
        self.content["localizations"].append({"languageCode": language_code, "audioMediaId": None, "coverMediaId": None, **body})
        return self.content["localizations"][-1]

    def assign_contributor(self, _content_id, body):
        self.calls.append("contributor")
        self.contributors.append({"role": body["role"], "languageCode": body["languageCode"], "contributorDisplayName": "Ali Kaan Uysal"})
        return self.contributors[-1]

    def get_content(self, _content_id): return self.content
    def get_media(self, asset_id): return self.assets[asset_id]
    def list_lullaby_instruments(self, _content_id): return self.instruments
    def list_content_contributors(self, _content_id): return self.contributors

    def publish_localization(self, _content_id, language_code):
        self.calls.append("publish")
        next(item for item in self.content["localizations"] if item["languageCode"] == language_code)["status"] = "PUBLISHED"
        return self.content


class _Response:
    def __init__(self, payload: bytes):
        self._stream = io.BytesIO(payload)
        self.headers = {"Content-Length": str(len(payload))}

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self, size: int = -1) -> bytes:
        return self._stream.read(size)


def _mp3(seed: int = 0) -> bytes:
    frame = b"\xff\xfb\x90\x64" + bytes([seed]) * (417 - 4)
    return frame * 2


def _mp3_frame(*, sample_index: int) -> bytes:
    sample_rate = (44100, 48000)[sample_index]
    frame_length = (144 * 128000) // sample_rate
    return bytes((0xFF, 0xFB, 0x90 | (sample_index << 2), 0x64)) + b"\x00" * (frame_length - 4)


def _zip(payload: bytes) -> bytes:
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("track.mp3", payload)
    return stream.getvalue()


class LullabyManifestTest(unittest.TestCase):
    def test_parse_summary_maps_catalog_codes_and_reports_piano(self):
        musician, codes, unsupported = parse_summary(
            " Müzik: Ali Kaan Uysal\n Enstrüman: Piyano ve Yaylı Orkestra "
        )
        self.assertEqual(musician, "Ali Kaan Uysal")
        self.assertEqual(codes, ("STRING_ORCHESTRA",))
        self.assertEqual(unsupported, ("Piyano",))

    def test_parse_summary_rejects_multiple_music_or_instrument_values(self):
        with self.assertRaisesRegex(StoryValidationError, "ambiguous Music"):
            parse_summary("Music: A\nMusic: B")
        with self.assertRaisesRegex(StoryValidationError, "ambiguous Instrument"):
            parse_summary("Instrument: Bell\nInstrument: Harp")

    def test_extract_rejects_oversized_zip_member_before_writing(self):
        with tempfile.TemporaryDirectory() as directory:
            zip_path = Path(directory) / "1.zip"
            with zipfile.ZipFile(zip_path, "w") as archive:
                archive.writestr("track.mp3", b"x" * 11)
            with patch("lullaby_manifest.MAX_EXTRACTED_AUDIO_BYTES", 10):
                with self.assertRaisesRegex(StoryValidationError, "extraction limit"):
                    _extract_single_audio(zip_path, Path(directory) / "audio", 1)

    def test_invalid_zip_and_invalid_mp3_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            invalid_zip = root / "invalid.zip"
            invalid_zip.write_bytes(b"not a zip")
            with self.assertRaisesRegex(StoryValidationError, "valid ZIP"):
                _extract_single_audio(invalid_zip, root / "audio", 1)
            invalid_mp3_zip = root / "invalid-mp3.zip"
            with zipfile.ZipFile(invalid_mp3_zip, "w") as archive:
                archive.writestr("track.mp3", b"not mp3")
            with self.assertRaisesRegex(StoryValidationError, "valid MP3 frame"):
                _extract_single_audio(invalid_mp3_zip, root / "audio", 2)

    def test_id3_only_audio_is_rejected_even_with_duration_override(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            zip_path = root / "1.zip"
            with zipfile.ZipFile(zip_path, "w") as archive:
                archive.writestr("track.mp3", b"ID3" + b"\x00" * 32)
            with self.assertRaisesRegex(StoryValidationError, "valid MP3 frame"):
                _extract_single_audio(zip_path, root / "audio", 1)

    def test_duration_sums_each_frame_at_its_own_sample_rate(self):
        with tempfile.TemporaryDirectory() as directory:
            audio_path = Path(directory) / "mixed.mp3"
            audio_path.write_bytes(_mp3_frame(sample_index=0) + _mp3_frame(sample_index=1))
            self.assertAlmostEqual(
                _mp3_duration_seconds(audio_path),
                1152 / 44100 + 1152 / 48000,
            )

    def test_csv_parse_errors_and_duplicate_headers_are_validation_errors(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            malformed = root / "malformed.csv"
            malformed.write_bytes(b"\xff\xfe")
            with self.assertRaisesRegex(StoryValidationError, "Cannot parse"):
                _read_rows(malformed)
            duplicate_headers = root / "duplicate.csv"
            duplicate_headers.write_text(
                "language,id,name,summary,image_url,image_url,summary_image_url\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(StoryValidationError, "duplicate columns"):
                _read_rows(duplicate_headers)

    def test_build_groups_languages_and_reuses_two_cover_roles(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "lullabies.csv"
            csv_path.write_text(
                "language,id,name,summary,image_url,summary_image_url\n"
                "en,156,Traditional Lullaby,\"Music: Ali Kaan Uysal\nInstrument: Piano - Glockenspiel\",cover.jpg,anim.gif\n"
                "tr,144,Dandini Dastana,\"Müzik: Ali Kaan Uysal\nEnstrüman: Piyano - Glockenspiel\",cover.jpg,anim.gif\n"
                "pt,310,Canção de Ninar Tradicional,\"Music: Ali Kaan Uysal\nInstrument: Piano - Glockenspiel\",cover.jpg,anim.gif\n",
                encoding="utf-8",
            )
            objects = {
                "/bucket/cover_images/cover.jpg": b"\xff\xd8\xff\xe0",
                "/bucket/cover_images/anim.gif": b"GIF89a123",
                "/bucket/156.zip": _zip(_mp3()),
                "/bucket/144.zip": _zip(_mp3()),
                "/bucket/310.zip": _zip(_mp3()),
            }

            def open_url(url: str, timeout: float):
                path = "/" + url.split("/", 3)[3]
                return _Response(objects[path])

            with patch("urllib.request.urlopen", side_effect=open_url):
                plan = build_lullaby_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket")

            self.assertEqual(len(plan.groups), 1)
            group = plan.groups[0]
            self.assertEqual(group.external_key, "lullaby.dandini-dastana")
            self.assertEqual({item.language_code for item in group.localizations}, {"en", "tr", "pt"})
            self.assertEqual(group.instrument_codes, ("GLOCKENSPIEL",))
            self.assertEqual(group.duration_minutes, 1)
            self.assertEqual(len(plan.media), 3)
            self.assertEqual(plan.expected_actions["contents"], 1)
            self.assertEqual(plan.expected_actions["contributor_assignments"], 1)
            self.assertTrue(any("Piano" in warning or "Piyano" in warning for warning in plan.warnings))
            client = _WorkflowClient()
            summary = execute_import(
                plan,
                client,
                _WorkflowReport(),
                (ContributorResolution("Ali Kaan Uysal", 99),),
            )
            self.assertEqual(summary["groups"], 1)
            self.assertEqual(client.calls, [
                "create", "media", "media", "media", "cover", "playback", "instruments",
                "localization", "localization", "localization", "contributor", "publish", "publish", "publish",
            ])

    def test_duration_override_only_fills_unreadable_frame_duration(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "lullabies.csv"
            csv_path.write_text(
                "language,id,name,summary,image_url,summary_image_url\n"
                "tr,1,Ninni,Music: M,cover.jpg,anim.gif\n",
                encoding="utf-8",
            )
            objects = {
                "/bucket/cover_images/cover.jpg": b"\xff\xd8\xff\xe0",
                "/bucket/cover_images/anim.gif": b"GIF89a123",
                "/bucket/1.zip": _zip(_mp3()),
            }
            def open_url(url: str, timeout: float):
                return _Response(objects["/" + url.split("/", 3)[3]])
            with patch("urllib.request.urlopen", side_effect=open_url):
                plan = build_lullaby_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", duration_override=99)
            self.assertEqual(plan.groups[0].duration_minutes, 1)
            objects["/bucket/1.zip"] = _zip(b"ID3" + b"\x00" * 20)
            with patch("urllib.request.urlopen", side_effect=open_url):
                with self.assertRaisesRegex(StoryValidationError, "valid MP3 frame"):
                    build_lullaby_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", duration_override=5)
            with self.assertRaisesRegex(StoryValidationError, "positive integer"):
                build_lullaby_plan(csv_path, duration_override=0)

    def test_case_distinct_cover_names_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path = Path(directory) / "lullabies.csv"
            csv_path.write_text(
                "language,id,name,summary,image_url,summary_image_url\n"
                "tr,1,Ninni,Music: M,Cover.jpg,anim.gif\n"
                "en,2,Lullaby,Music: M,cover.jpg,anim.gif\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(StoryValidationError, "differ only by case"):
                build_lullaby_plan(csv_path)

    def test_rejects_different_shared_audio(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "lullabies.csv"
            csv_path.write_text(
                "language,id,name,summary,image_url,summary_image_url\n"
                "tr,1,Gece Ninnisi,Music: M,cover.jpg,anim.gif\n"
                "en,2,Night Lullaby,Music: M,cover.jpg,anim.gif\n",
                encoding="utf-8",
            )
            objects = {
                "/bucket/cover_images/cover.jpg": b"\xff\xd8\xff\xe0",
                "/bucket/cover_images/anim.gif": b"GIF89a123",
                "/bucket/1.zip": _zip(_mp3(1)),
                "/bucket/2.zip": _zip(_mp3(2)),
            }

            def open_url(url: str, timeout: float):
                path = "/" + url.split("/", 3)[3]
                return _Response(objects[path])

            with patch("urllib.request.urlopen", side_effect=open_url):
                objects["/bucket/cover_images/cover.jpg"] = b"not-an-image"
                with self.assertRaisesRegex(StoryValidationError, "signature"):
                    build_lullaby_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket")
                objects["/bucket/cover_images/cover.jpg"] = b"\xff\xd8\xff\xe0"
                with self.assertRaisesRegex(StoryValidationError, "different audio files"):
                    build_lullaby_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket")

    def test_user_csv_has_eleven_groups_and_thirty_three_localizations(self):
        csv_path = Path(__file__).resolve().parents[5] / "cms" / "yuklenecek_hikayeler" / "lullabies" / "lullabies.csv"

        def open_url(url: str, timeout: float):
            object_name = urlparse(url).path.split("/", 2)[-1]
            if object_name.endswith(".zip"):
                payload = _zip(_mp3())
            elif object_name.lower().endswith(".gif"):
                payload = b"GIF89a123"
            else:
                payload = b"\xff\xd8\xff\xe0"
            return _Response(payload)

        with patch("urllib.request.urlopen", side_effect=open_url):
            plan = build_lullaby_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket")
        self.assertEqual(len(plan.groups), 11)
        self.assertEqual(sum(len(group.localizations) for group in plan.groups), 33)
        self.assertEqual(plan.expected_actions["publications"], 33)

    def test_external_key_selects_only_one_lullaby_group(self):
        csv_path = Path(__file__).resolve().parents[5] / "cms" / "yuklenecek_hikayeler" / "lullabies" / "lullabies.csv"

        def open_url(url: str, timeout: float):
            object_name = urlparse(url).path.split("/", 2)[-1]
            if object_name.endswith(".zip"):
                payload = _zip(_mp3())
            elif object_name.lower().endswith(".gif"):
                payload = b"GIF89a123"
            else:
                payload = b"\xff\xd8\xff\xe0"
            return _Response(payload)

        with patch("urllib.request.urlopen", side_effect=open_url):
            plan = build_lullaby_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                external_key="lullaby.dandini-dastana",
            )
        self.assertEqual([group.external_key for group in plan.groups], ["lullaby.dandini-dastana"])
        self.assertEqual(sum(len(group.localizations) for group in plan.groups), 3)

        class ExistingContentClient:
            def list_contents(self):
                return [{"externalKey": plan.groups[0].external_key}]

        with self.assertRaisesRegex(RuntimeError, "already exists"):
            remote_preflight(plan, ExistingContentClient())


class LullabyAdminClientWireTest(unittest.TestCase):
    def test_lullaby_endpoints_use_expected_paths_queries_and_bodies(self):
        requests: list[tuple[str, str, object]] = []

        class Handler(BaseHTTPRequestHandler):
            def _respond(self, payload: object) -> None:
                raw = json.dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(raw)))
                self.end_headers()
                self.wfile.write(raw)

            def do_GET(self):  # noqa: N802
                requests.append(("GET", self.path, None))
                self._respond([])

            def do_PUT(self):  # noqa: N802
                length = int(self.headers["Content-Length"])
                requests.append(("PUT", self.path, json.loads(self.rfile.read(length))))
                self._respond({} if self.path.endswith("/playback") else [])

            def log_message(self, *_args):
                return

        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            client = TellPalAdminClient(f"http://127.0.0.1:{server.server_address[1]}", sleep=lambda _: None)
            client.access_token = "test-token"
            client.update_lullaby_playback(42, 7, 3)
            client.replace_lullaby_instruments(42, ["BELL", "HARP"])
            client.list_lullaby_instruments(42, "en")
            client.list_instrument_catalog("pt")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)
        self.assertEqual(requests, [
            ("PUT", "/api/admin/contents/42/playback", {"audioMediaId": 7, "durationMinutes": 3}),
            ("PUT", "/api/admin/contents/42/instruments", {"instrumentCodes": ["BELL", "HARP"]}),
            ("GET", "/api/admin/contents/42/instruments?languageCode=en", None),
            ("GET", "/api/admin/instrument-catalog?languageCode=pt", None),
        ])


class LullabyImportCliTest(unittest.TestCase):
    def test_only_exact_import_confirmation_executes_writes(self):
        for entered, expected_status, expected_execute_calls in (
            ("import", 0, 1),
            (" import", 1, 0),
        ):
            with self.subTest(entered=entered):
                report = MagicMock(result_path=Path("report.json"))
                client = MagicMock()
                client.last_request = {"method": "GET", "path": "/api/admin/contents"}
                execute = MagicMock(return_value={"groups": 1, "contentIds": [1]})
                terminal = MagicMock()
                terminal.isatty.return_value = True
                with (
                    patch.dict(os.environ, {"TELLPAL_API_BASE_URL": "https://api.test", "TELLPAL_ADMIN_USERNAME": "admin"}, clear=True),
                    patch.object(sys, "argv", ["import_lullabies.py", "lullabies.csv"]),
                    patch.object(sys, "stdin", terminal),
                    patch.object(import_lullabies, "build_lullaby_plan", return_value=object()),
                    patch.object(import_lullabies, "LullabyImportRunReport", return_value=report),
                    patch.object(import_lullabies, "TellPalAdminClient", return_value=client),
                    patch.object(import_lullabies, "remote_preflight", return_value=()),
                    patch.object(import_lullabies, "format_preview", return_value="preview"),
                    patch.object(import_lullabies.getpass, "getpass", return_value="secret"),
                    patch("builtins.input", return_value=entered),
                    patch.object(import_lullabies, "execute_import", execute),
                ):
                    self.assertEqual(import_lullabies.main(), expected_status)
                self.assertEqual(execute.call_count, expected_execute_calls)
                if expected_status:
                    report.mark_cancelled.assert_called_once_with()

    def test_keyboard_interrupt_records_last_request_before_failure(self):
        report = MagicMock(result_path=Path("report.json"))
        client = MagicMock()
        client.last_request = {"method": "GET", "path": "/api/admin/contents"}
        terminal = MagicMock()
        terminal.isatty.return_value = True
        with (
            patch.dict(os.environ, {"TELLPAL_API_BASE_URL": "https://api.test", "TELLPAL_ADMIN_USERNAME": "admin"}, clear=True),
            patch.object(sys, "argv", ["import_lullabies.py", "lullabies.csv"]),
            patch.object(sys, "stdin", terminal),
            patch.object(import_lullabies, "build_lullaby_plan", return_value=object()),
            patch.object(import_lullabies, "LullabyImportRunReport", return_value=report),
            patch.object(import_lullabies, "TellPalAdminClient", return_value=client),
            patch.object(import_lullabies, "remote_preflight", side_effect=KeyboardInterrupt()),
            patch.object(import_lullabies.getpass, "getpass", return_value="secret"),
        ):
            self.assertEqual(import_lullabies.main(), 130)
        self.assertEqual(
            report.method_calls,
            [
                call.record_last_request(client.last_request),
                call.mark_failure(ANY),
            ],
        )


if __name__ == "__main__":
    unittest.main()
