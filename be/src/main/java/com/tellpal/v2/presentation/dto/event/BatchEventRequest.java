package com.tellpal.v2.presentation.dto.event;

import java.util.Collections;
import java.util.List;

public record BatchEventRequest(
        List<RecordContentEventRequest> contentEvents,
        List<RecordAppEventRequest> appEvents
) {
    public BatchEventRequest {
        if (contentEvents == null) contentEvents = Collections.emptyList();
        if (appEvents == null) appEvents = Collections.emptyList();
    }
}
