package com.tellpal.v2.content.application;

public class ContentContributorNotFoundException extends RuntimeException {

    public ContentContributorNotFoundException(Long id) {
        super("ContentContributor not found: " + id);
    }
}
