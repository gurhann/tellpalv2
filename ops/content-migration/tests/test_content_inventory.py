import csv
import tempfile
import unittest
from pathlib import Path

import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import content_inventory as inventory


class ContentInventoryTests(unittest.TestCase):
    def test_resolves_lullaby_and_meditation_from_categories(self):
        stories = [
            inventory.StoryRow("1", "Traditional Lullaby", "", 1, "", "", 2, 1, inventory.normalize_key("Traditional Lullaby")),
            inventory.StoryRow("2", "A Mindful Sleep Story Meditation", "", 1, "", "", 8, 3, inventory.normalize_key("A Mindful Sleep Story Meditation")),
        ]
        categories = [
            inventory.CategoryRow("en", "Lullaby and Relaxing Music 😴", "LULLABY", inventory.normalize_key("Lullaby and Relaxing Music 😴")),
            inventory.CategoryRow("en", "Meditation 🧘🍃", "MEDITATION", inventory.normalize_key("Meditation 🧘🍃")),
        ]
        links = [
            inventory.StoryCategoryLink(
                "en",
                "Lullaby and Relaxing Music 😴",
                "Traditional Lullaby",
                inventory.normalize_key("Lullaby and Relaxing Music 😴"),
                inventory.normalize_key("Traditional Lullaby"),
            ),
            inventory.StoryCategoryLink(
                "en",
                "Meditation 🧘🍃",
                "A Mindful Sleep Story Meditation",
                inventory.normalize_key("Meditation 🧘🍃"),
                inventory.normalize_key("A Mindful Sleep Story Meditation"),
            ),
        ]

        resolutions, missing = inventory.resolve_content_types(stories, categories, links, {})

        self.assertEqual([], missing)
        self.assertEqual("LULLABY", resolutions[0].final_type)
        self.assertEqual("MEDITATION", resolutions[1].final_type)
        self.assertEqual("MATCHED", resolutions[0].status)

    def test_page_count_resolves_story_audio_story_conflict(self):
        story = inventory.StoryRow("10", "Sleepless Apartment", "", 18, "", "", 5, 3, inventory.normalize_key("Sleepless Apartment"))
        categories = [
            inventory.CategoryRow("en", "Audio Books 🎧", "AUDIO_STORY", inventory.normalize_key("Audio Books 🎧")),
            inventory.CategoryRow("en", "Sleep Stories", "STORY", inventory.normalize_key("Sleep Stories")),
        ]
        links = [
            inventory.StoryCategoryLink("en", "Audio Books 🎧", "Sleepless Apartment", inventory.normalize_key("Audio Books 🎧"), story.normalized_name),
            inventory.StoryCategoryLink("en", "Sleep Stories", "Sleepless Apartment", inventory.normalize_key("Sleep Stories"), story.normalized_name),
        ]

        resolutions, _ = inventory.resolve_content_types([story], categories, links, {})

        self.assertEqual("STORY", resolutions[0].final_type)
        self.assertEqual("MATCHED", resolutions[0].status)
        self.assertIn("resolved by page_count", "|".join(resolutions[0].notes))

    def test_page_count_one_audio_story_is_out_of_scope(self):
        story = inventory.StoryRow("11", "Cloudy Dreams", "", 1, "", "", 4, 3, inventory.normalize_key("Cloudy Dreams"))

        resolutions, _ = inventory.resolve_content_types([story], [], [], {})

        self.assertIsNone(resolutions[0].final_type)
        self.assertEqual("OUT_OF_SCOPE", resolutions[0].status)
        self.assertEqual("audio_story_scope_exclusion", resolutions[0].decision_rule)

    def test_audio_story_category_is_out_of_scope(self):
        story = inventory.StoryRow("12", "Cloudy Dreams", "", 1, "", "", 4, 3, inventory.normalize_key("Cloudy Dreams"))
        category = inventory.CategoryRow("en", "Audio Stories", "AUDIO_STORY", inventory.normalize_key("Audio Stories"))
        link = inventory.StoryCategoryLink("en", "Audio Stories", "Cloudy Dreams", category.normalized_name, story.normalized_name)

        resolutions, _ = inventory.resolve_content_types([story], [category], [link], {})

        self.assertIsNone(resolutions[0].final_type)
        self.assertEqual("OUT_OF_SCOPE", resolutions[0].status)
        self.assertEqual("audio_story_scope_exclusion", resolutions[0].decision_rule)

    def test_generates_reports_from_utf8_csvs(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            stories = root / "tellpal_stories.csv"
            categories = root / "categories.csv"
            links = root / "stories_and_categories.csv"
            output = root / "out"

            write_rows(
                stories,
                ["id", "name", "author", "page_count", "dubbing", "illustrator", "duration", "age_range"],
                [{"id": "1", "name": "Masalın İçinde", "author": "Ali", "page_count": "1", "dubbing": "", "illustrator": "", "duration": "1", "age_range": "1"}],
            )
            write_rows(
                categories,
                ["language", "name", "type"],
                [{"language": "tr", "name": "Ninni ve Dinlendirici Müzikler 😴", "type": "LULLABY"}],
            )
            write_rows(
                links,
                ["language", "category_name", "story_name"],
                [{"language": "tr", "category_name": "Ninni ve Dinlendirici Müzikler 😴", "story_name": "Masalın İçinde"}],
            )

            exit_code = inventory.main([
                "--stories",
                str(stories),
                "--categories",
                str(categories),
                "--story-categories",
                str(links),
                "--output-dir",
                str(output),
            ])

            self.assertEqual(0, exit_code)
            with (output / "v2_import_manifest.csv").open(encoding="utf-8") as handle:
                manifest = list(csv.DictReader(handle))
            self.assertEqual("LULLABY", manifest[0]["content_type"])
            self.assertEqual("Masalın İçinde", manifest[0]["title"])

    def test_exclusions_are_kept_out_of_import_manifest(self):
        story = inventory.StoryRow("343", "Os Super Detetives", "", 1, "", "", 1, 1, inventory.normalize_key("Os Super Detetives"))
        exclusions = {
            "343": inventory.ExclusionRow("343", "Os Super Detetives", "manual review later")
        }

        resolutions, _ = inventory.resolve_content_types([story], [], [], exclusions)

        self.assertEqual("OUT_OF_SCOPE", resolutions[0].status)
        self.assertIsNone(resolutions[0].final_type)

    def test_copy_plan_excludes_likely_matches_by_default(self):
        story = inventory.StoryRow("1", "Cloudy Dreams", "", 18, "", "", 4, 3, inventory.normalize_key("Cloudy Dreams"))
        resolution = inventory.TypeResolution(story=story, final_type="STORY", status="MATCHED")
        resolution.canonical_key_value = "story-cloudy-dreams"
        item = inventory.DriveItem(
            item_id="drive-1",
            title="Cloudy",
            mime_type="audio/mpeg",
            url="",
            path="TELLPAL-ORTAK/Audio/Cloudy Dreams draft.mp3",
            modified_time="",
            normalized_title=inventory.normalize_key("Cloudy"),
            normalized_path=inventory.normalize_key("TELLPAL-ORTAK/Audio/Cloudy Dreams draft.mp3"),
        )

        report, copy_plan = inventory.build_drive_matches([resolution], [item])
        _, copy_plan_with_likely = inventory.build_drive_matches([resolution], [item], include_likely_in_copy_plan=True)

        self.assertEqual("LIKELY", report[0]["status"])
        self.assertEqual([], copy_plan)
        self.assertEqual(1, len(copy_plan_with_likely))

    def test_copy_plan_expands_matched_folder_to_child_files(self):
        story = inventory.StoryRow("20", "Dinoco Nerede", "", 19, "", "", 1, 1, inventory.normalize_key("Dinoco Nerede"))
        resolution = inventory.TypeResolution(story=story, final_type="STORY", status="MATCHED")
        resolution.canonical_key_value = "story-dinoco-nerede"
        folder = inventory.DriveItem(
            item_id="folder-1",
            title="Dinoco Nerede",
            mime_type=inventory.DRIVE_FOLDER_MIME_TYPE,
            url="",
            path="TELLPAL-ORTAK/Seslendirme/Dinoco Nerede",
            modified_time="",
            normalized_title=inventory.normalize_key("Dinoco Nerede"),
            normalized_path=inventory.normalize_key("TELLPAL-ORTAK/Seslendirme/Dinoco Nerede"),
        )
        child = inventory.DriveItem(
            item_id="file-1",
            title="dinoco_nerede.docx",
            mime_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            url="",
            path="TELLPAL-ORTAK/Seslendirme/Dinoco Nerede/dinoco_nerede.docx",
            modified_time="",
            normalized_title=inventory.normalize_key("dinoco_nerede.docx"),
            normalized_path=inventory.normalize_key("TELLPAL-ORTAK/Seslendirme/Dinoco Nerede/dinoco_nerede.docx"),
        )

        _, copy_plan = inventory.build_drive_matches([resolution], [folder, child])

        self.assertEqual(1, len(copy_plan))
        self.assertEqual("file-1", copy_plan[0]["source_drive_id"])
        self.assertEqual("dinoco_nerede.docx", copy_plan[0]["source_title"])


def write_rows(path: Path, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    unittest.main()
