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
import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;

/**
 * Application service for maintaining story pages and their localized page content.
 */
@Service
public class StoryPageManagementService {

    private final com.tellpal.v2.content.application.publication.ReadingPublicationMutationGuard readingPublication;
    private final ContentRepository contentRepository;
    private final ContentAssetReferenceValidator assetReferenceValidator;
    private final AssetProcessingApi assetProcessingApi;

    public StoryPageManagementService(
            ContentRepository contentRepository,
            ContentAssetReferenceValidator assetReferenceValidator,
            AssetProcessingApi assetProcessingApi,
            com.tellpal.v2.content.application.publication.ReadingPublicationMutationGuard readingPublication) {
        this.readingPublication = readingPublication;
        this.contentRepository = contentRepository;
        this.assetReferenceValidator = assetReferenceValidator;
        this.assetProcessingApi = assetProcessingApi;
    }

    /**
     * Adds a story page to story content and synchronizes page count ownership in the aggregate.
     */
    @Transactional
    public StoryPageRecord addStoryPage(AddStoryPageCommand command) {
        Content content = loadContent(command.contentId());
        if (command.afterPageNumber() == null) {
            StoryPage storyPage = content.addStoryPage(null);
            Content saved = contentRepository.save(content);
            schedulePageMediaForAllLocalizations(saved);
            return ContentManagementMapper.toStoryPageRecord(
                    command.contentId(),
                    saved.findStoryPage(storyPage.getPageNumber()).orElse(storyPage));
        }

        int temporaryOffset = requireTemporaryOffset(content);
        loadStoryPage(content, command.afterPageNumber());
        content.offsetStoryPagesAfter(command.afterPageNumber(), temporaryOffset);
        contentRepository.saveAndFlush(content);

        int insertedPageNumber = command.afterPageNumber() + 1;
        StoryPage insertedPage = content.addStoryPageAt(insertedPageNumber);
        contentRepository.saveAndFlush(content);

        content.renumberStoryPagesContiguously();
        Content persistedContent = contentRepository.save(content);
        schedulePageMediaForAllLocalizations(persistedContent);
        return ContentManagementMapper.toStoryPageRecord(
                command.contentId(),
                persistedContent.findStoryPage(insertedPageNumber).orElse(insertedPage));
    }

    /**
     * Updates language-independent source illustration metadata for an existing story page.
     */
    @Transactional
    public StoryPageRecord updateStoryPage(UpdateStoryPageCommand command) {
        Content content = loadContent(command.contentId());
        StoryPage storyPage = loadStoryPage(content, command.pageNumber());
        assetReferenceValidator.requireImageAsset(
                command.textlessIllustrationMediaId(),
                "textlessIllustrationMediaId");
        storyPage.updateTextlessIllustrationMediaId(command.textlessIllustrationMediaId());
        Content saved = contentRepository.save(content);
        return ContentManagementMapper.toStoryPageRecord(
                command.contentId(),
                saved.findStoryPage(command.pageNumber()).orElse(storyPage));
    }

    /**
     * Removes a story page from the aggregate.
     */
    @Transactional
    public void removeStoryPage(RemoveStoryPageCommand command) {
        Content content = loadContent(command.contentId());
        loadStoryPage(content, command.pageNumber());
        content.offsetStoryPagesAfter(command.pageNumber(), requireTemporaryOffset(content));
        contentRepository.saveAndFlush(content);
        content.removeStoryPageOnly(command.pageNumber());
        contentRepository.saveAndFlush(content);
        content.renumberStoryPagesContiguously();
        schedulePageMediaForAllLocalizations(contentRepository.save(content));
    }

    /**
     * Creates or replaces the localized body and audio content for one story page.
     */
    @Transactional
    public StoryPageLocalizationRecord upsertStoryPageLocalization(UpsertStoryPageLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        loadContentLocalization(content, command.languageCode());
        StoryPage storyPage = loadStoryPage(content, command.pageNumber());
        Long illustrationMediaId = command.preserveIllustrationBinding()
                ? storyPage.findLocalization(command.languageCode())
                    .orElseThrow(() -> new IllegalArgumentException("Korunacak mevcut sayfa görsel bağı bulunamadı")).getIllustrationMediaId()
                : command.illustrationMediaId();
        assetReferenceValidator.requireAudioAsset(command.audioMediaId(), "audioMediaId");
        assetReferenceValidator.requireRequiredImageAsset(illustrationMediaId, "illustrationMediaId");
        StoryPageLocalization localization = storyPage.upsertLocalization(
                command.languageCode(),
                command.bodyText(),
                command.audioMediaId(),
                illustrationMediaId);
        if(readingPublication.enabled()) contentRepository.saveAndFlush(content);
        schedulePageMedia(content, command.languageCode());
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

    private void schedulePageMediaForAllLocalizations(Content content) {
        content.getLocalizations().stream()
                .map(localization -> localization.getLanguageCode())
                .forEach(languageCode -> schedulePageMedia(content, languageCode));
    }

    private void schedulePageMedia(Content content, LanguageCode languageCode) {
        readingChanged(content,languageCode);
        if (content.getType() != com.tellpal.v2.content.domain.ContentType.STORY) return;
        var orderedSources = content.getStoryPages().stream()
                .sorted(java.util.Comparator.comparingInt(StoryPage::getPageNumber))
                .map(page -> page.findLocalization(languageCode)
                        .map(pageLocalization -> new AssetProcessingPageSource(
                                page.getId(), page.getPageNumber(), pageLocalization.getIllustrationMediaId(),
                                pageLocalization.getAudioMediaId()))
                        .orElseGet(() -> new AssetProcessingPageSource(page.getId(), page.getPageNumber(), null, null)))
                .toList();
        if (orderedSources.isEmpty()) return;
        assetProcessingApi.schedule(new ScheduleAssetProcessingCommand(
                AssetProcessingTarget.localization(requireContentId(content), languageCode),
                AssetProcessingKind.STORY_PAGE_MEDIA,
                AssetProcessingContentType.STORY,
                content.getExternalKey(), null, null, orderedSources.size(), orderedSources));
    }

    private void readingChanged(Content content,LanguageCode language) {
        readingPublication.changed(content,language);
    }

    private Content loadContent(Long contentId) {
        readingPublication.lock(contentId);
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

    private static int requireTemporaryOffset(Content content) {
        Integer pageCount = content.getPageCount();
        if (pageCount == null || pageCount < 0) {
            throw new IllegalStateException("Story page count must be available for renumbering");
        }
        return pageCount + 1;
    }
}
