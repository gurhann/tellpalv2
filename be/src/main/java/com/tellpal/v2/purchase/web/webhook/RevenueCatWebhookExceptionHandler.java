package com.tellpal.v2.purchase.web.webhook;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookExceptionHandler.class);

    @ExceptionHandler(RevenueCatAuthorizationFailedException.class)
    ProblemDetail handleAuthorizationFailure(
            RevenueCatAuthorizationFailedException exception,
            HttpServletRequest request) {
        log.warn(
                "revenuecat_webhook_failed status={} path={} errorCode={} reason={}",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                "revenuecat_authorization_failed",
                exception.getMessage());
        return create(
                HttpStatus.UNAUTHORIZED,
                "RevenueCat authorization failed",
                exception.getMessage(),
                "revenuecat_authorization_failed",
                request);
    }

    @ExceptionHandler(RevenueCatPayloadFormatException.class)
    ProblemDetail handleInvalidPayload(RevenueCatPayloadFormatException exception, HttpServletRequest request) {
        log.warn(
                "revenuecat_webhook_failed status={} path={} errorCode={} reason={}",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                "revenuecat_invalid_payload",
                exception.getMessage());
        return create(
                HttpStatus.BAD_REQUEST,
                "Invalid RevenueCat payload",
                exception.getMessage(),
                "revenuecat_invalid_payload",
                request);
    }

    @ExceptionHandler(InvalidPurchaseLookupValueException.class)
    ProblemDetail handleUnknownLookup(InvalidPurchaseLookupValueException exception, HttpServletRequest request) {
        log.warn(
                "revenuecat_webhook_failed status={} path={} errorCode={} reason={}",
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                request.getRequestURI(),
                "purchase_lookup_invalid",
                exception.getMessage());
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
        log.warn(
                "revenuecat_webhook_failed status={} path={} errorCode={} reason={}",
                HttpStatus.CONFLICT.value(),
                request.getRequestURI(),
                "purchase_attribution_failed",
                exception.getMessage());
        return create(
                HttpStatus.CONFLICT,
                "Purchase attribution failed",
                exception.getMessage(),
                "purchase_attribution_failed",
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn(
                "revenuecat_webhook_failed status={} path={} errorCode={} reason={}",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                "invalid_body",
                "Request body could not be parsed");
        return create(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "Request body could not be parsed",
                "invalid_body",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        log.warn(
                "revenuecat_webhook_failed status={} path={} errorCode={} reason={}",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                "invalid_request",
                exception.getMessage());
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
