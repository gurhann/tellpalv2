# P0a Reader Geometry Decision

Status: **APPROVED**, 2026-10-10.
Gürhan approved the recommended r3 preparation scope with "Uygun". Approval
permits its implementation and preparation freeze; candidate encoding, reader
qualification and Story completion require their separate actual evidence.

## Finding

The accepted mobile router exposes Library and Story Detail only
([router.dart](../../../app/mobile/lib/app/router.dart)). The older
`StoryPlayerPage` is a separate media PoC. Its image uses centered `contain`
inside the remaining `Expanded` space of a `SafeArea` column, after a variable
header, loading/error state, audio controls and page navigation
([story_player_page.dart](../../../app/mobile/lib/story_player_page.dart)).
No reader slot/DPR evidence linked to the selected bindings is available in the
retained evidence.
The Story 1.1 inventory explicitly records the PoC slot as unavailable and its
viewport/DPR examples as hypothetical
([inventory](p0a-slot-source-inventory.md)). A widget-layout diagnostic could
measure the PoC for specified synthetic states; it would not establish the
future accepted reader's layout or physical-device quality.

The r2 pilot currently requires affirmative reader geometry before reference
admission. Its two source-sized reference streams cannot supply that evidence.
Repeated source inspection or another normalization pass cannot resolve this
missing surface. Implementing a reader or running native trials is outside the
current [spec](spec-1-2-corpus-trial-protocol.md).

## Approved decision

Implement the approved r3 preparation scope whose initial handoff is **offline codec
feasibility at unchanged source resolution**. Keep CMS ownership as
`reading_page`; separately label the experimental comparison purpose
`source_resolution_codec_feasibility`. Source ownership is not a display role
qualification.

Use exactly the existing two bindings and independently checked v7 references:

| Binding | Experimental pixel geometry | Canonical RGBA8 pixel SHA-256 |
| --- | --- | --- |
| `binding-content71-page1-tr-reading` | 1712 × 2778 | `1fbbd6f4f5cfcb56dd62cd60a779ff8cd772921f7e7a5eaa7514e3d6f1ad26c6` |
| `binding-content19-page1-tr-reading` | 1284 × 2279 | `03802a378dc106e6af5505e94b72608b3f09e20ac6cadf4a606c4b8546f8fef2` |

The first handoff uses a single same-geometry arm for each binding, with no
resize, crop, upscale, inferred logical slot or inferred DPR. Keep original-byte
and normalized same-geometry JPEG q85 denominators separate. The finite
comparison ledger must pin candidate settings, reference/source identities,
ordering, retry limits and all planned states before any affected comparison.
Actual provider/binary qualification and candidate outputs remain Story 1.3.
Any new dependency/runtime installation retains its separate concrete scope
requirement.

Admit references only for this explicitly approved experimental purpose after
source rehash, metadata/predicate checks and independent correctness evidence
pass. A full-source reference cannot become an admitted reader-demand
reference merely by changing its label. Keep the normalization recipe, runtime,
canonical serialization and reference pixels unchanged.

Record reader logical slot, DPR, fit, unclamped demand, source adequacy and
native viewing quality as **unavailable**, linked to an open reader
qualification gate. A later reader/sizing/native batch requires measured or
otherwise accepted applicable geometry, compatible checked references, its
own revision and authorization. Preserve the native observation contract for
that later batch; the offline handoff has no native environments or observations.
Do not pool this batch with later sizing or reader-quality results. Visual
reader verdicts and quality-qualified savings remain pending.

## Exact approval boundary

Approval permits preparation of an r3 spec/context/corpus/protocol and verifier
changes separating experimental geometry from reader qualification. Preserve
the r2 snapshots and verification history, bind the new snapshots through a
detached immutable freeze, and complete a finite preregistered ledger before
any Story 1.3 handoff. The current geometry bypass rejection tests remain
mandatory for every comparison that claims reader-demand qualification.

This decision does not approve candidate execution in Story 1.2, additional
source acquisition, app/cache changes, native trials, deployment, production
profiles, reader quality or changes to OQ-2/3/4/6. All three real alpha/EXIF future
gates and all five synthetic normalization checks remain mandatory within their
existing scope. The 60 originals, 55 CMS bindings and prior private proofs remain
preserved.

The alternative is to retain r2 and prepare a bounded PoC layout diagnostic with
explicit viewport, insets, text scale, UI/audio state and binding identities.
The protocol permits calculation/admission evidence, and the implementation
plan allows bounded P0 use of the PoC. Such evidence must explicitly name the
legacy PoC purpose and cannot qualify the future reader or native quality.
No applicable diagnostic/admission evidence is ready today. Implementing an
accepted reader remains separately authorized mobile work outside this story.

## Retained r2 identities

- Corpus: `dbbe7b91b254abc2cff44fb297801f332f9107723a17b508e8ccc1c27cc00f92`.
- Protocol: `6a08848cd2d790e77911fe22529e50dbaa008d2951243bbd7b5b1bf4f0bda4d4`.
- Spec: `f2c18249ae99428938ecd365d62352fbdaff2328d441f5050803c4daf766d99b`.
- Verifier: `acea35f10849ab861e7c6060cbc19be7bd90724cc2a8ab14651058366c5ef681`.

These identities record the preserved r2 baseline. The approval creates a new
r3 preparation scope and does not retroactively certify r2 or its reader gate.
