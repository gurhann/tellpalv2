package com.tellpal.v2.event.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EventTrackingResults {

    private EventTrackingResults() {
    }

    public enum EventStream {
        CONTENT,
        APP
    }

    public enum EventIngestStatus {
        RECORDED,
        DUPLICATE_EVENT_ID,
        DUPLICATE_LEGACY_EVENT_KEY
    }

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

        public boolean recorded() {
            return status == EventIngestStatus.RECORDED;
        }
    }

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
