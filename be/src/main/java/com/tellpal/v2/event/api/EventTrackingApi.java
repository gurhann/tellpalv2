package com.tellpal.v2.event.api;

import com.tellpal.v2.event.api.EventTrackingCommands.RecordAppEventCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordBatchEventsCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordContentEventCommand;
import com.tellpal.v2.event.api.EventTrackingResults.EventBatchIngestResult;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestReceipt;

/**
 * Module-facing ingest API for content and application events.
 */
public interface EventTrackingApi {

    /**
     * Records one content event or returns a duplicate receipt when the event was already seen.
     */
    EventIngestReceipt recordContentEvent(RecordContentEventCommand command);

    /**
     * Records one application event or returns a duplicate receipt when the event was already seen.
     */
    EventIngestReceipt recordAppEvent(RecordAppEventCommand command);

    /**
     * Records a mixed batch of content and application events.
     */
    EventBatchIngestResult recordBatchEvents(RecordBatchEventsCommand command);
}
