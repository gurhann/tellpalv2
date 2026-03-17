package com.tellpal.v2.admin.web.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.admin.application.AdminAuthenticationFailedException;
import com.tellpal.v2.admin.application.AdminRefreshTokenReuseException;
import com.tellpal.v2.admin.application.AdminUserDisabledException;
import com.tellpal.v2.shared.web.admin.AdminApiController;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@RestControllerAdvice(annotations = AdminApiController.class)
public class AdminAuthExceptionHandler {

    private final AdminProblemDetailsFactory problemDetailsFactory;

    public AdminAuthExceptionHandler(AdminProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(AdminAuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailed(
            AdminAuthenticationFailedException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.UNAUTHORIZED,
                "Authentication failed",
                exception.getMessage(),
                "auth_failed",
                request);
    }

    @ExceptionHandler(AdminUserDisabledException.class)
    ProblemDetail handleDisabledUser(AdminUserDisabledException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.FORBIDDEN,
                "Admin user disabled",
                exception.getMessage(),
                "admin_disabled",
                request);
    }

    @ExceptionHandler(AdminRefreshTokenReuseException.class)
    ProblemDetail handleRefreshTokenReuse(AdminRefreshTokenReuseException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Refresh token reuse detected",
                exception.getMessage(),
                "refresh_token_reuse",
                request);
    }
}
