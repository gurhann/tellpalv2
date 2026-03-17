package com.tellpal.v2.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.user.api.AppUserReference;
import com.tellpal.v2.user.api.AppUserProfileReference;
import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserAuthenticationException;
import com.tellpal.v2.user.api.UserLookupApi;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.user.application.UserApplicationExceptions.FirebaseTokenVerificationException;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.AppUserRepository;

@Service
public class UserResolutionService implements UserResolutionApi, UserLookupApi {

    private final AppUserRepository appUserRepository;
    private final FirebaseTokenVerifier firebaseTokenVerifier;

    public UserResolutionService(
            AppUserRepository appUserRepository,
            FirebaseTokenVerifier firebaseTokenVerifier) {
        this.appUserRepository = appUserRepository;
        this.firebaseTokenVerifier = firebaseTokenVerifier;
    }

    @Override
    @Transactional
    public AuthenticatedAppUser resolveOrCreateByIdToken(String idToken) {
        VerifiedFirebaseToken verifiedToken = verifyToken(idToken);
        AppUser appUser = appUserRepository.findByFirebaseUid(verifiedToken.firebaseUid())
                .orElseGet(() -> AppUser.create(verifiedToken.firebaseUid(), false));

        boolean requiresSave = appUser.getId() == null;
        if (appUser.primaryProfile().isEmpty()) {
            appUser.ensurePrimaryProfile();
            requiresSave = true;
        }

        AppUser persistedUser = requiresSave ? appUserRepository.save(appUser) : appUser;
        return UserApiMapper.toAuthenticatedAppUser(persistedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AppUserReference> findByFirebaseUid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
        return appUserRepository.findByFirebaseUid(firebaseUid.trim())
                .map(UserApiMapper::toAppUserReference);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AppUserProfileReference> findPrimaryProfileByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        return appUserRepository.findById(userId)
                .map(UserApiMapper::toPrimaryProfileReference);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<AppUserProfileReference> findPrimaryProfileByFirebaseUid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new IllegalArgumentException("Firebase UID must not be blank");
        }
        return appUserRepository.findByFirebaseUid(firebaseUid.trim())
                .map(UserApiMapper::toPrimaryProfileReference);
    }

    private VerifiedFirebaseToken verifyToken(String idToken) {
        try {
            return firebaseTokenVerifier.verify(idToken);
        } catch (FirebaseTokenVerificationException exception) {
            throw new UserAuthenticationException(exception.getMessage(), exception);
        }
    }
}
