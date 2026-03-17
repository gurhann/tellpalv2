package com.tellpal.v2.asset.api;

import com.tellpal.v2.shared.domain.LanguageCode;

public record AssetProcessingStatusChangedEvent(
        Long contentId,
        LanguageCode languageCode,
        AssetProcessingState status) {

    public AssetProcessingStatusChangedEvent {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Asset processing state must not be null");
        }
    }
}
