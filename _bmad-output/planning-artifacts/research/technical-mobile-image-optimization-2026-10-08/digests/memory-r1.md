# Image memory and physical-device validation digest

Research date: 2026-10-08. Scope: native Flutter phones and tablets, requested Android floor API 24 and iOS/iPadOS floor 15. These floors are task constraints, not a claim that any particular Flutter/toolchain version supports them. No project files were read. No application/device measurements were performed.

Research pass: two focused retrieval rounds, seven web calls, twelve selected primary documents from Android Developers, Apple Developer, and Flutter. Search results outside these publishers were excluded. A guessed UIKit URL failed and a WWDC image-session URL redirected to a catalogue; neither is evidence. Apple article text was available in the search retrieval even when opening the JavaScript documentation page yielded only its shell.

## Research rules

1. Never conclude from training data alone. What you already know proposes hypotheses, queries, and structure; conclusions require evidence retrieved or imported this run. A claim you cannot evidence is stated as an unverified belief or not at all.
2. The research firewall. Project context — briefs, PRDs, code, memory, persistent facts — shapes what to ask, never what is true. It is inadmissible as evidence: every claim in a research artifact traces to a digest or import file with a source. Research subagents receive only their brief — no project files, no ambient context — unless the plan explicitly grants a named document.

## Verified claims and provenance

`pub_date` means original publication date, not crawl time. Where an update date was actually retrieved, it is recorded separately. Confidence describes support for the bounded statement, not a guarantee about application behavior. All source metadata is repeated per claim so this table can be imported independently.

| ID | Bounded claim | sourceURL / sourceTitle | publisher | pub_date | accessed | confidence | class | Change flag |
|---|---|---|---|---|---|---|---|---|
| C01 | Flutter retains renderable images in uncompressed form. Its example estimates a 3840×2160 image at over 30 MB for 32 bits/pixel. `cacheWidth`/`cacheHeight` request a smaller decode/storage size. Widget layout dimensions and encoded file size therefore do not themselves establish decoded memory cost. | [Image class](https://api.flutter.dev/flutter/widgets/Image-class.html) | Flutter | unknown | 2026-10-08 | high | primary_documentation | Rolling API docs; verify pinned Flutter behavior. |
| C02 | DevTools distinguishes Dart heap, native/external memory (including decoded images), raster cache and process RSS. RSS includes resident stacks, heaps and shared libraries. The documented memory chart polls at 500 ms, so it is sampled evidence rather than proof that every brief peak was captured. | [Use the Memory view](https://docs.flutter.dev/tools/devtools/memory) | Flutter | unknown; last_updated 2026-07-31 | 2026-10-08 | high | primary_documentation | Page says its detailed guide is current for DevTools 2.23.0; footer reflects Flutter 3.47. Validate actual tool version and available fields. |
| C03 | `ImageCache.clear()` evicts pending/keep-alive cache entries but does not release images still held by live application listeners. Unfinished loads can complete and be inserted again. A cleared cache counter is therefore insufficient evidence of released process memory or stopped work. | [clear method — ImageCache](https://api.flutter.dev/flutter/painting/ImageCache/clear.html) | Flutter | unknown | 2026-10-08 | high | primary_documentation | Rolling API docs. |
| C04 | `precacheImage` finishes when the first image is available or loading fails; its Future does not complete with an error, so use `onError`. Cache retention can be refused. Listener pinning can retain oversized/many images and cause immediate OS termination, sometimes without another error. A completed preload is neither a paint measurement nor permanent retention. | [precacheImage function](https://api.flutter.dev/flutter/widgets/precacheImage.html) | Flutter | unknown | 2026-10-08 | high | primary_documentation | Rolling API docs; check cache key/configuration and pinned SDK implementation. |
| C05 | Flutter's painting binding handles an OS memory-pressure notification by calling its superclass handler and `imageCache.clear()`. This documents cleanup behavior, not guaranteed warning delivery, cancellation, or release of all live/native resources. | [handleMemoryPressure — PaintingBinding](https://api.flutter.dev/flutter/painting/PaintingBinding/handleMemoryPressure.html) | Flutter | unknown | 2026-10-08 | high | primary_documentation | Rolling API docs; verify the Android/iOS embedding delivers expected events in the deployed version. |
| C06 | Android `getMemoryClass()` (API 5) returns an approximate per-application memory class in MB. `isLowRamDevice()` (API 19) is device-configuration classification intended for feature adaptation; it is not a live remaining-memory measurement. `getProcessMemoryInfo` has own-UID and sampling restrictions from Android Q, and `getMemoryInfo` discourages polling. Both classification APIs are available at the requested API 24 floor. | [ActivityManager API reference](https://developer.android.com/reference/android/app/ActivityManager) | Android Developers / Google | unknown | 2026-10-08 | high | primary_documentation | Platform `isLowRamDevice()` and `getMemoryClass()` were not marked deprecated in retrieved reference. Do not confuse platform API with separately deprecated compatibility wrappers. |
| C07 | Android `onTrimMemory` supports background cleanup. `TRIM_MEMORY_UI_HIDDEN` and `TRIM_MEMORY_BACKGROUND` remain documented. `RUNNING_MODERATE`, `RUNNING_LOW`, `RUNNING_CRITICAL`, `MODERATE`, and `COMPLETE` are deprecated in API 35 and no longer delivered since API 34. Inherited `onLowMemory` is deprecated in API 35 and never called from API 34. Threshold comparisons are recommended over equality. | [ComponentCallbacks2 API reference](https://developer.android.com/reference/android/content/ComponentCallbacks2) | Android Developers / Google | unknown; last_updated 2026-09-24 | 2026-10-08 | high | primary_documentation | **Recent/API-sensitive:** old foreground-pressure examples are not a current cross-version warning contract. |
| C08 | `adb shell dumpsys meminfo [-d] package_name-or-pid` reports a process snapshot in KB. PSS apportions shared pages; private dirty and heap allocations describe different quantities. The example includes native/Dalvik and graphics-related rows (`Gfx dev`, `EGL mtrack`, `GL mtrack`); output varies by platform. | [dumpsys — meminfo](https://developer.android.com/tools/dumpsys) | Android Developers / Google | unknown | 2026-10-08 | high | primary_documentation | Version-dependent categories; do not assume every GPU allocation is observable in every device's output. |
| C09 | iOS/iPadOS can terminate apps under memory pressure and create jetsam reports that differ from crash reports and lack app thread backtraces. Inspect the actual terminated process and `reason`; `per-process-limit` and system page shortage describe different conditions. Convert `rpages` with that report's `pageSize`; retain hardware/OS and build identity. | [Identifying high-memory use with jetsam event reports](https://developer.apple.com/documentation/xcode/identifying-high-memory-use-with-jetsam-event-reports) | Apple Developer | unknown | 2026-10-08 | high | primary_documentation | Report fields/platforms can evolve; use the recorded page size, not a hard-coded value. |
| C10 | Apple documents memory warnings through the app delegate, active view controllers, notifications and dispatch memory-pressure events. Delivery is best effort: rapid allocation can cause jetsam before warning/cleanup. Respond promptly, reduce allocations, and avoid scanning the whole object graph to find releasable memory, which can increase pressure. | [Responding to low-memory warnings](https://developer.apple.com/documentation/xcode/responding-to-low-memory-warnings) | Apple Developer | unknown | 2026-10-08 | high | primary_documentation | Applicability of a chosen native bridge must be checked against the deployed OS/toolchain. |
| C11 | Apple requires actual-device release testing across supported hardware/OS combinations. The debugger prevents suspension and masks watchdog behavior; launch from the home screen for representative background behavior. Available memory varies with other apps. The guide recommends testing excessive/corrupt image input and explains that encoded images can expand substantially when decoded. | [Testing a release build](https://developer.apple.com/documentation/xcode/testing-a-release-build) | Apple Developer | unknown | 2026-10-08 | high | primary_documentation | OS/device availability and debugger/tool behavior require revalidation on the chosen release. |
| C12 | Flutter recommends physical Android/iOS devices and profile mode for performance investigation, including the slowest reasonable user device. Debug mode and simulators/emulators are not representative of release performance. Profile is close to release with instrumentation; the timeline permits frame-by-frame UI analysis. | [Flutter performance profiling](https://docs.flutter.dev/perf/ui-performance) | Flutter | unknown; last_updated 2026-07-31 | 2026-10-08 | high | primary_documentation | Rolling docs reflect Flutter 3.47; pin renderer/engine/tool versions in results. |

## Engineering interpretation, explicitly not measured findings

The following are proposals inferred from the verified claims. Their source metadata is the corresponding C-record above; confidence is medium and class is `engineering_inference` unless otherwise stated.

| ID | Proposal / interpretation | Evidence |
|---|---|---|
| I01 | Treat transfer/disk bytes, decoded pixel bytes, live image ownership, transient decode buffers, renderer resources and total process memory as distinct budgets. `width × height × 4` is an estimate for one 32-bit pixel surface, not a universal maximum or total app footprint. Do not assume a universal GPU-copy multiplier or that all allocations live in Dart heap. | C01, C02, C08, C11 |
| I02 | Bound **both** active operations and estimated decoded bytes in flight; a concurrency cap alone permits several huge images. Prioritize visible content, limit adjacent reader/list prefetch, discard obsolete queued work and release owners/listeners on route changes. Start experiments with one active decode on constrained devices, then compare two; these are experiment arms, not verified safe defaults. | C03, C04, C10 |
| I03 | Cache size/count configuration is not a total process-memory cap: active listener owners and in-flight completion need separate controls. Pressure cleanup should pause speculative admission, drop queued prefetch, release reconstructible offscreen owners and clear eligible caches. Verify the resulting process footprint and the load completion behavior. | C03, C05, C10 |
| I04 | Use `isLowRamDevice` and memory class as initial Android adaptation hints, then calibrate by physical device results. They are not a safe Flutter native/GPU ceiling. On modern Android, proactive limits and lifecycle cleanup are necessary even when old foreground trim callbacks do not arrive. | C06, C07 |
| I05 | Handle reported preload/image failures; cap retries, show a fallback, and test invalid/oversized/offline input. Process death cannot be recovered with `onError`. Log dimensions/counts without private URLs. | C04, C09, C11 |

## Reproducible validation protocol

This section is `class=methodological_proposal`, `confidence=medium`, with foundations C01–C12. Counts/timings below are chosen experiment parameters, not official safety thresholds. No latency target, safe-MB limit or crash-free result has been established.

### 1. Freeze the experiment

Record commit/build UUID, Flutter/engine/renderer and profiling-tool versions; device hardware, RAM, OS/API, screen size/DPR/refresh rate, orientation, Android classification/memory class; network bandwidth/latency/loss; thermal/battery state and concurrent-app pressure recipe. Keep dataset hashes, image encoded bytes, intrinsic dimensions, target physical decode dimensions, decode format assumptions, cache keys and policies. Include the largest valid image, long/tall assets, transparent assets, mixed aspect ratios and deliberately corrupt input.

Use actual constrained phones **and** tablets. Cover an executable API 24-floor Android combination, representative newer Android where API 34+ warning changes apply, and executable iOS/iPadOS 15-floor combinations, plus supported newer releases. If floor hardware is unavailable, record that gap explicitly; an emulator is not substitute proof. First establish the candidate Flutter/toolchain can run the requested floor; that compatibility question is outside this memory digest.

### 2. Define cache states independently

| State | Reset / precondition | Verify before counting the trial |
|---|---|---|
| Application-cold image | Stop the app; remove only the controlled test image's persisted application cache entry; relaunch. | New app process, no matching decoded image owner/key, encoded data must be fetched; record HTTP/server cache state. This does **not** prove a cold OS filesystem cache or CDN. |
| Disk-warm, memory-cold | Populate the encoded on-disk entry, then terminate and restart the app without deleting it. | Matching decoded object absent; disk entry hit; record any conditional HTTP request or revalidation. |
| Memory-warm | Resolve/display once; retain the intended cache state; repeat in the same process. | Matching key/size/configuration hit and decoded availability, not merely a disk hit. Record eviction between trials. |

Do not manufacture a memory-cold state merely by calling `ImageCache.clear` while live listeners still exist. Change one policy at a time and randomize baseline/candidate run order. Separate prefetch-enabled and prefetch-disabled experiments.

### 3. Request-to-first-painted-frame latency

Assign one request ID and a monotonic `t0` when the image consumer requests resolution; this works for memory, disk and network paths. Mark network/cache admission, encoded-byte availability, image stream success/failure, and the first **target image** paint candidate with request/frame identity. Join that candidate to the renderer frame trace and use completion of the first rasterized frame containing the intended pixels as `t1`. Report `t1 − t0` as **request-to-first-painted-frame**, with this operational definition and its uncertainty.

Confirm that the measured frame contains the actual target pixels through synchronized visual capture or a frame-aware test harness. An image-ready callback, preload Future, placeholder paint or UI-only post-frame marker must not be labeled visible-image completion. If the harness cannot reliably associate pixels with a rasterized frame, report image-ready latency separately and mark first-painted-frame latency unavailable. If measuring screen presentation instead, call that a separate presentation metric; raster completion alone is not a display timestamp.

Record first ready, first painted, error/timeout, bytes transferred, cache hit tier, UI/raster duration and dropped/slow frames. Start with 30 independent trials per selected device/cache/scenario/policy cell, report `n`, failures, p50/p95/max and raw traces; this sample count does not establish rare-event safety. Keep capture/instrumentation overhead visible and compare to an uninstrumented release smoke run.

### 4. Observe process memory and attribution

In a physical-device **profile** run, correlate Dart heap/external/native/raster-cache/RSS traces with operation markers and image ownership. On Android save `adb shell dumpsys meminfo <package>` snapshots at baseline, image-ready/transition, measured peak, back-navigation idle and background/resume. Preserve TOTAL PSS, private dirty, native/managed allocations and available graphics rows. Sampling may miss brief peaks; retain timestamps and event traces. Use targeted allocation investigation after locating the risky interval rather than assuming the Dart object graph captures all image memory.

On iOS collect Xcode/Instruments process memory evidence during diagnosis, then run the archived release from the home screen with the debugger detached for background/termination checks. Save process footprint/peak traces and any jetsam/device diagnostics, correlate build, hardware, OS, image request and scenario. Do not claim image causation from an unexplained process restart; inspect jetsam reason and terminated-process identity.

Keep RSS, PSS, heap, cache accounting and iOS footprint in separate columns. They are different measurements and must not be added as if disjoint. Mark unavailable graphics data as unavailable. Observe post-navigation/post-idle recovery and baseline drift across repeated cycles, not merely one peak.

### 5. Adversarial scenario script

Run the same scripted gestures, assets, delays and cycle count for baseline and candidate. A starting script is 20 cycles followed by a 60-second idle observation; scale duration to the known failure mode and record every change.

1. Rapidly fling through long, heterogeneous image lists; reverse direction before queued loads finish; switch sections.
2. Open the largest detail image while list requests remain active; back out before completion; immediately open a different detail.
3. Advance/reverse reader pages faster than prefetch/decode completes; change document; exercise the beginning/end of the bounded preload window.
4. Rotate phone/tablet repeatedly during request/decode and while detail/reader transitions overlap; verify the new target dimensions and obsolete work handling.
5. Alternate list/detail/reader/back navigation while memory and disk caches turn over; look for sustained live-owner growth.
6. Background, lock and resume the release app; repeat while a documented set of competing applications is opened. Record the resulting pressure conditions instead of claiming that the same competing app always creates the same pressure.
7. Exercise the cleanup path through an explicit test hook and separately observe genuine OS/lifecycle signals. A synthetic callback validates handler behavior; it does not recreate real RAM exhaustion or prove that the OS will deliver a warning in time.
8. Repeat with network slowdown, offline misses, corrupt input and oversized dimensions; check bounded retries, cancellation/discard policy, stable fallback and no stale image painted into a reused view.

### 6. Decision record

Require completion/failure counts and latency/memory distributions per device/OS/cache state, before/after peak and sustained footprint, drift after repeated cycles, observed signal/cleanup behavior, and collected termination diagnostics. A candidate is acceptable only against agreed device-specific latency and memory objectives, with no unexplained regressions in worst-case scenarios. Preserve failures rather than dropping them from percentiles. Report “no termination observed in N runs under these conditions,” not “crash-free.”

No retrieved source establishes a universal safe process/cache MB limit or a guarantee that warnings precede termination. The memory class, pixel-size estimate and cache cap serve different purposes. Physical-device measurements and release telemetry are still required to select concrete limits and verify improvement.

## Unresolved questions / required experiments

- Exact decoded surface format, intermediate allocations, renderer/GPU residency and release latency for the pinned Flutter version and image formats.
- Native signal propagation through the deployed Flutter embedding, particularly API 34+ Android, and cleanup ownership beyond the image cache.
- Hardware/OS inventory that actually runs the requested floors and represents constrained tablets as well as phones.
- Per-device concurrency/decoded-byte/cache budgets and latency acceptance criteria.
- Existing production memory-related termination frequency; no app telemetry or code was admitted as research evidence in this task.
