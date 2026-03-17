package com.tellpal.v2.asset.infrastructure.media;

import com.tellpal.v2.asset.api.AssetKind;

record GeneratedAssetPlan(
        AssetKind kind,
        String objectPath,
        String mimeType) {

    GeneratedAssetPlan {
        if (kind == null) {
            throw new IllegalArgumentException("Asset kind must not be null");
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Object path must not be blank");
        }
    }
}
