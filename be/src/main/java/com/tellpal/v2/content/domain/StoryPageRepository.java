package com.tellpal.v2.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryPageRepository extends JpaRepository<StoryPage, StoryPageId> {

    List<StoryPage> findByContentIdOrderByPageNumberAsc(Long contentId);

    long countByContentId(Long contentId);
}
