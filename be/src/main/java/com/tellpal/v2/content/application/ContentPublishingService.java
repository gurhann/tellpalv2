package com.tellpal.v2.content.application;

import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentLocalizationRepository;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.StoryPageRepository;
import com.tellpal.v2.shared.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.ProcessingStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class ContentPublishingService {

    private final ContentRepository contentRepository;
    private final ContentLocalizationRepository contentLocalizationRepository;
    private final StoryPageRepository storyPageRepository;

    public ContentPublishingService(
            ContentRepository contentRepository,
            ContentLocalizationRepository contentLocalizationRepository,
            StoryPageRepository storyPageRepository) {
        this.contentRepository = contentRepository;
        this.contentLocalizationRepository = contentLocalizationRepository;
        this.storyPageRepository = storyPageRepository;
    }

    public void publishLocalization(Long contentId, String languageCode) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        ContentLocalization localization = contentLocalizationRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(contentId, languageCode));

        if (localization.getProcessingStatus() != ProcessingStatus.COMPLETED) {
            throw new ContentPublishingException(
                    "Cannot publish: processing status is not COMPLETED for contentId=" + contentId
                    + ", lang=" + languageCode);
        }

        if (content.getType() == ContentType.STORY) {
            long pageCount = storyPageRepository.countByContentId(contentId);
            if (pageCount == 0) {
                throw new ContentPublishingException(
                        "Cannot publish STORY: no pages found for contentId=" + contentId);
            }
        } else if (content.getType() == ContentType.MEDITATION
                || content.getType() == ContentType.AUDIO_STORY) {
            String bodyText = localization.getBodyText();
            if (bodyText == null || bodyText.isBlank()) {
                throw new ContentPublishingException(
                        "Cannot publish " + content.getType() + ": body_text must not be blank for contentId="
                        + contentId + ", lang=" + languageCode);
            }
        }

        localization.setStatus(LocalizationStatus.PUBLISHED);
        localization.setPublishedAt(OffsetDateTime.now());
        contentLocalizationRepository.save(localization);
    }

    public void archiveLocalization(Long contentId, String languageCode) {
        ContentLocalization localization = contentLocalizationRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new ContentLocalizationNotFoundException(contentId, languageCode));

        localization.setStatus(LocalizationStatus.ARCHIVED);
        contentLocalizationRepository.save(localization);
    }
}
