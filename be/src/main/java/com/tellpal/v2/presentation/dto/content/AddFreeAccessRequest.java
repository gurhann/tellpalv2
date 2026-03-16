package com.tellpal.v2.presentation.dto.content;

public record AddFreeAccessRequest(String accessKey, Long contentId, String languageCode) {
}
