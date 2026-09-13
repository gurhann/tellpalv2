from __future__ import annotations

import io
import json
import os
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

SCRIPT_DIR = Path(__file__).resolve().parents[1]
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from meditation_import_workflow import (
    MeditationRemotePreflightError,
    execute_import,
    format_preview,
    remote_preflight,
)
import import_meditations
from meditation_manifest import (
    StoryValidationError,
    _read_docx_body,
    _read_rows,
    assert_source_unchanged,
    build_meditation_plan,
    normalize_cover_stem,
)


class _Response:
    def __init__(self, payload: bytes):
        self.stream = io.BytesIO(payload)
        self.headers = {"Content-Length": str(len(payload))}

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self, size: int = -1) -> bytes:
        return self.stream.read(size)


class _Report:
    def mark_running(self):
        pass

    def set_phase(self, _phase):
        pass

    def add_content_id(self, _content_id):
        pass

    def record_step(self, _step):
        pass

    def record_asset(self, **_asset):
        pass

    def mark_success(self, _summary):
        pass


class _Client:
    def __init__(self):
        self.calls: list[str] = []
        self.content: dict[str, object] = {
            "contentId": 1,
            "type": "MEDITATION",
            "localizations": [],
        }
        self.assets: dict[int, dict[str, object]] = {}

    def create_content(self, body):
        self.calls.append("create")
        self.content.update(body)
        return self.content

    def upload_media(self, path, kind, checksum):
        self.calls.append("media")
        asset_id = len(self.assets) + 1
        payload = {
            "assetId": asset_id,
            "byteSize": Path(path).stat().st_size,
            "checksumSha256": checksum,
            "kind": kind,
        }
        self.assets[asset_id] = payload
        return payload

    def update_content(self, _content_id, body):
        self.calls.append("cover")
        self.content.update(body)
        return self.content

    def create_localization(self, _content_id, language_code, body):
        self.calls.append("localization")
        localization = {"languageCode": language_code, **body}
        self.content["localizations"].append(localization)
        return localization

    def get_content(self, _content_id):
        return self.content

    def get_media(self, asset_id):
        return self.assets[asset_id]

    def publish_localization(self, _content_id, language_code):
        self.calls.append("publish")
        for item in self.content["localizations"]:
            if item["languageCode"] == language_code:
                item["status"] = "PUBLISHED"
        return item


class _ExistingContentClient:
    def __init__(self, external_key: str):
        self.external_key = external_key
        self.calls: list[str] = []

    def list_contents(self):
        self.calls.append("list")
        return [{"contentId": 99, "externalKey": self.external_key}]


def _mp3(seed: int = 0) -> bytes:
    frame = b"\xff\xfb\x90\x64" + bytes([seed]) * (417 - 4)
    return frame * 2


def _zip(payload: bytes, name: str = "track.mp3") -> bytes:
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(name, payload)
    return stream.getvalue()


def _csv(rows: str) -> Path:
    root = Path(tempfile.mkdtemp(prefix="meditation-test-"))
    path = root / "meditations.csv"
    path.write_text(
        "language,id,name,summary,image_url,summary_image_url\n" + rows,
        encoding="utf-8",
    )
    return path


def _storage(objects: dict[str, bytes]):
    def open_url(url: str, timeout: float):
        object_name = url.split("/", 4)[-1]
        return _Response(objects[object_name])

    return open_url


class MeditationManifestTest(unittest.TestCase):
    def test_supplied_csv_groups_by_stable_stem_and_lists_members(self):
        csv_path = Path(__file__).resolve().parents[5] / "cms" / "yuklenecek_hikayeler" / "meditations" / "meditations.csv"
        objects: dict[str, bytes] = {}
        rows = _read_rows(csv_path)
        for row in rows:
            objects[f"cover_images/{row['image_url']}"] = b"\xff\xd8\xff\xe0"
            objects[f"{row['legacy_id']}.zip"] = _zip(_mp3(int(row["legacy_id"]) % 255))
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
            )
        self.assertEqual(len(plan.groups), 5)
        self.assertEqual(sum(len(group.localizations) for group in plan.groups), 18)
        self.assertEqual(
            {item.legacy_id for group in plan.groups for item in group.localizations},
            {108, 124, 125, 126, 127, 142, 143, 190, 210, 215, 449, 450, 451, 452, 453, 528, 529, 530},
        )
        self.assertEqual(plan.expected_actions["contents"], 5)
        self.assertEqual(plan.expected_actions["localizations"], 18)
        self.assertTrue(plan.missing_body_sources)
        self.assertTrue(any("summary_image_url" in warning for warning in plan.warnings))

    def test_body_sources_allow_live_plan_and_are_sent_as_description_and_body(self):
        csv_path = _csv(
            "tr,1,Titel,Summary,one_kapak.jpg,one_kapak.jpg\n"
            "en,2,Title,Description,one_kapak.jpg,one_kapak.jpg\n"
        )
        objects = {
            "cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0",
            "1.zip": _zip(_mp3()),
            "2.zip": _zip(_mp3(1)),
        }
        root = csv_path.parent / "body"
        root.mkdir()
        (root / "one" ).mkdir()
        (root / "one" / "tr.txt").write_text("Türkçe gövde", encoding="utf-8")
        (root / "one" / "en.txt").write_text("English body", encoding="utf-8")
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                body_directory=root,
            )
        self.assertFalse(plan.missing_body_sources)
        client = _Client()
        summary = execute_import(plan, client, _Report())
        self.assertEqual(summary["groups"], 1)
        self.assertEqual(client.calls, ["create", "media", "cover", "media", "localization", "media", "localization", "publish", "publish"])
        self.assertEqual(client.content["listeningCoverMediaId"], 1)
        localizations = {item["languageCode"]: item for item in client.content["localizations"]}
        self.assertEqual(localizations["tr"]["bodyText"], "Türkçe gövde")
        self.assertEqual(localizations["en"]["description"], "Description")
        self.assertEqual(localizations["tr"]["processingStatus"], "PENDING")

    def test_missing_body_is_warning_and_blocks_workflow_before_client_calls(self):
        csv_path = _csv("tr,1,Titel,Summary,one_kapak.jpg,one_kapak.jpg\n")
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(csv_path, storage_base_url="https://storage.test", storage_bucket="bucket")
        self.assertEqual(plan.missing_body_sources, ("meditation.one/tr",))
        remote_client = _ExistingContentClient(plan.groups[0].external_key)
        with self.assertRaisesRegex(MeditationRemotePreflightError, "before live import"):
            remote_preflight(plan, remote_client)
        self.assertEqual(remote_client.calls, [])
        with self.assertRaisesRegex(RuntimeError, "Body text is required"):
            execute_import(plan, _Client(), _Report())

    def test_allow_missing_body_stages_draft_pending_without_publication(self):
        csv_path = _csv("tr,1,Titel,Summary,one_kapak.jpg,one_kapak.jpg\n")
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                allow_missing_body=True,
                publish=False,
            )

        self.assertTrue(plan.live_import_available)
        self.assertTrue(plan.staged_import)
        self.assertEqual(plan.expected_actions["publications"], 0)
        preview = format_preview(plan)
        self.assertIn("STAGED (DRAFT/PENDING; publication disabled)", preview)
        self.assertIn("Missing body localizations: meditation.one/tr", preview)
        self.assertIn("Body text is intentionally staged and must be supplied before publication", preview)
        self.assertNotIn("Live import unavailable until body text is supplied", preview)

        client = _Client()
        summary = execute_import(plan, client, _Report())

        self.assertTrue(summary["stagedImport"])
        self.assertEqual(summary["missingBodySources"], ["meditation.one/tr"])
        self.assertNotIn("publish", client.calls)
        localization = client.content["localizations"][0]
        self.assertIsNone(localization["bodyText"])
        self.assertEqual(localization["status"], "DRAFT")
        self.assertEqual(localization["processingStatus"], "PENDING")

    def test_allow_missing_body_requires_no_publish(self):
        csv_path = _csv("tr,1,Titel,Summary,one_kapak.jpg,one_kapak.jpg\n")
        with self.assertRaisesRegex(StoryValidationError, "allow-missing-body requires --no-publish"):
            build_meditation_plan(csv_path, allow_missing_body=True)

    def test_duplicate_language_and_ambiguous_stem_are_precise(self):
        duplicate = _csv(
            "tr,1,A,Summary,one_kapak.jpg,one_kapak.jpg\n"
            "tr,2,B,Summary,one_kapak.jpg,one_kapak.jpg\n"
        )
        with self.assertRaisesRegex(StoryValidationError, "Duplicate language.*lines 2, 3"):
            build_meditation_plan(duplicate)

        ambiguous = _csv(
            "tr,1,A,Summary,one_kapak.jpg,one_kapak.jpg\n"
            "en,2,B,Summary,one_cover.jpg,one_cover.jpg\n"
        )
        with self.assertRaisesRegex(StoryValidationError, "Ambiguous normalized group stem 'one'.*lines 2, 3"):
            build_meditation_plan(ambiguous)

    def test_invalid_zip_mp3_and_unsafe_object_fail_without_plan(self):
        unsafe = _csv("tr,1,A,Summary,../one_kapak.jpg,one_kapak.jpg\n")
        with self.assertRaisesRegex(StoryValidationError, "safe object filename"):
            build_meditation_plan(unsafe)

        invalid_zip = _csv("tr,1,A,Summary,one_kapak.jpg,one_kapak.jpg\n")
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": b"not-zip"}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            with self.assertRaisesRegex(StoryValidationError, "1.zip is not a valid ZIP"):
                build_meditation_plan(invalid_zip, storage_base_url="https://storage.test", storage_bucket="bucket")

        invalid_mp3 = _csv("tr,1,A,Summary,one_kapak.jpg,one_kapak.jpg\n")
        objects["1.zip"] = _zip(b"not-mp3")
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            with self.assertRaisesRegex(StoryValidationError, "valid MP3 frame"):
                build_meditation_plan(invalid_mp3, storage_base_url="https://storage.test", storage_bucket="bucket")

    def test_normalized_stem_removes_legacy_suffixes(self):
        self.assertEqual(normalize_cover_stem("Best_Friend_Kapak.jpg"), "best_friend")
        self.assertEqual(normalize_cover_stem("best_friend_cover.png"), "best_friend")
        with self.assertRaisesRegex(StoryValidationError, "empty or ambiguous"):
            normalize_cover_stem("cover.jpg")

    def test_read_rows_normalizes_headers_and_rejects_extra_dictreader_fields(self):
        root = Path(tempfile.mkdtemp(prefix="meditation-csv-"))
        normalized = root / "normalized.csv"
        normalized.write_text(
            " Language , ID , Name , Summary , Image_URL , Summary_Image_URL\n"
            "tr,1,Title,Description,one_kapak.jpg,legacy-url\n",
            encoding="utf-8",
        )
        row = _read_rows(normalized)[0]
        self.assertEqual(row["language"], "tr")
        self.assertEqual(row["legacy_id"], 1)
        self.assertEqual(row["summary_image_url"], "legacy-url")

        extra = root / "extra.csv"
        extra.write_text(
            "language,id,name,summary,image_url,summary_image_url\n"
            "tr,1,Title,Description,one_kapak.jpg,one_kapak.jpg,unexpected\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(StoryValidationError, "extra CSV fields"):
            _read_rows(extra)

    def test_missing_turkish_and_invalid_cover_are_rejected_with_context(self):
        missing_turkish = _csv("en,1,Title,Description,one_kapak.jpg,one_kapak.jpg\n")
        with self.assertRaisesRegex(StoryValidationError, "group 'one'.*requires a Turkish row"):
            build_meditation_plan(missing_turkish)

        invalid_cover = _csv("tr,1,Title,Description,one_kapak.jpg,https://legacy/cover\n")
        objects = {"cover_images/one_kapak.jpg": b"not-an-image", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            with self.assertRaisesRegex(StoryValidationError, "Group 'one'.*signature"):
                build_meditation_plan(
                    invalid_cover,
                    storage_base_url="https://storage.test",
                    storage_bucket="bucket",
                )

    def test_remote_external_key_conflict_happens_before_any_write(self):
        csv_path = _csv("tr,1,Title,Description,one_kapak.jpg,one_kapak.jpg\n")
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                body_sources={("one", "tr"): "Body"},
            )
        client = _ExistingContentClient(plan.groups[0].external_key)
        with self.assertRaisesRegex(MeditationRemotePreflightError, "already exists"):
            remote_preflight(plan, client)
        self.assertEqual(client.calls, ["list"])

    def test_staged_remote_external_key_conflict_happens_before_any_write(self):
        csv_path = _csv("tr,1,Title,Description,one_kapak.jpg,one_kapak.jpg\n")
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                allow_missing_body=True,
                publish=False,
            )
        client = _ExistingContentClient(plan.groups[0].external_key)
        with self.assertRaisesRegex(MeditationRemotePreflightError, "already exists"):
            remote_preflight(plan, client)
        self.assertEqual(client.calls, ["list"])

    def test_source_fingerprint_preview_and_inactive_no_publish(self):
        csv_path = _csv("tr,1,Title,Description,one_kapak.jpg,one_kapak.jpg\n")
        body_path = csv_path.parent / "one.tr.md"
        body_path.write_text("Body", encoding="utf-8")
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                active=False,
                publish=False,
                body_directory=csv_path.parent,
            )
        preview = format_preview(plan)
        self.assertIn("Groups: 1", preview)
        self.assertIn("id=1", preview)
        self.assertNotIn("Live import: unavailable", preview)
        body_path.write_text("Changed", encoding="utf-8")
        with self.assertRaisesRegex(StoryValidationError, "changed after preview"):
            assert_source_unchanged(plan)

        body_path.write_text("Body", encoding="utf-8")
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            plan = build_meditation_plan(
                csv_path,
                storage_base_url="https://storage.test",
                storage_bucket="bucket",
                active=False,
                publish=False,
                body_directory=csv_path.parent,
            )
        client = _Client()
        summary = execute_import(plan, client, _Report())
        self.assertEqual(summary["publishedLanguages"], [])
        self.assertNotIn("publish", client.calls)
        self.assertFalse(client.content["active"])

    def test_body_directory_ignores_readme_and_docx_preserves_inline_marks(self):
        root = Path(tempfile.mkdtemp(prefix="meditation-body-"))
        (root / "README.md").write_text("documentation", encoding="utf-8")
        document = root / "one.EN.DOCX"
        xml = (
            b'<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
            b"<w:body><w:p><w:r><w:t>first</w:t><w:tab/><w:t>column</w:t>"
            b"<w:br/><w:t>line</w:t><w:cr/><w:t>end</w:t></w:r></w:p></w:body></w:document>"
        )
        with zipfile.ZipFile(document, "w") as archive:
            archive.writestr("word/document.xml", xml)
        self.assertEqual(_read_docx_body(document), "first\tcolumn\nline\nend")

        broken = root / "broken.docx"
        with zipfile.ZipFile(broken, "w"):
            pass
        with self.assertRaisesRegex(StoryValidationError, "Cannot read body source"):
            from meditation_manifest import _load_body_source

            _load_body_source(broken, "meditation.one", "en")

    def test_generated_key_and_title_length_limits_include_group_context(self):
        long_stem = "a" * 190 + "_kapak.jpg"
        long_key_csv = _csv(f"tr,1,Title,Description,{long_stem},{long_stem}\n")
        with self.assertRaisesRegex(StoryValidationError, "generated external key exceeds 180"):
            build_meditation_plan(long_key_csv)

        long_title = _csv(
            f"tr,1,{'T' * 256},Description,one_kapak.jpg,one_kapak.jpg\n"
        )
        objects = {"cover_images/one_kapak.jpg": b"\xff\xd8\xff\xe0", "1.zip": _zip(_mp3())}
        with patch("urllib.request.urlopen", side_effect=_storage(objects)):
            with self.assertRaisesRegex(StoryValidationError, "Group 'one'.*title exceeds 255"):
                build_meditation_plan(
                    long_title,
                    storage_base_url="https://storage.test",
                    storage_bucket="bucket",
                )


class MeditationImportCliTest(unittest.TestCase):
    def test_confirmation_is_case_insensitive_but_still_exact(self):
        for entered, expected_status, expected_execute_calls in (
            ("IMPORT", 0, 1),
            (" import", 1, 0),
        ):
            with self.subTest(entered=entered):
                report = MagicMock(result_path=Path("report.json"))
                client = MagicMock(last_request={"method": "GET", "path": "/api/admin/contents"})
                execute = MagicMock(return_value={"groups": 1, "contentIds": [1]})
                terminal = MagicMock()
                terminal.isatty.return_value = True
                with (
                    patch.dict(
                        os.environ,
                        {"TELLPAL_API_BASE_URL": "https://api.test", "TELLPAL_ADMIN_USERNAME": "admin"},
                        clear=True,
                    ),
                    patch.object(sys, "argv", ["import_meditations.py", "meditations.csv"]),
                    patch.object(sys, "stdin", terminal),
                    patch.object(
                        import_meditations,
                        "build_meditation_plan",
                        return_value=SimpleNamespace(missing_body_sources=()),
                    ),
                    patch.object(import_meditations, "MeditationImportRunReport", return_value=report),
                    patch.object(import_meditations, "TellPalAdminClient", return_value=client),
                    patch.object(import_meditations, "remote_preflight", return_value=()),
                    patch.object(import_meditations, "format_preview", return_value="preview"),
                    patch.object(import_meditations.getpass, "getpass", return_value="secret"),
                    patch("builtins.input", return_value=entered),
                    patch.object(import_meditations, "execute_import", execute),
                ):
                    self.assertEqual(import_meditations.main(), expected_status)
                self.assertEqual(execute.call_count, expected_execute_calls)
                if expected_status:
                    report.mark_cancelled.assert_called_once_with()

    def test_staged_option_allows_login_and_confirmation_after_body_preflight(self):
        report = MagicMock(result_path=Path("report.json"))
        client = MagicMock(last_request={"method": "GET", "path": "/api/admin/contents"})
        terminal = MagicMock()
        terminal.isatty.return_value = True
        plan = SimpleNamespace(
            missing_body_sources=("meditation.one/tr",),
            allow_missing_body=True,
            publish=False,
        )
        with (
            patch.dict(
                os.environ,
                {"TELLPAL_API_BASE_URL": "https://api.test", "TELLPAL_ADMIN_USERNAME": "admin"},
                clear=True,
            ),
            patch.object(
                sys,
                "argv",
                ["import_meditations.py", "meditations.csv", "--allow-missing-body", "--no-publish"],
            ),
            patch.object(sys, "stdin", terminal),
            patch.object(import_meditations, "build_meditation_plan", return_value=plan) as build_plan,
            patch.object(import_meditations, "MeditationImportRunReport", return_value=report),
            patch.object(import_meditations, "TellPalAdminClient", return_value=client),
            patch.object(import_meditations, "remote_preflight", return_value=()),
            patch.object(import_meditations, "format_preview", return_value="preview"),
            patch.object(import_meditations.getpass, "getpass", return_value="secret"),
            patch("builtins.input", return_value="import"),
            patch.object(import_meditations, "execute_import", return_value={"groups": 1, "contentIds": [1]}),
        ):
            self.assertEqual(import_meditations.main(), 0)
        self.assertTrue(build_plan.call_args.kwargs["allow_missing_body"])
        self.assertFalse(build_plan.call_args.kwargs["publish"])
        client.login.assert_called_once_with("admin", "secret")

    def test_missing_body_main_gate_stops_before_client_creation(self):
        report = MagicMock(result_path=Path("report.json"))
        plan = SimpleNamespace(
            missing_body_sources=("meditation.one/tr",),
            allow_missing_body=False,
            publish=True,
        )
        terminal = MagicMock()
        terminal.isatty.return_value = True
        with (
            patch.dict(
                os.environ,
                {"TELLPAL_API_BASE_URL": "https://api.test", "TELLPAL_ADMIN_USERNAME": "admin"},
                clear=True,
            ),
            patch.object(sys, "argv", ["import_meditations.py", "meditations.csv"]),
            patch.object(sys, "stdin", terminal),
            patch.object(import_meditations, "build_meditation_plan", return_value=plan) as build_plan,
            patch.object(import_meditations, "MeditationImportRunReport", return_value=report),
            patch.object(import_meditations, "TellPalAdminClient") as client_factory,
        ):
            self.assertEqual(import_meditations.main(), 2)
        self.assertFalse(build_plan.call_args.kwargs["allow_missing_body"])
        self.assertTrue(build_plan.call_args.kwargs["publish"])
        client_factory.assert_not_called()


if __name__ == "__main__":
    unittest.main()
