package com.tellpal.v2.content.web.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.content.application.ContentApplicationExceptions.AssetMediaTypeMismatchException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.AssetReferenceNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentFreeAccessAlreadyExistsException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentFreeAccessNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationAlreadyExistsException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentLocalizationNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.DuplicateContentExternalKeyException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.StoryPageNotFoundException;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContributorNotFoundException;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@RestControllerAdvice(basePackageClasses = {
        ContentAdminController.class,
        StoryPageAdminController.class,
        FreeAccessAdminController.class
})
public class ContentAdminExceptionHandler {

    private final AdminProblemDetailsFactory problemDetailsFactory;

    public ContentAdminExceptionHandler(AdminProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(DuplicateContentExternalKeyException.class)
    ProblemDetail handleDuplicateExternalKey(
            DuplicateContentExternalKeyException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Duplicate external key",
                exception.getMessage(),
                "duplicate_external_key",
                request);
    }

    @ExceptionHandler(ContentLocalizationAlreadyExistsException.class)
    ProblemDetail handleLocalizationAlreadyExists(
            ContentLocalizationAlreadyExistsException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Content localization already exists",
                exception.getMessage(),
                "content_localization_exists",
                request);
    }

    @ExceptionHandler(ContentNotFoundException.class)
    ProblemDetail handleContentNotFound(ContentNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Content not found",
                exception.getMessage(),
                "content_not_found",
                request);
    }

    @ExceptionHandler(ContentLocalizationNotFoundException.class)
    ProblemDetail handleLocalizationNotFound(
            ContentLocalizationNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Content localization not found",
                exception.getMessage(),
                "content_localization_not_found",
                request);
    }

    @ExceptionHandler(StoryPageNotFoundException.class)
    ProblemDetail handleStoryPageNotFound(StoryPageNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Story page not found",
                exception.getMessage(),
                "story_page_not_found",
                request);
    }

    @ExceptionHandler(ContributorNotFoundException.class)
    ProblemDetail handleContributorNotFound(ContributorNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Contributor not found",
                exception.getMessage(),
                "contributor_not_found",
                request);
    }

    @ExceptionHandler(ContentFreeAccessAlreadyExistsException.class)
    ProblemDetail handleContentFreeAccessAlreadyExists(
            ContentFreeAccessAlreadyExistsException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Content free access already exists",
                exception.getMessage(),
                "content_free_access_exists",
                request);
    }

    @ExceptionHandler(ContentFreeAccessNotFoundException.class)
    ProblemDetail handleContentFreeAccessNotFound(
            ContentFreeAccessNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Content free access not found",
                exception.getMessage(),
                "content_free_access_not_found",
                request);
    }

    @ExceptionHandler(AssetReferenceNotFoundException.class)
    ProblemDetail handleAssetReferenceNotFound(
            AssetReferenceNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Invalid asset reference",
                exception.getMessage(),
                "asset_not_found",
                request);
    }

    @ExceptionHandler(AssetMediaTypeMismatchException.class)
    ProblemDetail handleAssetMediaTypeMismatch(
            AssetMediaTypeMismatchException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Invalid asset media type",
                exception.getMessage(),
                "asset_media_type_mismatch",
                request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Content state conflict",
                exception.getMessage(),
                "content_state_conflict",
                request);
    }
}
