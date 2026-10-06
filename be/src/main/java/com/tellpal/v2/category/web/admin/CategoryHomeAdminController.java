package com.tellpal.v2.category.web.admin;

import java.util.List;
import com.tellpal.v2.shared.web.admin.AdminApiController;
import org.springframework.web.bind.annotation.*;
import com.tellpal.v2.category.application.CategoryHomeConfigurationService;
import com.tellpal.v2.category.application.CategoryHomeConfigurationService.Configuration;
import com.tellpal.v2.category.application.CategoryHomeConfigurationService.Snapshot;
import com.tellpal.v2.category.application.CategoryHomeStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** Admin surface for per-language Library configuration and atomic occupied-position exchanges. */
@RestController
@AdminApiController
@RequestMapping("/api/admin/categories")
@Tag(name="Category Library Home",description="Editorial Library rows, independent from card curation.")
@SecurityRequirement(name="adminBearerAuth")
@ApiResponses({@ApiResponse(responseCode="400",description="Invalid configuration (ProblemDetail)"),
    @ApiResponse(responseCode="401",description="Missing or invalid admin token (ProblemDetail)"),
    @ApiResponse(responseCode="403",description="Insufficient permission (ProblemDetail)"),
    @ApiResponse(responseCode="404",description="Localization missing (ProblemDetail)"),
    @ApiResponse(responseCode="409",description="Occupied order or stale edit (ProblemDetail)")})
public class CategoryHomeAdminController {
    private final CategoryHomeConfigurationService service;
    public CategoryHomeAdminController(CategoryHomeConfigurationService service) {this.service=service;}
    @GetMapping("/home") @Operation(summary="Read localized Library rows")
    @ApiResponse(responseCode="200",description="Current rows and revision returned")
    public Snapshot read(@RequestParam("lang") String lang) {return service.read(lang);}
    @PutMapping("/home") @Operation(summary="Atomically save localized Library rows",description="Requires the last-read revision and the complete localized STORY category set.")
    @ApiResponse(responseCode="200",description="Saved rows and new revision returned")
    public Snapshot save(@RequestParam("lang") String lang,@RequestBody SaveRows request) {
        return service.save(lang,request.expectedRevision(),request.rows());
    }
    @GetMapping("/{categoryId}/localizations/{lang}/home") @Operation(summary="Read one category home configuration")
    @ApiResponse(responseCode="200",description="Home configuration returned")
    public CategoryHomeStore.Row readOne(@PathVariable long categoryId,@PathVariable String lang) {return service.read(categoryId,lang);}
    @PutMapping("/{categoryId}/localizations/{lang}/home") @Operation(summary="Save one category home configuration")
    @ApiResponse(responseCode="200",description="Home configuration saved")
    public CategoryHomeStore.Row saveOne(@PathVariable long categoryId,@PathVariable String lang,@RequestBody HomeRequest r) {
        return service.save(categoryId,lang,new Configuration(categoryId,r.displayOrder(),r.audience(),r.contentLanguage()));
    }
    public record HomeRequest(Integer displayOrder,String audience,String contentLanguage) {}
    public record SaveRows(String expectedRevision,List<Configuration> rows) {}
}
