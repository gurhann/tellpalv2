package com.tellpal.v2.event.application;

import com.tellpal.v2.event.api.AppEventAttributionCandidate;
import com.tellpal.v2.event.api.AttributionAppEventType;
import com.tellpal.v2.event.domain.AppEvent;
import com.tellpal.v2.event.domain.AppEventType;

final class EventApiMapper {

    private EventApiMapper() {
    }

    static AppEventAttributionCandidate toAttributionCandidate(AppEvent appEvent) {
        return new AppEventAttributionCandidate(
                appEvent.getEventId(),
                appEvent.getProfileId(),
                toAttributionType(appEvent.getEventType()),
                appEvent.getContentId(),
                appEvent.getOccurredAt());
    }

    private static AttributionAppEventType toAttributionType(AppEventType eventType) {
        return switch (eventType) {
            case PAYWALL_SHOWN -> AttributionAppEventType.PAYWALL_SHOWN;
            case LOCKED_CONTENT_CLICKED -> AttributionAppEventType.LOCKED_CONTENT_CLICKED;
            default -> throw new IllegalArgumentException("Unsupported attribution event type: " + eventType);
        };
    }
}
