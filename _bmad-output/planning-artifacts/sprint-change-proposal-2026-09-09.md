# Sprint Change Proposal: Separate Lullaby Listing and Playback Covers

## 1. Issue Summary

The legacy lullaby records use two distinct visual assets:

- `image_url`: a static image shown while a lullaby is listed.
- `summary_image_url`: an animated GIF shown after the lullaby is opened.

The current CMS model has one shared `listeningCoverMediaId` for `LULLABY`. It can store an
`IMAGE` asset, but it cannot represent both legacy visual roles at the same time. The CMS therefore
cannot prepare an unambiguous migration or editorial record for a lullaby that has separate static
and animated covers.

This change is limited to CMS and the admin API. Public/mobile endpoints, public projections,
delivery packages, and asset-processing behavior remain unchanged.

## 2. Impact Analysis

### Trigger and evidence

| Checklist item | Status | Finding |
| --- | --- | --- |
| 1.1 Triggering story | [x] Done | Story 1.3 introduced the content-level listening cover; Story 1.4 and Story 1.7 made it editable for shared lullaby playback. |
| 1.2 Core problem | [x] Done | A newly discovered legacy-data requirement exposes a missing second shared cover role for lullabies. |
| 1.3 Evidence | [x] Done | Legacy rows shown in the supplied query contain different `image_url` and `summary_image_url` values, commonly static JPG plus GIF. |

### Epic and story impact

Epic 1 remains valid and stays CMS-focused. No completed story must be rolled back.

A new story is required because this is a distinct editorial ownership rule, not a cosmetic
extension of the existing form:

**Story 1.8: Manage separate shared lullaby listing and playback covers.**

It adds a static `listingCoverMediaId` beside the existing `listeningCoverMediaId`. The latter is
retained as the shared playback/detail cover and may reference a GIF registered as an `IMAGE` asset.

The following deferred work remains deferred:

- public/mobile lullaby reads;
- mobile URL and signed-download resolution;
- image-variant generation, animation conversion, and package delivery;
- mobile rendering and fallback behavior.

### Artifact impact

| Artifact | Status | Required adjustment |
| --- | --- | --- |
| Product requirements | [N/A] | The base requirements already permit image assets and CMS media references. No MVP change is needed. |
| Epic 1 plan | [!] Action-needed | Add Story 1.8 and replace stale text that says LULLABY uses the source/textless cover. |
| ADR-0010 | [!] Action-needed | Extend the cover-ownership decision with the two distinct LULLABY roles. |
| Project memory | [!] Action-needed | Record the new LULLABY listing/playback-cover distinction. |
| Admin API rules | [!] Action-needed | Document field ownership, IMAGE validation, and full-update preservation rules. |
| CMS UI standards | [N/A] | Existing content-detail and asset-picker rules apply; no durable UI rule is needed unless the implementation exposes a missing guardrail. |
| Public/mobile contracts | [N/A] | Explicitly out of scope. |

## 3. Recommended Approach

Choose **Direct Adjustment**: add a focused Story 1.8 to Epic 1 and implement it without
reworking the existing shared playback model.

| Option | Viability | Effort | Risk | Decision |
| --- | --- | --- | --- | --- |
| Direct adjustment | Viable | Medium | Low | Selected |
| Roll back Stories 1.3/1.4/1.7 | Not viable | High | High | Rejected; their ownership boundaries remain correct. |
| Reduce MVP scope | Not viable | Low | Medium | Rejected; CMS must represent the known legacy data before migration. |

The proposed ownership is:

| Visual role | Field | Scope | Expected format | Legacy source |
| --- | --- | --- | --- | --- |
| Listing/card cover | `listingCoverMediaId` | `Content` / `LULLABY` only | Static `IMAGE` | `image_url` |
| Playback/detail cover | `listeningCoverMediaId` | `Content` / `LULLABY` only | `IMAGE`, initially including GIF | `summary_image_url` |

Both references are optional at this CMS stage, must be positive IMAGE assets when present, and
must not be copied to `ContentLocalization` or `LullabyPlayback`. The new database column is
nullable and must be protected by the same type and direct-SQL integrity rules as the existing
content-level cover references.

## 4. Detailed Change Proposals

### Epic 1 / new Story 1.8

**NEW**

```md
### Story 1.8: Ninninin liste ve playback kapaklarını ayrı yönetme

Bir CMS editörü olarak, ninninin listede gösterilen statik kapağını ve açıldığında kullanılan
animasyonlu kapağını ayrı yönetmek istiyorum; böylece eski veri iki görsel rolünü kaybetmeden
yeni sisteme taşınabilir.

Acceptance Criteria:

Given a LULLABY content item,
when an editor selects or clears a listing cover,
then `listingCoverMediaId` is stored at content scope and references a positive IMAGE asset.

Given a LULLABY content item,
when an editor selects a playback/detail cover,
then the existing `listeningCoverMediaId` remains independent from `listingCoverMediaId` and may
reference a GIF registered as an IMAGE asset.

Given a LULLABY with multiple localizations,
when either shared cover changes,
then no localization, playback audio, duration, musician, instrument, processing state, or public
mobile contract changes.

Given a CMS content detail screen,
when an editor opens a LULLABY,
then it presents clearly labelled static listing-cover and animated playback-cover pickers with
their previews and field-level errors.

Given a non-LULLABY content item,
when a listing-cover value is submitted,
then the request is rejected and the stored value is unchanged.
```

### ADR-0010

**OLD**

```md
MEDITATION and LULLABY also use one content-level listening cover across languages.
```

**NEW**

```md
MEDITATION continues to use one content-level listening cover across languages. LULLABY has two
content-level, language-independent visual roles: `listingCoverMediaId` for its static listing
image and `listeningCoverMediaId` for its playback/detail image, including legacy GIF assets.
```

**Rationale:** This preserves the legacy semantic distinction without assigning shared media to
localizations or changing any public delivery behavior.

### CMS/Admin contract

**OLD**

```md
The content update payload and CMS form expose `textlessCoverMediaId` and `listeningCoverMediaId`.
```

**NEW**

```md
For LULLABY, the content update payload and CMS form expose `listingCoverMediaId` and
`listeningCoverMediaId` as separate shared IMAGE references. Full-update callers must send both
stored values when preserving them; omitted values clear their respective references under the
existing full-update semantics.
```

**Rationale:** The new field must not silently clear the GIF reference during an unrelated CMS
metadata update.

## 5. Implementation Handoff

**Scope classification:** Moderate. The work touches Flyway persistence, content-domain invariants,
admin REST contracts, CMS form composition, and regression tests; it does not alter public/mobile
contracts.

Implementation order:

1. Add the `contents.listing_cover_media_id` migration and database integrity checks.
2. Extend the `content` aggregate, commands, asset-reference validation, admin read/write DTOs, and
   OpenAPI documentation.
3. Add the CMS DTO/schema/view-model/form picker and previews for the two LULLABY visual roles.
4. Cover valid JPG/GIF selection, wrong type, non-LULLABY rejection, full-update preservation, and
   locale isolation with backend integration and CMS interaction tests.
5. Update ADR-0010, project memory, admin API rules, Epic 1, and sprint status only after approval.

Success criteria:

- The CMS can independently save, clear, and read both shared LULLABY covers.
- Legacy static-JPG plus animated-GIF pairs can be represented without data loss.
- No localization-level visual reference is created.
- Existing public/mobile responses and processing jobs have no contract or behavior change.

## 6. Checklist Completion

| Section | Status |
| --- | --- |
| 1. Understand trigger and context | [x] Done |
| 2. Epic impact assessment | [x] Done |
| 3. Artifact conflict and impact analysis | [x] Done |
| 4. Path forward evaluation | [x] Done |
| 5. Sprint change proposal components | [x] Done |
| 6. Final review and handoff | [x] Approved on 2026-09-09 |

## Approval and Routing

The user approved this proposal on 2026-09-09. The change is routed to the Developer workflow as
Story 1.8, with the CMS/admin-only boundary and success criteria in this proposal preserved.
