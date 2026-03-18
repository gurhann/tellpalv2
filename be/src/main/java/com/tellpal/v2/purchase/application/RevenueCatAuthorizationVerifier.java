package com.tellpal.v2.purchase.application;

/**
 * Port for verifying webhook authorization against RevenueCat credentials.
 */
public interface RevenueCatAuthorizationVerifier {

    void verify(String authorizationHeader);
}
