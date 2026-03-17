package com.tellpal.v2.event.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.event.domain.ContentEvent;

interface SpringDataContentEventRepository extends JpaRepository<ContentEvent, UUID> {

    Optional<ContentEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);
}
