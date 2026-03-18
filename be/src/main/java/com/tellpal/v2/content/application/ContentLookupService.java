package com.tellpal.v2.content.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentLocalizationLookupApi;
import com.tellpal.v2.content.api.ContentLocalizationReference;
import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-only application service for internal content lookup and localization visibility queries.
 */
@Service
@Transactional(readOnly = true)
public class ContentLookupService implements ContentLookupApi, ContentLocalizationLookupApi {

    private final ContentRepository contentRepository;

    public ContentLookupService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * Finds content by ID and maps it to the module-facing reference type.
     */
    @Override
    public Optional<ContentReference> findById(Long contentId) {
        return contentRepository.findById(requireContentId(contentId))
                .map(ContentApiMapper::toReference);
    }

    /**
     * Finds content by external key and maps it to the module-facing reference type.
     */
    @Override
    public Optional<ContentReference> findByExternalKey(String externalKey) {
        return contentRepository.findByExternalKey(requireExternalKey(externalKey))
                .map(ContentApiMapper::toReference);
    }

    /**
     * Returns visibility state for one localized content entry when it exists.
     */
    @Override
    public Optional<ContentLocalizationReference> findLocalization(Long contentId, LanguageCode languageCode) {
        Long requiredContentId = requireContentId(contentId);
        return contentRepository.findById(requiredContentId)
                .flatMap(content -> content.findLocalization(requireLanguageCode(languageCode))
                        .map(localization -> ContentApiMapper.toLocalizationReference(requiredContentId, localization)));
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static String requireExternalKey(String externalKey) {
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        return externalKey.trim();
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
