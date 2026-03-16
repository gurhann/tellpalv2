package com.tellpal.v2.presentation.api.public_api;

import com.tellpal.v2.content.application.ContentApplicationService;
import com.tellpal.v2.content.application.FreeAccessService;
import com.tellpal.v2.content.domain.Content;
import com.tellpal.v2.content.domain.ContentLocalization;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.content.domain.StoryPage;
import com.tellpal.v2.presentation.dto.content.PublicContentDetailResponse;
import com.tellpal.v2.presentation.dto.content.PublicContentResponse;
import com.tellpal.v2.presentation.dto.content.StoryPageResponse;
import com.tellpal.v2.shared.domain.LocalizationStatus;
import com.tellpal.v2.shared.domain.ProcessingStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/contents")
public class ContentPublicController {

    private final ContentApplicationService contentApplicationService;
    private final FreeAccessService freeAccessService;

    public ContentPublicController(ContentApplicationService contentApplicationService,
                                   FreeAccessService freeAccessService) {
        this.contentApplicationService = contentApplicationService;
        this.freeAccessService = freeAccessService;
    }

    @GetMapping
    public ResponseEntity<List<PublicContentResponse>> listContents(
            @RequestParam String lang,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String freeKey) {

        ContentType contentType = type != null ? ContentType.valueOf(type) : null;

        List<PublicContentResponse> responses = contentApplicationService.listContents(contentType)
                .stream()
                .flatMap(content -> {
                    Optional<ContentLocalization> locOpt = contentApplicationService
                            .findLocalization(content.getId(), lang);
                    return locOpt
                            .filter(loc -> loc.getStatus() == LocalizationStatus.PUBLISHED
                                    && loc.getProcessingStatus() == ProcessingStatus.COMPLETED)
                            .map(loc -> toPublicContentResponse(content, loc, freeKey))
                            .stream();
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicContentDetailResponse> getContent(
            @PathVariable Long id,
            @RequestParam String lang) {

        Content content = contentApplicationService.getContent(id);
        Optional<ContentLocalization> locOpt = contentApplicationService.findLocalization(id, lang);

        if (locOpt.isEmpty()
                || locOpt.get().getStatus() != LocalizationStatus.PUBLISHED
                || locOpt.get().getProcessingStatus() != ProcessingStatus.COMPLETED) {
            return ResponseEntity.notFound().build();
        }

        ContentLocalization loc = locOpt.get();
        boolean isFree = freeAccessService.isFree(id, lang, null);
        PublicContentDetailResponse response = new PublicContentDetailResponse(
                content.getId(), content.getType().name(), content.getExternalKey(),
                loc.getTitle(), loc.getDescription(), loc.getBodyText(),
                isFree, content.getPageCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pages")
    public ResponseEntity<List<StoryPageResponse>> getStoryPages(
            @PathVariable Long id,
            @RequestParam String lang) {

        contentApplicationService.getContent(id); // ensure content exists

        List<StoryPageResponse> pages = contentApplicationService.getStoryPages(id)
                .stream()
                .map(p -> new StoryPageResponse(p.getPageNumber(), p.getIllustrationMediaId()))
                .toList();

        return ResponseEntity.ok(pages);
    }

    private PublicContentResponse toPublicContentResponse(Content content,
                                                          ContentLocalization loc,
                                                          String freeKey) {
        boolean isFree = freeAccessService.isFree(content.getId(), loc.getLanguageCode(), freeKey);
        return new PublicContentResponse(
                content.getId(), content.getType().name(), content.getExternalKey(),
                loc.getTitle(), loc.getDescription(),
                isFree, content.getPageCount());
    }
}
