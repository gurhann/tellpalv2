package com.tellpal.v2.content.api;

import com.tellpal.v2.asset.api.ResolvedAssetReference;
import com.tellpal.v2.shared.domain.LanguageCode;

public record PublicStoryPage(
        Long contentId,
        int pageNumber,
        LanguageCode languageCode,
        String bodyText,
        ResolvedAssetReference illustration,
        ResolvedAssetReference audio) {

    public PublicStoryPage {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("Story page number must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
    }
}
