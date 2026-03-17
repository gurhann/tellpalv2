package com.tellpal.v2.asset.application;

import com.tellpal.v2.asset.domain.StorageProvider;

public class MediaAssetAlreadyExistsException extends RuntimeException {

    public MediaAssetAlreadyExistsException(StorageProvider provider, String objectPath) {
        super("Media asset already exists for provider " + provider + " and path " + objectPath);
    }
}
