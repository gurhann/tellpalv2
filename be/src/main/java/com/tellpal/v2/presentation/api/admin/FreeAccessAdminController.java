package com.tellpal.v2.presentation.api.admin;

import com.tellpal.v2.content.application.FreeAccessService;
import com.tellpal.v2.content.domain.ContentFreeAccess;
import com.tellpal.v2.presentation.dto.content.AddFreeAccessRequest;
import com.tellpal.v2.presentation.dto.content.FreeAccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/free-access")
public class FreeAccessAdminController {

    private final FreeAccessService freeAccessService;

    public FreeAccessAdminController(FreeAccessService freeAccessService) {
        this.freeAccessService = freeAccessService;
    }

    @PostMapping
    public ResponseEntity<FreeAccessResponse> addFreeAccess(@RequestBody AddFreeAccessRequest request) {
        ContentFreeAccess fa = freeAccessService.addFreeAccess(
                request.accessKey(), request.contentId(), request.languageCode());
        return ResponseEntity.status(201).body(toFreeAccessResponse(fa));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFreeAccess(@RequestParam String accessKey,
                                                  @RequestParam Long contentId,
                                                  @RequestParam String languageCode) {
        freeAccessService.removeFreeAccess(accessKey, contentId, languageCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FreeAccessResponse>> listFreeAccessByKey(@RequestParam String accessKey) {
        List<FreeAccessResponse> list = freeAccessService.listFreeAccessByKey(accessKey)
                .stream()
                .map(this::toFreeAccessResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    private FreeAccessResponse toFreeAccessResponse(ContentFreeAccess fa) {
        return new FreeAccessResponse(fa.getId(), fa.getAccessKey(), fa.getContentId(), fa.getLanguageCode());
    }
}
