package com.tellpal.v2.category.api;

import java.util.Optional;

/** Composes a controlled reading detail through its originating curated category. */
public interface LibraryStoryDetailQueryApi {
    Optional<LibraryStoryDetail> query(long categoryId, long contentId, String uiLanguage,
            String contentLanguage, String experience, LibraryHomeQueryApi.Audience audience);
}
