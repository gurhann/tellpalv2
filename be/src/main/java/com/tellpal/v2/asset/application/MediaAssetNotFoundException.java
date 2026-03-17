package com.tellpal.v2.asset.application;

public class MediaAssetNotFoundException extends RuntimeException {

    public MediaAssetNotFoundException(Long assetId) {
        super("Media asset not found: " + assetId);
    }
}
