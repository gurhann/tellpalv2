#!/usr/bin/env python3
"""Run the frozen local JPEG comparison without starting application or cloud services."""
import argparse
import csv
import hashlib
import io
import json
import os
import platform
from pathlib import Path
import shutil
import statistics
import subprocess
import sys

import PIL
from PIL import Image, ImageCms


def sha(data):
    return hashlib.sha256(data).hexdigest()


def write_json(path, value):
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2, ensure_ascii=False)
        stream.write("\n")


def inspect(data):
    with Image.open(io.BytesIO(data)) as image:
        image.load()
        icc = image.info.get("icc_profile", b"")
        exif_bytes = image.info.get("exif", b"")
        exif = image.getexif()
        description = None
        if icc:
            description = ImageCms.getProfileDescription(ImageCms.ImageCmsProfile(io.BytesIO(icc))).strip()
        return {
            "width": image.width, "height": image.height,
            "metadata": {
                "format": image.format, "mime": Image.MIME.get(image.format), "mode": image.mode,
                "frames": getattr(image, "n_frames", 1), "has_alpha": "A" in image.getbands() or "transparency" in image.info,
                "icc": {"present": bool(icc), "bytes": len(icc), "sha256": sha(icc) if icc else None, "description": description},
                "exif": {"present": bool(exif_bytes), "bytes": len(exif_bytes), "sha256": sha(exif_bytes) if exif_bytes else None,
                         "orientation": exif.get(274), "tag_count": len(exif)},
            },
        }


def run(command, log):
    completed = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    with log.open("x", encoding="utf-8") as stream:
        stream.write(completed.stdout)
    if completed.returncode:
        raise RuntimeError(f"Command failed; inspect private {log.name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--results", type=Path, default=Path(__file__).with_name("small-jpeg-results.json"))
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    repo = here.parents[3]
    source_root = args.source_root.resolve(strict=True)
    output = args.output_root.resolve()
    results_file = args.results.resolve()
    if output.exists() or results_file.exists():
        raise ValueError("Refusing to overwrite an existing experiment or results file")
    if output.is_relative_to(source_root) or output.is_relative_to(repo):
        raise ValueError("Experiment outputs must be isolated from originals and repository")
    if results_file.is_relative_to(source_root):
        raise ValueError("Results must not write into the source repository")
    corpus_path = here / "corpus.json"
    corpus_bytes = corpus_path.read_bytes()
    corpus = json.loads(corpus_bytes)
    assert len(corpus["samples"]) == 7
    os.umask(0o077)
    sources = []
    for sample in corpus["samples"]:
        source_path = (source_root / sample["path"]).resolve(strict=True)
        assert source_path.is_relative_to(source_root) and source_path.is_file()
        assert sample["id"].replace("-", "").isalnum() and sample["id"].isascii()
        data = source_path.read_bytes()
        assert sha(data) == sample["sha256"] and 0 < len(data) <= 20 * 1024 * 1024
        info = inspect(data)
        assert info["metadata"]["mime"] == "image/jpeg" and info["metadata"]["frames"] == 1
        assert info["width"] * info["height"] <= 40_000_000
        assert max(info["width"], info["height"]) <= corpus["source_resolution"]["processor_edge"]
        sources.append({"id": sample["id"], "file": f"originals/{sample['id']}.jpg", "bytes": len(data), "sha256": sha(data),
                        "provenance": sample["provenance"], "focus": sample["focus"], **info})
    assert len({s["id"] for s in sources}) == 7
    output.mkdir(mode=0o700)
    for name in ("originals", "outputs", "classes"):
        (output / name).mkdir(mode=0o700)
    for sample, source in zip(corpus["samples"], sources):
        with (output / source["file"]).open("xb") as stream:
            stream.write((source_root / sample["path"]).read_bytes())
        assert sha((output / source["file"]).read_bytes()) == source["sha256"]
    candidate_rows = []
    for source in sources:
        for device in ("phone", "tablet"):
            width, height = corpus["listing"][device]
            for quality in corpus["listing"]["qualities"]:
                candidate_rows.append([source["id"], f"listing-{device}-q{quality}", f"listing_{device}", width, height, quality])
        for quality in corpus["source_resolution"]["qualities"]:
            edge = corpus["source_resolution"]["processor_edge"]
            candidate_rows.append([source["id"], f"source-resolution-edge{edge}-q{quality}", "source_resolution_proxy", edge, 0, quality])
    assert len(candidate_rows) == 63
    candidate_file = output / "candidates.tsv"
    candidate_file.write_text("".join("\t".join(map(str, row)) + "\n" for row in candidate_rows))
    media = repo / "be/src/main/java/com/tellpal/v2/asset/infrastructure/media"
    java_sources = [media / "ListingCoverRenditionProcessor.java", media / "PageImageRenditionProcessor.java", here / "SmallJpegExperiment.java"]
    jars = [Path.home() / ".m2/repository/org/springframework" / name / "7.0.5" / f"{name}-7.0.5.jar"
            for name in ("spring-beans", "spring-context")]
    assert all(path.is_file() for path in java_sources + jars)
    java, javac = shutil.which("java"), shutil.which("javac")
    assert java and javac
    provenance = {
        "corpus_sha256": sha(corpus_bytes),
        "source_files": [{"file": str(path.relative_to(repo)), "sha256": sha(path.read_bytes())} for path in java_sources],
        "python_harness_sha256": sha(Path(__file__).read_bytes()),
        "compile_dependencies": [{"artifact": jar.name, "sha256": sha(jar.read_bytes())} for jar in jars],
        "java_binary_sha256": sha(Path(java).resolve().read_bytes()),
        "javac_binary_sha256": sha(Path(javac).resolve().read_bytes()),
        "compilation": "Isolated current source compilation; no application/container/cloud service started",
    }
    run([javac, "--release", "25", "-cp", os.pathsep.join(map(str, jars)), "-d", str(output / "classes"), *map(str, java_sources)], output / "compile.log")
    provenance["compiled_classes"] = [{"file": str(path.relative_to(output / "classes")), "sha256": sha(path.read_bytes())}
                                      for path in sorted((output / "classes").rglob("*.class"))]
    timing = corpus["timing"]
    run([java, "-Djava.awt.headless=true", "-Xmx512m", "-cp", str(output / "classes"),
         "com.tellpal.v2.asset.infrastructure.media.SmallJpegExperiment", str(output), str(candidate_file),
         str(timing["warmups_per_candidate"]), str(timing["observations_per_candidate"]), str(timing["shuffle_seed"])], output / "converter.log")
    environment = {"python": platform.python_version(), "pillow": PIL.__version__, "java_heap_cap": "512m; configuration only, not measured memory"}
    for line in (output / "converter-runtime.properties").read_text().splitlines():
        if line and not line.startswith("#"):
            key, value = line.split("=", 1)
            environment[key] = value
    observations = list(csv.DictReader((output / "converter-observations.tsv").open(), delimiter="\t"))
    assert len(observations) == 315
    source_map = {source["id"]: source for source in sources}
    results = []
    for row in csv.DictReader((output / "converter-output-manifest.tsv").open(), delimiter="\t"):
        data = (output / row["file"]).read_bytes()
        info = inspect(data)
        assert sha(data) == row["sha256"] and len(data) == int(row["bytes"])
        assert info["metadata"]["mime"] == "image/jpeg" and info["metadata"]["frames"] == 1 and not info["metadata"]["has_alpha"]
        assert info["width"] == int(row["width"]) and info["height"] == int(row["height"])
        source = source_map[row["sample_id"]]
        assert info["width"] <= source["width"] and info["height"] <= source["height"]
        if row["role"].startswith("listing_"):
            width, height = corpus["listing"][row["role"].removeprefix("listing_")]
            assert info["width"] <= width and info["height"] <= height
        else:
            assert (info["width"], info["height"]) == (source["width"], source["height"])
        calls = [o for o in observations if o["sample_id"] == row["sample_id"] and o["candidate_id"] == row["candidate_id"]]
        assert len(calls) == 5 and {int(c["repetition"]) for c in calls} == set(range(5))
        ms = [int(call["transform_ns"]) / 1_000_000 for call in calls]
        saved = source["bytes"] - len(data)
        results.append({"sample_id": row["sample_id"], "candidate_id": row["candidate_id"], "role": row["role"], "quality": int(row["quality"]),
                        "bytes": len(data), "sha256": sha(data), "file": row["file"], "processor_profile": row["processor_profile"], **info,
                        "source_saved_bytes": saved, "source_saved_percent": 100 * saved / source["bytes"],
                        "transform_ms": ms, "transform_ms_median": statistics.median(ms), "transform_ms_min": min(ms), "transform_ms_max": max(ms),
                        "measured_sequence": [int(call["sequence"]) for call in calls], "deterministic_repeat_count": 7})
    assert len(results) == 63
    for result in results:
        control = next((r for r in results if r["sample_id"] == result["sample_id"] and r["role"] == result["role"]
                        and r["quality"] == corpus["listing"]["control_quality"]), None) if result["role"].startswith("listing_") else None
        result["control_candidate_id"] = control["candidate_id"] if control else None
        result["control_saved_bytes"] = control["bytes"] - result["bytes"] if control else None
        result["control_saved_percent"] = 100 * result["control_saved_bytes"] / control["bytes"] if control else None
    for sample in corpus["samples"]:
        assert sha((source_root / sample["path"]).read_bytes()) == sample["sha256"]
        assert sha((output / f"originals/{sample['id']}.jpg").read_bytes()) == sample["sha256"]
    assert sha(corpus_path.read_bytes()) == provenance["corpus_sha256"]
    aggregate = []
    for candidate in sorted({r["candidate_id"] for r in results}):
        group = [r for r in results if r["candidate_id"] == candidate]
        baseline = sum(source_map[r["sample_id"]]["bytes"] for r in group)
        total = sum(r["bytes"] for r in group)
        aggregate.append({"candidate_id": candidate, "role": group[0]["role"], "quality": group[0]["quality"], "samples": len(group),
                          "source_bytes": baseline, "output_bytes": total, "source_saved_bytes": baseline - total,
                          "source_saved_percent": 100 * (baseline - total) / baseline,
                          "source_size_regressions": sum(r["source_saved_bytes"] < 0 for r in group),
                          "accounting": "One local output per frozen source for this candidate; not observed network traffic or combined device-variant savings"})
    result_doc = {
        "experiment": corpus["experiment"], "scope": corpus["scope"], "environment": environment, "provenance": provenance,
        "sources": sources, "results": sorted(results, key=lambda r: (r["sample_id"], r["candidate_id"])),
        "aliases": {"processor_edge": corpus["source_resolution"]["processor_edge"],
                    "equivalent_no_upscale_detail_edges": corpus["source_resolution"]["detail_edge_aliases"],
                    "equivalent_no_upscale_page_edges": corpus["source_resolution"]["page_edge_aliases"],
                    "condition": "All frozen sources fit each edge; geometry only, not separately encoded aliases or page qualification"},
        "aggregate": aggregate,
        "validation": {"sources": 7, "outputs": 63, "warmups": 126, "measured_calls": 315, "deterministic_calls": 441,
                       "source_hashes_unchanged": True, "copied_original_hashes_match": True,
                       "independent_pillow_reread_decode_mime_dimensions_bytes_sha": True, "no_upscale": True,
                       "overwrites_refused": True, "measured_boundary": timing["boundary"], "shuffle_seed": timing["shuffle_seed"],
                       "timing_claim": "In-process warmed converter only; source I/O/file writes/hashes/independent validation excluded; OS cache uncontrolled",
                       "human_visual_approval": False, "native_device_approval": False, "production_profile_approval": False},
        "limitations": corpus["pending"] + ["No mobile transfer/paint/memory, worker memory, cold-run, Firebase storage or publication/readiness measurement",
                                             "Java converters use white matte and null metadata; no explicit EXIF/ICC normalization or animation preservation",
                                             "Profile aliases are geometry equivalence; no actual reader-page sources were processed",
                                             "Local file-size deltas are not measured network savings; retained storage can increase"],
    }
    write_json(output / "private-results.json", result_doc)
    write_json(output / "paths-manifest.json", {"output_root": str(output), "source_root": str(source_root),
                                              "source_paths": {sample["id"]: str(source_root / sample["path"]) for sample in corpus["samples"]},
                                              "public_results": str(results_file), "files": [str(path.relative_to(output)) for path in sorted(output.rglob("*")) if path.is_file()]})
    for path in output.rglob("*"):
        path.chmod(0o700 if path.is_dir() else 0o600)
    results_file.parent.mkdir(parents=True, exist_ok=True)
    write_json(results_file, result_doc)
    print(json.dumps({"outputs": len(results), "measured_calls": len(observations), "aggregate": aggregate}, indent=2))


if __name__ == "__main__":
    main()
