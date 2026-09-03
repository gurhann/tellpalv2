package com.tellpal.v2.content.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ContributorTest {

    @Test
    void legacyCreateTrimsDisplayNameAndAssignsAuthorRole() {
        Contributor contributor = Contributor.create("  Ada Lovelace  ");

        assertThat(contributor.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(contributor.getRoles()).containsExactly(ContributorRole.AUTHOR);
    }

    @Test
    void createAcceptsOneOrMultipleUniqueRoles() {
        Contributor author = Contributor.create("Ada", Set.of(ContributorRole.AUTHOR));
        Contributor multidisciplinary = Contributor.create(
                "Mina",
                Set.of(ContributorRole.ILLUSTRATOR, ContributorRole.MUSICIAN));

        assertThat(author.getRoles()).containsExactly(ContributorRole.AUTHOR);
        assertThat(multidisciplinary.getRoles())
                .containsExactlyInAnyOrder(ContributorRole.ILLUSTRATOR, ContributorRole.MUSICIAN);
    }

    @Test
    void createRejectsNullEmptyOrNullContainingRoles() {
        Set<ContributorRole> nullContainingRoles = new LinkedHashSet<>();
        nullContainingRoles.add(ContributorRole.AUTHOR);
        nullContainingRoles.add(null);

        assertThatThrownBy(() -> Contributor.create("Ada", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one role");
        assertThatThrownBy(() -> Contributor.create("Ada", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one role");
        assertThatThrownBy(() -> Contributor.create("Ada", nullContainingRoles))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one role");
    }

    @Test
    void rolesViewIsImmutable() {
        Contributor contributor = Contributor.create("Ada", Set.of(ContributorRole.AUTHOR));

        assertThatThrownBy(() -> contributor.getRoles().add(ContributorRole.NARRATOR))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(contributor.getRoles()).containsExactly(ContributorRole.AUTHOR);
    }

    @Test
    void createRejectsBlankDisplayName() {
        assertThatThrownBy(() -> Contributor.create("  ", Set.of(ContributorRole.AUTHOR)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("display name");
    }
}
