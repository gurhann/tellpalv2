package com.tellpal.v2.content.application.query;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.asset.api.StoryDetailHeroQueryApi;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Resolves exact-language metadata and server access without full delivery hydration. */
@Service
@Transactional(readOnly=true)
public class StoryDetailMetadataQueryService implements StoryDetailMetadataQueryApi {
    private final StoryDetailMetadataReadRepository reads;
    private final ContentFreeAccessApi freeAccess;
    private final StoryDetailHeroQueryApi heroes;
    public StoryDetailMetadataQueryService(StoryDetailMetadataReadRepository reads,
            ContentFreeAccessApi freeAccess, StoryDetailHeroQueryApi heroes) {
        this.reads=reads; this.freeAccess=freeAccess; this.heroes=heroes;
    }
    @Override public Optional<StoryDetailMetadata> find(long id, LanguageCode language, boolean subscriber) {
        if(id<=0 || language==null) throw new IllegalArgumentException("Positive story identity and language are required");
        return reads.findEligible(id,language.value()).map(row -> {
            var credits=reads.credits(id,language.value());
            if(credits.size()>100) throw new IllegalStateException("Story credits exceed metadata bound");
            boolean free=freeAccess.resolveFreeAccess(language,null).contentIds().contains(id);
            return new StoryDetailMetadata(id,row.externalKey(),row.title(),row.ageRange(),row.durationMinutes(),
                row.summary(),credits,free,!free&&!subscriber,heroes.find(id,language.value(),row.sourceAssetId()));
        });
    }
}
