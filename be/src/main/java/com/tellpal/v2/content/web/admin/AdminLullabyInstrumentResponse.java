package com.tellpal.v2.content.web.admin;

import com.tellpal.v2.content.application.ContentManagementResults.LullabyInstrumentRecord;

/** Ordered selected instrument returned by the lullaby admin editor. */
public record AdminLullabyInstrumentResponse(
        Long instrumentId,
        String code,
        String displayName,
        int displayOrder) {

    static AdminLullabyInstrumentResponse from(LullabyInstrumentRecord record) {
        return new AdminLullabyInstrumentResponse(
                record.instrumentId(), record.code(), record.displayName(), record.displayOrder());
    }
}
