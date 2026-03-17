package com.tellpal.v2.admin.api;

public interface AdminAuthenticationApi {

    AdminAuthenticationResult login(AdminLoginCommand command);

    AdminAuthenticationResult refresh(AdminRefreshCommand command);

    void logout(AdminLogoutCommand command);
}
