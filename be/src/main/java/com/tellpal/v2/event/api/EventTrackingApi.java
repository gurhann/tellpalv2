package com.tellpal.v2.event.api;

import com.tellpal.v2.event.api.EventTrackingCommands.RecordAppEventCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordBatchEventsCommand;
import com.tellpal.v2.event.api.EventTrackingCommands.RecordContentEventCommand;
import com.tellpal.v2.event.api.EventTrackingResults.EventBatchIngestResult;
import com.tellpal.v2.event.api.EventTrackingResults.EventIngestReceipt;

public interface EventTrackingApi {

    EventIngestReceipt recordContentEvent(RecordContentEventCommand command);

    EventIngestReceipt recordAppEvent(RecordAppEventCommand command);

    EventBatchIngestResult recordBatchEvents(RecordBatchEventsCommand command);
}
