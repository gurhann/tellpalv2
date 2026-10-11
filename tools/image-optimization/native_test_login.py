#!/usr/bin/env python3
"""Explicit operator login for the existing QA identity; never invoked by the driver."""
import argparse
from dataclasses import dataclass, field
import http.client
import multiprocessing
import os
from pathlib import Path
import re
import signal
import ssl
import stat
import sys
import time
from urllib.parse import quote

from native_entry_config import (EntryError, canonical_uid, exact_keys, frozen_json,
    no_symlink, open_nofollow, private_file, private_output, require, strict_json)

LOGIN_SCOPE = "image-optimization-native-test-login-r1"
REQUEST_SECONDS = 10
CLEANUP_SECONDS = 2
MAX_TOKEN_BYTES = 8192
MAX_RESPONSE_BYTES = 32768
API_HOST = "identitytoolkit.googleapis.com"
TOKEN_URI = "https://oauth2.googleapis.com/token"
AUTH_URI = "https://accounts.google.com/o/oauth2/auth"
SAFE_CODES = frozenset(("login_inputs_rejected", "login_project_rejected", "login_credential_rejected",
    "login_account_unavailable", "login_account_disabled", "login_identity_rejected", "login_token_rejected",
    "login_auth_refused", "login_exchange_refused", "login_redirect_refused", "login_response_rejected",
    "login_timeout", "login_worker_failed", "login_cleanup_failed", "login_output_failed",
    "schema_rejected", "json_size_rejected", "duplicate_key", "nonfinite_number", "json_rejected",
    "path_rejected", "path_unavailable", "symlink_rejected", "repository_path_rejected",
    "file_rejected", "file_not_private", "file_size_rejected", "file_unavailable",
    "output_not_unique", "output_parent_not_private", "output_input_overlap", "output_unavailable",
    "qa_selection_rejected", "qa_selection_unavailable"))


def safe_reason(error):
    return str(error) if type(error) is EntryError and str(error) in SAFE_CODES else "login_auth_refused"


@dataclass(frozen=True, repr=False)
class LoginInputs:
    admin: dict = field(repr=False)
    project: str = field(repr=False)
    api_key: str = field(repr=False)
    uid: str = field(repr=False)
    admin_path: Path = field(repr=False)
    api_path: Path = field(repr=False)
    output: Path = field(repr=False)


def load_inputs(admin_path, api_path, output_path):
    """Use only explicit private paths; no home-directory credential discovery."""
    admin_path, api_path = no_symlink(admin_path), no_symlink(api_path)
    require(admin_path != api_path, "login_inputs_rejected")
    api = strict_json(private_file(api_path, 4096), 4096)
    exact_keys(api, ("schemaVersion", "firebaseProjectId", "webApiKey"))
    require(type(api["schemaVersion"]) is int and api["schemaVersion"] == 1, "login_inputs_rejected")
    project, api_key = api["firebaseProjectId"], api["webApiKey"]
    require(type(project) is str and re.fullmatch(r"[a-z][a-z0-9-]{4,62}", project), "login_project_rejected")
    require(type(api_key) is str and re.fullmatch(r"[A-Za-z0-9_-]{16,256}", api_key), "login_inputs_rejected")
    admin = strict_json(private_file(admin_path, 65536), 65536)
    require(type(admin) is dict and admin.get("type") == "service_account", "login_credential_rejected")
    require(admin.get("project_id") == project, "login_project_rejected")
    require(admin.get("token_uri") == TOKEN_URI and
        admin.get("auth_uri", AUTH_URI) == AUTH_URI and
        admin.get("universe_domain", "googleapis.com") == "googleapis.com", "login_credential_rejected")
    email = admin.get("client_email")
    require(type(email) is str and re.fullmatch(r"[A-Za-z0-9_-]+@" + re.escape(project) +
        r"\.iam\.gserviceaccount\.com", email), "login_credential_rejected")
    require(type(admin.get("private_key")) is str and 1 <= len(admin["private_key"]) <= 32768,
        "login_credential_rejected")
    uid = canonical_uid()
    require(type(uid) is str and 1 <= len(uid) <= 128, "qa_selection_rejected")
    output = private_output(output_path, (admin_path, api_path))
    return LoginInputs(admin, project, api_key, uid, admin_path, api_path, output)


def remaining_seconds(deadline):
    remaining = deadline - time.monotonic()
    require(remaining > 0, "login_timeout")
    return min(remaining, REQUEST_SECONDS)


def exchange_custom_token(api_key, custom_token, deadline, connection_factory=http.client.HTTPSConnection):
    """Fixed Google HTTPS origin, standard TLS trust, bounded JSON and no redirect handling."""
    connection = None
    try:
        connection = connection_factory(API_HOST, timeout=remaining_seconds(deadline),
            context=ssl.create_default_context())
        body = frozen_json({"token": custom_token, "returnSecureToken": True})
        path = "/v1/accounts:signInWithCustomToken?key=" + quote(api_key, safe="")
        connection.request("POST", path, body=body, headers={"Content-Type": "application/json",
            "Accept": "application/json", "Content-Length": str(len(body))})
        response = connection.getresponse()
        require(not 300 <= response.status <= 399, "login_redirect_refused")
        require(response.status == 200, "login_exchange_refused")
        headers = response.getheaders()
        content_types = [value for name, value in headers if name.lower() == "content-type"]
        require(len(content_types) == 1 and content_types[0].split(";", 1)[0].strip().lower() ==
            "application/json", "login_response_rejected")
        lengths = [value for name, value in headers if name.lower() == "content-length"]
        require(len(lengths) <= 1 and (not lengths or re.fullmatch(r"[0-9]{1,6}", lengths[0]) and
            int(lengths[0]) <= MAX_RESPONSE_BYTES), "login_response_rejected")
        encodings = [value for name, value in headers if name.lower() == "content-encoding"]
        require(not encodings or encodings == ["identity"], "login_response_rejected")
        data = response.read(MAX_RESPONSE_BYTES + 1)
        require(0 < len(data) <= MAX_RESPONSE_BYTES and
            (not lengths or len(data) == int(lengths[0])), "login_response_rejected")
        remaining_seconds(deadline)
        return strict_json(data, MAX_RESPONSE_BYTES)
    except EntryError:
        raise
    except Exception:
        raise EntryError("login_exchange_refused") from None
    finally:
        if connection is not None:
            connection.close()


class FirebaseLoginClient:
    """Explicit same-project Admin client, used only within the owned worker."""
    def __init__(self, admin, project):
        import firebase_admin
        from firebase_admin import auth, credentials
        self.sdk, self.auth = firebase_admin, auth
        self.app = firebase_admin.initialize_app(credentials.Certificate(admin),
            {"projectId": project, "httpTimeout": REQUEST_SECONDS}, name="native-operator-test-login")

    def get_user(self, uid):
        return self.auth.get_user(uid, app=self.app)

    def create_custom_token(self, uid):
        return self.auth.create_custom_token(uid, app=self.app)

    def verify_id_token(self, token, *, check_revoked):
        return self.auth.verify_id_token(token, app=self.app, check_revoked=check_revoked)

    def close(self):
        self.sdk.delete_app(self.app)


def obtain_verified_token(inputs, *, auth_factory=FirebaseLoginClient, exchange=exchange_custom_token,
                          clock=time.time, deadline=None):
    """Refuse before exchange when the canonical account is missing, disabled or mismatched."""
    deadline = time.monotonic() + REQUEST_SECONDS if deadline is None else deadline
    client = None
    try:
        remaining_seconds(deadline)
        client = auth_factory(inputs.admin, inputs.project)
        try:
            user = client.get_user(inputs.uid)
        except Exception:
            raise EntryError("login_account_unavailable") from None
        require(getattr(user, "uid", None) == inputs.uid, "login_identity_rejected")
        require(getattr(user, "disabled", None) is False, "login_account_disabled")
        remaining_seconds(deadline)
        custom_token = client.create_custom_token(inputs.uid)
        if type(custom_token) is bytes:
            custom_token = custom_token.decode("ascii", "strict")
        require(type(custom_token) is str and 0 < len(custom_token) <= MAX_TOKEN_BYTES and
            all(32 < ord(char) < 127 for char in custom_token), "login_token_rejected")
        response = exchange(inputs.api_key, custom_token, deadline)
        custom_token = None
        require(type(response) is dict and {"idToken", "refreshToken", "expiresIn"}.issubset(response) and
            set(response) <= {"idToken", "refreshToken", "expiresIn", "kind", "localId", "isNewUser"},
            "login_response_rejected")
        require("localId" not in response or response["localId"] == inputs.uid, "login_identity_rejected")
        require("isNewUser" not in response or response["isNewUser"] is False, "login_identity_rejected")
        require(type(response["expiresIn"]) is str and re.fullmatch(r"[1-9][0-9]{0,4}", response["expiresIn"]),
            "login_response_rejected")
        require(type(response["refreshToken"]) is str and 0 < len(response["refreshToken"]) <= MAX_TOKEN_BYTES,
            "login_response_rejected")
        token = response["idToken"]
        response.clear()
        require(type(token) is str and 0 < len(token) <= MAX_TOKEN_BYTES and
            all(32 < ord(char) < 127 for char in token), "login_token_rejected")
        remaining_seconds(deadline)
        claims = client.verify_id_token(token, check_revoked=True)
        require(type(claims) is dict and claims.get("uid") == inputs.uid and claims.get("sub") == inputs.uid and
            claims.get("aud") == inputs.project and
            claims.get("iss") == "https://securetoken.google.com/" + inputs.project, "login_identity_rejected")
        require(type(claims.get("exp")) is int and claims["exp"] > clock(), "login_token_rejected")
        remaining_seconds(deadline)
        return token
    except EntryError:
        raise
    except Exception:
        raise EntryError("login_auth_refused") from None
    finally:
        if client is not None:
            client.close()


def _login_worker(inputs, sender):
    try:
        os.setsid()
        null = os.open(os.devnull, os.O_WRONLY)
        os.dup2(null, 1)
        os.dup2(null, 2)
        os.close(null)
        allowed = {name: os.environ[name] for name in ("PATH", "HOME", "TMPDIR", "LANG", "LC_ALL")
            if name in os.environ}
        os.environ.clear()
        os.environ.update(allowed)
        token = obtain_verified_token(inputs)
        sender.send_bytes(frozen_json({"idToken": token}))
    except Exception as error:
        sender.send_bytes(frozen_json({"reason": safe_reason(error)}))
    finally:
        sender.close()


def stop_worker(process):
    """Kill only the owned group; retain a PID-only fallback if setsid failed."""
    if process.pid is None:
        return
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except (ProcessLookupError, PermissionError):
        pass
    if process.is_alive():
        process.kill()
    process.join(timeout=CLEANUP_SECONDS)
    require(not process.is_alive(), "login_cleanup_failed")


def login_in_worker(inputs):
    context = multiprocessing.get_context("spawn")
    receiver, sender = context.Pipe(duplex=False)
    process = context.Process(target=_login_worker, args=(inputs, sender))
    started = False
    try:
        process.start()
        started = True
        sender.close()
        require(receiver.poll(REQUEST_SECONDS), "login_timeout")
        response = strict_json(receiver.recv_bytes(MAX_TOKEN_BYTES + 256), MAX_TOKEN_BYTES + 256)
        require(type(response) is dict, "login_worker_failed")
        if set(response) == {"reason"}:
            reason = response["reason"]
            raise EntryError(reason if type(reason) is str and reason in SAFE_CODES else "login_worker_failed")
        require(set(response) == {"idToken"} and type(response["idToken"]) is str and
            0 < len(response["idToken"]) <= MAX_TOKEN_BYTES and
            all(32 < ord(char) < 127 for char in response["idToken"]), "login_worker_failed")
        return response["idToken"]
    except EntryError:
        raise
    except Exception:
        raise EntryError("login_worker_failed") from None
    finally:
        receiver.close()
        sender.close()
        if started:
            stop_worker(process)


def write_token(inputs, token):
    """Create one private output exclusively; remove only this inode on a failed write."""
    path = private_output(inputs.output, (inputs.admin_path, inputs.api_path))
    parent_descriptor, descriptor, created = None, None, None
    try:
        parent_descriptor = open_nofollow(path.parent, os.O_RDONLY | os.O_DIRECTORY)
        parent = os.fstat(parent_descriptor)
        require(parent.st_uid == os.getuid() and stat.S_IMODE(parent.st_mode) == 0o700,
            "output_parent_not_private")
        descriptor = os.open(path.name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW,
            0o600, dir_fd=parent_descriptor)
        created = os.fstat(descriptor)
        os.fchmod(descriptor, 0o600)
        with os.fdopen(descriptor, "wb") as handle:
            descriptor = None
            handle.write(token.encode("ascii") + b"\n")
            handle.flush()
            os.fsync(handle.fileno())
    except Exception:
        if created is not None:
            try:
                current = os.stat(path.name, dir_fd=parent_descriptor, follow_symlinks=False)
                if (current.st_dev, current.st_ino) == (created.st_dev, created.st_ino):
                    os.unlink(path.name, dir_fd=parent_descriptor)
            except OSError:
                pass
        raise EntryError("login_output_failed") from None
    finally:
        if descriptor is not None:
            os.close(descriptor)
        if parent_descriptor is not None:
            os.close(parent_descriptor)


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        self.exit(2, "login_cli_rejected\n")


def main(argv=None):
    parser = SafeParser(description="Operator-owned existing QA account login")
    parser.add_argument("--admin-credential", required=True)
    parser.add_argument("--web-api-config", required=True)
    parser.add_argument("--id-token-output", required=True)
    args = parser.parse_args(argv)
    try:
        inputs = load_inputs(args.admin_credential, args.web_api_config, args.id_token_output)
        token = login_in_worker(inputs)
        write_token(inputs, token)
        print(frozen_json({"scope": LOGIN_SCOPE, "status": "completed", "idTokenFileWritten": True,
            "nativeEntryGate": "open"}).decode("ascii"))
        return 0
    except Exception as error:
        print(frozen_json({"scope": LOGIN_SCOPE, "status": "refused", "reason": safe_reason(error),
            "nativeEntryGate": "open"}).decode("ascii"))
        return 4


if __name__ == "__main__":
    sys.exit(main())
