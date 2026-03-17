package com.tellpal.v2.user.web.mobile;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.user.application.UserApplicationExceptions.AppUserNotFoundException;
import com.tellpal.v2.user.application.UserApplicationExceptions.AssetMediaTypeMismatchException;
import com.tellpal.v2.user.application.UserApplicationExceptions.AssetReferenceNotFoundException;
import com.tellpal.v2.user.application.UserApplicationExceptions.FirebaseTokenVerificationException;
import com.tellpal.v2.user.application.UserApplicationExceptions.UserProfileNotFoundException;

@RestControllerAdvice(basePackageClasses = ProfileMobileController.class)
public class UserMobileExceptionHandler {

    @ExceptionHandler(FirebaseTokenVerificationException.class)
    ProblemDetail handleInvalidFirebaseToken(
            FirebaseTokenVerificationException exception,
            HttpServletRequest request) {
        return create(HttpStatus.UNAUTHORIZED, "Invalid Firebase token", exception.getMessage(), "firebase_auth_error", request);
    }

    @ExceptionHandler(AppUserNotFoundException.class)
    ProblemDetail handleAppUserNotFound(AppUserNotFoundException exception, HttpServletRequest request) {
        return create(HttpStatus.NOT_FOUND, "App user not found", exception.getMessage(), "app_user_not_found", request);
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    ProblemDetail handleUserProfileNotFound(UserProfileNotFoundException exception, HttpServletRequest request) {
        return create(HttpStatus.NOT_FOUND, "User profile not found", exception.getMessage(), "user_profile_not_found", request);
    }

    @ExceptionHandler(AssetReferenceNotFoundException.class)
    ProblemDetail handleAssetReferenceNotFound(AssetReferenceNotFoundException exception, HttpServletRequest request) {
        return create(HttpStatus.BAD_REQUEST, "Invalid avatar reference", exception.getMessage(), "asset_not_found", request);
    }

    @ExceptionHandler(AssetMediaTypeMismatchException.class)
    ProblemDetail handleAssetMediaTypeMismatch(
            AssetMediaTypeMismatchException exception,
            HttpServletRequest request) {
        return create(
                HttpStatus.BAD_REQUEST,
                "Invalid avatar media type",
                exception.getMessage(),
                "asset_media_type_mismatch",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = create(
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
        return create(HttpStatus.BAD_REQUEST, "Invalid request body", "Request body could not be parsed", "invalid_body", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return create(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), "invalid_request", request);
    }

    private ProblemDetail create(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("path", request.getRequestURI());
        return problemDetail;
    }
}
