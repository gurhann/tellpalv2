package com.tellpal.v2.category.web.admin;

import com.tellpal.v2.category.api.CategoryReference;

public record AdminCategoryResponse(
        Long categoryId,
        String type,
        String slug,
        boolean premium,
        boolean active) {

    static AdminCategoryResponse from(CategoryReference reference) {
        return new AdminCategoryResponse(
                reference.categoryId(),
                reference.type().name(),
                reference.slug(),
                reference.premium(),
                reference.active());
    }
}
