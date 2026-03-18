package com.tellpal.v2.event.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Result types returned by the event tracking API.
 */
public final class EventTrackingResults {

    private EventTrackingResults() {
    }

    /**
     * Identifies which event stream a receipt belongs to.
     */
    public enum EventStream {
        CONTENT,
        APP
    }

    /**
     * Describes whether an event was recorded or recognized as a duplicate.
     */
    public enum EventIngestStatus {
        RECORDED,
        DUPLICATE_EVENT_ID,
        DUPLICATE_LEGACY_EVENT_KEY
    }

    /**
     * Receipt returned after ingesting one event.
     */
    public record EventIngestReceipt(
            UUID eventId,
            EventStream stream,
            EventIngestStatus status,
            Instant ingestedAt) {

        public EventIngestReceipt {
            if (eventId == null) {
                throw new IllegalArgumentException("Event ID must not be null");
            }
            if (stream == null) {
                throw new IllegalArgumentException("Event stream must not be null");
            }
            if (status == null) {
                throw new IllegalArgumentException("Event ingest status must not be null");
            }
            if (ingestedAt == null) {
                throw new IllegalArgumentException("Ingested at must not be null");
            }
        }

        /**
         * Returns whether the event was newly recorded.
         */
        public boolean recorded() {
            return status == EventIngestStatus.RECORDED;
        }
    }

    /**
     * Aggregated ingest result for a batch request.
     */
    public record EventBatchIngestResult(List<EventIngestReceipt> receipts) {

        public EventBatchIngestResult {
            receipts = receipts == null ? List.of() : List.copyOf(receipts);
        }

        public long recordedCount() {
            return receipts.stream().filter(EventIngestReceipt::recorded).count();
        }

        public long duplicateCount() {
            return receipts.size() - recordedCount();
        }
    }
}
