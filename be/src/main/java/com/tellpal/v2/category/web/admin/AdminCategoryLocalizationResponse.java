package com.tellpal.v2.category.web.admin;

import java.time.Instant;

import com.tellpal.v2.category.application.CategoryManagementResults.CategoryLocalizationRecord;

public record AdminCategoryLocalizationResponse(
        Long categoryId,
        String languageCode,
        String name,
        String description,
        Long imageMediaId,
        String status,
        Instant publishedAt,
        boolean published) {

    static AdminCategoryLocalizationResponse from(CategoryLocalizationRecord record) {
        return new AdminCategoryLocalizationResponse(
                record.categoryId(),
                record.languageCode().value(),
                record.name(),
                record.description(),
                record.imageMediaId(),
                record.status().name(),
                record.publishedAt(),
                record.published());
    }
}
