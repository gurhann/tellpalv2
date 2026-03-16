-- V3: Seed v2_languages table with initial language records
-- Requirements: 21.1, 21.2, 21.3, 21.4

INSERT INTO v2_languages (code, display_name, is_active) VALUES
    ('tr', 'Türkçe',     true),
    ('en', 'İngilizce',  true),
    ('es', 'İspanyolca', true),
    ('pt', 'Portekizce', true),
    ('de', 'Almanca',    true)
ON CONFLICT (code) DO NOTHING;
