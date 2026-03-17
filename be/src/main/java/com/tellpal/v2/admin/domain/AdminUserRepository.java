package com.tellpal.v2.admin.domain;

import java.util.Optional;

public interface AdminUserRepository {

    Optional<AdminUser> findById(Long id);

    Optional<AdminUser> findByUsername(String username);

    boolean existsByUsername(String username);

    AdminUser save(AdminUser adminUser);
}
