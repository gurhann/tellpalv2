#!/usr/bin/env python3
"""Prepare private full-size references and test the same normalization path.

This experimental helper performs no candidate encoding or native measurement.
Each invocation creates a new private directory; older preparations are retained.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import re
import struct
import sys
from datetime import datetime, timezone
from io import BytesIO
from pathlib import Path

import PIL
from PIL import Image, ImageCms, ImageOps, features

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
MAGIC = b"tellpal-p0a-rgba8-srgb-straight-v1\0"
MAX_BYTES = 20 * 1024 * 1024
MAX_PIXELS = 40_000_000
PROFILE_LIMIT = 1024 * 1024
DESTINATION_SHA = "2b3aa1645779a9e634744faf9b01e9102b0c9b88fd6deced7934df86b949af7e"
P3_SHA = "0ff6958f98684c61f6bbdce1368ddeaf3873baf84545baba482e920d92a914c0"
BPC_EXPECTED_SHA = "745703e6cec94749c0e83d7c5501dd01c167d8c39673b22238e296ac26c88c26"
EXPECTED_RECEIPT_SHA = "60b8b2da5c116ef0c3fd7c2d7a3cfdfd18277e7bf1af0478fa8de707b0fa5179"
EXIF_EXPECTED = {
    1: ("ABC", "DEF"), 2: ("CBA", "FED"), 3: ("FED", "CBA"),
    4: ("DEF", "ABC"), 5: ("AD", "BE", "CF"), 6: ("DA", "EB", "FC"),
    7: ("FC", "EB", "DA"), 8: ("CF", "BE", "AD"),
}
P3_EXPECTED = [
    ([128, 64, 32], [138, 59, 21]), ([64, 128, 96], [32, 130, 93]),
    ([32, 64, 128], [18, 65, 133]), ([0, 0, 0], [0, 0, 0]),
    ([255, 255, 255], [255, 255, 255]), ([128, 128, 128], [128, 128, 128]),
]


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def bounded_read(path: Path, limit: int) -> bytes:
    if not path.is_file() or path.is_symlink() or path.stat().st_size > limit:
        raise ValueError("absent, symlinked or oversized private input")
    with path.open("rb") as stream:
        data = stream.read(limit + 1)
    if len(data) > limit:
        raise ValueError("private input exceeded its byte limit")
    return data


def pinned_profile(path: Path, expected_hash: str) -> bytes:
    data = bounded_read(path, PROFILE_LIMIT)
    if sha(data) != expected_hash:
        raise ValueError("profile identity does not match the frozen oracle")
    ImageCms.ImageCmsProfile(BytesIO(data))
    return data


def expand_palette(image: Image.Image) -> Image.Image:
    """Expand a static 8-bit palette into explicit straight RGBA8 pixels."""
    if image.mode != "P":
        return image
    palette = image.getpalette()
    if palette is None or len(palette) < 3:
        raise ValueError("palette lacks RGB entries")
    transparency = image.info.get("transparency")
    if isinstance(transparency, int):
        alpha = [255] * (len(palette) // 3)
        if 0 <= transparency < len(alpha):
            alpha[transparency] = 0
    elif isinstance(transparency, (bytes, bytearray, list, tuple)):
        alpha = list(transparency) + [255] * (len(palette) // 3 - len(transparency))
        alpha = [int(value) for value in alpha[: len(palette) // 3]]
    else:
        alpha = [255] * (len(palette) // 3)
    rgba = []
    for index in image.getdata():
        offset = int(index) * 3
        if offset + 2 >= len(palette):
            raise ValueError("palette index has no RGB entry")
        rgba.append((palette[offset], palette[offset + 1], palette[offset + 2], alpha[int(index)]))
    expanded = Image.new("RGBA", image.size)
    expanded.putdata(rgba)
    expanded.info.update(image.info)
    expanded.format = image.format
    return expanded


def normalize_image(image: Image.Image, destination: bytes, *, bpc: bool = True):
    """Preserve straight alpha while transforming every RGB pixel, including hidden RGB."""
    if image.width * image.height > MAX_PIXELS:
        raise ValueError("source pixel cap exceeded before load")
    if getattr(image, "n_frames", 1) != 1:
        raise ValueError("animation is outside this static preparation")
    if image.mode == "P":
        image = expand_palette(image)
    if image.mode not in {"RGB", "RGBA", "L"}:
        raise ValueError("unsupported mode; no implicit depth or alpha conversion")
    if image.format == "JPEG" and getattr(image, "bits", 8) != 8:
        raise ValueError("non-8-bit JPEG is unsupported")
    orientation = image.getexif().get(274)
    if orientation is not None and orientation not in range(1, 9):
        raise ValueError("unsupported EXIF orientation")
    source_icc = image.info.get("icc_profile")
    metadata = {
        "source_mime": Image.MIME.get(image.format),
        "icc_present": bool(source_icc),
        "icc_sha256": sha(source_icc) if source_icc else None,
        "assumed_srgb": not bool(source_icc),
        "exif_present": bool(image.info.get("exif")),
        "exif_orientation": orientation,
    }
    image.load()
    oriented = ImageOps.exif_transpose(image)
    alpha = oriented.convert("RGBA").getchannel("A") if (
        "A" in oriented.getbands() or "transparency" in oriented.info
    ) else None
    if source_icc:
        source_profile = ImageCms.ImageCmsProfile(BytesIO(source_icc))
        if source_profile.profile.xcolor_space.strip() != "RGB":
            raise ValueError("tagged non-RGB profile remains unqualified")
        rgb = ImageCms.profileToProfile(
            oriented.convert("RGB"), source_profile,
            ImageCms.ImageCmsProfile(BytesIO(destination)), outputMode="RGB",
            renderingIntent=1,
            flags=int(ImageCms.Flags.BLACKPOINTCOMPENSATION) if bpc else 0,
        )
        result = rgb.convert("RGBA")
        if alpha is not None:
            result.putalpha(alpha)
    else:
        result = oriented.convert("RGBA")
    result.info.clear()
    return result, metadata


def normalize(data: bytes, destination: bytes):
    if len(data) > MAX_BYTES:
        raise ValueError("source byte cap exceeded")
    # Palette-indexed PNGs may use packed 1/2/4/8-bit indices; true-color and
    # grayscale PNGs remain qualified only at the existing 8-bit depth.
    if data.startswith(b"\x89PNG\r\n\x1a\n") and len(data) >= 26:
        depth, color_type = data[24], data[25]
        if (color_type == 3 and depth not in {1, 2, 4, 8}) or (color_type != 3 and depth != 8):
            raise ValueError("unsupported PNG bit depth")
    with Image.open(BytesIO(data)) as image:
        return normalize_image(image, destination)


def canonical(image: Image.Image) -> bytes:
    if image.mode != "RGBA":
        raise ValueError("canonical serialization requires straight RGBA8")
    return MAGIC + struct.pack(">II", image.width, image.height) + image.tobytes()


def oracle_exif(destination: bytes) -> dict:
    colors = [(255, 0, i, a) for i, a in enumerate([0, 1, 128, 255, 64, 255])]
    lookup = {color: chr(65 + i) for i, color in enumerate(colors)}
    source = Image.new("RGBA", (3, 2))
    source.putdata(colors)
    actual = []
    for orientation, expected in EXIF_EXPECTED.items():
        fixture = source.copy()
        fixture.getexif()[274] = orientation
        fixture.info["exif"] = fixture.getexif().tobytes()
        out, _ = normalize_image(fixture, destination)
        rows = ["".join(lookup.get(out.getpixel((x, y)), "?")
                        for x in range(out.width)) for y in range(out.height)]
        second, _ = normalize_image(out, destination)
        actual.append({
            "orientation": orientation, "expected_rows": list(expected),
            "actual_rows": rows, "expected_axes": [len(expected[0]), len(expected)],
            "actual_axes": list(out.size), "orientation_removed": out.getexif().get(274) is None,
            "idempotent": canonical(second) == canonical(out),
        })
    passed = all(x["actual_rows"] == x["expected_rows"] and
                 x["actual_axes"] == x["expected_axes"] and
                 x["orientation_removed"] and x["idempotent"] for x in actual)
    return {"id": "exif-orientation-oracle-1-8", "result": "pass" if passed else "failed",
            "criterion": "literal rows and geometry exact; alpha/hidden RGB exact; metadata removal idempotent",
            "actual": actual}


def oracle_alpha(destination: bytes, p3: bytes) -> dict:
    pixels = [(17, 101, 233, 0), (80, 120, 160, 128), (0, 255, 13, 1), (33, 44, 55, 255)]
    fixture = Image.new("RGBA", (2, 2))
    fixture.putdata(pixels)
    out, _ = normalize_image(fixture, destination)
    untagged = list(out.getdata())
    fixture.putdata([(128, 64, 32, a) for a in [0, 1, 128, 255]])
    fixture.info["icc_profile"] = p3
    out, _ = normalize_image(fixture, destination)
    tagged = list(out.getdata())
    expected_tagged = [(138, 59, 21, a) for a in [0, 1, 128, 255]]
    passed = untagged == pixels and all(a[3] == e[3] and
             max(abs(a[i] - e[i]) for i in range(3)) <= 1
             for a, e in zip(tagged, expected_tagged))
    return {"id": "straight-alpha-hidden-rgb", "result": "pass" if passed else "failed",
            "criterion": "untagged RGBA exact; tagged RGB within 1 per channel independently of alpha; alpha exact",
            "expected_untagged": pixels, "actual_untagged": untagged,
            "expected_tagged": expected_tagged, "actual_tagged": tagged}


def oracle_palette(destination: bytes) -> dict:
    """Freeze packed palette expansion, including hidden RGB and opaque entries."""
    palette_rgb = [(17, 101, 233), (80, 120, 160), (0, 255, 13), (33, 44, 55)]
    expected_four = [(*rgb, alpha) for rgb, alpha in zip(palette_rgb, [0, 128, 255, 255])]
    cases = []
    for bits, colors, indices, expected, axes in (
        (1, palette_rgb[:2], [0, 1], expected_four[:2], [2, 1]),
        (2, palette_rgb, [0, 1, 2, 3], expected_four, [2, 2]),
        (4, palette_rgb, [0, 1, 2, 3], expected_four, [2, 2]),
        (8, palette_rgb, [0, 1, 2, 3], expected_four, [2, 2]),
    ):
        fixture = Image.new("P", tuple(axes))
        fixture.putpalette([channel for rgb in colors for channel in rgb])
        fixture.putdata(indices)
        fixture.info["transparency"] = bytes([0, 128, 255, 255])
        payload = BytesIO()
        fixture.save(payload, format="PNG", bits=bits)
        data = payload.getvalue()
        encoded_depth, encoded_type = data[24], data[25]
        out, _ = normalize(data, destination)
        actual = list(out.getdata())
        cases.append({"bits": bits, "encoded_depth": encoded_depth, "encoded_color_type": encoded_type,
                      "expected_rgba": expected, "actual_rgba": actual,
                      "expected_axes": axes, "actual_axes": list(out.size),
                      "passed": encoded_depth == bits and encoded_type == 3 and actual == expected and list(out.size) == axes})
    opaque = Image.new("P", (1, 1))
    opaque.putpalette([201, 202, 203])
    opaque.putdata([0])
    opaque_payload = BytesIO()
    opaque.save(opaque_payload, format="PNG", bits=4)
    opaque_data = opaque_payload.getvalue()
    opaque_out, _ = normalize(opaque_data, destination)
    opaque_expected = [(201, 202, 203, 255)]
    opaque_actual = list(opaque_out.getdata())
    cases.append({"bits": 4, "encoded_depth": opaque_data[24], "encoded_color_type": opaque_data[25],
                  "transparency_info_present": b"tRNS" in opaque_data,
                  "expected_rgba": opaque_expected, "actual_rgba": opaque_actual,
                  "expected_axes": [1, 1], "actual_axes": list(opaque_out.size),
                  "passed": opaque_data[24] == 4 and opaque_data[25] == 3 and
                  b"tRNS" not in opaque_data and opaque_actual == opaque_expected and list(opaque_out.size) == [1, 1]})
    passed = all(case["passed"] for case in cases)
    return {"id": "palette-rgba-expansion", "result": "pass" if passed else "failed",
            "criterion": "Packed 1/2/4/8-bit static palette PNGs expand to exact straight RGBA8; hidden RGB and alpha are preserved; indexed entries remain opaque where declared",
            "cases": cases}


def color_cases(destination: bytes, source_profile: bytes, expectations: list, *, bpc=True):
    rows = []
    for rgb, expected in expectations:
        fixture = Image.new("RGB", (1, 1), tuple(rgb))
        fixture.info["icc_profile"] = source_profile
        out, _ = normalize_image(fixture, destination, bpc=bpc)
        actual = list(out.getpixel((0, 0)))
        rows.append({"input_rgb": rgb, "expected_rgb": expected, "actual_rgba": actual,
                     "within_tolerance": actual[3] == 255 and
                     max(abs(actual[i] - expected[i]) for i in range(3)) <= 1})
    return rows


def run_checks(destination, p3, bpc_profile, expected, receipt) -> list:
    p3_rows = color_cases(destination, p3, P3_EXPECTED)
    enabled = color_cases(destination, bpc_profile,
                          [(x["input_rgb"], x["expected_bpc_rgb"]) for x in expected["expected"]])
    disabled = color_cases(destination, bpc_profile,
                           [(x["input_rgb"], x["expected_no_bpc_rgb"]) for x in expected["expected"]], bpc=False)
    sensitive = enabled[0]["actual_rgba"][:3] != disabled[0]["actual_rgba"][:3]
    return [oracle_exif(destination), oracle_alpha(destination, p3), oracle_palette(destination),
            {"id": "icc-display-p3-oracle", "result": "pass" if all(x["within_tolerance"] for x in p3_rows) else "failed",
             "criterion": "independent matrix/TRC oracle; RGB error <=1 per channel, alpha exact",
             "source_profile_sha256": sha(p3), "target_profile_sha256": sha(destination), "actual": p3_rows},
            {"id": "icc-nonzero-black-bpc-oracle", "result": "pass" if sensitive and
             all(x["within_tolerance"] for x in enabled + disabled) else "failed",
             "criterion": "frozen independent affine PCS oracle; RGB error <=1 per channel, alpha exact; enabled/disabled sentinel differs",
             "expected_sha256": BPC_EXPECTED_SHA, "expected_receipt_sha256": EXPECTED_RECEIPT_SHA,
             "expected_captured_at_utc": receipt["captured_at_utc"],
             "source_profile_sha256": sha(bpc_profile), "target_profile_sha256": sha(destination),
             "enabled": enabled, "disabled_diagnostic": disabled, "sensitivity_verified": sensitive}]


def write_new(path: Path, data: bytes):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "wb") as stream:
        stream.write(data)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--corpus", type=Path, default=HERE / "p0a-corpus.json")
    parser.add_argument("--destination-profile", type=Path, required=True)
    parser.add_argument("--p3-profile", type=Path, required=True)
    parser.add_argument("--bpc-profile", type=Path, required=True)
    parser.add_argument("--bpc-expected", type=Path, required=True)
    parser.add_argument("--expected-receipt", type=Path, required=True)
    args = parser.parse_args()
    root, output = args.source_root.resolve(), args.output_dir.resolve()
    if root.is_relative_to(REPO) or output.is_relative_to(REPO) or not root.is_dir():
        parser.error("sources and output must be private, outside the repository")
    destination = pinned_profile(args.destination_profile, DESTINATION_SHA)
    p3 = pinned_profile(args.p3_profile, P3_SHA)
    expected_raw = bounded_read(args.bpc_expected, PROFILE_LIMIT)
    receipt_raw = bounded_read(args.expected_receipt, PROFILE_LIMIT)
    if sha(expected_raw) != BPC_EXPECTED_SHA or sha(receipt_raw) != EXPECTED_RECEIPT_SHA:
        parser.error("independent expected inputs have changed")
    expected, receipt = json.loads(expected_raw), json.loads(receipt_raw)
    bpc_profile = pinned_profile(args.bpc_profile, expected["synthetic_profile_sha256"])
    if expected["target_profile_sha256"] != sha(destination):
        parser.error("oracle destination mismatch")
    corpus_raw = bounded_read(args.corpus, PROFILE_LIMIT)
    corpus = json.loads(corpus_raw)
    hashes = {x.get("integrity", {}).get("sha256") for x in corpus["specimens"]}
    hashes.update(x.get("sha256") for x in corpus["historical_controls"])
    hashes.discard(None)
    if len(hashes) > 60 or any(not re.fullmatch(r"[0-9a-f]{64}", str(x)) for x in hashes):
        parser.error("invalid or excessive source set")
    started = datetime.now(timezone.utc).isoformat()
    if datetime.fromisoformat(started) <= datetime.fromisoformat(receipt["captured_at_utc"]):
        parser.error("oracle expectations must precede this check")
    checks = run_checks(destination, p3, bpc_profile, expected, receipt)
    output.mkdir(mode=0o700, parents=True, exist_ok=False)
    refs_dir = output / "normalized-references"
    refs_dir.mkdir(mode=0o700)
    rows, failures = [], []
    for source_hash in sorted(hashes):
        source = root / (source_hash + ".source")
        try:
            if not source.resolve().is_relative_to(root):
                raise ValueError("source path escapes private root")
            data = bounded_read(source, MAX_BYTES)
            if sha(data) != source_hash:
                raise ValueError("source rehash mismatch")
            image, metadata = normalize(data, destination)
            encoded = canonical(image)
            if sha(bounded_read(source, MAX_BYTES)) != source_hash:
                raise ValueError("source changed during inspection")
            ident = source_hash + "-rgba8-srgb-v1"
            write_new(refs_dir / (ident + ".pixels"), encoded)
            bindings = [x["id"] for x in corpus["bindings"] if
                        any(s["id"] == x["specimen_id"] and
                            s.get("integrity", {}).get("sha256") == source_hash
                            for s in corpus["specimens"])]
            rows.append({"id": ident, "source_hash": source_hash, "binding_ids": bindings,
                         "width": image.width, "height": image.height,
                         "pixel_sha256": sha(encoded), "bytes": len(encoded),
                         "status": "unadmitted_preflight", "metadata": metadata})
        except (ValueError, OSError, ImageCms.PyCMSError, Image.DecompressionBombError) as exc:
            failures.append({"source_hash": source_hash, "result": "failed",
                             "reason": type(exc).__name__ + ": " + str(exc)})
    payload = {
        "schema": "p0a-normalization-preparation-v3", "started_at_utc": started,
        "completed_at_utc": datetime.now(timezone.utc).isoformat(),
        "input_corpus_sha256": sha(corpus_raw),
        "runtime": {"python": sys.version.split()[0], "platform": platform.platform(),
                    "pillow": PIL.__version__, "littlecms": features.version("littlecms2"),
                    "jpeg": features.version("jpg"), "zlib": features.version("zlib")},
        "implementation": {"script": Path(__file__).name,
                           "script_sha256": sha(Path(__file__).read_bytes()),
                           "destination_profile_sha256": sha(destination),
                           "canonical_magic_sha256": sha(MAGIC),
                           "settings": {"intent": 1, "bpc_flags": 8192, "resize": "none; full source geometry preflight",
                                        "alpha": "straight; tagged hidden RGB converted independently of alpha"}},
        "expected_receipt_sha256": sha(receipt_raw), "checks": checks,
        "references": rows, "source_failures": failures,
        "reference_admission": "unadmitted; rights, verified CMS bindings and required real pilot coverage remain separate gates",
    }
    receipt_bytes = (json.dumps(payload, indent=2, ensure_ascii=False) + "\n").encode()
    write_new(output / "preparation-receipt.json", receipt_bytes)
    success = not failures and all(x["result"] == "pass" for x in checks)
    print(json.dumps({"receipt_sha256": sha(receipt_bytes), "references": len(rows),
                      "checks": [(x["id"], x["result"]) for x in checks],
                      "source_failure_count": len(failures)}))
    return 0 if success else 1


if __name__ == "__main__":
    raise SystemExit(main())
