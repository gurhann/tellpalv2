from __future__ import annotations

import copy
import csv
import hashlib
import io
import sys
import tempfile
import unittest
import urllib.error
import zipfile
from pathlib import Path
from unittest.mock import MagicMock, patch
from urllib.parse import unquote, urlparse

SCRIPT_DIR = Path(__file__).resolve().parents[1]
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from audio_story_import_workflow import execute_import, remote_preflight
from audio_story_manifest import StoryValidationError, build_audio_story_plan, write_status_csv
from tellpal_admin_client import AdminApiError, AdminTransportError, TellPalAdminClient


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


def _zip(payload: bytes, *, second: bytes | None = None) -> bytes:
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("story.mp3", payload)
        if second is not None:
            archive.writestr("story-2.mp3", second)
    return stream.getvalue()


def _storage(objects: dict[str, bytes]):
    def open_url(request, **_kwargs):
        url = request.full_url if hasattr(request, "full_url") else str(request)
        path = unquote(urlparse(url).path)
        object_name = path.split("/", 2)[-1]
        if object_name not in objects:
            raise urllib.error.HTTPError(url, 404, "missing", {}, io.BytesIO())
        return _Response(objects[object_name])

    return open_url


class _Report:
    def __init__(self):
        self.rows = []
        self.assets = []

    def mark_running(self):
        pass

    def set_phase(self, _phase):
        pass

    def add_content_id(self, _content_id):
        pass

    def record_asset(self, **asset):
        self.assets.append(asset)

    def record_row(self, row):
        self.rows.append((row.line_number, row.status))

    def record_step(self, _step):
        pass

    def mark_success(self, summary):
        self.summary = summary


class _Client:
    def __init__(self, content, assets=None, *, fail_update=False):
        self.contents = [content]
        self.content = content
        self.assets = assets or {}
        self.calls = []
        self.next_asset_id = 300
        self.fail_update = fail_update

    def list_contents(self):
        self.calls.append("list")
        return copy.deepcopy(self.contents)

    def get_content(self, _content_id):
        self.calls.append("get-content")
        return copy.deepcopy(self.content)

    def get_media(self, asset_id):
        self.calls.append(("get-media", asset_id))
        if asset_id not in self.assets:
            raise AdminApiError("GET", f"/api/admin/media/{asset_id}", 404, {"detail": "missing"})
        return copy.deepcopy(self.assets[asset_id])

    def upload_media(self, path, kind, checksum):
        self.calls.append(("upload", kind))
        self.next_asset_id += 1
        asset_id = self.next_asset_id
        self.assets[asset_id] = {
            "assetId": asset_id,
            "mediaType": "IMAGE" if kind == "ORIGINAL_IMAGE" else "AUDIO",
            "checksumSha256": checksum,
            "byteSize": Path(path).stat().st_size,
        }
        return self.assets[asset_id]

    def update_content(self, content_id, body):
        self.calls.append("update-content")
        self.content.update(body)
        self.content["contentId"] = content_id
        return copy.deepcopy(self.content)

    def update_localization(self, _content_id, language_code, body):
        self.calls.append("update-localization")
        if self.fail_update:
            raise AdminTransportError("PUT", "/api/admin/contents/10/localizations/tr", "connection reset")
        localization = next(item for item in self.content["localizations"] if item["languageCode"] == language_code)
        localization.update(body)
        return copy.deepcopy(localization)


class _MultiClient(_Client):
    def __init__(self, contents, *, fail_localization_for=None):
        super().__init__(contents[0])
        self.contents = contents
        self.fail_localization_for = set(fail_localization_for or ())

    def get_content(self, content_id):
        self.calls.append(("get-content", content_id))
        return copy.deepcopy(next(content for content in self.contents if content["contentId"] == content_id))

    def update_content(self, content_id, body):
        self.calls.append(("update-content", content_id))
        content = next(content for content in self.contents if content["contentId"] == content_id)
        content.update(body)
        return copy.deepcopy(content)

    def update_localization(self, content_id, language_code, body):
        self.calls.append(("update-localization", content_id))
        if content_id in self.fail_localization_for:
            raise AdminApiError("PUT", f"/api/admin/contents/{content_id}/localizations/{language_code}", 422, {"detail": "invalid row"})
        content = next(content for content in self.contents if content["contentId"] == content_id)
        localization = next(item for item in content["localizations"] if item["languageCode"] == language_code)
        localization.update(body)
        return copy.deepcopy(localization)


def _content(*, title="Story One", narration=None, cover_id=None, status="PUBLISHED"):
    return {
        "contentId": 10,
        "type": "STORY",
        "externalKey": "story.one",
        "active": True,
        "ageRange": 4,
        "textlessCoverMediaId": 100,
        "listeningCoverMediaId": cover_id,
        "listingCoverMediaId": None,
        "localizations": [{
            "languageCode": "tr",
            "title": title,
            "description": "Keep this description",
            "bodyText": None,
            "coverMediaId": 200,
            "audioMediaId": None,
            "durationMinutes": None,
            "status": status,
            "processingStatus": "COMPLETED",
            "publishedAt": "2026-01-01T00:00:00Z",
            "narration": narration,
        }],
    }


class AudioStoryImportTest(unittest.TestCase):
    def test_admin_client_updates_localization_with_put(self):
        client = TellPalAdminClient("https://api.test")
        client._request_json = MagicMock(return_value={"contentId": 10})
        body = {"title": "Story One", "narration": {"audioMediaId": 22, "durationMinutes": 3}}
        self.assertEqual(client.update_localization(10, "tr", body), {"contentId": 10})
        client._request_json.assert_called_once_with("PUT", "/api/admin/contents/10/localizations/tr", body=body)

    def test_manifest_classifies_duplicates_and_row_storage_errors(self):
        csv_text = "language,id,name,image_url\ntr,1,Story One,one.jpg\ntr,1,Story One,one.jpg\nen,2,Missing,missing.jpg\n"
        objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(
                    csv_path,
                    storage_base_url="https://storage.test",
                    storage_bucket="bucket",
                    cache_directory=root / "cache",
                )
            self.assertEqual([row.status for row in plan.rows], ["PENDING", "SKIPPED_DUPLICATE", "ERROR"])
            write_status_csv(plan)
            with open(plan.status_csv_path, encoding="utf-8-sig", newline="") as source:
                status_rows = list(csv.DictReader(source))
            self.assertEqual(status_rows[1]["status"], "SKIPPED_DUPLICATE")

    def test_manifest_accepts_case_insensitive_headers_and_preserves_zero_padded_storage_id(self):
        csv_text = "Language,ID,Name,Image_URL\ntr,001,Story One,one.jpg\n"
        objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "001.zip": _zip(_mp3())}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(
                    csv_path,
                    storage_base_url="https://storage.test",
                    storage_bucket="bucket",
                    cache_directory=root / "cache",
                )
            self.assertEqual(plan.rows[0].status, "PENDING")
            self.assertEqual(plan.rows[0].legacy_id_text, "001")
            self.assertEqual(plan.rows[0].duration_minutes, 1)

    def test_status_csv_cannot_replace_source_csv(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            with self.assertRaises(StoryValidationError):
                build_audio_story_plan(csv_path, status_csv_path=csv_path, cache_directory=root / "cache")

    def test_success_preserves_localization_state_and_attaches_shared_cover(self):
        csv_text = "language,id,name,image_url\ntr,1,Story   One,one.jpg\n"
        objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            client = _Client(_content(title="Story One"))
            contexts = remote_preflight(plan, client)
            report = _Report()
            summary = execute_import(plan, client, report, contexts)
            localization = client.content["localizations"][0]
            self.assertEqual(summary["success"], 1)
            self.assertEqual(plan.rows[0].status, "SUCCESS")
            self.assertEqual(localization["status"], "PUBLISHED")
            self.assertEqual(localization["description"], "Keep this description")
            self.assertEqual(localization["narration"]["audioMediaId"], plan.rows[0].audio_asset_id)
            self.assertEqual(client.content["textlessCoverMediaId"], 100)
            self.assertNotIn("publish", client.calls)

    def test_existing_narration_is_reused_when_only_cover_is_missing(self):
        audio = _mp3()
        cover = b"\xff\xd8\xff\xe0"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            objects = {"cover_images/one.jpg": cover, "1.zip": _zip(audio)}
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            audio_id = 42
            client = _Client(
                _content(narration={"audioMediaId": audio_id, "durationMinutes": plan.rows[0].duration_minutes}),
                {audio_id: {"assetId": audio_id, "mediaType": "AUDIO", "checksumSha256": hashlib.sha256(audio).hexdigest()}},
            )
            contexts = remote_preflight(plan, client)
            execute_import(plan, client, _Report(), contexts)
            self.assertEqual(plan.rows[0].status, "SUCCESS")
            self.assertNotIn("update-localization", client.calls)

    def test_execution_persists_final_status_to_sidecar(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            status_path = root / "audio_stories.import.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(
                    csv_path,
                    storage_base_url="https://storage.test",
                    storage_bucket="bucket",
                    cache_directory=root / "cache",
                    status_csv_path=status_path,
                )
            contexts = remote_preflight(plan, _Client(_content()))
            execute_import(plan, _Client(_content()), _Report(), contexts)
            with status_path.open(encoding="utf-8-sig", newline="") as source:
                self.assertEqual(next(csv.DictReader(source))["status"], "SUCCESS")

    def test_deterministic_mutation_error_continues_to_later_row(self):
        csv_text = "language,id,name,image_url\ntr,1,Story One,one.jpg\ntr,2,Story Two,two.jpg\n"
        objects = {
            "cover_images/one.jpg": b"\xff\xd8\xff\xe0",
            "cover_images/two.jpg": b"\xff\xd8\xff\xe1",
            "1.zip": _zip(_mp3()),
            "2.zip": _zip(_mp3(1)),
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            first = _content(title="Story One")
            second = _content(title="Story Two")
            second["contentId"] = 11
            client = _MultiClient([first, second], fail_localization_for={10})
            contexts = remote_preflight(plan, client)
            summary = execute_import(plan, client, _Report(), contexts)
            self.assertEqual([row.status for row in plan.rows], ["ERROR", "SUCCESS"])
            self.assertEqual(summary["errors"], 1)

    def test_conflicting_existing_narration_is_row_error_without_writes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            audio = _mp3()
            objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(audio)}
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            client = _Client(_content(narration={"audioMediaId": 42, "durationMinutes": 999}), {42: {"assetId": 42, "mediaType": "AUDIO", "checksumSha256": hashlib.sha256(audio).hexdigest()}})
            contexts = remote_preflight(plan, client)
            execute_import(plan, client, _Report(), contexts)
            self.assertEqual(plan.rows[0].status, "ERROR")
            self.assertNotIn("update-content", client.calls)

    def test_same_target_with_different_source_is_marked_error(self):
        csv_text = "language,id,name,image_url\ntr,1,Story One,one.jpg\ntr,1,Story One,two.jpg\n"
        objects = {
            "cover_images/one.jpg": b"\xff\xd8\xff\xe0",
            "cover_images/two.jpg": b"\xff\xd8\xff\xe1",
            "1.zip": _zip(_mp3()),
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            contexts = remote_preflight(plan, _Client(_content()))
            self.assertEqual(plan.rows[0].status, "PENDING")
            self.assertEqual(plan.rows[1].status, "ERROR")
            self.assertEqual(len(contexts), 2)

    def test_existing_compatible_assets_are_already_imported_without_writes(self):
        audio = _mp3()
        cover = b"\xff\xd8\xff\xe0"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            objects = {"cover_images/one.jpg": cover, "1.zip": _zip(audio)}
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            assets = {
                41: {"assetId": 41, "mediaType": "IMAGE", "checksumSha256": hashlib.sha256(cover).hexdigest()},
                42: {"assetId": 42, "mediaType": "AUDIO", "checksumSha256": hashlib.sha256(audio).hexdigest()},
            }
            client = _Client(_content(narration={"audioMediaId": 42, "durationMinutes": plan.rows[0].duration_minutes}, cover_id=41), assets)
            contexts = remote_preflight(plan, client)
            summary = execute_import(plan, client, _Report(), contexts)
            self.assertEqual(summary["alreadyImported"], 1)
            self.assertEqual(plan.rows[0].status, "ALREADY_IMPORTED")
            self.assertFalse(any(call[0] == "upload" for call in client.calls if isinstance(call, tuple)))
            self.assertNotIn("update-localization", client.calls)

    def test_row_error_does_not_block_later_success(self):
        csv_text = "language,id,name,image_url\ntr,1,Not In CMS,one.jpg\ntr,1,Story One,one.jpg\n"
        objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            client = _Client(_content())
            contexts = remote_preflight(plan, client)
            summary = execute_import(plan, client, _Report(), contexts)
            self.assertEqual(plan.rows[0].status, "ERROR")
            self.assertEqual(plan.rows[1].status, "SUCCESS")
            self.assertEqual(summary["errors"], 1)

    def test_multiple_mp3_is_row_error_and_later_row_continues(self):
        csv_text = "language,id,name,image_url\ntr,1,Broken Story,broken.jpg\ntr,2,Story One,one.jpg\n"
        objects = {
            "cover_images/broken.jpg": b"\xff\xd8\xff\xe0",
            "cover_images/one.jpg": b"\xff\xd8\xff\xe0",
            "1.zip": _zip(_mp3(), second=_mp3(1)),
            "2.zip": _zip(_mp3()),
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            client = _Client(_content())
            contexts = remote_preflight(plan, client)
            execute_import(plan, client, _Report(), contexts)
            self.assertEqual(plan.rows[0].status, "ERROR")
            self.assertEqual(plan.rows[1].status, "SUCCESS")

    def test_conflicting_existing_listening_cover_is_row_error_without_upload(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            client = _Client(
                _content(cover_id=41),
                {41: {"assetId": 41, "mediaType": "IMAGE", "checksumSha256": "different-cover"}},
            )
            contexts = remote_preflight(plan, client)
            execute_import(plan, client, _Report(), contexts)
            self.assertEqual(plan.rows[0].status, "ERROR")
            self.assertNotIn("update-content", client.calls)

    def test_ambiguous_mutation_marks_unknown_and_stops(self):
        csv_text = "language,id,name,image_url\ntr,1,Story One,one.jpg\ntr,1,Story One,one.jpg\n"
        objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text(csv_text, encoding="utf-8")
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            client = _Client(_content(), fail_update=True)
            contexts = remote_preflight(plan, client)
            with self.assertRaises(AdminTransportError):
                execute_import(plan, client, _Report(), contexts)
            self.assertEqual(plan.rows[0].status, "UNKNOWN")
            self.assertEqual(plan.rows[1].status, "SKIPPED_DUPLICATE")

    def test_changed_source_csv_blocks_all_writes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "audio_stories.csv"
            csv_path.write_text("language,id,name,image_url\ntr,1,Story One,one.jpg\n", encoding="utf-8")
            objects = {"cover_images/one.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
            with patch("urllib.request.urlopen", side_effect=_storage(objects)):
                plan = build_audio_story_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket", cache_directory=root / "cache")
            csv_path.write_text("language,id,name,image_url\ntr,1,Changed,one.jpg\n", encoding="utf-8")
            client = _Client(_content())
            with self.assertRaises(StoryValidationError):
                execute_import(plan, client, _Report(), remote_preflight(plan, client))
            self.assertNotIn("update-localization", client.calls)


if __name__ == "__main__":
    unittest.main()
