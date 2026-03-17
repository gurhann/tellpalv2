package com.tellpal.v2.category.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.category.api.CategoryLookupApi;
import com.tellpal.v2.category.api.CategoryReference;
import com.tellpal.v2.category.domain.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryLookupService implements CategoryLookupApi {

    private final CategoryRepository categoryRepository;

    public CategoryLookupService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Optional<CategoryReference> findById(Long categoryId) {
        return categoryRepository.findById(requireCategoryId(categoryId))
                .map(CategoryApiMapper::toReference);
    }

    @Override
    public Optional<CategoryReference> findBySlug(String slug) {
        return categoryRepository.findBySlug(requireSlug(slug))
                .map(CategoryApiMapper::toReference);
    }

    private static Long requireCategoryId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        return categoryId;
    }

    private static String requireSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Category slug must not be blank");
        }
        return slug.trim();
    }
}
