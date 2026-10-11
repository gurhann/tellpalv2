"""Shared crypto vectors and pin-before-header TLS checks."""
import base64
from hashlib import sha256
import socket
import ssl
import threading
import unittest

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

from native_entry_config import EntryError
import native_entry_crypto as crypto
from test_native_entry_config import PrivateInputs


class ProofTests(unittest.TestCase):
    def verify(self, vector):
        return crypto.verify_proof(vector["publicKeySpkiBase64"], vector["signatureBase64"], scope=vector["scope"],
            batch_id=vector["batchId"], session_id=vector["sessionId"], launch_nonce=vector["launchNonce"], challenge=vector["challenge"])

    def test_shared_positive_vector_exact_payload(self):
        vector = crypto.CRYPTO_VECTOR
        key, der = crypto.public_key(vector["publicKeySpkiBase64"])
        payload = crypto.signed_payload(vector["scope"], vector["batchId"], vector["sessionId"], vector["launchNonce"], vector["challenge"], der)
        self.assertEqual(payload.count(b"\n"), 7)
        self.assertTrue(payload.endswith((vector["publicKeySha256"] + "\n").encode()))
        self.assertEqual(self.verify(vector), vector["publicKeySha256"])

    def test_shared_negative_signature_and_each_binding_mutation(self):
        for field, value in (("batchId", "changed"), ("sessionId", "changed"), ("launchNonce", "3" * 64),
            ("challenge", "2" * 63 + "3"), ("scope", "other"), ("signatureBase64", "AAAA")):
            vector = {**crypto.CRYPTO_VECTOR, field: value}
            with self.subTest(field=field), self.assertRaises(EntryError):
                self.verify(vector)

    def test_malformed_der_wrong_curve_key_replacement_and_bounds(self):
        for curve in (ec.SECP384R1(), ec.SECP256R1()):
            key = ec.generate_private_key(curve)
            vector = {**crypto.CRYPTO_VECTOR, "publicKeySpkiBase64": base64.b64encode(key.public_key().public_bytes(
                serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)).decode()}
            with self.assertRaises(EntryError):
                self.verify(vector)
        for value in ("AAAA", "!", base64.b64encode(b"x" * 257).decode()):
            with self.assertRaises(EntryError):
                crypto.public_key(value)
        with self.assertRaises(EntryError):
            crypto.decode_base64(base64.b64encode(b"x" * 129).decode(), 128, "signature_rejected")

    def test_nonce_is_32_random_bytes_lowercase_hex(self):
        first, second = crypto.random_nonce(), crypto.random_nonce()
        self.assertRegex(first, r"^[0-9a-f]{64}$")
        self.assertNotEqual(first, second)


class TLSTests(PrivateInputs):
    def server(self):
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        listener.listen(1)
        listener.settimeout(2)
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(self.values["tlsCertificateFile"], self.values["tlsPrivateKeyFile"])
        self.received = []
        def serve():
            try:
                connection, _ = listener.accept()
                connection.settimeout(1)
                with context.wrap_socket(connection, server_side=True) as secure:
                    try:
                        self.received.append(secure.recv(1024))
                    except OSError:
                        self.received.append(b"")
            except (OSError, ssl.SSLError):
                pass
            finally:
                listener.close()
        worker = threading.Thread(target=serve, daemon=True)
        worker.start()
        self.addCleanup(lambda: worker.join(timeout=3))
        self.addCleanup(listener.close)
        return "https://127.0.0.1:" + str(listener.getsockname()[1]), worker

    def test_exact_certificate_validity_hostname_and_private_key(self):
        certificate, context = crypto.certificate_identity(self.configuration())
        self.assertTrue(context.check_hostname)
        self.assertEqual(certificate.public_bytes(serialization.Encoding.DER), self.der)
        self.values["brokerOrigin"] = self.values["brokerOrigin"].replace("127.0.0.1", "127.0.0.2")
        with self.assertRaisesRegex(EntryError, "certificate_hostname_rejected"):
            crypto.certificate_identity(self.configuration())

    def test_pinned_tls_socket_returns_before_any_http_header_is_sent(self):
        address, worker = self.server()
        connection = crypto.pinned_connect(address, self.der, sha256(self.der).hexdigest(), timeout=2)
        self.assertEqual(self.received, [])
        connection.sendall(b"GET /synthetic HTTP/1.1\r\n\r\n")
        connection.close()
        worker.join(timeout=3)
        self.assertEqual(self.received, [b"GET /synthetic HTTP/1.1\r\n\r\n"])

    def test_wrong_pin_cannot_send_bearer_or_any_bytes(self):
        address, worker = self.server()
        with self.assertRaisesRegex(EntryError, "certificate_pin_rejected"):
            crypto.pinned_connect(address, self.der, "0" * 64, timeout=1)
        worker.join(timeout=3)
        self.assertEqual(self.received, [])


if __name__ == "__main__":
    unittest.main()
