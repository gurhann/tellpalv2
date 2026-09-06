package com.tellpal.v2.asset.api;

import com.tellpal.v2.shared.domain.LanguageCode;

/** Explicit processing target; localization targets include a language and content targets do not. */
public record AssetProcessingTarget(
        AssetProcessingTargetScope scope,
        Long contentId,
        LanguageCode languageCode) {

    public AssetProcessingTarget {
        if (scope == null) {
            throw new IllegalArgumentException("Asset processing target scope must not be null");
        }
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        if (scope == AssetProcessingTargetScope.LOCALIZATION && languageCode == null) {
            throw new IllegalArgumentException("Localization processing target requires a language code");
        }
        if (scope == AssetProcessingTargetScope.CONTENT && languageCode != null) {
            throw new IllegalArgumentException("Content processing target must not include a language code");
        }
    }

    public static AssetProcessingTarget localization(Long contentId, LanguageCode languageCode) {
        return new AssetProcessingTarget(AssetProcessingTargetScope.LOCALIZATION, contentId, languageCode);
    }

    public static AssetProcessingTarget content(Long contentId) {
        return new AssetProcessingTarget(AssetProcessingTargetScope.CONTENT, contentId, null);
    }

    public boolean isLocalization() {
        return scope == AssetProcessingTargetScope.LOCALIZATION;
    }

    public boolean isContent() {
        return scope == AssetProcessingTargetScope.CONTENT;
    }
}
