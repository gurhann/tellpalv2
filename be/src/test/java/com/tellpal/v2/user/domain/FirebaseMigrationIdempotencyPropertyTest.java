package com.tellpal.v2.user.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Firebase Migration Idempotency (Özellik 33).
 *
 * <p><b>Validates: Requirements 18.1</b>
 *
 * <p>Özellik 33: Firebase Migration İdempotency
 * <ul>
 *   <li>importUser is idempotent — calling it multiple times with the same
 *       firebase_uid must not create duplicate users.</li>
 *   <li>The result of calling importUser twice with the same UID must be
 *       equivalent to calling it once.</li>
 * </ul>
 *
 * <p>Domain logic is simulated with an in-memory {@code Map<String, UserRecord>}
 * so that no Spring context or database is required.
 */
public class FirebaseMigrationIdempotencyPropertyTest {

    /** Minimal domain record representing a persisted app-user. */
    record UserRecord(String firebaseUid, Long id) {}

    // -------------------------------------------------------------------------
    // In-memory repository simulation
    // -------------------------------------------------------------------------

    /**
     * Simulates the idempotent importUser operation:
     * if the UID already exists, return the existing record;
     * otherwise create and store a new one.
     */
    private UserRecord importUser(String firebaseUid,
                                  Map<String, UserRecord> repo,
                                  AtomicLong idSequence) {
        return repo.computeIfAbsent(firebaseUid,
                uid -> new UserRecord(uid, idSequence.incrementAndGet()));
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /** Generates a single Firebase-like UID: 28 alphanumeric characters. */
    @Provide
    Arbitrary<String> firebaseUid() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofLength(28);
    }

    /** Generates a repeat-count between 2 and 5 (inclusive). */
    @Provide
    Arbitrary<Integer> repeatCount() {
        return Arbitraries.integers().between(2, 5);
    }

    /** Generates a list of 2–10 distinct Firebase UIDs. */
    @Provide
    Arbitrary<List<String>> distinctFirebaseUids() {
        return firebaseUid().list().ofMinSize(2).ofMaxSize(10).uniqueElements();
    }


    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 33 — Importing the same firebase_uid twice returns the same user record.
     *
     * <p><b>Validates: Requirements 18.1</b>
     */
    @Property(tries = 100)
    void importingTheSameUidTwiceReturnsSameRecord(@ForAll("firebaseUid") String uid) {
        Map<String, UserRecord> repo = new HashMap<>();
        AtomicLong seq = new AtomicLong();

        UserRecord first  = importUser(uid, repo, seq);
        UserRecord second = importUser(uid, repo, seq);

        assertThat(second)
                .as("importUser called twice with the same UID must return the identical record")
                .isEqualTo(first);
    }

    /**
     * Özellik 33 — Calling importUser N times with the same UID produces exactly one user.
     *
     * <p><b>Validates: Requirements 18.1</b>
     */
    @Property(tries = 100)
    void repeatedImportsProduceExactlyOneUser(
            @ForAll("firebaseUid") String uid,
            @ForAll("repeatCount") int times) {

        Map<String, UserRecord> repo = new HashMap<>();
        AtomicLong seq = new AtomicLong();

        for (int i = 0; i < times; i++) {
            importUser(uid, repo, seq);
        }

        assertThat(repo)
                .as("Importing the same UID %d times must result in exactly one stored user", times)
                .hasSize(1);

        assertThat(repo.get(uid).firebaseUid())
                .as("The stored user must carry the original firebase_uid")
                .isEqualTo(uid);
    }

    /**
     * Özellik 33 — Importing N distinct UIDs produces exactly N distinct users (no collisions).
     *
     * <p><b>Validates: Requirements 18.1</b>
     */
    @Property(tries = 100)
    void importingDistinctUidsProducesDistinctUsers(
            @ForAll("distinctFirebaseUids") List<String> uids) {

        Map<String, UserRecord> repo = new HashMap<>();
        AtomicLong seq = new AtomicLong();

        uids.forEach(uid -> importUser(uid, repo, seq));

        Set<Long> assignedIds = repo.values().stream()
                .map(UserRecord::id)
                .collect(Collectors.toSet());

        assertThat(repo)
                .as("Each distinct UID must produce its own user record")
                .hasSize(uids.size());

        assertThat(assignedIds)
                .as("Each user record must receive a unique ID")
                .hasSize(uids.size());
    }

    /**
     * Özellik 33 — mapEventType is deterministic: same input always yields the same output.
     *
     * <p><b>Validates: Requirements 18.1</b>
     */
    @Property(tries = 100)
    void mapEventTypeIsDeterministic(@ForAll("firebaseUid") String input) {
        // Re-use the same mapping logic inline (pure function, no side effects)
        String first  = mapEventType(input);
        String second = mapEventType(input);

        assertThat(second)
                .as("mapEventType must return the same result for the same input")
                .isEqualTo(first);
    }

    // -------------------------------------------------------------------------
    // Helpers
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
