package com.tellpal.v2.content.application;

import java.util.Set;
import java.util.stream.Collectors;

import com.tellpal.v2.content.api.ResolvedContentFreeAccessSet;
import com.tellpal.v2.content.application.ContentFreeAccessResults.ContentFreeAccessRecord;
import com.tellpal.v2.content.domain.ContentFreeAccess;
import com.tellpal.v2.shared.domain.LanguageCode;

final class ContentFreeAccessMapper {

    private ContentFreeAccessMapper() {
    }

    static ContentFreeAccessRecord toRecord(ContentFreeAccess entry) {
        Long freeAccessId = entry.getId();
        if (freeAccessId == null || freeAccessId <= 0) {
            throw new IllegalStateException("Content free-access entry must be persisted before mapping");
        }
        return new ContentFreeAccessRecord(
                freeAccessId,
                entry.getAccessKey(),
                entry.getContentId(),
                entry.getLanguageCode());
    }

    static ResolvedContentFreeAccessSet toResolvedSet(
            LanguageCode languageCode,
            String resolvedAccessKey,
            Set<ContentFreeAccess> entries) {
        return new ResolvedContentFreeAccessSet(
                languageCode,
                resolvedAccessKey,
                entries.stream()
                        .map(ContentFreeAccess::getContentId)
                        .collect(Collectors.toUnmodifiableSet()));
    }
}
