-- V9: Create purchase tables
-- Requirements: 9, 10, 11, 12.7

-- 1. v2_subscription_products
CREATE TABLE v2_subscription_products (
    store                varchar(32)  NOT NULL,
    product_id           text         NOT NULL,
    product_type         varchar(32)  NOT NULL,
    billing_period_unit  varchar(16),
    billing_period_count int,
    entitlement_ids      jsonb        NOT NULL DEFAULT '[]',
    is_active            boolean      NOT NULL DEFAULT true,
    created_at           timestamptz  NOT NULL DEFAULT now(),
    updated_at           timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_subscription_products
        PRIMARY KEY (store, product_id),
    CONSTRAINT fk_v2_subscription_products_store
        FOREIGN KEY (store) REFERENCES v2_purchase_stores (code)
);

-- 2. v2_purchase_events
CREATE TABLE v2_purchase_events (
    id                            bigint       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id                       bigint       NOT NULL,
    occurred_at                   timestamptz  NOT NULL,
    ingested_at                   timestamptz  NOT NULL DEFAULT now(),
    source                        varchar(32)  NOT NULL,
    event_type                    varchar(64)  NOT NULL,
    product_id                    text,
    store                         varchar(32),
    currency                      varchar(3),
    price                         numeric,
    price_in_purchased_currency   numeric,
    tax_percentage                numeric,
    commission_percentage         numeric,
    period_type                   varchar(32),
    environment                   varchar(16),
    is_trial_conversion           boolean,
    cancel_reason                 varchar(32),
    expiration_reason             varchar(32),
    transaction_id                text,
    original_transaction_id       text,
    renewal_number                int,
    offer_code                    text,
    country_code                  varchar(2),
    presented_offering_id         text,
    new_product_id                text,
    expiration_at                 timestamptz,
    grace_period_expiration_at    timestamptz,
    auto_resume_at                timestamptz,
    event_timestamp_at            timestamptz,
    revenuecat_event_id           text,
    raw_payload                   jsonb,
    created_at                    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_purchase_events
        PRIMARY KEY (id),
    CONSTRAINT uq_v2_purchase_events_revenuecat_event_id
        UNIQUE (revenuecat_event_id),
    CONSTRAINT fk_v2_purchase_events_user
        FOREIGN KEY (user_id) REFERENCES v2_app_users (id),
    CONSTRAINT fk_v2_purchase_events_event_type
        FOREIGN KEY (event_type) REFERENCES v2_purchase_event_types (code),
    CONSTRAINT fk_v2_purchase_events_store
        FOREIGN KEY (store) REFERENCES v2_purchase_stores (code),
    CONSTRAINT fk_v2_purchase_events_period_type
        FOREIGN KEY (period_type) REFERENCES v2_subscription_period_types (code),
    CONSTRAINT fk_v2_purchase_events_environment
        FOREIGN KEY (environment) REFERENCES v2_purchase_environments (code),
    CONSTRAINT chk_v2_purchase_events_source
        CHECK (source IN ('REVENUECAT_WEBHOOK', 'CLIENT'))
);

-- 3. v2_purchase_context_snapshots
CREATE TABLE v2_purchase_context_snapshots (
    id                        bigint   NOT NULL GENERATED ALWAYS AS IDENTITY,
    purchase_event_id         bigint   NOT NULL,
    user_id                   bigint   NOT NULL,
    profile_id                bigint,
    attribution_window_seconds int,
    attributed_app_event_id   uuid,
    attributed_content_id     bigint,
    profile_snapshot          jsonb,
    created_at                timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_purchase_context_snapshots
        PRIMARY KEY (id),
    CONSTRAINT uq_v2_purchase_context_snapshots_purchase_event_id
        UNIQUE (purchase_event_id),
    CONSTRAINT fk_v2_purchase_context_snapshots_purchase_event
        FOREIGN KEY (purchase_event_id) REFERENCES v2_purchase_events (id),
    CONSTRAINT fk_v2_purchase_context_snapshots_user
        FOREIGN KEY (user_id) REFERENCES v2_app_users (id),
    CONSTRAINT fk_v2_purchase_context_snapshots_profile
        FOREIGN KEY (profile_id) REFERENCES v2_user_profiles (id) ON DELETE SET NULL,
    CONSTRAINT fk_v2_purchase_context_snapshots_content
        FOREIGN KEY (attributed_content_id) REFERENCES v2_contents (id) ON DELETE SET NULL
);
