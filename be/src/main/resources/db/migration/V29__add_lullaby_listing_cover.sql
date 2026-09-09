ALTER TABLE contents
    ADD COLUMN listing_cover_media_id bigint;

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_listing_cover_media_id_positive
        CHECK (listing_cover_media_id IS NULL OR listing_cover_media_id > 0),
    ADD CONSTRAINT chk_contents_listing_cover_media_id_content_type
        CHECK (listing_cover_media_id IS NULL OR type = 'LULLABY'),
    ADD CONSTRAINT fk_contents_listing_cover_media
        FOREIGN KEY (listing_cover_media_id) REFERENCES media_assets (id) ON DELETE SET NULL;

CREATE OR REPLACE FUNCTION ensure_lullaby_listing_cover_image() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.listing_cover_media_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM media_assets WHERE id = NEW.listing_cover_media_id AND media_type = 'IMAGE'
    ) THEN
        RAISE EXCEPTION 'listing_cover_media_id must reference an IMAGE asset';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_contents_listing_cover_image
    BEFORE INSERT OR UPDATE OF listing_cover_media_id ON contents
    FOR EACH ROW EXECUTE FUNCTION ensure_lullaby_listing_cover_image();

CREATE INDEX idx_contents_listing_cover_media_id
    ON contents (listing_cover_media_id)
    WHERE listing_cover_media_id IS NOT NULL;
