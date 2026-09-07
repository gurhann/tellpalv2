package com.tellpal.v2.content.migration;

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
class ContentCoverOwnershipMigrationIntegrationTest {

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
    void blocksV24WhenLegacyNonStorySourceCoversExist() throws Exception {
        migrateTo("23");
        long contentId = insertLegacyContent();

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("V24 blocked")
                .hasMessageContaining(Long.toString(contentId))
                .hasMessageContaining("textless_cover_media_id");
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

    private long insertLegacyContent() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            try (var result = statement.executeQuery("""
                    insert into contents (type, external_key, is_active, page_count, textless_cover_media_id)
                    values ('MEDITATION', 'legacy-cover-meditation', true, null, 41)
                    returning id
                    """)) {
                if (!result.next()) {
                    throw new IllegalStateException("Legacy content insert did not return an ID");
                }
                return result.getLong(1);
            }
        }
    }
}
