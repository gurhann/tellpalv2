# ADR-0008: Firebase Storage Uses One Bucket with Prefix-Isolated Direct Uploads

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

The asset module now owns a real Firebase Storage integration with direct browser uploads.

The project adopts these defaults:

- local and production runtime both use the same Firebase project and bucket
- environment isolation happens through a mandatory bucket path prefix:
  - local runtime uses `local`
  - production runtime uses `prod`
- manual CMS uploads go directly from the browser to Firebase Storage using backend-signed `PUT`
  URLs
- backend upload finalization validates blob existence plus observed `mimeType` and `byteSize`
  before registering or reusing `media_assets`
- generated processing assets reuse the same prefix model, so `/content/...` paths become
  `/{prefix}/content/...`
- `LOCAL_STUB` remains available only for legacy seed data and automated test doubles, not as the
  runtime upload default

The admin asset API adds two endpoints:

- `POST /api/admin/media/uploads`
- `POST /api/admin/media/uploads/complete`

## Consequences

- Local development now requires real Firebase Storage credentials for the backend profile instead
  of silently falling back to stub download URLs.
- CMS upload flows can register newly uploaded Firebase objects without proxying binaries through
  the backend.
- Signed download URL refresh now returns real Firebase/GCS signed URLs for
  `FIREBASE_STORAGE` assets.
- Asset processing paths and manual upload paths share the same prefix policy, so object ownership
  is easier to audit in the bucket.
- Automated tests use a fake Firebase storage client under the `test` profile so the backend test
  suite does not need live cloud credentials.

## Alternatives Considered

- Keeping separate buckets for local and production was rejected because the team explicitly wants
  one Firebase environment and only path-level isolation.
- Proxying uploads through the backend was rejected because the files can be large and the backend
  already models storage references instead of binary ownership.
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
