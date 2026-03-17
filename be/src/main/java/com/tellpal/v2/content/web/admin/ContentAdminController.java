package com.tellpal.v2.content.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Clock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.MarkContentLocalizationProcessingCommand;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.ContentPublicationCommands.ArchiveContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentPublicationService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/contents")
public class ContentAdminController {

    private final ContentManagementService contentManagementService;
    private final ContentPublicationService contentPublicationService;
    private final Clock clock;

    public ContentAdminController(
            ContentManagementService contentManagementService,
            ContentPublicationService contentPublicationService,
            Clock clock) {
        this.contentManagementService = contentManagementService;
        this.contentPublicationService = contentPublicationService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<AdminContentResponse> createContent(@Valid @RequestBody CreateContentRequest request) {
        AdminContentResponse response = AdminContentResponse.from(contentManagementService.createContent(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{contentId}")
                .buildAndExpand(response.contentId())
                .toUri())
                .body(response);
    }

    @PutMapping("/{contentId}")
    public AdminContentResponse updateContent(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateContentRequest request) {
        return AdminContentResponse.from(contentManagementService.updateContent(request.toCommand(contentId)));
    }

    @PostMapping("/{contentId}/localizations/{languageCode}")
    public ResponseEntity<AdminContentLocalizationResponse> createLocalization(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertContentLocalizationRequest request) {
        AdminContentLocalizationResponse response = AdminContentLocalizationResponse.from(
                contentManagementService.createLocalization(request.toCreateCommand(contentId, languageCode)));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri())
                .body(response);
    }

    @PutMapping("/{contentId}/localizations/{languageCode}")
    public AdminContentLocalizationResponse updateLocalization(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertContentLocalizationRequest request) {
        return AdminContentLocalizationResponse.from(
                contentManagementService.updateLocalization(request.toUpdateCommand(contentId, languageCode)));
    }

    @PatchMapping("/{contentId}/localizations/{languageCode}/processing-status")
    public AdminContentLocalizationResponse updateLocalizationProcessingStatus(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpdateContentLocalizationProcessingRequest request) {
        return AdminContentLocalizationResponse.from(contentManagementService.markLocalizationProcessingStatus(
                request.toCommand(contentId, languageCode)));
    }

    @PostMapping("/{contentId}/localizations/{languageCode}/publish")
    public AdminContentLocalizationResponse publishLocalization(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @RequestBody(required = false) PublishContentLocalizationRequest request) {
        PublishContentLocalizationRequest effectiveRequest = request == null
                ? new PublishContentLocalizationRequest(null)
                : request;
        return AdminContentLocalizationResponse.from(contentPublicationService.publishLocalization(
                effectiveRequest.toCommand(contentId, languageCode, clock)));
    }

    @PostMapping("/{contentId}/localizations/{languageCode}/archive")
    public AdminContentLocalizationResponse archiveLocalization(
            @PathVariable Long contentId,
            @PathVariable String languageCode) {
        return AdminContentLocalizationResponse.from(contentPublicationService.archiveLocalization(
                new ArchiveContentLocalizationCommand(contentId, LanguageCode.from(languageCode))));
    }
}

record CreateContentRequest(
        @NotNull(message = "type is required")
        ContentType type,
        @NotBlank(message = "externalKey is required")
        String externalKey,
        @Min(value = 0, message = "ageRange must not be negative")
        Integer ageRange,
        @NotNull(message = "active is required")
        Boolean active) {

    CreateContentCommand toCommand() {
        return new CreateContentCommand(type, externalKey, ageRange, active);
    }
}

record UpdateContentRequest(
        @NotBlank(message = "externalKey is required")
        String externalKey,
        @Min(value = 0, message = "ageRange must not be negative")
        Integer ageRange,
        @NotNull(message = "active is required")
        Boolean active) {

    UpdateContentCommand toCommand(Long contentId) {
        return new UpdateContentCommand(contentId, externalKey, ageRange, active);
    }
}

record UpsertContentLocalizationRequest(
        @NotBlank(message = "title is required")
        String title,
        String description,
        String bodyText,
        @Positive(message = "coverMediaId must be positive")
        Long coverMediaId,
        @Positive(message = "audioMediaId must be positive")
        Long audioMediaId,
        @Min(value = 0, message = "durationMinutes must not be negative")
        Integer durationMinutes,
        @NotNull(message = "status is required")
        LocalizationStatus status,
        @NotNull(message = "processingStatus is required")
        ProcessingStatus processingStatus,
        java.time.Instant publishedAt) {

    CreateContentLocalizationCommand toCreateCommand(Long contentId, String languageCode) {
        return new CreateContentLocalizationCommand(
                contentId,
                LanguageCode.from(languageCode),
                title,
                description,
                bodyText,
                coverMediaId,
                audioMediaId,
                durationMinutes,
                status,
                processingStatus,
                publishedAt);
    }

    UpdateContentLocalizationCommand toUpdateCommand(Long contentId, String languageCode) {
        return new UpdateContentLocalizationCommand(
                contentId,
                LanguageCode.from(languageCode),
                title,
                description,
                bodyText,
                coverMediaId,
                audioMediaId,
                durationMinutes,
                status,
                processingStatus,
                publishedAt);
    }
}

record UpdateContentLocalizationProcessingRequest(
        @NotNull(message = "processingStatus is required")
        ProcessingStatus processingStatus) {

    MarkContentLocalizationProcessingCommand toCommand(Long contentId, String languageCode) {
        return new MarkContentLocalizationProcessingCommand(contentId, LanguageCode.from(languageCode), processingStatus);
    }
}

record PublishContentLocalizationRequest(java.time.Instant publishedAt) {

    PublishContentLocalizationCommand toCommand(Long contentId, String languageCode, Clock clock) {
        java.time.Instant effectivePublishedAt = publishedAt == null ? java.time.Instant.now(clock) : publishedAt;
        return new PublishContentLocalizationCommand(contentId, LanguageCode.from(languageCode), effectivePublishedAt);
    }
}
