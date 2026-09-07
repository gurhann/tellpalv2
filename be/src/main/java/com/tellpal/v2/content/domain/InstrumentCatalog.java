package com.tellpal.v2.content.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Stable, language-independent instrument catalog entry.
 *
 * <p>The code is the value accepted by content-management commands. Localized labels are kept in
 * {@link InstrumentCatalogLocalization} rows so the same selection can be rendered in every
 * supported language.
 */
@Entity
@Table(name = "instrument_catalogs")
public class InstrumentCatalog extends BaseJpaEntity {

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "instrumentCatalog", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstrumentCatalogLocalization> localizations = new LinkedHashSet<>();

    protected InstrumentCatalog() {
    }

    private InstrumentCatalog(String code, boolean active) {
        this.code = requireCode(code);
        this.active = active;
    }

    /** Creates an active catalog entry. */
    public static InstrumentCatalog create(String code) {
        return new InstrumentCatalog(code, true);
    }

    /** Creates an entry with an explicit lifecycle state for reference-data maintenance. */
    public static InstrumentCatalog create(String code, boolean active) {
        return new InstrumentCatalog(code, active);
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }

    public Set<InstrumentCatalogLocalization> getLocalizations() {
        return Collections.unmodifiableSet(localizations);
    }

    public Optional<InstrumentCatalogLocalization> findLocalization(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Instrument catalog language code must not be null");
        }
        return localizations.stream()
                .filter(localization -> localization.getLanguageCode() == languageCode)
                .findFirst();
    }

    /** Adds or replaces one locale label without changing the stable catalog code. */
    public InstrumentCatalogLocalization upsertLocalization(LanguageCode languageCode, String displayName) {
        InstrumentCatalogLocalization localization = findLocalization(languageCode)
                .orElseGet(() -> {
                    InstrumentCatalogLocalization created = new InstrumentCatalogLocalization(
                            this, languageCode, displayName);
                    localizations.add(created);
                    return created;
                });
        localization.updateDisplayName(displayName);
        return localization;
    }

    /** Marks this reference-data row unavailable for future selections. */
    public void markActive(boolean active) {
        this.active = active;
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Instrument catalog code must not be blank");
        }
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
