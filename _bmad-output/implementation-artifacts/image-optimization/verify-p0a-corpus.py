#!/usr/bin/env python3
"""Deterministic, privacy-preserving Story 1.2 corpus/protocol verifier."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import platform
import re
import struct
import subprocess
import sys
from pathlib import Path

try:
    from PIL import Image, ImageCms
except ImportError:  # pragma: no cover
    Image = ImageCms = None

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
CORPUS = HERE / "p0a-corpus.json"
PROTOCOL = HERE / "p0a-trial-protocol.md"
SHA = re.compile(r"^[0-9a-f]{64}$")
MAX_BYTES = 20 * 1024 * 1024
MAX_PIXELS = 40_000_000
MAGIC = b"tellpal-p0a-rgba8-srgb-straight-v1\0"
REQUIRED_CHECKS = {"exif-orientation-oracle-1-8", "straight-alpha-hidden-rgb", "palette-rgba-expansion", "icc-display-p3-oracle", "icc-nonzero-black-bpc-oracle"}
MAX_RETAINED_ORIGINALS = 60
MAX_STORY_COUNT = 4
REQUIRED_CLASSES = {"title_bearing_cover", "ordinary_page", "text_dense_page", "large_illustration", "fine_lines", "gradients", "dark_art", "real_alpha", "exif_rotation", "exif_mirroring", "icc_tagged_color"}
CURRENT_PILOT_CLASSES = {"real_text_diacritics", "icc_color_conversion"}
EXCLUDED_PILOT_CLASSES = {"real_alpha", "exif_rotation", "exif_mirroring"}
PILOT_SCOPE_ID = "opaque-no-exif-orientation-v1"
FREEZE_REVISION_ID = "p0a-story-1-2-2026-10-10-r3"
CORPUS_SNAPSHOT_ID = "corpus-p0a-2026-10-10-r3"
PROTOCOL_SNAPSHOT_ID = "protocol-p0a-2026-10-10-r3"
EXPERIMENTAL_PURPOSE = "source_resolution_codec_feasibility"
SELECTED_BINDINGS = {
    "binding-content71-page1-tr-reading": ("24d27b3384c5ecf706f1c4458bc3bc869a2e55e85e4ca46e449a4e5dbdcbe075", "1fbbd6f4f5cfcb56dd62cd60a779ff8cd772921f7e7a5eaa7514e3d6f1ad26c6", 1712, 2778),
    "binding-content19-page1-tr-reading": ("1b21f7041b76d401a70447a3da7f72ffa1f8149b5508c588b7107d79d2d80169", "03802a378dc106e6af5505e94b72608b3f09e20ac6cadf4a606c4b8546f8fef2", 1284, 2279),
}
INDEPENDENT_REPORT_SHA = "8a6692fb7c27606100d2297181065e040870b4fe64e179337f6c6fbd32b2bd29"
INDEPENDENT_CHECKER_SHA = "cb8f0a6d7d17a3948b33a84ae6d3a18d1475d7e7dd115d8ddb73071b8548edec"
PREPARATION_RECEIPT_SHA = "8c4dd77fb14f0d1448cf98ef623098b3da5b01743afc83c60b384085481b1f8d"
NORMALIZATION_IMPLEMENTATION_SHA = "3ad752f0d8c04728630b65dbd90ff3c94c84119d7dc663932ba024f1fad82d40"
PAIR_KEY_FIELDS = ["seed", "source_sha256", "binding_id", "role", "geometry", "control_settings", "candidate_settings", "experimental_purpose", "source_owner_role", "reference_pixel_sha256", "freeze_revision_id", "provider_discovery_sha256"]
FUTURE_GATES = {"FQ-REAL-ALPHA": "real_alpha", "FQ-EXIF-ROTATION": "exif_rotation", "FQ-EXIF-MIRRORING": "exif_mirroring"}
PILOT_METADATA_FIELDS = ["frames", "has_alpha", "alpha_extrema", "exif.orientation", "icc"]
PIL_MIME = {"JPEG": "image/jpeg", "PNG": "image/png", "WEBP": "image/webp", "GIF": "image/gif"}
REQUIRED_COVERAGE = {
    "reading-cover-tr-title-bearing": ("title_bearing_cover", True, "tr"),
    "reading-cover-en-title-bearing": ("title_bearing_cover", True, "en"),
    "reading-cover-pt-title-bearing": ("title_bearing_cover", True, "pt"),
    "reading-page-ordinary": ("ordinary_page", True, "*"),
    "reading-page-text-dense": ("text_dense_page", True, "*"),
    "reading-page-large-illustration": ("large_illustration", True, "*"),
    "reading-page-fine-lines": ("fine_lines", True, "*"),
    "reading-page-gradients": ("gradients", True, "*"),
    "reading-page-dark-art": ("dark_art", True, "*"),
    "static-listening-alpha": ("real_alpha", True, None),
    "reading-page-exif-rotation": ("exif_rotation", True, "*"),
    "reading-page-exif-mirroring": ("exif_mirroring", True, "*"),
    "reading-page-icc": ("icc_tagged_color", True, "*"),
    "animation": ("animation", False, None),
    "non-story": ("non_story", False, None),
}


# Selective immutable r2 identities preserve ownership without freezing unknown metadata.
SPECIMEN_IDENTITY_FIELDS = ('id', 'kind', 'content_id', 'asset_id', 'page_number', 'language', 'roles', 'bindings')
BINDING_IDENTITY_FIELDS = ('id', 'content_id', 'asset_id', 'page_number', 'language', 'role', 'specimen_id', 'source_owner_role')
RETAINED_SPECIMEN_IDENTITIES_SHA = 'd34d531a0044374e798ad431d2a0321b6233fb739a86d9b1cd8893f7119245c5'
RETAINED_BINDING_IDENTITIES_SHA = '9c1370a0803e8a08ee999d5a968096376fc06ad33959134adaf7749306f64ba8'
RETAINED_SOURCE_IDENTITIES = {
    'cms-content71-page1-asset9816': '24d27b3384c5ecf706f1c4458bc3bc869a2e55e85e4ca46e449a4e5dbdcbe075',
    'cms-content71-cover-source-9888': '78ea1f1d05beefa3a2b35c2327a9ef7358c8894bcc5374c97a9399ac7a8982fc',
    'cms-content71-cover-tr-9778': '03f4ed692f96678afb1a0aeab03bc58ab97b25020b417a51b6958adbce199c6e',
    'cms-content71-cover-en-9779': '810bde841ccb4993b02f9077796395399f49564fa8051b38afd31d46039453cb',
    'cms-content71-cover-pt-9777': '07efde8e10e5b567dc90e1a670ddcb4cf1813b79398da4cc73b6e2a4b44bd5e6',
    'cms-content71-listening-artwork-11236': '2acf5576eef722858098c35f346448b45539e70c21a784468fd2a77d32ca3700',
    'cms-content70-listening-artwork-11234': 'ce099897dec565353df43f8c32ca64b80c91edc2e40a472a8b270714c8f63093',
    'cms-content70-cover-en-9585': 'a1dadc3fbbf62e16a286ff5bec63ce3aae10c9895aa6de4d2b592eb6fc0df4d2',
    'cms-content70-page1-asset9629': '2d055dc035a1b43e13f9296c00ff1154e81e2ff55273966f5499fb9711421a95',
    'cms-content70-page10-asset9647': 'a94322b6f584aba6ff4cfff61552367f0f9e2ab799b8449f2da7ba8ef2644336',
    'cms-content19-listening-artwork-11193': 'c7e39fe3e86e7cf5cf8498d8b6ca7b19bb8ea38cbfa1e37052bdb79f40d6a193',
    'cms-content19-page1-asset2037': '1b21f7041b76d401a70447a3da7f72ffa1f8149b5508c588b7107d79d2d80169',
    'cms-content71-asset9889-page1': '448b8185ea16b6ddb321bba575fb62fe206e8da43c958eb299831c2067b11c35',
    'cms-content71-asset9890-page2': '5050f0844c3a091fd80248954caf1a58063453d8446d7e4d66999aae6266e6fd',
    'cms-content71-asset9891-page3': '3672c54292d409a7ac1a5bb5c47f7136bf261b07d18b0d220bfd79a907a3b757',
    'cms-content71-asset9892-page4': '53db828ece869d9574a0820fc085e0c8b92ea7199b9c1fd1e6e53892a547b6be',
    'cms-content71-asset9893-page5': '01a5fa93910fcad76c704b0c042c4b49d4e2cf172cad2d5090dccc5278af9d9e',
    'cms-content71-asset9894-page6': 'de35b3a9c0c2a335e6bb41da9c7f551256c9906f87fa89c73dd25ed05f0bb4c0',
    'cms-content71-asset9895-page7': '82e4c2a8be6283caf915c08739d74ae47f290b49c36dc0608b8775ec6f80ee8d',
    'cms-content71-asset9896-page8': '25a172b065bd4d83d91631b9332d5e7b91a103e9922678a6eedd5333ecc42838',
    'cms-content71-asset9897-page9': '8276d3e91593277ea741629eee49d794035e8dddf9bc4581337c9c0f88ac309f',
    'cms-content71-asset9898-page10': '1fdeba4bcd8f4d0d5597366697e0b65f60aeedac75e6f649212e2ea100c9e9a8',
    'cms-content71-asset9899-page11': '3445a1b01d9db8290b107c68d477fd1f49351117d4af63581827db66c89b77bc',
    'cms-content71-asset9900-page12': 'eae2bd3e229559af0a5da56d2b9457864e41c1b7ae1a19516516ec787181d26d',
    'cms-content71-asset9901-page13': 'b8776b0333f132260fa76b48090d7a066d16a6f2a63d1e94c9bd5251aeac1645',
    'cms-content71-asset9902-page14': '9dc6567a11a392ba76e860e0a2aded77804bddbb2e86bfc941ed94e9811cf953',
    'cms-content71-asset9903-page15': '0535c35299e123d3d70d890b94ad9b2af281d19b646462f91f2f35fcbded1ab5',
    'cms-content71-asset9904-page16': 'b6a45a49a0717718adf7b55154f69154a537cd5db4857458cb2556aaaf7b0f19',
    'cms-content71-asset9905-page17': '78ea1f1d05beefa3a2b35c2327a9ef7358c8894bcc5374c97a9399ac7a8982fc',
    'cms-content71-asset9906-page18': '78ea1f1d05beefa3a2b35c2327a9ef7358c8894bcc5374c97a9399ac7a8982fc',
    'cms-content19-asset2200-page3': '37b61ff303b2da45eb8e77cfee4d89308051bcac7423f8d7be5ae91d4893de2b',
    'cms-content19-asset2201-page4': 'af35d91fa924f5f891210f095355a0dcaa904f0268afa84b71b57af3980a7a4a',
    'cms-content19-asset2202-page5': '8ad9a826fb208e6cc03f1bd335270e483e833ef91564a9e5666e95af55831d1c',
    'cms-content19-asset2203-page6': 'e7481b93467d5af66d8f7c92720722d583870a0b7df353e7b239f220194332ba',
    'cms-content19-asset2204-page7': '941e3f151faef5badc331c86d96a1951c9f357b3f54cb31bde7d2171532b618e',
    'cms-content19-asset2205-page8': '94daee2b203856a9686678fcdeff7f693da0829221ab83da0e594e3ca4c17cde',
    'cms-content19-asset2206-page9': 'c1d20a760015b1e90a643b337ca19fc569d1284f5191e01441c5fb01f50d05d0',
    'cms-content19-asset2207-page10': '4082dd2e5d5fe03bf8c6d56def0740b74bd52d35aef9b9356fc5644943418a9a',
    'cms-content19-asset2208-page11': 'aa5c1495c61ad9e17ed46bd1b7f65c179dedbf11694fa6460ff9b96b84dedc2e',
    'cms-content19-asset2209-page12': '8e0ee6965b6f7181aeb6a1557dfe27603e25b0a61326abd2180d06e2d8c26ad0',
    'cms-content19-asset2197': 'c3d600abf263ecdf12f5b43bd932f187380c67bbb6028c4933dfa7dd07d57e3b',
    'cms-content19-asset2198': 'c38016582533626c4ac7a30a9804ed7217d14ae8c0a385ac1878a7939e03f22e',
    'cms-content19-asset2199': 'cc701e0608cefcc45217d98c00ac6061533bef2947b986c026f7089b175d1aeb',
    'cms-content10-asset11219': 'febbb483a0ebcecf1086c312e4644f2fb05abb0ff69eb6b995c849dd2085641a',
    'cms-content10-asset1001-page8': '17a8cdda129808eac9d90445fb12299f027b75a4c2fa2c73f39d1907a5251c15',
    'cms-content10-asset1003-page9': '1b87fc920eac94b7abb155d4bad037e268c0155fc029ae597feeded4ebd30243',
    'cms-content10-asset1047': 'ec0e6abc604a8a64851e15e62ab2850135b81902c84cec005e0c7286682e5072',
    'cms-content10-asset1048-page1': '89059d8ff477d370a7adbe1bff4072bf6ab549a3448f059f2c20b8052a8a2ef2',
    'cms-content10-asset1049-page2': '691a8548f9cc9f9ec4096c702fe27f90a07bacfc6fa345c64ca83f5b76571845',
    'cms-content10-asset1050-page3': '1ed666ee6df180d4408c9b95c3bf63eefed75af7f614090d5bd8a7770e337644',
    'cms-content10-asset1051-page4': '3d8986429ca1bade6983910665784797e1d203dc193833afa60e701e9d922977',
    'cms-content10-asset1052-page5': 'eb570732d67ab98395e59a6e51bcae3dbcaf123f7d5f463f68c1aa51629f15d0',
    'cms-content10-asset1053-page6': '2cc2749df5213fc70c75361555d66ff7ad7a5ac8a75f70db95d19365800787fd',
    'cms-content10-asset1054-page7': '69e0b616a398db4441720a8878b3ff7adbb990f358e0eee0fcc472817c76f727',
    'cms-content10-asset1055-page8': '57c19f102482d42d1dbed3e854713257404913537783cefb62cd7bbe0506a797',
}
RETAINED_HISTORICAL_IDENTITIES = {
    'tr-story1': 'e60624a0ae77289204f9a04b0b29b9e7993e1691405e4ae8ef274428262208af',
    'en-story1': '66bdf429e5b4b87cd05a3af91607e5e063ebfd26f852d675f3f09b4a06cfcb3a',
    'pt-story1': '3114962b43b15b40b853963be1b5e2afd430f9b357d61cba2de60fe56a82a673',
    'en-story2': '83ca57b2b0771071eaa72597a6070e4d4004f5359af4fe6ab6ea1444c0ec611d',
    'tr-story3': '552cbec46198b314aaef14bf6e23b20d1c5a0287896e8500289f2338305e2565',
    'pt-story4': '3048aeb7f6be2fa05dc83d60840dbf2841711ccbfb26f42d1f570f6dff66ec75',
    'en-story11-p3': 'bb24c527c769343a1c7cea04309106b053fd67b4560bd9b71d4525c93ce7e45e',
}


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical(value: object) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True, allow_nan=False).encode()


def same(actual: object, expected: object) -> bool:
    """Canonical equality distinguishes bool/int/float in frozen metadata."""
    try:
        return canonical(actual) == canonical(expected)
    except (ValueError, TypeError):
        return False


def object_value(value: object) -> dict:
    """Read already diagnosed malformed containers without interrupting other checks."""
    return value if isinstance(value, dict) else {}


def id_lookup(rows: dict, ident: object, default=None):
    """Malformed identifiers must not interrupt subsequent diagnostics."""
    return rows.get(ident, default) if isinstance(ident, str) else default


def installed_provider_discovery() -> dict:
    """Read installed declarations and binary identities without invoking an encoder."""
    import PIL, PIL._imaging, PIL._webp
    from PIL import features
    files = {"python_executable": Path(sys.executable).resolve(), "pillow_imaging": Path(PIL._imaging.__file__).resolve(), "pillow_webp": Path(PIL._webp.__file__).resolve()}
    for path in list(files.values()):
        linked = subprocess.run(["otool", "-L", str(path)], capture_output=True, text=True, check=True)
        for line in linked.stdout.splitlines()[1:]:
            name = line.strip().split(" (")[0]
            dependency = ((path.parent / name.removeprefix("@loader_path/")).resolve() if name.startswith("@loader_path/")
                          else Path(name) if name.startswith("/") else None)
            if dependency is not None and dependency.is_file():
                files[dependency.name] = dependency
    return {"id": "installed-python-pillow-discovery-r3", "runtime": {
        "python": platform.python_version(), "platform": platform.platform(), "pillow": PIL.__version__,
        "jpeg": features.version_codec("jpg"), "webp_declared": features.check("webp"), "webp": features.version("webp"), "zlib": features.version_codec("zlib")},
        "binaries": sorted([{"id": name, "sha256": stream_hash(path)[0], "bytes": path.stat().st_size} for name, path in files.items()], key=lambda row: row["id"]),
        "encoding_performed": False, "encoder_qualification": False}

def stream_hash(path: Path) -> tuple[str, int]:
    h = hashlib.sha256(); total = 0
    with path.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            total += len(chunk); h.update(chunk)
    return h.hexdigest(), total

def jpeg_precision(data: bytes) -> int | None:
    """Return JPEG SOF sample precision without decoding pixel data."""
    if not data.startswith(b"\xff\xd8"):
        return None
    i = 2
    sof = {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}
    while i + 3 < len(data):
        if data[i] != 0xFF:
            i += 1
            continue
        while i < len(data) and data[i] == 0xFF:
            i += 1
        if i >= len(data):
            return None
        marker = data[i]; i += 1
        if marker in (0xD8, 0xD9):
            continue
        if i + 1 >= len(data):
            return None
        length = int.from_bytes(data[i:i + 2], "big")
        if length < 2 or i + length > len(data):
            return None
        if marker in sof and length >= 8:
            return data[i + 2]
        i += length
    return None

def actual_observation(image: object, raw: bytes) -> dict:
    """Capture only independently re-readable byte metadata."""
    fmt = getattr(image, "format", None)
    exif = image.getexif()
    icc_bytes = image.info.get("icc_profile")
    icc = {"present": bool(icc_bytes), "valid": None, "profile_color_space": None, "assumed_srgb": not bool(icc_bytes)}
    if icc_bytes:
        icc["sha256"] = sha(icc_bytes)
        if ImageCms is not None:
            from io import BytesIO
            profile = ImageCms.ImageCmsProfile(BytesIO(icc_bytes))
            icc.update(valid=True, profile_color_space=profile.profile.xcolor_space.strip())
    alpha = "A" in image.getbands() or "transparency" in image.info
    extrema = None
    if alpha:
        extrema = list(image.convert("RGBA").getchannel("A").getextrema())
    return {
        "bytes": len(raw), "actual_mime": PIL_MIME.get(fmt),
        "width": image.width, "height": image.height,
        "mode": image.mode, "declared_mime": PIL_MIME.get(fmt),
        "frames": getattr(image, "n_frames", 1), "has_alpha": alpha,
        "alpha_extrema": extrema,
        "exif": {"present": bool(image.info.get("exif")), "orientation": exif.get(274)},
        "icc": icc,
    }

def compare_observation(expected: dict, actual: dict, label: str, report: Report) -> None:
    for key in ("bytes", "actual_mime", "width", "height", "frames", "has_alpha"):
        if key not in expected or expected[key] is None:
            report.open(f"{label}: observation {key} unavailable")
        elif not same(expected[key], actual.get(key)):
            report.bad(f"{label}: {key} differs from private source")
    for section, fields in (("exif", ("present", "orientation")), ("icc", ("present",))):
        recorded = expected.get(section)
        if recorded is None:
            report.open(f"{label}: observation {section} unavailable")
            continue
        recorded = mapping(recorded, f"{label}.observation.{section}", report)
        for key in fields:
            if key not in recorded or (key == "present" and recorded[key] is None):
                report.open(f"{label}: observation {section}.{key} unavailable")
            elif not same(recorded[key], actual[section].get(key)):
                report.bad(f"{label}: {section} metadata differs from private source")
        if section == "icc" and actual[section]["present"]:
            if recorded.get("sha256") is None:
                report.open(f"{label}: observation ICC hash unavailable")
            elif recorded["sha256"] != actual[section].get("sha256"):
                report.bad(f"{label}: icc metadata differs from private source")
        elif section == "icc" and recorded.get("sha256") is not None:
            report.bad(f"{label}: absent ICC metadata has a contradictory profile hash")
        if section == "icc":
            for key in ("valid", "profile_color_space", "assumed_srgb"):
                if key not in recorded:
                    continue
                if actual[section]["present"] and key != "assumed_srgb" and actual[section].get(key) is None:
                    report.open(f"{label}: actual ICC {key} unavailable")
                elif recorded[key] is None and actual[section].get(key) is not None:
                    report.open(f"{label}: observation ICC {key} unavailable")
                elif not same(recorded[key], actual[section].get(key)):
                    report.bad(f"{label}: icc {key} differs from private source/policy")
    if actual.get("has_alpha") and expected.get("alpha_extrema") is None:
        report.open(f"{label}: observation alpha extrema unavailable")
    elif "alpha_extrema" in expected and not same(expected["alpha_extrema"], actual.get("alpha_extrema")):
        report.bad(f"{label}: alpha extrema differs from private source")
    for optional in ("mode", "declared_mime"):
        if optional in expected:
            if expected[optional] is None:
                report.open(f"{label}: observation {optional} unavailable")
            elif not same(expected[optional], actual.get(optional)):
                report.bad(f"{label}: observation {optional} differs from private source")
    if expected.get("decode_status") is None:
        report.open(f"{label}: observation full decode evidence unavailable")
    elif expected["decode_status"] != "verified_full_decode":
        report.bad(f"{label}: observation full decode evidence differs")

def scan_private_values(value: object, report: Report, key: str = "") -> None:
    if isinstance(value, dict):
        for name, child in value.items():
            lower = str(name).lower()
            if child not in (None, "") and any(token in lower for token in ("access_url", "signed_url", "token", "private_key", "secret", "credential", "source_path")):
                report.bad(f"private access field populated: {name}")
            scan_private_values(child, report, str(name))
    elif isinstance(value, list):
        for child in value: scan_private_values(child, report, key)
    elif isinstance(value, str) and re.search(r"(?:previewToken|alt=media|X-Goog-|GoogleAccessId|storage\.googleapis\.com|firebaseio\.com)", value, re.I):
        report.bad(f"private access material in value: {key}")
    elif isinstance(value, float) and not math.isfinite(value):
        report.bad(f"non-finite numeric value: {key}")


def known_source_metadata(specimen: dict) -> bool:
    """Unknown metadata cannot qualify a real coverage class or pilot source."""
    meta = specimen.get("metadata")
    if not isinstance(meta, dict) or type(meta.get("frames")) is not int or meta["frames"] < 1:
        return False
    if type(meta.get("has_alpha")) is not bool:
        return False
    extrema = meta.get("alpha_extrema")
    if meta["has_alpha"]:
        if not isinstance(extrema, list) or len(extrema) != 2 or any(type(x) is not int for x in extrema):
            return False
        if not 0 <= extrema[0] <= extrema[1] <= 255:
            return False
    elif extrema is not None:
        return False
    exif, icc = meta.get("exif"), meta.get("icc")
    if not isinstance(exif, dict) or type(exif.get("present")) is not bool or "orientation" not in exif:
        return False
    orientation = exif["orientation"]
    if orientation is not None and (type(orientation) is not int or orientation not in range(1, 9)):
        return False
    if not isinstance(icc, dict) or type(icc.get("present")) is not bool:
        return False
    if not icc["present"]:
        return icc.get("assumed_srgb") is True
    return isinstance(icc.get("sha256"), str) and bool(SHA.fullmatch(icc["sha256"]))


def feature_matches(specimen: dict, feature: str, language=None) -> bool:
    if not known_source_metadata(specimen):
        return False
    meta = specimen["metadata"]
    tags = {x for x in specimen.get("visual_tags", []) if isinstance(x, str)} if isinstance(specimen.get("visual_tags"), list) else set()
    roles = {x for x in specimen.get("roles", []) if isinstance(x, str)} if isinstance(specimen.get("roles"), list) else set()
    if feature == "title_bearing_cover":
        return (specimen.get("kind") == "reading_cover" and "reading_cover" in roles and
                specimen.get("language") == language and bool(tags & {"title_bearing", "title_bearing_cover"}))
    if feature == "real_alpha":
        extrema = meta.get("alpha_extrema")
        return meta.get("has_alpha") is True and isinstance(extrema, list) and len(extrema) == 2 and extrema[0] < 255
    orientation = (meta.get("exif") or {}).get("orientation")
    if feature == "exif_rotation": return orientation in {3, 5, 6, 7, 8}
    if feature == "exif_mirroring": return orientation in {2, 4, 5, 7}
    if feature in {"icc_tagged_color", "icc_color_conversion"}:
        icc = meta.get("icc") or {}
        return icc.get("present") is True and bool(SHA.fullmatch(str(icc.get("sha256"))))
    if feature == "real_text_diacritics":
        return "real_text" in tags and any("diacritics" in tag for tag in tags)
    if feature in {"ordinary_page", "text_dense_page", "large_illustration", "fine_lines", "gradients", "dark_art"}:
        return specimen.get("kind") == "story_page" and "reading_page" in roles and feature in tags
    return False


class Report:
    def __init__(self) -> None:
        self.invalid: list[str] = []
        self.incomplete: list[str] = []
        self.notes: list[str] = []

    def bad(self, msg: str) -> None: self.invalid.append(msg)
    def open(self, msg: str) -> None: self.incomplete.append(msg)
    def status(self) -> str:
        return "INVALID" if self.invalid else "INCOMPLETE" if self.incomplete else "PASS"
    def emit(self) -> int:
        rows = ([{"severity": "invalid", "message": x} for x in self.invalid] +
                [{"severity": "incomplete", "message": x} for x in self.incomplete] +
                [{"severity": "note", "message": x} for x in self.notes])
        print(json.dumps({"status": self.status(), "diagnostics": rows}, indent=2, ensure_ascii=False))
        return {"PASS": 0, "INCOMPLETE": 1, "INVALID": 2}[self.status()]


def mapping(value: object, label: str, report: Report) -> dict:
    if not isinstance(value, dict):
        report.bad(f"{label}: expected object")
        return {}
    return value


def sequence(value: object, label: str, report: Report) -> list:
    if not isinstance(value, list):
        report.bad(f"{label}: expected array")
        return []
    return value


def string_ids(value: object, label: str, report: Report) -> list[str]:
    """Validate set-like IDs before hashing; retain later integrity diagnostics."""
    result = []
    for item in sequence(value, label, report):
        if not isinstance(item, str) or not item:
            report.bad(f"{label}: expected nonempty string ID")
        elif item in result:
            report.bad(f"{label}: duplicate ID {item}")
        else:
            result.append(item)
    return result


def validate_preservation(data: dict, report: Report) -> None:
    """Conserve the approved identities and every bidirectional source link."""
    specimens = [row for row in sequence(data.get("specimens"), "specimens", report) if isinstance(row, dict)]
    bindings = [row for row in sequence(data.get("bindings"), "bindings", report) if isinstance(row, dict)]
    historical = [row for row in sequence(data.get("historical_controls"), "historical_controls", report) if isinstance(row, dict)]
    observations = sequence(data.get("byte_observations"), "byte_observations", report)
    for label, rows, fields, identity_hash in (
        ("specimen", specimens, SPECIMEN_IDENTITY_FIELDS, RETAINED_SPECIMEN_IDENTITIES_SHA),
        ("binding", bindings, BINDING_IDENTITY_FIELDS, RETAINED_BINDING_IDENTITIES_SHA)):
        projection = [{key: row.get(key) for key in fields} for row in rows]
        projection.sort(key=lambda row: str(row["id"]))
        if sha(canonical(projection)) != identity_hash:
            report.bad(f"retained {label} identities/ownership/links differ from immutable r2 snapshot")
    for label, rows, expected_ids in (("specimen", specimens, RETAINED_SOURCE_IDENTITIES),
                                      ("historical", historical, RETAINED_HISTORICAL_IDENTITIES)):
        ids = [row.get("id") for row in rows if isinstance(row.get("id"), str)]
        if len(ids) != len(expected_ids) or set(ids) != set(expected_ids):
            report.bad(f"retained {label} identity set differs from immutable r2 snapshot")
        for row in rows:
            ident = row.get("id")
            expected = id_lookup(expected_ids, ident)
            integrity = mapping(row.get("integrity"), f"{ident}.integrity", report) if label == "specimen" else row
            source_hash = integrity.get("sha256")
            if source_hash is None:
                report.open(f"{ident}: retained source hash unavailable")
            elif not isinstance(source_hash, str) or not SHA.fullmatch(source_hash):
                report.bad(f"{ident}: malformed retained source hash")
            elif expected is None or source_hash != expected:
                report.bad(f"{ident}: retained source hash differs from immutable r2 snapshot")
    expected_observations = {**RETAINED_SOURCE_IDENTITIES, **RETAINED_HISTORICAL_IDENTITIES}
    seen: set[str] = set()
    for row in observations:
        if not isinstance(row, dict):
            report.bad("byte observation must be object")
            continue
        ident = row.get("id")
        if not isinstance(ident, str) or ident not in expected_observations:
            report.bad(f"byte observation has malformed or unknown retained identity: {ident}")
            continue
        if ident in seen:
            report.bad(f"duplicate byte observation ID: {ident}")
        seen.add(ident)
        source_hash = row.get("sha256")
        if source_hash is None:
            report.open(f"{ident}: observation source hash unavailable")
        elif not isinstance(source_hash, str) or source_hash != expected_observations[ident]:
            report.bad(f"{ident}: observation source hash/link differs from retained identity")
    for ident in sorted(set(expected_observations) - seen):
        report.open(f"{ident}: full byte metadata observation missing")
    if len(observations) > len(expected_observations):
        report.bad("retained byte observation count exceeds the immutable 62 identities")
    elif len(observations) < len(expected_observations):
        report.open("retained byte observation evidence set is incomplete")

    norm = data.get("normalization") if isinstance(data.get("normalization"), dict) else {}
    correctness = norm.get("correctness") if isinstance(norm.get("correctness"), dict) else {}
    refs = sequence(correctness.get("prepared_references"), "normalization.correctness.prepared_references", report)
    binding_map = {row["id"]: row for row in bindings if isinstance(row.get("id"), str)}
    specimen_map = {row["id"]: row for row in specimens if isinstance(row.get("id"), str)}
    reference_links: dict[str, list[str]] = {}
    for ref in refs:
        if not isinstance(ref, dict):
            continue
        source_hash = ref.get("source_hash")
        if not isinstance(source_hash, str) or not SHA.fullmatch(source_hash):
            report.bad(f"{ref.get('id')}: prepared source hash malformed")
            continue
        links = string_ids(ref.get("binding_ids"), f"{ref.get('id')}.binding_ids", report)
        reference_links.setdefault(source_hash, []).extend(links)
        for binding_id in links:
            binding = binding_map.get(binding_id)
            specimen = id_lookup(specimen_map, binding.get("specimen_id")) if binding else None
            integrity = specimen.get("integrity") if specimen and isinstance(specimen.get("integrity"), dict) else {}
            if binding is None or specimen is None or (integrity.get("sha256") is not None and integrity.get("sha256") != source_hash):
                report.bad(f"{ref.get('id')}: reference binding/source backlink mismatch: {binding_id}")
    for binding_id, binding in binding_map.items():
        specimen = id_lookup(specimen_map, binding.get("specimen_id"))
        if specimen is None:
            continue
        links = string_ids(specimen.get("bindings"), f"{specimen.get('id')}.bindings", report)
        roles = string_ids(specimen.get("roles"), f"{specimen.get('id')}.roles", report)
        if binding_id not in links or binding.get("role") not in roles:
            report.bad(f"{binding_id}: specimen/binding ownership backlink mismatch")
        integrity = specimen.get("integrity") if isinstance(specimen.get("integrity"), dict) else {}
        source_hash = integrity.get("sha256")
        if isinstance(source_hash, str) and reference_links.get(source_hash, []).count(binding_id) != 1:
            report.bad(f"{binding_id}: binding/reference source link must occur exactly once")
        for linked in links:
            if linked not in binding_map or binding_map[linked].get("specimen_id") != specimen.get("id"):
                report.bad(f"{specimen.get('id')}: specimen/binding backlink mismatch: {linked}")

    source_hashes = {row.get("integrity", {}).get("sha256") for row in specimens
                     if isinstance(row.get("integrity"), dict) and isinstance(row["integrity"].get("sha256"), str)}
    historical_hashes = {row["sha256"] for row in historical if isinstance(row.get("sha256"), str)}
    story_ids = {row["content_id"] for row in specimens if type(row.get("content_id")) is int}
    counts = mapping(data.get("counts"), "counts", report)
    derived = {"cms_unique_retained_originals": len(source_hashes), "historical_regression_controls": len(historical) - 1,
               "historical_color_diagnostics": 1 if "en-story11-p3" in {row.get("id") for row in historical if isinstance(row.get("id"), str)} else 0,
               "total_unique_retained_originals": len(source_hashes | historical_hashes), "cms_binding_records": len(bindings),
               "source_story_count": len(story_ids), "source_story_ids": sorted(story_ids),
               "prepared_unadmitted_references": sum(isinstance(row, dict) and row.get("status") == "unadmitted_preflight" for row in refs),
               "admitted_pilot_references": sum(isinstance(row, dict) and row.get("status") == "admitted" for row in refs),
               "synthetic_rejection_fixtures": len(sequence(data.get("synthetic_rejection_fixtures"), "synthetic_rejection_fixtures", report))}
    for key, value in derived.items():
        if key in {"cms_unique_retained_originals", "total_unique_retained_originals"} and any(
            not isinstance(row.get("integrity"), dict) or row["integrity"].get("sha256") is None for row in specimens):
            continue
        if not same(counts.get(key), value):
            report.bad(f"counts.{key} differs from derived retained records")


def validate_source(data: dict, root: Path | None, report: Report) -> None:
    if root is None:
        report.open("--source-root was not supplied; source bytes remain unadmitted")
        return
    if not root.is_dir():
        report.bad("source-root is not a directory")
        return
    if root.resolve().is_relative_to(REPO.resolve()):
        report.bad("source-root must be outside the repository")
        return
    actual_by_hash: dict[str, dict] = {}
    for specimen in sequence(data.get("specimens"), "specimens", report):
        if not isinstance(specimen, dict):
            continue
        ident = specimen.get("id", "<unknown>")
        integrity = mapping(specimen.get("integrity"), f"{ident}.integrity", report)
        expected = integrity.get("sha256")
        if not isinstance(expected, str) or not SHA.fullmatch(expected):
            report.open(f"{ident}: source hash unavailable")
            continue
        if not isinstance(integrity.get("bytes"), int) or integrity["bytes"] <= 0:
            report.open(f"{ident}: source byte count unavailable")
        if integrity.get("verified_against_private_source") is not True:
            report.open(f"{ident}: private source verification is not recorded")
        if integrity.get("rehash_after_inspection") is not True:
            report.open(f"{ident}: post-inspection rehash is not recorded")
        path = root / f"{expected}.source"
        if not path.is_file():
            report.open(f"{ident}: private source copy is absent")
            continue
        if path.is_symlink():
            report.bad(f"{ident}: source copy must not be a symlink")
            continue
        try:
            with path.open("rb") as stream:
                bounded = stream.read(MAX_BYTES + 1)
        except OSError as exc:
            report.bad(f"{ident}: source read failed: {exc}")
            continue
        if len(bounded) > MAX_BYTES:
            report.bad(f"{ident}: source exceeds {MAX_BYTES} byte cap")
            continue
        if sha(bounded) != expected:
            report.bad(f"{ident}: source rehash mismatch")
        if integrity.get("bytes") not in (None, len(bounded)):
            report.bad(f"{ident}: recorded byte count differs")
        if not integrity.get("verified_against_private_source") or not integrity.get("rehash_after_inspection"):
            report.open(f"{ident}: private rehash is not recorded")
        if Image is None:
            report.open(f"{ident}: Pillow unavailable for full decode metadata")
            continue
        try:
            from io import BytesIO
            with Image.open(BytesIO(bounded)) as image:
                if image.format == "PNG" and len(bounded) >= 26 and bounded[:8] == b"\x89PNG\r\n\x1a\n":
                    depth, color_type = bounded[24], bounded[25]
                    if (color_type == 3 and depth not in {1, 2, 4, 8}) or (color_type != 3 and depth != 8):
                        report.open(f"{ident}: unsupported PNG depth/type {depth}/{color_type}")
                        continue
                if image.format == "JPEG" and jpeg_precision(bounded) not in (None, 8):
                    report.open(f"{ident}: unsupported non-8-bit JPEG precision")
                    continue
                axes = mapping(specimen.get("encoded_axes"), f"{ident}.encoded_axes", report)
                if axes and [image.width, image.height] != [axes.get("width"), axes.get("height")]:
                    report.bad(f"{ident}: dimensions differ from recorded axes")
                if image.width * image.height > MAX_PIXELS:
                    report.bad(f"{ident}: source exceeds {MAX_PIXELS} pixel cap")
                    continue
                image.load()
                actual_by_hash[expected] = actual_observation(image, bounded)
                if not isinstance(axes, dict) or not isinstance(axes.get("width"), int) or not isinstance(axes.get("height"), int):
                    report.open(f"{ident}: encoded axes metadata unavailable")
                actual = PIL_MIME.get(image.format)
                if specimen.get("actual_mime") is None:
                    report.open(f"{ident}: actual MIME record unavailable")
                elif actual != specimen.get("actual_mime"):
                    report.bad(f"{ident}: MIME differs from recorded actual MIME")
                meta = mapping(specimen.get("metadata"), f"{ident}.metadata", report)
                if meta.get("mode") is None:
                    report.open(f"{ident}: decoded mode record unavailable")
                elif meta.get("mode") != image.mode:
                    report.bad(f"{ident}: decoded mode differs from recorded metadata")
                if specimen.get("declared_mime") is None:
                    report.open(f"{ident}: declared MIME unavailable")
                elif specimen.get("declared_mime") != actual:
                    report.bad(f"{ident}: declared MIME differs from actual source")
                for field in ("has_alpha", "frames", "exif", "icc"):
                    if meta.get(field) is None:
                        report.open(f"{ident}: metadata {field} unavailable")
                if meta.get("frames") is None:
                    report.open(f"{ident}: frame metadata unavailable")
                if meta.get("frames") not in (None, getattr(image, "n_frames", 1)):
                    report.bad(f"{ident}: frame count differs")
                actual_alpha = "A" in image.getbands() or "transparency" in image.info
                if meta.get("has_alpha") is not None and meta.get("has_alpha") != actual_alpha:
                    report.bad(f"{ident}: alpha metadata differs from private source")
                if actual_alpha:
                    extrema = image.convert("RGBA").getchannel("A").getextrema()
                    recorded_extrema = meta.get("alpha_extrema")
                    if not isinstance(recorded_extrema, list):
                        report.open(f"{ident}: alpha extrema evidence unavailable")
                    elif list(extrema or []) != recorded_extrema:
                        report.bad(f"{ident}: alpha extrema differ from private source")
                exif = meta.get("exif")
                if isinstance(exif, dict):
                    actual_orientation = image.getexif().get(274)
                    if exif.get("orientation") != actual_orientation:
                        report.bad(f"{ident}: EXIF orientation differs from private source")
                    if exif.get("present") is None:
                        report.open(f"{ident}: EXIF presence record unavailable")
                    elif exif.get("present") != bool(image.info.get("exif")):
                        report.bad(f"{ident}: EXIF presence differs from private source")
                icc = meta.get("icc")
                if isinstance(icc, dict):
                    actual_icc = image.info.get("icc_profile")
                    if icc.get("present") is None:
                        report.open(f"{ident}: ICC presence record unavailable")
                    elif icc.get("present") != bool(actual_icc):
                        report.bad(f"{ident}: ICC presence differs from private source")
                    if actual_icc and icc.get("sha256") and icc.get("sha256") != sha(actual_icc):
                        report.bad(f"{ident}: ICC hash differs from private source")
                    elif actual_icc and not icc.get("sha256"):
                        report.open(f"{ident}: ICC profile identity unavailable")
                    if actual_icc and ImageCms is not None:
                        try: ImageCms.ImageCmsProfile(BytesIO(actual_icc))
                        except Exception as exc: report.bad(f"{ident}: ICC profile is invalid: {exc}")
        except Exception as exc:
            report.bad(f"{ident}: full decode failed: {exc}")
    for control in sequence(data.get("historical_controls"), "historical_controls", report):
        if not isinstance(control, dict):
            continue
        expected = control.get("sha256")
        if not isinstance(expected, str) or not SHA.fullmatch(expected):
            report.bad(f"{control.get('id')}: malformed historical hash")
            continue
        path = root / f"{expected}.source"
        if path.is_symlink() or not path.resolve().is_relative_to(root.resolve()):
            report.bad(f"{control.get('id')}: historical copy must not be a symlink or escape source-root")
            continue
        if not path.is_file():
            report.open(f"{control.get('id')}: historical control copy is absent")
        else:
            try:
                with path.open("rb") as stream:
                    bounded = stream.read(MAX_BYTES + 1)
            except OSError as exc:
                report.bad(f"{control.get('id')}: historical source read failed: {exc}")
                continue
            if len(bounded) > MAX_BYTES or sha(bounded) != expected:
                report.bad(f"{control.get('id')}: historical source integrity failure")
            elif Image is not None:
                try:
                    from io import BytesIO
                    with Image.open(BytesIO(bounded)) as image:
                        if image.width * image.height > MAX_PIXELS:
                            report.bad(f"{control.get('id')}: historical source exceeds pixel cap before load")
                            continue
                        if image.format == "PNG" and len(bounded) > 24 and bounded[24] != 8:
                            report.open(f"{control.get('id')}: unsupported non-8-bit PNG depth")
                            continue
                        if image.format == "JPEG" and jpeg_precision(bounded) not in (None, 8):
                            report.open(f"{control.get('id')}: unsupported non-8-bit JPEG precision")
                            continue
                        image.load()
                        actual_by_hash[expected] = actual_observation(image, bounded)
                        if image.width * image.height > MAX_PIXELS:
                            report.bad(f"{control.get('id')}: historical source exceeds pixel cap")
                except Exception as exc:
                    report.bad(f"{control.get('id')}: historical full decode failed: {exc}")
    for observation in sequence(data.get("byte_observations"), "byte_observations", report):
        if not isinstance(observation, dict):
            continue
        ident = observation.get("id")
        expected_hash = id_lookup(RETAINED_SOURCE_IDENTITIES, ident) or id_lookup(RETAINED_HISTORICAL_IDENTITIES, ident)
        actual = actual_by_hash.get(expected_hash) if expected_hash else None
        if actual is None:
            report.open(f"{ident}: actual source observation remains unavailable")
        else:
            compare_observation(observation, actual, f"{ident} byte observation", report)
    fixture_dir = root / "fixtures"
    fixtures = sequence(data.get("synthetic_rejection_fixtures"), "synthetic_rejection_fixtures", report)
    fixture_categories: list[str] = []
    for fixture in fixtures:
        if not isinstance(fixture, dict): continue
        filename = fixture.get("private_file")
        if not isinstance(filename, str) or Path(filename).is_absolute() or ".." in Path(filename).parts:
            report.bad(f"{fixture.get('id')}: fixture private_file must be safe relative name")
            continue
        path = fixture_dir / filename
        if path.is_symlink() or not path.resolve().is_relative_to(root.resolve()):
            report.bad(f"{fixture.get('id')}: fixture path escapes source-root or is symlinked")
            continue
        if not path.is_file():
            report.open(f"{fixture.get('id')}: private fixture bytes are absent")
            continue
        try:
            if path.stat().st_size > MAX_BYTES + 2 * 1024 * 1024:
                report.bad(f"{fixture.get('id')}: fixture exceeds finite probe bound")
                continue
            actual_hash, actual_bytes = stream_hash(path)
        except OSError as exc:
            report.bad(f"{fixture.get('id')}: fixture read/stat failed: {exc}")
            continue
        if actual_hash != fixture.get("sha256"):
            report.bad(f"{fixture.get('id')}: fixture rehash mismatch")
        metadata = mapping(fixture.get("metadata"), f"{fixture.get('id')}.metadata", report)
        if metadata.get("bytes") != actual_bytes:
            report.bad(f"{fixture.get('id')}: fixture byte metadata mismatch")
        if metadata.get("max_bytes") != MAX_BYTES or metadata.get("max_pixels") != MAX_PIXELS:
            report.bad(f"{fixture.get('id')}: frozen fixture limits differ from verifier")
        if fixture.get("category") == "excessive_bytes" and actual_bytes <= MAX_BYTES:
            report.bad(f"{fixture.get('id')}: excessive-byte fixture is within byte cap")
        if actual_bytes > MAX_BYTES and fixture.get("category") != "excessive_bytes":
            report.bad(f"{fixture.get('id')}: non-byte fixture exceeds byte cap")
        if Image is not None and actual_bytes <= MAX_BYTES:
            try:
                bounded = path.read_bytes()
            except OSError as exc:
                report.bad(f"{fixture.get('id')}: fixture read failed: {exc}")
                continue
            try:
                from io import BytesIO
                with Image.open(BytesIO(bounded)) as image:
                    actual_mime = PIL_MIME.get(image.format)
                    actual_pixels = image.width * image.height
                    if metadata.get("actual_mime") not in (None, actual_mime): report.bad(f"{fixture.get('id')}: fixture MIME metadata mismatch")
                    if metadata.get("pixels") not in (None, actual_pixels): report.bad(f"{fixture.get('id')}: fixture pixel metadata mismatch")
                    for axis, actual_axis in (("width", image.width), ("height", image.height)):
                        if axis in metadata and metadata[axis] != actual_axis: report.bad(f"{fixture.get('id')}: fixture {axis} metadata mismatch")
                    if fixture.get("category") == "excessive_pixels" and actual_pixels <= MAX_PIXELS: report.bad(f"{fixture.get('id')}: excessive-pixel fixture is within pixel cap")
                    if actual_pixels > MAX_PIXELS:
                        if fixture.get("category") != "excessive_pixels": report.bad(f"{fixture.get('id')}: unexpected excessive pixel fixture")
                        continue
                    image.load()
                    if fixture.get("category") == "truncated": report.bad(f"{fixture.get('id')}: truncated fixture decoded successfully")
                    if fixture.get("category") == "mime_mismatch" and metadata.get("declared_mime") == actual_mime: report.bad(f"{fixture.get('id')}: MIME mismatch fixture is not mismatched")
            except Exception:
                if fixture.get("category") != "truncated": report.bad(f"{fixture.get('id')}: fixture decode failed unexpectedly")


def validate_preparation(norm: dict, receipt_path: Path | None, report: Report) -> None:
    recipe = mapping(norm.get("recipe"), "normalization.recipe", report)
    correctness = mapping(norm.get("correctness"), "normalization.correctness", report)
    if receipt_path is None:
        report.open("--preparation-receipt was not supplied; preparation identity remains unverified")
        return
    try:
        if receipt_path.is_symlink() or receipt_path.resolve().is_relative_to(REPO) or receipt_path.stat().st_size > 1024 * 1024:
            report.bad("preparation receipt must be bounded and private, without symlinks")
            return
        raw = receipt_path.read_bytes()
        receipt = mapping(json.loads(raw), "preparation receipt", report)
    except Exception as exc:
        report.bad(f"preparation receipt cannot be read: {exc}")
        return
    expected = correctness.get("preparation_output_sha256") or recipe.get("preparation_output_sha256")
    if sha(raw) != expected:
        report.bad("preparation receipt hash differs from public preparation_output_sha256")
    impl = mapping(recipe.get("implementation"), "normalization.recipe.implementation", report)
    ri = mapping(receipt.get("implementation"), "preparation.implementation", report)
    for field in ("script_sha256", "destination_profile_sha256", "settings"):
        if ri.get(field) != impl.get(field):
            report.bad(f"preparation implementation {field} differs from public recipe")
    if ri.get("destination_profile_sha256") != mapping(recipe.get("destination_profile"), "normalization.recipe.destination_profile", report).get("sha256"):
        report.bad("preparation target profile differs from public recipe")
    if ri.get("script_sha256") != sha((HERE / "prepare-p0a-normalization.py").read_bytes()):
        report.bad("preparation script hash differs from the retained implementation")
    if receipt.get("runtime") != recipe.get("runtime"):
        report.bad("preparation runtime differs from public recipe")
    if receipt.get("source_failures"):
        if correctness.get("admitted_references"):
            report.bad("failed preparation sources cannot be admitted")
        else:
            report.open("preparation records failed source checks; references remain unadmitted")
    if receipt.get("expected_receipt_sha256") != correctness.get("expected_receipt_sha256"):
        report.bad("preparation independent expectations differ from public projection")
    public_rows = sequence(correctness.get("checks"), "normalization.correctness.checks", report)
    receipt_rows = sequence(receipt.get("checks"), "preparation.checks", report)
    public_ids = {x.get("id") for x in public_rows if isinstance(x, dict) and isinstance(x.get("id"), str)}
    receipt_ids = {x.get("id") for x in receipt_rows if isinstance(x, dict) and isinstance(x.get("id"), str)}
    failed_unadmitted = any(x.get("result") != "pass" for x in public_rows if isinstance(x, dict)) and not correctness.get("admitted_references")
    if public_ids != receipt_ids and not failed_unadmitted:
        report.bad("preparation correctness check IDs differ from public projection")
    if not failed_unadmitted:
        public_checks = {x.get("id"): x for x in public_rows if isinstance(x, dict) and isinstance(x.get("id"), str)}
        for check in receipt_rows:
            if not isinstance(check, dict): report.bad("preparation check must be object"); continue
            projected = id_lookup(public_checks, check.get("id"), {})
            if any(projected.get(k) != v for k, v in check.items()):
                report.bad(f"preparation expected/actual correctness evidence differs: {check.get('id')}")
    public_refs = {x.get("id"): x for x in sequence(correctness.get("prepared_references"), "normalization.correctness.prepared_references", report) if isinstance(x, dict) and isinstance(x.get("id"), str)}
    receipt_refs = {x.get("id"): x for x in sequence(receipt.get("references"), "preparation.references", report) if isinstance(x, dict) and isinstance(x.get("id"), str)}
    if set(public_refs) != set(receipt_refs): report.bad("preparation reference set differs from public projection")
    for ident, ref in public_refs.items():
        actual = receipt_refs.get(ident)
        if actual is None or any(not same(actual.get(k), ref.get(k)) for k in ("pixel_sha256", "source_hash", "width", "height", "bytes", "binding_ids", "metadata")):
            report.bad(f"preparation reference identity differs: {ident}")

def validate_pilot_scope(data: dict, report: Report) -> tuple[set[str], set[str]]:
    """Validate the immutable r2 pilot contract and return required/excluded classes."""
    scope = mapping(data.get("pilot_scope"), "pilot_scope", report)
    if scope.get("id") != PILOT_SCOPE_ID:
        report.bad(f"pilot_scope.id must be {PILOT_SCOPE_ID}")
    if scope.get("status") != "approved" or not all(scope.get(k) for k in ("approver", "approval_date", "approval_text")):
        report.open("pilot scope approval provenance incomplete")
    elif (scope["approver"], scope["approval_date"], scope["approval_text"]) != ("Gürhan", "2026-10-10", "Tamam öyle yapalım"):
        report.bad("pilot scope approval differs from the recorded human decision")
    required = set(string_ids(scope.get("required_for_current_pilot"), "pilot_scope.required_for_current_pilot", report))
    excluded = set(string_ids(scope.get("excluded_from_current_pilot"), "pilot_scope.excluded_from_current_pilot", report))
    if required != CURRENT_PILOT_CLASSES:
        report.bad("pilot_scope current required classes mismatch")
    if excluded != EXCLUDED_PILOT_CLASSES:
        report.bad("pilot_scope excluded classes mismatch")
    predicate = mapping(scope.get("eligibility_predicate"), "pilot_scope.eligibility_predicate", report)
    if predicate.get("static") is not True or predicate.get("one_frame") is not True:
        report.bad("pilot_scope requires static one-frame sources")
    if predicate.get("exif_orientation") != "explicitly absent; unknown and value 1 are ineligible":
        report.bad("pilot_scope EXIF predicate mismatch")
    if predicate.get("opaque") != "no alpha channel or exact alpha extrema [255,255]":
        report.bad("pilot_scope opacity predicate mismatch")
    if predicate.get("metadata_required") != PILOT_METADATA_FIELDS:
        report.bad("pilot_scope required metadata predicate mismatch")
    if scope.get("source_and_reference_revision") != FREEZE_REVISION_ID:
        report.bad("pilot_scope revision mismatch")
    expected_approval = {"status": "approved", "approver": "Gürhan", "approval_date": "2026-10-10", "approval_text": "Uygun",
                         "experimental_purpose": EXPERIMENTAL_PURPOSE, "no_resize_crop_or_upscale": True, "reader_qualification_claimed": False}
    if not same(scope.get("source_resolution_approval"), expected_approval):
        report.bad("r3 source-resolution approval/purpose boundary mismatch")

    future = mapping(data.get("future_qualification"), "future_qualification", report)
    if future.get("status") != "open":
        report.bad("future qualification must remain open")
    if future.get("scope_revision_required") is not True or future.get("approval_required_before_expansion") is not True:
        report.bad("future qualification approval/revision safeguards missing")
    gates = sequence(future.get("gates"), "future_qualification.gates", report)
    seen: set[str] = set()
    for gate in gates:
        if not isinstance(gate, dict):
            report.bad("future qualification gate must be object")
            continue
        ident = gate.get("id")
        if not isinstance(ident, str) or not ident:
            report.bad("future qualification gate id must be nonempty string")
            continue
        if ident in seen or ident not in {*FUTURE_GATES, "FQ-READER"}:
            report.bad(f"unexpected or duplicate future qualification gate: {ident}")
        seen.add(ident)
        expected_class = "reader_demand_qualification" if ident == "FQ-READER" else FUTURE_GATES.get(ident)
        if expected_class != gate.get("class"):
            report.bad(f"future qualification gate class mismatch: {ident}")
        if gate.get("status") != "open":
            report.bad(f"future qualification gate must remain open: {ident}")
        if gate.get("required_for_current_pilot") is not False:
            report.bad(f"future qualification gate must be excluded from current pilot: {ident}")
        if "evidence" not in gate or gate["evidence"] is not None:
            report.bad(f"future qualification gate has fabricated evidence: {ident}")
        for field in ("requirement", "next_action"):
            if not isinstance(gate.get(field), str) or not gate[field].strip():
                report.bad(f"future qualification gate {field} missing: {ident}")
        if ident == "FQ-READER":
            if gate.get("availability") != "unavailable" or any(field not in gate or gate[field] is not None for field in
                ("logical_slot", "dpr", "fit", "unclamped_required_full_image", "source_pixels_sufficient", "native_viewing_quality")):
                report.bad("reader qualification gate requires explicit unavailable/null evidence")
    for ident in sorted({*FUTURE_GATES, "FQ-READER"} - seen):
        report.bad(f"future qualification gate missing: {ident}")
    if future.get("synthetic_checks_do_not_close_gates") is not True:
        report.bad("future qualification synthetic safeguard missing")
    return CURRENT_PILOT_CLASSES, EXCLUDED_PILOT_CLASSES


def validate_pilot_binding(binding: dict, specimen: dict, reference: dict | None, label: str, report: Report) -> None:
    """Apply the r2 static opaque/no-orientation predicate to drafts and READY pilots."""
    meta = mapping(specimen.get("metadata"), f"{label}.metadata", report)
    if type(meta.get("frames")) is not int or meta.get("frames") != 1:
        report.bad(f"{label}: pilot source must be exactly one frame")
    if not known_source_metadata(specimen):
        report.bad(f"{label}: pilot source metadata is incomplete")
    if meta.get("has_alpha") is True:
        extrema = meta.get("alpha_extrema")
        if extrema != [255, 255]:
            report.bad(f"{label}: pilot source is not opaque")
    elif meta.get("has_alpha") is not False:
        report.bad(f"{label}: pilot alpha predicate is unknown")
    exif = meta.get("exif") if isinstance(meta.get("exif"), dict) else {}
    if "orientation" not in exif or exif.get("orientation") is not None:
        report.bad(f"{label}: pilot source requires explicitly absent EXIF orientation")
    if reference is not None and reference.get("source_hash") != object_value(specimen.get("integrity")).get("sha256"):
        report.bad(f"{label}: pilot reference/source hash mismatch")
    if binding.get("specimen_id") != specimen.get("id"):
        report.bad(f"{label}: binding/specimen mismatch")


def validate_source_resolution(data: dict, prepared: dict, bindings: list, specimen_map: dict, report: Report) -> None:
    """Admit exact existing references only for the approved offline purpose."""
    pilot = mapping(data.get("pilot_subset"), "pilot_subset", report)
    if pilot.get("experimental_purpose") != EXPERIMENTAL_PURPOSE:
        report.bad("pilot experimental purpose mismatch")
    if pilot.get("reader_qualification_claimed") is not False:
        report.bad("source-resolution pilot cannot claim reader-demand qualification")
    selected = string_ids(pilot.get("bindings"), "pilot_subset.bindings", report)
    if set(selected) != set(SELECTED_BINDINGS):
        report.bad("r3 pilot must preserve exactly the two approved bindings")
    normalization = mapping(data.get("normalization"), "normalization", report)
    correctness = mapping(normalization.get("correctness"), "normalization.correctness", report)
    recipe = mapping(normalization.get("recipe"), "normalization.recipe", report)
    decision = mapping(normalization.get("decision"), "normalization.decision", report)
    if correctness.get("preparation_output_sha256") != PREPARATION_RECEIPT_SHA or recipe.get("preparation_output_sha256") != PREPARATION_RECEIPT_SHA:
        report.bad("r3 must reuse the unchanged v7 preparation receipt identity")
    if decision.get("recipe_version") != "p0a-normalization-v1":
        report.bad("approved normalization recipe version cannot change")
    policies = {"exif_transformations": list(range(1, 9)),
                "tagged_color": "Convert to sRGB, relative colorimetric intent, black-point compensation.",
                "untagged_color": "Explicitly assume sRGB per specimen.",
                "alpha": "Retain straight alpha and hidden RGB.",
                "jpeg_matte": "White #FFFFFF only in a separately labelled JPEG matte arm.",
                "canonical_pixel_serialization": "ASCII tellpal-p0a-rgba8-srgb-straight-v1 followed by exactly one NUL byte 0x00, then width and height as unsigned big-endian uint32, then row-major packed RGBA8; no padding or metadata; SHA-256 over the entire stream."}
    if any(not same(recipe.get(key), value) for key, value in policies.items()):
        report.bad("approved normalization policy/serialization cannot change")
    implementation = mapping(recipe.get("implementation"), "normalization.recipe.implementation", report)
    if implementation.get("script_sha256") != NORMALIZATION_IMPLEMENTATION_SHA:
        report.bad("r3 normalization implementation identity must remain unchanged")
    independent = mapping(correctness.get("independent_reference_check"), "independent_reference_check", report)
    expected_independent = {"result": "pass", "report_sha256": INDEPENDENT_REPORT_SHA, "checker_sha256": INDEPENDENT_CHECKER_SHA,
                            "reference_count": 60, "reproduced_count": 60, "failed_count": 0}
    if any(not same(independent.get(key), value) for key, value in expected_independent.items()):
        if correctness.get("admitted_references"):
            report.bad("admitted references require unchanged independent v7 correctness evidence")
        else:
            report.open("independent v7 reference correctness remains incomplete")
    if len(prepared) != 60:
        report.bad("all 60 retained v7 prepared references must be preserved")
    admissions = sequence(correctness.get("admitted_references"), "admitted_references", report)
    seen = set()
    for admission in admissions:
        if not isinstance(admission, dict): continue
        ident = admission.get("id")
        if not isinstance(ident, str): continue
        if ident in seen: report.bad(f"duplicate purpose-scoped admission: {ident}")
        seen.add(ident)
        bid = admission.get("binding_id")
        if not isinstance(bid, str) or bid not in SELECTED_BINDINGS:
            report.bad(f"admission outside the two approved bindings: {ident}")
            continue
        source_hash, pixel_hash, width, height = SELECTED_BINDINGS[bid]
        binding = next((row for row in bindings if isinstance(row, dict) and row.get("id") == bid), {})
        expected = {"id": source_hash + "-rgba8-srgb-v1", "source_hash": source_hash, "pixel_sha256": pixel_hash, "width": width, "height": height,
                    "source_owner_role": "reading_page", "cms_binding_source_owner_role": binding.get("source_owner_role"),
                    "experimental_purpose": EXPERIMENTAL_PURPOSE, "freeze_revision_id": FREEZE_REVISION_ID,
                    "correctness_result": "pass", "independent_report_sha256": INDEPENDENT_REPORT_SHA,
                    "preparation_output_sha256": PREPARATION_RECEIPT_SHA}
        if any(not same(admission.get(key), value) for key, value in expected.items()):
            report.bad(f"purpose-scoped reference admission integrity mismatch: {ident}")
    expected_ids = {row[0] + "-rgba8-srgb-v1" for row in SELECTED_BINDINGS.values()}
    if seen != expected_ids:
        report.open("exactly two purpose-scoped reference admissions are required")
        if pilot.get("status") == "READY": report.bad("READY pilot lacks exactly two purpose-scoped admissions")
    for ident, ref in prepared.items():
        if not isinstance(ref, dict): continue
        if ref.get("status") == "admitted" and ident not in expected_ids:
            report.bad(f"other 58 references must remain unadmitted: {ident}")
        if ident in expected_ids:
            expected = next(row for row in SELECTED_BINDINGS.values() if row[0] + "-rgba8-srgb-v1" == ident)
            if not same([ref.get(key) for key in ("source_hash", "pixel_sha256", "width", "height")], list(expected)):
                report.bad(f"selected v7 source/pixel/geometry identity changed: {ident}")
    rows = sequence(pilot.get("experimental_geometry_evidence"), "experimental_geometry_evidence", report)
    geometry_ids = set()
    for row in rows:
        if not isinstance(row, dict): report.bad("experimental geometry evidence must be object"); continue
        bid = row.get("binding_id")
        if not isinstance(bid, str) or bid not in SELECTED_BINDINGS or bid in geometry_ids:
            report.bad("experimental geometry has unknown/duplicate approved binding")
            continue
        geometry_ids.add(bid)
        source_hash, pixel_hash, width, height = SELECTED_BINDINGS[bid]
        binding = next((x for x in bindings if isinstance(x, dict) and x.get("id") == bid), {})
        specimen = id_lookup(specimen_map, binding.get("specimen_id"), {})
        expected = {"source_sha256": source_hash, "source_owner_role": "reading_page", "cms_binding_source_owner_role": binding.get("source_owner_role"),
                    "experimental_purpose": EXPERIMENTAL_PURPOSE, "freeze_revision_id": FREEZE_REVISION_ID, "status": "verified",
                    "source_geometry": {"width": width, "height": height}, "reference_id": source_hash + "-rgba8-srgb-v1",
                    "reference_pixel_sha256": pixel_hash, "reference_geometry": {"width": width, "height": height},
                    "resize": False, "crop": False, "upscale": False, "independent_report_sha256": INDEPENDENT_REPORT_SHA, "reader_qualification_claimed": False}
        if any(not same(row.get(key), value) for key, value in expected.items()) or not same(row.get("source_geometry"), specimen.get("encoded_axes")):
            report.bad(f"experimental source-resolution geometry/purpose mismatch: {bid}")
        if not isinstance(row.get("evidence_id"), str) or not row["evidence_id"].strip():
            report.open(f"experimental geometry evidence pointer missing: {bid}")
    for bid in sorted(set(SELECTED_BINDINGS) - geometry_ids):
        report.open(f"experimental geometry evidence missing: {bid}")
        if pilot.get("status") == "READY": report.bad(f"READY pilot experimental geometry evidence missing: {bid}")


def validate_r3_ledger(data: dict, report: Report) -> None:
    """Validate the finite preregistration without producing any encoded output."""
    plan = data.get("trial_plan")
    if not isinstance(plan, dict): return
    pairs = sequence(plan.get("planned_pairs"), "trial_plan.planned_pairs", report)
    if plan.get("experimental_purpose") != EXPERIMENTAL_PURPOSE:
        report.bad("trial plan experimental purpose mismatch")
    if len(pairs) != 14:
        report.open("r3 comparison ledger requires exactly 14 preregistered pairs")
        if plan.get("status") == "PREREGISTERED": report.bad("PREREGISTERED ledger does not contain exactly 14 pairs")
    if plan.get("native_environments") != []:
        report.bad("offline batch cannot preregister native environments")
    if plan.get("native_status") != "not_applicable_offline_batch; future_qualification_pending" or plan.get("worker_status") != "not_run; measurement_deferred_story_1_4":
        report.bad("offline zero observations cannot claim native/worker success")
    qualification = {"status": "unqualified_pending_story_1_3", "qualified_runtime_scope": None, "actual_outputs_qualified": False,
                     "comparison_execution_allowed": False, "runtime_change_requires_new_revision": True}
    if not same(plan.get("provider_qualification"), qualification):
        report.bad("provider qualification must remain truthfully pending with execution disallowed")
    discovery = plan.get("provider_discovery")
    try:
        actual_discovery = installed_provider_discovery()
        if not same(discovery, actual_discovery):
            report.bad("installed provider discovery differs; new binding/revision required")
        discovery_hash = sha(canonical(discovery))
        if discovery_hash != plan.get("provider_discovery_sha256"):
            report.bad("provider discovery hash mismatch")
    except Exception as exc:
        discovery_hash = None
        report.open(f"installed provider discovery unavailable: {type(exc).__name__}")
    profile = {"policy": "omit_embedded_profile_and_exif; normalized_srgb_pixels_explicitly_assumed", "normalized_color_space": "sRGB",
               "destination_profile_sha256": "2b3aa1645779a9e634744faf9b01e9102b0c9b88fd6deced7934df86b949af7e"}
    def jpeg(quality): return {"format": "JPEG", "quality": quality, "mode": "RGB", "subsampling": 0, "optimize": False, "progressive": False, "profile_handling": profile}
    candidates = [("jpeg-q80", jpeg(80)), ("jpeg-q90", jpeg(90))]
    candidates += [(f"webp-lossy-q{quality}", {"format": "WEBP", "quality": quality, "mode": "RGB", "method": 6, "lossless": False, "profile_handling": profile}) for quality in (80, 85, 90)]
    candidates += [("webp-lossless", {"format": "WEBP", "mode": "RGB", "lossless": True, "method": 6, "exact": True, "profile_handling": profile}),
                   ("png-opaque", {"format": "PNG", "mode": "RGB", "compress_level": 6, "optimize": False, "profile_handling": profile})]
    expected_pairs = {f"{bid}-{label}-r3": (bid, settings) for bid in SELECTED_BINDINGS for label, settings in candidates}
    seen = set(); expected_order = []; attempt_ids = set()
    context = {"seed": 20261010, "environment_id": "offline-source-resolution-codec-r3", "cache_mode": "not_applicable_offline", "repeat": 1}
    if not same(plan.get("offline_order_context"), context): report.bad("offline ordering context mismatch")
    rules = {"canonical_json": "sorted_keys_compact_ascii_no_nan_infinity", "pair_key_fields": PAIR_KEY_FIELDS,
             "context_fields": ["seed", "environment_id", "cache_mode", "repeat", "pair_key_sha256"], "sort": "context_sha256_then_pair_id",
             "adjacency": "control_candidate_adjacent", "initial_arm": "first_digest_byte_even_control_odd_candidate", "maximum_repeat": 5}
    if not same(plan.get("order_rules"), rules): report.bad("deterministic ordering rules mismatch")
    binding_map = {b.get("id"): b for b in sequence(data.get("bindings"), "bindings", report) if isinstance(b, dict) and isinstance(b.get("id"), str)}
    specimen_map = {s.get("id"): s for s in sequence(data.get("specimens"), "specimens", report) if isinstance(s, dict) and isinstance(s.get("id"), str)}
    for pair in pairs:
        if not isinstance(pair, dict): continue
        ident = pair.get("id")
        if not isinstance(ident, str) or ident not in expected_pairs:
            report.bad("comparison pair outside exact r3 finite ledger")
            continue
        if ident in seen: report.bad(f"duplicate r3 comparison pair: {ident}")
        seen.add(ident); bid, settings = expected_pairs[ident]
        source_hash, pixel_hash, width, height = SELECTED_BINDINGS[bid]
        binding = binding_map.get(bid, {}); specimen = id_lookup(specimen_map, binding.get("specimen_id"), {})
        expected = {"binding_id": bid, "source_sha256": source_hash, "reference_id": source_hash + "-rgba8-srgb-v1", "reference_pixel_sha256": pixel_hash,
                    "role": "reading_page", "source_owner_role": "reading_page", "cms_binding_source_owner_role": binding.get("source_owner_role"),
                    "experimental_purpose": EXPERIMENTAL_PURPOSE, "geometry": {"width": width, "height": height}, "control_settings": jpeg(85),
                    "candidate_settings": settings, "baseline_denominator": "normalized-same-geometry-jpeg-q85", "arm_type": "same_geometry_codec",
                    "freeze_revision_id": FREEZE_REVISION_ID, "state": "not_run", "provider_discovery_sha256": discovery_hash,
                    "provider_qualification_status": "unqualified_pending_story_1_3", "raw_byte_delta": None, "quality_qualified_savings": None,
                    "reader_verdict": "pending", "native_verdict": "pending", "attempts": [], "attempt_limits": {"scheduled_per_arm": 1, "retry_limit": 1, "maximum_attempts_per_arm": 2},
                    "metric_counts": {"successful": 0, "failed": 0, "retry": 0, "metric_valid": 0},
                    "pixel_input_projection": {"source": "canonical_rgba8_srgb_straight_v1", "conversion": "drop_alpha_only_after_verified_opaque_255", "mode": "RGB", "resize": False, "crop": False, "upscale": False}}
        for key, value in expected.items():
            if key not in pair or not same(pair[key], value): report.bad(f"comparison pair r3 field mismatch ({key}): {ident}")
        denominator = {"label": "original_source_bytes", "source_sha256": source_hash, "bytes": object_value(specimen.get("integrity")).get("bytes"), "verified": True}
        if not same(pair.get("original_byte_denominator"), denominator) or type(denominator["bytes"]) is not int or denominator["bytes"] <= 0:
            report.bad(f"original-byte denominator must be positive verified source bytes: {ident}")
        unavailable = {"status": "unavailable", "sha256": None, "bytes": None, "actual_mime": None, "round_trip_decode": None, "verified": False}
        if not same(pair.get("outputs"), {"control": unavailable, "candidate": unavailable}):
            report.bad(f"Story 1.2 outputs must remain unavailable, never zero: {ident}")
        visual = mapping(pair.get("visual_assessment"), f"{ident}.visual_assessment", report)
        if visual.get("verdict") != "pending" or visual.get("evaluator_id") is not None or visual.get("viewing_conditions") is not None or visual.get("reassessment_history") != [] or not visual.get("reason") or not visual.get("blinding_limitations"):
            report.bad(f"visual verdict/conditions must remain truthfully pending: {ident}")
        reasons = ["not_run", "provider_unqualified", "outputs_unavailable", "visual_verdict_pending", "reader_native_qualification_open"]
        if pair.get("exclusion_reasons") != reasons: report.bad(f"all pair exclusion reasons must remain visible: {ident}")
        planned_ids = {arm: [f"{ident}-{arm}-attempt-1", f"{ident}-{arm}-retry-1"] for arm in ("control", "candidate")}
        if pair.get("planned_attempt_ids") != planned_ids: report.bad(f"distinct initial/retry attempt IDs missing: {ident}")
        for ids in planned_ids.values():
            for attempt_id in ids:
                if attempt_id in attempt_ids: report.bad("planned attempt ID is reused")
                attempt_ids.add(attempt_id)
        if pair.get("worker_fields_applicability") != "deferred_story_1_4; unavailable" or pair.get("native_fields_applicability") != "inapplicable_offline_batch; future_gate_open":
            report.bad(f"worker/native field applicability mismatch: {ident}")
        try:
            key = {field: plan.get("seed") if field == "seed" else pair.get(field) for field in PAIR_KEY_FIELDS}
            key_hash = sha(canonical(key))
            if pair.get("pair_key_sha256") != key_hash: report.bad(f"comparison pair-key hash mismatch: {ident}")
            context_hash = sha(canonical({**context, "pair_key_sha256": key_hash}))
            first = "control" if int(context_hash[:2], 16) % 2 == 0 else "candidate"
            expected_order.append({"pair_id": ident, "context_sha256": context_hash, "arms": [first, "candidate" if first == "control" else "control"]})
        except (ValueError, TypeError): report.bad(f"noncanonical pair/context values: {ident}")
    if seen != set(expected_pairs): report.open("r3 ledger missing one or more exact approved candidate pairs")
    if plan.get("offline_order") != sorted(expected_order, key=lambda row: (row["context_sha256"], row["pair_id"])):
        report.bad("seeded interleaved adjacency/parity/tie-break order mismatch")
    expected_counts = {"not_run": 14, "eligible": 0, "excluded": 14, "pending_provider": 14, "outputs_available": 0, "visual_passed": 0,
                       "reader_qualified": 0, "native_qualified": 0, "scheduled_offline_arm_observations": 28, "maximum_offline_attempts": 56, "actual_attempts": 0}
    if not same(plan.get("planned_counts"), expected_counts): report.bad("planned/eligible/excluded/offline attempt counts mismatch")


def validate(data: object, source_root: Path | None, protocol: Path, reference_root: Path | None, receipt_path: Path | None, report: Report,
             manifest_path: Path | None = None, corpus_hash: str | None = None) -> None:
    if not isinstance(data, dict):
        report.bad("corpus root must be an object")
        return
    required = {"schema_version", "artifact", "source_policy", "freeze", "normalization", "specimens", "historical_controls", "bindings", "coverage_matrix", "synthetic_rejection_fixtures", "pilot_scope", "future_qualification", "pilot_subset", "gates"}
    missing = required - set(data)
    for field in sorted(missing): report.bad(f"missing top-level field: {field}")
    for field in missing:
        data[field] = {} if field in {"source_policy", "freeze", "normalization", "pilot_scope", "future_qualification", "pilot_subset"} else []
    if data.get("schema_version") != 1 or data.get("artifact") != "p0a-corpus": report.bad("unsupported schema or artifact")
    scan_private_values(data, report)
    policy = mapping(data.get("source_policy"), "source_policy", report)
    required_pilot_classes, excluded_pilot_classes = validate_pilot_scope(data, report)
    if policy.get("cms_content_id") != 71: report.bad("CMS acquisition must include content 71")
    if policy.get("maximum_story_count") != MAX_STORY_COUNT: report.bad(f"maximum_story_count must be {MAX_STORY_COUNT}")
    if policy.get("maximum_retained_originals") != MAX_RETAINED_ORIGINALS: report.bad(f"maximum_retained_originals must be {MAX_RETAINED_ORIGINALS}")
    if policy.get("workflow_story_key") is not None: report.bad("workflow story_key must remain null")

    # Gates are part of the machine-readable handoff contract.  An unresolved
    # gate must keep the result INCOMPLETE even when the rest of the projection
    # is structurally valid; an unknown gate state is an integrity error.
    gates = sequence(data.get("gates"), "gates", report)
    gate_ids: set[str] = set()
    for gate in gates:
        if not isinstance(gate, dict):
            report.bad("gate must be an object")
            continue
        gate_id = gate.get("id")
        if not isinstance(gate_id, str) or not gate_id:
            report.bad("gate id must be a nonempty string")
        else:
            if gate_id in gate_ids:
                report.bad(f"duplicate gate id: {gate_id}")
            gate_ids.add(gate_id)
        state = gate.get("status")
        if not isinstance(state, str) or state not in {"satisfied", "open", "blocked", "pending"}:
            report.bad(f"{gate_id}: invalid gate status")
        elif state != "satisfied":
            report.open(f"{gate_id}: unresolved gate ({state})")
        if not isinstance(gate.get("reason"), str) or not gate["reason"].strip():
            report.bad(f"{gate_id}: gate reason missing")
    freeze = mapping(data.get("freeze"), "freeze", report)
    if any(key in freeze for key in ("corpus_sha256", "manifest_sha256", "freeze_manifest_sha256")):
        report.bad("corpus freeze cannot contain self-referential corpus/manifest hashes")
    for key, expected in (("revision_id", FREEZE_REVISION_ID), ("corpus_snapshot_id", CORPUS_SNAPSHOT_ID), ("protocol_snapshot_id", PROTOCOL_SNAPSHOT_ID)):
        if freeze.get(key) != expected:
            report.bad(f"freeze.{key} must be {expected}")
    for key in ("revision_id", "corpus_snapshot_id", "protocol_snapshot_id", "protocol_path"):
        if not isinstance(freeze.get(key), str) or not freeze[key]: report.bad(f"freeze.{key} missing")
    if freeze.get("protocol_path") != protocol.name: report.bad("protocol path mismatch")
    try:
        protocol_raw = protocol.read_bytes()
        protocol_text = protocol_raw.decode("utf-8")
    except (OSError, UnicodeError) as exc:
        report.bad(f"protocol cannot be read/decoded: {exc}")
        protocol_raw, protocol_text = b"", ""
    if not SHA.fullmatch(str(freeze.get("protocol_sha256"))): report.open("protocol hash is not pinned")
    elif sha(protocol_raw) != freeze.get("protocol_sha256"): report.bad("protocol hash mismatch")
    for key in ("revision_id", "corpus_snapshot_id", "protocol_snapshot_id"):
        match = re.search(r"authoritative snapshot binding is: revision `([^`]+)`, corpus snapshot `([^`]+)`, protocol snapshot `([^`]+)`", protocol_text)
        positions = {"revision_id": 1, "corpus_snapshot_id": 2, "protocol_snapshot_id": 3}
        if not match or freeze.get(key) != match.group(positions[key]): report.bad(f"protocol does not bind freeze.{key}")
    if freeze.get("freeze_ready") is not True:
        report.open("corpus/protocol freeze is not ready")
    if manifest_path is None:
        report.open("detached immutable freeze manifest is not supplied")
    else:
        try:
            if manifest_path.is_symlink() or manifest_path.resolve().is_relative_to(REPO.resolve()) or manifest_path.stat().st_size > 1024 * 1024:
                raise ValueError("unsafe or oversized detached manifest")
            manifest = json.loads(manifest_path.read_bytes())
            scan_private_values(manifest, report)
            normalization = data.get("normalization") or {}
            implementation = (normalization.get("recipe") or {}).get("implementation") or {}
            refs = (normalization.get("correctness") or {}).get("prepared_references") or []
            reference_set = [{k: x.get(k) for k in ("id", "source_hash", "pixel_sha256", "width", "height", "implementation_sha256", "destination_profile_sha256", "recipe_version")}
                             for x in refs if isinstance(x, dict)]
            reference_set.sort(key=lambda x: str(x["id"]))
            expected_manifest = {k: freeze.get(k) for k in ("revision_id", "corpus_snapshot_id", "protocol_snapshot_id", "protocol_sha256")}
            expected_manifest.update({"schema_version": 1, "corpus_sha256": corpus_hash,
                "normalization_implementation_sha256": implementation.get("script_sha256"),
                "reference_set_sha256": sha(json.dumps(reference_set, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode())})
            if not corpus_hash: report.open("corpus byte snapshot hash unavailable for manifest verification")
            for key, value in expected_manifest.items():
                if not isinstance(manifest, dict) or manifest.get(key) != value:
                    report.bad(f"detached freeze manifest mismatch: {key}")
        except Exception as exc:
            report.bad(f"detached freeze manifest invalid: {type(exc).__name__}")
    specimens = sequence(data.get("specimens"), "specimens", report)
    validate_preservation(data, report)
    specimen_map: dict[str, dict] = {}
    for item in specimens:
        if not isinstance(item, dict) or not isinstance(item.get("id"), str): report.bad("specimen must have string id"); continue
        if item["id"] in specimen_map: report.bad(f"duplicate specimen id: {item['id']}")
        specimen_map[item["id"]] = item
    bindings = sequence(data.get("bindings"), "bindings", report)
    bind_ids: set[str] = set(); tuples: set[tuple] = set()
    for item in bindings:
        if not isinstance(item, dict): report.bad("binding must be object"); continue
        ident = item.get("id")
        if not isinstance(ident, str) or ident in bind_ids: report.bad(f"duplicate or malformed binding id: {ident}")
        if isinstance(ident, str): bind_ids.add(ident)
        if not isinstance(item.get("specimen_id"), str) or item.get("specimen_id") not in specimen_map: report.bad(f"{ident}: unknown specimen")
        key = canonical({k: item.get(k) for k in ("content_id", "asset_id", "page_number", "language", "source_owner_role")})
        if key in tuples: report.bad(f"duplicate binding tuple: {ident}")
        tuples.add(key)
        specimen = id_lookup(specimen_map, item.get("specimen_id"))
        if specimen:
            for field in ("content_id", "asset_id", "page_number", "language"):
                if item.get(field) != specimen.get(field): report.bad(f"{ident}: binding {field} differs from specimen")
            if ident not in string_ids(specimen.get("bindings"), f"{specimen.get('id')}.bindings", report): report.bad(f"{ident}: specimen does not backlink binding")
            if item.get("role") not in string_ids(specimen.get("roles"), f"{specimen.get('id')}.roles", report): report.bad(f"{ident}: role is not declared by specimen")
            kind = specimen.get("kind")
            if not isinstance(kind, str): report.bad(f"{ident}: specimen kind must be string")
            owner = id_lookup({"story_page": "story_page", "reading_cover": "localized_cover", "source_cover": "story_cover"}, kind)
            if owner and item.get("source_owner_role") != owner: report.bad(f"{ident}: source ownership differs from specimen kind")
        if type(item.get("content_id")) is not int or type(item.get("asset_id")) is not int:
            report.bad(f"{ident}: CMS content/asset IDs must be integers")
    for item in specimens:
        if not isinstance(item, dict): continue
        ident = item.get("id", "<unknown>"); meta = mapping(item.get("metadata"), f"{ident}.metadata", report)
        exif_value = meta.get("exif")
        exif = {} if exif_value is None else mapping(exif_value, f"{ident}.metadata.exif", report)
        if exif.get("present") is False and exif.get("orientation") is not None: report.bad(f"{ident}: absent EXIF orientation must be null")
        icc_value = meta.get("icc")
        if icc_value is None:
            report.open(f"{ident}: ICC metadata unavailable")
        else:
            icc = mapping(icc_value, f"{ident}.metadata.icc", report)
            if icc.get("present") is False and not icc.get("assumed_srgb"): report.open(f"{ident}: untagged sRGB assumption missing")
        acquisition = item.get("acquisition")
        if isinstance(acquisition, dict) and acquisition.get("access_url"): report.bad(f"{ident}: access URL leaked")
        elif acquisition is not None and not isinstance(acquisition, dict): report.bad(f"{ident}: acquisition must be object")
        if isinstance(acquisition, dict):
            confirmation = mapping(acquisition.get("binding_verification"), f"{ident}.binding_verification", report)
            if confirmation.get("status") != "confirmed" or not all(confirmation.get(k) for k in ("actor", "date", "evidence_id")):
                report.open(f"{ident}: CMS binding confirmation unresolved")
    observations = sequence(data.get("byte_observations"), "byte_observations", report)
    observed = {x.get("id") for x in observations if isinstance(x, dict) and isinstance(x.get("id"), str)}
    historical = sequence(data.get("historical_controls"), "historical_controls", report)
    required_observed = set(specimen_map) | {x.get("id") for x in historical if isinstance(x, dict) and isinstance(x.get("id"), str)}
    retained_hashes = {x["integrity"]["sha256"] for x in specimens if isinstance(x, dict) and isinstance(x.get("integrity"), dict) and isinstance(x["integrity"].get("sha256"), str)}
    retained_hashes |= {x["sha256"] for x in historical if isinstance(x, dict) and isinstance(x.get("sha256"), str)}
    if len(retained_hashes) > MAX_RETAINED_ORIGINALS:
        report.bad(f"retained original count exceeds {MAX_RETAINED_ORIGINALS}")
    content_ids = {x.get("content_id") for x in specimens if isinstance(x, dict) and type(x.get("content_id")) is int}
    if type(policy.get("cms_content_id")) is int: content_ids.add(policy["cms_content_id"])
    if len(content_ids) > MAX_STORY_COUNT:
        report.bad(f"source acquisition exceeds content 71 plus {MAX_STORY_COUNT - 1} story bound")
    for ident in sorted(required_observed - observed): report.open(f"{ident}: full byte metadata observation missing")
    cells = sequence(data.get("coverage_matrix"), "coverage_matrix", report)
    cell_ids: set[str] = set()
    for cell in cells:
        if not isinstance(cell, dict): report.bad("coverage cell must be object"); continue
        ident = cell.get("id")
        if not isinstance(ident, str): report.bad("coverage id must be a string"); continue
        if ident in cell_ids: report.bad(f"duplicate/malformed coverage id: {ident}")
        cell_ids.add(ident)
        if ident not in REQUIRED_COVERAGE:
            report.bad(f"unexpected coverage cell: {ident}")
            continue
        expected_class, expected_applicable, allowed_language = REQUIRED_COVERAGE[ident]
        if cell.get("class") != expected_class or cell.get("applicable") is not expected_applicable:
            report.bad(f"{ident}: immutable class/applicability policy mismatch")
        if allowed_language != "*" and cell.get("language") != allowed_language:
            report.bad(f"{ident}: immutable language policy mismatch")
        status = cell.get("status")
        if not isinstance(status, str) or status not in {"satisfied", "missing", "deferred"}: report.bad(f"{ident}: invalid status")
        if cell.get("applicable", True) and status == "deferred": report.bad(f"{ident}: applicable class cannot be deferred")
        if expected_class in EXCLUDED_PILOT_CLASSES:
            if cell.get("required_for_current_pilot") is not False:
                report.bad(f"{ident}: excluded future class must declare required_for_current_pilot false")
            if status != "missing":
                report.bad(f"{ident}: excluded future class must remain missing")
            gate_id = next(key for key, value in FUTURE_GATES.items() if value == expected_class)
            if cell.get("future_qualification_gate_id") != gate_id:
                report.bad(f"{ident}: excluded future class gate linkage mismatch")
            if cell.get("specimen_ids") != [] or cell.get("hashes") != [] or cell.get("suitability_evidence") is not None:
                report.bad(f"{ident}: excluded future class has fabricated evidence")
        elif expected_applicable and cell.get("required_for_current_pilot") is not True:
            report.bad(f"{ident}: current coverage class must be required for pilot")
        refs = cell.get("specimen_ids"); hashes = cell.get("hashes")
        if not isinstance(refs, list) or not isinstance(hashes, list): report.bad(f"{ident}: specimen_ids/hashes must be arrays"); continue
        if status == "satisfied":
            if len(refs) != len(hashes) or not refs: report.bad(f"{ident}: satisfied cell lacks links")
            for ref, expected in zip(refs, hashes):
                if not isinstance(ref, str) or ref not in specimen_map: report.bad(f"{ident}: unknown specimen link")
                elif expected != mapping(specimen_map[ref].get("integrity"), f"{ref}.integrity", report).get("sha256"): report.bad(f"{ident}: linked hash does not equal specimen hash")
                elif not feature_matches(specimen_map[ref], expected_class, cell.get("language")):
                    report.bad(f"{ident}: linked specimen does not demonstrate required class")
            if not isinstance(cell.get("suitability_evidence"), str) or not cell["suitability_evidence"]: report.bad(f"{ident}: suitability evidence missing")
        elif expected_class in REQUIRED_CLASSES and expected_applicable and expected_class not in EXCLUDED_PILOT_CLASSES:
            report.open(f"{ident}: required coverage unresolved")
        if status == "missing" and (not isinstance(cell.get("next_action"), str) or not cell["next_action"].strip()):
            report.bad(f"{ident}: missing coverage next action unavailable")
    required_ids = set(REQUIRED_COVERAGE)
    for ident in sorted(required_ids - cell_ids):
        if REQUIRED_COVERAGE[ident][0] in EXCLUDED_PILOT_CLASSES:
            report.bad(f"future qualification coverage cell missing: {ident}")
        else:
            report.open(f"mandatory coverage cell missing: {ident}")
    fixture_categories: list[str] = []
    for fixture in sequence(data.get("synthetic_rejection_fixtures"), "synthetic_rejection_fixtures", report):
        if not isinstance(fixture, dict): report.bad("fixture must be object"); continue
        category = fixture.get("category")
        if not isinstance(category, str) or category not in {"truncated", "mime_mismatch", "excessive_bytes", "excessive_pixels"}: report.bad(f"{fixture.get('id')}: invalid fixture category")
        else: fixture_categories.append(fixture["category"])
        if fixture.get("sha256") is None: report.open(f"{fixture.get('id')}: fixture hash unavailable")
        elif not SHA.fullmatch(str(fixture.get("sha256"))): report.bad(f"{fixture.get('id')}: fixture hash malformed")
        if fixture.get("valid_quality_specimen") is not False: report.bad(f"{fixture.get('id')}: fixture must remain excluded from quality specimens")
        expected_rejections = {"truncated": "decode_truncated", "mime_mismatch": "declared_actual_mime_mismatch", "excessive_bytes": "source_byte_limit", "excessive_pixels": "source_pixel_limit"}
        if fixture.get("expected_rejection") != id_lookup(expected_rejections, category):
            report.bad(f"{fixture.get('id')}: expected rejection does not match fixture category")
    for category in sorted({"truncated", "mime_mismatch", "excessive_bytes", "excessive_pixels"} - set(fixture_categories)):
        report.open(f"required rejection fixture missing: {category}")
    if len(fixture_categories) != len(set(fixture_categories)):
        report.bad("rejection fixture categories must not be duplicated")
    norm = mapping(data.get("normalization"), "normalization", report); decision = mapping(norm.get("decision"), "normalization.decision", report); recipe = mapping(norm.get("recipe"), "normalization.recipe", report); correctness = mapping(norm.get("correctness"), "normalization.correctness", report)
    for field in ("implementation", "destination_profile"):
        recipe[field] = mapping(recipe.get(field), f"normalization.recipe.{field}", report)
    validate_preparation(norm, receipt_path, report)
    if decision.get("status") != "approved" or not decision.get("approver") or not decision.get("decision_date"): report.open("normalization approval provenance incomplete")
    for key in ("implementation_identity", "runtime_identity", "reference_store"):
        if not recipe.get(key): report.open(f"normalization.{key} missing")
    checks = sequence(correctness.get("checks"), "normalization.correctness.checks", report)
    admitted = sequence(correctness.get("admitted_references"), "normalization.correctness.admitted_references", report)
    if not checks:
        report.open("normalization correctness checks are missing")
    check_map: dict[str, dict] = {}
    for check in checks:
        if not isinstance(check, dict): report.bad("correctness check must be object"); continue
        ident = check.get("id")
        if not isinstance(ident, str) or not ident: report.bad("correctness check id must be nonempty")
        elif ident in check_map: report.bad(f"duplicate correctness check id: {ident}")
        else: check_map[ident] = check
        if check.get("result") != "pass": report.open(f"correctness check failed/unresolved: {ident}")
    for ident in sorted(REQUIRED_CHECKS - set(check_map)):
        report.open(f"mandatory correctness check missing: {ident}")
    for ref in admitted:
        if not isinstance(ref, dict): report.bad("admitted reference must be object"); continue
        ref_id = ref.get("id")
        if not isinstance(ref_id, str) or not ref_id: report.bad("admitted reference id must be nonempty")
        if not SHA.fullmatch(str(ref.get("pixel_sha256"))): report.bad(f"admitted reference hash malformed: {ref_id}")
        if ref.get("check_id") is not None and not isinstance(ref.get("check_id"), str):
            report.bad(f"admitted reference check ID must be a string: {ref_id}")
        linked = id_lookup(check_map, ref.get("check_id"))
        if ref.get("check_id") and linked and linked.get("result") != "pass":
            report.bad(f"admitted reference links failed correctness check: {ref_id}")
        if ref.get("correctness_result") != "pass" or (linked and linked.get("result") != "pass"):
            report.bad(f"admitted reference is not independently correct: {ref_id}")
    prepared_rows = sequence(correctness.get("prepared_references"), "normalization.correctness.prepared_references", report)
    admitted_ids = {x.get("id") for x in admitted if isinstance(x, dict) and isinstance(x.get("id"), str)}
    prepared_ids: set[str] = set()
    for ref in prepared_rows:
        if not isinstance(ref, dict):
            report.bad("prepared reference must be object")
            continue
        if not isinstance(ref.get("id"), str) or not SHA.fullmatch(str(ref.get("pixel_sha256"))):
            report.bad(f"prepared reference identity/hash malformed: {ref.get('id')}")
        elif ref["id"] in prepared_ids:
            report.bad(f"duplicate prepared reference ID: {ref['id']}")
        else:
            prepared_ids.add(ref["id"])
        if any(type(ref.get(axis)) is not int or ref[axis] <= 0 for axis in ("width", "height")):
            report.bad(f"prepared reference geometry must be positive integers: {ref.get('id')}")
        expected_reference = {"implementation_sha256": (recipe.get("implementation") or {}).get("script_sha256"),
                              "destination_profile_sha256": (recipe.get("destination_profile") or {}).get("sha256"),
                              "recipe_version": decision.get("recipe_version"), "preparation_output_sha256": correctness.get("preparation_output_sha256")}
        if any(not same(ref.get(key), value) for key, value in expected_reference.items()):
            report.bad(f"prepared reference recipe/runtime projection changed: {ref.get('id')}")
        if not isinstance(ref.get("status"), str) or ref.get("status") not in {"unadmitted_preflight", "admitted"}:
            report.bad(f"prepared reference status invalid: {ref.get('id')}")
        if ref.get("status") == "admitted" and (not isinstance(ref.get("id"), str) or ref.get("id") not in admitted_ids):
            report.bad(f"prepared reference marked admitted without admission record: {ref.get('id')}")
    prepared_map = {x.get("id"): x for x in prepared_rows if isinstance(x, dict) and isinstance(x.get("id"), str)}
    for ref in admitted:
        if not isinstance(ref, dict): continue
        original = id_lookup(prepared_map, ref.get("id"))
        if original is None or any(ref.get(k) != original.get(k) for k in ("source_hash", "pixel_sha256", "width", "height")):
            report.bad(f"admitted reference is not bound to prepared source/pixels: {ref.get('id')}")
        if not REQUIRED_CHECKS <= set(check_map) or any(x.get("result") != "pass" for x in check_map.values()):
            report.bad(f"admitted reference violates failed/missing correctness criteria: {ref.get('id')}")
    pilot = mapping(data.get("pilot_subset"), "pilot_subset", report)
    pilot_bindings = string_ids(pilot.get("bindings"), "pilot_subset.bindings", report)
    prepared = {x.get("id"): x for x in sequence(correctness.get("prepared_references"), "normalization.correctness.prepared_references", report) if isinstance(x, dict) and isinstance(x.get("id"), str)}
    validate_source_resolution(data, prepared, bindings, specimen_map, report)
    if reference_root is not None:
        refs_dir = reference_root.resolve()
        if refs_dir.is_relative_to(REPO.resolve()): report.bad("reference-root must be outside repository")
        for ref_id, ref in prepared.items():
            if not isinstance(ref_id, str) or not isinstance(ref, dict):
                report.bad("prepared reference must have object identity")
                continue
            if not re.fullmatch(r"[0-9a-f]{64}-rgba8-srgb-v1", str(ref_id)):
                report.bad(f"prepared reference id is unsafe: {ref_id}")
                continue
            try:
                ref_path = refs_dir / f"{ref_id}.pixels"
                if ref_path.is_symlink() or not ref_path.resolve().is_relative_to(refs_dir):
                    report.bad(f"prepared reference escapes reference-root: {ref_id}")
                    continue
                if not ref_path.is_file() or ref_path.is_symlink():
                    report.open(f"prepared reference bytes unavailable: {ref_id}")
                    continue
                if ref_path.stat().st_size > len(MAGIC) + 8 + 4 * MAX_PIXELS:
                    report.bad(f"prepared reference exceeds finite pixel stream bound: {ref_id}")
                    continue
                actual_hash, actual_bytes = stream_hash(ref_path)
                if actual_hash != ref.get("pixel_sha256"): report.bad(f"prepared reference hash mismatch: {ref_id}")
                canonical_header_bytes = len(MAGIC) + 8
                if actual_bytes < canonical_header_bytes: report.bad(f"prepared reference too short: {ref_id}")
                else:
                    with ref_path.open("rb") as stream:
                        header = stream.read(canonical_header_bytes)
                    if not header.startswith(MAGIC):
                        report.bad(f"prepared reference canonical magic mismatch: {ref_id}")
                    else:
                        width, height = struct.unpack(">II", header[-8:])
                        if [width, height] != [ref.get("width"), ref.get("height")]: report.bad(f"prepared reference geometry mismatch: {ref_id}")
                        if width == 0 or height == 0 or width * height > MAX_PIXELS: report.bad(f"prepared reference pixel cap: {ref_id}")
                        if actual_bytes != canonical_header_bytes + width * height * 4: report.bad(f"prepared reference byte geometry mismatch: {ref_id}")
            except OSError as exc:
                report.bad(f"prepared reference read/stat failed: {ref_id}: {exc}")
                continue
    else:
        report.open("--reference-root was not supplied; reference pixels remain unverified")
    pilot_refs = sequence(pilot.get("references"), "pilot_subset.references", report)
    pilot_cells = sequence(pilot.get("coverage_matrix"), "pilot_subset.coverage_matrix", report)
    pilot_classes = {x.get("class") for x in pilot_cells if isinstance(x, dict) and isinstance(x.get("class"), str)}
    selected_binding_ids = set(pilot_bindings)
    selected_refs: set[str] = set()
    for binding_id in pilot_bindings:
        binding = next((x for x in bindings if isinstance(x, dict) and x.get("id") == binding_id), None)
        if binding is None:
            report.bad(f"pilot subset links unknown binding: {binding_id}")
            continue
        specimen = id_lookup(specimen_map, binding.get("specimen_id"))
        if not isinstance(specimen, dict):
            report.bad(f"pilot subset binding has unknown specimen: {binding_id}")
            continue
        validate_pilot_binding(binding, specimen, None, f"pilot binding {binding_id}", report)
    for ref in pilot_refs:
        if not isinstance(ref, dict) or not isinstance(ref.get("id"), str) or ref["id"] not in prepared:
            report.bad(f"pilot reference is not prepared: {ref.get('id') if isinstance(ref, dict) else ref}")
            continue
        if ref["id"] in selected_refs:
            report.bad(f"pilot reference is duplicated: {ref['id']}")
        selected_refs.add(ref["id"])
        original = prepared[ref["id"]]
        if any(ref.get(k) != original.get(k) for k in ("source_hash", "pixel_sha256", "width", "height", "status")):
            report.bad(f"pilot reference projection differs from prepared identity: {ref['id']}")
        if not selected_binding_ids.intersection(string_ids(original.get("binding_ids"), f"{ref['id']}.binding_ids", report)):
            report.bad(f"pilot reference is outside selected bindings: {ref['id']}")
        if pilot.get("status") == "READY" and original.get("status") != "admitted":
            report.bad(f"pilot reference is not admitted: {ref['id']}")
    seen_pilot_classes: set[str] = set()
    for cell in pilot_cells:
        if not isinstance(cell, dict):
            report.bad("pilot coverage cell must be object")
            continue
        cls = cell.get("class")
        if not isinstance(cls, str):
            report.bad("pilot coverage class must be a string")
            continue
        if cls in seen_pilot_classes:
            report.bad(f"pilot coverage class is duplicated: {cls}")
        seen_pilot_classes.add(cls)
        source_id = cell.get("source_binding_id")
        ref_id = cell.get("reference_id")
        if cls in EXCLUDED_PILOT_CLASSES:
            if cell.get("status") != "missing" or any(cell.get(key) is not None for key in ("source_binding_id", "source_sha256", "reference_id", "reference_pixel_sha256")):
                report.bad(f"pilot future class must remain explicitly missing: {cell.get('id')}")
            gate_id = next(key for key, value in FUTURE_GATES.items() if value == cls)
            if cell.get("required_for_current_pilot") is not False or cell.get("future_qualification_gate_id") != gate_id:
                report.bad(f"pilot future class gate linkage mismatch: {cell.get('id')}")
            continue
        if cls not in required_pilot_classes:
            report.bad(f"pilot coverage has unexpected class: {cell.get('id')}")
            continue
        if cell.get("required_for_current_pilot") is not True:
            report.bad(f"pilot current class must be required: {cell.get('id')}")
        if cell.get("experimental_purpose") != EXPERIMENTAL_PURPOSE or cell.get("freeze_revision_id") != FREEZE_REVISION_ID:
            report.bad(f"pilot coverage purpose/revision mismatch: {cell.get('id')}")
        status = cell.get("status")
        if not isinstance(status, str) or status not in {"missing", "pending_admission", "satisfied"}:
            report.bad(f"pilot coverage status invalid: {cell.get('id')}")
        if status != "satisfied":
            report.open(f"pilot coverage unresolved: {cls} ({status})")
            if pilot.get("status") == "READY":
                report.bad(f"READY pilot has unresolved coverage: {cls}")
        if status == "missing":
            if any(cell.get(key) is not None for key in ("source_binding_id", "source_sha256", "reference_id", "reference_pixel_sha256", "suitability_evidence")):
                report.bad(f"missing pilot coverage has fabricated evidence: {cell.get('id')}")
            if not isinstance(cell.get("next_action"), str) or not cell["next_action"].strip():
                report.bad(f"missing pilot coverage next action unavailable: {cell.get('id')}")
            continue
        binding = next((x for x in bindings if isinstance(x, dict) and x.get("id") == source_id), None) if isinstance(source_id, str) else None
        reference = prepared.get(ref_id) if isinstance(ref_id, str) else None
        if binding is None or reference is None:
            report.bad(f"pilot cell links unknown binding/reference: {cell.get('id')}")
            continue
        specimen = id_lookup(specimen_map, binding.get("specimen_id"), {})
        validate_pilot_binding(binding, specimen, reference, f"pilot cell {cell.get('id')}", report)
        source_hash = object_value(specimen.get("integrity")).get("sha256")
        if source_id not in selected_binding_ids or ref_id not in selected_refs:
            report.bad(f"pilot cell is outside selected bindings/references: {cell.get('id')}")
        if reference.get("source_hash") != source_hash or not feature_matches(specimen, cls):
            report.bad(f"pilot cell source/reference does not demonstrate required class: {cell.get('id')}")
        if not cell.get("suitability_evidence") or cell.get("source_sha256") != source_hash or cell.get("reference_pixel_sha256") != reference.get("pixel_sha256"):
            report.bad(f"pilot cell lacks bound suitability/source/reference evidence: {cell.get('id')}")
        if pilot.get("status") == "READY" and (reference.get("status") != "admitted" or ref_id not in admitted_ids):
            report.bad(f"pilot cell reference is not admitted: {cell.get('id')}")
        if status == "satisfied" and (reference.get("status") != "admitted" or ref_id not in admitted_ids):
            report.bad(f"pilot coverage satisfied without admitted reference: {cell.get('id')}")
    for cls in sorted(required_pilot_classes):
        if cls not in pilot_classes:
            report.open(f"pilot coverage missing: {cls}")
    for cls in sorted(excluded_pilot_classes - pilot_classes):
        report.bad(f"pilot future qualification coverage missing: {cls}")
    if pilot.get("status") != "READY" or not pilot_refs:
        report.open("pilot subset is not ready")
    if pilot.get("geometry_evidence") is None:
        report.open("pilot role geometry evidence is unavailable")
        geometry_rows = []
    else:
        geometry_rows = sequence(pilot.get("geometry_evidence"), "pilot_subset.geometry_evidence", report)
    geometry_ids: set[str] = set()
    reader_claim = pilot.get("reader_qualification_claimed") is not False
    reader_claim = reader_claim or any(isinstance(row, dict) and row.get("status") in ("verified", "insufficient_pixels") for row in geometry_rows)
    plan_for_claim = data.get("trial_plan")
    if isinstance(plan_for_claim, dict):
        claim_pairs = plan_for_claim.get("planned_pairs")
        claim_pairs = claim_pairs if isinstance(claim_pairs, list) else []
        reader_claim = reader_claim or any(isinstance(pair, dict) and (pair.get("reader_qualification_claimed") is True or pair.get("experimental_purpose") == "reader_demand_qualification")
                                         for pair in claim_pairs)
    if reader_claim:
        report.bad("r3 preparation cannot claim reader-demand qualification without a separately authorized revision")
    for row in geometry_rows:
        if not isinstance(row, dict) or not isinstance(row.get("binding_id"), str):
            report.bad("pilot geometry evidence requires object/string binding ID")
            continue
        binding_id = row["binding_id"]
        if binding_id not in selected_binding_ids or binding_id in geometry_ids:
            report.bad(f"pilot geometry evidence has unselected/duplicate binding: {binding_id}")
        geometry_ids.add(binding_id)
        binding = next((x for x in bindings if isinstance(x, dict) and x.get("id") == binding_id), {})
        specimen = id_lookup(specimen_map, binding.get("specimen_id"), {})
        if row.get("source_sha256") != object_value(specimen.get("integrity")).get("sha256") or row.get("role") != binding.get("role"):
            report.bad(f"pilot role geometry source/role identity mismatch: {binding_id}")
        if row.get("freeze_revision_id") != freeze.get("revision_id"):
            report.bad(f"pilot role geometry revision mismatch: {binding_id}")
        status = row.get("status")
        if not reader_claim:
            if status != "unavailable" or row.get("qualification_purpose") != "reader_demand_qualification" or row.get("future_qualification_gate_id") != "FQ-READER":
                report.bad(f"reader geometry must remain unavailable behind its open gate: {binding_id}")
            for field in ("unclamped_required_full_image", "source_pixels_sufficient", "reference_id", "reference_pixel_sha256", "reference_geometry", "evidence_id", "logical_slot", "dpr", "fit", "native_viewing_quality"):
                if field not in row or row[field] is not None:
                    report.bad(f"reader geometry must preserve explicit null evidence ({field}): {binding_id}")
        if status not in ("unknown", "unavailable", "insufficient_pixels", "verified"):
            report.bad(f"pilot geometry evidence status invalid: {binding_id}")
        if status != "verified":
            if reader_claim: report.open(f"pilot role geometry unresolved: {binding_id} ({status})")
            if not isinstance(row.get("next_action"), str) or not row["next_action"].strip():
                report.bad(f"pilot role geometry next action missing: {binding_id}")
            if reader_claim and pilot.get("status") == "READY":
                report.bad(f"READY pilot role geometry unresolved: {binding_id}")
        if status in ("verified", "insufficient_pixels"):
            ref_id = row.get("reference_id")
            reference = prepared.get(ref_id) if isinstance(ref_id, str) else None
            demand = row.get("unclamped_required_full_image")
            axes = object_value(specimen.get("encoded_axes"))
            valid_demand = isinstance(demand, list) and len(demand) == 2 and all(type(x) is int and x > 0 for x in demand)
            if not valid_demand or not all(type(axes.get(key)) is int for key in ("width", "height")):
                report.bad(f"pilot role geometry demand/source axes invalid: {binding_id}")
                continue
            sufficient = axes["width"] >= demand[0] and axes["height"] >= demand[1]
            if row.get("source_pixels_sufficient") is not sufficient or (status == "verified" and not sufficient):
                report.bad(f"pilot role geometry source adequacy mismatch: {binding_id}")
            if status == "insufficient_pixels" and sufficient:
                report.bad(f"pilot role geometry shortage contradicts source axes: {binding_id}")
            if not isinstance(row.get("evidence_id"), str) or not row["evidence_id"].strip():
                report.open(f"pilot role geometry evidence pointer missing: {binding_id}")
            if reference is None or ref_id not in selected_refs or row.get("role") != binding.get("role"):
                report.bad(f"pilot role geometry binding/reference/role mismatch: {binding_id}")
            elif row.get("reference_pixel_sha256") != reference.get("pixel_sha256") or row.get("reference_geometry") != {"width": reference.get("width"), "height": reference.get("height")}:
                report.bad(f"pilot role geometry normalized reference mismatch: {binding_id}")
    for binding_id in sorted(selected_binding_ids - geometry_ids):
        report.open(f"pilot role geometry evidence missing: {binding_id}")
        if pilot.get("status") == "READY":
            report.bad(f"READY pilot role geometry evidence missing: {binding_id}")
    baseline = data.get("demand_baseline")
    if baseline is None:
        report.open("pilot demand baseline unavailable")
        baseline = {}
    else:
        baseline = mapping(baseline, "demand_baseline", report)
    assessment = baseline.get("current_source_assessment")
    if assessment is None:
        report.open("pilot role demand assessment unavailable")
        assessment = {}
    else:
        assessment = mapping(assessment, "demand_baseline.current_source_assessment", report)
    unavailable_rows = assessment.get("unavailable_roles", [])
    unavailable_rows = sequence(unavailable_rows, "demand_baseline.unavailable_roles", report)
    unavailable = {binding_id for row in unavailable_rows if isinstance(row, dict) and isinstance(row.get("binding_ids"), list)
                   for binding_id in row["binding_ids"] if isinstance(binding_id, str)}
    for binding_id in sorted(selected_binding_ids.intersection(unavailable)):
        if reader_claim: report.open(f"pilot role geometry unavailable: {binding_id}")
        if any(isinstance(row, dict) and row.get("binding_id") == binding_id and row.get("status") == "verified" for row in geometry_rows):
            report.bad(f"pilot geometry claims verification for unavailable role demand: {binding_id}")
        if reader_claim and pilot.get("status") == "READY":
            report.bad(f"READY pilot contains unavailable role geometry: {binding_id}")
    plan = data.get("trial_plan")
    if not isinstance(plan, dict):
        report.open("predeclared trial plan is missing")
    else:
        invariants = {"seed": 20261010, "observations_per_arm_per_cache_mode": 5,
            "retry_limit": 1, "control_observation_reuse": False,
            "cache_modes": ["network_cold", "disk_warm_decoded_cold", "memory_warm"],
            "pair_states": ["not_run", "failed", "incomparable", "rejected", "pending", "eligible"],
            "stop_rules": ["prerequisite_failure", "frozen_ledger_exhausted"],
            "maximum_planned_pairs": 4096, "sizing_and_same_geometry_codec_arms_separate": True}
        for key, value in invariants.items():
            if not same(plan.get(key), value): report.bad(f"predeclared trial rule mismatch: {key}")
        eligibility = {"compatible_pair_required": True, "verified_actual_values_required": 2,
            "positive_baseline_required": True, "missing_output_is_zero": False,
            "raw_and_quality_passed_subsets_separate": True, "quality_passed_verdict_required": "pass"}
        if plan.get("byte_eligibility") != eligibility: report.bad("predeclared byte eligibility rules mismatch")
        pairs = sequence(plan.get("planned_pairs"), "trial_plan.planned_pairs", report)
        environments = sequence(plan.get("native_environments"), "trial_plan.native_environments", report)
        n, e = len(pairs), len(environments)
        if n > 4096: report.bad("predeclared finite pair bound exceeded")
        counts = {"pairs": n, "environments": e, "base_native_observations": 30*n*e, "maximum_native_attempts": 60*n*e}
        if not same(plan.get("counts"), counts): report.bad("predeclared scheduled observation counts mismatch")
        if plan.get("status") != "PREREGISTERED" or n == 0: report.open("comparison pair ledger is not preregistered")
        pair_ids = set()
        for pair in pairs:
            if not isinstance(pair, dict): report.bad("comparison pair must be an object"); continue
            ident = pair.get("id")
            if not isinstance(ident, str) or not ident: report.bad("comparison pair id must be nonempty string"); continue
            if ident in pair_ids: report.bad("duplicate or missing comparison pair id")
            pair_ids.add(ident)
            binding = next((x for x in bindings if isinstance(x, dict) and x.get("id") == pair.get("binding_id")), None)
            ref_id = pair.get("reference_id")
            reference = prepared.get(ref_id) if isinstance(ref_id, str) else None
            if binding is None or reference is None: report.bad(f"comparison pair has unknown binding/reference: {ident}"); continue
            specimen = id_lookup(specimen_map, binding.get("specimen_id"), {})
            if not isinstance(specimen, dict):
                report.bad(f"comparison pair has unknown specimen: {ident}")
                continue
            validate_pilot_binding(binding, specimen, reference, f"comparison pair {ident}", report)
            if binding.get("id") not in selected_binding_ids or ref_id not in selected_refs:
                report.bad(f"comparison pair is outside selected pilot bindings/references: {ident}")
            if pair.get("role") != binding.get("role"):
                report.bad(f"comparison pair role differs from source binding: {ident}")
            if pair.get("source_sha256") != object_value(specimen.get("integrity")).get("sha256") or pair.get("reference_pixel_sha256") != reference.get("pixel_sha256"):
                report.bad(f"comparison pair source/reference hash mismatch: {ident}")
            if reference.get("source_hash") != pair.get("source_sha256"):
                report.bad(f"comparison pair reference belongs to another source: {ident}")
            if plan.get("status") == "PREREGISTERED" and (reference.get("status") != "admitted" or reference.get("id") not in admitted_ids):
                report.open(f"comparison pair reference admission unresolved: {ident}")
            if pair.get("geometry") != {"width": reference.get("width"), "height": reference.get("height")}:
                report.bad(f"comparison pair geometry differs from normalized reference: {ident}")
            if pair.get("state") not in invariants["pair_states"] or pair.get("arm_type") not in ("sizing", "same_geometry_codec"):
                report.bad(f"comparison pair state/arm invalid: {ident}")
            for key in ("role", "control_settings", "candidate_settings", "baseline_denominator", "freeze_revision_id"):
                if not pair.get(key): report.bad(f"comparison pair missing {key}: {ident}")
            if pair.get("freeze_revision_id") != freeze.get("revision_id"):
                report.bad(f"comparison pair freeze revision mismatch: {ident}")
    rights = mapping(policy.get("rights_provenance"), "rights_provenance", report)
    validate_r3_ledger(data, report)
    if not rights.get("recorded"): report.open("user rights provenance missing")
    elif not all(rights.get(k) for k in ("declaration", "declarant", "date")):
        report.bad("recorded rights declaration lacks required provenance")
    validate_source(data, source_root, report)
    private_patterns = [
        re.compile(rb"https?://[^\s\"'<>]+[?&](?:token|previewToken|sig|signature|X-Goog-[^=]+)=", re.I),
        re.compile(rb"(?:Authorization|access_token|private_key)[\"']?\s*[:=]\s*[\"'][A-Za-z0-9./_+=:-]{8,}", re.I),
        re.compile(rb"gs://[a-z0-9][a-z0-9._-]{2,}/[^\s\"')]+", re.I),
        re.compile(rb"AIza[0-9A-Za-z_-]{30,}|-----BEGIN [A-Z ]*PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}"),
        re.compile(rb"/private/tmp/[A-Za-z0-9][A-Za-z0-9_./-]+"),
    ]
    for path in HERE.rglob("*"):
        if "__pycache__" in path.parts: continue
        if path.is_symlink():
            report.bad(f"deliverable symlink is outside privacy scan contract: {path.relative_to(HERE)}")
            continue
        if not path.is_file(): continue
        try:
            if path.stat().st_size > 2 * 1024 * 1024:
                report.bad(f"deliverable exceeds metadata-only privacy scan bound: {path.relative_to(HERE)}")
                continue
            payload = path.read_bytes()
        except OSError as exc:
            report.bad(f"privacy scan could not read {path.relative_to(HERE)}: {exc}")
            continue
        if any(pattern.search(payload) for pattern in private_patterns): report.bad(f"private access material in {path.relative_to(HERE)}")


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--source-root", type=Path); parser.add_argument("--reference-root", type=Path); parser.add_argument("--preparation-receipt", type=Path); parser.add_argument("--freeze-manifest", type=Path); parser.add_argument("--corpus", type=Path, default=CORPUS); parser.add_argument("--protocol", type=Path, default=PROTOCOL); args = parser.parse_args()
    report = Report()
    try: corpus_raw = args.corpus.read_bytes(); data = json.loads(corpus_raw, parse_constant=lambda value: (_ for _ in ()).throw(ValueError("non-finite JSON number")))
    except Exception as exc: report.bad(f"cannot read corpus: {exc}"); return report.emit()
    try: validate(data, args.source_root.resolve() if args.source_root else None, args.protocol.resolve(), args.reference_root.resolve() if args.reference_root else None, args.preparation_receipt, report, args.freeze_manifest, sha(corpus_raw))
    except Exception as exc: report.bad(f"verifier internal validation failure: {type(exc).__name__}: {exc}")
    return report.emit()


if __name__ == "__main__": raise SystemExit(main())
