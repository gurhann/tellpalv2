package com.tellpal.v2.shared.application;

public class MediaAssetNotFoundException extends RuntimeException {

    public MediaAssetNotFoundException(Long id) {
        super("Media asset not found: " + id);
    }
}
