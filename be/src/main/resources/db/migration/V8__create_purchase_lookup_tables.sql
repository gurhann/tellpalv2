-- V8: Create purchase lookup tables
-- Requirements: 12.1, 12.2, 12.3, 12.4, 12.5

-- 1. v2_purchase_event_types
CREATE TABLE v2_purchase_event_types (
    code        varchar(64) NOT NULL,
    description text,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_purchase_event_types PRIMARY KEY (code)
);

INSERT INTO v2_purchase_event_types (code) VALUES
    ('INITIAL_PURCHASE'),
    ('RENEWAL'),
    ('CANCELLATION'),
    ('EXPIRATION'),
    ('UNCANCELLATION'),
    ('BILLING_ISSUE'),
    ('PRODUCT_CHANGE'),
    ('TRANSFER'),
    ('SUBSCRIPTION_PAUSED'),
    ('SUBSCRIPTION_EXTENDED'),
    ('TEMPORARY_ENTITLEMENT_GRANT');

-- 2. v2_subscription_period_types
CREATE TABLE v2_subscription_period_types (
    code        varchar(32) NOT NULL,
    description text,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_subscription_period_types PRIMARY KEY (code)
);

INSERT INTO v2_subscription_period_types (code) VALUES
    ('TRIAL'),
    ('INTRO'),
    ('NORMAL'),
    ('PROMOTIONAL'),
    ('PREPAID');

-- 3. v2_purchase_stores
CREATE TABLE v2_purchase_stores (
    code        varchar(32) NOT NULL,
    description text,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_purchase_stores PRIMARY KEY (code)
);

INSERT INTO v2_purchase_stores (code) VALUES
    ('APP_STORE'),
    ('PLAY_STORE'),
    ('STRIPE'),
    ('RC_BILLING'),
    ('AMAZON');

-- 4. v2_purchase_environments
CREATE TABLE v2_purchase_environments (
    code        varchar(16) NOT NULL,
    description text,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_purchase_environments PRIMARY KEY (code)
);

INSERT INTO v2_purchase_environments (code) VALUES
    ('SANDBOX'),
    ('PRODUCTION');

-- 5. v2_purchase_reason_codes
CREATE TABLE v2_purchase_reason_codes (
    code        varchar(32) NOT NULL,
    reason_type varchar(32) NOT NULL,
    description text,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_purchase_reason_codes PRIMARY KEY (code, reason_type),
    CONSTRAINT chk_v2_purchase_reason_codes_reason_type
        CHECK (reason_type IN ('CANCEL_REASON', 'EXPIRATION_REASON'))
);

INSERT INTO v2_purchase_reason_codes (code, reason_type) VALUES
    ('UNSUBSCRIBE',       'CANCEL_REASON'),
    ('BILLING_ERROR',     'CANCEL_REASON'),
    ('PRICE_INCREASE',    'CANCEL_REASON'),
    ('CUSTOMER_SUPPORT',  'CANCEL_REASON'),
    ('UNKNOWN',           'CANCEL_REASON'),
    ('BILLING_ERROR',     'EXPIRATION_REASON'),
    ('CUSTOMER_SUPPORT',  'EXPIRATION_REASON'),
    ('DEVELOPER',         'EXPIRATION_REASON'),
    ('PROMOTIONAL',       'EXPIRATION_REASON'),
    ('UNKNOWN',           'EXPIRATION_REASON');
