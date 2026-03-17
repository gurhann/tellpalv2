package com.tellpal.v2.shared.web.admin;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AdminWebRequestSupport {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = AdminWebRequestSupport.class.getName() + ".requestId";

    private AdminWebRequestSupport() {
    }

    public static String ensureRequestId(HttpServletRequest request, HttpServletResponse response) {
        String requestId = resolveRequestId(request);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        return requestId;
    }

    public static String resolveRequestId(HttpServletRequest request) {
        Object requestIdAttribute = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestIdAttribute instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }

        String requestIdHeader = request.getHeader(REQUEST_ID_HEADER);
        if (requestIdHeader != null && !requestIdHeader.isBlank()) {
            String requestId = requestIdHeader.trim();
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            return requestId;
        }

        String generatedRequestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, generatedRequestId);
        return generatedRequestId;
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwardedFor.split(",", 2)[0].trim();
    }
}
