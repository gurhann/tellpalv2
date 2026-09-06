package com.tellpal.v2.asset.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Domain-facing integration event emitted after a persisted processing status transition.
 */
public record AssetProcessingStatusChangedEvent(
        AssetProcessingTarget target,
        AssetProcessingState status) {

    public AssetProcessingStatusChangedEvent {
        if (target == null) {
            throw new IllegalArgumentException("Asset processing target must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Asset processing state must not be null");
        }
    }

    public AssetProcessingStatusChangedEvent(
            Long contentId, LanguageCode languageCode, AssetProcessingState status) {
        this(AssetProcessingTarget.localization(contentId, languageCode), status);
    }

    public Long contentId() { return target.contentId(); }

    public LanguageCode languageCode() { return target.languageCode(); }
}
