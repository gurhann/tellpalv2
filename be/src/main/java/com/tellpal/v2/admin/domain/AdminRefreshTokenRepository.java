package com.tellpal.v2.admin.domain;

import java.util.Optional;

public interface AdminRefreshTokenRepository {

    Optional<AdminRefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    boolean existsByRefreshTokenHash(String refreshTokenHash);

    AdminRefreshToken save(AdminRefreshToken adminRefreshToken);
}
