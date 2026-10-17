package com.tellpal.v2.user.application;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties;
import com.tellpal.v2.content.api.ReadingImageComparisonApi;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;

/** Her istek gerçek revoke kontrollü bearer ve tek kanonik QA kimliğini doğrular. */
@Service
@Profile({"local", "test"})
public class ReadingImageComparisonTestService {
    private static final String QA_UID="jLRypTmoBrhhX21HsDJojurcKTC3";
    private final FirebaseTokenVerifier tokens;
    private final FirebaseAuthenticationProperties auth;
    private final ReadingImageComparisonApi comparison;
    public ReadingImageComparisonTestService(FirebaseTokenVerifier tokens, FirebaseAuthenticationProperties auth, ReadingImageComparisonApi comparison) {
        this.tokens=tokens; this.auth=auth; this.comparison=comparison;
    }
    public ReadingImageComparisonApi.Admission manifest(String bearer, long id, String language) {
        comparison.requireConfigured(id,language); admit(bearer); return comparison.manifest(id, language);
    }
    public ReadingImageComparisonApi.Admission status(String bearer, ReadingImageComparisonApi.Identity identity) {
        comparison.requireConfigured(identity.contentId(),identity.language()); admit(bearer); return comparison.status(identity);
    }
    public byte[] read(String bearer, ReadingImageComparisonApi.Identity identity, ReadingImageDeliveryApi.Selection selection) {
        comparison.requireConfigured(identity.contentId(),identity.language()); admit(bearer);
        var bytes=comparison.read(identity, selection);
        comparison.requireConfigured(identity.contentId(),identity.language()); admit(bearer);
        comparison.status(identity); return bytes;
    }
    private void admit(String bearer) {
        if (auth.stubTokensEnabled() || !auth.checkRevoked()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gerçek revoke kontrollü Firebase doğrulaması gerekli");
        if (bearer==null || !bearer.startsWith("Bearer ") || bearer.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase bearer gerekli");
        String uid;
        try { uid=tokens.verify(bearer.substring(7).trim()).firebaseUid(); }
        catch (UserApplicationExceptions.FirebaseTokenVerificationException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firebase doğrulaması başarısız"); }
        if (!QA_UID.equals(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QA kullanıcısı gerekli");
    }
}
