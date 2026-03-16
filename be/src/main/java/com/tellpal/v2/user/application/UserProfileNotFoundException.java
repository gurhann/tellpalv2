package com.tellpal.v2.user.application;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(Long profileId) {
        super("UserProfile not found: " + profileId);
    }
}
