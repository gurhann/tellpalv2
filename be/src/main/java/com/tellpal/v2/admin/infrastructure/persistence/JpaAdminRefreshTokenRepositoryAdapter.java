package com.tellpal.v2.admin.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.admin.domain.AdminRefreshToken;
import com.tellpal.v2.admin.domain.AdminRefreshTokenRepository;

@Repository
public class JpaAdminRefreshTokenRepositoryAdapter implements AdminRefreshTokenRepository {

    private final SpringDataAdminRefreshTokenRepository repository;

    public JpaAdminRefreshTokenRepositoryAdapter(SpringDataAdminRefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AdminRefreshToken> findByRefreshTokenHash(String refreshTokenHash) {
        return repository.findByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public boolean existsByRefreshTokenHash(String refreshTokenHash) {
        return repository.existsByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public AdminRefreshToken save(AdminRefreshToken adminRefreshToken) {
        return repository.save(adminRefreshToken);
    }
}
