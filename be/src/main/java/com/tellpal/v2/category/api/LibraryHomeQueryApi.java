package com.tellpal.v2.category.api;

/** Server-owned Library selection; audience must originate from verified user context. */
public interface LibraryHomeQueryApi {
    /** Returns eligible rows in saved editorial order, using exact explicit content languages. */
    LibraryHomeCatalog query(String language, Audience audience);
    enum Audience { NON_SUBSCRIBER, SUBSCRIBER }
}
