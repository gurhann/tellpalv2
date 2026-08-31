from __future__ import annotations

import csv
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ElementTree
import zipfile
from pathlib import Path


SCRIPTS_DIRECTORY = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS_DIRECTORY))

from story_manifest import (  # noqa: E402
    _metadata_title_in_docx_filename,
    StoryValidationError,
    build_story_plan,
    extract_docx_pages,
    slugify_turkish_title,
)
from story_import_workflow import format_preview  # noqa: E402
from tellpal_admin_client import media_mime_type  # noqa: E402


class StoryManifestTest(unittest.TestCase):
    def test_matches_metadata_title_inside_descriptive_docx_filename(self) -> None:
        self.assertTrue(
            _metadata_title_in_docx_filename(
                "Feliz por Viver",
                Path("Feliz por Viver - Brazilian Portuguese (Horieber Oliveira).docx"),
            )
        )
        self.assertTrue(_metadata_title_in_docx_filename("İyi Ki Varım", Path("İyi ki Varım.docx")))
        self.assertFalse(_metadata_title_in_docx_filename("İyi Ki Varım", Path("Other story.docx")))

    def test_uses_image_gif_mime_type_for_gif_uploads(self) -> None:
        self.assertEqual(media_mime_type(Path("page.gif")), "image/gif")

    def test_slugifies_turkish_title_and_apostrophe(self) -> None:
        self.assertEqual(slugify_turkish_title("Bubi’nin Zorlu Seçimi"), "bubinin-zorlu-secimi")

    def test_extracts_supported_docx_page_marker_variants(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            path = Path(temporary_directory) / "story.docx"
            write_docx(
                path,
                (
                    "Örnek Hikâye",
                    "1.",
                    "Birinci sayfa",
                    "2.İkinci sayfa",
                    "3 Üçüncü sayfa",
                    "4\nDördüncü sayfanın ilk satırı\nİkinci satırı",
                ),
            )

            title, pages = extract_docx_pages(path, 4)

        self.assertEqual(title, "Örnek Hikâye")
        self.assertEqual(
            pages,
            {
                1: "Birinci sayfa",
                2: "İkinci sayfa",
                3: "Üçüncü sayfa",
                4: "Dördüncü sayfanın ilk satırı\nİkinci satırı",
            },
        )

    def test_extracts_pages_from_exact_blank_separated_blocks(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            path = Path(temporary_directory) / "story.docx"
            write_docx(
                path,
                (
                    "Örnek Hikâye",
                    "Birinci sayfanın ilk paragrafı",
                    "İkinci paragrafı",
                    "",
                    "İkinci sayfa",
                    "",
                    "Üçüncü sayfa",
                ),
            )

            title, pages = extract_docx_pages(path, 3)

        self.assertEqual(title, "Örnek Hikâye")
        self.assertEqual(
            pages,
            {
                1: "Birinci sayfanın ilk paragrafı\nİkinci paragrafı",
                2: "İkinci sayfa",
                3: "Üçüncü sayfa",
            },
        )

    def test_builds_complete_plan_and_warns_for_extra_files(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)

            plan = build_story_plan(root)

        self.assertEqual(plan.external_key, "story.ornek-hikaye")
        self.assertEqual(plan.page_count, 2)
        self.assertEqual(plan.expected_actions["media_uploads"], 8)
        self.assertEqual(plan.expected_actions["page_localizations"], 2)
        self.assertEqual(plan.expected_actions["contributor_assignments"], 3)
        self.assertEqual(plan.localizations[0].cover_path, "tr/KAPAK.JPG")
        self.assertIn(
            "Ignored for upload but included in source fingerprint: tr/note.txt",
            plan.warnings,
        )
        self.assertEqual({item.language_code for item in plan.contributor_assignments}, {None})

    def test_explicitly_allows_empty_textless_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            textless = root / "yazisiz"
            for path in textless.iterdir():
                path.unlink()

            with self.assertRaisesRegex(StoryValidationError, "kapak/cover image"):
                build_story_plan(root)
            plan = build_story_plan(root, allow_missing_textless=True)
            preview = format_preview(plan)

        self.assertIsNone(plan.textless_cover_path)
        self.assertEqual(plan.textless_page_paths, ())
        self.assertEqual(plan.expected_actions["media_uploads"], 5)
        self.assertIn("Textless attachments: 0 (explicitly skipped)", preview)
        self.assertTrue(any("Explicitly allowed missing textless media" in item for item in plan.warnings))

    def test_missing_textless_opt_in_rejects_partially_populated_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root)
            textless = root / "yazisiz"
            (textless / "1.png").unlink()

            with self.assertRaisesRegex(StoryValidationError, "only when .* directory is empty"):
                build_story_plan(root, allow_missing_textless=True)

    def test_missing_textless_opt_in_rejects_page_overrides(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root)

            with self.assertRaisesRegex(StoryValidationError, "cannot be combined"):
                build_story_plan(
                    root,
                    allow_missing_textless=True,
                    textless_page_overrides={1: "yazisiz/1.png"},
                )

    def test_overrides_localization_description_without_changing_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)

            plan = build_story_plan(root, description_overrides={"tr": None})

        self.assertIsNone(plan.localizations[0].description)
        self.assertTrue(any("Explicit description override: tr" in item for item in plan.warnings))

    def test_rejects_description_override_for_unavailable_language(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)

            with self.assertRaisesRegex(StoryValidationError, "unavailable languages"):
                build_story_plan(root, description_overrides={"de": None})

    def test_explicit_metadata_ids_disambiguate_equal_localization_titles(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            create_second_language_with_duplicate_title(root, page_count=2)

            with self.assertRaisesRegex(StoryValidationError, "found 2 matches"):
                build_story_plan(root)

            plan = build_story_plan(
                root,
                metadata_row_overrides={"tr": "42", "en": "43"},
            )

        self.assertEqual(
            [(item.language_code, item.title) for item in plan.localizations],
            [("tr", "Örnek Hikâye"), ("en", "Örnek Hikâye")],
        )
        self.assertEqual(
            sum("Explicit metadata row override" in warning for warning in plan.warnings),
            2,
        )

    def test_matches_one_titleless_docx_to_the_only_remaining_metadata_row(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            write_docx(
                root / "tr" / "story.docx",
                ("Birinci sayfa", "", "İkinci sayfa"),
            )

            plan = build_story_plan(root)

        self.assertEqual(plan.localizations[0].title, "Örnek Hikâye")
        self.assertEqual(
            [page.body_text for page in plan.localizations[0].pages],
            ["Birinci sayfa", "İkinci sayfa"],
        )
        self.assertTrue(any("sole remaining row" in warning for warning in plan.warnings))

    def test_extracts_titleless_docx_with_one_paragraph_per_page(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=3)
            write_docx(
                root / "tr" / "story.docx",
                ("Birinci sayfa", "\t", "İkinci sayfa", "Üçüncü sayfa"),
            )

            plan = build_story_plan(root)

        self.assertEqual(
            [page.body_text for page in plan.localizations[0].pages],
            ["Birinci sayfa", "İkinci sayfa", "Üçüncü sayfa"],
        )

    def test_extracts_titleless_marked_pages_with_unmarked_continuations(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            write_docx(
                root / "tr" / "story.docx",
                ("1. Birinci başlangıç", "Birinci devam", "2. İkinci başlangıç", "İkinci devam"),
            )

            plan = build_story_plan(root)

        self.assertEqual(
            [page.body_text for page in plan.localizations[0].pages],
            ["Birinci başlangıç\nBirinci devam", "İkinci başlangıç\nİkinci devam"],
        )

    def test_explicit_metadata_row_parses_markers_starting_at_first_docx_paragraph(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            write_docx(root / "tr" / "story.docx", ("1. Birinci sayfa", "2. İkinci sayfa"))

            plan = build_story_plan(root, metadata_row_overrides={"tr": "42"})

        self.assertEqual(
            [page.body_text for page in plan.localizations[0].pages],
            ["Birinci sayfa", "İkinci sayfa"],
        )

    def test_explicit_metadata_row_parses_automatic_word_list_numbers(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            write_numbered_docx(root / "tr" / "story.docx", ("Birinci sayfa", "İkinci sayfa"))

            plan = build_story_plan(root, metadata_row_overrides={"tr": "42"})

        self.assertEqual(
            [page.body_text for page in plan.localizations[0].pages],
            ["Birinci sayfa", "İkinci sayfa"],
        )

    def test_fails_when_image_signature_is_invalid(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root)
            (root / "tr" / "1.jpeg").write_bytes(b"not-an-image")

            with self.assertRaisesRegex(StoryValidationError, "JPEG signature is invalid"):
                build_story_plan(root)

    def test_fails_when_audio_is_empty(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root)
            (root / "tr" / "1.mp3").write_bytes(b"")

            with self.assertRaisesRegex(StoryValidationError, "Media file is empty"):
                build_story_plan(root)

    def test_fails_when_numbered_media_is_missing(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            (root / "tr" / "2.mp3").unlink()

            with self.assertRaisesRegex(StoryValidationError, "audio files must cover"):
                build_story_plan(root)

    def test_fails_when_numbered_media_is_duplicated(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root)
            (root / "tr" / "1.png").write_bytes(png_bytes(b"duplicate"))

            with self.assertRaisesRegex(StoryValidationError, "Ambiguous tr page 1 illustration"):
                build_story_plan(root)

    def test_accepts_descriptive_cover_numbered_gif_and_double_dot_page_names(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            language = root / "tr"
            textless = root / "yazisiz"
            (language / "KAPAK.JPG").unlink()
            (language / "Kapak revize.gif").write_bytes(gif_bytes(b"cover"))
            (language / "1.jpeg").unlink()
            (language / "1_-_Hareketli.gif").write_bytes(gif_bytes(b"page-1"))
            (language / "2.jpeg").rename(language / "2..jpeg")
            (textless / "1.png").rename(textless / "1_-_Hareketli.png")

            plan = build_story_plan(root)

        localization = plan.localizations[0]
        self.assertEqual(localization.cover_path, "tr/Kapak revize.gif")
        self.assertEqual(localization.pages[0].illustration_path, "tr/1_-_Hareketli.gif")
        self.assertEqual(localization.pages[1].illustration_path, "tr/2..jpeg")
        self.assertEqual(plan.textless_page_paths[0], "yazisiz/1_-_Hareketli.png")

    def test_prefers_revision_candidate_over_other_decorated_page_name(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root)
            language = root / "tr"
            (language / "1.jpeg").unlink()
            (language / "1_-_Hareketli.gif").write_bytes(gif_bytes(b"animated"))
            (language / "1 revize.gif").write_bytes(gif_bytes(b"revised"))

            plan = build_story_plan(root)

        self.assertEqual(plan.localizations[0].pages[0].illustration_path, "tr/1 revize.gif")
        self.assertTrue(any("ignored lower-priority candidate" in warning for warning in plan.warnings))

    def test_applies_explicit_textless_page_overrides_without_changing_source(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)
            textless = root / "yazisiz"
            (textless / "1.png").rename(textless / "IMG_3610.png")

            plan = build_story_plan(
                root,
                textless_page_overrides={
                    1: "yazisiz/2.png",
                    2: "yazisiz/IMG_3610.png",
                },
            )

        self.assertEqual(plan.textless_page_paths, ("yazisiz/2.png", "yazisiz/IMG_3610.png"))
        self.assertEqual(
            sum("Explicit textless page override" in warning for warning in plan.warnings),
            2,
        )

    def test_rejects_reusing_one_textless_override_file(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)

            with self.assertRaisesRegex(StoryValidationError, "cannot be assigned to multiple pages"):
                build_story_plan(
                    root,
                    textless_page_overrides={1: "yazisiz/2.png"},
                )

    def test_preview_shows_localization_and_write_breakdown(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            create_story_fixture(root, page_count=2)

            preview = format_preview(build_story_plan(root))

        self.assertIn("Age range: 5", preview)
        self.assertIn("Content localizations: 1", preview)
        self.assertIn("Story pages: 2", preview)
        self.assertIn("Textless attachments: 3 (1 cover + 2 pages)", preview)
        self.assertIn("title='Örnek Hikâye', duration=4", preview)
        self.assertIn("Fixed write calls excluding contributor creates:", preview)

    def test_repository_sample_folder_matches_expected_counts(self) -> None:
        repository_root = Path(__file__).resolve().parents[5]
        sample = repository_root / "cms" / "yuklenecek_hikayeler" / "bubinin zorlu seçimi"
        if not sample.is_dir():
            self.skipTest("Repository sample story folder is not available")

        plan = build_story_plan(sample)

        self.assertEqual(len(plan.localizations), 4)
        self.assertEqual(plan.page_count, 16)
        self.assertEqual(plan.expected_actions["media_uploads"], 149)
        self.assertEqual(plan.expected_actions["page_localizations"], 64)
        self.assertEqual(plan.expected_actions["contributor_assignments"], 9)
        self.assertEqual(plan.expected_actions["publications"], 4)


def create_story_fixture(root: Path, *, page_count: int = 1) -> None:
    language = root / "tr"
    textless = root / "yazisiz"
    language.mkdir(parents=True)
    textless.mkdir()

    with (root / "metadata.csv").open("w", encoding="utf-8", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(
            [
                "id",
                "is_publish",
                "name",
                "summary",
                "page_count",
                "author",
                "dubbing",
                "illustrator",
                "duration",
                "age_range",
            ]
        )
        writer.writerow(
            ["42", "1", "Örnek Hikâye", "Satır 1\nSatır 2", page_count, "Yazar", "Anlatıcı", "Çizer", 4, 5]
        )

    write_docx(
        language / "story.docx",
        ["Örnek Hikâye"]
        + [f"{page_number}. Sayfa {page_number} metni" for page_number in range(1, page_count + 1)],
    )

    (language / "KAPAK.JPG").write_bytes(jpeg_bytes(b"localized-cover"))
    (language / "note.txt").write_text("ignored", encoding="utf-8")
    (textless / "kapak.png").write_bytes(png_bytes(b"textless-cover"))
    for page_number in range(1, page_count + 1):
        payload = str(page_number).encode()
        (language / f"{page_number}.jpeg").write_bytes(jpeg_bytes(payload))
        (language / f"{page_number}.mp3").write_bytes(b"ID3" + payload)
        (textless / f"{page_number}.png").write_bytes(png_bytes(payload))


def create_second_language_with_duplicate_title(root: Path, *, page_count: int) -> None:
    language = root / "en"
    language.mkdir()
    with (root / "metadata.csv").open("a", encoding="utf-8", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(
            ["43", "1", "Örnek Hikâye", "English summary", page_count, "Yazar", "Anlatıcı", "Çizer", 4, 5]
        )
    write_docx(
        language / "story.docx",
        ["Örnek Hikâye"]
        + [f"{page_number}. English page {page_number}" for page_number in range(1, page_count + 1)],
    )
    (language / "KAPAK.JPG").write_bytes(jpeg_bytes(b"english-cover"))
    for page_number in range(1, page_count + 1):
        payload = f"en-{page_number}".encode()
        (language / f"{page_number}.jpeg").write_bytes(jpeg_bytes(payload))
        (language / f"{page_number}.mp3").write_bytes(b"ID3" + payload)


def jpeg_bytes(payload: bytes) -> bytes:
    return b"\xff\xd8\xff" + payload


def png_bytes(payload: bytes) -> bytes:
    return b"\x89PNG\r\n\x1a\n" + payload


def gif_bytes(payload: bytes) -> bytes:
    return b"GIF89a" + payload


def write_docx(path: Path, paragraphs: list[str] | tuple[str, ...]) -> None:
    word_namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    document = ElementTree.Element(f"{{{word_namespace}}}document")
    body = ElementTree.SubElement(document, f"{{{word_namespace}}}body")
    for paragraph_text in paragraphs:
        paragraph = ElementTree.SubElement(body, f"{{{word_namespace}}}p")
        run = ElementTree.SubElement(paragraph, f"{{{word_namespace}}}r")
        text = ElementTree.SubElement(run, f"{{{word_namespace}}}t")
        text.text = paragraph_text
    xml = ElementTree.tostring(document, encoding="utf-8", xml_declaration=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("word/document.xml", xml)


def write_numbered_docx(path: Path, paragraphs: tuple[str, ...]) -> None:
    word_namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    qualified = lambda name: f"{{{word_namespace}}}{name}"
    document = ElementTree.Element(qualified("document"))
    body = ElementTree.SubElement(document, qualified("body"))
    for paragraph_text in paragraphs:
        paragraph = ElementTree.SubElement(body, qualified("p"))
        properties = ElementTree.SubElement(paragraph, qualified("pPr"))
        numbering = ElementTree.SubElement(properties, qualified("numPr"))
        ElementTree.SubElement(numbering, qualified("ilvl"), {qualified("val"): "0"})
        ElementTree.SubElement(numbering, qualified("numId"), {qualified("val"): "1"})
        run = ElementTree.SubElement(paragraph, qualified("r"))
        ElementTree.SubElement(run, qualified("t")).text = paragraph_text

    numbering_root = ElementTree.Element(qualified("numbering"))
    abstract = ElementTree.SubElement(
        numbering_root, qualified("abstractNum"), {qualified("abstractNumId"): "0"}
    )
    level = ElementTree.SubElement(abstract, qualified("lvl"), {qualified("ilvl"): "0"})
    ElementTree.SubElement(level, qualified("start"), {qualified("val"): "1"})
    ElementTree.SubElement(level, qualified("numFmt"), {qualified("val"): "decimal"})
    instance = ElementTree.SubElement(numbering_root, qualified("num"), {qualified("numId"): "1"})
    ElementTree.SubElement(instance, qualified("abstractNumId"), {qualified("val"): "0"})

    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(
            "word/document.xml",
            ElementTree.tostring(document, encoding="utf-8", xml_declaration=True),
        )
        archive.writestr(
            "word/numbering.xml",
            ElementTree.tostring(numbering_root, encoding="utf-8", xml_declaration=True),
        )


if __name__ == "__main__":
    unittest.main()
