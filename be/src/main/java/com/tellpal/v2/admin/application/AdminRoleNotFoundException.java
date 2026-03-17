package com.tellpal.v2.admin.application;

public class AdminRoleNotFoundException extends RuntimeException {

    public AdminRoleNotFoundException(String roleCode) {
        super("Admin role not found: " + roleCode);
    }
}
