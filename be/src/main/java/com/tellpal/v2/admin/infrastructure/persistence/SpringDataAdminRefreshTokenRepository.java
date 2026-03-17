package com.tellpal.v2.admin.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tellpal.v2.admin.domain.AdminRefreshToken;

interface SpringDataAdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {

    Optional<AdminRefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    boolean existsByRefreshTokenHash(String refreshTokenHash);
}
