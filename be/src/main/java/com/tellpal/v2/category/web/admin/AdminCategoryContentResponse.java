package com.tellpal.v2.category.web.admin;

import com.tellpal.v2.category.application.CategoryManagementResults.CategoryContentRecord;

public record AdminCategoryContentResponse(
        Long categoryId,
        String languageCode,
        Long contentId,
        int displayOrder) {

    static AdminCategoryContentResponse from(CategoryContentRecord record) {
        return new AdminCategoryContentResponse(
                record.categoryId(),
                record.languageCode().value(),
                record.contentId(),
                record.displayOrder());
    }
}
