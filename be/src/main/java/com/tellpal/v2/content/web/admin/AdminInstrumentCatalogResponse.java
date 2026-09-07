package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContentManagementResults.InstrumentCatalogRecord;

/** Locale-resolved catalog option returned to CMS instrument selectors. */
public record AdminInstrumentCatalogResponse(
        Long instrumentId,
        String code,
        String displayName) {

    static AdminInstrumentCatalogResponse from(InstrumentCatalogRecord record) {
        return new AdminInstrumentCatalogResponse(record.instrumentId(), record.code(), record.displayName());
    }
}
