package com.tellpal.v2.admin.application;

public class AdminUserDisabledException extends RuntimeException {

    public AdminUserDisabledException(Long adminUserId) {
        super("Admin user is disabled: " + adminUserId);
    }
}
