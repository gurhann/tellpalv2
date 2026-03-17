package com.tellpal.v2.category.web.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.category.application.CategoryApplicationExceptions.AssetMediaTypeMismatchException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.AssetReferenceNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryContentNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationAlreadyExistsException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.CategoryNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentInactiveException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentLocalizationNotPublishedException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.ContentReferenceNotFoundException;
import com.tellpal.v2.category.application.CategoryApplicationExceptions.DuplicateCategorySlugException;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@RestControllerAdvice(basePackageClasses = {CategoryAdminController.class, CategoryCurationAdminController.class})
public class CategoryAdminExceptionHandler {

    private final AdminProblemDetailsFactory problemDetailsFactory;

    public CategoryAdminExceptionHandler(AdminProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(DuplicateCategorySlugException.class)
    ProblemDetail handleDuplicateSlug(DuplicateCategorySlugException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Duplicate category slug",
                exception.getMessage(),
                "duplicate_category_slug",
                request);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ProblemDetail handleCategoryNotFound(CategoryNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Category not found",
                exception.getMessage(),
                "category_not_found",
                request);
    }

    @ExceptionHandler(CategoryLocalizationAlreadyExistsException.class)
    ProblemDetail handleLocalizationAlreadyExists(
            CategoryLocalizationAlreadyExistsException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Category localization already exists",
                exception.getMessage(),
                "category_localization_exists",
                request);
    }

    @ExceptionHandler(CategoryLocalizationNotFoundException.class)
    ProblemDetail handleLocalizationNotFound(
            CategoryLocalizationNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Category localization not found",
                exception.getMessage(),
                "category_localization_not_found",
                request);
    }

    @ExceptionHandler(CategoryLocalizationNotPublishedException.class)
    ProblemDetail handleCategoryLocalizationNotPublished(
            CategoryLocalizationNotPublishedException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Category localization not published",
                exception.getMessage(),
                "category_localization_not_published",
                request);
    }

    @ExceptionHandler(CategoryContentNotFoundException.class)
    ProblemDetail handleCategoryContentNotFound(
            CategoryContentNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Curated content not found",
                exception.getMessage(),
                "category_content_not_found",
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

    @ExceptionHandler(ContentReferenceNotFoundException.class)
    ProblemDetail handleContentReferenceNotFound(
            ContentReferenceNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Invalid content reference",
                exception.getMessage(),
                "content_not_found",
                request);
    }

    @ExceptionHandler(ContentInactiveException.class)
    ProblemDetail handleContentInactive(ContentInactiveException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Content inactive",
                exception.getMessage(),
                "content_inactive",
                request);
    }

    @ExceptionHandler(ContentLocalizationNotPublishedException.class)
    ProblemDetail handleContentLocalizationNotPublished(
            ContentLocalizationNotPublishedException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Content localization not published",
                exception.getMessage(),
                "content_localization_not_published",
                request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Category state conflict",
                exception.getMessage(),
                "category_state_conflict",
                request);
    }
}
