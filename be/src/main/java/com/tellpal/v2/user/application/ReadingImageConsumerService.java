package com.tellpal.v2.user.application;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;

/** Library allowlist ve revoke kontrolü, ücretsiz medya erişiminden bağımsız doğrulanır. */
@Service("readingImageConsumerAccessService")
@Profile({"local","test"})
public class ReadingImageConsumerService {
    private final FirebaseTokenVerifier tokens;
    private final FirebaseAuthenticationProperties auth;
    private final ReadingImageConsumerApi consumer;
    private final Set<String> allowed;
    public ReadingImageConsumerService(FirebaseTokenVerifier tokens, FirebaseAuthenticationProperties auth,
            ReadingImageConsumerApi consumer, @Value("${tellpal.library.test.allowed-firebase-uids:}") String allowed) {
        this.tokens=tokens;this.auth=auth;this.consumer=consumer;
        this.allowed=Arrays.stream(allowed.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }
    /** Doğrulanmış bearer hesabını yeni admission ile birleştirir. */
    public Admission manifest(String bearer,long id,String language) {
        consumer.requireConfigured(id,language);
        String account=admit(bearer,null);
        return new Admission(account,consumer.manifest(id,language));
    }
    /** İstemci kapsamını aynı sunucu hesabı ve LIVE kimliğiyle yeniden doğrular. */
    public Admission status(String bearer,String account,ReadingImageComparisonApi.Identity identity) {
        consumer.requireConfigured(identity.contentId(),identity.language());admit(bearer,account);
        return new Admission(account,consumer.status(identity));
    }
    /** Storage sonrası revoke kontrolü ve son DB erişim kesiti geçmeden byte döndürmez. */
    public ReadingImageConsumerApi.Payload read(String bearer,String account,ReadingImageComparisonApi.Identity identity,ReadingImageDeliveryApi.Selection selection) {
        consumer.requireConfigured(identity.contentId(),identity.language());admit(bearer,account);
        var result=consumer.read(identity,selection);
        consumer.requireConfigured(identity.contentId(),identity.language());admit(bearer,account);
        consumer.status(identity);return result;
    }
    private String admit(String bearer,String expected) {
        if (auth.stubTokensEnabled() || !auth.checkRevoked()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Revoke kontrollü Firebase gerekli");
        if (bearer==null || !bearer.startsWith("Bearer ") || bearer.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Firebase bearer gerekli");
        String uid;
        try { uid=tokens.verify(bearer.substring(7).trim()).firebaseUid(); }
        catch (UserApplicationExceptions.FirebaseTokenVerificationException e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Firebase doğrulaması başarısız"); }
        if (!allowed.contains(uid) || (expected!=null && !expected.equals(uid)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Hesap kapsamı reddedildi");
        return uid;
    }
    public record Admission(String accountScope,ReadingImageComparisonApi.Admission delivery) { }
}
