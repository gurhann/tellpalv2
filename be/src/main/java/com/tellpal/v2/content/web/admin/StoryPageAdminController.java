package com.tellpal.v2.content.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tellpal.v2.content.application.ContentManagementCommands.AddStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.RemoveStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpdateStoryPageCommand;
import com.tellpal.v2.content.application.ContentManagementCommands.UpsertStoryPageLocalizationCommand;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.web.admin.AdminApiController;

@AdminApiController
@RequestMapping("/api/admin/contents/{contentId}/story-pages")
public class StoryPageAdminController {

    private final StoryPageManagementService storyPageManagementService;

    public StoryPageAdminController(StoryPageManagementService storyPageManagementService) {
        this.storyPageManagementService = storyPageManagementService;
    }

    @PostMapping
    public ResponseEntity<AdminStoryPageResponse> addStoryPage(
            @PathVariable Long contentId,
            @Valid @RequestBody AddStoryPageRequest request) {
        AdminStoryPageResponse response = AdminStoryPageResponse.from(
                storyPageManagementService.addStoryPage(request.toCommand(contentId)));
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{pageNumber}")
                .buildAndExpand(response.pageNumber())
                .toUri())
                .body(response);
    }

    @PutMapping("/{pageNumber}")
    public AdminStoryPageResponse updateStoryPage(
            @PathVariable Long contentId,
            @PathVariable int pageNumber,
            @Valid @RequestBody UpdateStoryPageRequest request) {
        return AdminStoryPageResponse.from(
                storyPageManagementService.updateStoryPage(request.toCommand(contentId, pageNumber)));
    }

    @DeleteMapping("/{pageNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStoryPage(@PathVariable Long contentId, @PathVariable int pageNumber) {
        storyPageManagementService.removeStoryPage(new RemoveStoryPageCommand(contentId, pageNumber));
    }

    @PutMapping("/{pageNumber}/localizations/{languageCode}")
    public AdminStoryPageLocalizationResponse upsertStoryPageLocalization(
            @PathVariable Long contentId,
            @PathVariable int pageNumber,
            @PathVariable String languageCode,
            @Valid @RequestBody UpsertStoryPageLocalizationRequest request) {
        return AdminStoryPageLocalizationResponse.from(
                storyPageManagementService.upsertStoryPageLocalization(
                        request.toCommand(contentId, pageNumber, languageCode)));
    }
}

record AddStoryPageRequest(
        @Positive(message = "pageNumber must be positive")
        int pageNumber,
        @Positive(message = "illustrationMediaId must be positive")
        Long illustrationMediaId) {

    AddStoryPageCommand toCommand(Long contentId) {
        return new AddStoryPageCommand(contentId, pageNumber, illustrationMediaId);
    }
}

record UpdateStoryPageRequest(
        @Positive(message = "illustrationMediaId must be positive")
        Long illustrationMediaId) {

    UpdateStoryPageCommand toCommand(Long contentId, int pageNumber) {
        return new UpdateStoryPageCommand(contentId, pageNumber, illustrationMediaId);
    }
}

record UpsertStoryPageLocalizationRequest(
        String bodyText,
        @Positive(message = "audioMediaId must be positive")
        Long audioMediaId) {

    UpsertStoryPageLocalizationCommand toCommand(Long contentId, int pageNumber, String languageCode) {
        return new UpsertStoryPageLocalizationCommand(
                contentId,
                pageNumber,
                LanguageCode.from(languageCode),
                bodyText,
                audioMediaId);
    }
}
