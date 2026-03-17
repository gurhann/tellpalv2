package com.tellpal.v2.admin.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminRefreshTokenHasherTest {

    private final AdminRefreshTokenHasher hasher = new AdminRefreshTokenHasher();

    @Test
    void hashesTokensDeterministically() {
        String firstHash = hasher.hash("refresh-token-value");
        String secondHash = hasher.hash("refresh-token-value");

        assertThat(firstHash).isEqualTo(secondHash);
        assertThat(firstHash).hasSize(64);
        assertThat(firstHash).isNotEqualTo("refresh-token-value");
    }

    @Test
    void rejectsBlankTokens() {
        assertThatThrownBy(() -> hasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }
}
