package com.tellpal.v2.user.web.mobile;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import com.tellpal.v2.user.application.LibraryHomeTestQueryService;
import com.tellpal.v2.category.api.LibraryHomeCatalog;
import com.tellpal.v2.category.api.LibraryStoryDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** Profile-restricted, verified Firebase Library listing for explicitly allowlisted development users. */
@RestController
@Profile({"local","test"})
@RequestMapping("/api/test/library")
@Tag(name="Test Library",description="Controlled development reading catalog; it grants no playback rights.")
@SecurityRequirement(name="mobileBearerAuth")
public class LibraryHomeTestController {
    private final LibraryHomeTestQueryService service;
    public LibraryHomeTestController(LibraryHomeTestQueryService service) {this.service=service;}
    @GetMapping @Operation(summary="Get eligible Library home rows")
    @ApiResponses({@ApiResponse(responseCode="200",description="Catalog returned, including empty success"),
        @ApiResponse(responseCode="400",description="Missing, invalid or unavailable language (ProblemDetail)"),
        @ApiResponse(responseCode="401",description="Missing or invalid Firebase token (ProblemDetail)"),
        @ApiResponse(responseCode="403",description="Verified user is not allowlisted (ProblemDetail)")})
    public LibraryHomeCatalog query(@RequestParam(name="lang",required=false) String language,
            @RequestHeader(name="Authorization",required=false) String authorization) {return service.query(authorization,language);}
    /** Returns metadata only after Firebase admission and curated identity checks. */
    @GetMapping("/stories/{contentId}")
    @Operation(summary="Get selected reading story metadata",description="Exact-language curated metadata and cover-only recommendations; no playback rights.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Reading metadata returned"),
        @ApiResponse(responseCode="400",description="Invalid scalar identity or experience (ProblemDetail)"),
        @ApiResponse(responseCode="401",description="Missing or invalid Firebase token (ProblemDetail)"),
        @ApiResponse(responseCode="403",description="Verified user is not allowlisted (ProblemDetail)"),
        @ApiResponse(responseCode="404",description="Selected curated story is unavailable (ProblemDetail)")})
    public LibraryStoryDetail detail(@PathVariable long contentId,
            @RequestParam(name="categoryId",defaultValue="0") long categoryId,
            @RequestParam(name="lang",required=false) String language,
            @RequestParam(name="contentLang",required=false) String contentLanguage,
            @RequestParam(name="experience",required=false) String experience,
            @RequestHeader(name="Authorization",required=false) String authorization) {
        return service.detail(authorization,categoryId,contentId,language,contentLanguage,experience);
    }
}
