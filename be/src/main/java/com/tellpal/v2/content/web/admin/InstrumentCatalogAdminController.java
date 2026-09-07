package com.tellpal.v2.content.web.admin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Admin read endpoint for the managed, locale-resolved instrument catalog. */
@AdminApiController
@RequestMapping({"/api/admin/instrument-catalog", "/api/admin/instruments"})
@Tag(name = "Admin Instrument Catalog", description = "Locale-resolved instrument reference data.")
@SecurityRequirement(name = "adminBearerAuth")
public class InstrumentCatalogAdminController {

    private final ContentManagementService contentManagementService;

    public InstrumentCatalogAdminController(ContentManagementService contentManagementService) {
        this.contentManagementService = contentManagementService;
    }

    @GetMapping
    @Operation(
            summary = "List instrument catalog",
            description = "Returns active instrument catalog options with labels for a supported locale.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instrument catalog returned"),
            @ApiResponse(responseCode = "400", description = "Locale or catalog data is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "401", description = "Admin token is missing or invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "403", description = "Admin user lacks permission", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<AdminInstrumentCatalogResponse> listInstrumentCatalog(
            @RequestParam(name = "languageCode", required = false) String languageCode,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "lang", required = false) String lang) {
        return contentManagementService.listInstrumentCatalog(
                        resolveLanguageCode(languageCode, language, lang))
                .stream()
                .map(AdminInstrumentCatalogResponse::from)
                .toList();
    }

    private static LanguageCode resolveLanguageCode(String languageCode, String language, String lang) {
        String resolved = firstPresent(languageCode, language, lang);
        if (resolved == null) {
            throw new IllegalArgumentException("A supported language code is required");
        }
        return LanguageCode.from(resolved);
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
