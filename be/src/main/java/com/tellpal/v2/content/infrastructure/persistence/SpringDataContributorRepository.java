package com.tellpal.v2.content.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.content.domain.Contributor;

interface SpringDataContributorRepository extends JpaRepository<Contributor, Long> {
}
