package com.tellpal.v2.shared.web.admin;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = AdminApiController.class)
public class AdminApiExceptionHandler {

    private final AdminProblemDetailsFactory problemDetailsFactory;

    public AdminApiExceptionHandler(AdminProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "Request validation failed",
                "validation_error",
                request);
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, second) -> first));
        problemDetail.setProperty("fieldErrors", fieldErrors);
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "Request body could not be parsed",
                "invalid_body",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                "invalid_request",
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "Admin user does not have permission for this action",
                "access_denied",
                request);
    }
}
