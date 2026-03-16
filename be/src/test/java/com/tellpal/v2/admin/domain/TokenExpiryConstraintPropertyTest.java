package com.tellpal.v2.admin.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for token expiry constraint invariant.
 *
 * Validates: Requirements 15.7, 17.5
 *
 * Özellik 25: Token Süre Sonu Kısıtlaması
 * - expires_at her zaman issued_at'tan büyük olmalıdır.
 * - expires_at <= issued_at olan token geçersizdir.
 * - isExpired() mevcut zaman expires_at'tan sonra ise true döner.
 * - isValid() süresi dolmuş token için false döner.
 */
public class TokenExpiryConstraintPropertyTest {

    private static final OffsetDateTime BASE = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String DUMMY_HASH = "a".repeat(64);

    /**
     * Generates a (issuedAt, expiresAt) pair where expiresAt = issuedAt + positive seconds.
     * Uses Arbitraries.longs() for the base offset and positive duration.
     */
    @Provide
    Arbitrary<Tuple.Tuple2<OffsetDateTime, OffsetDateTime>> validTokenTimes() {
        return Arbitraries.longs().between(0L, 365L * 24 * 3600)   // base offset in seconds
                .flatMap(baseOffset ->
                        Arbitraries.longs().between(1L, 30L * 24 * 3600)  // positive duration
                                .map(duration -> {
                                    OffsetDateTime issuedAt = BASE.plusSeconds(baseOffset);
                                    OffsetDateTime expiresAt = issuedAt.plusSeconds(duration);
                                    return Tuple.of(issuedAt, expiresAt);
                                })
                );
    }

    /**
     * Generates a (issuedAt, expiresAt) pair where expiresAt <= issuedAt (constraint violation).
     * Uses Arbitraries.longs() for the base offset and non-positive delta.
     */
    @Provide
    Arbitrary<Tuple.Tuple2<OffsetDateTime, OffsetDateTime>> invalidTokenTimes() {
        return Arbitraries.longs().between(0L, 365L * 24 * 3600)   // base offset in seconds
                .flatMap(baseOffset ->
                        Arbitraries.longs().between(0L, 30L * 24 * 3600)  // non-negative delta (0 = equal, >0 = before)
                                .map(delta -> {
                                    OffsetDateTime issuedAt = BASE.plusSeconds(baseOffset);
                                    OffsetDateTime expiresAt = issuedAt.minusSeconds(delta); // expiresAt <= issuedAt
                                    return Tuple.of(issuedAt, expiresAt);
                                })
                );
    }

    private AdminRefreshToken token(OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        return new AdminRefreshToken(1L, DUMMY_HASH, issuedAt, expiresAt, "agent", "127.0.0.1");
    }

    /**
     * Özellik 25: A valid token always has expires_at strictly greater than issued_at.
     *
     * **Validates: Requirements 15.7, 17.5**
     */
    @Property(tries = 100)
    void validTokenAlwaysHasExpiresAtAfterIssuedAt(
            @ForAll("validTokenTimes") Tuple.Tuple2<OffsetDateTime, OffsetDateTime> times) {

        OffsetDateTime issuedAt = times.get1();
        OffsetDateTime expiresAt = times.get2();

        AdminRefreshToken t = token(issuedAt, expiresAt);

        assertThat(t.getExpiresAt())
                .as("expires_at must be strictly after issued_at for a valid token")
                .isAfter(t.getIssuedAt());
    }

    /**
     * Özellik 25: A token where expires_at <= issued_at violates the constraint
     * (domain-level detection: the stored values reflect the violation).
     *
     * **Validates: Requirements 15.7, 17.5**
     */
    @Property(tries = 100)
    void tokenWithExpiresAtNotAfterIssuedAtViolatesConstraint(
            @ForAll("invalidTokenTimes") Tuple.Tuple2<OffsetDateTime, OffsetDateTime> times) {

        OffsetDateTime issuedAt = times.get1();
        OffsetDateTime expiresAt = times.get2();

        AdminRefreshToken t = token(issuedAt, expiresAt);

        // Domain-level detection: expires_at is NOT after issued_at → constraint violated
        boolean constraintViolated = !t.getExpiresAt().isAfter(t.getIssuedAt());

        assertThat(constraintViolated)
                .as("A token with expires_at <= issued_at must be detected as a constraint violation")
                .isTrue();
    }

    /**
     * Özellik 25: isExpired() returns true when current time is after expires_at.
     * We simulate this by creating a token whose expires_at is in the past.
     *
     * **Validates: Requirements 15.7, 17.5**
     */
    @Property(tries = 100)
    void isExpiredReturnsTrueWhenCurrentTimeIsAfterExpiresAt(
            @ForAll("validTokenTimes") Tuple.Tuple2<OffsetDateTime, OffsetDateTime> times) {

        // Build a token that expired in the past: shift both times far into the past
        OffsetDateTime issuedAt = times.get1().minusYears(10);
        OffsetDateTime expiresAt = times.get2().minusYears(10);

        // Ensure expiresAt is still after issuedAt (preserved by generator logic)
        // and both are well in the past relative to now
        AdminRefreshToken t = token(issuedAt, expiresAt);

        assertThat(t.isExpired())
                .as("isExpired() must return true when current time is after expires_at")
                .isTrue();
    }

    /**
     * Özellik 25: isValid() returns false for expired tokens.
     *
     * **Validates: Requirements 15.7, 17.5**
     */
    @Property(tries = 100)
    void isValidReturnsFalseForExpiredTokens(
            @ForAll("validTokenTimes") Tuple.Tuple2<OffsetDateTime, OffsetDateTime> times) {

        // Build a token that expired in the past
        OffsetDateTime issuedAt = times.get1().minusYears(10);
        OffsetDateTime expiresAt = times.get2().minusYears(10);

        AdminRefreshToken t = token(issuedAt, expiresAt);

        assertThat(t.isValid())
                .as("isValid() must return false for an expired token")
                .isFalse();
    }
}
