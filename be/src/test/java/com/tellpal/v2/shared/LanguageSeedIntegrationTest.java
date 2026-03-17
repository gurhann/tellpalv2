package com.tellpal.v2.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class LanguageSeedIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void seedsSupportedLanguagesAsActiveRecords() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select code, display_name, is_active from languages order by code");

        assertThat(rows).hasSize(5);
        assertThat(rows)
                .extracting(row -> row.get("code"))
                .containsExactly("de", "en", "es", "pt", "tr");
        assertThat(rows).allMatch(row -> Boolean.TRUE.equals(row.get("is_active")));
        assertThat(rows)
                .extracting(row -> row.get("display_name"))
                .containsExactly("Almanca", "İngilizce", "İspanyolca", "Portekizce", "Türkçe");
    }
}
