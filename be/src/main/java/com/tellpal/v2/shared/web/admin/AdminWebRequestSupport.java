package com.tellpal.v2.shared.web.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.tellpal.v2.shared.infrastructure.observability.RequestCorrelationSupport;

public final class AdminWebRequestSupport {

    public static final String REQUEST_ID_HEADER = RequestCorrelationSupport.REQUEST_ID_HEADER;
    public static final String REQUEST_ID_ATTRIBUTE = RequestCorrelationSupport.REQUEST_ID_ATTRIBUTE;

    private AdminWebRequestSupport() {
    }

    public static String ensureRequestId(HttpServletRequest request, HttpServletResponse response) {
        return RequestCorrelationSupport.ensureRequestId(request, response);
    }

    public static String resolveRequestId(HttpServletRequest request) {
        return RequestCorrelationSupport.resolveRequestId(request);
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",", 2)[0].trim();
    }
}
