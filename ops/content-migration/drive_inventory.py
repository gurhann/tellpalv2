#!/usr/bin/env python3
"""Export a recursive Google Drive folder inventory as JSONL."""

from __future__ import annotations

import argparse
import json
import time
import sys
from collections import deque
from pathlib import Path
from typing import TextIO

from drive_api import DRIVE_FOLDER_MIME_TYPE, DriveApiClient, build_drive_client


DEFAULT_TELLPAL_ORTAK_FOLDER_ID = "1valmQ5i9wyEhqJ_YG-NLkHdDFwF-fuLi"


def export_inventory(
    client: DriveApiClient,
    root_folder_id: str,
    progress_every: int = 25,
    logger: "ProgressLogger | None" = None,
) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    queue = deque([(root_folder_id, "TELLPAL-ORTAK")])
    visited: set[str] = set()

    while queue:
        folder_id, folder_path = queue.popleft()
        if folder_id in visited:
            continue
        visited.add(folder_id)
        if logger:
            logger.log(f"Listing folder {len(visited)}: {folder_path} ({folder_id})")
        children = client.list_children(folder_id)
        if logger:
            logger.log(f"Found {len(children)} child items in {folder_path}")
        for item in children:
            item_path = f"{folder_path}/{item.name}"
            rows.append(
                {
                    "id": item.file_id,
                    "title": item.name,
                    "mime_type": item.mime_type,
                    "url": item.web_view_link,
                    "path": item_path,
                    "parent_id": folder_id,
                    "modified_time": item.modified_time,
                }
            )
            if item.mime_type == DRIVE_FOLDER_MIME_TYPE:
                queue.append((item.file_id, item_path))
        if logger and (len(visited) % progress_every == 0 or not queue):
            logger.log(
                f"Progress: folders_listed={len(visited)} items_seen={len(rows)} folders_queued={len(queue)}"
            )
    return rows


def export_inventory_to_jsonl(
    client: DriveApiClient,
    root_folder_id: str,
    output: Path,
    progress_every: int = 25,
    logger: "ProgressLogger | None" = None,
) -> int:
    output.parent.mkdir(parents=True, exist_ok=True)
    item_count = 0
    queue = deque([(root_folder_id, "TELLPAL-ORTAK")])
    visited: set[str] = set()
    started_at = time.monotonic()

    with output.open("w", encoding="utf-8") as handle:
        while queue:
            folder_id, folder_path = queue.popleft()
            if folder_id in visited:
                continue
            visited.add(folder_id)
            if logger:
                logger.log(f"Listing folder {len(visited)}: {folder_path} ({folder_id})")
            children = client.list_children(folder_id)
            if logger:
                logger.log(f"Found {len(children)} child items in {folder_path}")
            for item in children:
                item_path = f"{folder_path}/{item.name}"
                row = {
                    "id": item.file_id,
                    "title": item.name,
                    "mime_type": item.mime_type,
                    "url": item.web_view_link,
                    "path": item_path,
                    "parent_id": folder_id,
                    "modified_time": item.modified_time,
                }
                handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True))
                handle.write("\n")
                item_count += 1
                if item.mime_type == DRIVE_FOLDER_MIME_TYPE:
                    queue.append((item.file_id, item_path))
            handle.flush()
            if logger and (len(visited) % progress_every == 0 or not queue):
                elapsed = time.monotonic() - started_at
                logger.log(
                    "Progress: "
                    f"folders_listed={len(visited)} items_seen={item_count} "
                    f"folders_queued={len(queue)} elapsed_seconds={elapsed:.1f}"
                )
    if logger:
        logger.log(f"Completed inventory export: items={item_count} output={output}")
    return item_count


def write_jsonl(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True))
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
    parser.add_argument("--root-folder-id", default=DEFAULT_TELLPAL_ORTAK_FOLDER_ID)
    parser.add_argument("--output", type=Path, default=Path("ops/content-migration/drive_inventory_cache.jsonl"))
    parser.add_argument("--log-file", type=Path, default=Path("ops/content-migration/out/drive_inventory.log"))
    parser.add_argument("--progress-every", type=int, default=25)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    with ProgressLogger(args.log_file) as logger:
        logger.log(f"Starting inventory export: root_folder_id={args.root_folder_id}")
        item_count = export_inventory_to_jsonl(
            build_drive_client(),
            args.root_folder_id,
            args.output,
            progress_every=args.progress_every,
            logger=logger,
        )
    print(json.dumps({"items": item_count, "output": str(args.output)}, ensure_ascii=True, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
