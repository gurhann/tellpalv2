package com.tellpal.v2.presentation.dto.content;

public record PublicContentDetailResponse(Long id, String type, String externalKey,
                                          String title, String description, String bodyText,
                                          boolean isFree, Integer pageCount) {
}
