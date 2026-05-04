"""Small Google Drive API wrapper for the content migration tools."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any


DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
DRIVE_SCOPES = ["https://www.googleapis.com/auth/drive"]


@dataclass(frozen=True)
class DriveFile:
    file_id: str
    name: str
    mime_type: str
    web_view_link: str
    modified_time: str
    parents: tuple[str, ...]


class DriveApiClient:
    def __init__(self, service: Any):
        self._service = service

    def list_children(self, folder_id: str) -> list[DriveFile]:
        query = f"'{escape_query_value(folder_id)}' in parents and trashed = false"
        fields = "nextPageToken, files(id, name, mimeType, webViewLink, modifiedTime, parents)"
        results: list[DriveFile] = []
        page_token: str | None = None
        while True:
            response = (
                self._service.files()
                .list(
                    q=query,
                    fields=fields,
                    pageSize=1000,
                    pageToken=page_token,
                    supportsAllDrives=True,
                    includeItemsFromAllDrives=True,
                )
                .execute()
            )
            for item in response.get("files", []):
                results.append(to_drive_file(item))
            page_token = response.get("nextPageToken")
            if not page_token:
                return results

    def find_child_folder(self, parent_id: str, name: str) -> DriveFile | None:
        query = (
            f"'{escape_query_value(parent_id)}' in parents and "
            f"name = '{escape_query_value(name)}' and "
            f"mimeType = '{DRIVE_FOLDER_MIME_TYPE}' and trashed = false"
        )
        response = (
            self._service.files()
            .list(
                q=query,
                fields="files(id, name, mimeType, webViewLink, modifiedTime, parents)",
                pageSize=10,
                supportsAllDrives=True,
                includeItemsFromAllDrives=True,
            )
            .execute()
        )
        files = response.get("files", [])
        return to_drive_file(files[0]) if files else None

    def find_child_file(self, parent_id: str, name: str) -> DriveFile | None:
        query = (
            f"'{escape_query_value(parent_id)}' in parents and "
            f"name = '{escape_query_value(name)}' and trashed = false"
        )
        response = (
            self._service.files()
            .list(
                q=query,
                fields="files(id, name, mimeType, webViewLink, modifiedTime, parents)",
                pageSize=10,
                supportsAllDrives=True,
                includeItemsFromAllDrives=True,
            )
            .execute()
        )
        files = response.get("files", [])
        return to_drive_file(files[0]) if files else None

    def create_folder(self, parent_id: str, name: str) -> DriveFile:
        body = {"name": name, "mimeType": DRIVE_FOLDER_MIME_TYPE, "parents": [parent_id]}
        response = (
            self._service.files()
            .create(
                body=body,
                fields="id, name, mimeType, webViewLink, modifiedTime, parents",
                supportsAllDrives=True,
            )
            .execute()
        )
        return to_drive_file(response)

    def copy_file(self, source_file_id: str, target_folder_id: str, name: str) -> DriveFile:
        body = {"name": name, "parents": [target_folder_id]}
        response = (
            self._service.files()
            .copy(
                fileId=source_file_id,
                body=body,
                fields="id, name, mimeType, webViewLink, modifiedTime, parents",
                supportsAllDrives=True,
            )
            .execute()
        )
        return to_drive_file(response)


def build_drive_client() -> DriveApiClient:
    return DriveApiClient(build_drive_service())


def build_drive_service() -> Any:
    try:
        from google.oauth2 import service_account
        from googleapiclient.discovery import build
    except ImportError as exc:
        raise RuntimeError(
            "Google Drive dependencies are missing. Install google-api-python-client, "
            "google-auth, and google-auth-oauthlib in the active Python environment."
        ) from exc

    application_credentials = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
    if application_credentials:
        credentials = service_account.Credentials.from_service_account_file(
            application_credentials,
            scopes=DRIVE_SCOPES,
        )
        return build("drive", "v3", credentials=credentials, cache_discovery=False)

    oauth_client_secret = os.environ.get("GOOGLE_OAUTH_CLIENT_SECRET")
    if oauth_client_secret:
        try:
            from google.auth.transport.requests import Request
            from google.oauth2.credentials import Credentials
            from google_auth_oauthlib.flow import InstalledAppFlow
        except ImportError as exc:
            raise RuntimeError(
                "OAuth flow dependencies are missing. Install google-auth-oauthlib."
            ) from exc

        token_path = Path(os.environ.get("GOOGLE_OAUTH_TOKEN_PATH", ".google-drive-token.json"))
        credentials = None
        if token_path.exists():
            credentials = Credentials.from_authorized_user_file(str(token_path), DRIVE_SCOPES)
        if not credentials or not credentials.valid:
            if credentials and credentials.expired and credentials.refresh_token:
                credentials.refresh(Request())
            else:
                flow = InstalledAppFlow.from_client_secrets_file(oauth_client_secret, DRIVE_SCOPES)
                credentials = flow.run_local_server(port=0)
            token_path.write_text(credentials.to_json(), encoding="utf-8")
        return build("drive", "v3", credentials=credentials, cache_discovery=False)

    raise RuntimeError(
        "Set GOOGLE_APPLICATION_CREDENTIALS or GOOGLE_OAUTH_CLIENT_SECRET before using Drive."
    )


def to_drive_file(item: dict[str, Any]) -> DriveFile:
    return DriveFile(
        file_id=str(item.get("id") or ""),
        name=str(item.get("name") or ""),
        mime_type=str(item.get("mimeType") or ""),
        web_view_link=str(item.get("webViewLink") or ""),
        modified_time=str(item.get("modifiedTime") or ""),
        parents=tuple(str(parent) for parent in item.get("parents", [])),
    )


def escape_query_value(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")
