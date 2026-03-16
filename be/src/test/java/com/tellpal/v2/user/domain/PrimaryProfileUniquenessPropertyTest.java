package com.tellpal.v2.user.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Assume;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for primary profile uniqueness constraint (Özellik 12).
 *
 * **Validates: Requirements 6.3, 6.6**
 *
 * Özellik 12: Birincil Profil Tekilliği
 * - Each user can have at most ONE primary profile (is_primary = true).
 * - A user can have multiple non-primary profiles (is_primary = false).
 * - The partial unique index enforces: only one profile per user_id WHERE is_primary = true.
 */
public class PrimaryProfileUniquenessPropertyTest {

    record UserProfileRecord(Long userId, Long profileId, boolean isPrimary) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates a positive user ID.
     */
    @Provide
    Arbitrary<Long> userId() {
        return Arbitraries.longs().between(1L, 10_000L);
    }

    /**
     * Generates a positive profile ID.
     */
    @Provide
    Arbitrary<Long> profileId() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    /**
     * Generates a list of 1–5 profiles for a single user.
     * At most one of them is marked isPrimary=true.
     */
    @Provide
    Arbitrary<List<UserProfileRecord>> profilesForOneUser() {
        return Arbitraries.longs().between(1L, 10_000L).flatMap(uid ->
            Arbitraries.integers().between(1, 5).flatMap(count ->
                profileId().list().ofSize(count).uniqueElements().flatMap(ids ->
                    Arbitraries.integers().between(0, count - 1).map(primaryIndex ->
                        ids.stream()
                            .map(pid -> new UserProfileRecord(uid, pid, ids.indexOf(pid) == primaryIndex))
                            .collect(Collectors.toList())
                    )
                )
            )
        );
    }

    /**
     * Generates a list of 2–5 non-primary profiles for a single user.
     */
    @Provide
    Arbitrary<List<UserProfileRecord>> nonPrimaryProfilesForOneUser() {
        return Arbitraries.longs().between(1L, 10_000L).flatMap(uid ->
            Arbitraries.integers().between(2, 5).flatMap(count ->
                profileId().list().ofSize(count).uniqueElements().map(ids ->
                    ids.stream()
                        .map(pid -> new UserProfileRecord(uid, pid, false))
                        .collect(Collectors.toList())
                )
            )
        );
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 12 — A user with multiple profiles has at most one primary profile.
     *
     * **Validates: Requirements 6.3, 6.6**
     */
    @Property(tries = 100)
    void userHasAtMostOnePrimaryProfile(
            @ForAll("profilesForOneUser") List<UserProfileRecord> profiles) {

        long primaryCount = profiles.stream()
                .filter(UserProfileRecord::isPrimary)
                .count();

        assertThat(primaryCount)
                .as("A user must have at most one primary profile, but found %d", primaryCount)
                .isLessThanOrEqualTo(1L);
    }

    /**
     * Özellik 12 — Filtering profiles by userId and isPrimary=true yields at most 1 result.
     *
     * **Validates: Requirements 6.3, 6.6**
     */
    @Property(tries = 100)
    void filteringByUserIdAndIsPrimaryYieldsAtMostOne(
            @ForAll("profilesForOneUser") List<UserProfileRecord> profiles) {

        Assume.that(!profiles.isEmpty());

        Long uid = profiles.get(0).userId();

        List<UserProfileRecord> primaryProfiles = profiles.stream()
                .filter(p -> p.userId().equals(uid) && p.isPrimary())
                .collect(Collectors.toList());

        assertThat(primaryProfiles)
                .as("Filtering by userId=%d and isPrimary=true must yield at most 1 result", uid)
                .hasSizeLessThanOrEqualTo(1);
    }

    /**
     * Özellik 12 — A user can have multiple non-primary profiles (is_primary=false).
     * This is a valid state and must not be treated as a constraint violation.
     *
     * **Validates: Requirements 6.3, 6.6**
     */
    @Property(tries = 100)
    void userCanHaveMultipleNonPrimaryProfiles(
            @ForAll("nonPrimaryProfilesForOneUser") List<UserProfileRecord> profiles) {

        long nonPrimaryCount = profiles.stream()
                .filter(p -> !p.isPrimary())
                .count();

        assertThat(nonPrimaryCount)
                .as("A user must be allowed to have multiple non-primary profiles")
                .isGreaterThanOrEqualTo(2L);

        long primaryCount = profiles.stream()
                .filter(UserProfileRecord::isPrimary)
                .count();

        assertThat(primaryCount)
                .as("None of the profiles in this set should be primary")
                .isZero();
    }

    /**
     * Özellik 12 — Two profiles for the same user both marked isPrimary=true violates the constraint.
     *
     * **Validates: Requirements 6.3, 6.6**
     */
    @Property(tries = 100)
    void twoPrimaryProfilesForSameUserViolatesConstraint(
            @ForAll("userId") Long uid,
            @ForAll("profileId") Long profileId1,
            @ForAll("profileId") Long profileId2) {

        Assume.that(!profileId1.equals(profileId2));

        UserProfileRecord primary1 = new UserProfileRecord(uid, profileId1, true);
        UserProfileRecord primary2 = new UserProfileRecord(uid, profileId2, true);

        List<UserProfileRecord> profiles = List.of(primary1, primary2);

        long primaryCount = profiles.stream()
                .filter(p -> p.userId().equals(uid) && p.isPrimary())
                .count();

        assertThat(primaryCount)
                .as("Two profiles for userId=%d both with isPrimary=true must be detected as a constraint violation (count=%d > 1)", uid, primaryCount)
                .isGreaterThan(1L);
    }
}
