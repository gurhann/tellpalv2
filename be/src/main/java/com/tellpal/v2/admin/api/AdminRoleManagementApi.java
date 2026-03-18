package com.tellpal.v2.admin.api;

/**
 * Module-facing use cases for maintaining admin role assignments.
 */
public interface AdminRoleManagementApi {

    /**
     * Assigns the requested role to the admin user when both references exist.
     *
     * <p>Role assignment is idempotent at the aggregate level, so assigning an already linked role
     * leaves the aggregate unchanged.
     */
    void assignRole(AdminAssignRoleCommand command);
}
