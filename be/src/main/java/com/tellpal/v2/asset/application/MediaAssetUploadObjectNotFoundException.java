package com.tellpal.v2.asset.application;

public class MediaAssetUploadObjectNotFoundException extends RuntimeException {

    public MediaAssetUploadObjectNotFoundException(String objectPath) {
        super("Uploaded storage object was not found: " + objectPath);
    }
}
