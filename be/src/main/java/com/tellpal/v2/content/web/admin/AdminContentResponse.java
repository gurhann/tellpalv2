package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.api.ContentReference;

public record AdminContentResponse(
        Long contentId,
        String type,
        String externalKey,
        boolean active,
        Integer ageRange,
        Integer pageCount) {

    static AdminContentResponse from(ContentReference reference) {
        return new AdminContentResponse(
                reference.contentId(),
                reference.type().name(),
                reference.externalKey(),
                reference.active(),
                reference.ageRange(),
                reference.pageCount());
    }
}
