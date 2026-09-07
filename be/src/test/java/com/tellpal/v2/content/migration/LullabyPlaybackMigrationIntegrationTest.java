package com.tellpal.v2.content.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
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
class LullabyPlaybackMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>("postgres:15");

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("drop schema if exists public cascade");
            statement.execute("create schema public");
        }
    }

    @Test
    void backfillsOneSharedPlaybackAndClearsLegacyLocalizationFields() throws Exception {
        migrateTo("24");
        long audioId = insertAudioAsset();
        long contentId = insertLullaby();
        insertLocalization(contentId, "tr", audioId, 11, null);
        insertLocalization(contentId, "en", audioId, 11, null);

        migrateLatest();

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            try (var result = statement.executeQuery("""
                    select playback.audio_media_id, playback.duration_minutes,
                           localization.audio_media_id, localization.duration_minutes
                      from lullaby_playbacks playback
                      join content_localizations localization on localization.content_id = playback.content_id
                     where playback.content_id = %d
                     order by localization.language_code
                    """.formatted(contentId))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isEqualTo(audioId);
                assertThat(result.getInt(2)).isEqualTo(11);
                assertThat(result.getObject(3)).isNull();
                assertThat(result.getObject(4)).isNull();
                assertThat(result.next()).isTrue();
                assertThat(result.getObject(3)).isNull();
                assertThat(result.getObject(4)).isNull();
            }
            try (var result = statement.executeQuery("""
                    select target_scope, language_code, status, content_type, audio_source_asset_id
                      from asset_processing
                     where content_id = %d
                    """.formatted(contentId))) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("CONTENT");
                assertThat(result.getString(2)).isNull();
                assertThat(result.getString(3)).isEqualTo("PENDING");
                assertThat(result.getString(4)).isEqualTo("LULLABY");
                assertThat(result.getLong(5)).isEqualTo(audioId);
            }
        }
    }

    @Test
    void blocksConflictingLegacyPlaybackValues() throws Exception {
        migrateTo("24");
        long firstAudioId = insertAudioAsset();
        long secondAudioId = insertAudioAsset();
        long contentId = insertLullaby();
        insertLocalization(contentId, "tr", firstAudioId, 11, null);
        insertLocalization(contentId, "en", secondAudioId, 12, null);

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("V25 blocked")
                .hasMessageContaining(Long.toString(contentId))
                .hasMessageContaining("conflicting");
    }

    @Test
    void blocksLegacyLullabyLocalizationFields() throws Exception {
        migrateTo("24");
        long contentId = insertLullaby();
        insertLocalization(contentId, "tr", null, null, "legacy body");

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("V25 blocked")
                .hasMessageContaining(Long.toString(contentId))
                .hasMessageContaining("title and publication state");
    }

    @Test
    void blocksLocalizedLullabyMusicianAssignments() throws Exception {
        migrateTo("24");
        long contentId = insertLullaby();
        execute("insert into contributors (display_name) values ('Legacy Musician')");
        execute("""
                insert into content_contributors (content_id, contributor_id, role, language_code, sort_order)
                values (%d, 1, 'MUSICIAN', 'tr', 0)
                """.formatted(contentId));

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("V25 blocked")
                .hasMessageContaining("MUSICIAN")
                .hasMessageContaining("language-scoped");
    }

    @Test
    void postMigrationRulesKeepLullabyFieldsAndMusicianGlobal() throws Exception {
        migrateTo("24");
        long contentId = insertLullaby();
        long audioId = insertAudioAsset();
        long imageId = insertImageAsset();
        migrateLatest();

        assertThatThrownBy(() -> execute("""
                insert into content_localizations (content_id, language_code, title, audio_media_id)
                values (%d, 'tr', 'Ninni', %d)
                """.formatted(contentId, imageId)))
                .hasMessageContaining("LULLABY localizations support only title");

        assertThatThrownBy(() -> execute("""
                insert into lullaby_playbacks (content_id, audio_media_id, duration_minutes)
                values (%d, %d, 10)
                """.formatted(contentId, imageId)))
                .hasMessageContaining("must reference an AUDIO asset");

        execute("""
                insert into lullaby_playbacks (content_id, audio_media_id, duration_minutes)
                values (%d, %d, 10)
                """.formatted(contentId, audioId));
        assertThatThrownBy(() -> execute("""
                update lullaby_playbacks set audio_media_id = %d where content_id = %d
                """.formatted(imageId, contentId)))
                .hasMessageContaining("must reference an AUDIO asset");
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
        return DriverManager.getConnection(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
    }

    private long insertAudioAsset() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                var result = statement.executeQuery("""
                insert into media_assets (provider, object_path, media_type, kind)
                values ('LOCAL', 'audio-' || (select count(*) + 1 from media_assets), 'AUDIO', 'ORIGINAL_AUDIO')
                returning id
                """)) {
            result.next();
            return result.getLong(1);
        }
    }

    private long insertImageAsset() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                var result = statement.executeQuery("""
                insert into media_assets (provider, object_path, media_type, kind)
                values ('LOCAL', 'image-' || (select count(*) + 1 from media_assets), 'IMAGE', 'ORIGINAL_IMAGE')
                returning id
                """)) {
            result.next();
            return result.getLong(1);
        }
    }

    private long insertLullaby() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                var result = statement.executeQuery("""
                insert into contents (type, external_key, is_active, page_count)
                values ('LULLABY', 'lullaby-' || (select count(*) + 1 from contents), true, null)
                returning id
                """)) {
            result.next();
            return result.getLong(1);
        }
    }

    private void insertLocalization(long contentId, String languageCode, Long audioId, Integer duration,
            String bodyText) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    insert into content_localizations
                        (content_id, language_code, title, body_text, audio_media_id, duration_minutes)
                    values (%d, '%s', 'Ninni', %s, %s, %s)
                    """.formatted(contentId, languageCode,
                            bodyText == null ? "null" : "'" + bodyText + "'",
                            audioId == null ? "null" : audioId,
                            duration == null ? "null" : duration));
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
