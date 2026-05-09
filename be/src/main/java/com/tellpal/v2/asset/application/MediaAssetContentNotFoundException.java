package com.tellpal.v2.asset.application;

public class MediaAssetContentNotFoundException extends RuntimeException {

    public MediaAssetContentNotFoundException(String objectPath) {
        super("Media asset content was not found at storage object path " + objectPath);
    }
}
