package com.tellpal.v2.content.web.admin;

import java.util.List;

import com.tellpal.v2.content.api.AdminStoryPageView;

public record AdminStoryPageReadResponse(
        Long contentId,
        int pageNumber,
        int localizationCount,
        List<AdminStoryPageLocalizationResponse> localizations) {

    static AdminStoryPageReadResponse from(AdminStoryPageView view) {
        return new AdminStoryPageReadResponse(
                view.contentId(),
                view.pageNumber(),
                view.localizationCount(),
                view.localizations().stream()
                        .map(AdminStoryPageLocalizationResponse::from)
                        .toList());
    }
}
