package com.tellpal.v2.content.application;

import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.content.api.ReadingImageSourceSnapshotApi;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Captures scalar provenance; the caller must recompare bindings before a future P3 adoption. */
@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ,
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
public class ReadingImageSourceSnapshotService implements ReadingImageSourceSnapshotApi {
    private final ContentRepository contents;
    public ReadingImageSourceSnapshotService(ContentRepository contents) { this.contents = contents; }

    @Override
    public Snapshot capture(long contentId, String language) {
        var code = LanguageCode.from(language);
        if (!code.value().equals(language)) throw new IllegalArgumentException("Exact canonical language required");
        var bindings = new ArrayList<Binding>();
        var errors = new ArrayList<String>();
        var optional = contents.findById(contentId);
        if (optional.isEmpty()) return new Snapshot(contentId, language, bindings, java.util.List.of("CONTENT_NOT_FOUND"));
        var content = optional.get();
        if (content.getType() != ContentType.STORY) errors.add("STORY_REQUIRED");
        var localized = content.findLocalization(code);
        if (localized.isEmpty() || localized.get().getCoverMediaId() == null) errors.add("EXACT_LANGUAGE_COVER_REQUIRED");
        else {
            var cover = localized.get();
            bindings.add(new Binding(Role.READING_COVER, cover.getId(), null, 0, cover.getCoverMediaId()));
            bindings.add(new Binding(Role.READING_DETAIL, cover.getId(), null, 0, cover.getCoverMediaId()));
        }
        var pages = content.getStoryPages().stream().sorted(Comparator.comparingInt(StoryPage::getPageNumber)).toList();
        if (pages.isEmpty()) errors.add("EMPTY_PAGE_SET");
        for (var page : pages) {
            var pageLocalization = page.findLocalization(code);
            if (pageLocalization.isEmpty() || pageLocalization.get().getIllustrationMediaId() == null) {
                errors.add("EXACT_LANGUAGE_PAGE_REQUIRED:" + page.getPageNumber());
                continue;
            }
            var loc = pageLocalization.get();
            bindings.add(new Binding(Role.READING_PAGE, loc.getId(), page.getId(), page.getPageNumber(), loc.getIllustrationMediaId()));
        }
        return new Snapshot(contentId, language, bindings, errors);
    }
    @Override
    public boolean matches(Snapshot captured) { return capture(captured.contentId(), captured.language()).equals(captured); }
}
