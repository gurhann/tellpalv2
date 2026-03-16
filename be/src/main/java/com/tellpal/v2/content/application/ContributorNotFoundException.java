package com.tellpal.v2.content.application;

public class ContributorNotFoundException extends RuntimeException {

    public ContributorNotFoundException(Long id) {
        super("Contributor not found: " + id);
    }
}
