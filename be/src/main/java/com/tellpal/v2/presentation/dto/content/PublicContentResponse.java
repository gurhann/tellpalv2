package com.tellpal.v2.presentation.dto.content;

public record PublicContentResponse(Long id, String type, String externalKey,
                                    String title, String description,
                                    boolean isFree, Integer pageCount) {
}
