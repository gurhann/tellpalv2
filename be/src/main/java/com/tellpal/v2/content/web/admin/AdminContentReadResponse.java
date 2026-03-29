package com.tellpal.v2.content.web.admin;

import java.util.List;

import com.tellpal.v2.content.api.AdminContentView;

public record AdminContentReadResponse(
        Long contentId,
        String type,
        String externalKey,
        boolean active,
        Integer ageRange,
        Integer pageCount,
        List<AdminContentLocalizationResponse> localizations) {

    static AdminContentReadResponse from(AdminContentView view) {
        return new AdminContentReadResponse(
                view.contentId(),
                view.type().name(),
                view.externalKey(),
                view.active(),
                view.ageRange(),
                view.pageCount(),
                view.localizations().stream()
                        .map(AdminContentLocalizationResponse::from)
                        .toList());
    }
}
