package com.tellpal.v2.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationAlreadyExistsException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.DuplicateContentExternalKeyException;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.DeleteContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.MarkContentLocalizationProcessingCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementResults.ContentLocalizationRecord;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Application service for creating and updating content aggregates and their localizations.
 *
 * <p>The service enforces external key uniqueness, validates referenced assets, and coordinates
 * localization processing state transitions inside the content aggregate.
 */
@Service
public class ContentManagementService {

    private final ContentRepository contentRepository;
    private final ContentAssetReferenceValidator assetReferenceValidator;

    public ContentManagementService(
            ContentRepository contentRepository,
            ContentAssetReferenceValidator assetReferenceValidator) {
        this.contentRepository = contentRepository;
        this.assetReferenceValidator = assetReferenceValidator;
    }

    /**
     * Creates a new content aggregate with its stable identity fields.
     */
    @Transactional
    public ContentReference createContent(CreateContentCommand command) {
        ensureExternalKeyAvailable(null, command.externalKey());
        Content savedContent = contentRepository.save(Content.create(
                command.type(),
                command.externalKey(),
                command.ageRange(),
                command.active()));
        return ContentApiMapper.toReference(savedContent);
    }

    /**
     * Updates core content metadata without changing localization state.
     */
    @Transactional
    public ContentReference updateContent(UpdateContentCommand command) {
        Content content = loadContent(command.contentId());
        ensureExternalKeyAvailable(command.contentId(), command.externalKey());
        content.updateDetails(command.externalKey(), command.ageRange(), command.active());
        return ContentApiMapper.toReference(contentRepository.save(content));
    }

    /**
     * Deactivates a content aggregate while preserving editorial data for admin reads.
     */
    @Transactional
    public void deleteContent(DeleteContentCommand command) {
        Content content = loadContent(command.contentId());
        content.deactivate();
        contentRepository.save(content);
    }

    /**
     * Creates a new localization for existing content after validating referenced assets.
     */
    @Transactional
    public ContentLocalizationRecord createLocalization(CreateContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        if (content.findLocalization(command.languageCode()).isPresent()) {
            throw new ContentLocalizationAlreadyExistsException(command.contentId(), command.languageCode());
        }
        validateLocalizationAssets(command.coverMediaId(), command.audioMediaId());
        ContentLocalization localization = content.upsertLocalization(
                command.languageCode(),
                command.title(),
                command.description(),
                command.bodyText(),
                command.coverMediaId(),
                command.audioMediaId(),
                command.durationMinutes(),
                command.status(),
                command.processingStatus(),
                command.publishedAt());
        return ContentManagementMapper.toLocalizationRecord(
                command.contentId(),
                contentRepository.save(content).findLocalization(command.languageCode())
                        .orElse(localization));
    }

    /**
     * Replaces localization content fields and visibility-related metadata for one language.
     */
    @Transactional
    public ContentLocalizationRecord updateLocalization(UpdateContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        loadLocalization(content, command.languageCode());
        validateLocalizationAssets(command.coverMediaId(), command.audioMediaId());
        ContentLocalization localization = content.upsertLocalization(
                command.languageCode(),
                command.title(),
                command.description(),
                command.bodyText(),
                command.coverMediaId(),
                command.audioMediaId(),
                command.durationMinutes(),
                command.status(),
                command.processingStatus(),
                command.publishedAt());
        return ContentManagementMapper.toLocalizationRecord(
                command.contentId(),
                contentRepository.save(content).findLocalization(command.languageCode())
                        .orElse(localization));
    }

    /**
     * Updates only the processing status of an existing localization.
     */
    @Transactional
    public ContentLocalizationRecord markLocalizationProcessingStatus(
            MarkContentLocalizationProcessingCommand command) {
        Content content = loadContent(command.contentId());
        ContentLocalization localization = loadLocalization(content, command.languageCode());
        localization.markProcessingStatus(command.processingStatus());
        return ContentManagementMapper.toLocalizationRecord(
                command.contentId(),
                contentRepository.save(content).findLocalization(command.languageCode())
                        .orElse(localization));
    }

    /**
     * Convenience operation for marking a localization as processing-complete.
     */
    @Transactional
    public ContentLocalizationRecord markAsReady(Long contentId, LanguageCode languageCode) {
        return markLocalizationProcessingStatus(new MarkContentLocalizationProcessingCommand(
                contentId,
                languageCode,
                ProcessingStatus.COMPLETED));
    }

    private void validateLocalizationAssets(Long coverMediaId, Long audioMediaId) {
        assetReferenceValidator.requireImageAsset(coverMediaId, "coverMediaId");
        assetReferenceValidator.requireAudioAsset(audioMediaId, "audioMediaId");
    }

    private Content loadContent(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private ContentLocalization loadLocalization(Content content, LanguageCode languageCode) {
        Long contentId = requireContentId(content);
        return content.findLocalization(languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(contentId, languageCode));
    }

    private void ensureExternalKeyAvailable(Long currentContentId, String externalKey) {
        contentRepository.findByExternalKey(externalKey)
                .filter(candidate -> !requireContentId(candidate).equals(currentContentId))
                .ifPresent(candidate -> {
                    throw new DuplicateContentExternalKeyException(externalKey);
                });
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before application mapping");
        }
        return contentId;
    }
}
