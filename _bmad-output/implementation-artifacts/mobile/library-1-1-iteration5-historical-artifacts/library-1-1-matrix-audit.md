## Completed iteration5 review: acceptance reopened

R1–R3 publication/metadata lookup/physical eviction failures reopen finite cleanup/disposal and disk ownership proofs (AC6/9 and rows10/11). The passing executed scenarios below do not establish those missing paths. Loopback6 halted pending human authorization. [Findings/plan](library-1-1-review-iteration5.md).

# Story 1.1 iteration 5 executed acceptance audit

The current implementation was verified on 2026-10-06 against baseline `b372c60b53f828f2fcadff40eb1c212684ad3d51`. [Current inventory](library-1-1-verification-audit.json) contains all 62 source hashes, 95 individually successful Flutter cases, named PostgreSQL/Spring methods, native binary identities and redacted artifact paths. Earlier iteration 3/4 executions are historical. Step4 review and human acceptance remain pending; sprint remains review.

## Executed coverage

- B1: eight `LibraryHomeIntegrationTest` PostgreSQL cases cover exact language/order/audience/locks, no substitution, 100 unready predecessors plus earlier inactive/DRAFT entries, omission and source-profile revision changes.
- B2: two `CategoryHomeAdminIntegrationTest` actual Spring/PostgreSQL cases verify admin ProblemDetail/errorCode validation and a configured preview limit of 2 applied after filtering. Existing 401/404/409 outcomes remain covered.
- B3: named `ListingCoverProcessingExecutorTest`, `ListingCoverRenditionProcessorTest`, ownership/persistence and production/local/test mapping cases are individually recorded in the full 368-test backend report, plus one documentation IT. Exact expiry and one-second-after, before recovery and with all captured ownership fields unchanged, reject complete/fail/publish and executor catch; persisted jobs/manifests remain identical.
- C1: 372 CMS unit cases include integer-bound checkbox opt-in/out roundtrip and pinned draft/revision error/reload cases; production build passed. Three Home interaction/visual and two affected category registry visuals passed with existing baselines.
- C2: all three actual CMS cases executed in each server audience (six total), not the skip branch: Turkish order/conflict save/reload; audience/content-language selections; Show in Library checkbox opt-out/in save/reload/catalog visibility. English and card orders remain unchanged; canonical fixture restored.
- F1: current 95-test Flutter machine run individually proves context/attempt/disposal guards, distinct loading/empty/error, background-refresh preservation and explicit single-flight recovery, independent same-asset provider retry, both 600 dp resize directions and actual platform text scaling absent an explicit harness override.
- F2: six actual catalog loopback cases cover stalled headers/body/error-body/trickle, 12 expired attempts followed by successful independent retry, and disposal. Native Android/iOS repeat all six and release every server-observed socket.
- F3: seven real CacheManager/engine-decode promotion cases prove matching-valid expired reuse, corrupt-expired recovery with unchanged active-reader bytes and no next-owner refetch, plus partial second-publication failure rollback and cleanup. Actual package metadata/storage disposal cases preserve admitted errors and drain before close.
- W1: twelve unchanged portrait goldens cover TR/EN/PT ×411×914/800×1280 dp ×1.0/1.3, long localized reference titles, missing artwork, disabled semantics, safe areas/contrast/48 dp controls and scroll retention. Four short/landscape error regressions pass.
- N1: five final-source six-case real catalog/JPEG matrices (30 cases): Android API 30 phone NON_SUBSCRIBER and SUBSCRIBER, API 30 tablet, iOS 27 phone and iPad. Each compares fresh authenticated HTTP metadata and engine-rendered real bitmaps, disabled controls and bounded cache diagnostics.
- N2: current Android phone and iOS tablet fault runs use real backend/GCS artwork through an in-process loopback fault proxy. They prove finite native transport, independent retry before old completion, one corrupt JPEG recovery, retained catalog/geometry on 403→503 background renewal and explicit recovery adopting a different fresh URL/expiry while the exact old URL permanently remains 403. Both selected phone/tablet variants are exercised.
- P1: exact `lib/main_library_dev.dart` Android/iOS compile/closed-entry UI smokes pass with current source/binary identities. Four unchanged PoC tests pass in the 95-test run; prior exact PoC native evidence is retained only with unchanged entry/dependency source hashes. Protected playback is not exercised.

## Iteration5 execution-task mapping

| Spec task | Executed current proof |
| --- | --- |
| Cleanup ownership, finite failure handling and guaranteed disposal | Actual CacheManager/FileSystem transient/persistent partial and retired deletion, enumeration and concurrent/repeated close regressions in F3/full95; unchanged admission/source seal; native faults preserve prior behavior |
| Strict numeric DTO conversion | Normal build_runner output and real HTTP/repository/Cubit invalidResponse for all nine integer positions in F1/full95 |
| V1–V4 regression gaps | Real1MiB-1/exact/+1 success/error bodies with socket release; eight held requests/ninth refusal/freed-slot retry; changed editorial successful renewal keeps snapshot; PostgreSQL captured unchanged attempt at exact lease expiry and one second later before recovery |
| Full verification and real acceptance | Backend368+1, CMS372/build/5affected/6actual, Flutter95/generation/analyze/pubget, HTTP34JPEGs, N1/N2/P1 and direct binary captures for all9runs; API36 failed startup remains a gap |

## Frozen 13-row matrix

|Row|Approved state|Executed proof and result|
|---|---|---|
|1|Eligible configured rows|B1,B2,C2,N1: NON_SUB TR[3,2,1,4], EN/PT[1,2,4,3]; first five eligible cards, correct identities/languages/locks. Configured2 independently passes.|
|2|Missing/unready ordinary localization|B1,N1: inactive/DRAFT/unready/missing language filtered before limit; empty rows omitted, surviving order preserved.|
|3|Three Turkish plus two English-only|B1,N1: ordinary TR category2 returns exactly[2,3,4], no padding.|
|4|Labeled English row in Turkish UI|B1,C2,N1: explicitEN category3[9,10], English item language with TR UI; reset mapping removes missing-TR row.|
|5|CMS changes only Turkish order|B1,C1,C2,N1: actual saved/reloaded TR[3,2,1,4], independent English/card order;409 and failed/background reload keep pinned draft. Checkbox visibility changes and restores only TR.|
|6|Subscriber UID|B1,C2,N1,real audience: same authorized existing identity with server SUB returns TR[3,2,1], EN/PT[1,2,3], no lock; row4 omitted.|
|7|Missing/invalid/unlisted token; bad language|B2,B3,real auth:401/401/403/400 ProblemDetail, no catalog disclosure and no unauthorized query.|
|8|Loading/empty/network error|F1,W1: distinct localized outcomes and current-context retry.|
|9|Catalog waiting limit|F1,F2,N2: catalogWait15s; actual headers/body-idle6s and total12s, max8 requests and1MiB body. Stalled/trickling/error bodies cancel and release sockets; obsolete completion rejected.|
|10|Absent/corrupt/late artwork|F1,F3,W1,N2: card error preserves title/lock/geometry/other cards;4/4/8s transfer and2s admission at10s artworkWait. Corrupt expired files recover without active-reader rewrite; phone/tablet fresh URLs replace rejected old URLs.|
|11|Old callbacks/retry/context/disposal|F1–F3,N2: obsolete successes/errors/timeouts/disk publication cannot win; both responsive directions restart ownership;12 catalog timeouts do not exhaust admission; partial promotion failure rolls back and drains cleanup.|
|12|Reselection/disabled controls|W1,N1: stack/scroll and selected Library retained; disabled roles/names and48dp controls; no deferred destination/action.|
|13|Stale workerA after retryB|B3: captured attempt/lease/source/profile completion and failure reject A; rejected completion catch cannot fail B or replace ready manifest.|

## Ten acceptance criteria

|AC|Current executed proof|Result|
|---|---|---|
|1 catalog/order/eligibility/limit|B1,B2,N1,real HTTP34 JPEGs|PASS|
|2 independent CMS order save/reload|B1,C1,C2,N1|PASS|
|3 exact selected/declared content language|B1,C2,N1|PASS|
|4 server audience/lock flags|B1,C2,N1,real two-audience/auth|PASS|
|5 distinct outcomes/artwork geometry|F1,F3,W1,N2|PASS|
|6 finite waits/current attempts/obsolete disposal|F1–F3,N2; R2 lookup-error close repro|REOPENED|
|7 shell/back/reselection/disabled semantics|W1,N1|PASS|
|8 matched references/languages/scales/contrast/48dp|W1 and30 real native matrix cases|PASS on explicitly recorded emulated profiles|
|9 real decoded cover/stale ownership|B3,F3,actual34 JPEGs; R1/R3 ownership repros|REOPENED|
|10 unchanged PoC|Four full-run PoC tests, unchanged-source retained native smokes, current exact dev-entry smokes|PASS|

All four iteration 5 execution tasks have corresponding current executed coverage above. Limits remain 80 disk objects/10 min and 48 decoded entries/32 MiB. Provider 403 is deliberately permanent in the proxy, not a claim that Firebase naturally expired during this run. Android API 24/iOS 15 minimums compile; API 24/iOS 15 runtime and physical-device performance remain unavailable. Universal Simulator lipo limitation and unrelated CMS raster gaps remain recorded separately. Only the home-list EXT-CATALOG slice closes; later consumers remain open.


Iteration5 verification completed on2026-10-06: backend368 tests plus1 documentationIT; CMS372 tests/build,5 affected checks and6 actual cases across both audiences; Flutter95 visible tests/generation/analyze/pubget. Five six-case native matrices passed on API30 phone/tablet and iOS27 phone/iPad, plus Androidphone/iPad faults and both exact dev entries. Real HTTP34 JPEGs and401/401/400/403 passed; canonical NON_SUB fixture and both Android display overrides restored. All native binaries were directly captured per run with unchanged source seals. API36 tablet crashed before Dart and remains a current runtime gap; minimum/older/physical/universal/provider-expiry gaps stay explicit. Fresh Step4 and human acceptance pending; frozen intent and sprint review unchanged.
