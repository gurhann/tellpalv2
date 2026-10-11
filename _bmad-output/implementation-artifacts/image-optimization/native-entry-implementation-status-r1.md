# Native fixture entry implementation receipt — r1

Date: 2026-10-11. Contract: `native-entry-implementation-r1.md` and `spec-1-5-native-experiment-entry.md`. Source baseline: `c72a874b9b348fba60826be78d235aee4a7158b6`.

This is an implementation receipt, not a physical qualification or measurement receipt. The accepted scope allowed source changes, synthetic tests and isolated local build attempts. No real Firebase authentication, native installation, account mutation, paid reservation, Device Streaming launch, production write or Story 1.6 benchmark was performed. `NATIVE-ENTRY`, OQ-6 and Story 1.6 remain open.

## Implemented source boundaries

The new host project has locked Firebase Admin, cryptography and Pillow dependencies, strict private config/manifest validation, exact immutable fixture bytes, live local/test backend binding, token-free reachability probes, one-time paired P-256 proof, pinned TLS, bounded workers/process groups and three finite driver commands. Inputs, signing materials, private logs and outputs stay outside the checkout. Public manifest projection excludes every host file path. Launcher public reports remain pending until both token-free reachability legs complete. A successful verify-entry receipt separately requires successful envelope admission, a complete selected immutable fixture transfer, an explicit freshly authenticated app revoke and exactly one native settled outcome. Authentication failure, expiry, host revocation, missing fixture or refused/missing native result produce only a failed receipt.

The Dart entry refuses debug/web modes, takes a native envelope once, verifies exact launcher/UID/project/session/pin/public-manifest bindings and detaches the ephemeral token into its own runtime holder. Its fresh generated GetIt container, ID-only auto_route router, Clean Architecture fixture feature and Freezed Cubit have independent route/provider/cache lifecycle ownership. Exact size/SHA/MIME/intrinsic dimensions are checked for warm and cold encoded files. Every transfer uses the existing cancellable `LibraryImageCache.profile` clientFactory seam. Expiry/revocation rejects late callbacks; renderer and listener handles use explicit clones and disposal.

Android has opt-in source sets, separate profile/release app/test identities and private signing/output guards. Apple has separate ImageExperiment/ImageExperimentUITests targets, three experiment configurations, a lowercase scheme, pod linkage declarations and ordinary Runner entry guards. Native bootstrap and private ledger storage are independent on each platform.

The private recorder caps export at 512 rows and 256 KiB with a dropped count and two-second deadline. It records hashes, role, slot/DPR/fit/alignment, requested and actual decode geometry, monotonic clock domain/unit, owned transfer bytes and separate status observer overhead. Unsupported counters remain null with reasons. Actual current onstage RenderImage draw may be observed; first rasterized target frame and physical presentation remain unverified. All three cache-state preparation commands return unavailable when internal owner settlement cannot be proved.

Cleanup attempts each owned stage even after a prior failure/deadline, retains fixed `cleanup_unavailable`, attempts native revoke and releases the fresh container. Native revoke carries only a fixed settled/refused result; the default and all error/lifecycle paths refuse. Settled requires a verified current draw, an exact duplicate-free authenticated app-revoked response, successful cache/router/container cleanup and stored private ledger. Native outcome gates refuse result upgrades after revocation. Synthetic tests cover native revoke failure, cache failure, double close and a cache close that never completes.

The synchronous review patch batch corrected strict revoke framing (`application/json`, length 2, no transfer encoding) and coalesced transport close. Polling is intentionally retired before the owned app revoke while expiry/credential guards stay active. A delayed retired status response cannot invalidate that revoke. The actual routed app/view now has a regression that decodes and draws an admitted fixture through the real cache/provider and pinned TLS transport, observes `onDraw`, authenticates revoke and releases its isolated container automatically. A temporary `verifiedDraw: true` to `false` mutation made this test fail at settled/refused as expected; the exact source was restored. Revocation before mounting creates no fixture route and performs bounded refused cleanup.

Host source identity and copying share one bounded deterministic file selection, including previously omitted manifests, native guards, Podfile/lock, pubspec, resources and assets; current original and copied bytes are checked before receipt use. Real prerequisite validation has positive synthetic platform fixtures and negative authorization/freshness/device/project/artifact/signing/installed-hash/quota/reservation checks. False authentication authorization refuses before Firebase/broker/launcher creation. Non-object Admin credential JSON roots return a fixed sanitized refusal. Swift metadata/challenge/envelope time validation uses overflow-safe arithmetic; Kotlin scans quote/escape-aware JSON nesting before recursive parsing on every untrusted path.

## Actual implementation checks

Commands are relative to the repository root unless a working directory is named. No test in these suites was skipped.

| Check | Actual result | Evidence boundary |
| --- | --- | --- |
| `app/mobile`: `dart run build_runner build --delete-conflicting-outputs` | Pass; final generation completed in 5 s and wrote 7 outputs | Installed build_runner reported the legacy delete option as removed/ignored. All annotations generated successfully. |
| `app/mobile`: `flutter test --concurrency=1 test/image_experiment test/native_runtime_guard_test.dart test/library test/story_detail` | Final 268/268 pass, 51 s | Includes all 57 fixture tests, real loopback TLS/socket, cache/SQLite/ownership, default release guard and Library/detail regression tests. |
| Fixture subset of the final full Flutter command above | 57/57 pass | Exact TLS pin-before-header, strict revoke framing, actual Dart-to-Python FixtureHandler interoperability, delayed status/close race, actual routed view draw/automatic cleanup, pre-mount revocation, duplicate/malformed JSON, fixture negatives and clone/draw ownership. |
| Automatic draw teardown mutation check | Pass | Temporarily setting the actual view's `verifiedDraw` false caused its expected settled/refused failure; source restored in `finally`. |
| Locked host suite | Final 76/76 pass, 3.894 s; independent final root run 76/76, 3.955 s | Synthetic-only; exact command below. Includes shared source selection/copy integrity, real prerequisite validation, sanitized malformed credential CLI refusals, driver outcome negatives and bounded server/process tests. |
| `app/mobile`: `flutter analyze` | Final pass, no issues, 2.7 s | Final patch API, tests and all ordinary/generated source analyzed. |
| Existing preparation host suite | Final 45/45 pass, 0.837 s | Exact command below; earlier independent root run also passed 45/45. |
| Swift iOS app and UI test source typecheck | Pass, exit 0; no warnings | arm64 iOS 15 target against installed iPhoneOS SDK/Flutter profile framework; source typecheck, not linked app or IPA. |
| Swift 5 host synthetic test runner | Seven checks pass | Includes `Int64.min/max` metadata/challenge/envelope boundary refusal without trap, proof mutation, shared vector, once/revoke race, ledger whitelist, certificate validity and launcher outcomes. |
| Kotlin source compiler and JVM runner | Three bootstrap/ledger/handshake sources compile; two PASS results | Existing Kotlin 2.0.21 and Corretto 17.0.8 caches; real cached Android JSON checks deep below-byte-cap inputs and valid escaped braces; JVM Base64 shim only. Activity/instrumentation compilation is excluded. |
| `python3 app/mobile/ios/ImageExperimentUITests/test_build_guard.py` | Final 10/10 pass, 0.030 s | Synthetic private guard paths, overflow/depth source contract checks; no signing/provisioning. |
| `ruby app/mobile/ios/ImageExperimentUITests/test_target_isolation.rb` | Pass | Actual Xcode source membership, target/configuration/scheme isolation. |
| `ruby -c app/mobile/ios/Podfile` and `xcodebuild -list` | Pass | Syntax/project enumeration only. |
| Private snapshot CocoaPods 1.16.2 `pod install --no-repo-update`, then `pod install --deployment --no-repo-update` | Both pass, exit 0 | Actual resolution/project generation; 5 pod versions, SPEC CHECKSUMS and other dependency sections unchanged. Generated lock and Manifest.lock matched; deployment kept the lock unchanged. Only validated lock copied back. Separate Swift version declarations preserve existing effective 5.0 values for CocoaPods target inspection. |
| Baseline byte comparison and generated exclusion | Pass for six protected files | Ordinary injection/router generated outputs, Library cache, Library release entry/harness and mobile lock are byte-identical to baseline; ordinary generated files contain no experiment registration/route. |
| `git diff --check` | Pass | Existing and new file whitespace checks were also run by owned host/native agents. |

Host suite command from `tools/image-optimization`:

```sh
UV_CACHE_DIR=/private/tmp/tellpal-native-entry-uv-cache \
  uv run --locked --offline --python 3.11 python -B -m unittest discover -s . -p 'test_native_entry_*.py'
```

Existing preparation suite command from the repository root:

```sh
python3 -m unittest discover -s tools/image-optimization -p test_native_pilot_setup.py
```

The Dart TLS test creates a temporary certificate/key outside Git with exact localhost SAN, serverAuth EKU and explicit digitalSignature/keyEncipherment key usage. Both ordinary trust/hostname validation and exact leaf DER pin finish before the HttpClient receives a socket and attaches a bearer. iOS SecurityContext accepts the single DER anchor; other platforms receive the PEM encoding of those same DER bytes. These passing macOS synthetic tests do not establish iOS/Android device TLS qualification.

Native compiler checks used the installed SDK/cache without upgrades. Swift app typecheck used `swiftc -typecheck -swift-version 5 -target arm64-apple-ios15.0`, installed iPhoneOS SDK, Flutter profile framework and the existing Runner bridging header over the four ImageExperiment Swift source files. UI test typecheck used the same target/SDK and installed XCTest framework over bootstrap, ledger and the test source. The host Swift runner compiled those three testable files plus a private runner outside Git and executed seven named checks. Kotlin used `org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17` with installed Android 36/Flutter profile/cached Kotlin classpaths, followed by its private JVM vector/outcome/deep-JSON runner. These are compiled source/synthetic checks, not APK/IPA build receipts.

CocoaPods commands ran in `/private/tmp/tellpal-native-pods-wjj198an/mobile/ios` with `CP_HOME_DIR` pointing to that snapshot's private CocoaPods directory and `COCOAPODS_DISABLE_STATS=true`; no dependency/SDK upgrade was performed. Private install/deployment logs and copy-back checks are retained there. The new lock SHA-256 is `d693f7c025b0f6f6217adce333b1fb2757107a4c1f7775f277531f3728a341d9`; its Podfile checksum is `69746b61f181e34caff6e0833096154385163957`. The existing ordinary Runner profile/custom-base-config CocoaPods warning remains. This proves pinned resolution/project generation, not full app linking/signing.

## Checks not run and remaining gates

| Check | Status / reason |
| --- | --- |
| Full Android Activity/instrumentation compile and Gradle DSL execution | Not run successfully; cached AndroidX LifecycleOwner was insufficient for a full Activity compile. The compiler pass above covers only the named sources. |
| Full Apple target app/test linking | Not run; pinned CocoaPods resolution/project generation passed, but actual linked app/test build remains unqualified. |
| Signed profile/release app/test APK or IPA | Not run; no private signing configuration/artifact receipt was supplied. |
| Installation, exact installed identity/certificate, debugger attachment observations | Not run; no authorized physical artifact/device binding. |
| iPhone 12 or Android Streaming broker/backend reachability and authenticated fixture | Not run. Latest read-only selected iPhone 12 detail observation confirms connected transport; an earlier CoreDevice list was stale, and historical disconnected receipts are preserved. ADB observed zero devices. The exact project identity and current principal Owner role were privately observed in the user-selected console window. Device Streaming monthly quota/usage was observed separately from automated testing quota; no real ID is tracked here. These observations supply no installed artifact, authentication or streaming reservation evidence. |
| Physical positive and negative pin/key/session/UID/fixture/teardown matrix, app-container ledger extraction | Not run. Source/synthetic passes never substitute for these observations. |
| Cache-state performance sampling, P95 or memory safety conclusion | Not run; unsupported owner/counter/frame evidence remains unavailable. |

Use `tools/image-optimization/NATIVE_ENTRY.md` for strict input schemas, exact build/verify commands, prerequisite receipts and rollback. A future physical run requires the exact signed app/test artifacts, selected connected device, accepted project/account/token, actual backend binding, token-free reachability and explicit installation/authentication authorization. Read-only signing discovery found zero valid configured signing identities. The driver does not create that authorization or install/reserve anything.

Latest private read-only console readiness shows Cloud Testing API's Device Streaming monthly Android Studio physical streaming Blaze limit of 4000 minutes with usage 0 (0%); a separate Spark row showed 30 with usage 0. These are configured quota observations, not a claim of 4000 free minutes. The exact free billing balance, reservation/session, installed artifact binding and authenticated fixture remain unqualified. No quota, IAM or billing setting was changed.

Rollback stops dispatch, revokes only the experiment session, closes owned host/native/Dart work and removes only exact experiment-owned namespace/build/app/test outputs after identity checks. Original inputs, other apps/media, the canonical QA account, backend behavior and historical/sprint trackers are preserved.
