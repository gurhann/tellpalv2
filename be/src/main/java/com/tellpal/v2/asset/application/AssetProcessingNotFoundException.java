package com.tellpal.v2.asset.application;

public class AssetProcessingNotFoundException extends RuntimeException {

    public AssetProcessingNotFoundException(Long contentId, String languageCode) {
        super("Asset processing not found for contentId=" + contentId + ", lang=" + languageCode);
    }
}
