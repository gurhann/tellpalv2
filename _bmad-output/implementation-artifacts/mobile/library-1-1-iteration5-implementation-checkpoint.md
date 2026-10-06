# Story 1.1 iteration 5 implementation checkpoint

Date: 2026-10-06. Scope: source corrections and targeted verification only. Full backend/CMS/Flutter and real CMS/HTTP/native acceptance are delegated to root and remain pending here. Prior modified-source evidence is historical; no Step4 or human acceptance is claimed.

## Implemented

- R1: partial cleanup retains owner/path until deletion succeeds or persisted metadata retains it. Admission and pending creations are bounded to 80 paths; failed cleanup has bounded subsequent retry. Duplicate path references retain one existing owner.
- R2: retirement keeps explicit files after metadata removal, coalesces concurrent cleanup, bounds records/reservations at 80, rejects new transfers on failed capacity cleanup, and handles timer errors. No immediate capacity retry loop.
- R3: close cancels/drains transfer/promotion/storage work, attempts cleanup and finally disposes the repository. Failed enumeration retains persisted file ownership. Retained local deletion can be retried after disposal without delegate access; concurrent closes coalesce and original cleanup errors propagate.
- P1: source DTO converters reject fractional/non-finite/non-numeric integer fields and regenerate serializers through build_runner. Real HTTP tests cover all nine integer positions through repository and Cubit invalidResponse.
- V1: real native loopback 1MiB-1, exactly1MiB and1MiB+1 success/error bodies verify response behavior, socket release and a successful current retry.
- V2: eight real loopback requests hold admission; ninth opens no connection; releasing a slot permits an independent retry.
- V3: successful renewal changes editorial order/names/titles/locks/metadata/membership and a row language; tests prove the loaded snapshot stays pinned and only matching category/language/content phone/tablet covers and expiry are adopted.
- V4: real PostgreSQL tests advance the application clock exactly to lease expiry and one second beyond before recovery, with captured processingId/attempt/source/profile/lease unchanged. Complete/fail/publish and executor rejected-publication/catch retain the full persisted job and existing ready manifests.

## Executed targeted verification

- Normal `dart run build_runner build`: success; generated serializers contain source `_integer`/`_optionalInteger` converters. Log: `/private/tmp/library-iteration5-generation.log`.
- `flutter analyze`: no issues. Log: `/private/tmp/library-iteration5-analyze.log`.
- Six targeted Flutter files: 69 tests passed (disposal, promotion, cache transport, repository, catalog transport, Cubit). Log: `/private/tmp/library-iteration5-targeted.log`.
- Wrapper through bash (existing mvnw executable bit is absent), real Colima/Testcontainers PostgreSQL: `LibraryHomeIntegrationTest`, 8 tests, zero failures/errors/skips. Log: `/private/tmp/library-iteration5-backend-targeted.log`. `DOCKER_HOST=unix:///Users/gurhankucuk/.colima/default/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` were used.
- LibraryCacheAdmission remains byte-identical to its preserved pre-loop snapshot. No vendor, golden, fixture, PoC, process-ownership, module-edge or production changes.

## Source SHA256 at checkpoint

```json
{
  "app/mobile/lib/core/network/library_cache_lifecycle.dart": "3db45da68282b3b3f4b2c50ea97b4229ecb35df79853772a4d3770d8c80e3689",
  "app/mobile/lib/core/network/library_image_cache.dart": "5579d213729ffdbac5dced6b1792cd88cbefca2bb7ef26f448587d7631202647",
  "app/mobile/lib/features/library/data/model/library_catalog_model.dart": "48d5fe8750ed99f612e07b8e17caa0c857fe8c2b00a1a61e3fce068751a2fb51",
  "app/mobile/lib/features/library/data/model/library_catalog_model.g.dart": "7cc56a37e0438e553c9d2772341e49f59de514c0a7323cdedb8960a1cd8d68b8",
  "app/mobile/test/library/library_cache_disposal_test.dart": "dfbe8374d156913665ff639826c4200aa9cb3bbbe5c43b18ea6b45076700878d",
  "app/mobile/test/library/library_cache_promotion_test.dart": "db25d2b2df73307bb20d97a9728d9bbe799c6d9f8d34f592ed06abb53ce92e7c",
  "app/mobile/test/library/library_repository_test.dart": "a685c43d70f0366c9689b41cb7a76bb9b70db3e6a6aab95eebbd5107b5e5cbbf",
  "app/mobile/test/library/library_catalog_transport_test.dart": "4e74a35ef18df32856d85f2ad51b9d62a4fdfdb2aa704488a9d7e3d5e61f398b",
  "app/mobile/test/library/library_cubit_test.dart": "82b2a52a821bb14d46bf190b3f1bee70aec454ae80c855a4ee81fe3b97064e14",
  "be/src/test/java/com/tellpal/v2/category/LibraryHomeIntegrationTest.java": "7879c1d7683a55309c4b7ada7afacef221759e4500b2fc17caff30a07cdd9130"
}
```

## Remaining root work

Run full required backend/CMS/Flutter verification and actual corrected-source CMS/HTTP/native matrices/fault/entry acceptance. Refresh source/binary/13-row/10-AC/EXT-CATALOG evidence only after those executions pass. Keep all existing minimumOS/provider/physical gaps explicit. Spec final acceptance checkbox remains pending.
