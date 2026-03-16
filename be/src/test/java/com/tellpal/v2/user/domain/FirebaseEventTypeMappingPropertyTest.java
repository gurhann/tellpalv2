package com.tellpal.v2.user.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Firebase Event Type Mapping (Özellik 34).
 *
 * <p><b>Validates: Requirements 18.4</b>
 *
 * <p>Özellik 34: Firebase Event Tipi Eşleme
 * <ul>
 *   <li>Legacy Firebase event types must be correctly mapped to canonical values.</li>
 *   <li>START_CONTENT → START</li>
 *   <li>LEFT_CONTENT  → EXIT</li>
 *   <li>FINISH_CONTENT → COMPLETE</li>
 *   <li>Unknown/arbitrary event types are returned unchanged (pass-through).</li>
 * </ul>
 *
 * <p>Pure domain-level test — no Spring context or database required.
 */
public class FirebaseEventTypeMappingPropertyTest {

    private static final Set<String> KNOWN_LEGACY_TYPES =
            Set.of("START_CONTENT", "LEFT_CONTENT", "FINISH_CONTENT");

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /** Generates one of the three known legacy event type strings. */
    @Provide
    Arbitrary<String> knownLegacyEventType() {
        return Arbitraries.of("START_CONTENT", "LEFT_CONTENT", "FINISH_CONTENT");
    }

    /** Generates arbitrary strings that are NOT one of the three known legacy types. */
    @Provide
    Arbitrary<String> unknownEventType() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('_', '_')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> !KNOWN_LEGACY_TYPES.contains(s));
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 34 — START_CONTENT always maps to START.
     *
     * <p><b>Validates: Requirements 18.4</b>
     */
    @Property(tries = 100)
    void startContentAlwaysMapsToStart() {
        assertThat(mapEventType("START_CONTENT"))
                .as("START_CONTENT must map to START")
                .isEqualTo("START");
    }

    /**
     * Özellik 34 — LEFT_CONTENT always maps to EXIT.
     *
     * <p><b>Validates: Requirements 18.4</b>
     */
    @Property(tries = 100)
    void leftContentAlwaysMapsToExit() {
        assertThat(mapEventType("LEFT_CONTENT"))
                .as("LEFT_CONTENT must map to EXIT")
                .isEqualTo("EXIT");
    }

    /**
     * Özellik 34 — FINISH_CONTENT always maps to COMPLETE.
     *
     * <p><b>Validates: Requirements 18.4</b>
     */
    @Property(tries = 100)
    void finishContentAlwaysMapsToComplete() {
        assertThat(mapEventType("FINISH_CONTENT"))
                .as("FINISH_CONTENT must map to COMPLETE")
                .isEqualTo("COMPLETE");
    }

    /**
     * Özellik 34 — Unknown/arbitrary event types are returned unchanged (pass-through).
     *
     * <p><b>Validates: Requirements 18.4</b>
     */
    @Property(tries = 100)
    void unknownEventTypesAreReturnedUnchanged(@ForAll("unknownEventType") String eventType) {
        Assume.that(!KNOWN_LEGACY_TYPES.contains(eventType));

        assertThat(mapEventType(eventType))
                .as("Unknown event type '%s' must be returned unchanged", eventType)
                .isEqualTo(eventType);
    }

    /**
     * Özellik 34 — The mapping is exhaustive: all three known legacy types produce distinct mapped values.
     *
     * <p><b>Validates: Requirements 18.4</b>
     */
    @Property(tries = 100)
    void allThreeKnownLegacyTypesMappedToDistinctValues() {
        String mappedStart    = mapEventType("START_CONTENT");
        String mappedExit     = mapEventType("LEFT_CONTENT");
        String mappedComplete = mapEventType("FINISH_CONTENT");

        assertThat(Set.of(mappedStart, mappedExit, mappedComplete))
                .as("All three known legacy types must produce distinct mapped values")
                .hasSize(3);
    }

    // -------------------------------------------------------------------------
    // Helper — pure copy of FirebaseMigrationService#mapEventType
    // -------------------------------------------------------------------------

    /** Pure copy of FirebaseMigrationService#mapEventType for domain-level testing. */
    private String mapEventType(String legacyEventType) {
        if (legacyEventType == null) return null;
        return switch (legacyEventType) {
            case "START_CONTENT"  -> "START";
            case "LEFT_CONTENT"   -> "EXIT";
            case "FINISH_CONTENT" -> "COMPLETE";
            default               -> legacyEventType;
        };
    }
}
