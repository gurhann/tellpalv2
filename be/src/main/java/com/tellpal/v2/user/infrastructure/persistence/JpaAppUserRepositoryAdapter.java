package com.tellpal.v2.user.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.AppUserRepository;

@Repository
public class JpaAppUserRepositoryAdapter implements AppUserRepository {

    private final SpringDataAppUserRepository repository;

    public JpaAppUserRepositoryAdapter(SpringDataAppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<AppUser> findByFirebaseUid(String firebaseUid) {
        return repository.findByFirebaseUid(firebaseUid);
    }

    @Override
    public boolean existsByFirebaseUid(String firebaseUid) {
        return repository.existsByFirebaseUid(firebaseUid);
    }

    @Override
    public AppUser save(AppUser appUser) {
        return repository.save(appUser);
    }
}
