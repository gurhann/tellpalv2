package com.tellpal.v2.purchase.web.webhook;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.AttributedContentNotFoundException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.InvalidPurchaseLookupValueException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.PurchaseAttributionUserNotFoundException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.PurchaseEventNotFoundException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.RevenueCatAuthorizationFailedException;
import com.tellpal.v2.purchase.application.PurchaseApplicationExceptions.RevenueCatPayloadFormatException;

@RestControllerAdvice(basePackageClasses = RevenueCatWebhookController.class)
public class RevenueCatWebhookExceptionHandler {

    @ExceptionHandler(RevenueCatAuthorizationFailedException.class)
    ProblemDetail handleAuthorizationFailure(
            RevenueCatAuthorizationFailedException exception,
            HttpServletRequest request) {
        return create(
                HttpStatus.UNAUTHORIZED,
                "RevenueCat authorization failed",
                exception.getMessage(),
                "revenuecat_authorization_failed",
                request);
    }

    @ExceptionHandler(RevenueCatPayloadFormatException.class)
    ProblemDetail handleInvalidPayload(RevenueCatPayloadFormatException exception, HttpServletRequest request) {
        return create(
                HttpStatus.BAD_REQUEST,
                "Invalid RevenueCat payload",
                exception.getMessage(),
                "revenuecat_invalid_payload",
                request);
    }

    @ExceptionHandler(InvalidPurchaseLookupValueException.class)
    ProblemDetail handleUnknownLookup(InvalidPurchaseLookupValueException exception, HttpServletRequest request) {
        return create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Unknown lookup value",
                exception.getMessage(),
                "purchase_lookup_invalid",
                request);
    }

    @ExceptionHandler({
            PurchaseEventNotFoundException.class,
            PurchaseAttributionUserNotFoundException.class,
            AttributedContentNotFoundException.class
    })
    ProblemDetail handleAttributionConflict(RuntimeException exception, HttpServletRequest request) {
        return create(
                HttpStatus.CONFLICT,
                "Purchase attribution failed",
                exception.getMessage(),
                "purchase_attribution_failed",
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return create(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "Request body could not be parsed",
                "invalid_body",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return create(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                exception.getMessage(),
                "invalid_request",
                request);
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
