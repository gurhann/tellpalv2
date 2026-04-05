package com.tellpal.v2.asset.web.admin;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyCompletedException;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyPendingException;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyRunningException;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingLocalizationNotFoundException;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingNotFoundException;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingRetryRequiredException;
import com.tellpal.v2.asset.application.MediaAssetAlreadyExistsException;
import com.tellpal.v2.asset.application.MediaAssetNotFoundException;
import com.tellpal.v2.asset.application.MediaAssetUploadMetadataMismatchException;
import com.tellpal.v2.asset.application.MediaAssetUploadObjectNotFoundException;
import com.tellpal.v2.asset.application.MediaAssetUploadTokenInvalidException;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@RestControllerAdvice(basePackageClasses = {AssetAdminController.class, AssetProcessingAdminController.class})
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

    @ExceptionHandler(MediaAssetUploadTokenInvalidException.class)
    ProblemDetail handleUploadTokenInvalid(
            MediaAssetUploadTokenInvalidException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "Asset upload token is invalid",
                exception.getMessage(),
                "asset_upload_token_invalid",
                request);
    }

    @ExceptionHandler(MediaAssetUploadObjectNotFoundException.class)
    ProblemDetail handleUploadedObjectNotFound(
            MediaAssetUploadObjectNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Uploaded asset object not found",
                exception.getMessage(),
                "asset_upload_object_not_found",
                request);
    }

    @ExceptionHandler(MediaAssetUploadMetadataMismatchException.class)
    ProblemDetail handleUploadedMetadataMismatch(
            MediaAssetUploadMetadataMismatchException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Uploaded asset metadata does not match the upload request",
                exception.getMessage(),
                "asset_upload_metadata_mismatch",
                request);
    }

    @ExceptionHandler(AssetProcessingNotFoundException.class)
    ProblemDetail handleProcessingNotFound(AssetProcessingNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Asset processing not found",
                exception.getMessage(),
                "asset_processing_not_found",
                request);
    }

    @ExceptionHandler(AssetProcessingLocalizationNotFoundException.class)
    ProblemDetail handleLocalizationNotFound(
            AssetProcessingLocalizationNotFoundException exception,
            HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "Content localization not found",
                exception.getMessage(),
                "asset_processing_localization_not_found",
                request);
    }

    @ExceptionHandler({
            AssetProcessingAlreadyPendingException.class,
            AssetProcessingAlreadyRunningException.class,
            AssetProcessingAlreadyCompletedException.class,
            AssetProcessingRetryRequiredException.class
    })
    ProblemDetail handleProcessingConflict(RuntimeException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "Asset processing conflict",
                exception.getMessage(),
                "asset_processing_conflict",
                request);
    }
}
