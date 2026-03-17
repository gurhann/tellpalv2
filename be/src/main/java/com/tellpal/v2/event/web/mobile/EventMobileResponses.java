package com.tellpal.v2.event.web.mobile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tellpal.v2.event.api.EventTrackingResults.EventBatchIngestResult;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestReceipt;

record EventReceiptResponse(
        UUID eventId,
        String stream,
        String status,
        Instant ingestedAt) {

    static EventReceiptResponse from(EventIngestReceipt receipt) {
        return new EventReceiptResponse(
                receipt.eventId(),
                receipt.stream().name(),
                receipt.status().name(),
                receipt.ingestedAt());
    }
}

record EventBatchResponse(
        long recordedCount,
        long duplicateCount,
        List<EventReceiptResponse> receipts) {

    static EventBatchResponse from(EventBatchIngestResult result) {
        return new EventBatchResponse(
                result.recordedCount(),
                result.duplicateCount(),
                result.receipts().stream().map(EventReceiptResponse::from).toList());
    }
}
