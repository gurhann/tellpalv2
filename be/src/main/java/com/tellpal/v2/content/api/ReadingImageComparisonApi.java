package com.tellpal.v2.content.api;

import java.util.UUID;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;

/** Yalnız sunucuda seçilmiş ücretsiz QA kopyasının retained LIVE karşılaştırması. */
public interface ReadingImageComparisonApi {
    /** Ayarsız/kapalı kapsamı I/O olmadan reddeder; açık run için auth kontrolünü rezerve eder. */
    void requireConfigured(long contentId, String language);
    Admission manifest(long contentId, String language);
    Admission status(Identity identity);
    byte[] read(Identity identity, ReadingImageDeliveryApi.Selection selection);
    record Identity(long contentId, String language, long liveGeneration, String revision, String manifestDigest, UUID jobId) { }
    record Admission(Identity identity, ReadingImageDeliveryApi.Descriptor descriptor, long leaseMillis) { }
}
