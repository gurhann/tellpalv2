package com.tellpal.v2.category.web.admin;

import com.tellpal.v2.category.api.AdminCategoryContentView;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryContentRecord;

public record AdminCategoryContentResponse(
        Long categoryId,
        String languageCode,
        Long contentId,
        int displayOrder) {

    static AdminCategoryContentResponse from(AdminCategoryContentView view) {
        return new AdminCategoryContentResponse(
                view.categoryId(),
                view.languageCode().value(),
                view.contentId(),
                view.displayOrder());
    }

    static AdminCategoryContentResponse from(CategoryContentRecord record) {
        return new AdminCategoryContentResponse(
                record.categoryId(),
                record.languageCode().value(),
                record.contentId(),
                record.displayOrder());
    }
}
