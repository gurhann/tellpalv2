package com.tellpal.v2.admin.application;

import com.tellpal.v2.admin.domain.AdminRefreshToken;
import com.tellpal.v2.admin.infrastructure.security.JwtTokenProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for refresh token rotation chain invariants.
 *
 * Validates: Requirement 15.2
 *
 * Özellik 23: Refresh Token Rotasyon Zinciri
 * - Rotasyon yoluyla yeni bir yenileme token'ı verildiğinde, eski token iptal edilmeli
 *   ve yeni token kaydında replaced_by_token_hash alanı yeni token'ın hash'ini içermelidir.
 *
 * No Spring context — pure domain objects + JwtTokenProvider instantiated directly.
 */
public class RefreshTokenRotationPropertyTest {

    private static final String TEST_SECRET = "test-secret-key-for-property-tests-min-32-chars!!";
    private static final JwtTokenProvider JWT = new JwtTokenProvider(TEST_SECRET, 1L);

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime FUTURE = NOW.plusDays(30);

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a fresh, valid AdminRefreshToken using a real UUID-based raw token. */
    private AdminRefreshToken freshToken(long userId) {
        String raw = JWT.generateRefreshToken();
        String hash = JWT.hashToken(raw);
        return new AdminRefreshToken(userId, hash, NOW, FUTURE, "agent", "127.0.0.1");
    }

    /**
     * Simulates a single rotation step:
     *  - revokes oldToken (sets revokedAt, sets replacedByTokenHash = newToken.tokenHash)
     *  - returns the new token
     */
    private AdminRefreshToken rotate(AdminRefreshToken oldToken, AdminRefreshToken newToken) {
        oldToken.setRevokedAt(OffsetDateTime.now());
        oldToken.setReplacedByTokenHash(newToken.getTokenHash());
        return newToken;
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /** Generates a chain length N in [1, 10]. */
    @Provide
    Arbitrary<Integer> chainLengths() {
        return Arbitraries.integers().between(1, 10);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 23 — Single rotation invariants:
     *  1. Old token's revokedAt is not null after rotation.
     *  2. Old token's replacedByTokenHash equals the hash of the new token.
     *  3. New token is valid (not expired, not revoked).
     *  4. Old token is no longer valid (isValid() = false).
     *
     * **Validates: Requirements 15.2**
     */
    @Property(tries = 100)
    void singleRotationInvariants() {
        AdminRefreshToken oldToken = freshToken(1L);
        AdminRefreshToken newToken = freshToken(1L);

        // Pre-conditions
        assertThat(oldToken.isValid()).as("old token must be valid before rotation").isTrue();
        assertThat(newToken.isValid()).as("new token must be valid before rotation").isTrue();

        // Perform rotation
        rotate(oldToken, newToken);

        // 1. Old token's revokedAt is not null
        assertThat(oldToken.getRevokedAt())
                .as("old token revokedAt must not be null after rotation")
                .isNotNull();

        // 2. Old token's replacedByTokenHash equals the new token's hash
        assertThat(oldToken.getReplacedByTokenHash())
                .as("old token replacedByTokenHash must equal new token's hash")
                .isEqualTo(newToken.getTokenHash());

        // 3. New token is still valid
        assertThat(newToken.isValid())
                .as("new token must be valid after rotation")
                .isTrue();

        // 4. Old token is no longer valid
        assertThat(oldToken.isValid())
                .as("old token must be invalid (revoked) after rotation")
                .isFalse();
    }

    /**
     * Özellik 23 — Chain of N rotations produces exactly N+1 tokens where:
     *  - All tokens except the last are revoked.
     *  - Each revoked token's replacedByTokenHash points to the next token's hash.
     *  - Only the last token is valid.
     *
     * **Validates: Requirements 15.2**
     */
    @Property(tries = 50)
    void chainOfNRotationsInvariants(@ForAll("chainLengths") int n) {
        // Build chain: token[0] → token[1] → ... → token[n]
        List<AdminRefreshToken> chain = new ArrayList<>(n + 1);
        chain.add(freshToken(1L));
        for (int i = 0; i < n; i++) {
            AdminRefreshToken next = freshToken(1L);
            rotate(chain.get(i), next);
            chain.add(next);
        }

        // Exactly N+1 tokens
        assertThat(chain).hasSize(n + 1);

        // All tokens except the last are revoked
        for (int i = 0; i < n; i++) {
            AdminRefreshToken t = chain.get(i);
            assertThat(t.isRevoked())
                    .as("token[%d] must be revoked in a chain of %d rotations", i, n)
                    .isTrue();
        }

        // Each revoked token's replacedByTokenHash points to the next token's hash
        for (int i = 0; i < n; i++) {
            assertThat(chain.get(i).getReplacedByTokenHash())
                    .as("token[%d].replacedByTokenHash must equal token[%d].tokenHash", i, i + 1)
                    .isEqualTo(chain.get(i + 1).getTokenHash());
        }

        // Only the last token is valid
        AdminRefreshToken last = chain.get(n);
        assertThat(last.isValid())
                .as("last token in chain must be valid")
                .isTrue();
    }

    /**
     * Özellik 23 — A token that has been revoked cannot be used again (isValid() = false).
     *
     * **Validates: Requirements 15.2**
     */
    @Property(tries = 100)
    void revokedTokenCannotBeUsedAgain() {
        AdminRefreshToken token = freshToken(1L);

        assertThat(token.isValid()).as("token must be valid before revocation").isTrue();

        // Revoke the token (simulate rotation or explicit logout)
        token.setRevokedAt(OffsetDateTime.now());

        assertThat(token.isValid())
                .as("revoked token must not be valid (isValid() = false)")
                .isFalse();

        assertThat(token.isRevoked())
                .as("isRevoked() must return true after revokedAt is set")
                .isTrue();
    }
}
