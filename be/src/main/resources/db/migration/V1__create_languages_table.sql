-- V1: Create v2_languages table
-- Requirements: 5.1, 5.2, 17.2

CREATE TABLE v2_languages (
    code        varchar(10)  NOT NULL,
    display_name text,
    is_active   boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT pk_v2_languages PRIMARY KEY (code)
);
