package com.tellpal.v2.admin.infrastructure.security;

import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AdminJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roleCodes = jwt.getClaimAsStringList(AdminJwtService.CLAIM_ROLES);
        Collection<GrantedAuthority> authorities = roleCodes == null
                ? List.of()
                : roleCodes.stream().map(this::toAuthority).toList();

        String principalName = jwt.getClaimAsString(AdminJwtService.CLAIM_USERNAME);
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private GrantedAuthority toAuthority(String roleCode) {
        String authority = roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
        return new SimpleGrantedAuthority(authority);
    }
}
