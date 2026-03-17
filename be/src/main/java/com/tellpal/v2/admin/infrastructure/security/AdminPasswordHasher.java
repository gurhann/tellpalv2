package com.tellpal.v2.admin.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminPasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public AdminPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(requireText(rawPassword, "Admin password must not be blank"));
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(
                requireText(rawPassword, "Admin password must not be blank"),
                requireText(passwordHash, "Admin password hash must not be blank"));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
