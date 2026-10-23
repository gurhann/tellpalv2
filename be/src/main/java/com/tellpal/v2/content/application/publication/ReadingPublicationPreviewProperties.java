package com.tellpal.v2.content.application.publication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.Limits;

/** Açık yerel çalışma ve toplam bütçe olmadan önizleme byte erişimi açılmaz. */
@Component
public record ReadingPublicationPreviewProperties(
        @Value("${tellpal.content.reading-publication.preview.run-id:}") String runId,
        @Value("${tellpal.content.reading-publication.preview.max-encoded-bytes:0}") long maxEncodedBytes,
        @Value("${tellpal.content.reading-publication.preview.max-pixels:0}") long maxPixels,
        @Value("${tellpal.content.reading-publication.preview.max-memory-bytes:0}") long maxMemoryBytes,
        @Value("${tellpal.content.reading-publication.preview.deadline-millis:0}") long deadlineMillis,
        @Value("${tellpal.content.reading-publication.preview.max-total-operations:0}") long maxTotalOperations,
        @Value("${tellpal.content.reading-publication.preview.max-total-bytes:0}") long maxTotalBytes) {
    public Limits limits() {
        return new Limits(maxEncodedBytes, maxPixels, maxMemoryBytes, deadlineMillis, runId, maxTotalOperations, maxTotalBytes);
    }
}
