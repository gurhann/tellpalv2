package com.tellpal.v2.admin.domain;

import java.util.List;
import java.util.Optional;

public interface AdminRoleRepository {

    Optional<AdminRole> findById(Long id);

    Optional<AdminRole> findByCode(String code);

    List<AdminRole> findAll();

    AdminRole save(AdminRole adminRole);
}
