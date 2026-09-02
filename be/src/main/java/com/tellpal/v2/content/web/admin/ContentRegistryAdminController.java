package com.tellpal.v2.content.web.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tellpal.v2.content.api.AdminContentQueryApi;
import com.tellpal.v2.content.api.AdminContentRegistryPage;
import com.tellpal.v2.content.api.AdminContentRegistryReadiness;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Serves compact, language-specific content rows for the CMS registry.
 */
@AdminApiController
@RequestMapping("/api/admin/content-registry")
@Tag(name = "Admin Content Registry", description = "Language-specific editorial readiness rows.")
@SecurityRequirement(name = "adminBearerAuth")
public class ContentRegistryAdminController {

    private final AdminContentQueryApi adminContentQueryApi;

    public ContentRegistryAdminController(AdminContentQueryApi adminContentQueryApi) {
        this.adminContentQueryApi = adminContentQueryApi;
    }

    /**
     * Lists a filtered page of content readiness rows for one selected language.
     */
    @GetMapping
    @Operation(summary = "List content registry", description = "Returns paged editorial readiness for a selected language.")
    public AdminContentRegistryPage listRegistry(
            @RequestParam String language,
            @RequestParam(required = false) ContentApiType type,
            @RequestParam(required = false) AdminContentRegistryReadiness readiness,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return adminContentQueryApi.listRegistry(
                LanguageCode.from(language), type, readiness, query, page, size);
    }
}
