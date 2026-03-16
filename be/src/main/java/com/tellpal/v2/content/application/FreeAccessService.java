package com.tellpal.v2.content.application;

import com.tellpal.v2.content.domain.ContentFreeAccess;
import com.tellpal.v2.content.domain.ContentFreeAccessRepository;
import com.tellpal.v2.content.domain.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FreeAccessService {

    public static final String DEFAULT_KEY = "default";

    private final ContentFreeAccessRepository contentFreeAccessRepository;
    private final ContentRepository contentRepository;

    public FreeAccessService(ContentFreeAccessRepository contentFreeAccessRepository,
                             ContentRepository contentRepository) {
        this.contentFreeAccessRepository = contentFreeAccessRepository;
        this.contentRepository = contentRepository;
    }

    public ContentFreeAccess addFreeAccess(String accessKey, Long contentId, String languageCode) {
        contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        if (contentFreeAccessRepository.existsByAccessKeyAndContentIdAndLanguageCode(
                accessKey, contentId, languageCode)) {
            return contentFreeAccessRepository
                    .findByAccessKeyAndContentIdAndLanguageCode(accessKey, contentId, languageCode)
                    .orElseThrow();
        }

        return contentFreeAccessRepository.save(new ContentFreeAccess(accessKey, contentId, languageCode));
    }

    public void removeFreeAccess(String accessKey, Long contentId, String languageCode) {
        contentFreeAccessRepository.deleteByAccessKeyAndContentIdAndLanguageCode(
                accessKey, contentId, languageCode);
    }

    @Transactional(readOnly = true)
    public List<ContentFreeAccess> listFreeAccessByKey(String accessKey) {
        return contentFreeAccessRepository.findByAccessKey(accessKey);
    }

    @Transactional(readOnly = true)
    public String resolveFreeKey(String freeKey) {
        if (freeKey == null || freeKey.isBlank()) {
            return DEFAULT_KEY;
        }
        if (contentFreeAccessRepository.findByAccessKey(freeKey).isEmpty()) {
            return DEFAULT_KEY;
        }
        return freeKey;
    }

    @Transactional(readOnly = true)
    public boolean isFree(Long contentId, String languageCode, String freeKey) {
        String resolvedKey = resolveFreeKey(freeKey);
        return contentFreeAccessRepository.existsByAccessKeyAndContentIdAndLanguageCode(
                resolvedKey, contentId, languageCode);
    }
}
