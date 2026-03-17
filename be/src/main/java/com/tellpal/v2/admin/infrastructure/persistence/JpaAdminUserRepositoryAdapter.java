package com.tellpal.v2.admin.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.admin.domain.AdminUser;
import com.tellpal.v2.admin.domain.AdminUserRepository;

@Repository
public class JpaAdminUserRepositoryAdapter implements AdminUserRepository {

    private final SpringDataAdminUserRepository repository;

    public JpaAdminUserRepositoryAdapter(SpringDataAdminUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public AdminUser save(AdminUser adminUser) {
        return repository.save(adminUser);
    }
}
