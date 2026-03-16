package com.tellpal.v2.presentation.dto.category;

public record CategoryResponse(Long id, String slug, String type, boolean isPremium, boolean isActive) {
}
