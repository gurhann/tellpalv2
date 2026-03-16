package com.tellpal.v2.event.application;

import java.util.UUID;

public class ContentEventNotFoundException extends RuntimeException {

    public ContentEventNotFoundException(UUID eventId) {
        super("Content event not found: " + eventId);
    }
}
