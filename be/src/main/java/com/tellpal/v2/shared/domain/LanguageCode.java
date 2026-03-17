package com.tellpal.v2.shared.domain;

import java.util.Arrays;

public enum LanguageCode {
    TR("tr"),
    EN("en"),
    ES("es"),
    PT("pt"),
    DE("de");

    private final String value;

    LanguageCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static LanguageCode from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Language code must not be blank");
        }

        return Arrays.stream(values())
                .filter(candidate -> candidate.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language code: " + value));
    }

    @Override
    public String toString() {
        return value;
    }
}
