package com.tellpal.v2.admin.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for refresh token hash uniqueness invariant.
 *
 * Validates: Requirements 13.6
 *
 * Özellik 22: Refresh Token Hash Benzersizliği
 * - Herhangi bir iki refresh token için, token_hash değerleri farklı olmalıdır.
 */
public class RefreshTokenHashUniquenessPropertyTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime FUTURE = NOW.plusHours(1);

    /** Generates SHA-256-like hex strings (64 lowercase hex characters). */
    @Provide
    Arbitrary<String> sha256HexStrings() {
        return Arbitraries.strings()
                .withChars("0123456789abcdef")
                .ofLength(64);
    }

    /**
     * Generates a list of 1-20 distinct token hashes using a set as intermediate.
     */
    @Provide
    Arbitrary<List<String>> distinctHashList() {
        return sha256HexStrings()
                .set()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(set -> List.copyOf(set));
    }

    private AdminRefreshToken token(Long userId, String hash) {
        return new AdminRefreshToken(userId, hash, NOW, FUTURE, "agent", "127.0.0.1");
    }

    /**
     * Özellik 22: A collection of AdminRefreshToken objects with distinct token hashes
     * must have no duplicate hashes (uniqueness invariant).
     *
     * **Validates: Requirements 13.6**
     */
    @Property(tries = 100)
    void distinctHashesProduceNoCollisions(@ForAll("distinctHashList") List<String> hashes) {
        List<AdminRefreshToken> tokens = hashes.stream()
                .map(h -> token(1L, h))
                .collect(Collectors.toList());

        Set<String> uniqueHashes = tokens.stream()
                .map(AdminRefreshToken::getTokenHash)
                .collect(Collectors.toSet());

        assertThat(uniqueHashes)
                .as("A list of AdminRefreshTokens with distinct hashes must have no duplicate token hashes")
                .hasSize(tokens.size());
    }

    /**
     * Özellik 22: Two tokens with the same hash represent a uniqueness constraint violation.
     *
     * **Validates: Requirements 13.6**
     */
    @Property(tries = 100)
    void sameHashRepresentsConstraintViolation(@ForAll("sha256HexStrings") String hash) {
        AdminRefreshToken first  = token(1L, hash);
        AdminRefreshToken second = token(2L, hash);

        assertThat(first.getTokenHash())
                .as("Two AdminRefreshTokens sharing the same hash violate the uniqueness constraint")
                .isEqualTo(second.getTokenHash());

        Set<String> hashes = new java.util.HashSet<>();
        hashes.add(first.getTokenHash());
        hashes.add(second.getTokenHash());
        assertThat(hashes)
                .as("Identical token hashes must be detected as a uniqueness violation (set size = 1)")
                .hasSize(1);
    }

    /**
     * Özellik 22: Token hash must not be null or blank.
     *
     * **Validates: Requirements 13.6**
     */
    @Property(tries = 100)
    void tokenHashIsNeverNullOrBlank(@ForAll("sha256HexStrings") String hash) {
        AdminRefreshToken token = token(1L, hash);

        assertThat(token.getTokenHash())
                .as("Token hash must not be null")
                .isNotNull();

        assertThat(token.getTokenHash().trim())
                .as("Token hash must not be blank")
                .isNotEmpty();
    }
}
