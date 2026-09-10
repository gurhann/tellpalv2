package com.tellpal.v2.content.migration;

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
class CanonicalAudioStoryTypeRemovalMigrationIntegrationTest {

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
    void blocksMigrationWhenCanonicalAudioStoryContentRemains() throws Exception {
        migrateTo("27");
        long contentId = insertContent("AUDIO_STORY", "legacy-audio-story");

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("contents")
                .hasMessageContaining("AUDIO_STORY")
                .hasMessageContaining(Long.toString(contentId));

        assertMigrationStoppedAt27();
        assertThat(count("contents", contentId)).isEqualTo(1);
        assertThat(constraintDefinition("chk_contents_type")).contains("AUDIO_STORY");
    }

    @Test
    void blocksMigrationWhenCanonicalAudioStoryCategoryRemains() throws Exception {
        migrateTo("27");
        long categoryId = insertCategory("legacy-audio-category");

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("categories")
                .hasMessageContaining("AUDIO_STORY")
                .hasMessageContaining(Long.toString(categoryId))
                .hasMessageContaining("legacy-audio-category");

        assertMigrationStoppedAt27();
        assertThat(count("categories", categoryId)).isEqualTo(1);
        assertThat(constraintDefinition("chk_categories_type")).contains("AUDIO_STORY");
    }

    @Test
    void blocksMigrationWhenCanonicalAudioStoryProcessingRemains() throws Exception {
        migrateTo("27");
        long contentId = insertContent("STORY", "processing-owner");
        insertLocalization(contentId);
        long processingId = insertAudioStoryProcessing(contentId);

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("asset_processing")
                .hasMessageContaining("AUDIO_STORY")
                .hasMessageContaining(Long.toString(processingId))
                .hasMessageContaining(Long.toString(contentId))
                .hasMessageContaining("legacy-processing");

        assertMigrationStoppedAt27();
        assertThat(count("asset_processing", processingId)).isEqualTo(1);
        assertThat(constraintDefinition("chk_asset_processing_content_type")).contains("AUDIO_STORY");
    }

    @Test
    void existingCanonicalRowsSurviveCleanMigration() throws Exception {
        migrateTo("27");
        long contentId = insertContent("STORY", "existing-story");
        long categoryId = insertCategory("existing-category", "STORY");
        long meditationId = insertContent("MEDITATION", "existing-meditation");
        long lullabyId = insertContent("LULLABY", "existing-lullaby");
        insertLocalization(contentId);
        long meditationProcessingId = insertProcessing(
                meditationId, "MEDITATION", "existing-meditation-processing", "CONTENT", null, "DELIVERY");
        long processingId = insertProcessing(
                contentId, "STORY", "existing-processing", "LOCALIZATION", "tr", "STORY_NARRATION");

        migrateLatest();

        assertThat(schemaVersion()).isEqualTo("29");
        assertThat(count("contents", contentId)).isEqualTo(1);
        assertThat(count("contents", meditationId)).isEqualTo(1);
        assertThat(count("contents", lullabyId)).isEqualTo(1);
        assertThat(count("categories", categoryId)).isEqualTo(1);
        assertThat(count("asset_processing", meditationProcessingId)).isEqualTo(1);
        assertThat(count("asset_processing", processingId)).isEqualTo(1);
    }

    @Test
    void cleanDatabaseRemovesAudioStoryFromCanonicalChecksAndKeepsScopedProcessingRules() throws Exception {
        migrateLatest();

        assertThat(schemaVersion()).isEqualTo("29");
        assertThatThrownBy(() -> execute("""
                insert into contents (type, external_key, is_active, page_count)
                values ('AUDIO_STORY', 'rejected-audio-story', true, null)
                """))
                .hasMessageContaining("chk_contents_type");
        assertThatThrownBy(() -> execute("""
                insert into categories (slug, type, is_premium, is_active)
                values ('rejected-audio-category', 'AUDIO_STORY', false, true)
                """))
                .hasMessageContaining("chk_categories_type");

        long contentId = insertContent("STORY", "valid-processing-owner");
        insertLocalization(contentId);
        assertThatThrownBy(() -> insertAudioStoryProcessing(contentId))
                .hasMessageContaining("chk_asset_processing_content_type");

        long nullableContentId = insertContent("STORY", "nullable-processing-owner");
        assertThat(insertProcessing(nullableContentId, null, null, "CONTENT", null, "DELIVERY"))
                .isPositive();

        long contentScopedId = insertContent("MEDITATION", "content-scoped-owner");
        assertThat(insertProcessing(contentScopedId, "MEDITATION", "content-scoped", "CONTENT", null, "DELIVERY"))
                .isPositive();
        assertThatThrownBy(() -> insertProcessing(
                insertContent("MEDITATION", "invalid-content-scope"),
                "MEDITATION",
                "invalid-content-scope",
                "CONTENT",
                "tr",
                "DELIVERY"))
                .hasMessageContaining("chk_asset_processing_target_language");

        long narrationContentId = insertContent("STORY", "narration-owner");
        insertLocalization(narrationContentId);
        assertThat(insertProcessing(
                narrationContentId, "STORY", "narration-processing", "LOCALIZATION", "tr", "STORY_NARRATION"))
                .isPositive();
        assertThatThrownBy(() -> insertProcessing(
                narrationContentId, "MEDITATION", "invalid-narration", "LOCALIZATION", "tr", "STORY_NARRATION"))
                .hasMessageContaining("chk_asset_processing_story_narration_target");

        assertThat(insertContent("STORY", "valid-story")).isPositive();
        assertThat(insertContent("MEDITATION", "valid-meditation")).isPositive();
        assertThat(insertContent("LULLABY", "valid-lullaby")).isPositive();
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
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration");
        if (version != null) {
            configuration.target(MigrationVersion.fromVersion(version));
        }
        return configuration.load();
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
    }

    private long insertContent(String type, String externalKey) throws Exception {
        return insertReturningId("""
                insert into contents (type, external_key, is_active, page_count)
                values ('%s', '%s', true, %s)
                returning id
                """.formatted(type, externalKey, "STORY".equals(type) ? "0" : "null"));
    }

    private long insertCategory(String slug) throws Exception {
        return insertCategory(slug, "AUDIO_STORY");
    }

    private long insertCategory(String slug, String type) throws Exception {
        return insertReturningId("""
                insert into categories (slug, type, is_premium, is_active)
                values ('%s', '%s', false, true)
                returning id
                """.formatted(slug, type));
    }

    private void insertLocalization(long contentId) throws Exception {
        execute("""
                insert into content_localizations
                    (content_id, language_code, title, status, processing_status)
                values (%d, 'tr', 'Processing owner', 'DRAFT', 'PENDING')
                """.formatted(contentId));
    }

    private long insertAudioStoryProcessing(long contentId) throws Exception {
        return insertProcessing(contentId, "AUDIO_STORY", "legacy-processing", "LOCALIZATION", "tr", "DELIVERY");
    }

    private long insertProcessing(
            long contentId,
            String contentType,
            String externalKey,
            String targetScope,
            String languageCode,
            String processingKind) throws Exception {
        String contentTypeSql = contentType == null ? "null" : "'%s'".formatted(contentType);
        String externalKeySql = externalKey == null ? "null" : "'%s'".formatted(externalKey);
        String targetScopeSql = "'%s'".formatted(targetScope);
        String languageCodeSql = languageCode == null ? "null" : "'%s'".formatted(languageCode);
        String processingKindSql = "'%s'".formatted(processingKind);
        String pageCountSql = "STORY".equals(contentType) ? "0" : "null";
        return insertReturningId("""
            insert into asset_processing
                    (content_id, language_code, content_type, external_key, target_scope, processing_kind, page_count)
                values (%d, %s, %s, %s, %s, %s, %s)
                returning id
                """.formatted(
                        contentId,
                        languageCodeSql,
                        contentTypeSql,
                        externalKeySql,
                        targetScopeSql,
                        processingKindSql,
                        pageCountSql));
    }

    private long insertReturningId(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Insert did not return an id");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String table, long id) {
        return jdbcCount("select count(*) from " + table + " where id = " + id);
    }

    private long jdbcCount(String sql) {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Count query failed", exception);
        }
    }

    private String constraintDefinition(String name) {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("select pg_get_constraintdef(oid) "
                        + "from pg_constraint where conname = '" + name + "'")) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Constraint not found: " + name);
            }
            return resultSet.getString(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Constraint query failed", exception);
        }
    }

    private String schemaVersion() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        select version
                          from flyway_schema_history
                         where success = true
                         order by installed_rank desc
                         limit 1
                        """)) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Flyway schema history is empty");
            }
            return resultSet.getString(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Schema version query failed", exception);
        }
    }

    private void assertMigrationStoppedAt27() {
        assertThat(schemaVersion()).isEqualTo("27");
    }
}
