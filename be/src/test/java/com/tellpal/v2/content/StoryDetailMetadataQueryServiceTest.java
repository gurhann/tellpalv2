package com.tellpal.v2.content;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.content.application.query.*;
import com.tellpal.v2.asset.api.StoryDetailHeroQueryApi;
import com.tellpal.v2.shared.domain.LanguageCode;

class StoryDetailMetadataQueryServiceTest {
    final StoryDetailMetadataReadRepository reads=mock(StoryDetailMetadataReadRepository.class);
    final ContentFreeAccessApi free=mock(ContentFreeAccessApi.class);
    final StoryDetailHeroQueryApi heroes=mock(StoryDetailHeroQueryApi.class);
    final StoryDetailMetadataQueryService service=new StoryDetailMetadataQueryService(reads,free,heroes);
    @Test void missingExactLocalizationDoesNotResolveAnotherLanguageOrMedia() {
        when(reads.findEligible(1,"tr")).thenReturn(Optional.empty());assertThat(service.find(1,LanguageCode.TR,false)).isEmpty();
        verifyNoInteractions(free,heroes);verify(reads,never()).findEligible(1,"en");
    }
    @Test void nullableMetadataAndSubscriberLockRemainServerOwned() {
        when(reads.findEligible(1,"pt")).thenReturn(Optional.of(new StoryDetailMetadataReadRepository.Row(1,"key","Title",null,null,null,null)));
        when(reads.credits(1,"pt")).thenReturn(List.of());when(free.resolveFreeAccess(LanguageCode.PT,null)).thenReturn(new ResolvedContentFreeAccessSet(LanguageCode.PT,"default",Set.of()));
        var locked=service.find(1,LanguageCode.PT,false).orElseThrow();assertThat(locked.isLocked()).isTrue();assertThat(locked.ageRange()).isNull();assertThat(locked.hero()).isNull();
        assertThat(service.find(1,LanguageCode.PT,true).orElseThrow().isLocked()).isFalse();
    }
}
