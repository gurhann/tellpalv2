package com.tellpal.v2.content.application.publication;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Limits;

/** Sayısal çalışma kaydı olmadan hiçbir medya admission'ı açılmaz. */
@Component
public record ReadingImageComparisonProperties(Environment environment,
        @Value("${tellpal.reading-comparison.enabled:false}") boolean enabled,
        @Value("${tellpal.reading-comparison.content-id:0}") long contentId,
        @Value("${tellpal.reading-comparison.language:}") String language,
        @Value("${tellpal.reading-comparison.run-id:}") String runId,
        @Value("${tellpal.reading-comparison.max-encoded-bytes:0}") long maxEncodedBytes,
        @Value("${tellpal.reading-comparison.max-pixels:0}") long maxPixels,
        @Value("${tellpal.reading-comparison.max-memory-bytes:0}") long maxMemoryBytes,
        @Value("${tellpal.reading-comparison.deadline-millis:0}") long deadlineMillis,
        @Value("${tellpal.reading-comparison.lease-millis:0}") long leaseMillis,
        @Value("${tellpal.reading-comparison.max-total-operations:0}") long maxTotalOperations,
        @Value("${tellpal.reading-comparison.max-total-bytes:0}") long maxTotalBytes) {
    public boolean admitted() {
        var profiles = Set.of(environment.getActiveProfiles());
        if (!enabled || !(profiles.contains("local") || profiles.contains("test"))
                || profiles.stream().anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"))
                || contentId <= 0 || contentId==71 || !"tr".equals(language) || runId.isBlank() || leaseMillis <= 0 || leaseMillis > Integer.MAX_VALUE) return false;
        try { limits(); return true; } catch (IllegalArgumentException e) { return false; }
    }
    public Limits limits() { return new Limits(maxEncodedBytes, maxPixels, maxMemoryBytes, deadlineMillis, runId, maxTotalOperations, maxTotalBytes); }
}
