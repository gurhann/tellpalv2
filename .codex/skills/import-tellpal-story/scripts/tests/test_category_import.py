from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError
from urllib.parse import quote

SCRIPT_DIR = Path(__file__).resolve().parents[1]
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from category_import_workflow import CategoryImportError, execute_import, remote_preflight
from category_import_report import CategoryImportRunReport
from category_manifest import CategoryValidationError, build_category_plan, verify_source_images
from tellpal_admin_client import AdminApiError, TellPalAdminClient


class _Report:
    def __init__(self):
        self.assets = []
        self.categories = []
        self.localizations = []
        self.steps = []

    def mark_running(self):
        pass

    def record_asset(self, *arguments):
        self.assets.append(arguments)

    def record_category(self, *arguments):
        self.categories.append(arguments)

    def record_localization(self, *arguments):
        self.localizations.append(arguments)

    def record_step(self, step):
        self.steps.append(step)

    def mark_success(self, summary):
        self.summary = summary

    def mark_failure(self, exception):
        self.failure = exception


class _Client:
    def __init__(self):
        self.categories = {}
        self.localizations = {}
        self.assets = {}
        self.calls = []
        self.next_category_id = 100
        self.next_asset_id = 500

    def list_categories(self):
        return list(self.categories.values())

    def create_category(self, body):
        self.calls.append(("create_category", body))
        category = {
            "categoryId": self.next_category_id,
            "type": body["type"],
            "slug": body["slug"],
            "premium": body["premium"],
            "active": body["active"],
        }
        self.next_category_id += 1
        self.categories[body["slug"]] = category
        self.localizations[category["categoryId"]] = []
        return category

    def list_category_localizations(self, category_id):
        return list(self.localizations.get(category_id, []))

    def create_category_localization(self, category_id, language_code, body):
        self.calls.append(("create_localization", category_id, language_code, body))
        localization = {
            "categoryId": category_id,
            "languageCode": language_code,
            **body,
            "published": False,
        }
        self.localizations[category_id].append(localization)
        return localization

    def register_media_asset(self, *, provider, object_path, kind, mime_type):
        self.calls.append(("register_media", provider, object_path, kind, mime_type))
        asset = {
            "assetId": self.next_asset_id,
            "provider": provider,
            "objectPath": object_path,
            "mediaType": "IMAGE",
            "kind": kind,
            "mimeType": mime_type,
        }
        self.assets[self.next_asset_id] = asset
        self.next_asset_id += 1
        return asset

    def get_media(self, asset_id):
        return self.assets[asset_id]


class CategoryImportTest(unittest.TestCase):
    def test_approved_mapping_has_33_groups_and_105_source_ids(self):
        mapping_path = SCRIPT_DIR.parent / "references" / "category_import_mapping.json"
        payload = json.loads(mapping_path.read_text(encoding="utf-8"))
        groups = payload["groups"]
        source_ids = [source_id for group in groups for source_id in group["sourceIds"]]
        self.assertEqual(len(groups), 33)
        self.assertEqual(len(source_ids), 105)
        self.assertEqual(len(source_ids), len(set(source_ids)))

    def test_full_approved_plan_executes_33_groups_and_105_localizations(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            mapping = json.loads(
                (SCRIPT_DIR.parent / "references" / "category_import_mapping.json").read_text(encoding="utf-8")
            )
            rows = []
            languages = ("tr", "en", "pt", "de")
            for group in mapping["groups"]:
                source_type = "AUDIO_STORY" if group["slug"] == "audio-books" else group["canonicalType"]
                for index, source_id in enumerate(group["sourceIds"]):
                    rows.append(
                        (
                            source_id,
                            f"Category {source_id}",
                            languages[index],
                            source_type,
                            f"{group['slug']}.png",
                        )
                    )
            csv_path, mapping_path = _fixture_files(root, rows, mapping["groups"])
            plan = build_category_plan(csv_path, mapping_path=mapping_path)
            client = _Client()
            summary = execute_import(plan, client, _Report(), remote_preflight(plan, client))

        self.assertEqual(summary["groups"], 33)
        self.assertEqual(summary["createdLocalizations"], 105)
        self.assertEqual(len(client.categories), 33)
        self.assertEqual(sum(len(items) for items in client.localizations.values()), 105)

    def test_manifest_maps_audio_story_to_story_and_redacts_signed_url(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(Path(directory), [
                (29, "Sesli Kitaplar", "tr", "AUDIO_STORY", "image.png"),
                (2, "Animal Friends", "de", "STORY", "image.png"),
            ], [
                {"slug": "audio-books", "canonicalType": "STORY", "sourceIds": [29]},
                {"slug": "animal-friends", "canonicalType": "STORY", "sourceIds": [2]},
            ])
            plan = build_category_plan(csv_path, mapping_path=mapping_path)

        self.assertEqual(plan.groups[0].canonical_type, "STORY")
        self.assertEqual(plan.groups[0].localizations[0].source_type, "AUDIO_STORY")
        serialized = json.dumps(plan.to_dict(), ensure_ascii=False)
        self.assertNotIn("token=secret", serialized)
        self.assertNotIn("https://", serialized)

    def test_unapproved_audio_story_id_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(
                Path(directory),
                [(999, "Audio Books", "tr", "AUDIO_STORY", "image.png")],
                [{"slug": "audio-books", "canonicalType": "STORY", "sourceIds": [999]}],
            )
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_firebase_image_url_must_use_storage_download_shape(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "categories.csv"
            csv_path.write_text(
                "id,name,description,language,type,image_url\n"
                "1,Stories,Description,tr,STORY,https://firebasestorage.googleapis.com/other/image.png\n",
                encoding="utf-8",
            )
            mapping_path = root / "mapping.json"
            mapping_path.write_text(
                json.dumps({"version": 1, "groups": [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1]}]}),
                encoding="utf-8",
            )
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_missing_source_image_is_rejected_before_mutation(self):
        plan = self._plan()
        missing = HTTPError(
            plan.groups[0].localizations[0].source_image_url,
            404,
            "missing",
            {},
            None,
        )
        with patch("category_manifest.urlopen", side_effect=missing):
            with self.assertRaises(CategoryValidationError):
                verify_source_images(plan, timeout_seconds=1)

    def test_non_firebase_image_host_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "categories.csv"
            csv_path.write_text(
                "id,name,description,language,type,image_url\n"
                "1,Stories,Description,tr,STORY,https://example.test/v0/b/test/o/image.png\n",
                encoding="utf-8",
            )
            mapping_path = root / "mapping.json"
            mapping_path.write_text(
                json.dumps({"version": 1, "groups": [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1]}]}),
                encoding="utf-8",
            )
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_unsupported_image_extension_is_rejected_during_plan_build(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(
                Path(directory),
                [(1, "Stories", "tr", "STORY", "image.webp")],
                [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1]}],
            )
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_invalid_source_id_is_rejected_before_remote_work(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(Path(directory), [
                (1, "Stories", "tr", "STORY", "image.png"),
                (2, "Unexpected", "en", "STORY", "image.png"),
            ], [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1]}])
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_boolean_mapping_id_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path, mapping_path = _fixture_files(
                root,
                [(1, "Stories", "tr", "STORY", "image.png")],
                [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [True]}],
            )
            with self.assertRaises(CategoryValidationError):
                build_category_plan(csv_path, mapping_path=mapping_path)

    def test_extra_remote_localization_is_rejected_before_mutation(self):
        plan = self._plan()
        client = _Client()
        client.categories["stories"] = {
            "categoryId": 7,
            "type": "STORY",
            "slug": "stories",
            "premium": False,
            "active": True,
        }
        client.localizations[7] = [
            {"categoryId": 7, "languageCode": "tr", "name": "Stories", "description": "Description", "status": "DRAFT", "publishedAt": None, "imageMediaId": 1},
            {"categoryId": 7, "languageCode": "en", "name": "Stories", "description": "Description", "status": "DRAFT", "publishedAt": None, "imageMediaId": 1},
            {"categoryId": 7, "languageCode": "de", "name": "Stories", "description": "Description", "status": "DRAFT", "publishedAt": None, "imageMediaId": 1},
        ]
        with self.assertRaises(CategoryImportError):
            remote_preflight(plan, client)
        self.assertEqual(client.calls, [])

    def test_missing_remote_localization_is_planned_for_creation(self):
        plan = self._plan()
        client = _Client()
        client.categories["stories"] = {
            "categoryId": 7,
            "type": "STORY",
            "slug": "stories",
            "premium": False,
            "active": True,
        }
        image_path = plan.groups[0].localizations[0].image_object_path
        client.assets[1] = {
            "assetId": 1,
            "objectPath": image_path,
            "mediaType": "IMAGE",
            "provider": "FIREBASE_STORAGE",
            "kind": "ORIGINAL_IMAGE",
        }
        client.localizations[7] = [
            {
                "categoryId": 7,
                "languageCode": "tr",
                "name": "Stories",
                "description": "Description",
                "status": "DRAFT",
                "publishedAt": None,
                "imageMediaId": 1,
            }
        ]
        remote = remote_preflight(plan, client)
        self.assertEqual([item.action for item in remote[0].localizations], ["REUSE", "CREATE"])

    def test_verification_rejects_wrong_image_asset(self):
        plan = self._plan()
        client = _Client()
        remote = remote_preflight(plan, client)
        execute_import(plan, client, _Report(), remote)
        category_id = next(iter(client.categories.values()))["categoryId"]
        client.assets[500]["objectPath"] = "wrong/path.png"
        with self.assertRaises(CategoryImportError):
            from category_import_workflow import _verify_group

            _verify_group(remote[0], category_id, client)

    def test_failure_report_redacts_urls_and_object_paths(self):
        plan = self._plan()
        with tempfile.TemporaryDirectory() as directory:
            report = CategoryImportRunReport(plan, "https://api.test", storage_root=Path(directory))
            report.mark_failure(
                RuntimeError(
                    "POST category_images/aile ve arkadaşlık.png?token=secret "
                    "https://firebasestorage.googleapis.com/v0/b/test/o/x.png?token=secret"
                )
            )
            result = json.loads(report.result_path.read_text(encoding="utf-8"))
        message = result["error"]["message"]
        self.assertNotIn("category_images/", message)
        self.assertNotIn("token=secret", message)
        self.assertNotIn("https://firebasestorage", message)

    def test_existing_type_conflict_does_not_mutate(self):
        plan = self._plan()
        client = _Client()
        client.categories["stories"] = {
            "categoryId": 7,
            "type": "MEDITATION",
            "slug": "stories",
            "premium": False,
            "active": True,
        }
        client.localizations[7] = []
        with self.assertRaises(CategoryImportError):
            remote_preflight(plan, client)
        self.assertEqual(client.calls, [])

    def test_image_registration_failure_happens_before_category_mutation(self):
        plan = self._plan()
        client = _Client()
        client.register_media_asset = MagicMock(
            side_effect=AdminApiError("POST", "/api/admin/media", 409, {"title": "conflict"})
        )
        report = _Report()
        remote = remote_preflight(plan, client)
        with self.assertRaises(AdminApiError):
            execute_import(plan, client, report, remote)
        self.assertEqual(client.categories, {})
        self.assertEqual(client.localizations, {})
        self.assertIsInstance(report.failure, AdminApiError)

    def test_rerun_reuses_categories_localizations_and_assets(self):
        plan = self._plan()
        client = _Client()
        first_report = _Report()
        execute_import(plan, client, first_report, remote_preflight(plan, client))
        first_call_count = len(client.calls)

        second_report = _Report()
        second_remote = remote_preflight(plan, client)
        self.assertEqual(second_remote[0].category_action, "REUSE")
        self.assertTrue(all(item.action == "REUSE" for item in second_remote[0].localizations))
        execute_import(plan, client, second_report, second_remote)

        self.assertEqual(len(client.categories), 1)
        self.assertEqual(len(client.localizations[100]), 2)
        self.assertEqual(len(client.assets), 1)
        self.assertEqual(len(client.calls), first_call_count)

    def test_admin_client_exposes_category_and_media_contracts(self):
        client = TellPalAdminClient("https://api.test")
        client._request_json = MagicMock(side_effect=[[], {"categoryId": 7}, [], {"assetId": 9}])
        self.assertEqual(client.list_categories(), [])
        self.assertEqual(client.create_category({"slug": "stories"})["categoryId"], 7)
        self.assertEqual(client.list_category_localizations(7), [])
        self.assertEqual(client.register_media_asset(
            provider="FIREBASE_STORAGE",
            object_path="category_images/image.png",
            kind="ORIGINAL_IMAGE",
            mime_type="image/png",
        )["assetId"], 9)
        calls = client._request_json.call_args_list
        self.assertEqual(calls[0].args[:2], ("GET", "/api/admin/categories"))
        self.assertEqual(calls[1].args[:2], ("POST", "/api/admin/categories"))
        self.assertEqual(calls[2].args[:2], ("GET", "/api/admin/categories/7/localizations"))
        self.assertEqual(calls[3].args[:2], ("POST", "/api/admin/media"))

    def _plan(self):
        with tempfile.TemporaryDirectory() as directory:
            csv_path, mapping_path = _fixture_files(Path(directory), [
                (1, "Stories", "tr", "STORY", "image.png"),
                (2, "Stories", "en", "STORY", "image.png"),
            ], [{"slug": "stories", "canonicalType": "STORY", "sourceIds": [1, 2]}])
            # The plan keeps the source path and fingerprint, so copy the fixture to a stable temp file.
            stable_directory = Path(tempfile.mkdtemp(prefix="tellpal-category-plan-"))
            stable_csv = stable_directory / "categories.csv"
            stable_mapping = stable_directory / "mapping.json"
            stable_csv.write_bytes(Path(csv_path).read_bytes())
            stable_mapping.write_bytes(Path(mapping_path).read_bytes())
        return build_category_plan(stable_csv, mapping_path=stable_mapping)


def _fixture_files(directory: Path, rows, groups):
    csv_path = directory / "categories.csv"
    csv_path.write_text(
        "id,name,description,language,type,image_url\n"
        + "\n".join(
            f'{source_id},{name},Description,{language},{source_type},'
            f'"https://firebasestorage.googleapis.com/v0/b/test/o/category_images%2F{quote(image)}?alt=media&token=secret"'
            for source_id, name, language, source_type, image in rows
        )
        + "\n",
        encoding="utf-8",
    )
    mapping_path = directory / "mapping.json"
    mapping_path.write_text(json.dumps({"version": 1, "groups": groups}), encoding="utf-8")
    return csv_path, mapping_path


if __name__ == "__main__":
    unittest.main()
