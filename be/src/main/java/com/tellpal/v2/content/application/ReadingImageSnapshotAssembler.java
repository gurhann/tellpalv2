package com.tellpal.v2.content.application;

import java.util.*;
import org.springframework.stereotype.Component;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Pure exact-language assembly usable inside the aggregate mutation/adoption lock. */
@Component
public class ReadingImageSnapshotAssembler {
    public Snapshot assemble(Content content, String language) {
        var code = LanguageCode.from(language);
        if (!code.value().equals(language)) throw new IllegalArgumentException("Exact canonical language required");
        long contentId = content.getId();
        var bindings = new ArrayList<Binding>();
        var errors = new ArrayList<String>();
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
}
