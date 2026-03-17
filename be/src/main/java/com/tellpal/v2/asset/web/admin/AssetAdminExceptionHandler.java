package com.tellpal.v2.asset.web.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.asset.application.MediaAssetAlreadyExistsException;
import com.tellpal.v2.asset.application.MediaAssetNotFoundException;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@RestControllerAdvice(basePackageClasses = AssetAdminController.class)
public class AssetAdminExceptionHandler {

    private final AdminProblemDetailsFactory problemDetailsFactory;

    public AssetAdminExceptionHandler(AdminProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(MediaAssetAlreadyExistsException.class)
    ProblemDetail handleAlreadyExists(MediaAssetAlreadyExistsException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Media asset already exists",
                exception.getMessage(),
                "media_asset_exists",
                request);
    }

    @ExceptionHandler(MediaAssetNotFoundException.class)
    ProblemDetail handleNotFound(MediaAssetNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Media asset not found",
                exception.getMessage(),
                "media_asset_not_found",
                request);
    }
}
