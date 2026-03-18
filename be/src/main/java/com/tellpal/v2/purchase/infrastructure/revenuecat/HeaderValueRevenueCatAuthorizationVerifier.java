package com.tellpal.v2.purchase.infrastructure.revenuecat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.RevenueCatAuthorizationFailedException;
import com.tellpal.v2.purchase.application.RevenueCatAuthorizationVerifier;

/**
 * Verifies webhook requests by matching the configured RevenueCat authorization header value.
 */
@Component
public class HeaderValueRevenueCatAuthorizationVerifier implements RevenueCatAuthorizationVerifier {

    private final String expectedAuthorizationHeader;

    public HeaderValueRevenueCatAuthorizationVerifier(
            @Value("${tellpal.purchase.revenuecat.authorization-header:}") String expectedAuthorizationHeader) {
        this.expectedAuthorizationHeader = expectedAuthorizationHeader == null
                ? ""
                : expectedAuthorizationHeader.trim();
    }

    /**
     * Rejects requests whose authorization header is missing, blank or different from the configured secret.
     */
    @Override
    public void verify(String authorizationHeader) {
        if (expectedAuthorizationHeader.isBlank()) {
            throw new RevenueCatAuthorizationFailedException("RevenueCat authorization header is not configured");
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new RevenueCatAuthorizationFailedException("RevenueCat authorization header must not be blank");
        }
        if (!expectedAuthorizationHeader.equals(authorizationHeader.trim())) {
            throw new RevenueCatAuthorizationFailedException("RevenueCat authorization header did not match");
        }
    }
}
