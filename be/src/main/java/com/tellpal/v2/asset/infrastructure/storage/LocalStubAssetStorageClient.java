package com.tellpal.v2.asset.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

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

    @Override
    public Optional<StorageObjectMetadata> findObjectMetadata(String objectPath) {
        byte[] content = stubContent(objectPath);
        return Optional.of(new StorageObjectMetadata("application/octet-stream", (long) content.length));
    }

    @Override
    public Optional<StorageObjectContent> openObject(String objectPath, StorageObjectRange range) {
        byte[] bytes = stubContent(objectPath);
        int start = 0;
        int end = bytes.length == 0 ? -1 : bytes.length - 1;
        Long rangeStart = null;
        Long rangeEnd = null;
        if (range != null) {
            start = Math.toIntExact(range.startInclusive());
            end = Math.toIntExact(range.endInclusive());
            rangeStart = range.startInclusive();
            rangeEnd = range.endInclusive();
        }
        byte[] content = end < start
                ? new byte[0]
                : java.util.Arrays.copyOfRange(bytes, start, end + 1);
        return Optional.of(new StorageObjectContent(
                "application/octet-stream",
                bytes.length,
                content.length,
                rangeStart,
                rangeEnd,
                new ByteArrayInputStream(content)));
    }

    private static String stripLeadingSlash(String objectPath) {
        return objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
    }

    private static byte[] stubContent(String objectPath) {
        return ("Local stub asset: " + stripLeadingSlash(objectPath)).getBytes(StandardCharsets.UTF_8);
    }
}
