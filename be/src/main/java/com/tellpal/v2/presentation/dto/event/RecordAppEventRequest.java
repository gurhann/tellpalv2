package com.tellpal.v2.presentation.dto.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecordAppEventRequest(
        UUID eventId,
        Long profileId,
        String eventType,
        OffsetDateTime occurredAt,
        Long contentId,
        String payload,
        String legacyEventKey
) {}
