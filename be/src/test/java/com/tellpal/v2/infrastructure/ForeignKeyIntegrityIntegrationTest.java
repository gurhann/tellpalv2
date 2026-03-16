package com.tellpal.v2.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Foreign Key Integrity (Özellik 26).
 *
 * Validates: Requirements 17.1, 17.6, 17.7
 *
 * - Req 17.1: The system must enforce referential integrity via foreign key constraints
 * - Req 17.6: Deleting a parent record must cascade to child records (ON DELETE CASCADE)
 * - Req 17.7: Deleting a referenced media asset must set the FK to NULL (ON DELETE SET NULL)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ForeignKeyIntegrityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("tellpal_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM v2_story_pages");
        jdbcTemplate.update("DELETE FROM v2_content_localizations");
        jdbcTemplate.update("DELETE FROM v2_contents");
        jdbcTemplate.update("DELETE FROM v2_media_assets");
    }

    /**
     * Req 17.6: Deleting a content row must cascade-delete its localizations.
     */
    @Test
    void cascadeDeleteContent_removesLocalizations() {
        // Insert a language row if not present (v2_languages is referenced by FK)
        jdbcTemplate.update(
                "INSERT INTO v2_languages (code, display_name, is_active) VALUES ('tr', 'Türkçe', true) " +
                "ON CONFLICT (code) DO NOTHING");

        // Insert content
        Long contentId = jdbcTemplate.queryForObject(
                "INSERT INTO v2_contents (type, external_key, is_active) VALUES (?, ?, true) RETURNING id",
                Long.class,
                "STORY", "ext-key-cascade-loc-" + System.nanoTime());

        // Insert localization for that content
        jdbcTemplate.update(
                "INSERT INTO v2_content_localizations (content_id, language_code, title, status, processing_status) " +
                "VALUES (?, 'tr', 'Test Title', 'DRAFT', 'PENDING')",
                contentId);

        // Verify localization exists
        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM v2_content_localizations WHERE content_id = ?",
                Integer.class, contentId);
        assertThat(countBefore).isEqualTo(1);

        // Delete the content
        jdbcTemplate.update("DELETE FROM v2_contents WHERE id = ?", contentId);

        // Assert localization was cascade-deleted
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM v2_content_localizations WHERE content_id = ?",
                Integer.class, contentId);
        assertThat(countAfter)
                .as("Localization must be cascade-deleted when parent content is deleted (Req 17.6)")
                .isEqualTo(0);
    }

    /**
     * Req 17.6: Deleting a content row must cascade-delete its story pages.
     */
    @Test
    void cascadeDeleteContent_removesStoryPages() {
        // Insert content of type STORY
        Long contentId = jdbcTemplate.queryForObject(
                "INSERT INTO v2_contents (type, external_key, is_active) VALUES (?, ?, true) RETURNING id",
                Long.class,
                "STORY", "ext-key-cascade-pages-" + System.nanoTime());

        // Insert a story page for that content
        jdbcTemplate.update(
                "INSERT INTO v2_story_pages (content_id, page_number) VALUES (?, ?)",
                contentId, 1);

        // Verify story page exists
        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM v2_story_pages WHERE content_id = ?",
                Integer.class, contentId);
        assertThat(countBefore).isEqualTo(1);

        // Delete the content
        jdbcTemplate.update("DELETE FROM v2_contents WHERE id = ?", contentId);

        // Assert story page was cascade-deleted
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM v2_story_pages WHERE content_id = ?",
                Integer.class, contentId);
        assertThat(countAfter)
                .as("Story page must be cascade-deleted when parent content is deleted (Req 17.6)")
                .isEqualTo(0);
    }

    /**
     * Req 17.7: Deleting a media asset must set cover_media_id to NULL in content_localizations.
     */
    @Test
    void setNullOnMediaAssetDelete_nullifiesCoverMediaId() {
        // Insert a language row if not present
        jdbcTemplate.update(
                "INSERT INTO v2_languages (code, display_name, is_active) VALUES ('tr', 'Türkçe', true) " +
                "ON CONFLICT (code) DO NOTHING");

        // Insert a media asset
        Long mediaId = jdbcTemplate.queryForObject(
                "INSERT INTO v2_media_assets (provider, object_path, kind) " +
                "VALUES ('FIREBASE_STORAGE', ?, 'ORIGINAL_IMAGE') RETURNING id",
                Long.class,
                "test/img-" + System.nanoTime() + ".jpg");

        // Insert content
        Long contentId = jdbcTemplate.queryForObject(
                "INSERT INTO v2_contents (type, external_key, is_active) VALUES (?, ?, true) RETURNING id",
                Long.class,
                "STORY", "ext-key-setnull-" + System.nanoTime());

        // Insert localization with cover_media_id pointing to the media asset
        jdbcTemplate.update(
                "INSERT INTO v2_content_localizations " +
                "(content_id, language_code, title, status, processing_status, cover_media_id) " +
                "VALUES (?, 'tr', 'Test Title', 'DRAFT', 'PENDING', ?)",
                contentId, mediaId);

        // Verify cover_media_id is set
        Long coverMediaIdBefore = jdbcTemplate.queryForObject(
                "SELECT cover_media_id FROM v2_content_localizations WHERE content_id = ? AND language_code = 'tr'",
                Long.class, contentId);
        assertThat(coverMediaIdBefore).isEqualTo(mediaId);

        // Delete the media asset
        jdbcTemplate.update("DELETE FROM v2_media_assets WHERE id = ?", mediaId);

        // Assert cover_media_id is now NULL (SET NULL behavior)
        Long coverMediaIdAfter = jdbcTemplate.queryForObject(
                "SELECT cover_media_id FROM v2_content_localizations WHERE content_id = ? AND language_code = 'tr'",
                Long.class, contentId);
        assertThat(coverMediaIdAfter)
                .as("cover_media_id must be set to NULL when referenced media asset is deleted (Req 17.7)")
                .isNull();
    }
}
