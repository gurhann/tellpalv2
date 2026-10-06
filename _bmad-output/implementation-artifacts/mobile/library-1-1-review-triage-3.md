# Story 1.1 review iteration 2 final triage / iteration 3 handoff

Date: 2026-10-05. Three fresh-context reviewers completed against the full tracked/untracked baseline diff (snapshot SHA079e794fb9a5784c9edba7f7e9fae5542a7284da09328b0d51fd92900adece62). Edge review initially stopped because its deletion-check reference was absent; the installed skill reference was copied byte-identically into the rendered snapshot and the same reviewer resumed/completed. No review instruction was changed.

## Classified findings

|ID|Severity|Route|Evidence and required action|
|---|---|---|---|
|T1|high|bad_spec|Default HttpFileService awaits send and body without a deadline; WebHelper admits ten concurrent calls and queues the rest. UI timeouts do not free those calls. Bound the actual transport lifecycle, including stalled headers and trickling/stalled bodies, and prove a current retry starts after the full queue was occupied.|
|T2|high|bad_spec|LibraryImageCache.close drains promotion/repository work but not admitted WebHelper download/file writes. A late transfer can create untracked files after emptyCache. Stop new transfers, cancel/drain every admitted/queued download and its complete file/metadata publication before emptying/closing storage. Prove stalled headers/body and late completion after close cannot write files or metadata.|
|T3|medium|patch|Blind and edge claims deduplicated: _promote skips expired shared entries; a real CacheManager/codec reproduction leaves shared.validTill expired after fresh decoded success. Preserve active-reader file bytes while renewing validated shared-cache reuse safely; assert the next owner uses cache without a new fetch. Reproduction: /private/tmp/library_expired_cache_review_test.dart.|
|T4|medium|patch|Root reproduced three RenderFlex overflows of99px at914×411dp, enlarged text, long-title missing artwork. Make error placeholder content fit its available card height, preserving actual title semantics,48dp retry and all existing portrait reference geometry/goldens. Reproduction: /private/tmp/library_short_viewport_review_test.dart and corresponding log.|
|T5|high|patch|Standalone LibraryHomeTestControllerTest bypasses profile selection. Add real Spring profile/context+mapping coverage proving production excludes both controller/service and /api/test/library; assert local/test still expose the guarded endpoint without Firebase/production connections.|
|T6|medium|patch|Existing profile tests compare strings and change source; they never reuse one source under P1 and P2 through JDBC/query/preparation. Add PostgreSQL coverage proving P2 excludes P1, schedules new preparation, publishes separate P2 revision and never serves stale P1 as current.|

No intent gap. T1/T2 loop back only outside the frozen block; T3–T6 patches survive and must be completed. Review iteration increments to3. Only LibraryImageCache is rolled back for re-derivation; preservation snapshot: /private/tmp/tellpal-library-review-loop3/library_image_cache.dart. Existing package seams/architecture and all other source remain in place.

## KEEP

Preserve background successful-catalog pinning, explicit single-flight renewal, context/catalog/image/owner transfer keys, responsive artwork owner replacement, actual delivered provenance, coalesced promotion and no rewriting an active shared file, scoped retry eviction, draining SQLite repository and8 passing cache regressions. Preserve CMS draft/revision snapshot and every backend/asset contract/fixture, auth guard, architecture/interface, count/lifetime bounds, request deadlines, matched reference goldens, actual HTTP/bitmap assertions and PoC. Do not edit frozen intent, reference snapshots, vendor package sources or user local processes. Do not replace the CacheManager with a bespoke media pipeline. Prior iteration2 acceptance is historical once affected source changes; rerun actual native acceptance and record new source hashes before closure.
