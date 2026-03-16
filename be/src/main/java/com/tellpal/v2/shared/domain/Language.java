package com.tellpal.v2.shared.domain;

public class Language {

    private final String code;
    private final String displayName;
    private final boolean isActive;

    public Language(String code, String displayName, boolean isActive) {
        this.code = code;
        this.displayName = displayName;
        this.isActive = isActive;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return isActive;
    }
}
