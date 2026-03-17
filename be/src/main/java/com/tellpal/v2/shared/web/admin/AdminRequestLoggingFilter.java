package com.tellpal.v2.shared.web.admin;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminRequestLoggingFilter.class);

    private final AdminAuthenticationFacade authenticationFacade;

    public AdminRequestLoggingFilter(AdminAuthenticationFacade authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = AdminWebRequestSupport.ensureRequestId(request, response);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            AuthenticatedAdmin admin = authenticationFacade.currentAdmin().orElse(null);
            log.info(
                    "admin_request requestId={} method={} path={} status={} adminUserId={} username={} clientIp={} durationMs={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    admin == null ? null : admin.adminUserId(),
                    admin == null ? null : admin.username(),
                    AdminWebRequestSupport.resolveClientIp(request),
                    durationMillis);
        }
    }
}
