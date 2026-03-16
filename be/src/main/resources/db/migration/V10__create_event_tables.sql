-- V10: Create event tables
-- Requirements: 7, 8, 17.3, 17.8

-- 1. v2_content_events
CREATE TABLE v2_content_events (
    event_id             uuid         NOT NULL,
    profile_id           bigint       NOT NULL,
    content_id           bigint       NOT NULL,
    language_code        varchar(10)  NOT NULL,
    event_type           varchar(32)  NOT NULL,
    occurred_at          timestamptz  NOT NULL,
    ingested_at          timestamptz  NOT NULL DEFAULT now(),
    session_id           uuid,
    left_page            int,
    engagement_seconds   int,
    metadata             jsonb,
    legacy_event_key     varchar(255),

    CONSTRAINT pk_v2_content_events
        PRIMARY KEY (event_id),
    CONSTRAINT fk_v2_content_events_profile
        FOREIGN KEY (profile_id) REFERENCES v2_user_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_content_events_content
        FOREIGN KEY (content_id) REFERENCES v2_contents (id) ON DELETE CASCADE,
    CONSTRAINT chk_v2_content_events_event_type
        CHECK (event_type IN ('START', 'EXIT', 'COMPLETE')),
    CONSTRAINT chk_v2_content_events_left_page
        CHECK (left_page >= 0),
    CONSTRAINT chk_v2_content_events_engagement_seconds
        CHECK (engagement_seconds >= 0)
);

CREATE UNIQUE INDEX uq_v2_content_events_profile_legacy_event_key
    ON v2_content_events (profile_id, legacy_event_key)
    WHERE legacy_event_key IS NOT NULL;

-- 2. v2_app_events
CREATE TABLE v2_app_events (
    event_id             uuid         NOT NULL,
    profile_id           bigint       NOT NULL,
    event_type           varchar(64)  NOT NULL,
    content_id           bigint,
    occurred_at          timestamptz  NOT NULL,
    ingested_at          timestamptz  NOT NULL DEFAULT now(),
    payload              jsonb,
    legacy_event_key     varchar(255),

    CONSTRAINT pk_v2_app_events
        PRIMARY KEY (event_id),
    CONSTRAINT fk_v2_app_events_profile
        FOREIGN KEY (profile_id) REFERENCES v2_user_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_v2_app_events_content
        FOREIGN KEY (content_id) REFERENCES v2_contents (id) ON DELETE SET NULL,
    CONSTRAINT chk_v2_app_events_event_type
        CHECK (event_type IN ('APP_OPENED', 'ONBOARDING_STARTED', 'ONBOARDING_COMPLETED', 'ONBOARDING_SKIPPED', 'PAYWALL_SHOWN', 'LOCKED_CONTENT_CLICKED'))
);

CREATE UNIQUE INDEX uq_v2_app_events_profile_legacy_event_key
    ON v2_app_events (profile_id, legacy_event_key)
    WHERE legacy_event_key IS NOT NULL;
