package com.tellpal.v2.content.application;

import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentLocalizationRepository;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.content.domain.StoryPageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContentApplicationService {

    private final ContentRepository contentRepository;
    private final ContentLocalizationRepository contentLocalizationRepository;
    private final StoryPageRepository storyPageRepository;

    public ContentApplicationService(
            ContentRepository contentRepository,
            ContentLocalizationRepository contentLocalizationRepository,
            StoryPageRepository storyPageRepository) {
        this.contentRepository = contentRepository;
        this.contentLocalizationRepository = contentLocalizationRepository;
        this.storyPageRepository = storyPageRepository;
    }

    public Content createContent(ContentType type, String externalKey, String ageRange) {
        Content content = new Content(type, externalKey);
        content.setAgeRange(ageRange);
        if (type == ContentType.STORY) {
            content.setPageCount(0);
        }
        return contentRepository.save(content);
    }

    public Content updateContent(Long id, String ageRange, Boolean isActive) {
        Content content = getContent(id);
        if (ageRange != null) {
            content.setAgeRange(ageRange);
        }
        if (isActive != null) {
            content.setActive(isActive);
        }
        return contentRepository.save(content);
    }

    @Transactional(readOnly = true)
    public Content getContent(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Content> listContents(ContentType type) {
        if (type != null) {
            return contentRepository.findAllByIsActiveTrueAndType(type);
        }
        return contentRepository.findAllByIsActiveTrue();
    }

    public ContentLocalization createLocalization(Long contentId, String languageCode,
            String title, String description, String bodyText) {
        Content content = getContent(contentId);
        ContentLocalization localization = new ContentLocalization(content, languageCode, title);
        localization.setDescription(description);
        localization.setBodyText(bodyText);
        return contentLocalizationRepository.save(localization);
    }

    public ContentLocalization updateLocalization(Long contentId, String languageCode,
            String title, String description, String bodyText) {
        ContentLocalization localization = contentLocalizationRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(contentId, languageCode));
        if (title != null) {
            localization.setTitle(title);
        }
        if (description != null) {
            localization.setDescription(description);
        }
        if (bodyText != null) {
            localization.setBodyText(bodyText);
        }
        return contentLocalizationRepository.save(localization);
    }

    public StoryPage addStoryPage(Long contentId, int pageNumber, Long illustrationMediaId) {
        Content content = getContent(contentId);
        StoryPage page = new StoryPage(content, pageNumber);
        page.setIllustrationMediaId(illustrationMediaId);
        StoryPage saved = storyPageRepository.save(page);
        updatePageCount(content);
        return saved;
    }

    public void removeStoryPage(Long contentId, int pageNumber) {
        Content content = getContent(contentId);
        com.tellpal.v2.content.domain.StoryPageId pageId =
                new com.tellpal.v2.content.domain.StoryPageId(contentId, pageNumber);
        storyPageRepository.deleteById(pageId);
        updatePageCount(content);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ContentLocalization> findLocalization(Long contentId, String languageCode) {
        return contentLocalizationRepository.findByContentIdAndLanguageCode(contentId, languageCode);
    }

    @Transactional(readOnly = true)
    public List<StoryPage> getStoryPages(Long contentId) {
        getContent(contentId); // ensure content exists
        return storyPageRepository.findByContentIdOrderByPageNumberAsc(contentId);
    }

    private void updatePageCount(Content content) {
        long count = storyPageRepository.countByContentId(content.getId());
        content.setPageCount((int) count);
        contentRepository.save(content);
    }
}
