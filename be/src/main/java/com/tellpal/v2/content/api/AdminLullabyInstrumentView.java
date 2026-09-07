package com.tellpal.v2.content.api;

/** Ordered content-level instrument selection used by admin projections. */
public record AdminLullabyInstrumentView(
        Long instrumentId,
        String code,
        String displayName,
        int displayOrder) {

    public AdminLullabyInstrumentView {
        if (instrumentId == null || instrumentId <= 0) {
            throw new IllegalArgumentException("Instrument catalog ID must be positive");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Instrument catalog code must not be blank");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Lullaby instrument display order must not be negative");
        }
        code = code.trim();
        if (displayName != null) {
            displayName = displayName.trim();
        }
    }
}
