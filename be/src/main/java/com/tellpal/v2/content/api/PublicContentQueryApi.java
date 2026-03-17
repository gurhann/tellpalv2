package com.tellpal.v2.content.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

public interface PublicContentQueryApi {

    List<PublicContentSummary> listContents(LanguageCode languageCode, String requestedAccessKey, ContentApiType type);

    List<PublicContentSummary> listContentsByIds(
            List<Long> contentIds,
            LanguageCode languageCode,
            String requestedAccessKey);

    Optional<PublicContentDetails> findContent(Long contentId, LanguageCode languageCode, String requestedAccessKey);

    Optional<List<PublicStoryPage>> findStoryPages(Long contentId, LanguageCode languageCode);
}
