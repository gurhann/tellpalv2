package com.tellpal.v2.user.api;

public interface UserResolutionApi {

    AuthenticatedAppUser resolveOrCreateByIdToken(String idToken);
}
