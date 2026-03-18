package com.tellpal.v2.content.application;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentFreeAccessApi;
import com.tellpal.v2.content.api.ResolvedContentFreeAccessSet;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentFreeAccessAlreadyExistsException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentFreeAccessNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.GrantContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.RevokeContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessResults.ContentFreeAccessRecord;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentFreeAccess;
import com.tellpal.v2.content.domain.ContentFreeAccessRepository;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Application service for maintaining and resolving localized free-access overrides for content.
 */
@Service
public class ContentFreeAccessService implements ContentFreeAccessApi {

    static final String DEFAULT_ACCESS_KEY = "default";

    private final ContentRepository contentRepository;
    private final ContentFreeAccessRepository contentFreeAccessRepository;

    public ContentFreeAccessService(
            ContentRepository contentRepository,
            ContentFreeAccessRepository contentFreeAccessRepository) {
        this.contentRepository = contentRepository;
        this.contentFreeAccessRepository = contentFreeAccessRepository;
    }

    /**
     * Grants free access to a localized content item for the requested access key.
     */
    @Transactional
    public ContentFreeAccessRecord grantFreeAccess(GrantContentFreeAccessCommand command) {
        String accessKey = normalizeAccessKey(command.accessKey());
        Content content = loadContent(command.contentId());
        ensureLocalizationExists(content, command.languageCode());
        if (contentFreeAccessRepository.existsByAccessKeyAndContentIdAndLanguageCode(
                accessKey,
                command.contentId(),
                command.languageCode())) {
            throw new ContentFreeAccessAlreadyExistsException(accessKey, command.contentId(), command.languageCode());
        }
        ContentFreeAccess savedEntry = contentFreeAccessRepository.save(ContentFreeAccess.grant(
                accessKey,
                command.contentId(),
                command.languageCode()));
        return ContentFreeAccessMapper.toRecord(savedEntry);
    }

    /**
     * Revokes an existing free-access entry.
     */
    @Transactional
    public void revokeFreeAccess(RevokeContentFreeAccessCommand command) {
        contentFreeAccessRepository.findByAccessKeyAndContentIdAndLanguageCode(
                        normalizeAccessKey(command.accessKey()),
                        command.contentId(),
                        command.languageCode())
                .ifPresentOrElse(
                        contentFreeAccessRepository::delete,
                        () -> {
                            throw new ContentFreeAccessNotFoundException(
                                    command.accessKey(),
                                    command.contentId(),
                                    command.languageCode());
                        });
    }

    /**
     * Lists all free-access entries stored under the effective access key.
     */
    @Transactional(readOnly = true)
    public List<ContentFreeAccessRecord> listFreeAccessEntries(String accessKey) {
        return contentFreeAccessRepository.findByAccessKey(normalizeRequestedAccessKey(accessKey)).stream()
                .map(ContentFreeAccessMapper::toRecord)
                .toList();
    }

    /**
     * Resolves the effective localized free-access set, falling back to the default key when
     * needed.
     */
    @Override
    @Transactional(readOnly = true)
    public ResolvedContentFreeAccessSet resolveFreeAccess(LanguageCode languageCode, String requestedAccessKey) {
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        String normalizedAccessKey = normalizeRequestedAccessKey(requestedAccessKey);
        String resolvedAccessKey = contentFreeAccessRepository.existsByAccessKey(normalizedAccessKey)
                ? normalizedAccessKey
                : DEFAULT_ACCESS_KEY;
        Set<ContentFreeAccess> entries = contentFreeAccessRepository.findByAccessKeyAndLanguageCode(
                resolvedAccessKey,
                requiredLanguageCode);
        return ContentFreeAccessMapper.toResolvedSet(requiredLanguageCode, resolvedAccessKey, entries);
    }

    /**
     * Checks whether one content item belongs to the effective localized free-access set.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isContentFree(Long contentId, LanguageCode languageCode, String requestedAccessKey) {
        Long requiredContentId = requireContentId(contentId);
        return resolveFreeAccess(languageCode, requestedAccessKey).contains(requiredContentId);
    }

    private Content loadContent(Long contentId) {
        return contentRepository.findById(requireContentId(contentId))
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    private void ensureLocalizationExists(Content content, LanguageCode languageCode) {
        Long contentId = requireContentId(content.getId());
        if (content.findLocalization(requireLanguageCode(languageCode)).isEmpty()) {
            throw new ContentLocalizationNotFoundException(contentId, languageCode);
        }
    }

    private static String normalizeRequestedAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            return DEFAULT_ACCESS_KEY;
        }
        return normalizeAccessKey(accessKey);
    }

    private static String normalizeAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("Access key must not be blank");
        }
        return accessKey.trim();
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
