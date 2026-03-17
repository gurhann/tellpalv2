package com.tellpal.v2.shared.infrastructure.observability;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class RequestCorrelationSupport {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = RequestCorrelationSupport.class.getName() + ".requestId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private RequestCorrelationSupport() {
    }

    public static String ensureRequestId(HttpServletRequest request, HttpServletResponse response) {
        String requestId = resolveRequestId(request);
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
}
