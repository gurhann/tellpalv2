package com.tellpal.v2.purchase.application;

public interface RevenueCatAuthorizationVerifier {

    void verify(String authorizationHeader);
}
