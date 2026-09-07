package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/** Content-owned ordered link to one stable instrument catalog entry. */
@Entity
@Table(name = "lullaby_instruments")
public class LullabyInstrument extends BaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_catalog_id", nullable = false)
    private InstrumentCatalog instrumentCatalog;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected LullabyInstrument() {
    }

    LullabyInstrument(Content content, InstrumentCatalog instrumentCatalog, int displayOrder) {
        this.content = requireContent(content);
        this.instrumentCatalog = requireCatalog(instrumentCatalog);
        this.displayOrder = requireNonNegative(displayOrder);
    }

    public InstrumentCatalog getInstrumentCatalog() {
        return instrumentCatalog;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = requireNonNegative(displayOrder);
    }

    private static Content requireContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        return content;
    }

    private static InstrumentCatalog requireCatalog(InstrumentCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Instrument catalog must not be null");
        }
        return catalog;
    }

    private static int requireNonNegative(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Lullaby instrument display order must not be negative");
        }
        return displayOrder;
    }
}
