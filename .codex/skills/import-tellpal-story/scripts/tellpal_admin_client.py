from __future__ import annotations

import json
import mimetypes
import random
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path
from typing import Callable


class AdminApiError(RuntimeError):
    def __init__(self, method: str, path: str, status: int, payload: object):
        self.method = method
        self.path = path
        self.status = status
        self.payload = payload
        super().__init__(f"{method} {path} returned HTTP {status}: {_error_summary(payload)}")


class AdminTransportError(RuntimeError):
    def __init__(self, method: str, path: str, reason: object):
        self.method = method
        self.path = path
        self.reason = reason
        super().__init__(f"{method} {path} failed before a definitive response: {reason}")


class TellPalAdminClient:
    """Small TellPal Admin API client with guarded read retries and token rotation."""

    def __init__(
        self,
        base_url: str,
        *,
        timeout_seconds: float = 120,
        sleep: Callable[[float], None] = time.sleep,
    ):
        self.base_url = validate_base_url(base_url)
        self.timeout_seconds = timeout_seconds
        self._sleep = sleep
        self.access_token: str | None = None
        self.refresh_token: str | None = None
        self.last_request: dict[str, object] | None = None

    def login(self, username: str, password: str) -> dict[str, object]:
        response = self._request_json(
            "POST",
            "/api/admin/auth/login",
            body={"username": username, "password": password},
            authenticated=False,
        )
        payload = _expect_dict(response, "login response")
        self.access_token = _required_string(payload, "accessToken")
        self.refresh_token = _required_string(payload, "refreshToken")
        return _without_tokens(payload)

    def logout(self) -> None:
        if not self.refresh_token:
            return
        refresh_token = self.refresh_token
        try:
            self._request_json(
                "POST",
                "/api/admin/auth/logout",
                body={"refreshToken": refresh_token},
                authenticated=False,
                allow_refresh=False,
            )
        finally:
            self.access_token = None
            self.refresh_token = None

    def list_contents(self) -> list[dict[str, object]]:
        return _expect_list(self._request_json("GET", "/api/admin/contents"), "content list")

    def get_content(self, content_id: int) -> dict[str, object]:
        return _expect_dict(self._request_json("GET", f"/api/admin/contents/{content_id}"), "content")

    def create_content(self, body: dict[str, object]) -> dict[str, object]:
        return _expect_dict(self._request_json("POST", "/api/admin/contents", body=body), "created content")

    def update_content(self, content_id: int, body: dict[str, object]) -> dict[str, object]:
        return _expect_dict(
            self._request_json("PUT", f"/api/admin/contents/{content_id}", body=body),
            "updated content",
        )

    def create_localization(
        self,
        content_id: int,
        language_code: str,
        body: dict[str, object],
    ) -> dict[str, object]:
        path = f"/api/admin/contents/{content_id}/localizations/{language_code}"
        return _expect_dict(self._request_json("POST", path, body=body), "created localization")

    def publish_localization(self, content_id: int, language_code: str) -> dict[str, object]:
        path = f"/api/admin/contents/{content_id}/localizations/{language_code}/publish"
        return _expect_dict(
            self._request_json("POST", path, body={"publishedAt": None}),
            "published localization",
        )

    def list_story_pages(self, content_id: int) -> list[dict[str, object]]:
        path = f"/api/admin/contents/{content_id}/story-pages"
        return _expect_list(self._request_json("GET", path), "story page list")

    def add_story_page(self, content_id: int) -> dict[str, object]:
        path = f"/api/admin/contents/{content_id}/story-pages"
        return _expect_dict(
            self._request_json("POST", path, body={"afterPageNumber": None}),
            "created story page",
        )

    def update_story_page(self, content_id: int, page_number: int, media_id: int) -> dict[str, object]:
        path = f"/api/admin/contents/{content_id}/story-pages/{page_number}"
        return _expect_dict(
            self._request_json("PUT", path, body={"textlessIllustrationMediaId": media_id}),
            "updated story page",
        )

    def upsert_story_page_localization(
        self,
        content_id: int,
        page_number: int,
        language_code: str,
        body: dict[str, object],
    ) -> dict[str, object]:
        path = (
            f"/api/admin/contents/{content_id}/story-pages/{page_number}"
            f"/localizations/{language_code}"
        )
        return _expect_dict(self._request_json("PUT", path, body=body), "story page localization")

    def search_contributors(self, display_name: str) -> list[dict[str, object]]:
        query = urllib.parse.urlencode({"q": display_name, "limit": 100})
        return _expect_list(
            self._request_json("GET", f"/api/admin/contributors?{query}"),
            "contributor list",
        )

    def create_contributor(self, display_name: str) -> dict[str, object]:
        return _expect_dict(
            self._request_json(
                "POST",
                "/api/admin/contributors",
                body={"displayName": display_name},
            ),
            "created contributor",
        )

    def list_content_contributors(self, content_id: int) -> list[dict[str, object]]:
        return _expect_list(
            self._request_json("GET", f"/api/admin/contents/{content_id}/contributors"),
            "content contributor list",
        )

    def assign_contributor(self, content_id: int, body: dict[str, object]) -> dict[str, object]:
        return _expect_dict(
            self._request_json("POST", f"/api/admin/contents/{content_id}/contributors", body=body),
            "contributor assignment",
        )

    def get_media(self, asset_id: int) -> dict[str, object]:
        return _expect_dict(self._request_json("GET", f"/api/admin/media/{asset_id}"), "media asset")

    def upload_media(self, path: str | Path, kind: str, checksum_sha256: str) -> dict[str, object]:
        file_path = Path(path)
        mime_type = media_mime_type(file_path)
        boundary = f"TellPal-{uuid.uuid4().hex}"
        body = _multipart_body(
            boundary,
            fields={"kind": kind, "checksumSha256": checksum_sha256},
            file_path=file_path,
            mime_type=mime_type,
        )
        return _expect_dict(
            self._request_bytes(
                "POST",
                "/api/admin/media/uploads",
                data=body,
                content_type=f"multipart/form-data; boundary={boundary}",
            ),
            "uploaded media asset",
        )

    def _request_json(
        self,
        method: str,
        path: str,
        *,
        body: dict[str, object] | None = None,
        authenticated: bool = True,
        allow_refresh: bool = True,
    ) -> object:
        data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
        return self._request_bytes(
            method,
            path,
            data=data,
            content_type="application/json" if data is not None else None,
            authenticated=authenticated,
            allow_refresh=allow_refresh,
        )

    def _request_bytes(
        self,
        method: str,
        path: str,
        *,
        data: bytes | None = None,
        content_type: str | None = None,
        authenticated: bool = True,
        allow_refresh: bool = True,
    ) -> object:
        normalized_method = method.upper()
        attempts = 3 if normalized_method == "GET" else 1
        refreshed = False
        for attempt in range(attempts):
            try:
                return self._send_once(
                    normalized_method,
                    path,
                    data=data,
                    content_type=content_type,
                    authenticated=authenticated,
                )
            except AdminApiError as exception:
                if (
                    exception.status == 401
                    and authenticated
                    and allow_refresh
                    and not refreshed
                    and self.refresh_token
                ):
                    self._refresh_tokens()
                    refreshed = True
                    continue
                if normalized_method == "GET" and exception.status in {429, 502, 503, 504} and attempt + 1 < attempts:
                    self._sleep(_retry_delay(attempt))
                    continue
                raise
            except AdminTransportError:
                if normalized_method == "GET" and attempt + 1 < attempts:
                    self._sleep(_retry_delay(attempt))
                    continue
                raise
        raise AssertionError("request retry loop exhausted unexpectedly")

    def _send_once(
        self,
        method: str,
        path: str,
        *,
        data: bytes | None,
        content_type: str | None,
        authenticated: bool,
    ) -> object:
        self.last_request = {"method": method, "path": path}
        headers = {"Accept": "application/json", "User-Agent": "TellPal-Story-Import-Agent/1.0"}
        if content_type:
            headers["Content-Type"] = content_type
        if authenticated:
            if not self.access_token:
                raise RuntimeError("Admin client is not authenticated")
            headers["Authorization"] = f"Bearer {self.access_token}"
        request = urllib.request.Request(self._url(path), data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                raw = response.read()
        except urllib.error.HTTPError as exception:
            payload = _decode_payload(exception.read())
            raise AdminApiError(method, path, exception.code, payload) from exception
        except (urllib.error.URLError, TimeoutError, OSError) as exception:
            raise AdminTransportError(method, path, exception) from exception
        return _decode_payload(raw)

    def _refresh_tokens(self) -> None:
        if not self.refresh_token:
            raise RuntimeError("Cannot refresh without a refresh token")
        response = self._request_json(
            "POST",
            "/api/admin/auth/refresh",
            body={"refreshToken": self.refresh_token},
            authenticated=False,
            allow_refresh=False,
        )
        payload = _expect_dict(response, "refresh response")
        self.access_token = _required_string(payload, "accessToken")
        self.refresh_token = _required_string(payload, "refreshToken")

    def _url(self, path: str) -> str:
        return f"{self.base_url}{path}"


def validate_base_url(base_url: str) -> str:
    normalized = (base_url or "").strip().rstrip("/")
    parsed = urllib.parse.urlparse(normalized)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise ValueError("TELLPAL_API_BASE_URL must be an absolute http(s) URL")
    if parsed.query or parsed.fragment:
        raise ValueError("TELLPAL_API_BASE_URL must not contain a query or fragment")
    return normalized


def media_mime_type(path: Path) -> str:
    explicit = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".mp3": "audio/mpeg",
    }
    mime_type = explicit.get(path.suffix.casefold()) or mimetypes.guess_type(path.name)[0]
    if not mime_type:
        raise ValueError(f"Cannot determine MIME type for {path}")
    return mime_type


def _multipart_body(
    boundary: str,
    *,
    fields: dict[str, str],
    file_path: Path,
    mime_type: str,
) -> bytes:
    chunks: list[bytes] = []
    for name, value in fields.items():
        chunks.extend(
            [
                f"--{boundary}\r\n".encode("ascii"),
                f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("ascii"),
                value.encode("utf-8"),
                b"\r\n",
            ]
        )
    safe_filename = file_path.name.replace('"', "")
    chunks.extend(
        [
            f"--{boundary}\r\n".encode("ascii"),
            f'Content-Disposition: form-data; name="file"; filename="{safe_filename}"\r\n'.encode("utf-8"),
            f"Content-Type: {mime_type}\r\n\r\n".encode("ascii"),
            file_path.read_bytes(),
            b"\r\n",
            f"--{boundary}--\r\n".encode("ascii"),
        ]
    )
    return b"".join(chunks)


def _decode_payload(raw: bytes) -> object:
    if not raw:
        return None
    text = raw.decode("utf-8", errors="replace")
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        return {"raw": text[:2000]}


def _expect_dict(payload: object, label: str) -> dict[str, object]:
    if not isinstance(payload, dict):
        raise RuntimeError(f"Expected {label} to be a JSON object")
    return payload


def _expect_list(payload: object, label: str) -> list[dict[str, object]]:
    if not isinstance(payload, list) or not all(isinstance(item, dict) for item in payload):
        raise RuntimeError(f"Expected {label} to be a JSON object list")
    return payload


def _required_string(payload: dict[str, object], field: str) -> str:
    value = payload.get(field)
    if not isinstance(value, str) or not value:
        raise RuntimeError(f"Response is missing required string field {field}")
    return value


def _without_tokens(payload: dict[str, object]) -> dict[str, object]:
    return {key: value for key, value in payload.items() if key not in {"accessToken", "refreshToken"}}


def _retry_delay(attempt: int) -> float:
    return (0.5 * (2**attempt)) + random.uniform(0.0, 0.2)


def _error_summary(payload: object) -> str:
    if isinstance(payload, dict):
        for key in ("detail", "message", "title", "errorCode"):
            value = payload.get(key)
            if value:
                return str(value)
    return str(payload)[:500]
