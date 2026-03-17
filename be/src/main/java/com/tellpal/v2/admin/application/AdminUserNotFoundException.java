package com.tellpal.v2.admin.application;

public class AdminUserNotFoundException extends RuntimeException {

    public AdminUserNotFoundException(Long adminUserId) {
        super("Admin user not found: " + adminUserId);
    }
}
