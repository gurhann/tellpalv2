package com.tellpal.v2.shared.web.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class AdminProblemDetailsFactory {

    private final AdminAuthenticationFacade authenticationFacade;

    public AdminProblemDetailsFactory(AdminAuthenticationFacade authenticationFacade) {
        this.authenticationFacade = authenticationFacade;
    }

    public ProblemDetail create(
            HttpStatusCode status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("requestId", AdminWebRequestSupport.resolveRequestId(request));
        problemDetail.setProperty("path", request.getRequestURI());
        authenticationFacade.currentAdmin().ifPresent(admin -> problemDetail.setProperty("adminUserId", admin.adminUserId()));
        return problemDetail;
    }
}
