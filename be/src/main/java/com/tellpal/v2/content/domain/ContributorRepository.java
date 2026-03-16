package com.tellpal.v2.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContributorRepository extends JpaRepository<Contributor, Long> {

    List<Contributor> findByDisplayNameContainingIgnoreCase(String displayName);
}
