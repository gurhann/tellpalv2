package com.tellpal.v2.content.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ContributorRoleMigrationIntegrationTest {

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
    void backfillsDistinctHistoricalRolesWithoutChangingAssignments() throws Exception {
        migrateTo("19");
        long contentId = insertContent("migration-role-backfill");
        long secondContentId = insertContent("migration-role-backfill-second");
        long multidisciplinaryContributorId = insertContributor("Mina Kaya");
        long unassignedContributorId = insertContributor("Ada Yilmaz");
        insertAssignment(contentId, multidisciplinaryContributorId, "AUTHOR", null, "M. Kaya", 0);
        insertAssignment(secondContentId, multidisciplinaryContributorId, "AUTHOR", null, "Mina Kaya", 0);
        insertAssignment(contentId, multidisciplinaryContributorId, "NARRATOR", "tr", "Mina", 0);
        List<AssignmentSnapshot> assignmentsBeforeMigration = selectAssignments();

        migrateLatest();

        assertThat(selectRoles(multidisciplinaryContributorId))
                .containsExactly("AUTHOR", "NARRATOR");
        assertThat(selectRoles(unassignedContributorId)).containsExactly("AUTHOR");
        assertThat(selectAssignments()).containsExactlyElementsOf(assignmentsBeforeMigration);
        assertThat(selectNormalizedDisplayName(multidisciplinaryContributorId)).isEqualTo("mina kaya");
        assertThat(selectNormalizedDisplayName(unassignedContributorId)).isEqualTo("ada yilmaz");
    }

    @Test
    void duplicatePreflightReportsEveryConflictingIdAndNameAndRollsBackSchemaChanges() throws Exception {
        migrateTo("19");
        long firstAdaId = insertContributor(" Ada ");
        long secondAdaId = insertContributor("ada");
        long firstMinaId = insertContributor("MINA");
        long secondMinaId = insertContributor(" mina ");

        assertThatThrownBy(this::migrateLatest)
                .hasMessageContaining("V20 blocked")
                .hasMessageContaining("id=" + firstAdaId)
                .hasMessageContaining("name=' Ada '")
                .hasMessageContaining("id=" + secondAdaId)
                .hasMessageContaining("name='ada'")
                .hasMessageContaining("id=" + firstMinaId)
                .hasMessageContaining("name='MINA'")
                .hasMessageContaining("id=" + secondMinaId)
                .hasMessageContaining("name=' mina '");

        assertThat(tableExists("contributor_roles")).isFalse();
        assertThat(columnExists("contributors", "normalized_display_name")).isFalse();
        assertThat(selectContributorNames()).containsExactly(" Ada ", "ada", "MINA", " mina ");
    }

    @Test
    void generatedNormalizedNameRejectsNewTrimmedCaseInsensitiveDuplicate() throws Exception {
        migrateLatest();
        long contributorId = insertContributor("  Ada Lovelace  ");
        long secondContributorId = insertContributor("Grace Hopper");

        assertThat(selectNormalizedDisplayName(contributorId)).isEqualTo("ada lovelace");
        assertThatThrownBy(() -> insertContributor("ADA LOVELACE"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_contributors_normalized_display_name");

        updateContributorName(contributorId, "  Augusta Ada  ");

        assertThat(selectNormalizedDisplayName(contributorId)).isEqualTo("augusta ada");
        assertThatThrownBy(() -> updateContributorName(secondContributorId, "AUGUSTA ADA"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("uk_contributors_normalized_display_name");
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

    private long insertContent(String externalKey) throws Exception {
        return insertReturningId("""
                insert into contents (type, external_key, is_active, page_count)
                values ('STORY', '%s', true, 0)
                returning id
                """.formatted(externalKey));
    }

    private long insertContributor(String displayName) throws Exception {
        return insertReturningId("""
                insert into contributors (display_name)
                values ('%s')
                returning id
                """.formatted(displayName));
    }

    private void insertAssignment(
            long contentId,
            long contributorId,
            String role,
            String languageCode,
            String creditName,
            int sortOrder) throws Exception {
        String languageValue = languageCode == null ? "null" : "'%s'".formatted(languageCode);
        execute("""
                insert into content_contributors (
                    content_id,
                    contributor_id,
                    role,
                    language_code,
                    credit_name,
                    sort_order
                )
                values (%d, %d, '%s', %s, '%s', %d)
                """.formatted(contentId, contributorId, role, languageValue, creditName, sortOrder));
    }

    private List<String> selectRoles(long contributorId) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                    select role
                    from contributor_roles
                    where contributor_id = %d
                    order by role
                    """.formatted(contributorId));
            var roles = new java.util.ArrayList<String>();
            while (resultSet.next()) {
                roles.add(resultSet.getString(1));
            }
            return roles;
        }
    }

    private List<AssignmentSnapshot> selectAssignments() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                    select id, content_id, contributor_id, role, language_code, credit_name, sort_order
                         , created_at, updated_at
                    from content_contributors
                    order by id
                    """);
            var assignments = new java.util.ArrayList<AssignmentSnapshot>();
            while (resultSet.next()) {
                assignments.add(new AssignmentSnapshot(
                        resultSet.getLong("id"),
                        resultSet.getLong("content_id"),
                        resultSet.getLong("contributor_id"),
                        resultSet.getString("role"),
                        resultSet.getString("language_code"),
                        resultSet.getString("credit_name"),
                        resultSet.getInt("sort_order"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant()));
            }
            return assignments;
        }
    }

    private String selectNormalizedDisplayName(long contributorId) throws Exception {
        return selectString("""
                select normalized_display_name
                from contributors
                where id = %d
                """.formatted(contributorId));
    }

    private List<String> selectContributorNames() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("select display_name from contributors order by id");
            var names = new java.util.ArrayList<String>();
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
            return names;
        }
    }

    private void updateContributorName(long contributorId, String displayName) throws Exception {
        execute("""
                update contributors
                set display_name = '%s'
                where id = %d
                """.formatted(displayName, contributorId));
    }

    private boolean tableExists(String tableName) throws Exception {
        return selectBoolean("""
                select exists (
                    select 1
                    from information_schema.tables
                    where table_schema = 'public' and table_name = '%s'
                )
                """.formatted(tableName));
    }

    private boolean columnExists(String tableName, String columnName) throws Exception {
        return selectBoolean("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = '%s'
                      and column_name = '%s'
                )
                """.formatted(tableName, columnName));
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

    private String selectString(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (!resultSet.next()) {
                throw new IllegalStateException("Query did not return a row");
            }
            return resultSet.getString(1);
        }
    }

    private boolean selectBoolean(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (!resultSet.next()) {
                throw new IllegalStateException("Query did not return a row");
            }
            return resultSet.getBoolean(1);
        }
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record AssignmentSnapshot(
            long id,
            long contentId,
            long contributorId,
            String role,
            String languageCode,
            String creditName,
            int sortOrder,
            Instant createdAt,
            Instant updatedAt) {
    }
}
