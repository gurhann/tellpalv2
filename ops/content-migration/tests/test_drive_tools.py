import tempfile
import unittest
from pathlib import Path

import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import drive_copy
import drive_inventory
from drive_api import DRIVE_FOLDER_MIME_TYPE, DriveFile


class DriveInventoryTests(unittest.TestCase):
    def test_exports_recursive_inventory_schema(self):
        client = FakeDriveClient(
            children={
                "root": [
                    DriveFile("folder-1", "Texts", DRIVE_FOLDER_MIME_TYPE, "url-folder", "2026-01-01T00:00:00Z", ("root",)),
                    DriveFile("file-1", "Story.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "url-file", "2026-01-02T00:00:00Z", ("root",)),
                ],
                "folder-1": [
                    DriveFile("file-2", "Audio.mp3", "audio/mpeg", "url-audio", "2026-01-03T00:00:00Z", ("folder-1",)),
                ],
            }
        )

        rows = drive_inventory.export_inventory(client, "root")

        self.assertEqual(3, len(rows))
        self.assertEqual(
            {"id", "title", "mime_type", "url", "path", "parent_id", "modified_time"},
            set(rows[0].keys()),
        )
        self.assertEqual("TELLPAL-ORTAK/Texts/Audio.mp3", rows[2]["path"])

    def test_streams_inventory_to_jsonl(self):
        client = FakeDriveClient(
            children={
                "root": [
                    DriveFile("folder-1", "Texts", DRIVE_FOLDER_MIME_TYPE, "url-folder", "2026-01-01T00:00:00Z", ("root",)),
                ],
                "folder-1": [
                    DriveFile("file-1", "Story.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "url-file", "2026-01-02T00:00:00Z", ("folder-1",)),
                ],
            }
        )
        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / "drive_inventory_cache.jsonl"

            count = drive_inventory.export_inventory_to_jsonl(client, "root", output, progress_every=1)

            self.assertEqual(2, count)
            self.assertEqual(2, len(output.read_text(encoding="utf-8").splitlines()))


class DriveCopyTests(unittest.TestCase):
    def test_dry_run_writes_no_drive_changes(self):
        plan = [copy_plan_item()]
        client = FakeDriveClient(children={})

        rows = drive_copy.execute_copy_plan(client, plan, "parent", "V2 İçerik Envanteri", execute=False)

        self.assertEqual("DRY_RUN", rows[0]["status"])
        self.assertEqual([], client.created_folders)
        self.assertEqual([], client.copied_files)

    def test_execute_creates_target_folders_and_copies_file(self):
        plan = [copy_plan_item()]
        client = FakeDriveClient(children={"parent": []})

        rows = drive_copy.execute_copy_plan(client, plan, "parent", "V2 İçerik Envanteri", execute=True)

        self.assertEqual("COPIED", rows[0]["status"])
        self.assertEqual(
            ["V2 İçerik Envanteri", "STORY-story-test-test", "01_localizations", "tr", "texts"],
            [folder[1] for folder in client.created_folders],
        )
        self.assertEqual(("source-1", "Story.docx"), client.copied_files[0][:2])

    def test_execute_skips_duplicate_target_file(self):
        plan = [copy_plan_item()]
        client = FakeDriveClient(children={"parent": []})
        root = client.create_folder("parent", "V2 İçerik Envanteri")
        content = client.create_folder(root.file_id, "STORY-story-test-test")
        loc = client.create_folder(content.file_id, "01_localizations")
        lang = client.create_folder(loc.file_id, "tr")
        texts = client.create_folder(lang.file_id, "texts")
        client.children[texts.file_id].append(
            DriveFile("existing", "Story.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "", "", (texts.file_id,))
        )
        client.created_folders.clear()

        rows = drive_copy.execute_copy_plan(client, plan, "parent", "V2 İçerik Envanteri", execute=True)

        self.assertEqual("SKIPPED_DUPLICATE", rows[0]["status"])
        self.assertEqual([], client.copied_files)

    def test_writes_copy_result_summary(self):
        with tempfile.TemporaryDirectory() as tmp:
            output_dir = Path(tmp)

            summary = drive_copy.write_results(
                output_dir,
                [
                    drive_copy.result_row(copy_plan_item(), "target-1", "COPIED", ""),
                    drive_copy.result_row(copy_plan_item(), "", "SKIPPED_DUPLICATE", ""),
                ],
            )

            self.assertEqual({"COPIED": 1, "SKIPPED_DUPLICATE": 1}, summary)
            self.assertTrue((output_dir / "copy_result.csv").exists())
            self.assertTrue((output_dir / "post_copy_summary.json").exists())

    def test_streaming_copy_writes_partial_results_as_it_goes(self):
        with tempfile.TemporaryDirectory() as tmp:
            output_dir = Path(tmp)

            summary = drive_copy.execute_copy_plan_streaming(
                client=None,
                copy_plan=[copy_plan_item()],
                target_parent_id="parent",
                target_root_name="V2 İçerik Envanteri",
                output_dir=output_dir,
                execute=False,
            )

            self.assertEqual({"DRY_RUN": 1}, summary)
            self.assertIn("DRY_RUN", (output_dir / "copy_result.csv").read_text(encoding="utf-8"))


def copy_plan_item() -> dict[str, str]:
    return {
        "source_drive_id": "source-1",
        "source_title": "Story.docx",
        "canonical_key": "story-test",
        "asset_role": "text",
        "language": "tr",
        "target_content_folder": "STORY-story-test-test",
        "target_subfolder": "01_localizations/tr/texts",
    }


class FakeDriveClient:
    def __init__(self, children: dict[str, list[DriveFile]]):
        self.children = {key: list(value) for key, value in children.items()}
        self.created_folders: list[tuple[str, str]] = []
        self.copied_files: list[tuple[str, str, str]] = []
        self.next_id = 1

    def list_children(self, folder_id: str) -> list[DriveFile]:
        return list(self.children.get(folder_id, []))

    def find_child_folder(self, parent_id: str, name: str) -> DriveFile | None:
        for item in self.children.get(parent_id, []):
            if item.name == name and item.mime_type == DRIVE_FOLDER_MIME_TYPE:
                return item
        return None

    def find_child_file(self, parent_id: str, name: str) -> DriveFile | None:
        for item in self.children.get(parent_id, []):
            if item.name == name:
                return item
        return None

    def create_folder(self, parent_id: str, name: str) -> DriveFile:
        file_id = f"folder-{self.next_id}"
        self.next_id += 1
        item = DriveFile(file_id, name, DRIVE_FOLDER_MIME_TYPE, "", "", (parent_id,))
        self.children.setdefault(parent_id, []).append(item)
        self.children.setdefault(file_id, [])
        self.created_folders.append((parent_id, name))
        return item

    def copy_file(self, source_file_id: str, target_folder_id: str, name: str) -> DriveFile:
        file_id = f"copy-{self.next_id}"
        self.next_id += 1
        item = DriveFile(file_id, name, "application/octet-stream", "", "", (target_folder_id,))
        self.children.setdefault(target_folder_id, []).append(item)
        self.copied_files.append((source_file_id, name, target_folder_id))
        return item


if __name__ == "__main__":
    unittest.main()
