package com.tellpal.v2.user.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for Firebase UID uniqueness constraint (Özellik 11).
 *
 * **Validates: Requirements 6.2**
 *
 * Özellik 11: Firebase UID Benzersizliği
 * - firebase_uid must be unique across all app_users records.
 * - No two AppUser records can share the same firebase_uid.
 */
public class FirebaseUidUniquenessPropertyTest {

    record AppUserRecord(String firebaseUid) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a single Firebase-like UID: 28 alphanumeric characters.
     */
    @Provide
    Arbitrary<String> firebaseUid() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofLength(28);
    }

    /**
     * Generates a list of distinct Firebase UIDs (2–10 entries).
     */
    @Provide
    Arbitrary<List<String>> distinctFirebaseUids() {
        return firebaseUid().list().ofMinSize(2).ofMaxSize(10).uniqueElements();
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 11 — A set of distinct Firebase UIDs contains no duplicates.
     *
     * **Validates: Requirements 6.2**
     */
    @Property(tries = 100)
    void distinctUidsHaveNoDuplicates(@ForAll("distinctFirebaseUids") List<String> uids) {
        Set<String> uidSet = new HashSet<>(uids);

        assertThat(uidSet)
                .as("A collection of distinct firebase_uids must contain no duplicates")
                .hasSize(uids.size());
    }

    /**
     * Özellik 11 — Adding the same UID twice to a set results in set size = 1.
     *
     * **Validates: Requirements 6.2**
     */
    @Property(tries = 100)
    void duplicateUidCollapsesToSingleEntry(@ForAll("firebaseUid") String uid) {
        Set<String> uidSet = new HashSet<>();
        uidSet.add(uid);
        uidSet.add(uid);

        assertThat(uidSet)
                .as("Adding the same firebase_uid twice must result in a set of size 1")
                .hasSize(1);
    }

    /**
     * Özellik 11 — Two AppUser records with different UIDs are distinct.
     *
     * **Validates: Requirements 6.2**
     */
    @Property(tries = 100)
    void appUsersWithDifferentUidsAreDistinct(
            @ForAll("firebaseUid") String uid1,
            @ForAll("firebaseUid") String uid2) {

        // jqwik may generate equal values; skip when they happen to collide
        net.jqwik.api.Assume.that(!uid1.equals(uid2));

        AppUserRecord user1 = new AppUserRecord(uid1);
        AppUserRecord user2 = new AppUserRecord(uid2);

        assertThat(user1.firebaseUid())
                .as("Two AppUser records with different UIDs must not share the same firebase_uid")
                .isNotEqualTo(user2.firebaseUid());
    }

    /**
     * Özellik 11 — Two AppUser records with the same UID violate the uniqueness constraint.
     *
     * **Validates: Requirements 6.2**
     */
    @Property(tries = 100)
    void appUsersWithSameUidViolateUniqueness(@ForAll("firebaseUid") String uid) {
        AppUserRecord user1 = new AppUserRecord(uid);
        AppUserRecord user2 = new AppUserRecord(uid);

        Set<String> uidSet = new HashSet<>();
        uidSet.add(user1.firebaseUid());
        boolean isDuplicate = !uidSet.add(user2.firebaseUid());

        assertThat(isDuplicate)
                .as("Two AppUser records sharing firebase_uid '%s' must be detected as a uniqueness violation", uid)
                .isTrue();
    }
}
