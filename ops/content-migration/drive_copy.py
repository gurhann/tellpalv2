#!/usr/bin/env python3
"""Copy approved Drive assets into the TellPal V2 inventory folder structure."""

from __future__ import annotations

import argparse
import csv
import json
import sys
import time
from collections import Counter
from pathlib import Path
from typing import TextIO

from drive_api import DriveApiClient, build_drive_client
from drive_inventory import DEFAULT_TELLPAL_ORTAK_FOLDER_ID


COPY_RESULT_FIELDS = [
    "source_drive_id",
    "source_title",
    "canonical_key",
    "asset_role",
    "language",
    "target_content_folder",
    "target_subfolder",
    "target_file_id",
    "status",
    "message",
]


def load_copy_plan(path: Path) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    with path.open("r", encoding="utf-8-sig") as handle:
        for line in handle:
            if line.strip():
                rows.append(json.loads(line))
    return rows


def execute_copy_plan(
    client: DriveApiClient | None,
    copy_plan: list[dict[str, str]],
    target_parent_id: str,
    target_root_name: str,
    execute: bool,
) -> list[dict[str, str]]:
    if execute and client is None:
        raise ValueError("client is required in execute mode")

    results: list[dict[str, str]] = []
    target_root_id = ""
    folder_cache: dict[tuple[str, str], str] = {}

    if execute:
        assert client is not None
        target_root_id = ensure_folder(client, target_parent_id, target_root_name, folder_cache)

    for item in copy_plan:
        try:
            if execute:
                assert client is not None
                target_folder_id = ensure_target_folder(client, target_root_id, item, folder_cache)
                existing = client.find_child_file(target_folder_id, item["source_title"])
                if existing is not None:
                    results.append(result_row(item, existing.file_id, "SKIPPED_DUPLICATE", "Target file already exists"))
                    continue
                copied = client.copy_file(item["source_drive_id"], target_folder_id, item["source_title"])
                results.append(result_row(item, copied.file_id, "COPIED", ""))
            else:
                results.append(result_row(item, "", "DRY_RUN", "No Drive write attempted"))
        except Exception as exc:  # noqa: BLE001 - migration report must capture per-file failures.
            results.append(result_row(item, "", "FAILED", str(exc)))
    return results


def execute_copy_plan_streaming(
    client: DriveApiClient | None,
    copy_plan: list[dict[str, str]],
    target_parent_id: str,
    target_root_name: str,
    output_dir: Path,
    execute: bool,
    logger: "ProgressLogger | None" = None,
    progress_every: int = 10,
) -> dict[str, int]:
    if execute and client is None:
        raise ValueError("client is required in execute mode")

    output_dir.mkdir(parents=True, exist_ok=True)
    result_path = output_dir / "copy_result.csv"
    summary_counter: Counter[str] = Counter()
    target_root_id = ""
    folder_cache: dict[tuple[str, str], str] = {}
    started_at = time.monotonic()

    if execute:
        assert client is not None
        if logger:
            logger.log(f"Ensuring target root folder: {target_root_name}")
        target_root_id = ensure_folder(client, target_parent_id, target_root_name, folder_cache)

    with result_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=COPY_RESULT_FIELDS, extrasaction="ignore")
        writer.writeheader()
        for index, item in enumerate(copy_plan, start=1):
            row = copy_one_item(client, item, target_root_id, folder_cache, execute)
            writer.writerow(row)
            handle.flush()
            summary_counter[row["status"]] += 1
            if logger and (index % progress_every == 0 or index == len(copy_plan)):
                elapsed = time.monotonic() - started_at
                logger.log(
                    "Progress: "
                    f"processed={index}/{len(copy_plan)} status={dict(sorted(summary_counter.items()))} "
                    f"elapsed_seconds={elapsed:.1f}"
                )

    summary = dict(sorted(summary_counter.items()))
    write_summary(output_dir, summary)
    if logger:
        logger.log(f"Completed copy plan: {summary}")
    return summary


def copy_one_item(
    client: DriveApiClient | None,
    item: dict[str, str],
    target_root_id: str,
    folder_cache: dict[tuple[str, str], str],
    execute: bool,
) -> dict[str, str]:
    try:
        if execute:
            assert client is not None
            target_folder_id = ensure_target_folder(client, target_root_id, item, folder_cache)
            existing = client.find_child_file(target_folder_id, item["source_title"])
            if existing is not None:
                return result_row(item, existing.file_id, "SKIPPED_DUPLICATE", "Target file already exists")
            copied = client.copy_file(item["source_drive_id"], target_folder_id, item["source_title"])
            return result_row(item, copied.file_id, "COPIED", "")
        return result_row(item, "", "DRY_RUN", "No Drive write attempted")
    except Exception as exc:  # noqa: BLE001 - migration report must capture per-file failures.
        return result_row(item, "", "FAILED", str(exc))


def ensure_target_folder(
    client: DriveApiClient,
    target_root_id: str,
    item: dict[str, str],
    folder_cache: dict[tuple[str, str], str],
) -> str:
    content_folder_id = ensure_folder(client, target_root_id, item["target_content_folder"], folder_cache)
    current_parent = content_folder_id
    for segment in split_subfolder(item.get("target_subfolder", "")):
        current_parent = ensure_folder(client, current_parent, segment, folder_cache)
    return current_parent


def ensure_folder(
    client: DriveApiClient,
    parent_id: str,
    name: str,
    folder_cache: dict[tuple[str, str], str],
) -> str:
    cache_key = (parent_id, name)
    cached = folder_cache.get(cache_key)
    if cached:
        return cached
    existing = client.find_child_folder(parent_id, name)
    if existing is not None:
        folder_cache[cache_key] = existing.file_id
        return existing.file_id
    created = client.create_folder(parent_id, name)
    folder_cache[cache_key] = created.file_id
    return created.file_id


def split_subfolder(value: str) -> list[str]:
    return [segment for segment in value.replace("\\", "/").split("/") if segment]


def result_row(item: dict[str, str], target_file_id: str, status: str, message: str) -> dict[str, str]:
    return {
        "source_drive_id": item.get("source_drive_id", ""),
        "source_title": item.get("source_title", ""),
        "canonical_key": item.get("canonical_key", ""),
        "asset_role": item.get("asset_role", ""),
        "language": item.get("language", ""),
        "target_content_folder": item.get("target_content_folder", ""),
        "target_subfolder": item.get("target_subfolder", ""),
        "target_file_id": target_file_id,
        "status": status,
        "message": message,
    }


def write_results(output_dir: Path, rows: list[dict[str, str]]) -> dict[str, int]:
    output_dir.mkdir(parents=True, exist_ok=True)
    with (output_dir / "copy_result.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=COPY_RESULT_FIELDS, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
    summary = dict(sorted(Counter(row["status"] for row in rows).items()))
    write_summary(output_dir, summary)
    return summary


def write_summary(output_dir: Path, summary: dict[str, int]) -> None:
    with (output_dir / "post_copy_summary.json").open("w", encoding="utf-8") as handle:
        json.dump(summary, handle, ensure_ascii=False, indent=2, sort_keys=True)
        handle.write("\n")


class ProgressLogger:
    def __init__(self, path: Path | None):
        self.path = path
        self.handle: TextIO | None = None

    def __enter__(self) -> "ProgressLogger":
        if self.path is not None:
            self.path.parent.mkdir(parents=True, exist_ok=True)
            self.handle = self.path.open("w", encoding="utf-8")
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:
        if self.handle is not None:
            self.handle.close()

    def log(self, message: str) -> None:
        line = f"{time.strftime('%Y-%m-%d %H:%M:%S')} {message}"
        safe_line = line.encode(sys.stdout.encoding or "utf-8", errors="backslashreplace").decode(
            sys.stdout.encoding or "utf-8"
        )
        print(safe_line, flush=True)
        if self.handle is not None:
            self.handle.write(line)
            self.handle.write("\n")
            self.handle.flush()


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--copy-plan", type=Path, required=True)
    parser.add_argument("--target-parent-id", default=DEFAULT_TELLPAL_ORTAK_FOLDER_ID)
    parser.add_argument("--target-root-name", default="V2 İçerik Envanteri")
    parser.add_argument("--output-dir", type=Path, default=Path("ops/content-migration/out"))
    parser.add_argument("--log-file", type=Path, default=Path("ops/content-migration/out/drive_copy.log"))
    parser.add_argument("--progress-every", type=int, default=10)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--dry-run", action="store_true")
    mode.add_argument("--execute", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    execute = bool(args.execute)
    copy_plan = load_copy_plan(args.copy_plan)
    client = build_drive_client() if execute else None
    with ProgressLogger(args.log_file) as logger:
        logger.log(f"Starting copy plan: entries={len(copy_plan)} execute={execute}")
        summary = execute_copy_plan_streaming(
            client=client,
            copy_plan=copy_plan,
            target_parent_id=args.target_parent_id,
            target_root_name=args.target_root_name,
            output_dir=args.output_dir,
            execute=execute,
            logger=logger,
            progress_every=args.progress_every,
        )
    print(json.dumps(summary, ensure_ascii=True, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
