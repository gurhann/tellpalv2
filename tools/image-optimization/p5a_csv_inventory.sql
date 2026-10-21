-- Her SELECT'i ayrı çalıştırıp sonucunu belirtilen adla CSV dışa aktarın.
-- Yalnız metadata okunur. Dört CSV aynı atomik snapshot iddiası taşımaz.

-- 1) hikayeler.csv — Her hikâye/dil bir satır; dil kaydı olmayan hikâye korunur.
SELECT statement_timestamp() AS observed_at,
       c.id AS content_id,
       c.external_key,
       c.is_active,
       c.page_count AS declared_page_count,
       c.textless_cover_media_id,
       c.listening_cover_media_id,
       l.id AS localization_id,
       l.language_code,
       l.title,
       l.cover_media_id AS reading_cover_media_id,
       l.status AS editorial_status,
       l.processing_status AS legacy_processing_status,
       l.published_at
FROM public.contents c
LEFT JOIN public.content_localizations l ON l.content_id = c.id
WHERE c.type = 'STORY'
ORDER BY c.id, l.language_code, l.id;

-- 2) sayfalar.csv — Kayıtlı sayfa/dil bağları; çevirisi olmayan sayfa da korunur.
SELECT statement_timestamp() AS observed_at,
       p.content_id,
       p.id AS page_id,
       p.page_number,
       p.textless_illustration_media_id,
       l.id AS page_localization_id,
       l.language_code,
       l.illustration_media_id
FROM public.story_pages p
JOIN public.contents c ON c.id = p.content_id
LEFT JOIN public.story_page_localizations l ON l.story_page_id = p.id
WHERE c.type = 'STORY'
ORDER BY p.content_id, p.page_number, l.language_code, l.id;

-- 3) medya.csv — Yukarıdaki STORY kaynaklarının referans verdiği medya kayıtları.
-- UNION yalnız ID'leri birleştirir; rol bağları ilk iki CSV'de korunur.
SELECT statement_timestamp() AS observed_at,
       a.id AS asset_id,
       a.provider,
       a.object_path,
       a.media_type,
       a.kind,
       a.mime_type,
       a.byte_size,
       a.checksum_sha256
FROM public.media_assets a
WHERE a.id IN (
    SELECT l.cover_media_id
    FROM public.content_localizations l
    JOIN public.contents c ON c.id = l.content_id
    WHERE c.type = 'STORY'
    UNION
    SELECT c.textless_cover_media_id FROM public.contents c WHERE c.type = 'STORY'
    UNION
    SELECT c.listening_cover_media_id FROM public.contents c WHERE c.type = 'STORY'
    UNION
    SELECT p.textless_illustration_media_id
    FROM public.story_pages p JOIN public.contents c ON c.id = p.content_id
    WHERE c.type = 'STORY'
    UNION
    SELECT l.illustration_media_id
    FROM public.story_page_localizations l
    JOIN public.story_pages p ON p.id = l.story_page_id
    JOIN public.contents c ON c.id = p.content_id
    WHERE c.type = 'STORY'
)
ORDER BY a.id;

-- 4) legacy_isler.csv — STORY iş durumları; hata mesajı veya lease/token alınmaz.
SELECT statement_timestamp() AS observed_at,
       j.id AS job_id,
       j.content_id,
       j.language_code,
       j.target_scope,
       j.processing_kind,
       j.cover_source_asset_id,
       j.status,
       j.attempt_count
FROM public.asset_processing j
JOIN public.contents c ON c.id = j.content_id
WHERE c.type = 'STORY'
ORDER BY j.content_id, j.id;
