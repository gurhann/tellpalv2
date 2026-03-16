package com.tellpal.v2.presentation.dto.category;

public record PublicCategoryResponse(Long id, String slug, String type, boolean isPremium,
                                     String name, String description) {
}
