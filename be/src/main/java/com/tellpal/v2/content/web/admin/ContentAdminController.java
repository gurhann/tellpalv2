package com.tellpal.v2.content.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.time.Clock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.api.AdminContentQueryApi;
import com.tellpal.v2.content.application.ContentApplicationExceptions.ContentNotFoundException;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.DeleteContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.MarkContentLocalizationProcessingCommand;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateContentCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentManagementCommands;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.ContentPublicationCommands.ArchiveContentLocalizationCommand;
import com.tellpal.v2.content.application.ContentPublicationService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.LocalizationStatus;
import com.tellpal.v2.content.domain.ProcessingStatus;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@AdminApiController
@RequestMapping("/api/admin/contents")
@Tag(name = "Admin Contents", description = "Content creation, localization, and publication endpoints.")
@SecurityRequirement(name = "adminBearerAuth")
public class ContentAdminController {

    private final AdminContentQueryApi adminContentQueryApi;
    private final ContentManagementService contentManagementService;
    private final ContentPublicationService contentPublicationService;
    private final Clock clock;

    public ContentAdminController(
            AdminContentQueryApi adminContentQueryApi,
            ContentManagementService contentManagementService,
            ContentPublicationService contentPublicationService,
            Clock clock) {
        this.adminContentQueryApi = adminContentQueryApi;
        this.contentManagementService = contentManagementService;
        this.contentPublicationService = contentPublicationService;
        this.clock = clock;
    }

    @GetMapping
    @Operation(
            summary = "List content",
            description = "Returns content metadata and localized snapshots for CMS list screens, including inactive entries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content list returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminContentReadResponse> listContents() {
        return adminContentQueryApi.listContents().stream()
                .map(AdminContentReadResponse::from)
                .toList();
    }

    @GetMapping("/{contentId}")
    @Operation(
            summary = "Get one content",
            description = "Returns content metadata and localized snapshots for one content aggregate.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content returned"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminContentReadResponse getContent(@PathVariable Long contentId) {
        return adminContentQueryApi.findContent(contentId)
                .map(AdminContentReadResponse::from)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
    }

    @DeleteMapping("/{contentId}")
    @Operation(
            summary = "Delete content",
            description = "Deactivates one content aggregate and preserves its editorial history for admin reads.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Content deleted"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<Void> deleteContent(@PathVariable Long contentId) {
        contentManagementService.deleteContent(new DeleteContentCommand(contentId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Create content", description = "Creates a new content aggregate with its base metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Content created"),
            @ApiResponse(responseCode = "400", description = "Content request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Content external key is already in use", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public ResponseEntity<AdminContentResponse> createContent(@Valid @RequestBody CreateContentRequest request) {
        AdminContentResponse response = AdminContentResponse.from(contentManagementService.createContent(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{contentId}")
                .buildAndExpand(response.contentId())
                .toUri())
                .body(response);
    }

    @PutMapping("/{contentId}")
    @Operation(summary = "Update content", description = "Updates base metadata and content-scoped covers. listingCoverMediaId is an IMAGE reference available only for LULLABY content.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content updated"),
            @ApiResponse(responseCode = "400", description = "Content update is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Content external key is already in use", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminContentResponse updateContent(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateContentRequest request) {
        return AdminContentResponse.from(contentManagementService.updateContent(request.toCommand(contentId)));
    }

    @PutMapping({"/{contentId}/playback", "/{contentId}/lullaby-playback"})
    @Operation(
            summary = "Update lullaby playback",
            description = "Creates or updates the single shared audio source for LULLABY content.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lullaby playback updated"),
            @ApiResponse(responseCode = "400", description = "Playback request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Playback processing conflicts with the current job state", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminLullabyPlaybackResponse updateLullabyPlayback(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateLullabyPlaybackRequest request) {
        return AdminLullabyPlaybackResponse.from(contentManagementService.upsertLullabyPlayback(
                contentId,
                request.toCommand()));
    }

    @GetMapping("/{contentId}/instruments")
    @Operation(
            summary = "List lullaby instruments",
            description = "Returns one lullaby's ordered instrument selection, optionally with a locale label.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lullaby instruments returned"),
            @ApiResponse(responseCode = "400", description = "Locale or content type is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminLullabyInstrumentResponse> listLullabyInstruments(
            @PathVariable Long contentId,
            @RequestParam(name = "languageCode", required = false) String languageCode,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "lang", required = false) String lang) {
        String resolvedLanguage = firstPresent(languageCode, language, lang);
        return contentManagementService.listLullabyInstruments(
                        contentId,
                        resolvedLanguage == null ? null : LanguageCode.from(resolvedLanguage))
                .stream()
                .map(AdminLullabyInstrumentResponse::from)
                .toList();
    }

    @PutMapping("/{contentId}/instruments")
    @Operation(
            summary = "Replace lullaby instruments",
            description = "Replaces the complete ordered instrument selection for one lullaby by stable catalog codes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lullaby instruments replaced"),
            @ApiResponse(responseCode = "400", description = "Instrument selection is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminLullabyInstrumentResponse> replaceLullabyInstruments(
            @PathVariable Long contentId,
            @RequestParam(name = "languageCode", required = false) String languageCode,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "lang", required = false) String lang,
            @Valid @RequestBody ReplaceLullabyInstrumentsRequest request) {
        String resolvedLanguage = firstPresent(languageCode, language, lang);
        LanguageCode responseLanguage = resolvedLanguage == null ? null : LanguageCode.from(resolvedLanguage);
        return contentManagementService.replaceLullabyInstruments(
                        request.toCommand(contentId), responseLanguage).stream()
                .map(AdminLullabyInstrumentResponse::from)
                .toList();
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @PostMapping("/{contentId}/localizations/{languageCode}")
    @Operation(summary = "Create content localization", description = "Creates one localized content representation for a language.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Content localization created"),
            @ApiResponse(responseCode = "400", description = "Localization request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Content localization already exists", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
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
    @Operation(summary = "Update content localization", description = "Updates one localized content representation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content localization updated"),
            @ApiResponse(responseCode = "400", description = "Localization update is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminContentLocalizationResponse updateLocalization(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertContentLocalizationRequest request) {
        return AdminContentLocalizationResponse.from(
                contentManagementService.updateLocalization(request.toUpdateCommand(contentId, languageCode)));
    }

    @PatchMapping("/{contentId}/localizations/{languageCode}/processing-status")
    @Operation(summary = "Update localization processing status", description = "Marks the processing status of one content localization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Processing status updated"),
            @ApiResponse(responseCode = "400", description = "Processing status request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public AdminContentLocalizationResponse updateLocalizationProcessingStatus(
            @PathVariable Long contentId,
            @PathVariable String languageCode,
            @Valid @RequestBody UpdateContentLocalizationProcessingRequest request) {
        return AdminContentLocalizationResponse.from(contentManagementService.markLocalizationProcessingStatus(
                request.toCommand(contentId, languageCode)));
    }

    @PostMapping("/{contentId}/localizations/{languageCode}/publish")
    @Operation(summary = "Publish content localization", description = "Publishes one localized content item after publication rules are satisfied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content localization published"),
            @ApiResponse(responseCode = "400", description = "Publish request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "409", description = "Publication preconditions are not satisfied", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
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
    @Operation(summary = "Archive content localization", description = "Archives one localized content item and removes it from public visibility.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content localization archived"),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content or localization was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
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
        Boolean active,
        @Positive(message = "textlessCoverMediaId must be positive")
        Long textlessCoverMediaId,
        @Positive(message = "listeningCoverMediaId must be positive")
        Long listeningCoverMediaId,
        @Positive(message = "listingCoverMediaId must be positive")
        Long listingCoverMediaId) {

    UpdateContentCommand toCommand(Long contentId) {
        return new UpdateContentCommand(
                contentId, externalKey, ageRange, active, textlessCoverMediaId, listeningCoverMediaId, listingCoverMediaId);
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
        ProcessingStatus processingStatus,
        java.time.Instant publishedAt,
        @Valid StoryNarrationRequest narration) {

    UpsertContentLocalizationRequest(String title, String description, String bodyText, Long coverMediaId,
            Long audioMediaId, Integer durationMinutes, LocalizationStatus status,
            ProcessingStatus processingStatus, java.time.Instant publishedAt) {
        this(title, description, bodyText, coverMediaId, audioMediaId, durationMinutes, status,
                processingStatus, publishedAt, null);
    }

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
                publishedAt,
                narration == null ? null : narration.toCommand());
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
                publishedAt,
                narration == null ? null : narration.toCommand());
    }
}

record UpdateLullabyPlaybackRequest(
        @NotNull(message = "audioMediaId is required")
        @Positive(message = "audioMediaId must be positive")
        Long audioMediaId,
        @NotNull(message = "durationMinutes is required")
        @Min(value = 0, message = "durationMinutes must not be negative")
        Integer durationMinutes) {

    ContentManagementCommands.LullabyPlaybackCommand toCommand() {
        return new ContentManagementCommands.LullabyPlaybackCommand(audioMediaId, durationMinutes);
    }
}

record ReplaceLullabyInstrumentsRequest(
        @NotNull(message = "instrumentCodes is required")
        List<@NotBlank(message = "instrument code must not be blank") String> instrumentCodes) {

    ContentManagementCommands.LullabyInstrumentSelectionCommand toCommand(Long contentId) {
        return new ContentManagementCommands.LullabyInstrumentSelectionCommand(contentId, instrumentCodes);
    }
}

record StoryNarrationRequest(
        @NotNull(message = "audioMediaId is required") @Positive(message = "audioMediaId must be positive") Long audioMediaId,
        @NotNull(message = "durationMinutes is required") @Min(value = 0, message = "durationMinutes must not be negative") Integer durationMinutes) {
    ContentManagementCommands.StoryNarrationCommand toCommand() {
        return new ContentManagementCommands.StoryNarrationCommand(audioMediaId, durationMinutes);
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
