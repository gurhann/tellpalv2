package com.tellpal.v2.content.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.content.api.ContentReference;
import com.tellpal.v2.content.domain.ContentRepository;

@Service
@Transactional(readOnly = true)
public class ContentLookupService implements ContentLookupApi {

    private final ContentRepository contentRepository;

    public ContentLookupService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public Optional<ContentReference> findById(Long contentId) {
        return contentRepository.findById(requireContentId(contentId))
                .map(ContentApiMapper::toReference);
    }

    @Override
    public Optional<ContentReference> findByExternalKey(String externalKey) {
        return contentRepository.findByExternalKey(requireExternalKey(externalKey))
                .map(ContentApiMapper::toReference);
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static String requireExternalKey(String externalKey) {
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("Content external key must not be blank");
        }
        return externalKey.trim();
    }
}
