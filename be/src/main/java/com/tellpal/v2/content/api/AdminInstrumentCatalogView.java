package com.tellpal.v2.content.api;

/** Locale-resolved active instrument catalog option for admin editors. */
public record AdminInstrumentCatalogView(
        Long instrumentId,
        String code,
        String displayName) {

    public AdminInstrumentCatalogView {
        if (instrumentId == null || instrumentId <= 0) {
            throw new IllegalArgumentException("Instrument catalog ID must be positive");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Instrument catalog code must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Instrument catalog display name must not be blank");
        }
        code = code.trim();
        displayName = displayName.trim();
    }
}
