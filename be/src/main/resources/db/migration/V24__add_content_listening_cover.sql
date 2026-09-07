DO $$
DECLARE
    legacy_content_ids text;
BEGIN
    SELECT string_agg(id::text, ', ' ORDER BY id)
      INTO legacy_content_ids
      FROM contents
     WHERE type <> 'STORY'
       AND textless_cover_media_id IS NOT NULL;

    IF legacy_content_ids IS NOT NULL THEN
        RAISE EXCEPTION
            'V24 blocked: non-STORY contents [%] still have textless_cover_media_id; resolve legacy cover ownership before applying this migration',
            legacy_content_ids;
    END IF;
END $$;

ALTER TABLE contents
    ADD COLUMN listening_cover_media_id bigint;

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_listening_cover_media_id_positive
        CHECK (listening_cover_media_id IS NULL OR listening_cover_media_id > 0);

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_textless_cover_media_id_content_type
        CHECK (textless_cover_media_id IS NULL OR type = 'STORY');

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_listening_cover_media_id_content_type
        CHECK (listening_cover_media_id IS NULL OR type IN ('STORY', 'MEDITATION', 'LULLABY'));

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_cover_media_ids_distinct
        CHECK (
            textless_cover_media_id IS NULL
                OR listening_cover_media_id IS NULL
                OR textless_cover_media_id <> listening_cover_media_id
        );

CREATE INDEX idx_contents_listening_cover_media_id
    ON contents (listening_cover_media_id)
    WHERE listening_cover_media_id IS NOT NULL;
