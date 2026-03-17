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

@Service
public class ContentPublicationService {

    private final ContentRepository contentRepository;
    private final ContentPublicationPolicy publicationPolicy = new ContentPublicationPolicy();

    public ContentPublicationService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Transactional
    public ContentLocalizationRecord publishLocalization(PublishContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        ContentLocalization localization = loadLocalization(content, command.languageCode());
        publicationPolicy.publish(content, localization, command.publishedAt());
        return ContentManagementMapper.toLocalizationRecord(
                command.contentId(),
                contentRepository.save(content).findLocalization(command.languageCode())
                        .orElse(localization));
    }

    @Transactional
    public ContentLocalizationRecord archiveLocalization(ArchiveContentLocalizationCommand command) {
        Content content = loadContent(command.contentId());
        ContentLocalization localization = loadLocalization(content, command.languageCode());
        publicationPolicy.archive(localization);
        return ContentManagementMapper.toLocalizationRecord(
                command.contentId(),
                contentRepository.save(content).findLocalization(command.languageCode())
                        .orElse(localization));
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
