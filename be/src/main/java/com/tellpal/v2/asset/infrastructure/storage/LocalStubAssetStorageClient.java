package com.tellpal.v2.asset.infrastructure.storage;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import com.tellpal.v2.asset.domain.StorageProvider;

@Component
public class LocalStubAssetStorageClient implements AssetStorageClient {

    private static final Duration SIGNED_URL_TTL = Duration.ofHours(1);
    private static final String BASE_URL = "http://localhost:8080/_stub/assets";

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL_STUB;
    }

    @Override
    public StorageSignedDownloadUrl createSignedDownloadUrl(String objectPath, Instant issuedAt) {
        String normalizedObjectPath = stripLeadingSlash(objectPath);
        Instant expiresAt = issuedAt.plus(SIGNED_URL_TTL);
        String encodedPath = UriUtils.encodePath(normalizedObjectPath, StandardCharsets.UTF_8);
        String url = BASE_URL + "/" + encodedPath + "?expiresAt=" + expiresAt;
        return new StorageSignedDownloadUrl(url, expiresAt);
    }

    private static String stripLeadingSlash(String objectPath) {
        return objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
    }
}
