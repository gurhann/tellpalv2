package com.tellpal.v2.shared.infrastructure.observability;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request carries a correlation id in both headers and logging context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestCorrelationSupport.ensureRequestId(request, response);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(RequestCorrelationSupport.REQUEST_ID_MDC_KEY, requestId)) {
            filterChain.doFilter(request, response);
        }
    }
}
