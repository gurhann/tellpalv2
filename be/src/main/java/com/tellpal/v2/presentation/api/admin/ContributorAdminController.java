package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.content.application.ContributorApplicationService;
import com.tellpal.v2.content.domain.ContentContributor;
import com.tellpal.v2.content.domain.Contributor;
import com.tellpal.v2.content.domain.ContributorRole;
import com.tellpal.v2.presentation.dto.contributor.AddContentContributorRequest;
import com.tellpal.v2.presentation.dto.contributor.ContentContributorResponse;
import com.tellpal.v2.presentation.dto.contributor.ContributorResponse;
import com.tellpal.v2.presentation.dto.contributor.CreateContributorRequest;
import com.tellpal.v2.presentation.dto.contributor.UpdateContributorRequest;
import com.tellpal.v2.presentation.dto.contributor.UpdateSortOrderRequest;
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
@RequestMapping("/api/admin/contributors")
public class ContributorAdminController {

    private final ContributorApplicationService contributorApplicationService;

    public ContributorAdminController(ContributorApplicationService contributorApplicationService) {
        this.contributorApplicationService = contributorApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<ContributorResponse>> listContributors() {
        List<ContributorResponse> list = contributorApplicationService.listContributors()
                .stream()
                .map(this::toContributorResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<ContributorResponse> createContributor(@RequestBody CreateContributorRequest request) {
        Contributor contributor = contributorApplicationService.createContributor(request.displayName());
        return ResponseEntity.status(201).body(toContributorResponse(contributor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContributorResponse> getContributor(@PathVariable Long id) {
        Contributor contributor = contributorApplicationService.getContributor(id);
        return ResponseEntity.ok(toContributorResponse(contributor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContributorResponse> updateContributor(@PathVariable Long id,
                                                                  @RequestBody UpdateContributorRequest request) {
        Contributor contributor = contributorApplicationService.updateContributor(id, request.displayName());
        return ResponseEntity.ok(toContributorResponse(contributor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContributor(@PathVariable Long id) {
        contributorApplicationService.deleteContributor(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/content-assignments")
    public ResponseEntity<ContentContributorResponse> addContentContributor(
            @RequestBody AddContentContributorRequest request) {
        ContentContributor cc = contributorApplicationService.addContentContributor(
                request.contentId(),
                request.contributorId(),
                ContributorRole.valueOf(request.role()),
                request.languageCode(),
                request.creditName(),
                request.sortOrder());
        return ResponseEntity.status(201).body(toContentContributorResponse(cc));
    }

    @DeleteMapping("/content-assignments")
    public ResponseEntity<Void> removeContentContributor(@RequestParam Long contentId,
                                                          @RequestParam Long contributorId) {
        contributorApplicationService.removeContentContributor(contentId, contributorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/content-assignments")
    public ResponseEntity<List<ContentContributorResponse>> listContentContributors(
            @RequestParam Long contentId) {
        List<ContentContributorResponse> list = contributorApplicationService
                .listContentContributors(contentId)
                .stream()
                .map(this::toContentContributorResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/content-assignments/{id}/sort-order")
    public ResponseEntity<ContentContributorResponse> updateSortOrder(@PathVariable Long id,
                                                                       @RequestBody UpdateSortOrderRequest request) {
        ContentContributor cc = contributorApplicationService.updateSortOrder(id, request.sortOrder());
        return ResponseEntity.ok(toContentContributorResponse(cc));
    }

    private ContributorResponse toContributorResponse(Contributor c) {
        return new ContributorResponse(c.getId(), c.getDisplayName());
    }

    private ContentContributorResponse toContentContributorResponse(ContentContributor cc) {
        return new ContentContributorResponse(
                cc.getId(),
                cc.getContentId(),
                cc.getContributorId(),
                cc.getRole().name(),
                cc.getLanguageCode(),
                cc.getCreditName(),
                cc.getSortOrder());
    }
}
