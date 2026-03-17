package com.tellpal.v2.shared.web.admin;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthenticationFacade {

    public Optional<AuthenticatedAdmin> currentAdmin() {
        return currentAdmin(SecurityContextHolder.getContext().getAuthentication());
    }

    public Optional<AuthenticatedAdmin> currentAdmin(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
                || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Long adminUserId = parseAdminUserId(jwtAuthenticationToken.getToken().getSubject());
        if (adminUserId == null) {
            return Optional.empty();
        }

        Set<String> roleCodes = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .map(roleCode -> roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return Optional.of(new AuthenticatedAdmin(
                adminUserId,
                authentication.getName(),
                roleCodes));
    }

    private static Long parseAdminUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
