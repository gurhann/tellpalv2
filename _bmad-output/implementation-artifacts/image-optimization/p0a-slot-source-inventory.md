# P0a.1 — Current Image Slot and Source Inventory

Story 1.1 delivers a bounded, read-only inventory. It does **not** close full P0,
representative-corpus, visual-quality, normalized-orientation, physical-device,
resource-budget or production-profile gates. All viewport/DPR calculations below
are hypothetical examples derived from retained code, not device measurements.

The machine-readable [inventory](p0a-slot-source-inventory.json) contains full
Git HEAD, dirty working-tree provenance, before/after SHA-256 and byte counts
for every retained input, code anchors, allowlisted evidence projections, explicit
joins, source/selection geometry and a complete role/scenario matrix. The
[verifier](verify-p0a-inventory.py) independently reproduces the arithmetic and
validates retained inputs, counts, joins, matrix completeness, local links and
privacy. Each observation and auxiliary projection requires a verifier-owned,
complete and exact destination-to-source mapping, expected input identity and
pointer shape. ID, MIME, metadata, output-byte and case projections have separate
narrow schemas; an inventory cannot omit a field to bypass its verification.
The default run also passes 31 malformed temporary inventories through the same
`verify()` path and requires their rejection, including changed bytes or asset
IDs with omitted mappings, incorrect source mappings and extra auxiliary fields.
It uses only the Python standard library, local repository reads and isolated
temporary copies for negative checks:

```sh
python3 _bmad-output/implementation-artifacts/image-optimization/verify-p0a-inventory.py
```

The [epic context](epic-1-context.md) and
[client contract](../../specs/spec-publish-time-image-optimization/client-image-contract.md)
were loaded before execution. Source code, Mobile1.2 artifacts, historical
experiments, shared BMad configuration and production settings remain inputs.
No media acquisition, authentication, encoder execution or runtime experiment
is part of this inventory.

## Evidence boundaries and counting

| Retained collection | Bindings / observations | Unique image hashes | What it establishes |
| --- | ---: | ---: | --- |
| Original artwork source bindings | 21 | 19 | Recorded byte identity and encoded dimensions; three language bindings are distinct from unique bytes |
| Listing rendition observations | 52 | 38 | Recorded phone/tablet JPEG output identity and actual dimensions, including repeated observations |
| Current HTTP matrix case heroes | 29 | 8 | Historical corrected-fixture hero asset IDs, MIME, bytes and dimensions; `/matrices/*/cases/*/hero` only |
| Current HTTP `actualSources` | 6 | 3 | An incomplete repeated subset; cannot stand in for all heroes |
| Historical small-JPEG controls | 6 | 6 | Small cover controls; no representative masters or reader pages |
| Separate Display P3 diagnostic | 1 | 1 | Additional color probe; retained separately from the six controls |

Historical inputs are the [artwork evidence](../mobile/library-1-2-existing-artwork-evidence.json),
[current HTTP evidence](../mobile/library-1-2-http-evidence.json),
[small-JPEG corpus](../../specs/spec-publish-time-image-optimization/experiments/corpus.json),
[encoded results](../../specs/spec-publish-time-image-optimization/experiments/small-jpeg-results.json)
and [color diagnostic](../../specs/spec-publish-time-image-optimization/experiments/color-diagnostic/color-comparison.json).

These collections overlap. Their unique counts must not be summed as a corpus
size. Historical pre-correction matrices and initial probes are retained in their
original files but excluded from the current hero collection.

Exactly 22 of 52 listing observations join to a retained small-JPEG experiment
output using **SHA-256, byte size and both dimensions**. That output's recorded
sample identity resolves the original hash and dimensions; language is also
required before joining an artwork binding. The other 30 observations have
unknown original relations. No external-key/story-number/filename inference is
used. Listing artwork has no recorded asset IDs or MIME fields: those remain
null. Joined actual MIME is available only through the experiment's recorded
byte-inspection pointer. A JPEG filename, current encoder policy or planned
WebP path is insufficient evidence of actual MIME or asset identity.

Current hero descriptors join source bindings by hash, bytes, both dimensions
and content language. Source asset-ID/MIME observations retain the exact current
case pointers; identical bytes across languages do not establish one asset ID.
Metadata joins to the small-JPEG controls use the same byte/dimension tuple.
Unknown alpha, ICC and orientation for other sources are not recorded as false.
Unobserved orientation and normalized dimensions remain null throughout; even
a retained absent orientation tag is not a native orientation measurement.

## Roles and inherited framing

| Role | Status | Slot and source behavior |
| --- | --- | --- |
| Library STORY listing | Accepted code | `H = 0.230 × MediaQuery.height + 16`, `W = 0.75 × H`; shared PreviewCard |
| STORY recommendations | Accepted code | Same card formula and PreviewCard; exact recommendation image bindings are absent from current HTTP evidence |
| STORY detail hero | Accepted code | Parent full width; aspect ratio `0.9` phone / `1.1` tablet; localized original JPEG/PNG |
| Legacy reader | PoC only | `Expanded` remainder of a SafeArea Column after variable header/player controls; `contain`, centered |
| Future reader | Unimplemented | No accepted slot/source selection or bounded decoder to inventory |
| STORY listening artwork | Unimplemented | Content-scoped listening source, separate from localized reading/source covers |
| MEDITATION listening artwork | Unimplemented | Shared content-scoped source; no accepted listening slot |
| LULLABY static listing | Unimplemented | Independently owned static listing source; STORY controls do not qualify it |
| LULLABY playback/detail animation | Unimplemented | Separate listening artwork, potentially animated; no frame/residency evidence |

Accepted cards and hero use `cover` with implicit centered alignment. Card
layout has 16 logical pixels of horizontal list padding and spacing, and clips
the image stack to a 16-pixel corner radius. The entire card rectangle is the
image slot. The locked-card badge covers a 32×32 region at left/top 12; its error
retry control occupies 48×48 at bottom-left when applicable. These overlays do
not reduce geometric demand, and no embedded-text safe rectangle has been
measured.

The loaded detail hero sits in a full-width ListView with bottom-only padding;
its parent-width assumption is explicit in each scenario. It has clipped bottom
corners, a 48-pixel back control at left 16/top inset + 16, a locked-state control
at right 16/top inset + 16, and duration/age pills along the bottom (left/right
16, bottom 8). Locked, loaded artwork receives sigma-5 blur. These are inherited
presentation details, not quality approval; source title lettering may be cropped
or occluded. Pill/text-safe bounds and enlarged-text effects are unavailable.

The breakpoint is `MediaQuery.size.shortestSide >= 600`, including after
rotation or window resizing. A tablet layout chooses tablet cover when present,
then phone fallback; phone layouts choose phone. Library SafeArea changes child
constraints/padding but does not subtract its padding from `MediaQuery.size`.
The inventory therefore does not subtract top/bottom insets from the height
formula. Loaded detail does not wrap the hero in SafeArea. Hypothetical insets
are recorded separately and have no claimed native provenance.

## Two-axis demand and decode limits

For intrinsic axes `Iw, Ih`, logical slot `Lw, Lh`, and DPR `d`:

```text
Pw = ceil(Lw*d); Ph = ceil(Lh*d)
scale = max(Pw/Iw, Ph/Ih) for cover; min(...) for contain
required = ceil(Iw*scale), ceil(Ih*scale)          # never source-clamped
bounded = min(Iw, ceil(Iw*min(1,scale))),
          min(Ih, ceil(Ih*min(1,scale)))          # decoder target only
```

The verifier uses exact rational arithmetic, with independent Decimal fixtures
for rounding and the current hero floor/square-root cap. It covers tall/wide,
contain/cover, fractional DPR, phone/tablet, rotation/window, decoder ceilings
and 100×100 → 200×200 shortage with fixed expected values. Ceil-rounded axes
remain visible in the saved result; integer rounding cannot create detail.

Example: a 390×844 logical phone at DPR 3 produces a 157.59×210.12 card and
473×631 physical slot. A recorded 512×909 original needs 473×840 full-image
pixels under cover. A 270×480 phone rendition needs 473×841 for its own slightly
rounded aspect ratio. The original has enough encoded pixels for this example;
the rendition does not. A source-capped 270×480 decoder does not clear that
shortage. On the same phone, the detail slot is 390×433⅓ and 1170×1300 physical;
the same original needs 1170×2078 and is itself insufficient.

Each geometry group records all exact observation IDs, intrinsic dimensions and
any verified original relation. Each accepted role/scenario cell separates:

- Unclamped original demand when joined, or explicitly conditional selected-image
  demand when the original relation is unavailable.
- Original shortage (null when unknown) and rendition shortage against both
  unclamped required axes, including small aspect-ratio differences from rounding.
- Selected-image demand, contract source-capped decode, requested/current modeled
  decode geometry, and a separate modeled decode shortage.
- Actual decoded dimensions, actual decode shortage, resource shortage and quality
  qualification: all unavailable. Encoded-axis calculations do not qualify
  orientation-normalized pixels or visual sharpness.

The five scenarios are phone portrait (390×844 @3), tablet portrait
(1024×1366 @2), rotated phone (844×390 @3), a 600×900 @1.5 window and a
599×900 @1.25 window. The matrix evaluates the recorded matching-layout profiles
plus phone fallback on tablet. It does not claim that each recorded image was
selected on those devices or that a fallback was observed. Each of the six
PoC/unimplemented roles has an explicit unavailable cell for every scenario.

Current listing defaults are q85 JPEG **bounding boxes** of 360×480 / 600×800,
not guaranteed output sizes. Retained outputs include 270×480, 296×480,
451×800, 493×800, 428×695 and 321×570. PreviewCard requests width 360 / 600
and no height. The model assumes no upscale, so these already-small outputs
remain at their intrinsic dimensions; resolved native decoder dimensions are
unmeasured. Profile defaults do not establish production environment values.

Current hero requests floor-rounded source-ratio targets from
`min(1, 1080/Iw or 1600/Iw, 2048/Ih, sqrt(4194304/(Iw*Ih)))`.
It is source-capped and unrelated to slot/DPR demand. Its 4,194,304-pixel ceiling
is at most one assumed 16 MiB RGBA plane; this is not a process/native memory
bound. Fixed verifier cases also exercise larger hypothetical sources; no new
source bytes were acquired or encoded.

The page experiment processor defaults to longest edge 1440, q85, no upscale,
40 million input pixels, JPEG output and white background compositing. That is
code evidence only for this inventory: no real page corpus/output binding is
introduced. Legacy `DefaultImageOptimizationAdapter` returns WebP
`GeneratedAssetPlan` records. Plans are not encoded, decoded or independently
verified WebP bytes and never count as observed outputs.

## Cache and residency evidence limits

The isolated Library entry configures decoded LRU controls of 48 entries / 32 MiB.
The legacy main entry separately sets 24 MiB. These entry-specific controls must
not be conflated or applied as a measured global footprint.

Library encoded caching uses account/access ownership, 80 objects and ten-minute
staleness; its default owner ceiling is 90. Hero uses a distinct app-owned profile
with 20 files, 100 MiB encoded disk capacity, four owned attempts, 20 MiB per
transfer and ten-minute staleness. Immutable source/checksum, context and transfer
attempt identities, cleanup/cancellation and decoder-key ownership are visible
in code. They are not observations of concurrent decodes, total process memory,
peak allocation or retained listener cost. Transfer limits and disk bytes are
separate from decoded memory. This inventory changes no limits or ownership.

The reader PoC precaches the current and next `NetworkImage` without a bounded
decode target and displays `Image.network(... fit: contain)`. Its variable
Expanded slot is unavailable. Precache completion does not establish first
onstage target paint, retention, decoder peak or physical-device safety. Code
inspection does not certify success or failure of its runtime behavior.

## Open evidence and next actions

| Missing evidence | Next authorized work needed |
| --- | --- |
| High-resolution masters | Story 1.2: freeze rights-cleared, retained representative originals; do not enlarge small controls |
| Real reader text pages | Freeze title/body/diacritic pages with fine lines, gradients and dark artwork; record actual source roles |
| Alpha and compositing | Freeze transparent sources/edges and agree background policy; current white JPEG compositing is not approval |
| EXIF rotation/mirroring | Freeze rotated/mirrored sources, define normalization and verify displayed axes; orientation remains unavailable here |
| ICC diversity | Add broader unmanaged/profiled sources; retain the separate Display P3 probe and agree normalization/human assessment |
| Animation | Qualify independently owned LULLABY listening artwork, all-frame behavior and runtime residency; still images cannot certify it |
| Embedded-text safe regions | Annotate and inspect crop/overlay/enlarged-text behavior; safe regions/native observations remain unavailable |
| Exact listing/recommendation identities | Retain sanctioned immutable manifests/byte joins; no ID/MIME inference from profile or filenames |
| Reader/listening geometry | Separately implement/authorize missing surfaces and measure actual constraints; no guessed Expanded height |
| Native decode/latency/resource measurements | Stories 1.5/1.6 need a frozen protocol, verified separately authorized entry and physical devices; preserve unavailable counters/floor hardware |

The six historical small controls and separate P3 diagnostic are preserved with
exact hash/byte/dimension/MIME pointers. Their historical experiment has 63 real
encoded result records. Retained aggregate evidence includes output growth:
listing-tablet q85 grew relative to five of seven small source files; the
source-resolution edge960 q85 aggregate grew 4.6688%, with six of seven sources
larger. These seven-sample statistics explicitly include the separately labeled
P3 diagnostic and do not certify a six-control-only result. Re-encoding an
already-small JPEG can increase bytes, so growth remains a later decision,
not an optimization success assumption.

This inventory closes only the bounded recording/verification task with explicit
gaps. Source acquisition, native experiments, new codecs, profiles/budgets,
admission changes and application/policy implementation require the subsequent
scopes described by the dedicated epic.
