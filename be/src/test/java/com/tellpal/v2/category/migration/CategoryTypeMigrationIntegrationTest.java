package com.tellpal.v2.category.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CategoryTypeMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:15");

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("drop schema if exists public cascade");
            statement.execute("create schema public");
        }
    }

    @Test
    void migratesLegacyContentCategoryWhenAllCuratedContentSharesOneType() throws Exception {
        migrateTo("16");
        long categoryId = insertCategory("featured-sleep", "CONTENT");
        insertCategoryLocalization(categoryId, "tr", "Featured Sleep");
        long contentId = insertContent("STORY", "moonlight-story");
        insertContentLocalization(contentId, "tr", "Moonlight Story");
        insertCategoryContent(categoryId, "tr", contentId, 0);

        migrateLatest();

        assertThat(selectCategoryType(categoryId)).isEqualTo("STORY");
    }

    @Test
    void blocksMigrationForLegacyContentCategoryWithoutCuratedContent() throws Exception {
        migrateTo("16");
        insertCategory("empty-category", "CONTENT");

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("empty-category")
                .hasMessageContaining("no curated content");
    }

    @Test
    void blocksMigrationForLegacyContentCategoryWithMixedCuratedTypes() throws Exception {
        migrateTo("16");
        long categoryId = insertCategory("mixed-category", "CONTENT");
        insertCategoryLocalization(categoryId, "tr", "Mixed Category");

        long storyContentId = insertContent("STORY", "forest-story");
        insertContentLocalization(storyContentId, "tr", "Forest Story");
        insertCategoryContent(categoryId, "tr", storyContentId, 0);

        long meditationContentId = insertContent("MEDITATION", "breathing-room");
        insertContentLocalization(meditationContentId, "tr", "Breathing Room");
        insertCategoryContent(categoryId, "tr", meditationContentId, 1);

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("mixed-category")
                .hasMessageContaining("multiple content types");
    }

    @Test
    void blocksMigrationForLegacyParentGuidanceCategory() throws Exception {
        migrateTo("16");
        insertCategory("sleep-routines", "PARENT_GUIDANCE");

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("sleep-routines")
                .hasMessageContaining("PARENT_GUIDANCE");
    }

    private void migrateTo(String version) {
        flyway(version).migrate();
    }

    private void migrateLatest() {
        flyway(null).migrate();
    }

    private Flyway flyway(String version) {
        var configuration = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(
                        POSTGRESQL.getJdbcUrl(),
                        POSTGRESQL.getUsername(),
                        POSTGRESQL.getPassword())
                .locations("classpath:db/migration");
        if (version != null) {
            configuration.target(MigrationVersion.fromVersion(version));
        }
        return configuration.load();
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword());
    }

    private long insertCategory(String slug, String type) throws Exception {
        return insertReturningId("""
                insert into categories (slug, type, is_premium, is_active)
                values ('%s', '%s', false, true)
                returning id
                """.formatted(slug, type));
    }

    private void insertCategoryLocalization(long categoryId, String languageCode, String name) throws Exception {
        execute("""
                insert into category_localizations (
                    category_id,
                    language_code,
                    name,
                    status
                )
                values (%d, '%s', '%s', 'DRAFT')
                """.formatted(categoryId, languageCode, name));
    }

    private long insertContent(String type, String externalKey) throws Exception {
        String pageCountValue = "STORY".equals(type) ? "0" : "null";
        return insertReturningId("""
                insert into contents (type, external_key, is_active, age_range, page_count)
                values ('%s', '%s', true, null, %s)
                returning id
                """.formatted(type, externalKey, pageCountValue));
    }

    private void insertContentLocalization(long contentId, String languageCode, String title) throws Exception {
        execute("""
                insert into content_localizations (
                    content_id,
                    language_code,
                    title,
                    description,
                    body_text,
                    cover_media_id,
                    audio_media_id,
                    duration_minutes,
                    status,
                    processing_status,
                    published_at
                )
                values (%d, '%s', '%s', null, null, null, null, null, 'DRAFT', 'PENDING', null)
                """.formatted(contentId, languageCode, title));
    }

    private void insertCategoryContent(long categoryId, String languageCode, long contentId, int displayOrder)
            throws Exception {
        execute("""
                insert into category_contents (category_id, language_code, content_id, display_order)
                values (%d, '%s', %d, %d)
                """.formatted(categoryId, languageCode, contentId, displayOrder));
    }

    private String selectCategoryType(long categoryId) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                    select type
                    from categories
                    where id = %d
                    """.formatted(categoryId));
            if (!resultSet.next()) {
                throw new IllegalStateException("Category not found after migration: " + categoryId);
            }
            return resultSet.getString(1);
        }
    }

    private long insertReturningId(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (!resultSet.next()) {
                throw new IllegalStateException("Insert did not return an id");
            }
            return resultSet.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
