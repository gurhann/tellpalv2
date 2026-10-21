"""Başlıklı P5b CSV sözleşmesini ve rol/kaynak sayımlarını doğrular."""
import csv
from datetime import datetime
from io import StringIO
import re

from p5b_source_io import PreflightError, require

PILOTS = {18: ("story.kocaman-bir-sarilma", 16, 17, 6_191_423),
          62: ("story.pofuduk-misir-pops", 23, 24, 120_305_904)}
FIELDS = ("observed_at", "pilot_content_id", "expected_external_key", "language_code", "content_id",
          "external_key", "content_type", "declared_page_count", "localization_id", "role", "page_number",
          "page_id", "page_localization_id", "source_asset_id", "asset_id", "provider", "object_path",
          "mime_type", "byte_size", "checksum_sha256")
NUMERIC = {"pilot_content_id", "content_id", "declared_page_count", "localization_id", "page_number",
           "page_id", "page_localization_id", "source_asset_id", "asset_id", "byte_size"}
HASH = re.compile(r"[0-9a-fA-F]{64}")
ASSET_METADATA = ("asset_id", "provider", "object_path", "mime_type", "byte_size", "checksum_sha256")


def parse_csv(data):
    try:
        reader = csv.DictReader(StringIO(data.decode("utf-8-sig")), strict=True)
        require(reader.fieldnames is not None and len(reader.fieldnames) == len(FIELDS) and
                set(reader.fieldnames) == set(FIELDS), "CSV_HEADER_INVALID")
        rows = []
        for row in reader:
            require(len(rows) < 1000 and None not in row and None not in row.values(), "CSV_ROW_INVALID")
            row = {key: None if value in ("", "NULL", "\\N") else value for key, value in row.items()}
            for key in NUMERIC:
                value = row[key]
                if value is None:
                    continue
                require(bool(re.fullmatch(r"[0-9]{1,19}", value)), "CSV_INTEGER_INVALID")
                row[key] = int(value)
                require(row[key] >= (0 if key == "declared_page_count" else 1), "CSV_INTEGER_INVALID")
            require(row["declared_page_count"] is None or row["declared_page_count"] <= 997,
                    "DECLARED_PAGE_COUNT_LIMIT")
            require(row["pilot_content_id"] in PILOTS and row["language_code"] == "tr" and
                    row["expected_external_key"] == PILOTS[row["pilot_content_id"]][0], "PILOT_SCOPE_INVALID")
            require(row["role"] in ("READING_COVER", "READING_DETAIL", "READING_PAGE"), "ROLE_INVALID")
            rows.append(row)
        require(bool(rows), "CSV_EMPTY")
        return rows
    except (csv.Error, UnicodeError):
        raise PreflightError("CSV_INVALID") from None


def validate_inventory(rows):
    errors, sources, roles, seen, times = [], {}, [], set(), set()
    for index, row in enumerate(rows, start=2):
        def error(code, **values):
            errors.append({"code": code, "csvRecord": index, **values})
        try:
            timestamp = datetime.fromisoformat(row["observed_at"] or "")
            require(timestamp.tzinfo is not None, "TIMESTAMP_INVALID")
            times.add(timestamp)
        except (ValueError, PreflightError):
            error("TIMESTAMP_INVALID")
        for field in ("content_id", "localization_id", "declared_page_count", "source_asset_id", *ASSET_METADATA):
            if row[field] is None:
                error("MISSING_VALUE", field=field)
            elif isinstance(row[field], str) and not row[field].strip():
                error("BLANK_VALUE", field=field)
        if row["content_id"] != row["pilot_content_id"] or row["external_key"] != row["expected_external_key"] or row["content_type"] != "STORY":
            error("STORY_IDENTITY_MISMATCH")
        slot = (row["pilot_content_id"], row["role"], row["page_number"])
        if slot in seen:
            error("DUPLICATE_ROLE_SLOT")
        seen.add(slot)
        if row["role"] == "READING_PAGE":
            for field in ("page_number", "page_id", "page_localization_id"):
                if row[field] is None:
                    error("MISSING_PAGE_BINDING", field=field)
        elif any(row[field] is not None for field in ("page_number", "page_id", "page_localization_id")):
            error("UNEXPECTED_PAGE_BINDING")
        if row["asset_id"] != row["source_asset_id"]:
            error("MEDIA_BINDING_MISMATCH")
        if row["checksum_sha256"] is not None and not HASH.fullmatch(row["checksum_sha256"]):
            error("SHA256_INVALID")
        asset = row["source_asset_id"]
        if asset is not None:
            if asset in sources and any(row[key] != sources[asset][key] for key in ASSET_METADATA):
                error("CONFLICTING_ASSET_METADATA", assetId=asset)
            else:
                sources[asset] = row
        roles.append({key: row[key] for key in FIELDS if key not in ("object_path", "cover_media_id")})
    if len(times) != 1:
        errors.append({"code": "SINGLE_STATEMENT_TIMESTAMP_REQUIRED"})
    summaries = []
    for pilot, baseline in PILOTS.items():
        group = [row for row in rows if row["pilot_content_id"] == pilot]
        declared = {row["declared_page_count"] for row in group}
        identities = {tuple(row[key] for key in ("content_id", "external_key", "content_type", "localization_id")) for row in group}
        if len(declared) != 1 or None in declared or len(identities) != 1:
            errors.append({"code": "MISSING_OR_INCONSISTENT_SCOPE", "pilotContentId": pilot})
        count = next(iter(declared)) if len(declared) == 1 else None
        actual_pages = {row["page_number"] for row in group if row["role"] == "READING_PAGE"}
        if count is None or count > 997 or actual_pages != set(range(1, count + 1)):
            errors.append({"code": "PAGE_SET_MISMATCH", "pilotContentId": pilot})
        for role in ("READING_COVER", "READING_DETAIL"):
            if sum(row["role"] == role for row in group) != 1:
                errors.append({"code": "MISSING_OR_DUPLICATE_ROLE", "pilotContentId": pilot, "role": role})
        summaries.append(summarize(group, sources, pilot, baseline))
    return {"errors": errors, "roles": roles, "sources": sources, "pilots": summaries,
            "observedAt": next(iter(times)).isoformat() if len(times) == 1 else None}


def summarize(rows, sources, pilot=None, baseline=None):
    ids = {row["source_asset_id"] for row in rows if row["source_asset_id"] is not None}
    metadata_complete = all(sources[asset]["byte_size"] is not None and sources[asset]["asset_id"] == asset and
        {tuple(row[key] for key in ASSET_METADATA) for row in rows if row["source_asset_id"] == asset} ==
        {tuple(sources[asset][key] for key in ASSET_METADATA)}
        for asset in ids)
    missing = any(row["source_asset_id"] is None for row in rows)
    total = sum(sources[asset]["byte_size"] for asset in ids) if metadata_complete and not missing and rows else None
    expected = {"roleRows": baseline[1] + 2 if baseline else 43,
                "boundRoleLinks": baseline[1] + 2 if baseline else 43,
                "uniqueSources": baseline[2] if baseline else 41,
                "recordedUniqueBytes": baseline[3] if baseline else 126_497_327}
    observed = {"roleRows": len(rows), "boundRoleLinks": sum(row["source_asset_id"] is not None and
                row["asset_id"] == row["source_asset_id"] for row in rows), "uniqueSources": len(ids),
                "recordedUniqueBytes": total}
    return {"pilotContentId": pilot, "expectedP5a": expected, "observedCsv": observed,
            "deltaFromP5a": {key: None if observed[key] is None else observed[key] - value for key, value in expected.items()}}
