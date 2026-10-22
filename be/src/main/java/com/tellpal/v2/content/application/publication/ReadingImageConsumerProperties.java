package com.tellpal.v2.content.application.publication;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Limits;

/** Sayısal çalışma kaydı olmadan hiçbir medya admission'ı açılmaz. */
@Component
public record ReadingImageConsumerProperties(Environment environment,
        @Value("${tellpal.reading-consumer.enabled:false}") boolean enabled,
        @Value("${tellpal.reading-consumer.run-id:}") String runId,
        @Value("${tellpal.reading-consumer.max-encoded-bytes:0}") long maxEncodedBytes,
        @Value("${tellpal.reading-consumer.max-pixels:0}") long maxPixels,
        @Value("${tellpal.reading-consumer.max-memory-bytes:0}") long maxMemoryBytes,
        @Value("${tellpal.reading-consumer.deadline-millis:0}") long deadlineMillis,
        @Value("${tellpal.reading-consumer.lease-millis:0}") long leaseMillis,
        @Value("${tellpal.reading-consumer.max-total-operations:0}") long maxTotalOperations,
        @Value("${tellpal.reading-consumer.max-total-bytes:0}") long maxTotalBytes) {
    public boolean admitted() {
        var profiles = Set.of(environment.getActiveProfiles());
        if (!enabled || !(profiles.contains("local") || profiles.contains("test"))
                || profiles.stream().anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"))
                || runId.isBlank() || leaseMillis <= 0 || leaseMillis > Integer.MAX_VALUE) return false;
        try { limits(); return true; } catch (IllegalArgumentException e) { return false; }
    }
    public Limits limits() { return new Limits(maxEncodedBytes, maxPixels, maxMemoryBytes, deadlineMillis, runId, maxTotalOperations, maxTotalBytes); }
}
