# ADR-0008: Firebase Storage Uses One Bucket with Backend-Mediated CMS Files

## Status

Accepted

## Context

The first asset implementation modeled media storage as `provider + objectPath` references, but the
`FIREBASE_STORAGE` provider still used a stub client that returned fake signed URLs. The CMS also
registered uploaded assets only after operators or tests inserted the storage object by other means.

That left three gaps:

- local and production runtime behavior diverged because local relied on `LOCAL_STUB`
- the CMS could not upload original image or audio files through the real asset module
- generated processing assets had no environment prefix isolation inside the shared bucket

The product direction is to use one Firebase project and bucket across local and production
runtime, while still separating object ownership by deterministic environment prefixes.

## Decision

The asset module now owns a real Firebase Storage integration with backend-mediated CMS file
operations.

The project adopts these defaults:

- local and production runtime both use the same Firebase project and bucket
- environment isolation happens through a mandatory bucket path prefix:
  - local runtime uses `local`
  - production runtime uses `prod`
- manual CMS uploads go through the backend using `multipart/form-data`; the browser does not send
  media bytes directly to Firebase/GCS
- CMS inline previews use short-lived backend content tokens and stream bytes through the backend;
  browser-rendered preview URLs do not point at Firebase/GCS signed URLs
- backend upload writes object streams to Firebase Storage and validates observed `mimeType` and
  `byteSize` before registering or reusing `media_assets`
- the older signed upload handshake and backend proxy endpoint remain as deprecated compatibility
  paths for older clients, not as the CMS default
- generated processing assets reuse the same prefix model, so `/content/...` paths become
  `/{prefix}/content/...`
- `LOCAL_STUB` remains available only for legacy seed data and automated test doubles, not as the
  runtime upload default

The admin asset API includes:

- `POST /api/admin/media/uploads` for backend multipart upload
- `POST /api/admin/media/{assetId}/content-token` for authenticated preview-token issuance
- `GET|HEAD /api/admin/media/{assetId}/content?token=...` for token-authenticated streaming
- deprecated compatibility endpoints for signed upload initiation, upload completion, upload proxy,
  and cached signed download URL refresh

## Consequences

- Local development now requires real Firebase Storage credentials for the backend profile instead
  of silently falling back to stub download URLs.
- CMS upload and preview flows no longer depend on operator networks being able to reach
  `storage.googleapis.com`.
- Backend bandwidth now includes CMS upload and preview traffic; Railway upload limits and service
  sizing must account for original image/audio files.
- Signed download URL refresh still returns real Firebase/GCS signed URLs for `FIREBASE_STORAGE`
  assets, but CMS preview components must not render those URLs directly.
- Asset processing paths and manual upload paths share the same prefix policy, so object ownership
  is easier to audit in the bucket.
- Automated tests use a fake Firebase storage client under the `test` profile so the backend test
  suite does not need live cloud credentials.

## Alternatives Considered

- Keeping separate buckets for local and production was rejected because the team explicitly wants
  one Firebase environment and only path-level isolation.
- Keeping direct browser upload as the CMS default was rejected after production operators hit
  network/certificate failures while reaching `storage.googleapis.com`.
- Removing signed upload endpoints immediately was rejected because keeping them deprecated for a
  transition period avoids breaking older clients and manual diagnostics.
- Reusing `LOCAL_STUB` for local runtime was rejected because it hides Firebase credential and CORS
  problems until late environments.
- Extending `GET /api/admin/media/{assetId}` or `POST /api/admin/media` to implicitly create upload
  URLs was rejected because upload initiation and asset registration are separate use cases with
  different validation and idempotency semantics.

## Related Files or Modules

- `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/`
- `be/src/main/java/com/tellpal/v2/asset/application/AssetRegistryService.java`
- `be/src/main/java/com/tellpal/v2/asset/web/admin/AssetAdminController.java`
- `cms/src/features/assets/`

## Supersedes / Superseded By

- None
