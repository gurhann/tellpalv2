create table content_events (
    event_id uuid primary key,
    profile_id bigint not null references user_profiles (id),
    content_id bigint not null references contents (id),
    language_code varchar(8) not null references languages (code),
    event_type varchar(20) not null,
    occurred_at timestamptz not null,
    ingested_at timestamptz not null default now(),
    session_id uuid null,
    left_page integer null,
    engagement_seconds integer null,
    metadata jsonb null,
    legacy_event_key varchar(191) null,
    constraint chk_content_events_event_type check (
        event_type in ('START', 'EXIT', 'COMPLETE')
    ),
    constraint chk_content_events_left_page_positive check (
        left_page is null or left_page > 0
    ),
    constraint chk_content_events_left_page_exit_only check (
        left_page is null or event_type = 'EXIT'
    ),
    constraint chk_content_events_engagement_seconds_non_negative check (
        engagement_seconds is null or engagement_seconds >= 0
    ),
    constraint chk_content_events_legacy_event_key_not_blank check (
        legacy_event_key is null or btrim(legacy_event_key) <> ''
    )
);

create index idx_content_events_profile_occurred_at
    on content_events (profile_id, occurred_at desc);
create index idx_content_events_content_occurred_at
    on content_events (content_id, occurred_at desc);
create index idx_content_events_event_type_occurred_at
    on content_events (event_type, occurred_at desc);
create index idx_content_events_session_id
    on content_events (session_id)
    where session_id is not null;
create unique index uk_content_events_profile_legacy_event_key
    on content_events (profile_id, legacy_event_key)
    where legacy_event_key is not null;

create table app_events (
    event_id uuid primary key,
    profile_id bigint not null references user_profiles (id),
    event_type varchar(32) not null,
    content_id bigint null references contents (id),
    occurred_at timestamptz not null,
    ingested_at timestamptz not null default now(),
    payload jsonb null,
    legacy_event_key varchar(191) null,
    constraint chk_app_events_event_type check (
        event_type in (
            'APP_OPENED',
            'ONBOARDING_STARTED',
            'ONBOARDING_COMPLETED',
            'ONBOARDING_SKIPPED',
            'PAYWALL_SHOWN',
            'LOCKED_CONTENT_CLICKED'
        )
    ),
    constraint chk_app_events_locked_content_requires_content_id check (
        event_type <> 'LOCKED_CONTENT_CLICKED' or content_id is not null
    ),
    constraint chk_app_events_legacy_event_key_not_blank check (
        legacy_event_key is null or btrim(legacy_event_key) <> ''
    )
);

create index idx_app_events_profile_occurred_at
    on app_events (profile_id, occurred_at desc);
create index idx_app_events_content_occurred_at
    on app_events (content_id, occurred_at desc)
    where content_id is not null;
create index idx_app_events_event_type_occurred_at
    on app_events (event_type, occurred_at desc);
create unique index uk_app_events_profile_legacy_event_key
    on app_events (profile_id, legacy_event_key)
    where legacy_event_key is not null;
