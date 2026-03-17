package com.tellpal.v2.user.infrastructure.firebase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import com.tellpal.v2.user.application.FirebaseTokenVerifier;
import com.tellpal.v2.user.application.UserApplicationExceptions.FirebaseTokenVerificationException;
import com.tellpal.v2.user.application.VerifiedFirebaseToken;

final class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private static final String APP_NAME = "tellpal-v2-firebase-auth";

    private final FirebaseAuthenticationProperties properties;

    private volatile FirebaseAuth firebaseAuth;

    FirebaseAdminTokenVerifier(FirebaseAuthenticationProperties properties) {
        this.properties = properties;
    }

    @Override
    public VerifiedFirebaseToken verify(String idToken) {
        String requiredToken = requireText(idToken, "Firebase ID token must not be blank");
        try {
            FirebaseToken token = authClient().verifyIdToken(requiredToken, properties.checkRevoked());
            return new VerifiedFirebaseToken(token.getUid());
        } catch (FirebaseAuthException exception) {
            throw new FirebaseTokenVerificationException("Firebase ID token could not be verified", exception);
        }
    }

    private FirebaseAuth authClient() {
        FirebaseAuth currentClient = firebaseAuth;
        if (currentClient != null) {
            return currentClient;
        }
        synchronized (this) {
            if (firebaseAuth == null) {
                firebaseAuth = FirebaseAuth.getInstance(firebaseApp());
            }
            return firebaseAuth;
        }
    }

    private FirebaseApp firebaseApp() {
        return FirebaseApp.getApps().stream()
                .filter(candidate -> APP_NAME.equals(candidate.getName()))
                .findFirst()
                .orElseGet(this::initializeFirebaseApp);
    }

    private FirebaseApp initializeFirebaseApp() {
        try {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(loadCredentials());
            if (properties.projectId() != null && !properties.projectId().isBlank()) {
                optionsBuilder.setProjectId(properties.projectId().trim());
            }
            return FirebaseApp.initializeApp(optionsBuilder.build(), APP_NAME);
        } catch (IOException exception) {
            throw new FirebaseTokenVerificationException("Firebase Admin SDK credentials could not be loaded", exception);
        }
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (properties.credentialsPath() == null || properties.credentialsPath().isBlank()) {
            return GoogleCredentials.getApplicationDefault();
        }
        Path credentialsPath = Path.of(properties.credentialsPath().trim());
        try (InputStream inputStream = Files.newInputStream(credentialsPath)) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new FirebaseTokenVerificationException(message);
        }
        return value.trim();
    }
}
