package com.tellpal.v2.asset.application;

public class MediaAssetContentUnavailableException extends RuntimeException {

    public MediaAssetContentUnavailableException(Long assetId) {
        super("Media asset " + assetId + " is not available for CMS preview");
    }
}
