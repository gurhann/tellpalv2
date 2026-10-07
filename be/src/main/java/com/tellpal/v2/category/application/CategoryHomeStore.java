package com.tellpal.v2.category.application;

import java.util.List;

/** Scalar persistence port for Library configuration and bounded curation reads. */
public interface CategoryHomeStore {
    record Row(long categoryId, String slug, String name, String description, String type,
            boolean active, String status, Integer displayOrder, String audience,
            String contentLanguage, long version) {}
    List<Row> list(String language);
    void lockLanguage(String language);
    void deferOrderConstraint();
    void update(String language, Row row);
    boolean localizationExists(long categoryId, String language);
    boolean languageEnabled(String language);
    boolean localizationPublished(long categoryId,String language);
    boolean member(long categoryId, long contentId, String language);
    List<Long> candidates(long categoryId, String language, int offset, int limit);
}
