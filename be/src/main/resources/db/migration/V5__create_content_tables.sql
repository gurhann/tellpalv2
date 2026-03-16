-- V5: Create content tables
-- Requirements: 1, 2, 3, 5, 17.3, 17.4, 23.1

-- 1. v2_contents
CREATE TABLE v2_contents (
    id           bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
    type         varchar(32)  NOT NULL,
    external_key text         NOT NULL,
    is_active    boolean      NOT NULL DEFAULT true,
    age_range    int,
    page_count   int,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_contents PRIMARY KEY (id),
    CONSTRAINT uq_v2_contents_external_key UNIQUE (external_key),
    CONSTRAINT chk_v2_contents_type CHECK (
        type IN ('STORY', 'AUDIO_STORY', 'MEDITATION', 'LULLABY')
    )
);

-- 2. v2_content_localizations
CREATE TABLE v2_content_localizations (
    content_id         bigint       NOT NULL,
    language_code      varchar(10)  NOT NULL,
    title              text         NOT NULL,
    description        text,
    body_text          text,
    cover_media_id     bigint,
    audio_media_id     bigint,
    duration_minutes   int,
    status             varchar(16)  NOT NULL DEFAULT 'DRAFT',
    processing_status  varchar(16)  NOT NULL DEFAULT 'PENDING',
    published_at       timestamptz,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_content_localizations PRIMARY KEY (content_id, language_code),
    CONSTRAINT fk_v2_content_localizations_content
        FOREIGN KEY (content_id) REFERENCES v2_contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_content_localizations_language
        FOREIGN KEY (language_code) REFERENCES v2_languages(code),
    CONSTRAINT fk_v2_content_localizations_cover_media
        FOREIGN KEY (cover_media_id) REFERENCES v2_media_assets(id) ON DELETE SET NULL,
    CONSTRAINT fk_v2_content_localizations_audio_media
        FOREIGN KEY (audio_media_id) REFERENCES v2_media_assets(id) ON DELETE SET NULL,
    CONSTRAINT chk_v2_content_localizations_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT chk_v2_content_localizations_processing_status CHECK (
        processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

-- 3. v2_story_pages
CREATE TABLE v2_story_pages (
    content_id            bigint  NOT NULL,
    page_number           int     NOT NULL,
    illustration_media_id bigint,

    CONSTRAINT pk_v2_story_pages PRIMARY KEY (content_id, page_number),
    CONSTRAINT fk_v2_story_pages_content
        FOREIGN KEY (content_id) REFERENCES v2_contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_story_pages_illustration_media
        FOREIGN KEY (illustration_media_id) REFERENCES v2_media_assets(id) ON DELETE SET NULL,
    CONSTRAINT chk_v2_story_pages_page_number CHECK (page_number >= 1)
);

-- 4. v2_story_page_localizations
CREATE TABLE v2_story_page_localizations (
    content_id    bigint       NOT NULL,
    page_number   int          NOT NULL,
    language_code varchar(10)  NOT NULL,
    text_content  text,
    audio_media_id bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_story_page_localizations PRIMARY KEY (content_id, page_number, language_code),
    CONSTRAINT fk_v2_story_page_localizations_page
        FOREIGN KEY (content_id, page_number) REFERENCES v2_story_pages(content_id, page_number) ON DELETE CASCADE,
    CONSTRAINT fk_v2_story_page_localizations_audio_media
        FOREIGN KEY (audio_media_id) REFERENCES v2_media_assets(id) ON DELETE SET NULL
);

-- 5. v2_contributors
CREATE TABLE v2_contributors (
    id           bigint      NOT NULL GENERATED ALWAYS AS IDENTITY,
    display_name text        NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_contributors PRIMARY KEY (id)
);

-- 6. v2_content_contributors
CREATE TABLE v2_content_contributors (
    id             bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
    content_id     bigint       NOT NULL,
    contributor_id bigint       NOT NULL,
    role           varchar(32)  NOT NULL,
    language_code  varchar(10),
    credit_name    text,
    sort_order     int          NOT NULL DEFAULT 0,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_content_contributors PRIMARY KEY (id),
    CONSTRAINT fk_v2_content_contributors_content
        FOREIGN KEY (content_id) REFERENCES v2_contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_content_contributors_contributor
        FOREIGN KEY (contributor_id) REFERENCES v2_contributors(id),
    CONSTRAINT fk_v2_content_contributors_language
        FOREIGN KEY (language_code) REFERENCES v2_languages(code),
    CONSTRAINT chk_v2_content_contributors_role CHECK (
        role IN ('AUTHOR', 'ILLUSTRATOR', 'NARRATOR', 'MUSICIAN')
    ),
    CONSTRAINT chk_v2_content_contributors_sort_order CHECK (sort_order >= 0)
);

-- 7. v2_content_free_access
CREATE TABLE v2_content_free_access (
    id            bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
    access_key    text         NOT NULL,
    content_id    bigint       NOT NULL,
    language_code varchar(10)  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_content_free_access PRIMARY KEY (id),
    CONSTRAINT uq_v2_content_free_access UNIQUE (access_key, content_id, language_code),
    CONSTRAINT fk_v2_content_free_access_content
        FOREIGN KEY (content_id) REFERENCES v2_contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_content_free_access_language
        FOREIGN KEY (language_code) REFERENCES v2_languages(code)
);
