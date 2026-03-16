package com.tellpal.v2.presentation.dto.category;

public record CreateCategoryRequest(String slug, String type, boolean isPremium) {
}
