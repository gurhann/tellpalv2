package com.tellpal.v2.admin.application;

public class AdminAuthenticationFailedException extends RuntimeException {

    public AdminAuthenticationFailedException(String message) {
        super(message);
    }
}
