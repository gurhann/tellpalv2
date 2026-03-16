package com.tellpal.v2.presentation.dto.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecordContentEventRequest(
        UUID eventId,
        Long profileId,
        Long contentId,
        String languageCode,
        String eventType,
        OffsetDateTime occurredAt,
        UUID sessionId,
        Integer leftPage,
        Integer engagementSeconds,
        String metadata,
        String legacyEventKey
) {}
