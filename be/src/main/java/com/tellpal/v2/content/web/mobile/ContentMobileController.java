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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/contents")
@Tag(name = "Mobile Contents", description = "Public content discovery and story page endpoints for mobile clients.")
public class ContentMobileController {

    private final PublicContentQueryApi publicContentQueryApi;

    public ContentMobileController(PublicContentQueryApi publicContentQueryApi) {
        this.publicContentQueryApi = publicContentQueryApi;
    }

    @GetMapping
    @Operation(summary = "List visible contents", description = "Returns visible content summaries for one language and optional filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content summaries returned"),
            @ApiResponse(responseCode = "400", description = "Content query is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<MobileContentSummaryResponse> listContents(
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "type", required = false) ContentApiType type,
            @RequestParam(name = "freeKey", required = false) String freeKey) {
        return publicContentQueryApi.listContents(LanguageCode.from(languageCode), freeKey, type).stream()
                .map(MobileContentSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{contentId}")
    @Operation(summary = "Get one content item", description = "Returns public content details for one visible localization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Content details returned"),
            @ApiResponse(responseCode = "400", description = "Content request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Content was not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public MobileContentDetailsResponse getContent(
            @PathVariable Long contentId,
            @RequestParam("lang") String languageCode,
            @RequestParam(name = "freeKey", required = false) String freeKey) {
        return publicContentQueryApi.findContent(contentId, LanguageCode.from(languageCode), freeKey)
                .map(MobileContentDetailsResponse::from)
                .orElseThrow(() -> new MobileContentNotFoundException(contentId, languageCode));
    }

    @GetMapping("/{contentId}/pages")
    @Operation(summary = "List story pages", description = "Returns localized story pages for a visible story content item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Story pages returned"),
            @ApiResponse(responseCode = "400", description = "Page request is invalid", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
            @ApiResponse(responseCode = "404", description = "Story pages were not found for the requested content", content = @Content(schema = @Schema(ref = "#/components/schemas/ProblemDetail")))
    })
    public List<MobileStoryPageResponse> listPages(
            @PathVariable Long contentId,
            @RequestParam("lang") String languageCode) {
        return publicContentQueryApi.findStoryPages(contentId, LanguageCode.from(languageCode))
                .map(pages -> pages.stream().map(MobileStoryPageResponse::from).toList())
                .orElseThrow(() -> new MobileContentNotFoundException(contentId, languageCode));
    }
}
