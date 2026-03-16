package com.tellpal.v2.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    List<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByUserIdAndIsPrimaryTrue(Long userId);
}
