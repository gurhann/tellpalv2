"""Paired TLS broker with immutable, authenticated fixture delivery and owned teardown."""
import base64
from hashlib import sha256
import hmac
import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
import multiprocessing
import secrets
import socket
import ssl
import signal
import threading
import time
from urllib.parse import parse_qs, urlsplit

from native_entry_config import (EntryError, HASH, ID, ROOT, SCOPE, canonical_uid, exact_keys,
    frozen_json, origin, private_file, require, strict_json, validate_reader_entries, READER_CASES)
from native_entry_crypto import certificate_identity, public_key, random_nonce, verify_proof

REQUEST_SECONDS = 10
BASE_FIELDS = frozenset(("scope", "batchId", "sessionId", "launchNonce", "publicKeySpkiBase64"))


def _firebase_worker(credential_data, project, uid, token, output):
    """Private IPC transports runtime credentials; only fixed results leave the worker."""
    app = None
    try:
        os.setsid()
        # SDK exceptions and traces cannot leak to inherited console/log descriptors.
        null = os.open(os.devnull, os.O_WRONLY)
        os.dup2(null, 1)
        os.dup2(null, 2)
        os.close(null)
        import firebase_admin
        from firebase_admin import credentials
        from firebase_admin import auth
        app = firebase_admin.initialize_app(credentials.Certificate(credential_data),
            {"projectId": project, "httpTimeout": REQUEST_SECONDS}, name="native-fixture-worker")
        user = auth.get_user(uid, app=app)
        require(not user.disabled, "qa_disabled")
        claims = auth.verify_id_token(token, app=app, check_revoked=True)
        require(claims.get("uid") == uid and claims.get("sub") == uid and claims.get("aud") == project and
            claims.get("iss") == "https://securetoken.google.com/" + project, "token_identity_rejected")
        require(type(claims.get("exp")) is int and claims["exp"] * 1000 > int(time.time() * 1000), "token_expired")
        output.send_bytes(frozen_json({"exp": claims["exp"]}))
    except EntryError as error:
        output.send_bytes(frozen_json({"reason": str(error)}))
    except Exception:
        output.send_bytes(frozen_json({"reason": "token_verification_failed"}))
    finally:
        if app is not None:
            firebase_admin.delete_app(app)
        output.close()


class FirebaseVerifier:
    """Fresh verification in an owned process with a total ten-second deadline."""
    def __init__(self, config):
        self.credential_data = strict_json(private_file(config.values["firebaseAdminCredentialFile"]))
        require(type(self.credential_data) is dict, "admin_credential_rejected")
        require(self.credential_data.get("project_id") == config.values["firebaseProjectId"], "admin_project_rejected")
        self.project = config.values["firebaseProjectId"]
        self.uid = canonical_uid()
        self.closed = threading.Event()
        self.lock = threading.Lock()
        self.workers = set()

    def __call__(self, token):
        require(not self.closed.is_set(), "verifier_closed")
        context = multiprocessing.get_context("spawn")
        receiver, sender = context.Pipe(duplex=False)
        process = context.Process(target=_firebase_worker, args=(self.credential_data, self.project, self.uid, token, sender))
        try:
            with self.lock:
                require(not self.closed.is_set(), "verifier_closed")
                process.start()
                self.workers.add(process)
            sender.close()
            require(receiver.poll(REQUEST_SECONDS), "token_verification_timeout")
            response = strict_json(receiver.recv_bytes(1024), 1024)
            require(set(response) == {"exp"}, response.get("reason", "token_verification_failed"))
            return response
        except EntryError:
            raise
        except (OSError, EOFError, ValueError):
            raise EntryError("token_verification_failed") from None
        finally:
            self.stop_worker(process)
            with self.lock:
                self.workers.discard(process)
            receiver.close()
            sender.close()

    @staticmethod
    def stop_worker(process):
        if process.pid is not None:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                if process.is_alive():
                    process.kill()
            process.join(timeout=1)

    def close(self):
        self.closed.set()
        with self.lock:
            workers = list(self.workers)
            self.credential_data = None
        for process in workers:
            self.stop_worker(process)


def backend_request(config, token=None):
    """Only the fixed loopback Library admission endpoint can be reached."""
    parsed = origin(config.values["backendOrigin"])
    connection = None
    timer = None
    started = time.monotonic()
    try:
        constructor = http.client.HTTPSConnection if parsed.scheme == "https" else http.client.HTTPConnection
        connection = constructor(parsed.hostname, parsed.port, timeout=REQUEST_SECONDS)
        connection.connect()
        owned_socket = connection.sock
        def stop():
            try:
                owned_socket.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            finally:
                owned_socket.close()
        timer = threading.Timer(max(0, REQUEST_SECONDS - (time.monotonic() - started)), stop)
        timer.daemon = True
        timer.start()
        headers = {"Accept": "application/json"}
        if token is not None:
            headers["Authorization"] = "Bearer " + token
        connection.request("GET", "/api/test/library?lang=tr", headers=headers)
        response = connection.getresponse()
        require(response.getheader("Location") is None and response.status not in (301, 302, 303, 307, 308), "backend_redirect_rejected")
        content = response.read(65537)
        require(len(content) <= 65536 and time.monotonic() - started < REQUEST_SECONDS, "backend_response_size_rejected")
        if token is None:
            require(response.status in (200, 401, 403), "backend_probe_failed")
        else:
            require(response.status == 200 and response.getheader("Content-Type", "").split(";")[0] == "application/json",
                "backend_admission_rejected")
            require(type(strict_json(content)) is dict, "backend_admission_rejected")
        return True
    except EntryError:
        raise
    except Exception:
        raise EntryError("backend_unavailable") from None
    finally:
        if timer is not None:
            timer.cancel()
        if connection is not None:
            connection.close()


class Broker:
    """One bounded launch; the launcher pairs the key outside the network hello."""
    def __init__(self, config, manifest, verifier, backend=backend_request, clock=time.time, monotonic=time.monotonic, case_id=None):
        self.config, self.manifest, self.verifier, self.backend = config, manifest, verifier, backend
        self.clock, self.monotonic = clock, monotonic
        self.lock = threading.RLock()
        self.case_id = case_id or manifest.entries[0]["caseId"]
        require(self.case_id in manifest.objects, "unknown_case")
        self.reader = validate_reader_entries(config.values["batchId"], manifest.entries)
        require(not self.reader or self.case_id == READER_CASES[0], "reader_selection_rejected")
        self.allowed_cases = frozenset((entry["caseId"] for entry in manifest.entries) if self.reader else (self.case_id,))
        self.session_id = secrets.token_urlsafe(24)
        self.nonce = random_nonce()
        self.started = monotonic()
        self.deadline = int((clock() + 30) * 1000)
        self.paired = None
        self.challenge = None
        self.claimed = False
        self.admission_successful = False
        self.explicit_app_revoked = False
        self.fixture_delivery_count = 0
        self.fixture_delivery_bytes = 0
        self.fixture_delivery_sha256 = None
        self.fixture_deliveries = {}
        self.token = None
        self.expires_at = None
        self.reason = None
        self.probes = set()
        self.reachability_ready = threading.Event()
        self.active = set()
        self.generation = 0
        self.timer = threading.Timer(180, lambda: self.revoke("session_expired"))
        self.timer.daemon = True
        self.timer.start()

    def metadata(self):
        values = self.config.values
        return {"schemaVersion": 1, "scope": SCOPE, "batchId": values["batchId"],
            "sessionId": self.session_id, "launchNonce": self.nonce, "deadline": self.deadline,
            "expectedUid": canonical_uid(), "expectedAppId": values["expectedAppId"],
            "firebaseProjectId": values["firebaseProjectId"], "backendEnvironment": values["backendEnvironment"],
            "brokerOrigin": values["brokerOrigin"], "brokerCertificateDerBase64": values["brokerCertificateDerBase64"],
            "brokerCertificateSha256": values["brokerCertificateSha256"],
            "manifestSha256": self.manifest.private_sha256, "publicManifestSha256": self.manifest.public_sha256,
            "caseId": self.case_id}

    def launch_binding(self, session_id, nonce):
        require(session_id == self.session_id and nonce == self.nonce, "launch_binding_rejected")
        require(self.reason is None and self.monotonic() - self.started < 30 and
            int(self.clock() * 1000) < self.deadline, "bootstrap_expired")

    def probe(self, session_id, nonce, backend=False):
        with self.lock:
            self.launch_binding(session_id, nonce)
        if backend:
            self.backend(self.config)
        with self.lock:
            self.launch_binding(session_id, nonce)
            self.probes.add("backend" if backend else "broker")
            if self.probes == {"broker", "backend"}:
                self.reachability_ready.set()
        return {"status": "reachable", "scope": SCOPE, "batchId": self.config.values["batchId"],
            **({"reachability": "proxied"} if backend else {})}

    def pair_launcher(self, payload):
        exact_keys(payload, ("sessionId", "launchNonce", "publicKeySpkiBase64"))
        _, der = public_key(payload["publicKeySpkiBase64"])
        with self.lock:
            self.launch_binding(payload["sessionId"], payload["launchNonce"])
            require(self.probes == {"broker", "backend"}, "reachability_not_qualified")
            require(self.paired is None, "launcher_replacement_rejected")
            self.paired = der

    def paired_status(self, session_id, nonce):
        with self.lock:
            self.launch_binding(session_id, nonce)
            return {"paired": self.paired is not None,
                **({"publicKeySha256": sha256(self.paired).hexdigest()} if self.paired is not None else {})}

    def validate_binding(self, value, fields):
        exact_keys(value, fields)
        self.launch_binding(value["sessionId"], value["launchNonce"])
        require(value["scope"] == SCOPE and value["batchId"] == self.config.values["batchId"], "proof_binding_rejected")
        _, der = public_key(value["publicKeySpkiBase64"])
        require(self.paired is not None and hmac.compare_digest(self.paired, der), "unpaired_key")

    def make_challenge(self, value):
        with self.lock:
            self.validate_binding(value, BASE_FIELDS)
            require(self.challenge is None and not self.claimed, "challenge_replayed")
            self.challenge = random_nonce()
            return {name: value[name] for name in ("scope", "batchId", "sessionId", "launchNonce")} | {
                "challenge": self.challenge, "expiresAt": self.deadline}

    def claim(self, value):
        # Consumption and proof validation share a lock: exactly one concurrent claim wins.
        claim_started = self.monotonic()
        with self.lock:
            self.validate_binding(value, BASE_FIELDS | {"challenge", "signatureBase64"})
            require(not self.claimed and self.challenge is not None and value["challenge"] == self.challenge, "claim_replayed")
            verify_proof(value["publicKeySpkiBase64"], value["signatureBase64"], scope=value["scope"],
                batch_id=value["batchId"], session_id=value["sessionId"], launch_nonce=value["launchNonce"], challenge=value["challenge"])
            self.claimed = True
            self.challenge = None
        try:
            token_bytes = private_file(self.config.values["idTokenFile"], 8192)
            token = token_bytes.decode("ascii").strip()
            require(0 < len(token) <= 8192 and all(32 < ord(c) < 127 for c in token), "token_rejected")
            claims = self.verifier(token)
            with self.lock:
                self.launch_binding(value["sessionId"], value["launchNonce"])
                require(self.monotonic() - claim_started < REQUEST_SECONDS, "claim_deadline_exceeded")
            self.backend(self.config, token)
            with self.lock:
                self.launch_binding(value["sessionId"], value["launchNonce"])
                require(self.monotonic() - claim_started < REQUEST_SECONDS, "claim_deadline_exceeded")
                issued = int(self.clock() * 1000)
                expires = min(issued + 180000, int(claims["exp"] * 1000), int((self.clock() + 180 - (self.monotonic() - self.started)) * 1000))
                require(expires > issued, "session_expired")
                envelope = {"schemaVersion": 1, "scope": SCOPE, "batchId": self.config.values["batchId"],
                    "sessionId": self.session_id, "generation": self.generation, "issuedAt": issued,
                    "expiresAt": expires, "expectedUid": canonical_uid(), "accessAlias": secrets.token_urlsafe(24),
                    "backendEnvironment": self.config.values["backendEnvironment"],
                    "brokerOrigin": self.config.values["brokerOrigin"],
                    "brokerCertificateDerBase64": self.config.values["brokerCertificateDerBase64"],
                    "brokerCertificateSha256": self.config.values["brokerCertificateSha256"],
                    "manifestSha256": self.manifest.private_sha256, "publicManifestSha256": self.manifest.public_sha256,
                    "publicManifestBase64": base64.b64encode(self.manifest.public_bytes).decode("ascii"), "idToken": token}
                data = frozen_json(envelope)
                require(len(data) <= 65536, "envelope_size_rejected")
                self.token, self.expires_at = token, expires
                self.admission_successful = True
                return data
        except EntryError:
            self.revoke("admission_failed")
            raise
        except Exception:
            self.revoke("admission_failed")
            raise EntryError("admission_failed") from None

    def state(self):
        with self.lock:
            if self.expires_at is not None and int(self.clock() * 1000) >= self.expires_at:
                self.revoke("session_expired")
            if self.reason == "session_expired":
                return {"status": "expired", "reason": "session_expired"}
            if self.reason is not None:
                return {"status": "revoked", "reason": self.reason}
            require(self.token is not None, "session_not_admitted")
            return {"status": "admitted", "reason": "session_active"}

    def authorize(self, session_id, authorization):
        with self.lock:
            require(session_id == self.session_id and type(authorization) is str and authorization.startswith("Bearer ") and
                len(authorization) <= 8199, "session_auth_rejected")
            token = authorization[7:]
            # Revoked status stays observable with the original bearer digest, without retaining the token.
            expected = self.token_digest if hasattr(self, "token_digest") else sha256(self.token.encode()).digest() if self.token else b""
            require(hmac.compare_digest(expected, sha256(token.encode()).digest()), "session_auth_rejected")
            status = self.state()
        if status["status"] == "admitted":
            try:
                self.verifier(token)
            except Exception:
                self.revoke("token_verification_failed")
                return self.state()
            return self.state()
        return status

    def fixture(self, case_id, session_id, authorization):
        require(type(case_id) is str and ID.fullmatch(case_id), "case_rejected")
        status = self.authorize(session_id, authorization)
        require(status["status"] == "admitted", status["reason"])
        with self.lock:
            require(case_id in self.allowed_cases and case_id in self.manifest.objects, "unknown_case")
            require(self.state()["status"] == "admitted", "session_revoked")
            entry = next(item for item in self.manifest.entries if item["caseId"] == case_id)
            return entry, self.manifest.objects[case_id]

    def own(self, connection):
        with self.lock:
            require(self.state()["status"] == "admitted" and len(self.active) < 4, "transfer_capacity_rejected")
            self.active.add(connection)

    def release(self, connection):
        with self.lock:
            self.active.discard(connection)

    def completed_fixture(self, case_id, byte_count, digest):
        """Only a fully written selected immutable response is a delivery observation."""
        with self.lock:
            require(self.admission_successful and case_id in self.allowed_cases, "fixture_delivery_unverified")
            entry = next(item for item in self.manifest.entries if item["caseId"] == case_id)
            require(type(byte_count) is int and byte_count == entry["byteSize"] and digest == entry["outputSha256"],
                "fixture_delivery_unverified")
            # App teardown can race the last successful write after receiving all bytes.
            # Every other terminal state is rejected by the final outcome validator.
            require(self.reason in (None, "app_revoked"), "fixture_delivery_unverified")
            self.fixture_delivery_count += 1
            self.fixture_delivery_bytes = byte_count
            self.fixture_delivery_sha256 = digest
            delivery = self.fixture_deliveries.setdefault(case_id, {"count": 0, "bodyBytes": 0,
                "outputSha256": digest})
            delivery["count"] += 1
            delivery["bodyBytes"] += byte_count

    def revoke_from_app(self, session_id, authorization):
        """Retain positive teardown evidence only for active authenticated app revocation."""
        status = self.authorize(session_id, authorization)
        if status["status"] != "admitted":
            return status
        with self.lock:
            status = self.state()
            if status["status"] != "admitted":
                return status
            require(self.admission_successful, "session_not_admitted")
            self.explicit_app_revoked = True
            self.revoke("app_revoked")
            return self.state()

    def revoke(self, reason="host_revoked"):
        with self.lock:
            if self.reason is None:
                self.reason = reason
            if self.token is not None:
                self.token_digest = sha256(self.token.encode()).digest()
            self.token = None
            self.challenge = None
            self.paired = None
            self.reachability_ready.set()
            connections = list(self.active)
            self.active.clear()
        for connection in connections:
            try:
                connection.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            finally:
                connection.close()

    def close(self):
        self.revoke()
        self.timer.cancel()
        if threading.current_thread() is not self.timer:
            self.timer.join(timeout=1)
        self.manifest.objects.clear()
        close = getattr(self.verifier, "close", None)
        if close is not None:
            close()


class FixtureHTTPServer(ThreadingHTTPServer):
    daemon_threads = True
    block_on_close = False
    allow_reuse_address = False
    def __init__(self, address, broker, ssl_context):
        self.broker = broker
        self.slots = threading.BoundedSemaphore(4)
        self.connection_lock = threading.Lock()
        self.connections = set()
        self.workers = set()
        super().__init__(address, FixtureHandler)
        self.socket = ssl_context.wrap_socket(self.socket, server_side=True, do_handshake_on_connect=False)

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            request.close()
            return
        worker = None
        try:
            worker = threading.Thread(target=self.process_request_thread, args=(request, client_address), daemon=True)
            with self.connection_lock:
                self.connections.add(request)
                self.workers.add(worker)
            worker.start()
        except Exception:
            with self.connection_lock:
                self.connections.discard(request)
                self.workers.discard(worker)
            self.slots.release()
            raise

    def process_request_thread(self, request, client_address):
        def stop_request():
            try:
                request.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            finally:
                request.close()
        timer = threading.Timer(REQUEST_SECONDS, stop_request)
        timer.daemon = True
        timer.start()
        try:
            request.settimeout(REQUEST_SECONDS)
            request.do_handshake()
            super().process_request_thread(request, client_address)
        except (OSError, ssl.SSLError):
            self.shutdown_request(request)
        finally:
            timer.cancel()
            with self.connection_lock:
                self.connections.discard(request)
                self.workers.discard(threading.current_thread())
            self.slots.release()

    def server_close(self):
        deadline = getattr(self, "owned_cleanup_deadline", time.monotonic() + 15)
        with self.connection_lock:
            connections = list(self.connections)
            self.connections.clear()
            workers = list(self.workers)
        for connection in connections:
            try:
                connection.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            finally:
                connection.close()
        super().server_close()
        for worker in workers:
            worker.join(timeout=max(0, deadline - time.monotonic()))

    def handle_error(self, request, client_address):
        pass


class FixtureHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "FixtureEntry"
    sys_version = ""
    def log_message(self, *args):
        pass

    def send_error(self, code, message=None, explain=None):
        self.reply(code, {"reason": "request_rejected"})

    def reply(self, code, value, mime="application/json"):
        data = value if type(value) is bytes else frozen_json(value)
        self.send_response(code)
        self.send_header("Content-Type", mime)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(data)
        self.close_connection = True

    def one_header(self, name):
        values = self.headers.get_all(name, [])
        require(len(values) == 1, "header_rejected")
        return values[0]

    def body(self):
        require(self.headers.get("Transfer-Encoding") is None and
            self.one_header("Content-Type") == "application/json", "body_rejected")
        length = self.one_header("Content-Length")
        require(length.isascii() and length.isdigit() and 0 < int(length) <= 65536, "body_size_rejected")
        data = self.rfile.read(int(length))
        require(len(data) == int(length), "body_truncated")
        return strict_json(data)

    def dispatch(self, method):
        broker = self.server.broker
        try:
            require(len(self.path) <= 512 and len(self.headers.as_bytes()) <= 16384 and
                not self.headers.get("Expect") and self.one_header("Host") ==
                urlsplit(broker.config.values["brokerOrigin"]).netloc, "request_rejected")
            parsed = urlsplit(self.path)
            require(not parsed.scheme and not parsed.netloc and not parsed.fragment, "path_rejected")
            if method == "GET" and parsed.path == "/experiment/v1/bootstrap/paired":
                query = parse_qs(parsed.query, keep_blank_values=True, strict_parsing=True)
                exact_keys(query, ("sessionId", "launchNonce"))
                require(all(len(value) == 1 for value in query.values()), "query_rejected")
                result = broker.paired_status(query["sessionId"][0], query["launchNonce"][0])
            else:
                require(not parsed.query, "query_rejected")
                path = parsed.path
                if method == "GET" and path in ("/experiment/v1/probe", "/experiment/v1/backend-probe"):
                    require(self.headers.get("Authorization") is None, "probe_credentials_rejected")
                    result = broker.probe(self.one_header("X-Experiment-Session"),
                        self.one_header("X-Experiment-Launch-Nonce"), path.endswith("backend-probe"))
                elif method == "POST" and path == "/experiment/v1/bootstrap/challenge":
                    result = broker.make_challenge(self.body())
                elif method == "POST" and path == "/experiment/v1/bootstrap/claim":
                    result = broker.claim(self.body())
                elif method == "GET" and path == "/experiment/v1/session/status":
                    result = broker.authorize(self.one_header("X-Experiment-Session"), self.one_header("Authorization"))
                elif method == "POST" and path == "/experiment/v1/session/revoke":
                    exact_keys(self.body(), ())
                    result = broker.revoke_from_app(self.one_header("X-Experiment-Session"), self.one_header("Authorization"))
                else:
                    raise EntryError("path_rejected")
            self.reply(200, result)
        except EntryError as error:
            self.reply(403, {"reason": str(error)})
        except (ValueError, TypeError):
            self.reply(400, {"reason": "request_rejected"})
        except (OSError, socket.timeout):
            if urlsplit(self.path).path == "/experiment/v1/bootstrap/claim":
                broker.revoke("bootstrap_delivery_failed")
            self.close_connection = True

    def do_GET(self):
        parsed = urlsplit(self.path)
        parts = parsed.path.split("/")
        if len(parts) == 7 and parts[:4] == ["", "experiment", "v1", "batches"]:
            broker = self.server.broker
            sent = False
            try:
                require(len(self.path) <= 512 and len(self.headers.as_bytes()) <= 16384 and
                    not self.headers.get("Expect") and self.one_header("Host") ==
                    urlsplit(broker.config.values["brokerOrigin"]).netloc and
                    not parsed.query and not parsed.fragment and not parsed.netloc and not parsed.scheme and
                    parts[4] == broker.config.values["batchId"] and parts[5] == "objects", "path_rejected")
                entry, data = broker.fixture(parts[6], self.one_header("X-Experiment-Session"), self.one_header("Authorization"))
                broker.own(self.connection)
                self.send_response(200)
                self.send_header("Content-Type", entry["mime"])
                self.send_header("Content-Length", str(len(data)))
                self.send_header("Cache-Control", "no-store")
                self.send_header("Connection", "close")
                self.end_headers()
                sent = True
                deadline = time.monotonic() + REQUEST_SECONDS
                for offset in range(0, len(data), 65536):
                    require(broker.state()["status"] == "admitted" and time.monotonic() < deadline, "transfer_expired")
                    self.wfile.write(data[offset:offset + 65536])
                self.wfile.flush()
                broker.completed_fixture(parts[6], len(data), sha256(data).hexdigest())
                self.close_connection = True
            except EntryError as error:
                if not sent:
                    self.reply(403, {"reason": str(error)})
                self.close_connection = True
            except (OSError, socket.timeout):
                self.close_connection = True
            finally:
                broker.release(self.connection)
        else:
            self.dispatch("GET")

    def do_POST(self):
        self.dispatch("POST")


def create_server(config, broker):
    certificate_identity(config)
    parsed = origin(config.values["brokerOrigin"], broker=True)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    try:
        context.load_cert_chain(config.values["tlsCertificateFile"], config.values["tlsPrivateKeyFile"])
        return FixtureHTTPServer((parsed.hostname, parsed.port), broker, context)
    except (OSError, ssl.SSLError):
        raise EntryError("broker_unavailable") from None
