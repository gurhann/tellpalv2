package com.tellpal.v2.admin.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class AdminRefreshTokenHasherPropertyTest {

    private final AdminRefreshTokenHasher hasher = new AdminRefreshTokenHasher();

    @Property(tries = 150)
    void hashesNonBlankTokensToStableHex(@ForAll("nonBlankTokens") String rawToken) {
        String hash = hasher.hash(rawToken);

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
        assertThat(hasher.hash(rawToken)).isEqualTo(hash);
    }

    @Provide
    Arbitrary<String> nonBlankTokens() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~!$&'()*+,;=:@/")
                .ofMinLength(1)
                .ofMaxLength(128)
                .filter(value -> !value.isBlank());
    }
}
