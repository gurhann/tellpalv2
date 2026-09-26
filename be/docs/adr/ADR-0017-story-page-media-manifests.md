# ADR-0017: Immutable Story Page Media Manifests for the Playback Experiment

## Status

Accepted

## Context

The asset processing worker previously planned story-page image outputs without reading or transforming image bytes. The mobile page API continued to resolve the original localized illustration. A protected playback experiment needs a stable, ordered set of real, verified page-image renditions while legacy clients continue using their existing references.

## Decision

- `content` owns localized page source identities and order and passes an ordered snapshot to the separate `STORY_PAGE_MEDIA` job through asset public contracts.
- `asset` reads and bounds source bytes, accepts JPEG and PNG input, decodes under a pixel cap, resizes without upscaling, encodes JPEG, uploads, then verifies stored MIME type, size, SHA-256, and image decodability before publishing readiness.
- The initial configurable experiment profile defaults to a 1440-pixel longest edge, JPEG quality 85, 40 million input pixels, and 20 MiB source bytes. These are operational experiment defaults, not production rendition commitments.
- Revision identity incorporates ordered page IDs/numbers, source-byte SHA-256 fingerprints, page audio source IDs, and profile version. Objects live below an environment-prefixed `revisions/{revision}` path. Each completed revision is retained with an immutable manifest; retries may reuse only identical content-addressed output.
- Page audio remains the existing source asset reference. The job kind does not update legacy localization readiness or alter current public page responses.
- Test delivery requires a verified Firebase ID token and a UID listed in `TELLPAL_STORY_PAGE_MEDIA_TEST_ALLOWED_FIREBASE_UIDS`. The default allowlist is empty. The endpoint returns short-lived signed rendition URLs only after authorization and only for a manifest matching the current ordered source snapshot. An explicit retained revision can be fetched by allowlisted testers.

## Consequences

- Image processing completion means real stored bytes were checked, and failures remain visible through the existing persistent retry lifecycle.
- Replacing a page source creates a new output path and manifest revision; older revisions remain available to authorized test clients.
- Test credentials are environment configuration and must not be added to mobile binaries or committed configuration.
- JPEG/PNG are the supported input formats for this initial page-media slice. Other image formats fail the job with an actionable processing error.

## Alternatives Considered

- Reusing `DELIVERY` was rejected because its status listener changes legacy localization readiness.
- Replacing original assets was rejected because legacy readers and older revisions must remain available.
- Public or anonymous test access was rejected; only configured Firebase testers receive short-lived URLs.

## Related Files or Modules

- `architecture.md`
- `be/src/main/java/com/tellpal/v2/asset/`
- `be/src/main/java/com/tellpal/v2/content/api/PublicContentQueryApi.java`
- `be/src/main/java/com/tellpal/v2/content/web/mobile/TestStoryPageMediaController.java`
- `be/src/main/resources/db/migration/V32__add_story_page_media_revisions.sql`

## Supersedes / Superseded By

- None
