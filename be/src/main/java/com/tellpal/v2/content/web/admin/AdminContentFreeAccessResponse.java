package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContentFreeAccessResults.ContentFreeAccessRecord;

public record AdminContentFreeAccessResponse(
        Long freeAccessId,
        String accessKey,
        Long contentId,
        String languageCode) {

    static AdminContentFreeAccessResponse from(ContentFreeAccessRecord record) {
        return new AdminContentFreeAccessResponse(
                record.freeAccessId(),
                record.accessKey(),
                record.contentId(),
                record.languageCode().value());
    }
}
