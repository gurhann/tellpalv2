#!/usr/bin/env python3
"""Compare private Java pre-encode RGB observations with explicit LCMS references."""

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import sys

from PIL import Image, ImageChops, ImageCms, features


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def differences(first, second):
    delta = ImageChops.difference(first, second)
    histogram = delta.histogram()
    channels = [sum(histogram[channel * 256 + value] for channel in range(3)) for value in range(256)]
    count = sum(channels)
    cumulative = 0
    percentile = 0
    for value, frequency in enumerate(channels):
        cumulative += frequency
        if cumulative >= count * 0.95:
            percentile = value
            break
    red, green, blue = delta.split()
    maximum_channel = ImageChops.lighter(ImageChops.lighter(red, green), blue)
    return {
        "meanAbsoluteChannelError0to255": sum(value * frequency for value, frequency in enumerate(channels)) / count,
        "p95AbsoluteChannelError0to255": percentile,
        "maximumAbsoluteChannelError0to255": max(value for value, frequency in enumerate(channels) if frequency),
        "exactPixelFraction": maximum_channel.histogram()[0] / (first.width * first.height),
    }


def run(arguments):
    return subprocess.run(arguments, check=True, capture_output=True, text=True).stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    parser.add_argument("--corpus", type=Path, default=Path(__file__).resolve().parent.parent / "corpus.json")
    args = parser.parse_args()
    os.umask(0o077)
    source_root = args.source_root.resolve(strict=True)
    if os.path.lexists(args.output_root):
        parser.error("Output root must be fresh; refusing to overwrite an existing path")
    output_root = args.output_root.resolve()
    helper_root = Path(__file__).resolve().parent
    repository = next((parent for parent in helper_root.parents if (parent / ".git").exists()), None)
    if repository and output_root.is_relative_to(repository):
        parser.error("Keep diagnostic RGB and execution outputs outside the repository")
    if output_root.is_relative_to(source_root):
        parser.error("Keep diagnostic outputs outside --source-root")
    corpus_bytes = args.corpus.read_bytes()
    samples = json.loads(corpus_bytes)["samples"]
    before_hashes = {}
    for sample in samples:
        relative = Path(sample["path"])
        source = (source_root / relative).resolve(strict=True)
        if relative.is_absolute() or not source.is_relative_to(source_root):
            parser.error("Source escapes --source-root")
        before_hashes[sample["path"]] = sha256(source.read_bytes())
        if before_hashes[sample["path"]] != sample["sha256"]:
            parser.error("Source checksum differs from the frozen corpus")
    output_root.mkdir(parents=True, exist_ok=False, mode=0o700)
    pixel_root = output_root / "pixels"
    java_source = helper_root / "ColorProbe.java"
    run(["javac", "-d", str(output_root), str(java_source)])
    observations = run(["java", "-Djava.awt.headless=true", "-cp", str(output_root), "ColorProbe",
                        "--source-root", str(source_root), "--output-root", str(pixel_root),
                        *[sample["path"] for sample in samples]])
    runtime = None
    decoded = {}
    for line in observations.splitlines():
        fields = line.split("|")
        if fields[0] == "runtime":
            runtime = dict(zip(["version", "vendor", "vmName"], fields[1:]))
        elif fields[0] == "decoded":
            decoded[fields[1]] = {
                "width": int(fields[2]), "height": int(fields[3]), "bufferedImageType": int(fields[4]),
                "colorSpaceClass": fields[5], "isCsSrgb": fields[6] == "true", "decodedIccSha256": fields[7],
                "readerClass": fields[8], "providerClass": fields[9], "providerVendor": fields[10],
                "providerVersion": fields[11], "decodedRgbSha256": fields[12], "drawnRgbSha256": fields[13],
                "readerSeekForwardOnly": True, "readerIgnoreMetadata": True,
            }
    srgb = ImageCms.createProfile("sRGB")
    cases = []
    for sample in samples:
        relative = sample["path"]
        source = source_root / relative
        with Image.open(source) as image:
            image.load()
            icc = image.info.get("icc_profile")
            unmanaged = image.convert("RGB")
            prefix = relative.replace("/", "-").replace("\\", "-")
            java_decoded = Image.frombytes("RGB", image.size, (pixel_root / (prefix + ".decoded.rgb")).read_bytes())
            java_drawn = Image.frombytes("RGB", image.size, (pixel_root / (prefix + ".drawn.rgb")).read_bytes())
            case = {
                "id": sample["id"], "relativeSource": relative, "sourceSha256": sample["sha256"],
                "sourceBytes": source.stat().st_size, "sourceMime": Image.MIME[image.format],
                "width": image.width, "height": image.height, "orientationTag": image.getexif().get(274),
                "embeddedIccBytes": len(icc) if icc else 0, "embeddedIccSha256": sha256(icc) if icc else None,
                "javaDecodedObservation": decoded[relative],
                "javaDrawVsJavaDecoded": differences(java_drawn, java_decoded),
                "javaDrawVsPillowUnmanagedRgb": differences(java_drawn, unmanaged),
            }
            if icc:
                embedded = ImageCms.ImageCmsProfile(io.BytesIO(icc))
                case["embeddedIccDescription"] = ImageCms.getProfileDescription(embedded).strip()
                case["references"] = {}
                for name, intent in [("relativeColorimetric", ImageCms.Intent.RELATIVE_COLORIMETRIC),
                                     ("perceptual", ImageCms.Intent.PERCEPTUAL)]:
                    reference = ImageCms.profileToProfile(image, embedded, srgb, renderingIntent=intent, outputMode="RGB")
                    case["references"][name] = {
                        "pillowUnmanagedVsReference": differences(unmanaged, reference),
                        "javaDrawVsReference": differences(java_drawn, reference),
                        "referenceRgbSha256": sha256(reference.tobytes()),
                    }
            cases.append(case)
    after_hashes = {sample["path"]: sha256((source_root / sample["path"]).read_bytes()) for sample in samples}
    if after_hashes != before_hashes:
        raise RuntimeError("Original source hashes changed during the diagnostic")
    if args.corpus.read_bytes() != corpus_bytes:
        raise RuntimeError("Frozen corpus changed during the diagnostic")
    result = {
        "experiment": "p0-small-jpeg-color-diagnostic-2026-10-08",
        "scope": "Seven real legacy small cover JPEGs; no-resize pre-encode sample proof only.",
        "notEstablished": ["human appearance approval", "retained Display P3 gamut", "general ICC/PNG behavior",
                           "EXIF rotation", "alpha", "animation", "post-JPEG quality", "production image profile"],
        "versions": {"python": sys.version.split()[0], "pillow": Image.__version__,
                     "littlecms": features.version("littlecms2"), "java": runtime},
        "helpers": {"ColorProbe.java": sha256(java_source.read_bytes()),
                    "run_color_diagnostic.py": sha256(Path(__file__).read_bytes()),
                    "ColorProbe.class": sha256((output_root / "ColorProbe.class").read_bytes())},
        "corpusSha256": sha256(corpus_bytes),
        "sourceIntegrity": {"verifiedBefore": True, "verifiedAfter": True,
                            "hashes": [{"relativeSource": name, "beforeSha256": before_hashes[name],
                                        "afterSha256": after_hashes[name]} for name in before_hashes]},
        "referenceMethod": "Pillow JPEG decode; explicit embedded ICC to generated sRGB with LCMS relative-colorimetric and perceptual intents. Untagged sources remain unmanaged RGB controls. No EXIF transpose, resizing, alpha or lossy re-encoding.",
        "javaMethod": "Exact current converter reader.setInput(input,true,true)/read(0), then white TYPE_INT_RGB drawing with bicubic/quality hints at unchanged dimensions. RGB dumps use getRGB, so compare Java's sRGB pixel values.",
        "metricQualification": "8-bit channel absolute differences, not perceptual quality thresholds. JPEG implementations and color rounding may differ. ICC removal alone does not establish ignored profiles; sRGB normalization does not retain all P3 gamut.",
        "cases": cases,
    }
    destination = output_root / "color-comparison.json"
    with destination.open("x") as report_file:
        report_file.write(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"cases": len(cases), "reportSha256": sha256(destination.read_bytes())}))


if __name__ == "__main__":
    main()
