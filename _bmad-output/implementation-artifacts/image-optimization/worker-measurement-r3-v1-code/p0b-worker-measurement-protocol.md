# P0b.2 Proposed Local Worker Measurement Protocol

Status: proposed; Story 1.4 approval is required before image execution. Date: 2026-10-10.

## Scope and predecessors

This batch measures the existing canonical-reference worker path, not processing an original compressed source. It materializes full admitted pixels without read-time subsampling, projects RGBA to RGB, encodes the frozen settings, then fully rereads/decodes the output in another process. No normalization, resize, crop, upscale, source acquisition, dependency or production change is proposed. Original-source decode/normalization costs remain unmeasured; this limitation resolves the story's full-decode-control wording explicitly.

Preserve the r3 corpus/protocol/detached freeze and all v7 streams. Reverify the actual Story 1.3 `encoder-pilot-r3-v2` files with its unchanged verifier. Parent public result SHA-256 is `fd00e5ccfffc6a4a898284d80e2d98c699359cd337aa6aaf1d3d768560287f3a`; private scope SHA-256 is `89fa6fc8465d6cdd1d6446742046c59995c81aab584108337ceda2439ef1f7b1`. Parent commit is `0f21043f3c0bb4d251fc42668196ab22903d3398`. No old evidence or script is rewritten.

Use exactly the inherited 14 pairs/28 initial arms, separate JPEG q85 controls, frozen settings/order and initial/retry identities. A new exclusive private batch namespaces those identities; it does not alter `offline_order_context`. No warm-up, extra repeat, sizing or subsampling arm is added. Six historical small-JPEG controls and the separate P3 diagnostic retain historical identities but receive no new resource observations. Actual attempted/successful/failed/retry/output-verified/resource-valid/not-run counts are distinct.

## Environment and admission

Observed planning environment: macOS 27.0.1 arm64, Apple M2, eight physical/logical CPU cores and 17,179,869,184 bytes RAM. The executable remains installed Python 3.14.6/Pillow 12.3.0 matching provider discovery `c0139ede48cf1345a021874eb985efd325655d2ea8ca668f50eb5b49bff26e67`. Reobserve and bind OS/build/CPU/RAM/runtime/API availability before execution; mismatch prevents admission. No hostname, serial, credentials or private paths enter public evidence.

OS file-cache and background/thermal state are uncontrolled and reported accordingly. Fresh processes establish process-local baselines; they do not prove disk-cold input. The measurement environment is recorded separately from the inherited order context. No container, Railway or Java environment is inferred.

Create scope and planned ledger before any encode. Bind settled public inputs, detached freeze, preparation/reference set, actual predecessor scope/result/output verification, provider/environment, collector/worker/verifier code, this protocol, all limits, counter units and exact schedule. Parent preflight retains four bounded fixture rejection categories; fixtures do not enter resource or quality denominators. Rehash all 60 originals/streams and frozen files before/after, with selected inputs/runtime/code guarded between attempts. Integrity failure stops globally with retained remaining not-run rows.

## Measured phases

| Phase | Boundary and interpretation |
|---|---|
| Bootstrap/guards | Process spawn/import/request and integrity work; separate from active image phases. |
| Canonical reference read | Immediately before opening admitted `.pixels` → bounded read and input close. This is not compressed-source read/download. |
| Transform/projection | Read complete → canonical hash/header/opacity checks, RGBA-to-RGB projection and encode-ready Pillow image. |
| Encoding | Exclusive writer open → save, flush, fsync and output close; filesystem synchronization is included. |
| Output verification | Fresh process immediately before output reopen → unchanged full `inspect_output` assertions, including reference checks, JPEG tables and lossless RGB equality. |
| Cleanup | Close images, release raw/pixel buffers and perform one `gc.collect()` after active work. |
| Instrumented attempt wall | Durable started receipt → supervisor acceptance of completed readback; includes both process starts, guards, idle/settle and observer overhead. |
| Active pipeline wall | Sum of the four non-overlapping read, transform, encoding and verification durations; never substituted for attempt wall. |

Download, original compressed-source decode/normalization, upload, stored-object reread and production job-claim→ready-commit are `inapplicable_unexercised`, with null durations. Output reread is not stored-cloud-object reread. No human/native quality verdict is produced.

Each arm uses an encoder child followed by a fresh verification child, with one active measured child at a time. Both have a five-second live idle baseline after bootstrap/guards, then active work, cleanup, and thirty seconds alive without active image work before settled capture and exit. No repeated GC or image operations occur in that settle window. Idle RSS is the supervisor's endpoint capture at or after five seconds from idle-start; settled RSS is its endpoint capture at or after thirty seconds from cleanup-end. Both endpoints precede the next phase/exit, remain on the same live PID, and retain actual capture delay. The verifier derives endpoint selection from raw records, never a producer's summarized value or zero after exit. These windows alone total about 32.7 minutes across a successful 28-arm batch; actual work/guards add time. A child deadline includes its idle/settle windows and is not equivalent to the previous pilot's encoding deadline.

## Counters, clocks and coverage

Use `time.perf_counter_ns()` monotonic nanoseconds, recording its implementation/resolution. Admission verifies supervisor and child clocks share the same system-wide monotonic domain; different origins or clock changes invalidate metrics. Child phase CPU deltas come from `resource.getrusage(RUSAGE_SELF)` user/system seconds. A single supervisor reaps each exact PID with `os.wait4`, recording exit/signal, lifetime user/system CPU seconds and `ru_maxrss` bytes. Do not let `Popen.poll` or `communicate` consume terminal rusage first. Descendant/guard costs are not relabelled encoding-only CPU.

External supervisor sampling uses macOS `proc_pidinfo(pid, PROC_PIDTASKINFO=4)` via stdlib `ctypes` at 1 Hz. The arm64 `proc_taskinfo` ABI is 96 bytes: six uint64 fields followed by twelve int32 fields. `pti_resident_size` is bytes; `pti_threadnum` counts all process OS threads. Wrong return size, missing PID or API error yields an explicit error/null, never zero-filled measurement. Encoder-specific thread counts/configuration remain unavailable; OS task threads do not establish codec parallelism.

Record phase-boundary RSS/thread snapshots and externally sampled PID/timestamps. A snapshot taken after a phase event retains its actual capture timestamp/delay and is not presented as an exact boundary value. Keep idle/settled endpoints, sampled phase/lifetime peaks and kernel lifetime high-water separately. Sampled peaks are observed lower bounds; 1 Hz can miss short spikes. The kernel high-water includes bootstrap/cleanup and cannot establish an encoding-only peak. Negative settled-minus-idle deltas are valid; negative durations/byte counts are invalid. Never add overlapping memory counters.

Retain planned/actual sample timestamps, lateness, counts, failures, maximum gap and phase coverage, including short phases with no periodic sample. A delayed tick is visible; it is not silently filled or converted into a zero peak. A resource-valid successful arm requires all required idle/settled/terminal observations and stage boundaries from both children. Expected spawn non-start, early failure or timeout instead retains partial telemetry and explicit null/unavailable reasons, backed by finalized supervisor spawn/exit/signal/deadline records; the ordinary output retry remains available. These failed observations are excluded from successful resource aggregates. Unexpected missing/malformed required evidence or an unverifiable lifecycle stops the batch as instrumentation failure. A final event line interrupted by verified termination may be retained as explicitly incomplete, never parsed as a valid event; malformed complete records, replay or wrong ownership still stop. Optional unsupported counters do not stop. Short-stage or delayed-tick limitations remain disclosed even when required records are complete.

Python total heap, separate native allocator/PSS/private-dirty/graphics memory are unavailable/null. Java heap and container counters are inapplicable/null. Do not enable tracemalloc or introduce a profiler. Record actual sampling-call/boundary cost and collector CPU/wall overhead; workload perturbation is unavailable and no overhead subtraction or percentage speed claim is made.

Primary local unit/ABI evidence is the installed SDK's `libproc.h:96`, `sys/proc_info.h:124–143,726–727`, `getrusage.2:94–95` and `sys/stat.h:167–168,303`. Retain their identities/hashes privately. `ps` RSS uses KiB and is not a fallback in this protocol. Counter provenance includes OS/API/code identity; do not invent an inaccessible dyld-shared-cache library hash.

## Isolation, disk and finite failures

All output, TMPDIR, raw events/samples/logs and receipts live in a new private batch outside Git. Reject raw parent segments, symlinks, existing batch/file paths and overlap in either direction with any input or old batch. No automatic deletion, old-directory chmod or overwrite is allowed. Use exclusive writes; finalize/fsync and make receipts/raw files read-only on completion or termination. Started/request records precede spawn; partial history survives timeout or malformed evidence.

Measure logical file bytes and allocated bytes (`st_blocks × 512`) separately. Deduplicate hardlinks by device/inode. Record output/partial bytes, attempt TMPDIR, logs/receipts/telemetry and retained batch footprint separately, with 1 Hz/boundary/final disk observations. Sampled temporary peaks are lower bounds; retained cumulative disk is not active-temp peak. No nested or overlapping categories are summed twice.

Limits: 28 initial arms, maximum 56 attempts, one distinct retry for encode/readback/spawn failure, 64 MiB per output, 120 seconds per measured child including idle/settle, 300 seconds per preflight/integrity call, 5,400 seconds global batch, and 4 GiB retained batch ceiling. Bound each child stdout/stderr to 1 MiB, each raw telemetry stream to 2 MiB, with at most 256 samples and 64 phase events per child. Bounded drains prevent pipe deadlock. Overflow is instrumentation failure, not truncated success.

Spawn owned process groups. On timeout send TERM, allow two seconds, then KILL if needed and always reap; retain signal/exit/grace, partial output and raw samples. Integrity, required-instrumentation, global-deadline, disk-cap or cancellation failure stops the batch; it never triggers an output retry. Ordinary encode/readback exhaustion marks that arm failed and continues remaining schedule. An attempt already executed is retained even when receipts, measurements or outputs fail later verification.

## Independent verification and handoff

The final verifier imports neither collector nor any encoder. It reopens outputs with the unchanged codec inspector, validates exact schedule/request/scope/code/runtime/output bindings, and derives measurements/counts from bounded raw records rather than producer summaries. Validate strict finite types/units, PID/attempt ownership, timestamps/order/lifecycle, phase windows, complete stage logs, terminal rusage and the global stop rule. Output validity and resource validity remain separate. Measurement verification establishes recorded evidence consistency and output rereading, not decoder diversity or an independent physical recapture of past RSS.

New output hashes must match their verified predecessor counterparts; mismatch is retained as output-verification failure with the same one-retry policy. Never borrow old bytes to fill a missing new output. Only two verified actual compatible values and positive named denominators permit raw byte deltas/ratios. Preserve growth, rejection, missing outputs and source shortages. Original-source bytes and same-geometry JPEG q85 controls are distinct denominators; totals are storage, not journey traffic.

Publish `p0b-comparison.json` and `p0b-worker-findings.md` with per-arm observations, valid counts, limitations and evidence-backed OQ-2/3/4 questions/proposals. Visual/reader/native verdicts remain pending; quality-qualified savings stay null. Reader/DPR, real alpha/EXIF and OQ-2/3/4/6 remain open. One observation per arm and descriptive summaries do not establish P95, leak/device safety, production budgets or codec/profile/reuse acceptance. Keep synthetic metric tests separate from actual batch evidence.
