package com.tellpal.v2.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findByExternalKey(String externalKey);

    List<Content> findAllByIsActiveTrue();

    List<Content> findAllByIsActiveTrueAndType(ContentType type);
}
