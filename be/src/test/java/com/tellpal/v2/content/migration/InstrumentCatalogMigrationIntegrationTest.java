package com.tellpal.v2.content.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class InstrumentCatalogMigrationIntegrationTest {

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
    void seedsStableTurkishCatalogAndKeepsLabelsInSeparateRows() throws Exception {
        flyway().migrate();

        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                var result = statement.executeQuery("""
                        select catalog.code, localization.display_name
                          from instrument_catalogs catalog
                          join instrument_catalog_localizations localization
                            on localization.instrument_catalog_id = catalog.id
                           and localization.language_code = 'tr'
                         order by catalog.id
                        """)) {
            List<String> labels = new ArrayList<>();
            List<String> codes = new ArrayList<>();
            while (result.next()) {
                codes.add(result.getString(1));
                labels.add(result.getString(2));
            }
            assertThat(codes).containsExactly(
                    "CELESTA", "BELL", "VIOLIN", "RHODES", "GLOCKENSPIEL", "HARP", "VIBRAPHONE",
                    "STRING_ORCHESTRA");
            assertThat(labels).containsExactly(
                    "Çelesta", "Bell", "Keman", "Rhodes", "Glockenspiel", "Arp", "Vibrafon",
                    "Yaylı Orkestra");
        }
    }

    @Test
    void instrumentLinksRequireLullabyContentAndUniqueOrders() throws Exception {
        flyway().migrate();
        long storyId = insertContent("STORY", "story-instrument-link");
        long firstCatalogId = catalogId("CELESTA");
        long secondCatalogId = catalogId("BELL");

        assertThatThrownBy(() -> execute("""
                insert into lullaby_instruments (content_id, instrument_catalog_id, display_order)
                values (%d, %d, 0)
                """.formatted(storyId, firstCatalogId)))
                .hasMessageContaining("requires LULLABY content");

        long lullabyId = insertContent("LULLABY", "lullaby-instrument-link");
        execute("insert into lullaby_instruments (content_id, instrument_catalog_id, display_order) values (%d, %d, 0)"
                .formatted(lullabyId, firstCatalogId));
        assertThatThrownBy(() -> execute("""
                insert into lullaby_instruments (content_id, instrument_catalog_id, display_order)
                values (%d, %d, 0)
                """.formatted(lullabyId, secondCatalogId)))
                .hasMessageContaining("duplicate key");

        assertThatThrownBy(() -> execute("""
                insert into instrument_catalogs (code, is_active)
                values ('bell', true)
                """))
                .hasMessageContaining("chk_instrument_catalogs_code_normalized");

        long nonContiguousLullabyId = insertContent("LULLABY", "non-contiguous-instrument-order");
        assertThatThrownBy(() -> execute("""
                insert into lullaby_instruments (content_id, instrument_catalog_id, display_order)
                values (%d, %d, 1)
                """.formatted(nonContiguousLullabyId, firstCatalogId)))
                .hasMessageContaining("zero-based and contiguous");

        long duplicateCatalogLullabyId = insertContent("LULLABY", "duplicate-instrument-catalog");
        execute("insert into lullaby_instruments (content_id, instrument_catalog_id, display_order) values (%d, %d, 0)"
                .formatted(duplicateCatalogLullabyId, firstCatalogId));
        assertThatThrownBy(() -> execute("""
                insert into lullaby_instruments (content_id, instrument_catalog_id, display_order)
                values (%d, %d, 1)
                """.formatted(duplicateCatalogLullabyId, firstCatalogId)))
                .hasMessageContaining("uk_lullaby_instruments_content_catalog");

        assertThatThrownBy(() -> execute("update contents set type = 'STORY' where id = " + lullabyId))
                .hasMessageContaining("cannot leave LULLABY");
    }

    private Flyway flyway() {
        return Flyway.configure()
                .cleanDisabled(false)
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .load();
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
    }

    private long insertContent(String type, String externalKey) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                var result = statement.executeQuery("""
                        insert into contents (type, external_key, is_active, page_count)
                        values ('%s', '%s', true, %s)
                        returning id
                        """.formatted(type, externalKey, "STORY".equals(type) ? "0" : "null"))) {
            result.next();
            return result.getLong(1);
        }
    }

    private long catalogId(String code) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                var result = statement.executeQuery("select id from instrument_catalogs where code = '" + code + "'")) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
