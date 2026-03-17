package com.tellpal.v2.content.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.application.ContentFreeAccessCommands.GrantContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.RevokeContentFreeAccessCommand;
import com.tellpal.v2.content.application.ContentFreeAccessService;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/free-access")
public class FreeAccessAdminController {

    private final ContentFreeAccessService contentFreeAccessService;

    public FreeAccessAdminController(ContentFreeAccessService contentFreeAccessService) {
        this.contentFreeAccessService = contentFreeAccessService;
    }

    @PostMapping
    public ResponseEntity<AdminContentFreeAccessResponse> grantFreeAccess(
            @Valid @RequestBody CreateContentFreeAccessRequest request) {
        AdminContentFreeAccessResponse response = AdminContentFreeAccessResponse.from(
                contentFreeAccessService.grantFreeAccess(request.toCommand()));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{accessKey}/languages/{languageCode}/contents/{contentId}")
                .buildAndExpand(response.accessKey(), response.languageCode(), response.contentId())
                .toUri())
                .body(response);
    }

    @GetMapping
    public java.util.List<AdminContentFreeAccessResponse> listFreeAccessEntries(
            @RequestParam(name = "accessKey", required = false) String accessKey) {
        return contentFreeAccessService.listFreeAccessEntries(accessKey).stream()
                .map(AdminContentFreeAccessResponse::from)
                .toList();
    }

    @DeleteMapping("/{accessKey}/languages/{languageCode}/contents/{contentId}")
    public ResponseEntity<Void> revokeFreeAccess(
            @PathVariable String accessKey,
            @PathVariable String languageCode,
            @PathVariable Long contentId) {
        contentFreeAccessService.revokeFreeAccess(new RevokeContentFreeAccessCommand(
                accessKey,
                contentId,
                LanguageCode.from(languageCode)));
        return ResponseEntity.noContent().build();
    }
}

record CreateContentFreeAccessRequest(
        @NotBlank(message = "accessKey is required")
        String accessKey,
        @Positive(message = "contentId must be positive")
        Long contentId,
        @NotBlank(message = "languageCode is required")
        String languageCode) {

    GrantContentFreeAccessCommand toCommand() {
        return new GrantContentFreeAccessCommand(accessKey, contentId, LanguageCode.from(languageCode));
    }
}
