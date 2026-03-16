package com.tellpal.v2.presentation.dto.content;

public record ContentResponse(Long id, String type, String externalKey, boolean isActive, String ageRange, Integer pageCount) {
}
