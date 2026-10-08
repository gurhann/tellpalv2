# Publish-time image optimization follow-up

Status: recorded for planning after mobile Story 1.2 is complete.
Requested by the human on 2026-10-07; implementation is outside Story 1.2.

Published content must use optimized cover and content images rather than serving raw uploaded image files to consumers. Plan optimization as part of content publication. Evaluate resolution reduction and image compression, with separate quality and size targets appropriate to covers and content pages, including phone and tablet use.

The planning work must establish representative before/after file sizes, rendered quality and legibility, target resolutions, compression/format choices, and publication behavior when optimization fails. Inspect the existing asset/publication contracts before choosing processing or storage architecture. Determine how existing published images will be handled and how optimized outputs receive correct dimensions, MIME types, byte sizes and checksums. Keep revision consistency and ordinary versus candidate access intact.

No numeric resolution, quality target, implementation architecture or delivery date is selected by this note. No existing media is changed. The approved iOS native-test capture envelope increase from 8 to 16 MiB only enables unmodified SDK screenshot evidence; it does not implement or satisfy content-image optimization.

Track this follow-up from [mobile epics](epics-mobile.md) and [Story 1.2](../implementation-artifacts/mobile/spec-1-2-open-the-selected-story-detail-from-library.md). After Story 1.2, turn it into a separately reviewed implementation plan with measurable quality, size and publication acceptance criteria.
