"""Mevcut migration'larla geçici ve TCP kapalı PostgreSQL SELECT sınaması."""
import csv
from hashlib import sha256
from io import BytesIO, StringIO
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

from PIL import Image

from p5b_local_source_preflight import run_preflight

ROOT = Path(__file__).resolve().parents[2]
PG_BIN = os.environ.get("P5B_POSTGRES_BIN")


@unittest.skipUnless(PG_BIN, "Yerel PostgreSQL için P5B_POSTGRES_BIN gerekir; canlı DSN kabul edilmez.")
class PilotSqlTest(unittest.TestCase):
    def command(self, name, *args, sql=None):
        result = subprocess.run([str(Path(PG_BIN) / name), *map(str, args)], input=sql,
                                capture_output=True, text=True, timeout=45,
                                env={key: value for key, value in os.environ.items()
                                     if not key.startswith(("PG", "PSQL"))})
        self.assertEqual(0, result.returncode, result.stderr[-3000:])
        return result.stdout

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="p5b-sql-", dir="/private/tmp")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.data = self.root / "data"
        self.command("initdb", "-D", self.data, "--auth=trust", "--no-locale", "--encoding=UTF8", "-U", "p5b_test")
        self.command("pg_ctl", "-D", self.data, "-l", self.root / "server.log", "-o",
                     f"-c listen_addresses='' -c unix_socket_directories={self.root} -p 55439", "-w", "start")
        self.addCleanup(lambda: self.command("pg_ctl", "-D", self.data, "-m", "immediate", "-w", "stop"))
        migrations = sorted((ROOT / "be/src/main/resources/db/migration").glob("V*__*.sql"),
                            key=lambda path: int(re.match(r"V([0-9]+)", path.name)[1]))
        for migration in migrations:
            self.sql(migration.read_text())
        self.sql("""INSERT INTO contents(id,type,external_key,page_count) VALUES
(18,'STORY','story.kocaman-bir-sarilma',0),(62,'STORY','story.pofuduk-misir-pops',0);
INSERT INTO content_localizations(id,content_id,language_code,title,cover_media_id)
VALUES (18,18,'tr','Sentetik 18',1800),(62,62,'tr','Sentetik 62',6200);
INSERT INTO story_pages(id,content_id,page_number)
SELECT 1800+n,18,n FROM generate_series(1,16) n UNION ALL
SELECT 6200+n,62,n FROM generate_series(1,23) n;
INSERT INTO story_page_localizations(story_page_id,language_code,illustration_media_id)
SELECT id,'tr',id FROM story_pages;
INSERT INTO media_assets(id,provider,object_path,media_type,kind,mime_type,byte_size,checksum_sha256)
SELECT n,'SYNTHETIC','synthetic/'||n,'IMAGE','ORIGINAL_IMAGE','image/png',100,repeat('a',64)
FROM (SELECT 1800+n AS n FROM generate_series(0,16) n UNION ALL
      SELECT 6200+n FROM generate_series(0,23) n) ids;
""")
        self.query = (ROOT / "tools/image-optimization/p5b_pilot_sources.sql").read_text()

    def sql(self, text):
        return self.command("psql", "-X", "-q", "-v", "ON_ERROR_STOP=1", "-h", self.root,
                            "-p", "55439", "-U", "p5b_test", "-d", "postgres", "--csv", sql=text)

    def rows(self, mutation=""):
        # Mutation yalnız bu testin yeni, geçici veritabanında gerçekleşir.
        return list(csv.DictReader(StringIO(self.sql("BEGIN;" + mutation + self.query + "ROLLBACK;"))))

    def test_select_preserves_complete_and_missing_bindings(self):
        rows = self.rows()
        self.assertEqual(43, len(rows))
        self.assertEqual(41, len({row["asset_id"] for row in rows}))
        self.assertEqual(1, len({row["observed_at"] for row in rows}))
        self.assertEqual(43, len(self.rows("SET TRANSACTION READ ONLY;")))
        rows = self.rows("DELETE FROM contents WHERE id=18;")
        missing = [row for row in rows if row["pilot_content_id"] == "18"]
        self.assertEqual(3, len(missing))
        self.assertTrue(all(row["content_id"] == "" for row in missing))
        rows = self.rows("DELETE FROM content_localizations WHERE content_id=18;")
        self.assertEqual(18, sum(row["localization_id"] == "" for row in rows))
        rows = self.rows("DELETE FROM story_pages WHERE id=1802; UPDATE contents SET page_count=16 WHERE id=18;")
        gap = next(row for row in rows if row["pilot_content_id"] == "18" and row["page_number"] == "2")
        self.assertEqual("", gap["page_id"])
        rows = self.rows("DELETE FROM story_page_localizations WHERE story_page_id=1802;")
        gap = next(row for row in rows if row["page_id"] == "1802")
        self.assertEqual("", gap["page_localization_id"])
        rows = self.rows("UPDATE content_localizations SET cover_media_id=NULL WHERE id=18;")
        self.assertEqual(2, sum(row["source_asset_id"] == "" for row in rows))
        rows = self.rows("DELETE FROM media_assets WHERE id=1800;")
        self.assertEqual(2, sum(row["source_asset_id"] == "1800" and row["asset_id"] == "" for row in rows))
        rows = self.rows("UPDATE contents SET page_count=15 WHERE id=18;")
        self.assertTrue(any(row["page_number"] == "16" and row["page_id"] == "1816" for row in rows))

    def test_large_declaration_is_bounded_and_real_pages_remain_visible(self):
        mutation = """INSERT INTO story_pages(id,content_id,page_number) VALUES (9999,18,1001);
UPDATE contents SET page_count=2147483647 WHERE id=18;"""
        rows = self.rows(mutation)
        pilot = [row for row in rows if row["pilot_content_id"] == "18"]
        self.assertEqual(1025, len(rows))
        self.assertEqual(1000, len(pilot))
        self.assertEqual({"2147483647"}, {row["declared_page_count"] for row in pilot})
        self.assertTrue(any(row["page_number"] == "1001" and row["page_id"] == "9999" for row in pilot))
        csv_path = self.root / "large.csv"
        csv_path.write_text(self.sql("BEGIN;" + mutation + self.query + "ROLLBACK;"))
        mapping_path = self.root / "mapping.json"
        mapping_path.write_text("{}")
        report = run_preflight(csv_path, mapping_path)
        self.assertEqual("BLOCKED", report["status"])
        self.assertEqual([{"code": "DECLARED_PAGE_COUNT_LIMIT"}], report["errors"])

    def test_actual_select_output_reaches_preflight_with_exact_media_identity(self):
        assets = sorted({int(row["asset_id"]) for row in self.rows()})
        mapping, expected, updates = {}, {}, []
        for asset in assets:
            fmt = "JPEG" if asset < 6200 else "PNG"
            mime = "image/jpeg" if fmt == "JPEG" else "image/png"
            output = BytesIO()
            with Image.new("RGB", (8, 6), (asset % 256, 40, 100)) as image:
                image.save(output, format=fmt)
            data = output.getvalue()
            path = self.root / f"{asset}.original"
            path.write_bytes(data)
            mapping[str(asset)] = str(path)
            digest = sha256(data).hexdigest()
            expected[asset] = {"bytes": len(data), "sha256": digest, "mime": mime}
            updates.append(f"UPDATE media_assets SET mime_type='{mime}', byte_size={len(data)}, "
                           f"checksum_sha256='{digest}' WHERE id={asset};")
        self.sql("\n".join(updates))
        csv_path = self.root / "selected.csv"
        csv_path.write_text(self.sql("BEGIN READ ONLY;" + self.query + "COMMIT;"))
        mapping_path = self.root / "mapping.json"
        mapping_path.write_text(json.dumps(mapping))
        report = run_preflight(csv_path, mapping_path)
        self.assertEqual("LOCAL_OBSERVATIONS_COMPLETE", report["status"], report["errors"])
        self.assertEqual(43, len(report["roles"]))
        self.assertEqual(41, len(report["sources"]))
        for source in report["sources"]:
            identity = expected[source["assetId"]]
            self.assertEqual(identity, source["expected"])
            self.assertEqual(identity["bytes"], source["observed"]["bytes"])
            self.assertEqual(identity["sha256"], source["observed"]["sha256"])
            self.assertEqual(identity["mime"], source["observed"]["image"]["mime"])
            for match in ("byteMatch", "sha256Match", "mimeMatch"):
                self.assertIs(True, source[match])
            self.assertEqual(identity["sha256"], sha256(Path(mapping[str(source["assetId"])]).read_bytes()).hexdigest())
        self.assertEqual("NOT_EVALUATED", report["p2Result"])
        self.assertEqual("NOT_ESTABLISHED", report["p5bCompletion"])


if __name__ == "__main__":
    unittest.main()
