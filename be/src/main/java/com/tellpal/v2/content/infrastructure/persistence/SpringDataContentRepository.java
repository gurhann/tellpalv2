package com.tellpal.v2.content.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.content.domain.Content;

interface SpringDataContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findByExternalKey(String externalKey);

    boolean existsByExternalKey(String externalKey);

    List<Content> findAllByActiveTrue();

    List<Content> findAllByActiveTrueAndIdIn(Collection<Long> contentIds);
}
