-- DataGrip/IntelliJ: bu tek SELECT sonucunu başlıklarla UTF-8 CSV olarak dışa aktarın.
-- Yalnız metadata okunur; dosya varlığı, piksel veya storage sürümü kanıtlanmaz.
WITH pilots(pilot_content_id, expected_external_key, language_code) AS (
    VALUES (18::bigint, 'story.kocaman-bir-sarilma', 'tr'),
           (62::bigint, 'story.pofuduk-misir-pops', 'tr')
), scopes AS (
    SELECT t.*, c.id AS content_id, c.external_key, c.type AS content_type,
           c.page_count AS declared_page_count, l.id AS localization_id, l.cover_media_id
    FROM pilots t
    LEFT JOIN public.contents c ON c.id = t.pilot_content_id
    LEFT JOIN public.content_localizations l
           ON l.content_id = c.id AND l.language_code = t.language_code
), bindings AS (
    SELECT s.*, r.role, NULL::integer AS page_number, NULL::bigint AS page_id,
           NULL::bigint AS page_localization_id, s.cover_media_id AS source_asset_id
    FROM scopes s
    CROSS JOIN (VALUES ('READING_COVER'), ('READING_DETAIL')) r(role)
    UNION ALL
    SELECT s.*, 'READING_PAGE', slots.page_number, p.id, pl.id, pl.illustration_media_id
    FROM scopes s
    -- Beyan edilmiş boşluklar ve beyan dışında kalmış gerçek sayfalar birlikte korunur.
    -- Placeholder üretimi pilot başına en çok 997 sayfadır; gerçek beyan değiştirilmez.
    -- Okuyucu 997 üstü beyanı açıkça reddeder. Gerçek sayfalara bu sınır uygulanmaz.
    LEFT JOIN LATERAL (
        SELECT generate_series(1, LEAST(COALESCE(s.declared_page_count, 0), 997)) AS page_number
        UNION
        SELECT actual.page_number FROM public.story_pages actual WHERE actual.content_id = s.content_id
    ) slots ON true
    LEFT JOIN public.story_pages p ON p.content_id = s.content_id AND p.page_number = slots.page_number
    LEFT JOIN public.story_page_localizations pl
           ON pl.story_page_id = p.id AND pl.language_code = s.language_code
)
SELECT statement_timestamp() AS observed_at,
       b.pilot_content_id, b.expected_external_key, b.language_code,
       b.content_id, b.external_key, b.content_type, b.declared_page_count,
       b.localization_id, b.role, b.page_number, b.page_id, b.page_localization_id,
       b.source_asset_id, a.id AS asset_id, a.provider, a.object_path,
       a.mime_type, a.byte_size, a.checksum_sha256
FROM bindings b
LEFT JOIN public.media_assets a ON a.id = b.source_asset_id
ORDER BY b.pilot_content_id,
         CASE b.role WHEN 'READING_COVER' THEN 0 WHEN 'READING_DETAIL' THEN 1 ELSE 2 END,
         b.page_number, b.page_id;
