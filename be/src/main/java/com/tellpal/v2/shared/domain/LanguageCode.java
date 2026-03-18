package com.tellpal.v2.shared.domain;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Canonical language codes supported across TellPal modules.
 *
 * <p>This enum is safe to use across module boundaries as a shared domain primitive.
 */
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

    /**
     * Returns the serialized language code value.
     */
    @JsonValue
    public String value() {
        return value;
    }

    /**
     * Parses a language code from persisted or API-facing text.
     */
    @JsonCreator
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
