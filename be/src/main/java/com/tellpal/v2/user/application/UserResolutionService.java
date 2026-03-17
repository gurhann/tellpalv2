package com.tellpal.v2.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.user.api.AuthenticatedAppUser;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.AppUserRepository;

@Service
public class UserResolutionService implements UserResolutionApi {

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
        VerifiedFirebaseToken verifiedToken = firebaseTokenVerifier.verify(idToken);
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
}
