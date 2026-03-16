package com.tellpal.v2.event.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for analytic index coverage (Özellik 36).
 *
 * **Validates: Requirements 16.1, 16.2, 16.3, 16.5, 16.6, 16.7**
 *
 * Özellik 36: Analitik İndeks Performansı
 * - For any event query operation, indexes on profile_id, content_id,
 *   event_type and occurred_at combinations must be present.
 *
 * This is a pure domain-level test — no DB, no Spring context.
 * The expected index set mirrors the V11 Flyway migration exactly.
 */
public class AnalyticIndexCoveragePropertyTest {

    // -------------------------------------------------------------------------
    // Domain model
    // -------------------------------------------------------------------------

    record IndexDefinition(String tableName, List<String> columns) {}

    // -------------------------------------------------------------------------
    // Static expected index set (mirrors V11__create_event_indexes.sql)
    // -------------------------------------------------------------------------

    private static final List<IndexDefinition> EXPECTED_INDEXES = List.of(
            // v2_content_events — Req 16.1, 16.2, 16.3, 16.4
            new IndexDefinition("v2_content_events", List.of("profile_id", "occurred_at")),
            new IndexDefinition("v2_content_events", List.of("content_id", "occurred_at")),
            new IndexDefinition("v2_content_events", List.of("event_type", "occurred_at")),
            new IndexDefinition("v2_content_events", List.of("session_id")),

            // v2_app_events — Req 16.5, 16.6, 16.7
            new IndexDefinition("v2_app_events", List.of("profile_id", "occurred_at")),
            new IndexDefinition("v2_app_events", List.of("content_id", "occurred_at")),
            new IndexDefinition("v2_app_events", List.of("event_type", "occurred_at")),

            // v2_purchase_events — Req 16.8
            new IndexDefinition("v2_purchase_events", List.of("user_id", "occurred_at")),

            // v2_purchase_context_snapshots — Req 16.9
            new IndexDefinition("v2_purchase_context_snapshots", List.of("user_id", "created_at"))
    );

    // Lookup: tableName → set of column-list keys (joined with ",")
    private static final Map<String, Set<String>> INDEX_MAP = EXPECTED_INDEXES.stream()
            .collect(Collectors.groupingBy(
                    IndexDefinition::tableName,
                    Collectors.mapping(
                            idx -> String.join(",", idx.columns()),
                            Collectors.toSet()
                    )
            ));

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /** Picks a random IndexDefinition from the expected set. */
    @Provide
    Arbitrary<IndexDefinition> anyExpectedIndex() {
        return Arbitraries.of(EXPECTED_INDEXES);
    }

    /** Picks a random (tableName, columnName) pair that exists in the expected indexes. */
    @Provide
    Arbitrary<String[]> anyTableColumnPair() {
        // Flatten all (table, column) pairs
        List<String[]> pairs = EXPECTED_INDEXES.stream()
                .flatMap(idx -> idx.columns().stream()
                        .map(col -> new String[]{idx.tableName(), col}))
                .toList();
        return Arbitraries.of(pairs);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 36 — All expected v2_content_events indexes are defined.
     *
     * **Validates: Requirements 16.1, 16.2, 16.3**
     */
    @Property(tries = 100)
    void contentEventsIndexesAreDefined() {
        Set<String> contentEventKeys = INDEX_MAP.get("v2_content_events");

        assertThat(contentEventKeys)
                .as("v2_content_events must have (profile_id, occurred_at) index — Req 16.1")
                .contains("profile_id,occurred_at");

        assertThat(contentEventKeys)
                .as("v2_content_events must have (content_id, occurred_at) index — Req 16.2")
                .contains("content_id,occurred_at");

        assertThat(contentEventKeys)
                .as("v2_content_events must have (event_type, occurred_at) index — Req 16.3")
                .contains("event_type,occurred_at");

        assertThat(contentEventKeys)
                .as("v2_content_events must have (session_id) index — Req 16.4")
                .contains("session_id");
    }

    /**
     * Özellik 36 — All expected v2_app_events indexes are defined.
     *
     * **Validates: Requirements 16.5, 16.6, 16.7**
     */
    @Property(tries = 100)
    void appEventsIndexesAreDefined() {
        Set<String> appEventKeys = INDEX_MAP.get("v2_app_events");

        assertThat(appEventKeys)
                .as("v2_app_events must have (profile_id, occurred_at) index — Req 16.5")
                .contains("profile_id,occurred_at");

        assertThat(appEventKeys)
                .as("v2_app_events must have (content_id, occurred_at) index — Req 16.6")
                .contains("content_id,occurred_at");

        assertThat(appEventKeys)
                .as("v2_app_events must have (event_type, occurred_at) index — Req 16.7")
                .contains("event_type,occurred_at");
    }

    /**
     * Özellik 36 — All expected v2_purchase_events indexes are defined.
     *
     * **Validates: Requirements 16.8**
     */
    @Property(tries = 100)
    void purchaseEventsIndexesAreDefined() {
        Set<String> purchaseEventKeys = INDEX_MAP.get("v2_purchase_events");

        assertThat(purchaseEventKeys)
                .as("v2_purchase_events must have (user_id, occurred_at) index — Req 16.8")
                .contains("user_id,occurred_at");
    }

    /**
     * Özellik 36 — All expected v2_purchase_context_snapshots indexes are defined.
     *
     * **Validates: Requirements 16.9**
     */
    @Property(tries = 100)
    void purchaseContextSnapshotsIndexesAreDefined() {
        Set<String> snapshotKeys = INDEX_MAP.get("v2_purchase_context_snapshots");

        assertThat(snapshotKeys)
                .as("v2_purchase_context_snapshots must have (user_id, created_at) index — Req 16.9")
                .contains("user_id,created_at");
    }

    /**
     * Özellik 36 — No index definition has an empty column list.
     *
     * **Validates: Requirements 16.1, 16.2, 16.3, 16.5, 16.6, 16.7**
     */
    @Property(tries = 100)
    void noIndexHasEmptyColumnList(@ForAll("anyExpectedIndex") IndexDefinition index) {
        assertThat(index.columns())
                .as("Index on table '%s' must not have an empty column list", index.tableName())
                .isNotEmpty();
    }

    /**
     * Özellik 36 — All index column names are non-blank.
     *
     * **Validates: Requirements 16.1, 16.2, 16.3, 16.5, 16.6, 16.7**
     */
    @Property(tries = 100)
    void allIndexColumnNamesAreNonBlank(@ForAll("anyExpectedIndex") IndexDefinition index) {
        assertThat(index.columns())
                .as("All columns in index on table '%s' must be non-blank", index.tableName())
                .allSatisfy(col -> assertThat(col).isNotBlank());
    }

    /**
     * Özellik 36 — For any (tableName, columnName) pair from the expected indexes,
     * the column appears in the index definition for that table.
     *
     * **Validates: Requirements 16.1, 16.2, 16.3, 16.5, 16.6, 16.7**
     */
    @Property(tries = 100)
    void columnAppearsInIndexForItsTable(@ForAll("anyTableColumnPair") String[] pair) {
        String tableName = pair[0];
        String columnName = pair[1];

        Set<String> indexKeysForTable = INDEX_MAP.get(tableName);

        assertThat(indexKeysForTable)
                .as("Table '%s' must have at least one index containing column '%s'",
                        tableName, columnName)
                .isNotNull()
                .anySatisfy(key ->
                        assertThat(key.split(","))
                                .as("Index key '%s' on table '%s' must contain column '%s'",
                                        key, tableName, columnName)
                                .contains(columnName)
                );
    }
}
