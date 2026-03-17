package com.tellpal.v2.admin.web.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.admin.application.AdminAuthenticationFailedException;
import com.tellpal.v2.admin.application.AdminRefreshTokenReuseException;
import com.tellpal.v2.admin.application.AdminUserDisabledException;

@RestControllerAdvice(basePackageClasses = AdminAuthController.class)
public class AdminAuthExceptionHandler {

    @ExceptionHandler(AdminAuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailed(AdminAuthenticationFailedException exception) {
        return problemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.getMessage());
    }

    @ExceptionHandler(AdminUserDisabledException.class)
    ProblemDetail handleDisabledUser(AdminUserDisabledException exception) {
        return problemDetail(HttpStatus.FORBIDDEN, "Admin user disabled", exception.getMessage());
    }

    @ExceptionHandler(AdminRefreshTokenReuseException.class)
    ProblemDetail handleRefreshTokenReuse(AdminRefreshTokenReuseException exception) {
        return problemDetail(HttpStatus.CONFLICT, "Refresh token reuse detected", exception.getMessage());
    }

    private static ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
