package com.tellpal.v2.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.StoryPageNotFoundException;
import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.RemoveStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpsertStoryPageLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageLocalizationRecord;
import com.tellpal.v2.content.application.ContentManagementResults.StoryPageRecord;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.content.domain.StoryPageLocalization;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Application service for maintaining story pages and their localized page content.
 */
@Service
public class StoryPageManagementService {

    private final ContentRepository contentRepository;
    private final ContentAssetReferenceValidator assetReferenceValidator;

    public StoryPageManagementService(
            ContentRepository contentRepository,
            ContentAssetReferenceValidator assetReferenceValidator) {
        this.contentRepository = contentRepository;
        this.assetReferenceValidator = assetReferenceValidator;
    }

    /**
     * Adds a story page to story content and synchronizes page count ownership in the aggregate.
     */
    @Transactional
    public StoryPageRecord addStoryPage(AddStoryPageCommand command) {
        Content content = loadContent(command.contentId());
        StoryPage storyPage = content.addStoryPage(command.pageNumber());
        return ContentManagementMapper.toStoryPageRecord(
                command.contentId(),
                contentRepository.save(content).findStoryPage(command.pageNumber()).orElse(storyPage));
    }

    /**
     * Updates illustration metadata for an existing story page.
     */
    @Transactional
    public StoryPageRecord updateStoryPage(UpdateStoryPageCommand command) {
        Content content = loadContent(command.contentId());
        StoryPage storyPage = loadStoryPage(content, command.pageNumber());
        return ContentManagementMapper.toStoryPageRecord(
                command.contentId(),
                contentRepository.save(content).findStoryPage(command.pageNumber()).orElse(storyPage));
    }

    /**
     * Removes a story page from the aggregate.
     */
    @Transactional
    public void removeStoryPage(RemoveStoryPageCommand command) {
        Content content = loadContent(command.contentId());
        loadStoryPage(content, command.pageNumber());
        content.removeStoryPage(command.pageNumber());
        contentRepository.save(content);
    }

    /**
     * Creates or replaces the localized body and audio content for one story page.
     */
    @Transactional
    public StoryPageLocalizationRecord upsertStoryPageLocalization(UpsertStoryPageLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        loadContentLocalization(content, command.languageCode());
        StoryPage storyPage = loadStoryPage(content, command.pageNumber());
        assetReferenceValidator.requireAudioAsset(command.audioMediaId(), "audioMediaId");
        assetReferenceValidator.requireRequiredImageAsset(command.illustrationMediaId(), "illustrationMediaId");
        StoryPageLocalization localization = storyPage.upsertLocalization(
                command.languageCode(),
                command.bodyText(),
                command.audioMediaId(),
                command.illustrationMediaId());
        StoryPage persistedContent = contentRepository.save(content)
                .findStoryPage(command.pageNumber())
                .orElse(storyPage);
        StoryPageLocalization persistedLocalization = persistedContent.findLocalization(command.languageCode())
                .orElse(localization);
        return ContentManagementMapper.toStoryPageLocalizationRecord(
                command.contentId(),
                command.pageNumber(),
                persistedLocalization);
    }

    private Content loadContent(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private void loadContentLocalization(Content content, LanguageCode languageCode) {
        Long contentId = requireContentId(content);
        if (content.findLocalization(languageCode).isEmpty()) {
            throw new ContentLocalizationNotFoundException(contentId, languageCode);
        }
    }

    private StoryPage loadStoryPage(Content content, int pageNumber) {
        Long contentId = requireContentId(content);
        return content.findStoryPage(pageNumber)
                .orElseThrow(() -> new StoryPageNotFoundException(contentId, pageNumber));
    }

    private static Long requireContentId(Content content) {
        Long contentId = content.getId();
        if (contentId == null || contentId <= 0) {
            throw new IllegalStateException("Content must be persisted before story-page operations");
        }
        return contentId;
    }
}
