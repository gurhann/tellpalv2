package com.tellpal.v2.content.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.content.domain.ContentFreeAccess;
import com.tellpal.v2.content.domain.ContentFreeAccessRepository;
import com.tellpal.v2.shared.domain.LanguageCode;

@Repository
public class JpaContentFreeAccessRepositoryAdapter implements ContentFreeAccessRepository {

    private final SpringDataContentFreeAccessRepository repository;

    public JpaContentFreeAccessRepositoryAdapter(SpringDataContentFreeAccessRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByAccessKey(String accessKey) {
        return repository.existsByAccessKey(accessKey);
    }

    @Override
    public boolean existsByAccessKeyAndContentIdAndLanguageCode(String accessKey, Long contentId, LanguageCode languageCode) {
        return repository.existsByAccessKeyAndContentIdAndLanguageCode(accessKey, contentId, languageCode);
    }

    @Override
    public Optional<ContentFreeAccess> findByAccessKeyAndContentIdAndLanguageCode(
            String accessKey,
            Long contentId,
            LanguageCode languageCode) {
        return repository.findByAccessKeyAndContentIdAndLanguageCode(accessKey, contentId, languageCode);
    }

    @Override
    public List<ContentFreeAccess> findByAccessKey(String accessKey) {
        return repository.findByAccessKeyOrderByLanguageCodeAscContentIdAsc(accessKey);
    }

    @Override
    public Set<ContentFreeAccess> findByAccessKeyAndLanguageCode(String accessKey, LanguageCode languageCode) {
        return new LinkedHashSet<>(repository.findByAccessKeyAndLanguageCodeOrderByContentIdAsc(accessKey, languageCode));
    }

    @Override
    public ContentFreeAccess save(ContentFreeAccess contentFreeAccess) {
        return repository.save(contentFreeAccess);
    }

    @Override
    public void delete(ContentFreeAccess contentFreeAccess) {
        repository.delete(contentFreeAccess);
    }
}
