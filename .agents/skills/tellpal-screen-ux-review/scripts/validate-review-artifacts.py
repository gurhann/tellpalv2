#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# ///
"""Validate TellPal UX review artifacts for function and domain coverage."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

REQUIRED_FILES = ("function-map.json", "domain-evidence.json", "findings.json", "change-plan.json")


def load_json(path: Path, errors: list[str]) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{path.name}: invalid JSON ({exc})")
        return {}
    if not isinstance(data, dict):
        errors.append(f"{path.name}: root must be an object")
        return {}
    return data


def records(data: dict[str, Any], key: str, filename: str, errors: list[str]) -> list[dict[str, Any]]:
    value = data.get(key)
    if not isinstance(value, list):
        errors.append(f"{filename}: '{key}' must be an array")
        return []
    invalid = [index for index, item in enumerate(value) if not isinstance(item, dict)]
    if invalid:
        errors.append(f"{filename}: '{key}' entries at {invalid} must be objects")
    return [item for item in value if isinstance(item, dict)]


def require_fields(items: list[dict[str, Any]], fields: tuple[str, ...], label: str, errors: list[str]) -> None:
    for index, item in enumerate(items):
        missing = [field for field in fields if not item.get(field)]
        if missing:
            errors.append(f"{label}[{index}]: missing {', '.join(missing)}")


def validate(run_folder: Path) -> dict[str, Any]:
    errors: list[str] = []
    missing_files = [name for name in REQUIRED_FILES if not (run_folder / name).is_file()]
    if missing_files:
        errors.extend(f"missing required artifact: {name}" for name in missing_files)
        return {"ok": False, "errors": errors, "warnings": []}

    artifacts = {name: load_json(run_folder / name, errors) for name in REQUIRED_FILES}
    functions = records(artifacts["function-map.json"], "functions", "function-map.json", errors)
    evidence = records(artifacts["domain-evidence.json"], "evidence", "domain-evidence.json", errors)
    findings = records(artifacts["findings.json"], "findings", "findings.json", errors)
    changes = records(artifacts["change-plan.json"], "changes", "change-plan.json", errors)

    require_fields(functions, ("id", "purpose", "trigger", "result", "evidence_ids"), "functions", errors)
    require_fields(evidence, ("id", "source", "claim", "verdict"), "evidence", errors)
    require_fields(findings, ("id", "severity", "user_impact", "affected_function_ids"), "findings", errors)
    require_fields(changes, ("id", "finding_id", "scope", "preserved_function_ids", "evidence_ids", "change"), "changes", errors)

    function_ids = {item.get("id") for item in functions if item.get("id")}
    evidence_by_id = {item.get("id"): item for item in evidence if item.get("id")}
    finding_ids = {item.get("id") for item in findings if item.get("id")}
    verified_evidence_ids = {item_id for item_id, item in evidence_by_id.items() if item.get("verdict") == "VERIFIED"}

    for item in functions:
        for evidence_id in item.get("evidence_ids", []):
            if evidence_id not in evidence_by_id:
                errors.append(f"function '{item.get('id')}': unknown evidence '{evidence_id}'")
    for item in findings:
        for function_id in item.get("affected_function_ids", []):
            if function_id not in function_ids:
                errors.append(f"finding '{item.get('id')}': unknown function '{function_id}'")
    for item in changes:
        if item.get("finding_id") not in finding_ids:
            errors.append(f"change '{item.get('id')}': unknown finding '{item.get('finding_id')}'")
        if item.get("scope") not in {"mockup", "production"}:
            errors.append(f"change '{item.get('id')}': scope must be mockup or production")
        preserved = item.get("preserved_function_ids", [])
        if not isinstance(preserved, list) or not preserved:
            errors.append(f"change '{item.get('id')}': preserved_function_ids must not be empty")
        for function_id in preserved if isinstance(preserved, list) else []:
            if function_id not in function_ids:
                errors.append(f"change '{item.get('id')}': unknown preserved function '{function_id}'")
        for evidence_id in item.get("evidence_ids", []):
            if evidence_id not in verified_evidence_ids:
                errors.append(f"change '{item.get('id')}': evidence '{evidence_id}' is not VERIFIED")

    return {"ok": not errors, "errors": errors, "warnings": []}


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate function preservation and domain evidence in a UX review run.")
    parser.add_argument("run_folder", type=Path, help="Folder containing review JSON artifacts")
    parser.add_argument("-o", "--output", type=Path, help="Write JSON result to this file instead of stdout")
    args = parser.parse_args()
    result = validate(args.run_folder)
    rendered = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        args.output.write_text(rendered + "\n", encoding="utf-8")
    else:
        print(rendered)
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())

