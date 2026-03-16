package com.tellpal.v2.user.infrastructure.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {

    private final FirebaseApp firebaseApp;

    public FirebaseAuthService(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    /**
     * Verifies the given Firebase ID token and returns the Firebase UID.
     *
     * @param idToken the Firebase ID token from the client
     * @return the Firebase UID extracted from the token
     * @throws FirebaseAuthException if the token is invalid or Firebase is not initialized
     */
    public String verifyIdToken(String idToken) {
        if (firebaseApp == null) {
            throw new FirebaseAuthException("Firebase is not initialized. Cannot verify ID token.");
        }
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken);
            return decodedToken.getUid();
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            throw new FirebaseAuthException("Invalid Firebase ID token: " + e.getMessage(), e);
        }
    }
}
