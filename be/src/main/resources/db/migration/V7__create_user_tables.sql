-- V7: Create user tables
-- Requirements: 6.2, 6.3, 6.6, 17.2, 17.8

-- 1. v2_app_users
CREATE TABLE v2_app_users (
    id                  bigint      NOT NULL GENERATED ALWAYS AS IDENTITY,
    firebase_uid        text        NOT NULL,
    is_allow_marketing  boolean     NOT NULL DEFAULT false,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_app_users PRIMARY KEY (id),
    CONSTRAINT uq_v2_app_users_firebase_uid UNIQUE (firebase_uid)
);

-- 2. v2_user_profiles
CREATE TABLE v2_user_profiles (
    id               bigint   NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id          bigint   NOT NULL,
    name             text,
    age_range        int,
    avatar_media_id  bigint,
    favorite_genres  text[]   NOT NULL DEFAULT '{}',
    main_purposes    text[]   NOT NULL DEFAULT '{}',
    is_primary       boolean  NOT NULL DEFAULT false,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_user_profiles PRIMARY KEY (id),
    CONSTRAINT fk_v2_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES v2_app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_user_profiles_avatar_media
        FOREIGN KEY (avatar_media_id) REFERENCES v2_media_assets(id) ON DELETE SET NULL
);

-- 3. Partial unique index: only one primary profile per user
CREATE UNIQUE INDEX uq_v2_user_profiles_primary ON v2_user_profiles (user_id) WHERE is_primary = true;
