package com.tellpal.v2.user.application;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.user.api.UserResolutionApi;
import com.tellpal.v2.user.api.UserAuthenticationException;
import com.tellpal.v2.category.api.LibraryHomeCatalog;
import com.tellpal.v2.category.api.LibraryHomeQueryApi;
import com.tellpal.v2.category.api.LibraryStoryDetail;
import com.tellpal.v2.category.api.LibraryStoryDetailQueryApi;

/** Resolves identity before entering the catalog's read-only transaction; test audience is server-owned. */
@Service
@Profile({"local","test"})
public class LibraryHomeTestQueryService {
    private final UserResolutionApi users;
    private final LibraryHomeQueryApi library;
    private final LibraryStoryDetailQueryApi details;
    private final Set<String> allowed;
    private final Set<String> subscribers;
    public LibraryHomeTestQueryService(UserResolutionApi users,LibraryHomeQueryApi library,LibraryStoryDetailQueryApi details,
            @Value("${tellpal.library.test.allowed-firebase-uids:}") String allowed,
            @Value("${tellpal.library.test.subscriber-firebase-uids:}") String subscribers) {
        this.users=users;this.library=library;this.details=details;this.allowed=parse(allowed);this.subscribers=parse(subscribers);
        if(!this.allowed.containsAll(this.subscribers)) throw new IllegalArgumentException("Library subscriber fixtures must be allowlisted");
    }
    public LibraryHomeCatalog query(String authorization,String language) {
        String uid=admit(authorization);
        try {return library.query(language,subscribers.contains(uid)?LibraryHomeQueryApi.Audience.SUBSCRIBER:LibraryHomeQueryApi.Audience.NON_SUBSCRIBER);}
        catch(IllegalArgumentException e) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Library language is invalid or unavailable");}
    }
    /** Resolves controlled audience before querying exact reading detail. */
    public LibraryStoryDetail detail(String authorization,long categoryId,long contentId,String uiLanguage,
            String contentLanguage,String experience) {
        String uid=admit(authorization);
        try {
            return details.query(categoryId,contentId,uiLanguage,contentLanguage,experience,
                subscribers.contains(uid)?LibraryHomeQueryApi.Audience.SUBSCRIBER:LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Story is unavailable"));
        } catch(IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Reading detail request is invalid");
        }
    }
    private String admit(String authorization) {
        if(authorization==null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Firebase bearer token is required");
        String uid;
        try {uid=users.resolveOrCreateByIdToken(authorization.substring(7).trim()).firebaseUid();}
        catch(UserAuthenticationException e) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Firebase authentication is invalid");}
        if(!allowed.contains(uid)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"User is not enabled for Library testing");
        return uid;
    }
    private static Set<String> parse(String value) {return Arrays.stream(value.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toUnmodifiableSet());}
}
