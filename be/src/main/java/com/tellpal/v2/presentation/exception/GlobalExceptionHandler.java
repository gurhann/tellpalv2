package com.tellpal.v2.presentation.exception;

import com.tellpal.v2.admin.application.AdminAuthException;
import com.tellpal.v2.category.application.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryNotFoundException;
import com.tellpal.v2.content.application.ContentContributorNotFoundException;
import com.tellpal.v2.content.application.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentPublishingException;
import com.tellpal.v2.content.application.ContributorNotFoundException;
import com.tellpal.v2.shared.application.MediaAssetNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AdminAuthException.class)
    public ResponseEntity<ErrorResponse> handleAdminAuthException(AdminAuthException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentNotFoundException(ContentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ContentLocalizationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentLocalizationNotFoundException(ContentLocalizationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ContentPublishingException.class)
    public ResponseEntity<ErrorResponse> handleContentPublishingException(ContentPublishingException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFoundException(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CategoryLocalizationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryLocalizationNotFoundException(CategoryLocalizationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ContributorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContributorNotFoundException(ContributorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ContentContributorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContentContributorNotFoundException(ContentContributorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MediaAssetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMediaAssetNotFoundException(MediaAssetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred"));
    }

    public record ErrorResponse(String message) {
    }
}
