package com.tellpal.v2.content.api;

import com.tellpal.v2.asset.api.LibraryCoverQueryApi.Covers;

/** Lightweight reading discovery metadata; cover references never include playback assets. */
public record ContentPreview(long contentId, String externalKey, String type, String experienceType,
        String contentLanguage, String title, Integer ageRange, Integer durationMinutes,
        boolean isFree, boolean isLocked, Covers covers) {}
