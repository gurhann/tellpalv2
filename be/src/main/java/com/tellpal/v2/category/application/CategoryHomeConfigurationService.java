package com.tellpal.v2.category.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Atomically saves one presentation language's home configuration, preserving every curation lane. */
@Service
public class CategoryHomeConfigurationService {
    private final CategoryHomeStore store;
    public CategoryHomeConfigurationService(CategoryHomeStore store) { this.store=store; }
    public record Snapshot(String language, String revision, List<CategoryHomeStore.Row> rows) {}
    public record Configuration(long categoryId, Integer displayOrder, String audience, String contentLanguage) {}
    public static class Conflict extends RuntimeException { public Conflict(String message) { super(message); } }
    public static class Missing extends RuntimeException { public Missing() { super("Category localization was not found"); } }
    @Transactional(readOnly=true)
    public Snapshot read(String language) {
        String lang=language(language);
        List<CategoryHomeStore.Row> rows=store.list(lang);
        return new Snapshot(lang,revision(rows),rows);
    }
    @Transactional(readOnly=true)
    public CategoryHomeStore.Row read(long id, String language) {
        return read(language).rows().stream().filter(r->r.categoryId()==id).findFirst().orElseThrow(Missing::new);
    }
    @Transactional
    public Snapshot save(String language, String expectedRevision, List<Configuration> configurations) {
        String lang=language(language);
        store.lockLanguage(lang);
        Snapshot current=read(lang);
        if (!Objects.equals(expectedRevision,current.revision())) throw new Conflict("Library order changed; reload before saving");
        if (configurations==null || configurations.size()!=current.rows().size()) throw new Conflict("Library category set changed; reload before saving");
        var ids=new HashSet<Long>(); var orders=new HashSet<Integer>();
        for (Configuration c: configurations) {
            if (c==null || !ids.add(c.categoryId())) throw new IllegalArgumentException("Duplicate category ID");
            if (current.rows().stream().noneMatch(r->r.categoryId()==c.categoryId())) throw new Missing();
            validate(c,orders);
        }
        store.deferOrderConstraint();
        for (Configuration c:configurations) update(lang,c);
        return read(lang);
    }
    @Transactional
    public CategoryHomeStore.Row save(long id, String language, Configuration c) {
        String lang=language(language); store.lockLanguage(lang); read(id,lang);
        validate(c,new HashSet<>());
        if (c.categoryId()!=id) throw new IllegalArgumentException("Category ID differs from path");
        if (c.displayOrder()!=null && store.list(lang).stream().anyMatch(r->r.categoryId()!=id && c.displayOrder().equals(r.displayOrder())))
            throw new Conflict("Home order is already occupied; use atomic row reorder");
        update(lang,c); return read(id,lang);
    }
    private void validate(Configuration c, HashSet<Integer> orders) {
        if (c.displayOrder()!=null && (c.displayOrder()<0 || !orders.add(c.displayOrder()))) throw new IllegalArgumentException("Home positions must be unique and nonnegative");
        if (c.audience()==null || !List.of("ALL","NON_SUBSCRIBER").contains(c.audience())) throw new IllegalArgumentException("Invalid home audience");
        if (c.contentLanguage()!=null) {
            String content=language(c.contentLanguage());
            if (!store.localizationExists(c.categoryId(),content)) throw new IllegalArgumentException("Explicit content-language curation lane is missing");
        }
    }
    private void update(String language, Configuration c) {
        store.update(language,new CategoryHomeStore.Row(c.categoryId(),null,null,null,null,false,null,c.displayOrder(),c.audience(),
                c.contentLanguage()==null ? null : language(c.contentLanguage()),0));
    }
    private String language(String value) {
        String lang=LanguageCode.from(value).value();
        if (!store.languageEnabled(lang)) throw new IllegalArgumentException("Library language is unavailable");
        return lang;
    }
    private static String revision(List<CategoryHomeStore.Row> rows) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rows.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
