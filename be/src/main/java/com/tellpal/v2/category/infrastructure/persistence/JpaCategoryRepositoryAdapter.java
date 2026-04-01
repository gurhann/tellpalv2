package com.tellpal.v2.category.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryRepository;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final SpringDataCategoryRepository repository;

    public JpaCategoryRepositoryAdapter(SpringDataCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Category> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlug(slug);
    }

    @Override
    public List<Category> findAllActive() {
        return repository.findAllByActiveTrue();
    }

    @Override
    public List<Category> findAllForAdminRead() {
        return repository.findAllByOrderByIdAsc();
    }

    @Override
    public Category save(Category category) {
        return repository.save(category);
    }
}
