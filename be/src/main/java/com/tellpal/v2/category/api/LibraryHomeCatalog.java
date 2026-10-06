package com.tellpal.v2.category.api;

import java.util.List;
import com.tellpal.v2.content.api.ContentPreview;

/** Versioned cover-only Library response; it grants no protected-media access. */
public record LibraryHomeCatalog(int schemaVersion, String requestedLanguage,
        LibraryHomeQueryApi.Audience audience, int previewLimit, List<Row> rows) {
    public LibraryHomeCatalog { rows = List.copyOf(rows); }
    public record Row(long categoryId, String slug, String name, String description,
            int displayOrder, String contentLanguage, List<ContentPreview> items) {
        public Row { items = List.copyOf(items); }
    }
}
