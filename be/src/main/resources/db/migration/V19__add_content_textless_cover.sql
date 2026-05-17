ALTER TABLE contents
    ADD COLUMN textless_cover_media_id bigint;

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_textless_cover_media_id_positive
        CHECK (textless_cover_media_id IS NULL OR textless_cover_media_id > 0);

CREATE INDEX idx_contents_textless_cover_media_id
    ON contents (textless_cover_media_id)
    WHERE textless_cover_media_id IS NOT NULL;
