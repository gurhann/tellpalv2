---
title: 'Story 1.8: Ninninin liste ve playback kapaklarını ayrı yönetme'
type: 'feature'
created: '2026-09-09'
status: 'done'
baseline_commit: 'c5b2ca04e9046dcd4f135f6a34b3b741f532d703'
context:
  - 'AGENTS.md'
  - 'cms/AGENTS.md'
  - 'be/docs/project-memory.md'
  - 'be/docs/adr/ADR-0010-content-cover-ownership.md'
  - '_bmad-output/planning-artifacts/sprint-change-proposal-2026-09-09.md'
---

<frozen-after-approval reason="human-approved scope — do not modify unless the user renegotiates">

## Intent

**Problem:** Legacy LULLABY rows can contain a static `image_url` for listing and an animated
`summary_image_url` for playback/detail. One content-level `listeningCoverMediaId` cannot preserve
both visual roles.

**Approach:** Add a LULLABY-only content-level `listingCoverMediaId` for the static list/card
image. Preserve the existing content-level `listeningCoverMediaId` as the independent playback/
detail cover, including GIF assets registered with media type `IMAGE`. Both assets stay shared by
all localizations.

## Boundaries & Constraints

**Always:** `listingCoverMediaId` is nullable, positive when present, and references an `IMAGE`
asset. It is permitted only for `LULLABY`; localization rows and `LullabyPlayback` do not store
either cover. The full content-update request preserves every cover field only when the client
sends its existing value. Existing asset validation and field-level `ProblemDetail` mapping apply.

**Never:** Change public/mobile responses, signed URL resolution, package generation, image
optimization, GIF conversion, mobile rendering, or playback processing. Do not reuse the listing
cover as a fallback for the playback/detail cover, and do not migrate legacy production data in
this story.

## Acceptance Criteria

- Given a LULLABY content item, when an editor saves a valid static IMAGE as `listingCoverMediaId`,
  then the content read response returns it and every localization shares the same reference.
- Given a LULLABY content item, when an editor saves an IMAGE asset whose MIME type is `image/gif`
  as `listeningCoverMediaId`, then it remains independent from `listingCoverMediaId`.
- Given a non-LULLABY content item, when a request contains `listingCoverMediaId`, then the request
  fails without partial persistence.
- Given a content update preserving existing covers, when a CMS metadata edit is submitted, then
  both cover IDs remain unchanged.
- Given the LULLABY CMS detail view, when both cover fields are rendered, then their static-list
  and animated-playback purposes are visibly labelled and separately selectable.

## Code Map

- `be/src/main/resources/db/migration/` — nullable `listing_cover_media_id`, database ownership and
  asset integrity rules.
- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` — LULLABY-only aggregate invariant.
- `be/src/main/java/com/tellpal/v2/content/application/` and `web/admin/` — command, IMAGE
  validation, DTO, OpenAPI, and field-level error contract.
- `cms/src/features/contents/` — DTO/schema/view model, mutation preservation, and labelled asset
  pickers/previews.
- `be/src/test/java/com/tellpal/v2/content/` and `cms/src/features/contents/**/*.test.*` — backend
  integration and CMS interaction regression coverage.

## Verification

- `cd be && ./mvnw test -Dtest=ContentTest,ContentManagementIntegrationTest,ContentAdminIntegrationTest`
- `cd cms && npm run test`
- `cd cms && npm run build`

</frozen-after-approval>

## Verification Notes

- Attempted `cd cms && node node_modules\\@playwright\\test\\cli.js test e2e/visual` on 2026-09-09.
  Vite started, but all Chromium visual cases failed immediately during browser execution; the
  environment did not provide a working Chromium runtime. No baselines were updated.

## Suggested Review Order

**Ownership and database integrity**

- The migration makes the new reference nullable, LULLABY-only, and safe for direct SQL writes.
  [V29__add_lullaby_listing_cover.sql:1](../../be/src/main/resources/db/migration/V29__add_lullaby_listing_cover.sql#L1)

- The aggregate keeps all cover roles content-scoped and rejects unsupported ownership.
  [Content.java:252](../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L252)

**Admin write and read contract**

- Validation checks both cover references through the asset module before persistence.
  [ContentManagementService.java:83](../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java#L83)

- The admin update request documents and carries the LULLABY-only listing cover.
  [ContentAdminController.java:138](../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java#L138)

**CMS editor binding**

- The form gives static listing and animated playback covers separate labelled pickers.
  [content-form.tsx:343](../../cms/src/features/contents/components/content-form.tsx#L343)

- The mutation always preserves and submits both content-level cover values.
  [use-save-content.ts:75](../../cms/src/features/contents/mutations/use-save-content.ts#L75)

**Regression evidence**

- The integration test covers GIF MIME, localization sharing, validation, and independent clearing.
  [ContentAdminIntegrationTest.java:514](../../be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java#L514)

- The CMS form test protects picker submission and field-level validation rendering.
  [content-form.test.tsx:299](../../cms/src/features/contents/components/content-form.test.tsx#L299)
