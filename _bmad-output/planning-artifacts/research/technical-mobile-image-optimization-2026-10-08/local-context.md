# Local context and evidence boundary

Inspected 2026-10-08. These are repository observations, not independent evidence for general technical claims. Detailed read-only inventories: [mobile](imports/mobile-inventory.md), [backend](imports/backend-inventory.md). Existing JPEG measurements are imported from [P0 findings](../../../specs/spec-publish-time-image-optimization/p0-small-jpeg-findings.md); no experiment was rerun.

## Decision anchors
- Story 1.2 detail delivers verified original JPEG/PNG bytes; client decode limits cannot reduce that network payload.
- Listing produces real verified JPEG derivatives, phone bounding box360×480/tablet600×800,q85. These boxes are not guaranteed output dimensions for nonmatching aspect ratios.
- Page experiment creates real immutable JPEGs; legacy WebP entries are plans, not a real WebP encoder.
- New Library entry decoded cache:48 entries/32MiB; Hero decoded width1080/1600,height2048 plus pixel ceiling. Encoded Hero disk20files/100MiB and20MiB transfer/four owned attempts are separate limits.
- Preview/hero slots use BoxFit.cover; current decode selection does not use actual slot geometry or DPR. Maintain inherited crop/appearance pending explicit quality/framing evaluation.
- Existing owned transfer cancellation, checksum validation, immutable disk identities, revision guards and access boundaries should survive optimization.
- Older reader PoC uses unbounded Image.network/precache; it is a separate path from accepted Story1.2 and not an accepted new production reader.
- Server converters fully decode before resizing; current polling batch is sequential, not proof of ten simultaneous decodes.
- Prior six-small-JPEG controls total269651bytes; source-resolution q85 totals383718bytes(+42.30%). The corpus has no representative high-resolution masters or reader pages and no native quality acceptance.

## Existing constraints
Read AGENTS.md, project memory, ADR0017/0018/0019, architecture.md and mobile spine before contextual recommendations. Preserve localizations/role ownership, module APIs/events/IDs, source/profile/attempt fencing, immutable readiness, account/access-scoped cache, no original fallback and agreed rollout(new publications first; historical backfill separate). API24/iOS15 floor is local build/spine evidence, not a claim about AVIF compatibility.

No code,production media,existing draft,cache or policy changed. No actual blur,OOM,leak,peak-memory improvement or production performance benefit has been measured by this source inspection.
