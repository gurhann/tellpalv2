from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

SCRIPT_DIR = Path(__file__).resolve().parents[1]
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from category_story_mapping_manifest import (  # noqa: E402
    CategoryStoryMappingValidationError,
    build_category_story_mapping_plan,
)
from category_story_mapping_report import CategoryStoryMappingRunReport  # noqa: E402
from category_story_mapping_workflow import (  # noqa: E402
    CategoryStoryMappingImportError,
    execute_mapping_import,
    remote_preflight,
)
from tellpal_admin_client import AdminApiError, AdminTransportError, TellPalAdminClient  # noqa: E402


class FakeAdminClient:
    def __init__(self, *, category_type="STORY", category_status="DRAFT", contents=None, current=None):
        self.categories = [{"categoryId": 7, "type": category_type, "slug": "stories", "active": True}]
        self.category_localizations = {
            7: [
                {
                    "categoryId": 7,
                    "languageCode": "tr",
                    "name": "Hikayeler",
                    "description": "",
                    "imageMediaId": None,
                    "status": category_status,
                    "publishedAt": None,
                    "published": category_status == "PUBLISHED",
                }
            ]
        }
        self.contents = contents or []
        self.curation = {(7, "tr"): list(current or [])}
        self.updated_localizations = []
        self.added_links = []
        self.events = []

    def list_categories(self):
        return self.categories

    def list_category_localizations(self, category_id):
        return self.category_localizations[category_id]

    def list_contents(self):
        return self.contents

    def list_category_contents(self, category_id, language_code):
        return list(self.curation[(category_id, language_code)])

    def update_category_localization(self, category_id, language_code, body):
        self.events.append(("publish", category_id, language_code, body))
        self.updated_localizations.append((category_id, language_code, body))
        self.category_localizations[category_id][0].update(body)
        self.category_localizations[category_id][0]["published"] = body["status"] == "PUBLISHED"
        return self.category_localizations[category_id][0]

    def add_category_content(self, category_id, language_code, body):
        self.events.append(("add", category_id, language_code, body))
        self.added_links.append((category_id, language_code, body))
        response = {
            "categoryId": category_id,
            "languageCode": language_code,
            "contentId": body["contentId"],
            "displayOrder": body["displayOrder"],
        }
        self.curation[(category_id, language_code)].append(response)
        return response


class FakeReport:
    def __init__(self):
        self.published = []
        self.links = []
        self.steps = []
        self.summary = None
        self.skipped = []

    def mark_running(self):
        pass

    def record_category_localization(self, *args):
        self.published.append(args)

    def record_link(self, assignment, action):
        self.links.append((assignment.content_id, action))

    def record_step(self, step):
        self.steps.append(step)

    def mark_success(self, summary):
        self.summary = summary

    def record_skip(self, assignment, exception):
        self.skipped.append((assignment.content_id, str(exception)))


def content(content_id, title, *, status="PUBLISHED", active=True, content_type="STORY"):
    return {
        "contentId": content_id,
        "type": content_type,
        "active": active,
        "localizations": [
            {
                "contentId": content_id,
                "languageCode": "tr",
                "title": title,
                "status": status,
            }
        ],
    }


class CategoryMappingManifestTests(unittest.TestCase):
    def test_positional_header_multiline_title_duplicates_and_audio_canonicalization(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "mapping.csv"
            path.write_text(
                "language,type,name,name\n"
                'tr,STORY,Hikayeler,"Bir\nÖykü"\n'
                'tr,STORY,Hikayeler,"Bir\nÖykü"\n'
                "tr,AUDIO_STORY,Hikayeler,Bir Öykü\n",
                encoding="utf-8",
            )
            plan = build_category_story_mapping_plan(path)

        self.assertEqual(plan.logical_row_count, 3)
        self.assertEqual(plan.unique_assignment_count, 2)
        self.assertEqual(plan.duplicate_count, 1)
        self.assertEqual(plan.rows[0].canonical_type, "STORY")
        self.assertEqual(plan.rows[1].source_type, "AUDIO_STORY")
        self.assertEqual(plan.rows[1].canonical_type, "STORY")

    def test_same_title_in_multiple_categories_is_reported_as_many_to_many_risk(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "mapping.csv"
            path.write_text(
                "language,type,name,name\n"
                "tr,STORY,Hikayeler,Bir Öykü\n"
                "tr,STORY,Uykudan Önce,Bir Öykü\n",
                encoding="utf-8",
            )
            plan = build_category_story_mapping_plan(path)

        self.assertEqual(plan.content_candidate_risk_count, 1)
        self.assertEqual(plan.ambiguity_risks[0].category_names, ("Hikayeler", "Uykudan Önce"))

    def test_duplicate_header_is_positional_and_wrong_header_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "mapping.csv"
            path.write_text("language,type,category,story\n", encoding="utf-8")
            with self.assertRaises(CategoryStoryMappingValidationError):
                build_category_story_mapping_plan(path)


class CategoryMappingWorkflowTests(unittest.TestCase):
    def _plan(self, *rows):
        with tempfile.NamedTemporaryFile("w", suffix=".csv", encoding="utf-8", delete=False) as stream:
            stream.write("language,type,name,name\n")
            for row in rows:
                stream.write(",".join(row) + "\n")
            path = Path(stream.name)
        self.addCleanup(lambda: path.unlink(missing_ok=True))
        return build_category_story_mapping_plan(path)

    def test_preflight_and_execution_publish_category_and_create_links(self):
        plan = self._plan(
            ("tr", "STORY", "Hikayeler", "Bir Öykü"),
            ("tr", "STORY", "Hikayeler", "İkinci Öykü"),
        )
        client = FakeAdminClient(contents=[content(101, "Bir Öykü"), content(102, "İkinci Öykü")])
        remote = remote_preflight(plan, client)

        self.assertTrue(remote.ready)
        self.assertEqual(remote.publish_count, 1)
        self.assertEqual(remote.create_count, 2)

        report = FakeReport()
        summary = execute_mapping_import(plan, client, report, remote)

        self.assertEqual(summary["createdLinks"], 2)
        self.assertEqual(summary["publishedCategoryLocalizations"], 1)
        self.assertEqual([item[2]["displayOrder"] for item in client.added_links], [0, 1])
        self.assertEqual([event[0] for event in client.events], ["publish", "add", "add"])
        self.assertEqual(client.events[0][3]["status"], "PUBLISHED")
        self.assertTrue(client.events[0][3]["publishedAt"])
        self.assertEqual(report.steps, ["mutations-complete", "verification-complete"])

    def test_existing_exact_links_are_reused_without_mutation(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "Bir Öykü"))
        client = FakeAdminClient(
            category_status="PUBLISHED",
            contents=[content(101, "Bir Öykü")],
            current=[{"contentId": 101, "displayOrder": 0}],
        )
        remote = remote_preflight(plan, client)

        self.assertTrue(remote.ready)
        self.assertEqual(remote.reuse_count, 1)
        self.assertEqual(remote.create_count, 0)
        self.assertEqual(client.added_links, [])

    def test_invalid_rows_are_skipped_while_valid_rows_in_same_lane_are_created(self):
        plan = self._plan(
            ("tr", "STORY", "Hikayeler", "Missing"),
            ("tr", "STORY", "Hikayeler", "Draft"),
            ("tr", "STORY", "Hikayeler", "Valid"),
            ("tr", "STORY", "Hikayeler", "Ambiguous"),
        )
        client = FakeAdminClient(contents=[content(101, "Draft", status="DRAFT"),
            content(102, "Valid"), content(103, "Ambiguous"), content(104, "Ambiguous")])
        remote = remote_preflight(plan, client)
        self.assertTrue(remote.ready)
        self.assertEqual(remote.skipped_count, 3)
        summary = execute_mapping_import(plan, client, FakeReport(), remote)
        self.assertEqual(summary["createdLinks"], 1)
        self.assertEqual(summary["skippedLinks"], 3)
        self.assertEqual(client.added_links[0][2], {"contentId": 102, "displayOrder": 2})
        rerun = remote_preflight(plan, client)
        self.assertEqual(rerun.reuse_count, 1)
        self.assertEqual(rerun.create_count, 0)

    def test_order_conflict_skips_only_affected_row_and_preserves_existing_links(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "Conflicting"),
                          ("tr", "STORY", "Hikayeler", "Valid"))
        client = FakeAdminClient(contents=[content(101, "Conflicting"), content(102, "Valid")],
                                 current=[{"contentId": 999, "displayOrder": 0}])
        summary = execute_mapping_import(plan, client, FakeReport(), remote_preflight(plan, client))
        self.assertEqual(summary["createdLinks"], 1)
        self.assertEqual(summary["skippedLinks"], 1)
        self.assertEqual(client.curation[(7, "tr")][0], {"contentId": 999, "displayOrder": 0})

    def test_definitive_api_rejection_does_not_block_next_assignment(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "Rejected"),
                          ("tr", "STORY", "Hikayeler", "Valid"))
        client = FakeAdminClient(contents=[content(101, "Rejected"), content(102, "Valid")])
        original = client.add_category_content
        def add(category_id, language, body):
            if body["contentId"] == 101:
                raise AdminApiError("POST", "/contents", 409, {"detail": "Rejected"})
            return original(category_id, language, body)
        client.add_category_content = add
        report = FakeReport()
        summary = execute_mapping_import(plan, client, report, remote_preflight(plan, client))
        self.assertEqual(summary["createdLinks"], 1)
        self.assertEqual(summary["skippedLinks"], 1)
        self.assertEqual(report.skipped[0][0], 101)

    def test_uncertain_write_or_auth_failure_stops_without_retry(self):
        for error in (AdminTransportError("POST", "/contents", "timeout"),
                      AdminApiError("POST", "/contents", 401, {}),
                      AdminApiError("POST", "/contents", 500, {})):
            with self.subTest(error=type(error).__name__, message=str(error)):
                plan = self._plan(("tr", "STORY", "Hikayeler", "First"),
                                  ("tr", "STORY", "Hikayeler", "Second"))
                client = FakeAdminClient(contents=[content(101, "First"), content(102, "Second")])
                client.add_category_content = MagicMock(side_effect=error)
                with self.assertRaises(type(error)):
                    execute_mapping_import(plan, client, FakeReport(), remote_preflight(plan, client))
                self.assertEqual(client.add_category_content.call_count, 1)

    def test_missing_category_does_not_block_valid_category(self):
        plan = self._plan(("tr", "STORY", "Absent category", "First"),
                          ("tr", "STORY", "Hikayeler", "Second"))
        client = FakeAdminClient(contents=[content(101, "First"), content(102, "Second")])
        summary = execute_mapping_import(plan, client, FakeReport(), remote_preflight(plan, client))
        self.assertEqual(summary["skippedLinks"], 1)
        self.assertEqual(summary["createdLinks"], 1)

    def test_publication_rejection_skips_lane_and_continues_next_category(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "First"),
                          ("tr", "STORY", "Other", "Second"))
        client = FakeAdminClient(contents=[content(101, "First"), content(102, "Second")])
        client.categories.append({**client.categories[0], "categoryId": 8})
        client.category_localizations[8] = [
            {**client.category_localizations[7][0], "categoryId": 8, "name": "Other"}]
        client.curation[(8, "tr")] = []
        original = client.update_category_localization
        def publish(category_id, language, body):
            if category_id == 7:
                raise AdminApiError("PUT", "/localizations/tr", 409, {})
            return original(category_id, language, body)
        client.update_category_localization = publish
        report = FakeReport()
        summary = execute_mapping_import(plan, client, report, remote_preflight(plan, client))
        self.assertEqual(summary["skippedLinks"], 1)
        self.assertEqual(summary["createdLinks"], 1)
        self.assertEqual(client.added_links[0][0], 8)

    def test_no_valid_rows_never_publishes_an_empty_category(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "Missing"))
        client = FakeAdminClient()
        with self.assertRaisesRegex(CategoryStoryMappingImportError, "no valid assignments"):
            execute_mapping_import(plan, client, FakeReport(), remote_preflight(plan, client))
        self.assertEqual(client.events, [])

    def test_ambiguous_content_blocks_without_writes(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "Bir Öykü"))
        client = FakeAdminClient(contents=[content(101, "Bir Öykü"), content(102, "Bir Öykü")])
        remote = remote_preflight(plan, client)

        self.assertFalse(remote.ready)
        self.assertIn("ambiguous_content_localization", {item.code for item in remote.conflicts})
        self.assertEqual(client.updated_localizations, [])
        self.assertEqual(client.added_links, [])

    def test_existing_order_conflict_blocks_without_reorder(self):
        plan = self._plan(("tr", "STORY", "Hikayeler", "Bir Öykü"))
        client = FakeAdminClient(
            category_status="PUBLISHED",
            contents=[content(101, "Bir Öykü")],
            current=[{"contentId": 101, "displayOrder": 4}],
        )
        remote = remote_preflight(plan, client)

        self.assertFalse(remote.ready)
        self.assertIn("existing_order_conflict", {item.code for item in remote.conflicts})
        self.assertEqual(client.added_links, [])

    def test_unpublished_and_inactive_content_are_blocked(self):
        plan = self._plan(
            ("tr", "STORY", "Hikayeler", "Taslak Öykü"),
            ("tr", "STORY", "Hikayeler", "Pasif Öykü"),
        )
        client = FakeAdminClient(
            contents=[
                content(101, "Taslak Öykü", status="DRAFT"),
                content(102, "Pasif Öykü", active=False),
            ]
        )
        remote = remote_preflight(plan, client)

        self.assertFalse(remote.ready)
        codes = {item.code for item in remote.conflicts}
        self.assertEqual(codes, {"unpublished_content_localization", "inactive_content"})
        self.assertEqual(client.added_links, [])

    def test_story_and_audio_story_overlap_in_one_lane_is_blocked(self):
        plan = self._plan(
            ("tr", "STORY", "Hikayeler", "Bir Öykü"),
            ("tr", "AUDIO_STORY", "Hikayeler", "Bir Öykü"),
        )
        client = FakeAdminClient(contents=[content(101, "Bir Öykü")])
        remote = remote_preflight(plan, client)

        self.assertFalse(remote.ready)
        self.assertIn("duplicate_canonical_content_in_lane", {item.code for item in remote.conflicts})
        self.assertEqual(client.added_links, [])

    def test_source_fingerprint_blocks_execution_after_csv_change(self):
        with tempfile.NamedTemporaryFile("w", suffix=".csv", encoding="utf-8", delete=False) as stream:
            stream.write("language,type,name,name\ntr,STORY,Hikayeler,Bir Öykü\n")
            path = Path(stream.name)
        self.addCleanup(lambda: path.unlink(missing_ok=True))
        plan = build_category_story_mapping_plan(path)
        client = FakeAdminClient(contents=[content(101, "Bir Öykü")])
        remote = remote_preflight(plan, client)
        path.write_text(
            "language,type,name,name\ntr,STORY,Hikayeler,Bir Öykü\ntr,STORY,Hikayeler,İkinci Öykü\n",
            encoding="utf-8",
        )

        with self.assertRaises(CategoryStoryMappingValidationError):
            execute_mapping_import(plan, client, FakeReport(), remote)
        self.assertEqual(client.updated_localizations, [])
        self.assertEqual(client.added_links, [])


class CategoryMappingClientTests(unittest.TestCase):
    def test_new_client_wrappers_use_expected_paths_and_bodies(self):
        client = TellPalAdminClient("https://example.test")
        client._request_json = MagicMock(
            side_effect=[
                {"categoryId": 7},
                [],
                {"contentId": 101},
            ]
        )

        client.update_category_localization(
            7,
            "tr",
            {"name": "Hikayeler", "status": "PUBLISHED", "publishedAt": "now"},
        )
        client.list_category_contents(7, "tr")
        client.add_category_content(7, "tr", {"contentId": 101, "displayOrder": 0})

        self.assertEqual(
            client._request_json.call_args_list[0].args[:2],
            ("PUT", "/api/admin/categories/7/localizations/tr"),
        )
        self.assertEqual(
            client._request_json.call_args_list[0].kwargs["body"]["status"],
            "PUBLISHED",
        )
        self.assertEqual(
            client._request_json.call_args_list[1].args[:2],
            ("GET", "/api/admin/categories/7/localizations/tr/contents"),
        )
        self.assertEqual(
            client._request_json.call_args_list[2].args[:2],
            ("POST", "/api/admin/categories/7/localizations/tr/contents"),
        )
        self.assertEqual(client._request_json.call_args_list[2].kwargs["body"]["displayOrder"], 0)


class CategoryMappingCliTests(unittest.TestCase):
    def test_live_cli_requires_exact_import_confirmation_and_repreflights(self):
        import import_category_story_mappings as cli

        remote = MagicMock(ready=True)
        client = MagicMock()
        client.last_request = None
        summary = {
            "lanes": 1,
            "publishedCategoryLocalizations": 0,
            "createdLinks": 0,
            "reusedLinks": 1,
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "mapping.csv"
            path.write_text(
                "language,type,name,name\ntr,STORY,Hikayeler,Bir Öykü\n",
                encoding="utf-8",
            )
            report = MagicMock()
            with (
                patch.object(sys, "argv", ["import_category_story_mappings.py", str(path)]),
                patch.dict(
                    "os.environ",
                    {
                        "TELLPAL_API_BASE_URL": "https://example.test",
                        "TELLPAL_ADMIN_USERNAME": "admin",
                    },
                    clear=False,
                ),
                patch.object(sys.stdin, "isatty", return_value=True),
                patch.object(cli, "CategoryStoryMappingRunReport", return_value=report),
                patch.object(cli, "TellPalAdminClient", return_value=client),
                patch.object(cli.getpass, "getpass", return_value="secret"),
                patch.object(cli, "remote_preflight", side_effect=[remote, remote]) as preflight,
                patch.object(cli, "execute_mapping_import", return_value=summary) as execute,
                patch.object(cli, "format_preview", return_value="preview"),
                patch.object(cli, "require_ready"),
                patch("builtins.input", return_value="import"),
            ):
                result = cli.main()

        self.assertEqual(result, 0)
        self.assertEqual(preflight.call_count, 2)
        execute.assert_called_once()
        client.logout.assert_called_once()


class CategoryMappingReportTests(unittest.TestCase):
    def test_report_has_no_secret_url_and_records_remote_plan(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "mapping.csv"
            csv_path.write_text(
                "language,type,name,name\ntr,STORY,Hikayeler,Bir Öykü\n",
                encoding="utf-8",
            )
            plan = build_category_story_mapping_plan(csv_path)
            report = CategoryStoryMappingRunReport(
                plan,
                "https://example.test",
                storage_root=root / "reports",
            )
            report.record_remote_preflight(
                type("Remote", (), {"to_dict": lambda self: {"ready": True}})()
            )
            report.record_last_request({"method": "GET", "path": "/api/admin/contents"})

            payload = json.loads(report.result_path.read_text(encoding="utf-8"))
            self.assertEqual(payload["remotePreflight"], {"ready": True})
            self.assertNotIn("Bearer", json.dumps(payload))
            self.assertNotIn("password", json.dumps(payload).lower())

    def test_report_persists_preflight_and_runtime_skips_with_partial_status(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "mapping.csv"
            path.write_text("language,type,name,name\ntr,STORY,Hikayeler,Missing\n"
                            "tr,STORY,Hikayeler,Rejected\ntr,STORY,Hikayeler,Valid\n", encoding="utf-8")
            plan = build_category_story_mapping_plan(path)
            client = FakeAdminClient(contents=[content(101, "Rejected"), content(102, "Valid")])
            remote = remote_preflight(plan, client)
            report = CategoryStoryMappingRunReport(plan, "https://example.test", storage_root=root / "reports")
            report.record_remote_preflight(remote)
            report.record_skip(remote.lanes[0].assignments[0], AdminApiError("POST", "/contents", 409, {}))
            report.mark_success({"createdLinks": 1, "skippedLinks": 2})
            payload = json.loads(report.result_path.read_text(encoding="utf-8"))
            self.assertEqual(payload["status"], "COMPLETED_WITH_SKIPS")
            self.assertEqual(len(payload["skippedRecords"]), 2)
            self.assertEqual(payload["skippedRecords"][0]["code"], "missing_content_localization")
            self.assertEqual(payload["skippedRecords"][1]["code"], "api_record_rejected")


if __name__ == "__main__":
    unittest.main()
