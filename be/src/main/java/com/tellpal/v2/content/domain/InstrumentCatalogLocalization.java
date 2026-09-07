package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/** Locale-specific display label for one stable instrument catalog entry. */
@Entity
@Table(name = "instrument_catalog_localizations")
public class InstrumentCatalogLocalization extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_catalog_id", nullable = false)
    private InstrumentCatalog instrumentCatalog;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    protected InstrumentCatalogLocalization() {
    }

    InstrumentCatalogLocalization(
            InstrumentCatalog instrumentCatalog, LanguageCode languageCode, String displayName) {
        this.instrumentCatalog = requireCatalog(instrumentCatalog);
        this.languageCode = requireLanguageCode(languageCode);
        updateDisplayName(displayName);
    }

    public InstrumentCatalog getInstrumentCatalog() {
        return instrumentCatalog;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    void updateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Instrument catalog display name must not be blank");
        }
        this.displayName = displayName.trim();
    }

    private static InstrumentCatalog requireCatalog(InstrumentCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Instrument catalog must not be null");
        }
        return catalog;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Instrument catalog language code must not be null");
        }
        return languageCode;
    }
}
