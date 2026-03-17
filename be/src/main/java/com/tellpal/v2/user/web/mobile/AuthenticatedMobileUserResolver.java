package com.tellpal.v2.user.web.mobile;

import org.springframework.stereotype.Component;

import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserResolutionApi;

@Component
final class AuthenticatedMobileUserResolver {

    private final UserResolutionApi userResolutionApi;

    AuthenticatedMobileUserResolver(UserResolutionApi userResolutionApi) {
        this.userResolutionApi = userResolutionApi;
    }

    AuthenticatedAppUser resolveCurrentUser(String authorizationHeader) {
        return userResolutionApi.resolveOrCreateByIdToken(extractBearerToken(authorizationHeader));
    }

    private static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header must be provided");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must use Bearer token");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Bearer token must not be blank");
        }
        return token;
    }
}
