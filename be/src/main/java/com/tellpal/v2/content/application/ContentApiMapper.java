package com.tellpal.v2.content.application;

import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.domain.Content;

final class ContentApiMapper {

    private ContentApiMapper() {
    }

    static ContentReference toReference(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before mapping to API");
        }
        return new ContentReference(
                contentId,
                ContentApiType.valueOf(content.getType().name()),
                content.getExternalKey(),
                content.isActive(),
                content.getAgeRange(),
                content.getPageCount());
    }
}
