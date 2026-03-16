package com.tellpal.v2.presentation.dto.content;

public record CreateLocalizationRequest(String languageCode, String title, String description, String bodyText) {
}
