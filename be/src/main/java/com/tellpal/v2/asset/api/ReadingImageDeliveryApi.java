package com.tellpal.v2.asset.api;

import java.util.List;
import java.util.Map;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;

/** Sabit LIVE kanıtının tam descriptor ve doğrulanmış byte teslimat sınırı. */
public interface ReadingImageDeliveryApi {
    /** Mantıksal doğrulama isteğini bulut auth çağrısından önce toplam bütçeye ayırır. */
    void reserveControl(Limits limits);
    Descriptor descriptor(ReadingPublicationReadyApi.Proof proof);
    byte[] read(ReadingPublicationReadyApi.Proof proof, Selection selection, Limits limits);
    record Limits(long maxEncodedBytes, long maxPixels, long maxMemoryBytes, long deadlineMillis, String runId, long maxTotalOperations, long maxTotalBytes) {
        public Limits {
            if (runId==null || !runId.matches("[A-Za-z0-9_-]{1,120}") || maxTotalOperations<=0 || maxTotalBytes<=0 || maxEncodedBytes <= 0 || maxEncodedBytes > Integer.MAX_VALUE - 8L || maxPixels <= 0
                    || maxMemoryBytes <= 0 || deadlineMillis <= 0 || deadlineMillis > Integer.MAX_VALUE)
                throw new IllegalArgumentException("EXPLICIT_DELIVERY_LIMITS_REQUIRED");
            try {
                if (maxMemoryBytes < Math.addExact(Math.multiplyExact(maxEncodedBytes, 4), Math.multiplyExact(maxPixels, 16)))
                    throw new IllegalArgumentException("DELIVERY_MEMORY_LIMIT");
            } catch (ArithmeticException e) { throw new IllegalArgumentException("DELIVERY_MEMORY_OVERFLOW", e); }
        }
    }
    record Selection(Role role, int pageNumber, String renditionKey, boolean canonicalSource) { }
    record Raster(String provider, String objectKey, String generation, String mime, long byteSize,
            String sha256, int width, int height) { }
    record Source(ReadingImagePreparationApi.Binding binding, Raster raster) { }
    record Rendition(String renditionKey, String sourceSha256, String profileFingerprint, Raster raster) { }
    record Entry(Source source, List<Rendition> renditions) {
        public Entry { renditions=List.copyOf(renditions); }
    }
    record Profile(String fingerprint, int width, int height, boolean allowGrowth) { }
    record Descriptor(List<Entry> images, Map<Role, List<Profile>> profiles) {
        public Descriptor {
            images=List.copyOf(images);
            profiles=profiles.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        }
    }
}
