"""İki TR pilotunun özel CSV/orijinallerini ağsız ve salt okunur inceler."""
import argparse
from hashlib import sha256
import json
import os
from pathlib import Path
import sys

from p5b_image_probe import MAX_PIXELS, inspect_image
from p5b_inventory import HASH, parse_csv, summarize, validate_inventory
from p5b_source_io import MAX_FILE_BYTES, PreflightError, read_bounded, require, write_report


def load_mapping(data):
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, "DUPLICATE_MAPPING_KEY")
            result[key] = value
        return result
    try:
        mapping = json.loads(data.decode("utf-8"), object_pairs_hook=unique_pairs)
        require(type(mapping) is dict and len(mapping) <= 1000, "MAPPING_INVALID")
        for asset, path in mapping.items():
            require(asset.isascii() and asset.isdigit() and str(int(asset)) == asset and int(asset) > 0,
                    "MAPPING_ASSET_ID_INVALID")
            require(type(path) is str and 0 < len(path) <= 4096 and "\x00" not in path and
                    Path(path).is_absolute(), "MAPPING_PATH_INVALID")
            try:
                os.fsencode(path)
            except UnicodeEncodeError:
                raise PreflightError("MAPPING_PATH_INVALID") from None
        return mapping
    except PreflightError:
        raise
    except (ValueError, UnicodeError, RecursionError):
        raise PreflightError("MAPPING_INVALID") from None


def inspect_source(asset, metadata, path):
    result = {"assetId": asset, "p2Result": "NOT_EVALUATED", "status": "BLOCKED", "errors": [],
              "expected": {"bytes": metadata["byte_size"], "sha256": metadata["checksum_sha256"],
                           "mime": metadata["mime_type"]}, "observed": None,
              "byteMatch": None, "sha256Match": None, "mimeMatch": None}
    if path is None:
        result["errors"].append("ORIGINAL_NOT_PROVIDED")
        return result
    try:
        data = read_bounded(path)
        digest = sha256(data).hexdigest()
        result["observed"] = {"bytes": len(data), "sha256": digest, "image": inspect_image(data)}
        for key, expected, observed in (
            ("byteMatch", metadata["byte_size"], len(data)),
            ("sha256Match", metadata["checksum_sha256"], digest),
            ("mimeMatch", metadata["mime_type"], result["observed"]["image"].get("mime")),
        ):
            if expected is None or key == "sha256Match" and not HASH.fullmatch(expected):
                result["errors"].append(key.upper() + "_UNVERIFIED")
                continue
            result[key] = expected.lower() == observed if key == "sha256Match" else expected == observed
            if not result[key]:
                result["errors"].append(key.upper() + "_MISMATCH")
        result["errors"].extend(result["observed"]["image"]["errors"])
    except PreflightError as error:
        result["errors"].append(str(error))
    result["status"] = "BLOCKED" if result["errors"] else "LOCAL_OBSERVATIONS_COMPLETE"
    return result


def run_preflight(csv_path, mapping_path):
    report = {"schemaVersion": 1, "scope": "p5b-pilot-source-preflight", "status": "BLOCKED",
              "p2Result": "NOT_EVALUATED", "realPilotValidation": "NOT_ESTABLISHED",
              "p5bCompletion": "NOT_ESTABLISHED", "errors": [], "roles": [], "sources": [],
              "localLimits": {"maxFileBytes": MAX_FILE_BYTES, "maxPixels": MAX_PIXELS,
                              "productionProfile": False},
              "notObserved": ["currentLiveObjectExistence", "storageGenerationOrVersion", "currentLiveBytes",
                              "immutability", "visualQuality", "productionAcceptance", "p2Admission"],
              "notes": ["Yalnız kullanıcı tarafından sağlanan yerel kopyalar ve CSV karşılaştırılır.",
                        "Kapak/detail ortak kaynağı rol sayımında iki, dosya/byte sayımında bir kez yer alır.",
                        "Yerel decode başarısı P2 veya üretim kabulü değildir."]}
    try:
        csv_data = read_bounded(csv_path, 2 * 1024 * 1024)
        mapping_data = read_bounded(mapping_path, 1024 * 1024)
        mapping = load_mapping(mapping_data)
        rows = parse_csv(csv_data)
        inventory = validate_inventory(rows)
        report.update(errors=inventory["errors"], roles=inventory["roles"], pilots=inventory["pilots"],
                      observedAt=inventory["observedAt"], baselineComparison=summarize(rows, inventory["sources"]),
                      inputSha256={"csv": sha256(csv_data).hexdigest(), "mapping": sha256(mapping_data).hexdigest()})
        source_ids = {str(asset) for asset in inventory["sources"]}
        if set(mapping) - source_ids:
            report["errors"].append({"code": "MAPPING_OUTSIDE_PILOT", "assetIds": sorted(set(mapping) - source_ids)})
        report["sources"] = [inspect_source(asset, metadata, mapping.get(str(asset)))
                             for asset, metadata in sorted(inventory["sources"].items())]
        observed_sources = [source["observed"] for source in report["sources"]]
        report["localSourceTotals"] = {"providedSources": sum(source is not None for source in observed_sources),
            "uniqueBytes": sum(source["bytes"] for source in observed_sources) if
            all(source is not None for source in observed_sources) and observed_sources else None}
        if not report["errors"] and all(not source["errors"] for source in report["sources"]):
            report["status"] = "LOCAL_OBSERVATIONS_COMPLETE"
    except PreflightError as error:
        report["errors"].append({"code": str(error)})
    return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--csv", required=True, type=Path)
    parser.add_argument("--mapping", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args(argv)
    report = run_preflight(args.csv, args.mapping)
    try:
        write_report(args.report, (json.dumps(report, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode("utf-8"))
    except (PreflightError, OSError):
        print("P5b raporu yazılamadı; yeni ve özel bir çıktı yolu kullanın.", file=sys.stderr)
        return 2
    print(f"P5b yerel rapor: {report['status']}; P2 ve gerçek pilot kabulü değerlendirilmedi.")
    return 0 if report["status"] == "LOCAL_OBSERVATIONS_COMPLETE" else 1


if __name__ == "__main__":
    sys.exit(main())
