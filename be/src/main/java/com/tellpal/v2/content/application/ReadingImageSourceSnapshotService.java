package com.tellpal.v2.content.application;

import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.content.api.ReadingImageSourceSnapshotApi;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Captures scalar provenance; the caller must recompare bindings before a future P3 adoption. */
@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ,
        propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
public class ReadingImageSourceSnapshotService implements ReadingImageSourceSnapshotApi {
    private final ContentRepository contents;
    public ReadingImageSourceSnapshotService(ContentRepository contents) { this.contents = contents; }

    @Override
    public Snapshot capture(long contentId, String language) {
        var code = LanguageCode.from(language);
        if (!code.value().equals(language)) throw new IllegalArgumentException("Exact canonical language required");
        var optional = contents.findById(contentId);
        if(optional.isEmpty()) return new Snapshot(contentId,language,java.util.List.of(),java.util.List.of("CONTENT_NOT_FOUND"));
        return new ReadingImageSnapshotAssembler().assemble(optional.get(),language);
    }
    @Override
    public boolean matches(Snapshot captured) { return capture(captured.contentId(), captured.language()).equals(captured); }
}
