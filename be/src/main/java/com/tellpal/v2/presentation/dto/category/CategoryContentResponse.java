package com.tellpal.v2.presentation.dto.category;

public record CategoryContentResponse(Long id, Long categoryId, String languageCode, Long contentId, int displayOrder) {
}
