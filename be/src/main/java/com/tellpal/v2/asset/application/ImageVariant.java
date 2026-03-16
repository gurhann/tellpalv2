package com.tellpal.v2.asset.application;

public enum ImageVariant {
    THUMBNAIL_PHONE,
    THUMBNAIL_TABLET,
    DETAIL_PHONE,
    DETAIL_TABLET;

    public String suffix() {
        return name().toLowerCase();
    }
}
