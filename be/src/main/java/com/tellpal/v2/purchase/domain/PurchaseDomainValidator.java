package com.tellpal.v2.purchase.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PurchaseDomainValidator {

    private PurchaseDomainValidator() {
    }

    static Instant requireInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static Long normalizePositiveId(Long value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static Integer requirePositiveInteger(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static Integer normalizePositiveInteger(Integer value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static String requireCode(String value, String message) {
        return requireText(value, message).toUpperCase(Locale.ROOT);
    }

    static String normalizeOptionalCode(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    static String normalizeCurrencyCode(String value) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("Currency code must be a 3-letter ISO code");
        }
        return normalized;
    }

    static String normalizeCountryCode(String value) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() != 2) {
            throw new IllegalArgumentException("Country code must be a 2-letter ISO code");
        }
        return normalized;
    }

    static BigDecimal normalizeRatio(BigDecimal value, String message) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(message);
        }
        return value.stripTrailingZeros();
    }

    static List<String> copyTextList(List<String> value, String message) {
        ArrayList<String> copy = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return copy;
        }
        value.forEach(entry -> copy.add(requireText(entry, message)));
        return copy;
    }

    static Map<String, Object> copyJsonMap(Map<String, Object> value) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        if (value == null || value.isEmpty()) {
            return copy;
        }
        value.forEach((key, entryValue) -> copy.put(requireText(key, "JSON keys must not be blank"), entryValue));
        return copy;
    }
}
