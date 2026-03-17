package com.tellpal.v2.content.web.mobile;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.api.PublicContentQueryApi;
import com.tellpal.v2.shared.domain.LanguageCode;

@RestController
@RequestMapping("/api/contents")
public class ContentMobileController {

    private final PublicContentQueryApi publicContentQueryApi;

    public ContentMobileController(PublicContentQueryApi publicContentQueryApi) {
        this.publicContentQueryApi = publicContentQueryApi;
    }

    @GetMapping
    public List<MobileContentSummaryResponse> listContents(
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "type", required = false) ContentApiType type,
            @RequestParam(name = "freeKey", required = false) String freeKey) {
        return publicContentQueryApi.listContents(LanguageCode.from(languageCode), freeKey, type).stream()
                .map(MobileContentSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{contentId}")
    public MobileContentDetailsResponse getContent(
            @PathVariable Long contentId,
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "freeKey", required = false) String freeKey) {
        return publicContentQueryApi.findContent(contentId, LanguageCode.from(languageCode), freeKey)
                .map(MobileContentDetailsResponse::from)
                .orElseThrow(() -> new MobileContentNotFoundException(contentId, languageCode));
    }

    @GetMapping("/{contentId}/pages")
    public List<MobileStoryPageResponse> listPages(
            @PathVariable Long contentId,
            @RequestParam("lang") String languageCode) {
        return publicContentQueryApi.findStoryPages(contentId, LanguageCode.from(languageCode))
                .map(pages -> pages.stream().map(MobileStoryPageResponse::from).toList())
                .orElseThrow(() -> new MobileContentNotFoundException(contentId, languageCode));
    }
}
