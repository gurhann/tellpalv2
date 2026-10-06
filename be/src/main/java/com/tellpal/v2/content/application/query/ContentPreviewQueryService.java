package com.tellpal.v2.content.application.query;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.asset.api.LibraryCoverQueryApi;
import com.tellpal.v2.content.api.ContentFreeAccessApi;
import com.tellpal.v2.content.api.ContentPreview;
import com.tellpal.v2.content.api.ContentPreviewQueryApi;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Filters exact-language scalar metadata and resolves only verified listing thumbnails. */
@Service
@Transactional(readOnly=true)
public class ContentPreviewQueryService implements ContentPreviewQueryApi {
    private final ContentPreviewReadRepository reads;
    private final ContentFreeAccessApi freeAccess;
    private final LibraryCoverQueryApi covers;
    public ContentPreviewQueryService(ContentPreviewReadRepository reads,ContentFreeAccessApi freeAccess,LibraryCoverQueryApi covers) {
        this.reads=reads;this.freeAccess=freeAccess;this.covers=covers;
    }
    @Override public List<ContentPreview> list(List<Long> ids, LanguageCode language, boolean subscriber) {
        if(ids==null || language==null || ids.size()>100 || ids.stream().anyMatch(id->id==null || id<=0))
            throw new IllegalArgumentException("Preview query requires a language and at most 100 positive IDs");
        if(ids.isEmpty()) return List.of();
        var rows=reads.findEligible(ids,language.value()).stream().collect(Collectors.toMap(ContentPreviewReadRepository.Row::id,Function.identity()));
        var free=freeAccess.resolveFreeAccess(language,null).contentIds();
        return ids.stream().distinct().filter(rows::containsKey).map(id->{var r=rows.get(id);
            boolean isFree=free.contains(id);
            return new ContentPreview(id,r.externalKey(),"STORY","READING",language.value(),r.title(),r.ageRange(),r.durationMinutes(),
                    isFree,!isFree&&!subscriber,covers.find(id,language.value(),r.sourceAssetId()));
        }).toList();
    }
}
