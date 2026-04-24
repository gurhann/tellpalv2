package com.tellpal.v2.category.web.admin;

import com.tellpal.v2.category.api.AdminCategoryContentView;

public record AdminCategoryContentResponse(
        Long categoryId,
        String languageCode,
        Long contentId,
        int displayOrder,
        String externalKey,
        String localizedTitle) {

    static AdminCategoryContentResponse from(AdminCategoryContentView view) {
        return new AdminCategoryContentResponse(
                view.categoryId(),
                view.languageCode().value(),
                view.contentId(),
                view.displayOrder(),
                view.externalKey(),
                view.localizedTitle());
    }
}
