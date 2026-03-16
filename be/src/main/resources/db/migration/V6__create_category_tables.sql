-- V6: Create category tables
-- Requirements: 4, 17.3

-- 1. v2_categories
CREATE TABLE v2_categories (
    id         bigint      NOT NULL GENERATED ALWAYS AS IDENTITY,
    slug       text        NOT NULL,
    type       varchar(32) NOT NULL,
    is_premium boolean     NOT NULL DEFAULT false,
    is_active  boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_categories PRIMARY KEY (id),
    CONSTRAINT uq_v2_categories_slug UNIQUE (slug),
    CONSTRAINT chk_v2_categories_type CHECK (type IN ('CONTENT', 'PARENT_GUIDANCE'))
);

-- 2. v2_category_localizations
CREATE TABLE v2_category_localizations (
    category_id    bigint      NOT NULL,
    language_code  varchar(10) NOT NULL,
    name           text        NOT NULL,
    description    text,
    image_media_id bigint,
    status         varchar(16) NOT NULL DEFAULT 'DRAFT',
    published_at   timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_category_localizations PRIMARY KEY (category_id, language_code),
    CONSTRAINT fk_v2_category_localizations_category
        FOREIGN KEY (category_id) REFERENCES v2_categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_category_localizations_language
        FOREIGN KEY (language_code) REFERENCES v2_languages(code),
    CONSTRAINT fk_v2_category_localizations_image_media
        FOREIGN KEY (image_media_id) REFERENCES v2_media_assets(id) ON DELETE SET NULL,
    CONSTRAINT chk_v2_category_localizations_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    )
);

-- 3. v2_category_contents
CREATE TABLE v2_category_contents (
    id            bigint      NOT NULL GENERATED ALWAYS AS IDENTITY,
    category_id   bigint      NOT NULL,
    language_code varchar(10) NOT NULL,
    content_id    bigint      NOT NULL,
    display_order int         NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_category_contents PRIMARY KEY (id),
    CONSTRAINT uq_v2_category_contents UNIQUE (category_id, language_code, content_id),
    CONSTRAINT fk_v2_category_contents_category
        FOREIGN KEY (category_id) REFERENCES v2_categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_category_contents_content
        FOREIGN KEY (content_id) REFERENCES v2_contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_category_contents_language
        FOREIGN KEY (language_code) REFERENCES v2_languages(code),
    CONSTRAINT chk_v2_category_contents_display_order CHECK (display_order >= 0)
);
