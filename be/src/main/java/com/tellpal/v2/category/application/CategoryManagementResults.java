package com.tellpal.v2.category.application;

import java.time.Instant;

import com.tellpal.v2.category.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.LanguageCode;

public final class CategoryManagementResults {

    private CategoryManagementResults() {
    }

    public record CategoryLocalizationRecord(
            Long categoryId,
            LanguageCode languageCode,
            String name,
            String description,
            Long imageMediaId,
            LocalizationStatus status,
            Instant publishedAt,
            boolean published) {

        public CategoryLocalizationRecord {
            categoryId = requirePositiveId(categoryId, "Category ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            name = requireText(name, "Category localization name must not be blank");
            status = requireStatus(status);
        }
    }

    public record CategoryContentRecord(Long categoryId, LanguageCode languageCode, Long contentId, int displayOrder) {

        public CategoryContentRecord {
            categoryId = requirePositiveId(categoryId, "Category ID must be positive");
            languageCode = requireLanguageCode(languageCode);
            contentId = requirePositiveId(contentId, "Content ID must be positive");
            if (displayOrder < 0) {
                throw new IllegalArgumentException("Category display order must not be negative");
            }
        }
    }

    private static Long requirePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static LocalizationStatus requireStatus(LocalizationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Category localization status must not be null");
        }
        return status;
    }
}
