package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.content.domain.Contributor;

interface SpringDataContributorRepository extends JpaRepository<Contributor, Long> {

    List<Contributor> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
