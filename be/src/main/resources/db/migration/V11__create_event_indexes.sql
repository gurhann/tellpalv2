-- V11: Create analytics indexes
-- Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8, 16.9

-- v2_content_events indexes
CREATE INDEX idx_v2_content_events_profile_occurred_at
    ON v2_content_events (profile_id, occurred_at);

CREATE INDEX idx_v2_content_events_content_occurred_at
    ON v2_content_events (content_id, occurred_at);

CREATE INDEX idx_v2_content_events_event_type_occurred_at
    ON v2_content_events (event_type, occurred_at);

CREATE INDEX idx_v2_content_events_session_id
    ON v2_content_events (session_id);

-- v2_app_events indexes
CREATE INDEX idx_v2_app_events_profile_occurred_at
    ON v2_app_events (profile_id, occurred_at);

CREATE INDEX idx_v2_app_events_content_occurred_at
    ON v2_app_events (content_id, occurred_at);

CREATE INDEX idx_v2_app_events_event_type_occurred_at
    ON v2_app_events (event_type, occurred_at);

-- v2_purchase_events indexes
CREATE INDEX idx_v2_purchase_events_user_occurred_at
    ON v2_purchase_events (user_id, occurred_at);

-- v2_purchase_context_snapshots indexes
CREATE INDEX idx_v2_purchase_context_snapshots_user_created_at
    ON v2_purchase_context_snapshots (user_id, created_at);
