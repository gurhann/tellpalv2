package com.tellpal.v2.admin.application;

public class AdminRefreshTokenReuseException extends RuntimeException {

    public AdminRefreshTokenReuseException(String message) {
        super(message);
    }
}
