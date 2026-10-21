-- Canlıda kullanıcı tarafından çalıştırılacak ilk P5a metadata dökümü.
-- Dosyanın tamamını aynı bağlantıda çalıştırın. Sonuç: tek inventory_json hücresi.
-- Önceden açık transaction bulunmayan yeni ve boş bir sorgu oturumu kullanın.
-- Uygulama verisi yazılmaz; ayarlar yalnız bu transaction boyunca geçerlidir.
-- Hata olursa aynı bağlantıda ROLLBACK çalıştırın. Eksik tablo/sütun uydurulmaz.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
-- İstemcinin açabildiği boş transaction için de modu açıkça belirt.
-- İlk veri sorgusundan önce uygulanamazsa hata verir; koruma kaldırılmaz.
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY;
SET LOCAL statement_timeout = '10s';
SET LOCAL lock_timeout = '1s';
SET LOCAL idle_in_transaction_session_timeout = '15s';
SET LOCAL search_path = pg_catalog, public;
-- Satır güvenliği kataloğu filtreleyecekse eksik sonuç yerine hata verilir.
SET LOCAL row_security = off;

WITH
stories AS MATERIALIZED (
    SELECT c.id, c.external_key, c.is_active, c.page_count,
           c.textless_cover_media_id, c.listening_cover_media_id
    FROM public.contents c
    WHERE c.type = 'STORY'
    ORDER BY c.id
    LIMIT 10001
),
localizations AS MATERIALIZED (
    SELECT l.id, l.content_id, l.language_code, l.title, l.cover_media_id,
           l.status, l.processing_status, l.published_at
    FROM public.content_localizations l
    JOIN stories c ON c.id = l.content_id
    ORDER BY l.content_id, l.language_code, l.id
    LIMIT 100001
),
pages AS MATERIALIZED (
    SELECT p.id, p.content_id, p.page_number, p.textless_illustration_media_id
    FROM public.story_pages p
    JOIN stories c ON c.id = p.content_id
    ORDER BY p.content_id, p.page_number, p.id
    LIMIT 100001
),
page_localizations AS MATERIALIZED (
    SELECT l.id, l.story_page_id, l.language_code, l.illustration_media_id
    FROM public.story_page_localizations l
    JOIN pages p ON p.id = l.story_page_id
    ORDER BY p.content_id, p.page_number, l.language_code, l.id
    LIMIT 100001
),
asset_ids AS MATERIALIZED (
    SELECT cover_media_id AS id FROM localizations
    UNION SELECT textless_cover_media_id FROM stories
    UNION SELECT listening_cover_media_id FROM stories
    UNION SELECT textless_illustration_media_id FROM pages
    UNION SELECT illustration_media_id FROM page_localizations
),
assets AS MATERIALIZED (
    SELECT a.id, a.provider, a.object_path, a.media_type, a.kind,
           a.mime_type, a.byte_size, a.checksum_sha256
    FROM public.media_assets a
    JOIN asset_ids r ON r.id = a.id
    ORDER BY a.id
    LIMIT 100001
),
legacy_jobs AS MATERIALIZED (
    SELECT j.id, j.content_id, j.language_code, j.status, j.attempt_count,
           j.target_scope, j.processing_kind, j.cover_source_asset_id
    FROM public.asset_processing j
    JOIN stories c ON c.id = j.content_id
    ORDER BY j.content_id, j.id
    LIMIT 100001
),
optional_tables(table_name) AS (
    VALUES ('listing_cover_revisions'), ('story_page_media_manifests'),
           ('story_page_media_manifest_entries'), ('reading_image_jobs'),
           ('reading_image_manifests'), ('reading_source_admissions'),
           ('reading_publications'), ('reading_publication_availability'),
           ('reading_image_integrity'), ('reading_image_attempt_evidence'),
           ('reading_image_source_captures')
),
schema_info AS MATERIALIZED (
    SELECT c.table_name, c.column_name, c.data_type, c.udt_name, c.ordinal_position
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name IN (
          'contents', 'content_localizations', 'story_pages',
          'story_page_localizations', 'media_assets', 'asset_processing',
          'listing_cover_revisions', 'story_page_media_manifests',
          'story_page_media_manifest_entries', 'reading_image_jobs',
          'reading_image_manifests', 'reading_source_admissions',
          'reading_publications', 'reading_publication_availability',
          'reading_image_integrity', 'reading_image_attempt_evidence',
          'reading_image_source_captures'
      )
),
counts AS MATERIALIZED (
    SELECT (SELECT count(*) FROM stories) AS stories,
           (SELECT count(*) FROM localizations) AS localizations,
           (SELECT count(*) FROM pages) AS pages,
           (SELECT count(*) FROM page_localizations) AS page_localizations,
           (SELECT count(*) FROM assets) AS assets,
           (SELECT count(*) FROM legacy_jobs) AS legacy_jobs,
           (SELECT count(*) * 2 FROM localizations) +
           (SELECT count(*) FROM pages p JOIN localizations l
            ON l.content_id = p.content_id) AS required_reading_bindings,
           coalesce((SELECT sum(octet_length(to_jsonb(s)::text)) FROM stories s), 0) +
           coalesce((SELECT sum(octet_length(to_jsonb(l)::text)) FROM localizations l), 0) +
           coalesce((SELECT sum(octet_length(to_jsonb(p)::text)) FROM pages p), 0) +
           coalesce((SELECT sum(octet_length(to_jsonb(l)::text)) FROM page_localizations l), 0) +
           coalesce((SELECT sum(octet_length(to_jsonb(a)::text)) FROM assets a), 0) +
           coalesce((SELECT sum(octet_length(to_jsonb(j)::text)) FROM legacy_jobs j), 0)
           AS metadata_bytes
),
guard AS MATERIALIZED (
    SELECT current_setting('transaction_read_only') = 'on'
       AND current_setting('transaction_isolation') = 'repeatable read'
       AND stories <= 10000 AND localizations <= 100000 AND pages <= 100000
       AND page_localizations <= 100000 AND assets <= 100000
       AND legacy_jobs <= 100000 AND required_reading_bindings <= 100000
       -- Büyük JSON birleştirilmeden önce 60 MiB; zarf/şema için ayrıca pay kalır.
       AND metadata_bytes <= 62914560 AS ok
    FROM counts
),
report AS MATERIALIZED (
    SELECT jsonb_build_object(
        'schemaVersion', 1,
        'scope', 'PRODUCTION_STORY_DATABASE_METADATA_ONLY',
        'queryVersion', 'p5a-manual-core-v2',
        'snapshotAt', transaction_timestamp(),
        'snapshot', pg_current_snapshot()::text,
        'database', current_database(),
        'readOnly', current_setting('transaction_read_only'),
        'isolation', current_setting('transaction_isolation'),
        'coreComplete', g.ok,
        'complete', false,
        'storageVerified', false,
        'executable', false,
        'status', CASE WHEN g.ok THEN 'CORE_EXPORTED_SUPPLEMENTAL_PENDING'
                       ELSE 'LIMIT_OR_TRANSACTION_REJECTED' END,
        'counts', (SELECT to_jsonb(n) FROM counts n),
        'schema', (SELECT coalesce(jsonb_agg(to_jsonb(s)
                   ORDER BY s.table_name, s.ordinal_position), '[]'::jsonb)
                   FROM schema_info s),
        'supplemental', (SELECT jsonb_object_agg(t.table_name,
            CASE WHEN to_regclass(format('public.%I', t.table_name)) IS NULL
                 THEN 'NOT_DEPLOYED' ELSE 'NOT_COLLECTED' END) FROM optional_tables t),
        'stories', CASE WHEN g.ok THEN
            (SELECT coalesce(jsonb_agg(to_jsonb(s) ORDER BY s.id), '[]'::jsonb) FROM stories s) END,
        'localizations', CASE WHEN g.ok THEN
            (SELECT coalesce(jsonb_agg(to_jsonb(l) ORDER BY l.content_id, l.language_code, l.id),
                                     '[]'::jsonb) FROM localizations l) END,
        'pages', CASE WHEN g.ok THEN
            (SELECT coalesce(jsonb_agg(to_jsonb(p) ORDER BY p.content_id, p.page_number, p.id),
                                     '[]'::jsonb) FROM pages p) END,
        'pageLocalizations', CASE WHEN g.ok THEN
            (SELECT coalesce(jsonb_agg(to_jsonb(l) ORDER BY l.story_page_id, l.language_code, l.id),
                                     '[]'::jsonb) FROM page_localizations l) END,
        'assets', CASE WHEN g.ok THEN
            (SELECT coalesce(jsonb_agg(to_jsonb(a) ORDER BY a.id), '[]'::jsonb) FROM assets a) END,
        'legacyJobs', CASE WHEN g.ok THEN
            (SELECT coalesce(jsonb_agg(to_jsonb(j) ORDER BY j.content_id, j.id), '[]'::jsonb)
             FROM legacy_jobs j) END
    ) AS body
    FROM guard g
)
SELECT CASE WHEN octet_length(body::text) <= 67108864 THEN body
            ELSE jsonb_build_object('schemaVersion', 1, 'coreComplete', false,
                 'complete', false, 'executable', false, 'status', 'RESULT_SIZE_LIMIT')
       END AS inventory_json
FROM report;

ROLLBACK;
