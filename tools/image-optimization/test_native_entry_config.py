"""Synthetic private-input and independent raster admission checks."""
import base64
from datetime import datetime, timedelta, timezone
from hashlib import sha256
from io import BytesIO
import ipaddress
import json
import os
from pathlib import Path
import socket
import tempfile
import unittest

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID
from PIL import Image

import native_entry_config as config


class PrivateInputs(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="native-entry-test-")
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name).resolve()
        os.chmod(self.base, 0o700)
        key = ec.generate_private_key(ec.SECP256R1())
        name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "localhost")])
        cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
            .serial_number(x509.random_serial_number()).not_valid_before(datetime.now(timezone.utc) - timedelta(minutes=1))
            .not_valid_after(datetime.now(timezone.utc) + timedelta(days=1))
            .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address("127.0.0.1")), x509.DNSName("localhost")]), critical=False)
            .sign(key, hashes.SHA256()))
        self.der = cert.public_bytes(serialization.Encoding.DER)
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", 0))
            port = probe.getsockname()[1]
        self.values = {"schemaVersion": 1, "scope": config.SCOPE, "batchId": "test_batch", "platform": "android",
            "mode": "release", "deviceAlias": "test_device", "deviceSelector": "synthetic-device",
            "expectedAppId": "com.tellpal.tellpal_mobile_poc.imageexperiment.release", "firebaseProjectId": "synthetic-project",
            "backendEnvironment": "local", "backendOrigin": "http://127.0.0.1:18080", "brokerOrigin": "https://127.0.0.1:" + str(port),
            "brokerCertificateDerBase64": base64.b64encode(self.der).decode(), "brokerCertificateSha256": sha256(self.der).hexdigest(),
            "backendStartupReceiptPath": str(self.file("backend.json", b"{}")),
            "idTokenFile": str(self.file("token", b"synthetic.id.token")),
            "firebaseAdminCredentialFile": str(self.file("admin.json", b'{"project_id":"synthetic-project"}')),
            "tlsCertificateFile": str(self.file("cert.pem", cert.public_bytes(serialization.Encoding.PEM))),
            "tlsPrivateKeyFile": str(self.file("key.pem", key.private_bytes(serialization.Encoding.PEM,
                serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))),
            "signingConfigFile": str(self.file("signing.properties", b"synthetic-only")),
            "privateOutputRoot": str(self.base / "output")}
        image = BytesIO()
        Image.new("RGB", (2, 3), (90, 100, 110)).save(image, format="PNG")
        self.image = image.getvalue()
        self.entry = {"caseId": "case_a", "role": "isolated_raster_fixture", "arm": "control",
            "sourceSha256": "0" * 64, "referenceSha256": "1" * 64, "outputSha256": sha256(self.image).hexdigest(),
            "mime": "image/png", "byteSize": len(self.image), "width": 2, "height": 3,
            "encoderRevision": "synthetic_encoder", "profileRevision": "synthetic_profile",
            "filePath": str(self.file("fixture.png", self.image)), "logicalSlotWidth": 40, "logicalSlotHeight": 50,
            "dpr": 2.0, "fit": "contain", "alignmentX": 0.0, "alignmentY": 0,
            "purposeAcceptanceReceipt": "synthetic_acceptance"}
        self.manifest = {"schemaVersion": 1, "scope": config.SCOPE, "batchId": "test_batch",
            "corpusSha256": "2" * 64, "referenceSha256": "1" * 64, "sourceSha256": "0" * 64, "entries": [self.entry]}
        self.values["manifestPath"] = str(self.file("manifest.json", config.frozen_json(self.manifest)))
        self.config_path = self.file("config.json", config.frozen_json(self.values))

    def file(self, name, data):
        path = self.base / name
        path.write_bytes(data)
        os.chmod(path, 0o600)
        return path

    def configuration(self):
        self.config_path.write_bytes(config.frozen_json(self.values))
        return config.load_config(self.config_path)

    def admitted_manifest(self):
        Path(self.values["manifestPath"]).write_bytes(config.frozen_json(self.manifest))
        return config.load_manifest(self.configuration())


class ConfigurationTests(PrivateInputs):
    def test_valid_config_has_no_identity_or_credentials_in_repr(self):
        loaded = self.configuration()
        self.assertEqual(loaded.values["scope"], config.SCOPE)
        self.assertNotIn("synthetic", repr(loaded))
        self.assertNotIn(str(self.base), repr(loaded))

    def test_unknown_duplicate_trailing_nonfinite_invalid_utf8_reject(self):
        for data in (b'{"a":1,"a":2}', b'{} {}', b'{"a":NaN}', b'{"a":1e999}', b'"\xff"'):
            with self.subTest(data=data), self.assertRaises(config.EntryError):
                config.strict_json(data)
        self.values["secret"] = "must-not-appear"
        with self.assertRaisesRegex(config.EntryError, "^schema_rejected$"):
            self.configuration()

    def test_boolean_schema_wrong_mode_identity_scope_and_public_origin_reject(self):
        for key, value in (("schemaVersion", True), ("mode", "debug"), ("expectedAppId", "com.tellpal.tellpal_mobile_poc"),
            ("scope", "old_scope"), ("backendEnvironment", "production"), ("backendOrigin", "https://example.com:443"),
            ("brokerOrigin", "https://example.com:443"), ("brokerOrigin", "https://user@127.0.0.1:443"),
            ("brokerOrigin", "https://127.0.0.1:443/"), ("brokerOrigin", "https://127.0.0.1:0")):
            original = self.values[key]
            self.values[key] = value
            with self.subTest(key=key, value=value), self.assertRaises(config.EntryError):
                self.configuration()
            self.values[key] = original

    def test_config_size_private_mode_regular_and_symlink_reject(self):
        self.config_path.write_bytes(b" " * 16385)
        with self.assertRaises(config.EntryError):
            config.load_config(self.config_path)
        self.configuration()
        os.chmod(self.config_path, 0o644)
        with self.assertRaises(config.EntryError):
            config.load_config(self.config_path)
        os.chmod(self.config_path, 0o600)
        symlink = self.base / "link"
        symlink.symlink_to(self.config_path)
        with self.assertRaisesRegex(config.EntryError, "symlink_rejected"):
            config.load_config(symlink)
        with self.assertRaises(config.EntryError):
            config.private_file(self.base)

    def test_private_output_unique_external_no_overlap(self):
        loaded = self.configuration()
        output = config.private_output(loaded.values["privateOutputRoot"], [self.config_path], create=True)
        self.assertEqual(output.stat().st_mode & 0o777, 0o700)
        with self.assertRaisesRegex(config.EntryError, "output_not_unique"):
            self.configuration()
        with self.assertRaises(config.EntryError):
            config.private_output(config.ROOT / "output", [])
        with self.assertRaisesRegex(config.EntryError, "output_input_overlap"):
            config.private_output(self.base / "nested", [self.base / "nested/file"])

    def test_mismatched_certificate_pin_rejects(self):
        self.values["brokerCertificateSha256"] = "0" * 64
        with self.assertRaisesRegex(config.EntryError, "pin_rejected"):
            self.configuration()


class ManifestTests(PrivateInputs):
    def test_exact_public_projection_hashes_frozen_bytes_and_snapshot_survives_path_change(self):
        admitted = self.admitted_manifest()
        self.assertNotIn(b"filePath", admitted.public_bytes)
        self.assertNotIn(str(self.base).encode(), admitted.public_bytes)
        self.assertEqual(admitted.public_sha256, sha256(admitted.public_bytes).hexdigest())
        Path(self.entry["filePath"]).write_bytes(b"changed")
        self.assertEqual(admitted.objects["case_a"], self.image)

    def test_changed_sha_size_mime_dimension_and_absent_admission_reject(self):
        for key, value in (("outputSha256", "3" * 64), ("byteSize", 1), ("mime", "image/jpeg"),
            ("width", 3), ("height", 9000), ("purposeAcceptanceReceipt", ""), ("width", True),
            ("dpr", True), ("logicalSlotWidth", 10 ** 1000), ("alignmentX", 1.01)):
            original = self.entry[key]
            self.entry[key] = value
            with self.subTest(key=key), self.assertRaises(config.EntryError):
                self.admitted_manifest()
            self.entry[key] = original

    def test_duplicate_ids_missing_fields_unknown_fields_and_capacity_reject(self):
        self.manifest["entries"] = [self.entry] * 2
        with self.assertRaisesRegex(config.EntryError, "case_rejected"):
            self.admitted_manifest()
        self.manifest["entries"] = [self.entry] * 21
        with self.assertRaisesRegex(config.EntryError, "manifest_capacity_rejected"):
            self.admitted_manifest()
        self.manifest["entries"] = [self.entry]
        self.entry["unknown"] = True
        with self.assertRaisesRegex(config.EntryError, "schema_rejected"):
            self.admitted_manifest()

    def test_alpha_orientation_and_animation_reject(self):
        cases = []
        alpha = BytesIO()
        Image.new("RGBA", (2, 3), (0, 0, 0, 128)).save(alpha, format="PNG")
        cases.append((alpha.getvalue(), "image/png"))
        orientation = BytesIO()
        exif = Image.Exif()
        exif[274] = 1
        Image.new("RGB", (2, 3)).save(orientation, format="JPEG", exif=exif)
        cases.append((orientation.getvalue(), "image/jpeg"))
        animation = BytesIO()
        Image.new("RGB", (2, 3)).save(animation, format="PNG", save_all=True,
            append_images=[Image.new("RGB", (2, 3), "red")], duration=100, loop=0)
        cases.append((animation.getvalue(), "image/png"))
        for content, mime in cases:
            Path(self.entry["filePath"]).write_bytes(content)
            self.entry.update(outputSha256=sha256(content).hexdigest(), byteSize=len(content), mime=mime)
            with self.subTest(mime=mime), self.assertRaises(config.EntryError):
                self.admitted_manifest()

    def test_fixture_symlink_or_public_permissions_reject(self):
        os.chmod(self.entry["filePath"], 0o644)
        with self.assertRaisesRegex(config.EntryError, "file_not_private"):
            self.admitted_manifest()
        os.chmod(self.entry["filePath"], 0o600)
        link = self.base / "fixture-link"
        link.symlink_to(self.entry["filePath"])
        self.entry["filePath"] = str(link)
        with self.assertRaisesRegex(config.EntryError, "symlink_rejected"):
            self.admitted_manifest()

    def test_independent_crc_validation_rejects_corruption_even_when_manifest_hash_matches(self):
        content = bytearray(self.image)
        chunk_type = content.index(b"IDAT")
        length = int.from_bytes(content[chunk_type - 4:chunk_type], "big")
        content[chunk_type + 4 + length] ^= 1
        content = bytes(content)
        Path(self.entry["filePath"]).write_bytes(content)
        self.entry.update(byteSize=len(content), outputSha256=sha256(content).hexdigest())
        with self.assertRaisesRegex(config.EntryError, "fixture_decode_rejected"):
            self.admitted_manifest()


if __name__ == "__main__":
    unittest.main()
