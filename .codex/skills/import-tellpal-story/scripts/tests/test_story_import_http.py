from __future__ import annotations

import json
import os
import re
import sys
import tempfile
import threading
import unittest
import urllib.parse
from email import policy
from email.parser import BytesParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest import mock


SCRIPTS_DIRECTORY = Path(__file__).resolve().parents[1]
TESTS_DIRECTORY = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIRECTORY))
sys.path.insert(0, str(TESTS_DIRECTORY))

from story_import_report import ImportRunReport  # noqa: E402
from import_story import _is_import_confirmation  # noqa: E402
from story_import_verifier import ImportVerificationError, _assert_processing_status  # noqa: E402
from story_import_workflow import (  # noqa: E402
    RemotePreflightError,
    assert_source_unchanged,
    execute_import,
    remote_preflight,
)
from story_manifest import build_story_plan  # noqa: E402
from tellpal_admin_client import TellPalAdminClient  # noqa: E402
from test_story_manifest import create_story_fixture  # noqa: E402


class StoryImportHttpTest(unittest.TestCase):
    def test_accepts_only_the_import_confirmation_keyword(self) -> None:
        self.assertTrue(_is_import_confirmation("import"))
        self.assertTrue(_is_import_confirmation(" IMPORT "))
        self.assertFalse(_is_import_confirmation(""))
        self.assertFalse(_is_import_confirmation("import story.example"))

    def test_final_processing_status_accepts_pending_or_completed(self) -> None:
        _assert_processing_status("PENDING", {"PENDING", "COMPLETED"}, "processingStatus")
        _assert_processing_status("COMPLETED", {"PENDING", "COMPLETED"}, "processingStatus")
        with self.assertRaises(ImportVerificationError):
            _assert_processing_status("FAILED", {"PENDING", "COMPLETED"}, "processingStatus")

    def test_refreshes_rotating_tokens_after_read_unauthorized(self) -> None:
        with FakeTellPalApi() as api:
            api.state.expire_next_contents_request = True
            client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
            client.login("admin", "secret")

            self.assertEqual(client.list_contents(), [])
            client.logout()

        self.assertIn(("POST", "/api/admin/auth/refresh"), api.state.requests)
        self.assertEqual(api.state.refresh_count, 1)

    def test_executes_endpoint_workflow_and_publishes_last(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            plan = build_story_plan(root)
            with FakeTellPalApi() as api, mock.patch.dict(
                os.environ,
                {"LOCALAPPDATA": str(Path(temporary_directory) / "local-app-data")},
            ):
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")
                resolutions = remote_preflight(plan, client)
                report = ImportRunReport(plan, api.base_url)
                assert_source_unchanged(plan)

                summary = execute_import(plan, client, report, resolutions)
                client.logout()

            self.assertEqual(summary["pageCount"], 1)
            self.assertEqual(summary["uploadedAssetCount"], 5)
            self.assertEqual(summary["contributorAssignmentCount"], 3)
            self.assertEqual(api.state.localizations["tr"]["status"], "PUBLISHED")
            self.assertEqual(report.state["status"], "COMPLETED")
            self.assertEqual(len(api.state.assets), 5)
            self.assertTrue(all(asset["checksumSha256"] for asset in api.state.assets.values()))
            persisted_report = report.result_path.read_text(encoding="utf-8")
            self.assertNotIn("access-token", persisted_report)
            self.assertNotIn("refresh-token", persisted_report)
            self.assertNotIn("secret", persisted_report)

            requests = api.state.requests
            paths = [path for _, path in requests]
            content_create_index = requests.index(("POST", "/api/admin/contents"))
            publish_index = requests.index(("POST", "/api/admin/contents/1/localizations/tr/publish"))
            assignment_indices = [
                index
                for index, request in enumerate(requests)
                if request == ("POST", "/api/admin/contents/1/contributors")
            ]
            self.assertGreater(
                content_create_index,
                requests.index(("GET", "/api/admin/contributors?q=Yazar&limit=100")),
            )
            self.assertGreater(publish_index, max(assignment_indices))
            self.assertEqual(paths[-1], "/api/admin/auth/logout")

    def test_executes_workflow_without_textless_mutations_when_explicitly_allowed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            for path in (root / "yazisiz").iterdir():
                path.unlink()
            plan = build_story_plan(root, allow_missing_textless=True)
            with FakeTellPalApi() as api, mock.patch.dict(
                os.environ,
                {"LOCALAPPDATA": str(Path(temporary_directory) / "local-app-data")},
            ):
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")
                resolutions = remote_preflight(plan, client)
                report = ImportRunReport(plan, api.base_url)

                summary = execute_import(plan, client, report, resolutions)
                client.logout()

            self.assertEqual(summary["uploadedAssetCount"], 3)
            self.assertIsNone(api.state.content["textlessCoverMediaId"])
            self.assertIsNone(api.state.pages[1]["textlessIllustrationMediaId"])
            self.assertNotIn(("PUT", "/api/admin/contents/1"), api.state.requests)
            self.assertNotIn(("PUT", "/api/admin/contents/1/story-pages/1"), api.state.requests)
            self.assertIn(
                "textless-media-skipped",
                [item["step"] for item in report.state["completedSteps"]],
            )

    def test_existing_external_key_stops_before_content_writes(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            plan = build_story_plan(root)
            with FakeTellPalApi() as api:
                api.state.preexisting_contents.append(
                    {"contentId": 77, "externalKey": plan.external_key, "type": "STORY"}
                )
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")

                with self.assertRaisesRegex(RemotePreflightError, "already exists"):
                    remote_preflight(plan, client)

            self.assertNotIn(("POST", "/api/admin/contents"), api.state.requests)

    def test_ambiguous_contributor_stops_before_content_writes(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            plan = build_story_plan(root)
            with FakeTellPalApi() as api:
                api.state.contributors = {10: "Yazar", 11: "yazar"}
                api.state.next_contributor_id = 12
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")

                with self.assertRaisesRegex(RemotePreflightError, "multiple exact matches"):
                    remote_preflight(plan, client)

            self.assertNotIn(("POST", "/api/admin/contents"), api.state.requests)

    def test_explicit_contributor_id_resolves_duplicate_exact_names(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            plan = build_story_plan(root)
            with FakeTellPalApi() as api:
                api.state.contributors = {10: "Yazar", 11: "yazar"}
                api.state.next_contributor_id = 12
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")

                resolutions = remote_preflight(
                    plan,
                    client,
                    contributor_id_overrides={"yazar": 10},
                )

            author = next(item for item in resolutions if item.display_name == "Yazar")
            self.assertEqual(author.contributor_id, 10)
            self.assertNotIn(("POST", "/api/admin/contents"), api.state.requests)

    def test_invalid_contributor_id_override_stops_before_content_writes(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            plan = build_story_plan(root)
            with FakeTellPalApi() as api:
                api.state.contributors = {10: "Yazar", 11: "yazar"}
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")

                with self.assertRaisesRegex(RemotePreflightError, "exact-match ids"):
                    remote_preflight(
                        plan,
                        client,
                        contributor_id_overrides={"yazar": 99},
                    )

            self.assertNotIn(("POST", "/api/admin/contents"), api.state.requests)

    def test_changed_source_fingerprint_stops_before_content_writes(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory) / "story"
            root.mkdir()
            create_story_fixture(root)
            plan = build_story_plan(root)
            with FakeTellPalApi() as api:
                client = TellPalAdminClient(api.base_url, sleep=lambda _: None)
                client.login("admin", "secret")
                remote_preflight(plan, client)
                (root / "tr" / "note.txt").write_text("changed", encoding="utf-8")

                with self.assertRaisesRegex(RemotePreflightError, "changed after preview"):
                    assert_source_unchanged(plan)

            self.assertNotIn(("POST", "/api/admin/contents"), api.state.requests)


class FakeTellPalState:
    def __init__(self) -> None:
        self.requests: list[tuple[str, str]] = []
        self.preexisting_contents: list[dict[str, object]] = []
        self.content: dict[str, object] | None = None
        self.localizations: dict[str, dict[str, object]] = {}
        self.pages: dict[int, dict[str, object]] = {}
        self.assets: dict[int, dict[str, object]] = {}
        self.contributors: dict[int, str] = {}
        self.assignments: list[dict[str, object]] = []
        self.next_asset_id = 100
        self.next_contributor_id = 500
        self.expire_next_contents_request = False
        self.refresh_count = 0


class FakeTellPalApi:
    def __init__(self) -> None:
        self.state = FakeTellPalState()
        handler = _handler_for(self.state)
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    @property
    def base_url(self) -> str:
        host, port = self.server.server_address
        return f"http://{host}:{port}"

    def __enter__(self) -> "FakeTellPalApi":
        self.thread.start()
        return self

    def __exit__(self, *_: object) -> None:
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)


def _handler_for(state: FakeTellPalState) -> type[BaseHTTPRequestHandler]:
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self) -> None:  # noqa: N802
            path, query = self._path_and_query()
            self._record(path, query)
            if path == "/api/admin/contents":
                if state.expire_next_contents_request:
                    state.expire_next_contents_request = False
                    self._json({"errorCode": "admin_token_invalid", "detail": "expired"}, status=401)
                    return
                contents = list(state.preexisting_contents)
                if state.content:
                    contents.append(self._content_response())
                self._json(contents)
                return
            if path == "/api/admin/contributors":
                search = urllib.parse.parse_qs(query).get("q", [""])[0].casefold()
                self._json(
                    [
                        {"contributorId": identifier, "displayName": name}
                        for identifier, name in state.contributors.items()
                        if search in name.casefold()
                    ]
                )
                return
            if path == "/api/admin/contents/1":
                self._json(self._content_response())
                return
            if path == "/api/admin/contents/1/story-pages":
                self._json([state.pages[number] for number in sorted(state.pages)])
                return
            if path == "/api/admin/contents/1/contributors":
                self._json(
                    [
                        {
                            "contentId": 1,
                            **assignment,
                            "contributorDisplayName": state.contributors[assignment["contributorId"]],
                        }
                        for assignment in state.assignments
                    ]
                )
                return
            media_match = re.fullmatch(r"/api/admin/media/(\d+)", path)
            if media_match:
                self._json(state.assets[int(media_match.group(1))])
                return
            self.send_error(404)

        def do_POST(self) -> None:  # noqa: N802
            path, query = self._path_and_query()
            self._record(path, query)
            if path == "/api/admin/auth/login":
                self._json(
                    {
                        "adminUserId": 1,
                        "username": "admin",
                        "roleCodes": ["ADMIN"],
                        "accessToken": "access-token",
                        "accessTokenExpiresAt": "2030-01-01T00:00:00Z",
                        "refreshToken": "refresh-token",
                        "refreshTokenExpiresAt": "2030-01-02T00:00:00Z",
                    }
                )
                return
            if path == "/api/admin/auth/logout":
                self._read_body()
                self.send_response(204)
                self.end_headers()
                return
            if path == "/api/admin/auth/refresh":
                self._json_body()
                state.refresh_count += 1
                self._json(
                    {
                        "adminUserId": 1,
                        "username": "admin",
                        "roleCodes": ["ADMIN"],
                        "accessToken": "rotated-access-token",
                        "accessTokenExpiresAt": "2030-01-01T00:00:00Z",
                        "refreshToken": "rotated-refresh-token",
                        "refreshTokenExpiresAt": "2030-01-02T00:00:00Z",
                    }
                )
                return
            if path == "/api/admin/contents":
                body = self._json_body()
                state.content = {"contentId": 1, **body, "pageCount": 0, "textlessCoverMediaId": None}
                self._json(state.content, status=201)
                return
            if path == "/api/admin/media/uploads":
                fields, file_bytes, mime_type = self._multipart_body()
                asset_id = state.next_asset_id
                state.next_asset_id += 1
                kind = fields["kind"]
                asset = {
                    "assetId": asset_id,
                    "provider": "FIREBASE_STORAGE",
                    "objectPath": f"local/manual/{asset_id}",
                    "mediaType": "AUDIO" if kind == "ORIGINAL_AUDIO" else "IMAGE",
                    "kind": kind,
                    "mimeType": mime_type,
                    "byteSize": len(file_bytes),
                    "checksumSha256": fields["checksumSha256"],
                }
                state.assets[asset_id] = asset
                self._json(asset)
                return
            localization_match = re.fullmatch(r"/api/admin/contents/1/localizations/([^/]+)", path)
            if localization_match:
                language_code = localization_match.group(1)
                body = self._json_body()
                state.localizations[language_code] = {
                    "contentId": 1,
                    "languageCode": language_code,
                    **body,
                    "visibleToMobile": False,
                }
                self._json(state.localizations[language_code], status=201)
                return
            if path == "/api/admin/contents/1/story-pages":
                self._json_body()
                page_number = len(state.pages) + 1
                page = {
                    "contentId": 1,
                    "pageNumber": page_number,
                    "textlessIllustrationMediaId": None,
                    "localizationCount": 0,
                    "localizations": [],
                }
                state.pages[page_number] = page
                state.content["pageCount"] = page_number
                self._json(page, status=201)
                return
            if path == "/api/admin/contributors":
                body = self._json_body()
                contributor_id = state.next_contributor_id
                state.next_contributor_id += 1
                state.contributors[contributor_id] = body["displayName"]
                self._json({"contributorId": contributor_id, "displayName": body["displayName"]}, status=201)
                return
            if path == "/api/admin/contents/1/contributors":
                body = self._json_body()
                state.assignments.append(body)
                self._json({"contentId": 1, **body}, status=201)
                return
            publish_match = re.fullmatch(r"/api/admin/contents/1/localizations/([^/]+)/publish", path)
            if publish_match:
                self._json_body()
                localization = state.localizations[publish_match.group(1)]
                localization["status"] = "PUBLISHED"
                localization["publishedAt"] = "2026-08-10T00:00:00Z"
                self._json(localization)
                return
            self.send_error(404)

        def do_PUT(self) -> None:  # noqa: N802
            path, query = self._path_and_query()
            self._record(path, query)
            localization_match = re.fullmatch(
                r"/api/admin/contents/1/story-pages/(\d+)/localizations/([^/]+)",
                path,
            )
            if localization_match:
                page_number = int(localization_match.group(1))
                language_code = localization_match.group(2)
                body = self._json_body()
                localization = {
                    "contentId": 1,
                    "pageNumber": page_number,
                    "languageCode": language_code,
                    **body,
                }
                state.pages[page_number]["localizations"].append(localization)
                state.pages[page_number]["localizationCount"] += 1
                self._json(localization)
                return
            page_match = re.fullmatch(r"/api/admin/contents/1/story-pages/(\d+)", path)
            if page_match:
                page_number = int(page_match.group(1))
                body = self._json_body()
                state.pages[page_number].update(body)
                self._json(state.pages[page_number])
                return
            if path == "/api/admin/contents/1":
                body = self._json_body()
                state.content.update(body)
                self._json(state.content)
                return
            self.send_error(404)

        def _content_response(self) -> dict[str, object]:
            if not state.content:
                return {}
            return {**state.content, "localizations": list(state.localizations.values())}

        def _path_and_query(self) -> tuple[str, str]:
            parsed = urllib.parse.urlsplit(self.path)
            return parsed.path, parsed.query

        def _record(self, path: str, query: str) -> None:
            recorded_path = f"{path}?{query}" if query else path
            state.requests.append((self.command, recorded_path))

        def _read_body(self) -> bytes:
            length = int(self.headers.get("Content-Length", "0"))
            return self.rfile.read(length)

        def _json_body(self) -> dict[str, object]:
            body = json.loads(self._read_body().decode("utf-8"))
            if not isinstance(body, dict):
                raise AssertionError("Expected object request body")
            return body

        def _multipart_body(self) -> tuple[dict[str, str], bytes, str]:
            content_type = self.headers["Content-Type"]
            message = BytesParser(policy=policy.default).parsebytes(
                f"Content-Type: {content_type}\r\nMIME-Version: 1.0\r\n\r\n".encode("ascii")
                + self._read_body()
            )
            fields: dict[str, str] = {}
            file_bytes = b""
            file_mime_type = ""
            for part in message.iter_parts():
                name = part.get_param("name", header="content-disposition")
                payload = part.get_payload(decode=True)
                if name == "file":
                    file_bytes = payload
                    file_mime_type = part.get_content_type()
                elif name:
                    fields[name] = payload.decode("utf-8")
            return fields, file_bytes, file_mime_type

        def _json(self, payload: object, *, status: int = 200) -> None:
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, *_: object) -> None:
            return

    return Handler


if __name__ == "__main__":
    unittest.main()
