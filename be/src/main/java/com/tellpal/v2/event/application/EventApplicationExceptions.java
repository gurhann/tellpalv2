package com.tellpal.v2.event.application;

public final class EventApplicationExceptions {

    private EventApplicationExceptions() {
    }

    public static final class ReferencedContentNotFoundException extends RuntimeException {

        public ReferencedContentNotFoundException(Long contentId) {
            super("Referenced content not found: " + contentId);
        }
    }
}
