package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRepository;
import com.tellpal.v2.content.domain.ContributorRepository.ContributorRegistryPage;

@Repository
public class JpaContributorRepositoryAdapter implements ContributorRepository {

    private final SpringDataContributorRepository repository;

    public JpaContributorRepositoryAdapter(SpringDataContributorRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Contributor> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Contributor> findByNormalizedDisplayName(String normalizedDisplayName) {
        return repository.findByNormalizedDisplayName(normalizedDisplayName);
    }

    @Override
    public List<Contributor> findRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Override
    public List<Contributor> searchByDisplayName(String query, int limit) {
        return repository.searchByDisplayName(query, PageRequest.of(0, limit));
    }

    @Override
    public ContributorRegistryPage findRegistryPage(String query,
            com.tellpal.v2.content.domain.ContributorRole role, int page, int size) {
        org.springframework.data.domain.Page<Contributor> result = repository.findRegistryPage(query, role,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
        return new ContributorRegistryPage(result.getContent(), result.getTotalElements());
    }

    @Override
    public void delete(Contributor contributor) {
        repository.delete(contributor);
    }

    @Override
    public Contributor save(Contributor contributor) {
        return repository.save(contributor);
    }

    @Override
    public Contributor saveAndFlush(Contributor contributor) {
        return repository.saveAndFlush(contributor);
    }
}
