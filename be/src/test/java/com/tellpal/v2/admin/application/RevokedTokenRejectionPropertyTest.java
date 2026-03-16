package com.tellpal.v2.admin.application;

import com.tellpal.v2.admin.domain.AdminRefreshToken;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for revoked token rejection invariants.
 *
 * Validates: Requirement 15.4
 *
 * Özellik 24: İptal Edilmiş Token Reddi
 * - revokedAt alanı set edilmiş bir token her zaman reddedilmelidir
 *   (isValid() = false, isRevoked() = true).
 *
 * No Spring context — pure AdminRefreshToken domain object tests.
 */
public class RevokedTokenRejectionPropertyTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime FUTURE = NOW.plusDays(30);
    private static final OffsetDateTime PAST = NOW.minusDays(1);

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a valid (not expired, not revoked) token. */
    private AdminRefreshToken validToken() {
        return new AdminRefreshToken(1L, "hash-valid", NOW, FUTURE, "agent", "127.0.0.1");
    }

    /** Creates an already-expired token (expiresAt in the past). */
    private AdminRefreshToken expiredToken() {
        return new AdminRefreshToken(1L, "hash-expired", PAST.minusDays(30), PAST, "agent", "127.0.0.1");
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates random OffsetDateTime values by mapping random longs to epoch-seconds.
     * Covers past, present, and future timestamps.
     */
    @Provide
    Arbitrary<OffsetDateTime> randomOffsetDateTimes() {
        return Arbitraries.longs()
                .between(-31_536_000L, 31_536_000L)   // ±1 year in seconds from now
                .map(offsetSeconds -> NOW.plusSeconds(offsetSeconds));
    }

    /** Generates OffsetDateTime values strictly in the past (revokedAt ≤ now). */
    @Provide
    Arbitrary<OffsetDateTime> pastOrPresentDateTimes() {
        return Arbitraries.longs()
                .between(-31_536_000L, 0L)
                .map(offsetSeconds -> NOW.plusSeconds(offsetSeconds));
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 24 — A token with revokedAt set is always rejected:
     *  isValid() = false, isRevoked() = true.
     *
     * **Validates: Requirements 15.4**
     */
    @Property(tries = 100)
    void tokenWithRevokedAtIsAlwaysRejected(@ForAll("randomOffsetDateTimes") OffsetDateTime revokedAt) {
        AdminRefreshToken token = validToken();
        token.setRevokedAt(revokedAt);

        assertThat(token.isRevoked())
                .as("isRevoked() must be true when revokedAt is set (revokedAt=%s)", revokedAt)
                .isTrue();

        assertThat(token.isValid())
                .as("isValid() must be false when revokedAt is set (revokedAt=%s)", revokedAt)
                .isFalse();
    }

    /**
     * Özellik 24 — A token revoked at any point in the past or present is always invalid.
     *
     * **Validates: Requirements 15.4**
     */
    @Property(tries = 100)
    void tokenRevokedAtAnyPastTimeIsInvalid(@ForAll("pastOrPresentDateTimes") OffsetDateTime revokedAt) {
        AdminRefreshToken token = validToken();
        token.setRevokedAt(revokedAt);

        assertThat(token.isValid())
                .as("A token revoked at past/present time must be invalid (revokedAt=%s)", revokedAt)
                .isFalse();

        assertThat(token.isRevoked())
                .as("isRevoked() must be true for past/present revokedAt (revokedAt=%s)", revokedAt)
                .isTrue();
    }

    /**
     * Özellik 24 — A token that is both expired AND revoked is invalid.
     *
     * **Validates: Requirements 15.4**
     */
    @Property(tries = 100)
    void expiredAndRevokedTokenIsInvalid(@ForAll("randomOffsetDateTimes") OffsetDateTime revokedAt) {
        AdminRefreshToken token = expiredToken();
        token.setRevokedAt(revokedAt);

        assertThat(token.isExpired())
                .as("Token must be expired (expiresAt is in the past)")
                .isTrue();

        assertThat(token.isRevoked())
                .as("Token must be revoked when revokedAt is set")
                .isTrue();

        assertThat(token.isValid())
                .as("A token that is both expired and revoked must be invalid")
                .isFalse();
    }

    /**
     * Özellik 24 — A valid token becomes invalid immediately after revocation.
     *
     * **Validates: Requirements 15.4**
     */
    @Property(tries = 100)
    void validTokenBecomesInvalidAfterRevocation(@ForAll("randomOffsetDateTimes") OffsetDateTime revokedAt) {
        AdminRefreshToken token = validToken();

        assertThat(token.isValid())
                .as("Token must be valid before revocation")
                .isTrue();

        assertThat(token.isRevoked())
                .as("Token must not be revoked before revokedAt is set")
                .isFalse();

        // Revoke the token
        token.setRevokedAt(revokedAt);

        assertThat(token.isValid())
                .as("Token must be invalid immediately after revocation (revokedAt=%s)", revokedAt)
                .isFalse();

        assertThat(token.isRevoked())
                .as("isRevoked() must be true immediately after revocation")
                .isTrue();
    }

    /**
     * Özellik 24 — The revokedAt timestamp is always preserved after being set (immutability of revocation).
     *
     * **Validates: Requirements 15.4**
     */
    @Property(tries = 100)
    void revokedAtTimestampIsPreservedAfterBeingSet(@ForAll("randomOffsetDateTimes") OffsetDateTime revokedAt) {
        AdminRefreshToken token = validToken();
        token.setRevokedAt(revokedAt);

        assertThat(token.getRevokedAt())
                .as("getRevokedAt() must return the exact timestamp that was set")
                .isEqualTo(revokedAt);

        // Reading it again must return the same value (no mutation side-effects)
        assertThat(token.getRevokedAt())
                .as("getRevokedAt() must be stable across multiple reads")
                .isEqualTo(revokedAt);
    }
}
