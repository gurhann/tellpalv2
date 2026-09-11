package com.tellpal.v2.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;
import com.tellpal.v2.content.application.ContentPublicationCommands.ArchiveContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentPublicationPolicy;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingRecord;

/**
 * Application service for publishing and archiving content localizations.
 *
 * <p>The service delegates publication readiness checks to {@link ContentPublicationPolicy} so story
 * content cannot be published before its page localizations are complete.
 */
@Service
public class ContentPublicationService {

    private final ContentRepository contentRepository;
    private final AssetProcessingApi assetProcessingApi;
    private final ContentPublicationPolicy publicationPolicy = new ContentPublicationPolicy();

    public ContentPublicationService(ContentRepository contentRepository, AssetProcessingApi assetProcessingApi) {
        this.contentRepository = contentRepository;
        this.assetProcessingApi = assetProcessingApi;
    }

    /**
     * Publishes a localization when all type-specific publication prerequisites are met.
     */
    @Transactional
    public ContentLocalizationRecord publishLocalization(PublishContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        ContentLocalization localization = loadLocalization(content, command.languageCode());
        publicationPolicy.publish(content, localization, command.publishedAt());
        Content savedContent = contentRepository.save(content);
        return toLocalizationRecord(savedContent, command.languageCode());
    }

    /**
     * Archives a published or draft localization without deleting it.
     */
    @Transactional
    public ContentLocalizationRecord archiveLocalization(ArchiveContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        ContentLocalization localization = loadLocalization(content, command.languageCode());
        publicationPolicy.archive(localization);
        Content savedContent = contentRepository.save(content);
        return toLocalizationRecord(savedContent, command.languageCode());
    }

    private ContentLocalizationRecord toLocalizationRecord(Content content,
            com.tellpal.v2.shared.domain.LanguageCode languageCode) {
        Long contentId = requireContentId(content);
        ContentLocalization localization = content.findLocalization(languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(contentId, languageCode));
        AssetProcessingRecord narrationProcessing = localization.getNarration() == null
                ? null
                : assetProcessingApi.findNarrationByLocalization(contentId, languageCode).orElse(null);
        ProcessingStatus sharedProcessingStatus = content.getType() == ContentType.LULLABY
                ? assetProcessingApi.findByContent(contentId)
                        .map(processing -> ProcessingStatus.valueOf(processing.status().name()))
                        .orElse(ProcessingStatus.PENDING)
                : null;
        return ContentManagementMapper.toLocalizationRecord(
                contentId, localization, narrationProcessing, sharedProcessingStatus);
    }

    private Content loadContent(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private ContentLocalization loadLocalization(Content content, com.tellpal.v2.shared.domain.LanguageCode languageCode) {
        Long contentId = requireContentId(content);
        return content.findLocalization(languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(contentId, languageCode));
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before publication operations");
        }
        return contentId;
    }
}
