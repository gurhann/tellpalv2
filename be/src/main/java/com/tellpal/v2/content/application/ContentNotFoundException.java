package com.tellpal.v2.content.application;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException(Long id) {
        super("Content not found: " + id);
    }
}
