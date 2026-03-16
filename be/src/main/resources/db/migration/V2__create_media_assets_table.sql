-- V2: Create v2_media_assets table
-- Requirements: 5.1, 5.2, 5.3, 17.2

CREATE TABLE v2_media_assets (
    id               bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
    provider         varchar(32)  NOT NULL DEFAULT 'FIREBASE_STORAGE',
    object_path      text         NOT NULL,
    kind             varchar(32)  NOT NULL,
    mime_type        text,
    bytes            bigint,
    checksum_sha256  char(64),
    download_url     text,
    created_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_media_assets PRIMARY KEY (id),
    CONSTRAINT uq_v2_media_assets_provider_path UNIQUE (provider, object_path),
    CONSTRAINT chk_v2_media_assets_kind CHECK (
        kind IN (
            'ORIGINAL_IMAGE',
            'ORIGINAL_AUDIO',
            'THUMBNAIL_PHONE',
            'THUMBNAIL_TABLET',
            'DETAIL_PHONE',
            'DETAIL_TABLET',
            'OPTIMIZED_AUDIO',
            'CONTENT_ZIP',
            'CONTENT_ZIP_PART1',
            'CONTENT_ZIP_PART2'
        )
    )
);
