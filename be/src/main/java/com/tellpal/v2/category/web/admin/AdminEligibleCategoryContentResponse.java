package com.tellpal.v2.category.web.admin;

import java.time.Instant;

import com.tellpal.v2.category.api.AdminEligibleCategoryContentView;

public record AdminEligibleCategoryContentResponse(
        Long contentId,
        String externalKey,
        String localizedTitle,
        String languageCode,
        Instant publishedAt) {

    static AdminEligibleCategoryContentResponse from(AdminEligibleCategoryContentView view) {
        return new AdminEligibleCategoryContentResponse(
                view.contentId(),
                view.externalKey(),
                view.localizedTitle(),
                view.languageCode().value(),
                view.publishedAt());
    }
}
