package com.tellpal.v2.asset.application;

public class MediaAssetUploadTokenInvalidException extends RuntimeException {

    public MediaAssetUploadTokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediaAssetUploadTokenInvalidException(String message) {
        super(message);
    }
}
