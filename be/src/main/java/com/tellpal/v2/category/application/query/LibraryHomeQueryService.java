package com.tellpal.v2.category.application.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.category.api.LibraryHomeCatalog;
import com.tellpal.v2.category.api.LibraryHomeQueryApi;
import com.tellpal.v2.category.application.CategoryHomeStore;
import com.tellpal.v2.content.api.ContentPreview;
import com.tellpal.v2.content.api.ContentPreviewQueryApi;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Composes bounded scalar preview reads in editorial row/card order without language substitution. */
@Service
@Transactional(readOnly=true)
public class LibraryHomeQueryService implements LibraryHomeQueryApi {
    private final CategoryHomeStore store;
    private final ContentPreviewQueryApi previews;
    private final int limit;
    public LibraryHomeQueryService(CategoryHomeStore store,ContentPreviewQueryApi previews,
            @Value("${tellpal.library.preview-limit:5}") int limit) {
        if(limit<=0 || limit>100) throw new IllegalArgumentException("Library preview limit must be between 1 and 100");
        this.store=store; this.previews=previews; this.limit=limit;
    }
    @Override public LibraryHomeCatalog query(String language, Audience audience) {
        if (audience==null) throw new IllegalArgumentException("Audience is required");
        String lang=LanguageCode.from(language).value();
        if (!store.languageEnabled(lang)) throw new IllegalArgumentException("Library language is unavailable");
        var rows=new ArrayList<LibraryHomeCatalog.Row>();
        for (var row:store.list(lang)) {
            if (!row.active() || !"PUBLISHED".equals(row.status()) || row.displayOrder()==null
                    || (audience==Audience.SUBSCRIBER && "NON_SUBSCRIBER".equals(row.audience()))) continue;
            String contentLanguage=row.contentLanguage()==null ? lang : row.contentLanguage();
            if(!store.languageEnabled(contentLanguage) || !store.localizationPublished(row.categoryId(),contentLanguage)) continue;
            List<ContentPreview> items=new ArrayList<>(); var seen=new HashSet<Long>();
            for(int offset=0;items.size()<limit;offset+=100) {
                List<Long> ids=store.candidates(row.categoryId(),contentLanguage,offset,100);
                if(ids.isEmpty()) break;
                var batch=previews.list(ids, LanguageCode.from(contentLanguage),audience==Audience.SUBSCRIBER);
                for(var item:batch) if(seen.add(item.contentId()) && items.size()<limit) items.add(item);
                if(ids.size()<100) break;
            }
            if(!items.isEmpty()) rows.add(new LibraryHomeCatalog.Row(row.categoryId(),row.slug(),row.name(),row.description(),
                    row.displayOrder(),contentLanguage,items));
        }
        return new LibraryHomeCatalog(1,lang,audience,limit,rows);
    }
}
