package com.tellpal.v2.event.application;

import java.util.UUID;

public class AppEventNotFoundException extends RuntimeException {

    public AppEventNotFoundException(UUID eventId) {
        super("App event not found: " + eventId);
    }
}
