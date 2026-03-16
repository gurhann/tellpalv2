package com.tellpal.v2.presentation.dto.media;

public record MediaAssetResponse(
        Long id,
        String provider,
        String objectPath,
        String kind,
        String mimeType,
        Long bytes,
        String checksumSha256,
        String downloadUrl) {
}
