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
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingTarget;

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
    private final AssetProcessingApi assetProcessingApi;

    public ContentManagementService(
            ContentRepository contentRepository,
            ContentAssetReferenceValidator assetReferenceValidator,
            AssetProcessingApi assetProcessingApi) {
        this.contentRepository = contentRepository;
        this.assetReferenceValidator = assetReferenceValidator;
        this.assetProcessingApi = assetProcessingApi;
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
        assetReferenceValidator.requireImageAsset(command.textlessCoverMediaId(), "textlessCoverMediaId");
        assetReferenceValidator.requireImageAsset(command.listeningCoverMediaId(), "listeningCoverMediaId");
        content.updateDetails(command.externalKey(), command.ageRange(), command.active());
        content.updateCoverMediaIds(command.textlessCoverMediaId(), command.listeningCoverMediaId());
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
        upsertNarration(content, command.languageCode(), command.narration());
        Content savedContent = contentRepository.save(content);
        scheduleNarrationProcessing(savedContent, command.languageCode(), command.narration(), true);
        return toLocalizationRecord(savedContent, command.languageCode());
    }

    /**
     * Replaces localization content fields and visibility-related metadata for one language.
     */
    @Transactional
    public ContentLocalizationRecord updateLocalization(UpdateContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        ContentLocalization existingLocalization = loadLocalization(content, command.languageCode());
        boolean narrationChanged = narrationChanged(existingLocalization, command.narration());
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
        upsertNarration(content, command.languageCode(), command.narration());
        Content savedContent = contentRepository.save(content);
        scheduleNarrationProcessing(savedContent, command.languageCode(), command.narration(), narrationChanged);
        return toLocalizationRecord(savedContent, command.languageCode());
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
        Content savedContent = contentRepository.save(content);
        return toLocalizationRecord(savedContent, command.languageCode());
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

    private void upsertNarration(Content content, LanguageCode languageCode,
            ContentManagementCommands.StoryNarrationCommand narration) {
        if (narration == null) {
            return;
        }
        if (content.getType() != com.tellpal.v2.content.domain.ContentType.STORY) {
            throw new IllegalArgumentException("Narration is only supported for STORY content");
        }
        assetReferenceValidator.requireAudioAsset(narration.audioMediaId(), "narration.audioMediaId");
        content.upsertStoryNarration(languageCode, narration.audioMediaId(), narration.durationMinutes());
    }

    private void scheduleNarrationProcessing(Content content, LanguageCode languageCode,
            ContentManagementCommands.StoryNarrationCommand narration, boolean narrationChanged) {
        if (narration == null || !narrationChanged) return;
        assetProcessingApi.schedule(new ScheduleAssetProcessingCommand(
                AssetProcessingTarget.localization(requireContentId(content), languageCode),
                AssetProcessingKind.STORY_NARRATION,
                AssetProcessingContentType.STORY,
                content.getExternalKey(), null, narration.audioMediaId(), 0));
    }

    private ContentLocalizationRecord toLocalizationRecord(Content content, LanguageCode languageCode) {
        ContentLocalization localization = content.findLocalization(languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(requireContentId(content), languageCode));
        AssetProcessingRecord narrationProcessing = localization.getNarration() == null
                ? null
                : assetProcessingApi.findNarrationByLocalization(requireContentId(content), languageCode).orElse(null);
        return ContentManagementMapper.toLocalizationRecord(requireContentId(content), localization, narrationProcessing);
    }

    private static boolean narrationChanged(ContentLocalization localization,
            ContentManagementCommands.StoryNarrationCommand narration) {
        if (narration == null || localization.getNarration() == null) {
            return narration != null && localization.getNarration() == null;
        }
        return !narration.audioMediaId().equals(localization.getNarration().getAudioMediaId())
                || !narration.durationMinutes().equals(localization.getNarration().getDurationMinutes());
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
