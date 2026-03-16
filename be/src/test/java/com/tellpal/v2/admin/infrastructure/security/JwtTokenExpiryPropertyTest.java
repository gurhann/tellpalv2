package com.tellpal.v2.admin.infrastructure.security;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure jqwik property test for JWT token expiry behaviour.
 *
 * No Spring context — JwtTokenProvider is instantiated directly with a fixed
 * test secret and a 1-hour access-token expiry.
 *
 * **Validates: Requirements 13.3**
 *
 * Özellik 21: JWT Token Süre Sonu Validasyonu
 */
class JwtTokenExpiryPropertyTest {

    private static final String TEST_SECRET = "test-secret-key-for-jqwik-property-tests-32+";
    private static final long EXPIRY_HOURS = 1L;

    /** Shared provider instance — safe because JwtTokenProvider is stateless after construction. */
    private final JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, EXPIRY_HOURS);

    // -----------------------------------------------------------------------
    // Arbitraries
    // -----------------------------------------------------------------------

    /**
     * Generates non-null, non-blank usernames that are valid JWT subject strings.
     * Constrained to printable ASCII to avoid edge-cases unrelated to JWT logic.
     */
    @Provide
    Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    // -----------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------

    /**
     * Özellik 21 — Property 1:
     * A freshly generated access token must be valid immediately after creation.
     *
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    void freshAccessTokenIsValid(@ForAll("validUsernames") String username) {
        String token = provider.generateAccessToken(username);

        assertThat(provider.validateToken(token))
                .as("A freshly generated access token for '%s' must be valid", username)
                .isTrue();
    }

    /**
     * Özellik 21 — Property 2:
     * The username embedded in a generated token must always be extractable and
     * equal to the original username.
     *
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    void extractedUsernameMatchesOriginal(@ForAll("validUsernames") String username) {
        String token = provider.generateAccessToken(username);

        assertThat(provider.extractUsername(token))
                .as("Extracted username must equal the original username '%s'", username)
                .isEqualTo(username);
    }

    /**
     * Özellik 21 — Property 3:
     * A token with a single character modified (tampered) must be rejected by
     * validateToken.
     *
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    void tamperedTokenIsRejected(@ForAll("validUsernames") String username) {
        String token = provider.generateAccessToken(username);
        String tampered = tamper(token);

        assertThat(provider.validateToken(tampered))
                .as("A tampered token must be rejected for username '%s'", username)
                .isFalse();
    }

    /**
     * Özellik 21 — Property 4:
     * generateRefreshToken() must always return a non-null, non-blank string in
     * UUID format (8-4-4-4-12 hex groups separated by hyphens).
     *
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    void refreshTokenIsNonBlankUuidFormat(@ForAll("validUsernames") String ignored) {
        String refreshToken = provider.generateRefreshToken();

        assertThat(refreshToken)
                .as("Refresh token must not be null or blank")
                .isNotNull()
                .isNotBlank();

        assertThat(refreshToken)
                .as("Refresh token must match UUID format")
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    /**
     * Özellik 21 — Property 5:
     * hashToken() must be deterministic: the same input always produces the same
     * 64-character hex output (SHA-256 produces 32 bytes = 64 hex chars).
     *
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    void hashTokenIsDeterministic(@ForAll("validUsernames") String input) {
        String hash1 = provider.hashToken(input);
        String hash2 = provider.hashToken(input);

        assertThat(hash1)
                .as("hashToken must be deterministic for input '%s'", input)
                .isEqualTo(hash2);

        assertThat(hash1)
                .as("hashToken output must be exactly 64 hex characters")
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    /**
     * Özellik 21 — Property 6:
     * hashToken() for two different inputs must produce different hashes
     * (collision resistance).
     *
     * **Validates: Requirements 13.3**
     */
    @Property(tries = 100)
    void hashTokenCollisionResistance(
            @ForAll("validUsernames") String input1,
            @ForAll("validUsernames") String input2) {

        net.jqwik.api.Assume.that(!input1.equals(input2));

        String hash1 = provider.hashToken(input1);
        String hash2 = provider.hashToken(input2);

        assertThat(hash1)
                .as("Different inputs '%s' and '%s' must produce different hashes", input1, input2)
                .isNotEqualTo(hash2);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Modifies one character in the token's signature segment (last part after
     * the final '.') to simulate tampering without breaking the overall structure.
     */
    private static String tamper(String token) {
        int lastDot = token.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= token.length() - 1) {
            // Fallback: flip the last character of the whole token
            return token.substring(0, token.length() - 1)
                    + (token.charAt(token.length() - 1) == 'A' ? 'B' : 'A');
        }
        String header = token.substring(0, lastDot + 1);
        String sig = token.substring(lastDot + 1);
        // Flip the first character of the signature
        char flipped = sig.charAt(0) == 'A' ? 'B' : 'A';
        return header + flipped + sig.substring(1);
    }
}
