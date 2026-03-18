package com.tellpal.v2.event.application;

/**
 * Application-layer exceptions raised by event ingest use cases.
 */
public final class EventApplicationExceptions {

    private EventApplicationExceptions() {
    }

    public static final class ReferencedContentNotFoundException extends RuntimeException {

        public ReferencedContentNotFoundException(Long contentId) {
            super("Referenced content not found: " + contentId);
        }
    }
}
