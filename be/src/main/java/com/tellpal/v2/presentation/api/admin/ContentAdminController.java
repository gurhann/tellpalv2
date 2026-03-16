package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.content.application.ContentApplicationService;
import com.tellpal.v2.content.application.ContentPublishingService;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.presentation.dto.content.AddStoryPageRequest;
import com.tellpal.v2.presentation.dto.content.ContentResponse;
import com.tellpal.v2.presentation.dto.content.CreateContentRequest;
import com.tellpal.v2.presentation.dto.content.CreateLocalizationRequest;
import com.tellpal.v2.presentation.dto.content.LocalizationResponse;
import com.tellpal.v2.presentation.dto.content.StoryPageResponse;
import com.tellpal.v2.presentation.dto.content.UpdateContentRequest;
import com.tellpal.v2.presentation.dto.content.UpdateLocalizationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/contents")
public class ContentAdminController {

    private final ContentApplicationService contentApplicationService;
    private final ContentPublishingService contentPublishingService;

    public ContentAdminController(ContentApplicationService contentApplicationService,
                                  ContentPublishingService contentPublishingService) {
        this.contentApplicationService = contentApplicationService;
        this.contentPublishingService = contentPublishingService;
    }

    @GetMapping
    public ResponseEntity<List<ContentResponse>> listContents(@RequestParam(required = false) String type) {
        ContentType contentType = type != null ? ContentType.valueOf(type) : null;
        List<ContentResponse> responses = contentApplicationService.listContents(contentType)
                .stream()
                .map(this::toContentResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<ContentResponse> createContent(@RequestBody CreateContentRequest request) {
        Content content = contentApplicationService.createContent(
                ContentType.valueOf(request.type()), request.externalKey(), request.ageRange());
        return ResponseEntity.status(201).body(toContentResponse(content));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentResponse> getContent(@PathVariable Long id) {
        Content content = contentApplicationService.getContent(id);
        return ResponseEntity.ok(toContentResponse(content));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContentResponse> updateContent(@PathVariable Long id,
                                                         @RequestBody UpdateContentRequest request) {
        Content content = contentApplicationService.updateContent(id, request.ageRange(), request.isActive());
        return ResponseEntity.ok(toContentResponse(content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        contentApplicationService.updateContent(id, null, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/localizations")
    public ResponseEntity<LocalizationResponse> createLocalization(@PathVariable Long id,
                                                                   @RequestBody CreateLocalizationRequest request) {
        ContentLocalization localization = contentApplicationService.createLocalization(
                id, request.languageCode(), request.title(), request.description(), request.bodyText());
        return ResponseEntity.status(201).body(toLocalizationResponse(localization));
    }

    @PutMapping("/{id}/localizations/{lang}")
    public ResponseEntity<LocalizationResponse> updateLocalization(@PathVariable Long id,
                                                                   @PathVariable String lang,
                                                                   @RequestBody UpdateLocalizationRequest request) {
        ContentLocalization localization = contentApplicationService.updateLocalization(
                id, lang, request.title(), request.description(), request.bodyText());
        return ResponseEntity.ok(toLocalizationResponse(localization));
    }

    @PostMapping("/{id}/localizations/{lang}/publish")
    public ResponseEntity<Void> publishLocalization(@PathVariable Long id, @PathVariable String lang) {
        contentPublishingService.publishLocalization(id, lang);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pages")
    public ResponseEntity<StoryPageResponse> addStoryPage(@PathVariable Long id,
                                                          @RequestBody AddStoryPageRequest request) {
        StoryPage page = contentApplicationService.addStoryPage(id, request.pageNumber(), request.illustrationMediaId());
        return ResponseEntity.status(201).body(toStoryPageResponse(page));
    }

    @DeleteMapping("/{id}/pages/{pageNumber}")
    public ResponseEntity<Void> removeStoryPage(@PathVariable Long id, @PathVariable int pageNumber) {
        contentApplicationService.removeStoryPage(id, pageNumber);
        return ResponseEntity.noContent().build();
    }

    private ContentResponse toContentResponse(Content c) {
        return new ContentResponse(c.getId(), c.getType().name(), c.getExternalKey(),
                c.isActive(), c.getAgeRange(), c.getPageCount());
    }

    private LocalizationResponse toLocalizationResponse(ContentLocalization l) {
        return new LocalizationResponse(l.getLanguageCode(), l.getTitle(), l.getDescription(),
                l.getStatus().name(), l.getProcessingStatus().name());
    }

    private StoryPageResponse toStoryPageResponse(StoryPage p) {
        return new StoryPageResponse(p.getPageNumber(), p.getIllustrationMediaId());
    }
}
