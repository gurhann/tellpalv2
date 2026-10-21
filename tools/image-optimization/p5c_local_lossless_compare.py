"""İki doğrulanmış TR pilotunu yalnız yerelde PNG/kayıpsız WebP ile karşılaştırır."""
import argparse
from hashlib import sha256
import json
import os
from pathlib import Path
import platform
import subprocess
import sys
import time

from PIL import features, __version__ as PILLOW_VERSION

from p5b_inventory import parse_csv, summarize, validate_inventory
from p5b_local_source_preflight import load_mapping
from p5b_source_io import MAX_FILE_BYTES, ROOT, PreflightError, read_bounded, require
from p5c_lossless_codec import (DIMENSIONS, SETTINGS, decode_source, digest, encode_candidate,
                               validate_candidate)
from p5c_lossless_output import (OUTPUT_BUDGET, create_private_directory, exclusive_bytes, gallery,
                                json_bytes, totals)

CSV_SHA256 = "9d7baf864996f46c283b90e35b7a9d582d748800865f3f2bd7a2148106c882c1"
SOURCE_TIMEOUT = 120
REPORT_RESERVE = 2 * 1024 * 1024


def check_record(data, expected):
    require(len(data) == expected["byte_size"], "SOURCE_BYTES_MISMATCH")
    require(digest(data) == expected["checksum_sha256"].lower(), "SOURCE_SHA256_MISMATCH")
    pixels, values, record = decode_source(data, expected["pilot_content_id"])
    require({"PNG": "image/png", "JPEG": "image/jpeg"}[record["format"]] == expected["mime_type"],
            "SOURCE_MIME_MISMATCH")
    return pixels, values, record


def worker(request):
    """Her kaynak için süre sınırı konan süreçte kontrol ve iki seri encode yapar."""
    started = time.perf_counter()
    data = read_bounded(request["source"])
    expected = request["expected"]
    pixels, values, source_record = check_record(data, expected)
    record = {"assetId": expected["source_asset_id"], "pilotContentId": expected["pilot_content_id"],
              "sourceBytes": len(data), "sourceSha256": digest(data), "source": source_record,
              "roles": request["roles"], "candidates": {}}
    if request["mode"] == "check":
        return record
    directory = Path(request["output"])
    remaining = request["budget"]
    record["sourceFile"] = f'source-{record["assetId"]}.' + ("jpg" if source_record["format"] == "JPEG" else "png")
    remaining -= exclusive_bytes(directory / record["sourceFile"], data, remaining)
    for format_name in ("PNG", "WEBP"):
        payload, elapsed = encode_candidate(pixels, values, format_name, remaining)
        filename = f'{record["assetId"]}.{format_name.lower()}'
        # Hatalı aday da silinmez; doğrulama sonucu başarısız çalışmanın özel kanıtında kalır.
        remaining -= exclusive_bytes(directory / filename, payload, remaining)
        candidate = validate_candidate(read_bounded(directory / filename, OUTPUT_BUDGET), format_name, pixels, values)
        candidate.update(file=filename, encodeRgbToMemoryMillis=elapsed, settings=SETTINGS[format_name])
        record["candidates"][format_name] = candidate
    record["writtenBytes"] = request["budget"] - remaining
    record["sourceProcessMillis"] = (time.perf_counter() - started) * 1000
    return record


def run_child(request, timeout=SOURCE_TIMEOUT):
    started = time.perf_counter()
    try:
        completed = subprocess.run([sys.executable, "-B", str(Path(__file__).absolute()), "--worker"],
                                   input=json_bytes(request), capture_output=True, timeout=timeout,
                                   env={**os.environ, "PYTHONDONTWRITEBYTECODE": "1"})
    except subprocess.TimeoutExpired:
        raise PreflightError("SOURCE_TIMEOUT") from None
    try:
        result = json.loads(completed.stdout)
    except (UnicodeError, ValueError):
        raise PreflightError("SOURCE_WORKER_FAILED") from None
    require(completed.returncode == 0 and result.get("status") == "PASS",
            result.get("error", "SOURCE_WORKER_FAILED"))
    record = result["record"]
    record["wallProcessMillis"] = (time.perf_counter() - started) * 1000
    return record


def load_inputs(csv_path, mapping_path):
    csv_data = read_bounded(csv_path, 2 * 1024 * 1024)
    require(digest(csv_data) == CSV_SHA256, "CSV_BASELINE_SHA256_MISMATCH")
    rows = parse_csv(csv_data)
    inventory = validate_inventory(rows)
    require(not inventory["errors"], "INVENTORY_INVALID")
    baseline = summarize(rows, inventory["sources"])
    require(all(delta == 0 for delta in baseline["deltaFromP5a"].values()), "INVENTORY_BASELINE_MISMATCH")
    mapping_data = read_bounded(mapping_path, 1024 * 1024)
    mapping = load_mapping(mapping_data)
    require(set(mapping) == {str(asset) for asset in inventory["sources"]}, "MAPPING_SCOPE_MISMATCH")
    requests = []
    for asset, expected in sorted(inventory["sources"].items()):
        roles = [{"role": row["role"], "pageNumber": row["page_number"]} for row in rows
                 if row["source_asset_id"] == asset]
        requests.append({"source": mapping[str(asset)], "expected": expected, "roles": roles, "mode": "check"})
    return requests, {"csv": digest(csv_data), "mapping": digest(mapping_data)}


def protected_snapshot():
    """Önceki yerel kanıtların tüm byte'larını hash'ler; içerik değiştirmez."""
    result = {}
    evidence = ROOT / "be/docs/evidence"
    for path in sorted(evidence.rglob("*")):
        require(not path.is_symlink(), "PROTECTED_EVIDENCE_SYMLINK")
        if not path.is_file():
            continue
        hasher = sha256()
        with path.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                hasher.update(block)
        result[str(path.relative_to(ROOT))] = hasher.hexdigest()
    return result


def preservation(requests, input_paths, input_hashes, before):
    sources = []
    for request in requests:
        observed = digest(read_bounded(request["source"]))
        sources.append({"assetId": request["expected"]["source_asset_id"],
                        "beforeSha256": request["expected"]["checksum_sha256"].lower(),
                        "afterSha256": observed,
                        "equal": observed == request["expected"]["checksum_sha256"].lower()})
    input_equal = {key: digest(read_bounded(path)) == input_hashes[key] for key, path in input_paths.items()}
    after = protected_snapshot()
    return {"sources": sources, "allSourcesUnchanged": all(source["equal"] for source in sources),
            "inputHashesUnchanged": input_equal, "previousEvidenceFiles": len(before),
            "previousEvidenceUnchanged": before == after,
            "previousEvidenceBefore": before, "previousEvidenceAfter": after}


def run_compare(csv_path, mapping_path, output_dir):
    """Ön kontrolleri bütün kaynaklarda bitirir; ilk engelde yeni encode başlatmaz."""
    directory = create_private_directory(output_dir)
    report = {"schemaVersion": 1, "scope": "P5C_LOCAL_LOSSLESS", "status": "BLOCKED", "errors": [],
              "measuredAtUtc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "p2Result": "NOT_EVALUATED", "productionAcceptance": False, "deviceAcceptance": False,
              "visualQualityAcceptance": False, "networkUsed": False, "sources": [],
              "limits": {"sourceBytes": MAX_FILE_BYTES, "pixels": 40_000_000,
                         "sourceProcessSeconds": SOURCE_TIMEOUT, "totalOutputBytes": OUTPUT_BUDGET},
              "runtime": {"python": platform.python_version(), "pillow": PILLOW_VERSION,
                          "libwebp": features.version("webp"), "zlib": features.version("zlib"),
                          "platform": platform.platform()}, "settings": SETTINGS,
              "timingScope": "LOCAL_PILLOW_ONLY_NOT_MOBILE_PERFORMANCE",
              "colorConversion": False, "resize": False}
    requests, input_hashes, before = [], {}, protected_snapshot()
    written = 0
    try:
        requests, input_hashes = load_inputs(csv_path, mapping_path)
        report["inputSha256"] = input_hashes
        report["preflightSources"] = []
        report["stage"] = "PREFLIGHT"
        for request in requests:
            report["activeAssetId"] = request["expected"]["source_asset_id"]
            report["preflightSources"].append(run_child(request))
        report["stage"] = "ENCODE"
        for request, preflight in zip(requests, report["preflightSources"]):
            report["activeAssetId"] = request["expected"]["source_asset_id"]
            timeout = SOURCE_TIMEOUT - preflight["wallProcessMillis"] / 1000
            require(timeout > 0, "SOURCE_TIMEOUT")
            encoded = run_child({**request, "mode": "encode", "output": str(directory),
                                 "budget": OUTPUT_BUDGET - REPORT_RESERVE - written}, timeout=timeout)
            report["sources"].append(encoded)
            written += encoded["writtenBytes"]
            print(f'P5c: {len(report["sources"])}/41 kaynak, {2 * len(report["sources"])}/82 aday doğrulandı.',
                  file=sys.stderr, flush=True)
        written += exclusive_bytes(directory / "gallery.html", gallery(report["sources"]),
                                   OUTPUT_BUDGET - REPORT_RESERVE - written)
        report["status"] = "LOCAL_LOSSLESS_COMPLETE"
        report["stage"] = "COMPLETE"
        del report["activeAssetId"]
    except (PreflightError, OSError, ValueError) as error:
        report["errors"].append(str(error) if isinstance(error, PreflightError) else "LOCAL_OPERATION_FAILED")
    finally:
        if requests:
            try:
                report["preservation"] = preservation(requests, {"csv": csv_path, "mapping": mapping_path},
                                                      input_hashes, before)
                require(report["preservation"]["allSourcesUnchanged"] and
                        all(report["preservation"]["inputHashesUnchanged"].values()) and
                        report["preservation"]["previousEvidenceUnchanged"], "PROTECTED_INPUT_CHANGED")
            except (PreflightError, OSError) as error:
                report["errors"].append(str(error) if isinstance(error, PreflightError) else "PRESERVATION_FAILED")
        report["pilots"] = totals(report["sources"])
        report["candidateCount"] = sum(len(record["candidates"]) for record in report["sources"])
        report["uniqueSources"] = len(report["sources"])
        report["roleBindings"] = sum(len(record["roles"]) for record in report["sources"])
        if report["errors"]:
            report["status"] = "BLOCKED"
        # Geride kalan kısmi adaylar da toplam bütçeye dahildir; hiçbir dosya silinmez.
        written = sum(path.stat().st_size for path in directory.iterdir() if path.is_file())
        report["outputBytesBeforeReport"] = written
        payload = json_bytes(report)
        exclusive_bytes(directory / "report.json", payload, OUTPUT_BUDGET - written)
    return report


def main(argv=None):
    if (argv if argv is not None else sys.argv[1:]) == ["--worker"]:
        try:
            record = worker(json.load(sys.stdin))
            sys.stdout.buffer.write(json_bytes({"status": "PASS", "record": record}))
            return 0
        except Exception as error:
            code = str(error) if isinstance(error, PreflightError) else "SOURCE_DECODE_OR_ENCODE_FAILED"
            sys.stdout.buffer.write(json_bytes({"status": "FAIL", "error": code}))
            return 1
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--csv", required=True, type=Path)
    parser.add_argument("--mapping", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        report = run_compare(args.csv, args.mapping, args.output_dir)
    except (PreflightError, OSError):
        print("Yeni, özel ve repo dışı bir çıktı dizini gerekir; var olan yol kullanılmaz.", file=sys.stderr)
        return 2
    print(f'P5c: {report["status"]}; {report["candidateCount"]} aday. P2/üretim/cihaz kabulü açık.')
    return 0 if report["status"] == "LOCAL_LOSSLESS_COMPLETE" else 1


if __name__ == "__main__":
    sys.exit(main())
