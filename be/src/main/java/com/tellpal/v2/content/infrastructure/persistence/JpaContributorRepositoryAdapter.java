package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRepository;

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
    public List<Contributor> findRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Override
    public Contributor save(Contributor contributor) {
        return repository.save(contributor);
    }
}
