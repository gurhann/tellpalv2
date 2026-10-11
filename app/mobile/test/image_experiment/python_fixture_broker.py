"""Synthetic-only Dart interoperability fixture using the real broker HTTP handler."""
import json
from pathlib import Path
import ssl
import sys
import threading
import time
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "tools/image-optimization"))
from native_entry_config import require
from native_fixture_server import FixtureHTTPServer


class SyntheticBroker:
    def __init__(self, data):
        self.config = SimpleNamespace(values={"brokerOrigin": "https://localhost:1"})
        self.data = data
        self.active = True

    def authorize(self, session, authorization):
        require(self.active and session == self.data["session"] and
                authorization == "Bearer " + self.data["token"], "synthetic_auth_refused")
        return {"status": "admitted", "reason": "none"}

    def revoke_from_app(self, session, authorization):
        self.authorize(session, authorization)
        self.active = False
        return {"status": "revoked", "reason": "app_revoked"}


def main():
    # Only generated synthetic TLS material/token arrives over private stdin.
    data = json.loads(sys.stdin.readline())
    broker = SyntheticBroker(data)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(data["certificate"], data["key"])
    server = FixtureHTTPServer(("127.0.0.1", 0), broker, context)
    broker.config.values["brokerOrigin"] = f"https://localhost:{server.server_port}"
    worker = threading.Thread(target=server.serve_forever, kwargs={"poll_interval": 0.05}, daemon=True)
    worker.start()
    print(json.dumps({"port": server.server_port}), flush=True)
    try:
        require(sys.stdin.readline().strip() == "stop", "synthetic_stop_refused")
    finally:
        server.owned_cleanup_deadline = time.monotonic() + 2
        server.shutdown()
        server.server_close()
        worker.join(timeout=2)


if __name__ == "__main__":
    main()
