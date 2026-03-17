package com.tellpal.v2.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class AppUserPrimaryProfilePropertyTest {

    @Property(tries = 150)
    void arbitraryProfileCreationSequenceKeepsExactlyOnePrimaryProfile(@ForAll("primaryFlags") List<Boolean> primaryFlags) {
        AppUser appUser = AppUser.create("firebase-" + UUID.randomUUID(), false);
        UserProfile expectedPrimary = appUser.primaryProfile().orElseThrow();

        for (int index = 0; index < primaryFlags.size(); index++) {
            UserProfile createdProfile = appUser.addProfile(
                    "Profile " + index,
                    "6-8",
                    null,
                    new String[] {"genre-" + index},
                    new String[] {"purpose-" + index},
                    primaryFlags.get(index));
            if (primaryFlags.get(index)) {
                expectedPrimary = createdProfile;
            }
        }

        List<UserProfile> primaryProfiles = appUser.getProfiles().stream()
                .filter(UserProfile::isPrimary)
                .toList();

        assertThat(primaryProfiles).containsExactly(expectedPrimary);
    }

    @Provide
    Arbitrary<List<Boolean>> primaryFlags() {
        return Arbitraries.of(Boolean.TRUE, Boolean.FALSE)
                .list()
                .ofMinSize(0)
                .ofMaxSize(20);
    }
}
