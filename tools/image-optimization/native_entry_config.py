"""Finite, private inputs for the isolated fixture entry. Errors contain codes only."""
from dataclasses import dataclass, field
from hashlib import sha256
import base64
import ipaddress
import json
import math
import os
from pathlib import Path
import re
import stat
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[2]
SCOPE = "image-optimization-native-fixture-r1"
ID = re.compile(r"[A-Za-z0-9_-]{1,64}")
HASH = re.compile(r"[0-9a-f]{64}")
MAX_FILE = 20 * 1024 * 1024
MAX_TOTAL = 100 * 1024 * 1024
MAX_PIXELS = 16_777_216
MAX_BACKEND_RECEIPT_BYTES = 256 * 1024
MAX_BACKEND_SOURCE_FILES = 1024
CONFIG_KEYS = frozenset(("schemaVersion", "scope", "batchId", "platform", "mode",
    "deviceAlias", "deviceSelector", "expectedAppId", "firebaseProjectId",
    "backendEnvironment", "backendOrigin", "backendStartupReceiptPath", "brokerOrigin",
    "brokerCertificateDerBase64", "brokerCertificateSha256", "manifestPath", "idTokenFile",
    "firebaseAdminCredentialFile", "tlsCertificateFile", "tlsPrivateKeyFile",
    "signingConfigFile", "privateOutputRoot"))
ENTRY_KEYS = frozenset(("caseId", "role", "arm", "sourceSha256", "referenceSha256",
    "outputSha256", "mime", "byteSize", "width", "height", "encoderRevision",
    "profileRevision", "filePath", "logicalSlotWidth", "logicalSlotHeight", "dpr",
    "fit", "alignmentX", "alignmentY", "purposeAcceptanceReceipt"))
MANIFEST_KEYS = frozenset(("schemaVersion", "scope", "batchId", "corpusSha256",
    "referenceSha256", "sourceSha256", "entries"))
INPUT_KEYS = ("backendStartupReceiptPath", "manifestPath", "idTokenFile",
    "firebaseAdminCredentialFile", "tlsCertificateFile", "tlsPrivateKeyFile", "signingConfigFile")


class EntryError(ValueError):
    """A fixed public refusal code; no nested exception is retained."""


def require(condition, code):
    if not condition:
        raise EntryError(code)


def finite_number(value):
    if type(value) not in (int, float):
        return False
    try:
        return math.isfinite(value)
    except (OverflowError, ValueError):
        return False


def exact_keys(value, keys):
    require(type(value) is dict and set(value) == set(keys), "schema_rejected")


def strict_json(data, limit=65536):
    require(type(data) is bytes and len(data) <= limit, "json_size_rejected")
    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, "duplicate_key")
            result[key] = value
        return result
    def finite(value):
        result = float(value)
        require(math.isfinite(result), "nonfinite_number")
        return result
    try:
        return json.loads(data.decode("utf-8", "strict"), object_pairs_hook=pairs,
            parse_float=finite, parse_constant=lambda _: require(False, "nonfinite_number"))
    except EntryError:
        raise
    except (UnicodeError, ValueError, RecursionError, OverflowError):
        raise EntryError("json_rejected") from None


def frozen_json(value):
    return json.dumps(value, separators=(",", ":"), ensure_ascii=True, allow_nan=False).encode("utf-8")


def no_symlink(path):
    require(type(path) in (str, Path) or isinstance(path, Path), "path_rejected")
    require(isinstance(path, Path) or isinstance(path, str) and "\x00" not in path, "path_rejected")
    path = Path(path)
    require(path.is_absolute() and ".." not in path.parts, "path_rejected")
    try:
        for item in (path, *path.parents):
            require(not item.is_symlink(), "symlink_rejected")
        require(not path.resolve().is_relative_to(ROOT.resolve()), "repository_path_rejected")
        return path
    except OSError:
        raise EntryError("path_unavailable") from None


def open_nofollow(path, flags, mode=0o600):
    """Walk stable directory descriptors so an ancestor swap cannot redirect an open."""
    path = no_symlink(path)
    parent = os.open("/", os.O_RDONLY | os.O_DIRECTORY)
    try:
        for component in path.parts[1:-1]:
            next_parent = os.open(component, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent)
            os.close(parent)
            parent = next_parent
        return os.open(path.name, flags | os.O_NOFOLLOW, mode, dir_fd=parent)
    finally:
        os.close(parent)


def private_file(path, limit=65536, exact_mode=True):
    path = no_symlink(path)
    descriptor = None
    try:
        descriptor = open_nofollow(path, os.O_RDONLY | os.O_NONBLOCK)
        info = os.fstat(descriptor)
        require(stat.S_ISREG(info.st_mode) and info.st_uid == os.getuid(), "file_rejected")
        permissions = stat.S_IMODE(info.st_mode)
        require(permissions == 0o600 if exact_mode else permissions in (0o400, 0o600), "file_not_private")
        require(info.st_size <= limit, "file_size_rejected")
        with os.fdopen(descriptor, "rb") as handle:
            descriptor = None
            data = handle.read(limit + 1)
        require(len(data) <= limit, "file_size_rejected")
        return data
    except OSError:
        raise EntryError("file_unavailable") from None
    finally:
        if descriptor is not None:
            os.close(descriptor)


def private_output(path, inputs, create=False):
    path = no_symlink(path)
    try:
        require(not path.exists(), "output_not_unique")
        parent = path.parent
        parent_descriptor = open_nofollow(parent, os.O_RDONLY | os.O_DIRECTORY)
        try:
            info = os.fstat(parent_descriptor)
        finally:
            os.close(parent_descriptor)
        require(stat.S_ISDIR(info.st_mode) and info.st_uid == os.getuid() and
            stat.S_IMODE(info.st_mode) == 0o700, "output_parent_not_private")
        for item in inputs:
            source = no_symlink(item)
            require(not source.is_relative_to(path) and not path.is_relative_to(source), "output_input_overlap")
        if create:
            parent_descriptor = open_nofollow(parent, os.O_RDONLY | os.O_DIRECTORY)
            try:
                os.mkdir(path.name, mode=0o700, dir_fd=parent_descriptor)
            finally:
                os.close(parent_descriptor)
        return path
    except OSError:
        raise EntryError("output_unavailable") from None


def write_private(path, value):
    data = value if type(value) is bytes else frozen_json(value) + b"\n"
    try:
        descriptor = open_nofollow(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL)
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(data)
    except OSError:
        raise EntryError("receipt_write_failed") from None


def origin(value, *, broker=False):
    require(type(value) is str and len(value) <= 512, "origin_rejected")
    try:
        parsed = urlsplit(value)
        require(parsed.scheme == ("https" if broker else parsed.scheme) and
            parsed.scheme in (("https",) if broker else ("http", "https")) and
            parsed.hostname is not None and parsed.username is None and parsed.password is None and
            parsed.path == "" and not parsed.query and not parsed.fragment,
            "origin_rejected")
        require(parsed.port is not None and 1 <= parsed.port <= 65535, "port_rejected")
        host = parsed.hostname
        try:
            address = ipaddress.ip_address(host)
        except ValueError:
            address = None
        if broker:
            require(host == "localhost" or address is not None and
                (address.is_private or address.is_loopback) and not address.is_unspecified and not address.is_multicast,
                "broker_host_rejected")
        else:
            require(host == "localhost" or address is not None and address.is_loopback, "backend_not_loopback")
        return parsed
    except (ValueError, OverflowError):
        raise EntryError("origin_rejected") from None


def canonical_uid():
    try:
        value = strict_json((ROOT / "tools/mobile-test-account.json").read_bytes())
        uid = value.get("firebaseUid")
        require(type(uid) is str and len(uid) <= 128, "qa_selection_rejected")
        return uid
    except OSError:
        raise EntryError("qa_selection_unavailable") from None


@dataclass(frozen=True, repr=False)
class Configuration:
    values: dict = field(repr=False)
    config_sha256: str


def load_config(path):
    data = private_file(path, 16384)
    value = strict_json(data, 16384)
    exact_keys(value, CONFIG_KEYS)
    require(type(value["schemaVersion"]) is int and value["schemaVersion"] == 1 and
        value["scope"] == SCOPE, "scope_rejected")
    for name in ("batchId", "deviceAlias"):
        require(type(value[name]) is str and ID.fullmatch(value[name]), "id_rejected")
    require(value["platform"] in ("ios", "android") and value["mode"] in ("profile", "release"), "mode_rejected")
    require(value["backendEnvironment"] in ("local", "test"), "environment_rejected")
    require(type(value["deviceSelector"]) is str and 1 <= len(value["deviceSelector"]) <= 256 and
        all(32 < ord(c) < 127 for c in value["deviceSelector"]), "selector_rejected")
    require(type(value["firebaseProjectId"]) is str and
        re.fullmatch(r"[a-z][a-z0-9-]{4,62}", value["firebaseProjectId"]), "project_rejected")
    prefix = "com.tellpal.tellpalMobilePoc.imageexperiment" if value["platform"] == "ios" else "com.tellpal.tellpal_mobile_poc.imageexperiment"
    require(value["expectedAppId"] == prefix + "." + value["mode"], "app_identity_rejected")
    origin(value["brokerOrigin"], broker=True)
    origin(value["backendOrigin"])
    require(type(value["brokerCertificateSha256"]) is str and HASH.fullmatch(value["brokerCertificateSha256"]), "pin_rejected")
    try:
        cert = base64.b64decode(value["brokerCertificateDerBase64"], validate=True)
        require(0 < len(cert) <= 8192 and sha256(cert).hexdigest() == value["brokerCertificateSha256"], "pin_rejected")
    except EntryError:
        raise
    except (ValueError, TypeError):
        raise EntryError("certificate_rejected") from None
    inputs = [path]
    for name in INPUT_KEYS:
        limit = (MAX_TOTAL if name == "manifestPath" else 8192 if name == "idTokenFile" else
            MAX_BACKEND_RECEIPT_BYTES if name == "backendStartupReceiptPath" else 65536)
        private_file(value[name], limit)
        inputs.append(value[name])
    private_output(value["privateOutputRoot"], inputs)
    return Configuration(value, sha256(data).hexdigest())


@dataclass(frozen=True, repr=False)
class Manifest:
    entries: tuple
    objects: dict = field(repr=False)
    private_sha256: str
    public_bytes: bytes

    @property
    def public_sha256(self):
        return sha256(self.public_bytes).hexdigest()


def load_manifest(config):
    from io import BytesIO
    from PIL import Image
    data = private_file(config.values["manifestPath"], MAX_TOTAL)
    value = strict_json(data, MAX_TOTAL)
    exact_keys(value, MANIFEST_KEYS)
    require(type(value["schemaVersion"]) is int and value["schemaVersion"] == 1 and
        value["scope"] == SCOPE and value["batchId"] == config.values["batchId"], "manifest_binding_rejected")
    for name in ("corpusSha256", "referenceSha256", "sourceSha256"):
        require(type(value[name]) is str and HASH.fullmatch(value[name]), "manifest_hash_rejected")
    require(type(value["entries"]) is list and 1 <= len(value["entries"]) <= 20, "manifest_capacity_rejected")
    objects, public_entries, total = {}, [], 0
    for entry in value["entries"]:
        exact_keys(entry, ENTRY_KEYS)
        case = entry["caseId"]
        require(type(case) is str and ID.fullmatch(case) and case not in objects, "case_rejected")
        require(entry["role"] == "isolated_raster_fixture" and entry["arm"] in ("control", "candidate") and
            entry["fit"] in ("contain", "cover"), "fixture_role_rejected")
        for name in ("sourceSha256", "referenceSha256", "outputSha256"):
            require(type(entry[name]) is str and HASH.fullmatch(entry[name]), "fixture_hash_rejected")
        for name in ("encoderRevision", "profileRevision", "purposeAcceptanceReceipt"):
            require(type(entry[name]) is str and ID.fullmatch(entry[name]), "fixture_admission_rejected")
        for name in ("width", "height", "byteSize"):
            require(type(entry[name]) is int and 1 <= entry[name] <= (MAX_FILE if name == "byteSize" else 8192), "fixture_geometry_rejected")
        require(entry["width"] * entry["height"] <= MAX_PIXELS, "fixture_pixels_rejected")
        for name in ("logicalSlotWidth", "logicalSlotHeight", "dpr", "alignmentX", "alignmentY"):
            numeric = entry[name]
            require(finite_number(numeric) and
                (-1 <= numeric <= 1 if name.startswith("alignment") else numeric > 0), "fixture_slot_rejected")
        require(entry["mime"] in ("image/jpeg", "image/png", "image/webp"), "fixture_mime_rejected")
        content = private_file(entry["filePath"], MAX_FILE, exact_mode=False)
        total += len(content)
        require(total <= MAX_TOTAL and len(content) == entry["byteSize"] and
            sha256(content).hexdigest() == entry["outputSha256"], "fixture_bytes_rejected")
        try:
            with Image.open(BytesIO(content)) as structure:
                require(structure.size == (entry["width"], entry["height"]), "fixture_format_rejected")
                structure.verify()
            with Image.open(BytesIO(content)) as image:
                require({"JPEG": "image/jpeg", "PNG": "image/png", "WEBP": "image/webp"}.get(image.format) == entry["mime"] and
                    image.size == (entry["width"], entry["height"]), "fixture_format_rejected")
                require(getattr(image, "n_frames", 1) == 1 and not getattr(image, "is_animated", False) and
                    274 not in image.getexif(), "fixture_metadata_rejected")
                image.load()
                if "A" in image.getbands() or "transparency" in image.info:
                    require(image.convert("RGBA").getchannel("A").getextrema() == (255, 255), "fixture_alpha_rejected")
        except EntryError:
            raise
        except Exception:
            raise EntryError("fixture_decode_rejected") from None
        objects[case] = content
        public_entries.append({key: val for key, val in entry.items() if key != "filePath"})
    private_output(config.values["privateOutputRoot"], [entry["filePath"] for entry in value["entries"]])
    public = frozen_json({**value, "entries": public_entries})
    require(len(public) <= 32768, "public_manifest_size_rejected")
    return Manifest(tuple(public_entries), objects, sha256(data).hexdigest(), public)
