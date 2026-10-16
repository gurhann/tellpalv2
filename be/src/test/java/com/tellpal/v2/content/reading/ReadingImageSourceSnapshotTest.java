package com.tellpal.v2.content.reading;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.tellpal.v2.content.application.ReadingImageSourceSnapshotService;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;

class ReadingImageSourceSnapshotTest {
    @Test void exactLanguageMissingAndEmptyInputsNeverBecomeOldValidSnapshot() {
        var repository=mock(ContentRepository.class);var content=mock(Content.class);
        when(repository.findById(9L)).thenReturn(Optional.of(content));when(content.getType()).thenReturn(ContentType.STORY);
        when(content.findLocalization(LanguageCode.TR)).thenReturn(Optional.empty());when(content.getStoryPages()).thenReturn(Set.of());
        var service=new ReadingImageSourceSnapshotService(repository);
        assertThat(service.capture(9,"tr").errors()).containsExactly("EXACT_LANGUAGE_COVER_REQUIRED","EMPTY_PAGE_SET");
        assertThatThrownBy(() -> service.capture(9,"TR")).hasMessageContaining("Exact canonical");
        when(repository.findById(10L)).thenReturn(Optional.empty());assertThat(service.capture(10,"tr").errors()).containsExactly("CONTENT_NOT_FOUND");
    }
    @Test void orderedPageAndLocalizationProvenanceAreScalarAndLanguageSpecific() {
        var repository=mock(ContentRepository.class);var content=mock(Content.class);var cover=mock(ContentLocalization.class);
        var page=mock(StoryPage.class);var localized=mock(StoryPageLocalization.class);
        when(repository.findById(9L)).thenReturn(Optional.of(content));when(content.getType()).thenReturn(ContentType.STORY);
        when(content.findLocalization(LanguageCode.TR)).thenReturn(Optional.of(cover));when(cover.getId()).thenReturn(7L);when(cover.getCoverMediaId()).thenReturn(3L);
        when(content.getStoryPages()).thenReturn(Set.of(page));when(page.getId()).thenReturn(11L);when(page.getPageNumber()).thenReturn(1);
        when(page.findLocalization(LanguageCode.TR)).thenReturn(Optional.of(localized));when(localized.getId()).thenReturn(17L);when(localized.getIllustrationMediaId()).thenReturn(4L);
        var snapshot=new ReadingImageSourceSnapshotService(repository).capture(9,"tr");snapshot.requireComplete();
        assertThat(snapshot.bindings()).extracting(b -> b.role()).containsExactly(Role.READING_COVER,Role.READING_DETAIL,Role.READING_PAGE);
        assertThat(snapshot.bindings().get(2).localizationId()).isEqualTo(17);assertThat(snapshot.bindings().get(2).pageId()).isEqualTo(11);
    }
}
