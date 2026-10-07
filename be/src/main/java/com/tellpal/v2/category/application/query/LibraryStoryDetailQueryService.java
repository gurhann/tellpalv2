package com.tellpal.v2.category.application.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.category.api.*;
import com.tellpal.v2.category.application.CategoryHomeStore;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Rechecks curation and visibility before exact-language detail and ordered recommendations. */
@Service
@Transactional(readOnly=true)
public class LibraryStoryDetailQueryService implements LibraryStoryDetailQueryApi {
    private final CategoryHomeStore store;
    private final StoryDetailMetadataQueryApi details;
    private final ContentPreviewQueryApi previews;
    private final int limit;
    public LibraryStoryDetailQueryService(CategoryHomeStore store,StoryDetailMetadataQueryApi details,
            ContentPreviewQueryApi previews,@Value("${tellpal.library.preview-limit:5}") int limit) {
        if(limit<1 || limit>100) throw new IllegalArgumentException("Invalid Library preview limit");
        this.store=store; this.details=details; this.previews=previews; this.limit=limit;
    }
    @Override public Optional<LibraryStoryDetail> query(long categoryId,long id,String uiLanguage,
            String contentLanguage,String experience,LibraryHomeQueryApi.Audience audience) {
        if(categoryId<=0 || id<=0 || audience==null || !"READING".equals(experience))
            throw new IllegalArgumentException("Invalid reading detail identity");
        String ui=LanguageCode.from(uiLanguage).value(),lang=LanguageCode.from(contentLanguage).value();
        if(!java.util.List.of("tr","en","pt").contains(ui) || !java.util.List.of("tr","en","pt").contains(lang))
            throw new IllegalArgumentException("Unsupported Library language");
        if(!store.languageEnabled(ui) || !store.languageEnabled(lang)) return Optional.empty();
        var row=store.list(ui).stream().filter(r->r.categoryId()==categoryId).findFirst().orElse(null);
        boolean subscriber=audience==LibraryHomeQueryApi.Audience.SUBSCRIBER;
        if(row==null || !row.active() || !"PUBLISHED".equals(row.status()) || row.displayOrder()==null
            || (subscriber && "NON_SUBSCRIBER".equals(row.audience()))
            || !lang.equals(row.contentLanguage()==null?ui:row.contentLanguage())
            || !store.localizationPublished(categoryId,lang) || !store.member(categoryId,id,lang)) return Optional.empty();
        return details.find(id,LanguageCode.from(lang),subscriber).map(detail -> {
            var recommendations=new ArrayList<ContentPreview>(); var seen=new HashSet<Long>(); seen.add(id);
            for(int offset=0;recommendations.size()<limit;offset+=100) {
                var ids=store.candidates(categoryId,lang,offset,100);
                if(ids.isEmpty()) break;
                for(var item:previews.list(ids,LanguageCode.from(lang),subscriber))
                    if(seen.add(item.contentId()) && recommendations.size()<limit) recommendations.add(item);
                if(ids.size()<100) break;
            }
            return new LibraryStoryDetail(1,categoryId,id,detail.externalKey(),"STORY","READING",ui,lang,audience,
                detail.title(),detail.ageRange(),detail.durationMinutes(),detail.summary(),detail.credits(),detail.isFree(),
                detail.isLocked(),detail.hero(),recommendations);
        });
    }
}
