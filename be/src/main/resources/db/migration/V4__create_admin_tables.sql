-- V4: Create admin tables
-- Requirements: 13.1, 13.2, 13.5, 13.6, 14.1, 15.6, 15.7, 17.2, 17.5

CREATE TABLE admin_users (
    id              bigserial    NOT NULL,
    username        varchar      NOT NULL,
    password_hash   varchar      NOT NULL,
    enabled         boolean      NOT NULL DEFAULT true,
    last_login_at   timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_users PRIMARY KEY (id),
    CONSTRAINT uq_admin_users_username UNIQUE (username)
);

CREATE TABLE admin_roles (
    code        varchar      NOT NULL,
    description varchar,
    created_at  timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_roles PRIMARY KEY (code)
);

CREATE TABLE admin_user_roles (
    admin_user_id  bigint       NOT NULL,
    role_code      varchar      NOT NULL,
    created_at     timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_user_roles PRIMARY KEY (admin_user_id, role_code),
    CONSTRAINT fk_admin_user_roles_user FOREIGN KEY (admin_user_id)
        REFERENCES admin_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_roles_role FOREIGN KEY (role_code)
        REFERENCES admin_roles (code) ON DELETE CASCADE
);

CREATE TABLE admin_refresh_tokens (
    id                      bigserial    NOT NULL,
    admin_user_id           bigint       NOT NULL,
    token_hash              varchar      NOT NULL,
    issued_at               timestamptz  NOT NULL,
    expires_at              timestamptz  NOT NULL,
    revoked_at              timestamptz,
    replaced_by_token_hash  varchar,
    user_agent              varchar,
    ip                      varchar,
    created_at              timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_admin_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_admin_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_admin_refresh_tokens_user FOREIGN KEY (admin_user_id)
        REFERENCES admin_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_admin_refresh_tokens_expires_after_issued
        CHECK (expires_at > issued_at)
);
