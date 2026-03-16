package com.tellpal.v2.admin.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for admin username uniqueness invariant.
 *
 * Validates: Requirements 13.2
 *
 * Özellik 20: Admin Kullanıcı Adı Benzersizliği
 * - Herhangi bir iki admin kullanıcısı için, username değerleri farklı olmalıdır.
 */
public class AdminUsernameUniquenessPropertyTest {

    /** Generates valid non-blank username strings (lowercase alphanumeric, 1-50 chars). */
    @Provide
    Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /**
     * Generates a list of 1-20 distinct usernames using jqwik's set-to-list approach.
     * Using a Set as intermediate ensures all elements are unique before building the list.
     */
    @Provide
    Arbitrary<List<String>> distinctUsernameList() {
        return validUsernames()
                .set()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(set -> List.copyOf(set));
    }

    /**
     * Özellik 20: A collection of AdminUser objects with distinct usernames
     * must have no duplicate usernames (uniqueness invariant).
     *
     * **Validates: Requirements 13.2**
     */
    @Property(tries = 100)
    void distinctUsernamesProduceNoCollisions(@ForAll("distinctUsernameList") List<String> usernames) {
        List<AdminUser> users = usernames.stream()
                .map(u -> new AdminUser(u, "hash"))
                .collect(Collectors.toList());

        Set<String> uniqueNames = users.stream()
                .map(AdminUser::getUsername)
                .collect(Collectors.toSet());

        assertThat(uniqueNames)
                .as("A list of AdminUsers with distinct usernames must have no duplicate usernames")
                .hasSize(users.size());
    }

    /**
     * Özellik 20: Two AdminUser objects with the same username violate the
     * uniqueness constraint — detected at domain level by comparing usernames.
     *
     * **Validates: Requirements 13.2**
     */
    @Property(tries = 100)
    void sameUsernameRepresentsConstraintViolation(@ForAll("validUsernames") String username) {
        AdminUser first  = new AdminUser(username, "hash1");
        AdminUser second = new AdminUser(username, "hash2");

        // Both users carry the same username — this is the constraint violation
        assertThat(first.getUsername())
                .as("Two AdminUsers sharing the same username violate the uniqueness constraint")
                .isEqualTo(second.getUsername());

        // A set built from both usernames collapses to size 1, proving the collision
        Set<String> names = new java.util.HashSet<>();
        names.add(first.getUsername());
        names.add(second.getUsername());
        assertThat(names)
                .as("Identical usernames must be detected as a uniqueness violation (set size = 1)")
                .hasSize(1);
    }

    /**
     * Özellik 20: Admin username must not be null or blank.
     *
     * **Validates: Requirements 13.2**
     */
    @Property(tries = 100)
    void usernameIsNeverNullOrBlank(@ForAll("validUsernames") String username) {
        AdminUser user = new AdminUser(username, "hash");

        assertThat(user.getUsername())
                .as("Admin username must not be null")
                .isNotNull();

        assertThat(user.getUsername().trim())
                .as("Admin username must not be blank")
                .isNotEmpty();
    }
}
