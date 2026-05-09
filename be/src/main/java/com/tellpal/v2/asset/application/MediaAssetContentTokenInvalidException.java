package com.tellpal.v2.asset.application;

public class MediaAssetContentTokenInvalidException extends RuntimeException {

    public MediaAssetContentTokenInvalidException(String message) {
        super(message);
    }

    public MediaAssetContentTokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
