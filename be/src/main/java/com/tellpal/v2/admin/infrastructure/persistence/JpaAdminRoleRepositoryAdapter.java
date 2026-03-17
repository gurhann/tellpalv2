package com.tellpal.v2.admin.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.admin.domain.AdminRole;
import com.tellpal.v2.admin.domain.AdminRoleRepository;

@Repository
public class JpaAdminRoleRepositoryAdapter implements AdminRoleRepository {

    private final SpringDataAdminRoleRepository repository;

    public JpaAdminRoleRepositoryAdapter(SpringDataAdminRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AdminRole> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<AdminRole> findByCode(String code) {
        return repository.findByCode(code);
    }

    @Override
    public List<AdminRole> findAll() {
        return repository.findAll();
    }

    @Override
    public AdminRole save(AdminRole adminRole) {
        return repository.save(adminRole);
    }
}
