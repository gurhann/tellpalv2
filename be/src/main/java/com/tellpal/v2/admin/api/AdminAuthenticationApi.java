package com.tellpal.v2.admin.api;

/**
 * Module-facing use cases for authenticating admin users and managing their token lifecycle.
 */
public interface AdminAuthenticationApi {

    /**
     * Authenticates an active admin user and issues a fresh access token plus refresh token pair.
     *
     * <p>Invalid credentials and disabled users are rejected with business exceptions rather than a
     * partial result.
     */
    AdminAuthenticationResult login(AdminLoginCommand command);

    /**
     * Rotates a valid refresh token and returns a replacement token pair.
     *
     * <p>The current refresh token is revoked as part of the same transaction. Reused, revoked, or
     * expired tokens are rejected.
     */
    AdminAuthenticationResult refresh(AdminRefreshCommand command);

    /**
     * Revokes the supplied refresh token when it is still active.
     *
     * <p>This operation is idempotent and silently ignores tokens that do not resolve to an active
     * session.
     */
    void logout(AdminLogoutCommand command);
}
