"""P-256 pairing proofs and exact TLS identities for the fixture broker."""
import base64
from datetime import datetime, timezone
from hashlib import sha256
import secrets
import socket
import ssl
from urllib.parse import urlsplit

from cryptography import x509
from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

from native_entry_config import EntryError, HASH, ID, SCOPE, origin, private_file, require

# Public cross-language test material. The scalar used to create this fixture is
# deliberately a known test constant and is never used for runtime pairing.
CRYPTO_VECTOR = {
    "scope": SCOPE, "batchId": "vector_batch", "sessionId": "vector_session",
    "launchNonce": "11" * 32, "challenge": "22" * 32,
    "publicKeySpkiBase64": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaxfR8uEsQkf4vOblY6RA8ncDfYEt6zOg9KE5RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7LtkBoN79R9Q==",
    "signatureBase64": "MEQCIAQ13SrX9he8nb5rlo7aWhB9DEcacbzNKMS6wr8xog4sAiAlRPElM4ZFIt2C6DDdv3fOhR2FFPx+kJ7ETRADBHnDow==",
    "publicKeySha256": "5cd252fb0ce8932436faf8ccd1040981b89ee4ad6b9fe9e2a2b7e71aacb27cd3",
}


def random_nonce():
    return secrets.token_hex(32)


def decode_base64(value, limit, code):
    require(type(value) is str and len(value) <= ((limit + 2) // 3) * 4, code)
    try:
        data = base64.b64decode(value, validate=True)
        require(0 < len(data) <= limit and base64.b64encode(data).decode("ascii") == value, code)
        return data
    except (ValueError, TypeError):
        raise EntryError(code) from None


def public_key(value):
    data = decode_base64(value, 256, "public_key_rejected")
    try:
        key = serialization.load_der_public_key(data)
        require(isinstance(key, ec.EllipticCurvePublicKey) and isinstance(key.curve, ec.SECP256R1), "public_key_rejected")
        require(key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo) == data,
            "public_key_rejected")
        return key, data
    except EntryError:
        raise
    except (ValueError, TypeError):
        raise EntryError("public_key_rejected") from None


def signed_payload(scope, batch_id, session_id, launch_nonce, challenge, spki_der):
    require(scope == SCOPE and type(batch_id) is str and ID.fullmatch(batch_id) and
        type(session_id) is str and ID.fullmatch(session_id), "proof_binding_rejected")
    require(type(launch_nonce) is str and HASH.fullmatch(launch_nonce) and
        type(challenge) is str and HASH.fullmatch(challenge), "proof_nonce_rejected")
    return ("\n".join(("tellpal-native-fixture-v1", scope, batch_id, session_id,
        launch_nonce, challenge, sha256(spki_der).hexdigest())) + "\n").encode("ascii")


def verify_proof(key_base64, signature_base64, *, scope, batch_id, session_id, launch_nonce, challenge):
    key, der = public_key(key_base64)
    signature = decode_base64(signature_base64, 128, "signature_rejected")
    payload = signed_payload(scope, batch_id, session_id, launch_nonce, challenge, der)
    try:
        key.verify(signature, payload, ec.ECDSA(hashes.SHA256()))
    except (InvalidSignature, ValueError):
        raise EntryError("proof_rejected") from None
    return sha256(der).hexdigest()


def certificate_identity(config):
    """Validate exact DER, validity, SAN hostname and private key before binding TLS."""
    values = config.values
    der = decode_base64(values["brokerCertificateDerBase64"], 8192, "certificate_rejected")
    try:
        certificate = x509.load_der_x509_certificate(der)
        pem_certificate = x509.load_pem_x509_certificate(private_file(values["tlsCertificateFile"]))
        require(pem_certificate.public_bytes(serialization.Encoding.DER) == der and
            sha256(der).hexdigest() == values["brokerCertificateSha256"], "certificate_pin_rejected")
        now = datetime.now(timezone.utc)
        require(certificate.not_valid_before_utc <= now < certificate.not_valid_after_utc, "certificate_expired")
        # Python's TLS verifier performs the actual SAN/hostname validation per connection.
        context = ssl.create_default_context(cadata=certificate.public_bytes(serialization.Encoding.PEM).decode("ascii"))
        context.verify_flags |= getattr(ssl, "VERIFY_X509_PARTIAL_CHAIN", 0)
        context.check_hostname = True
        key = serialization.load_pem_private_key(private_file(values["tlsPrivateKeyFile"]), password=None)
        require(key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo) ==
            certificate.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo),
            "certificate_key_rejected")
        host = origin(values["brokerOrigin"], broker=True).hostname
        extension = certificate.extensions.get_extension_for_class(x509.SubjectAlternativeName).value
        import ipaddress
        try:
            expected = ipaddress.ip_address(host)
            require(expected in extension.get_values_for_type(x509.IPAddress), "certificate_hostname_rejected")
        except ValueError:
            require(host in extension.get_values_for_type(x509.DNSName), "certificate_hostname_rejected")
        return certificate, context
    except EntryError:
        raise
    except Exception:
        raise EntryError("certificate_rejected") from None


def pinned_connect(broker_origin, certificate_der, certificate_sha256, timeout=10):
    """Return a verified socket only after pin checking, before any HTTP header."""
    parsed = origin(broker_origin, broker=True)
    require(sha256(certificate_der).hexdigest() == certificate_sha256, "certificate_pin_rejected")
    raw = None
    tls = None
    try:
        pem = ssl.DER_cert_to_PEM_cert(certificate_der)
        context = ssl.create_default_context(cadata=pem)
        context.verify_flags |= getattr(ssl, "VERIFY_X509_PARTIAL_CHAIN", 0)
        raw = socket.create_connection((parsed.hostname, parsed.port), timeout=timeout)
        tls = context.wrap_socket(raw, server_hostname=parsed.hostname)
        raw = None
        require(sha256(tls.getpeercert(binary_form=True)).hexdigest() == certificate_sha256,
            "certificate_pin_rejected")
        return tls
    except EntryError:
        if tls is not None:
            tls.close()
        raise
    except (OSError, ValueError):
        if tls is not None:
            tls.close()
        raise EntryError("tls_rejected") from None
    finally:
        if raw is not None:
            raw.close()
