package com.tellpal.v2.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class StoryPageCountPropertyTest {

    @Property(tries = 120)
    void storyPageCountMatchesSequentiallyAddedPages(@ForAll @IntRange(min = 1, max = 25) int pageCount) {
        Content content = Content.create(ContentType.STORY, "story-pages-" + pageCount, null, true);

        IntStream.rangeClosed(1, pageCount)
                .forEach(content::addStoryPage);

        assertThat(content.getPageCount()).isEqualTo(pageCount);
        assertThat(content.getStoryPages()).hasSize(pageCount);
        assertThat(content.getStoryPages().stream().map(StoryPage::getPageNumber).toList())
                .containsExactlyElementsOf(IntStream.rangeClosed(1, pageCount).boxed().toList());
    }
}
