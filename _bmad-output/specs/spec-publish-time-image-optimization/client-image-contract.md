# Client Image Selection and Residency

Status: research-informed draft, 2026-10-08. These requirements refine CAP-2/CAP-4; numerical profiles, residency budgets and native acceptance remain open. Evidence and version limits: [research sections 2–5](../../planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/research.md). Follow ADR-0018 and parent AD-8/AD-10/AD-12; this companion adopts no new Flutter architecture or runtime configuration.

## Fitted physical demand

For each role record the actual logical image slot, DPR, fit/alignment, orientation-normalized source dimensions, supported zoom and embedded-text safe region. Preserve inherited `BoxFit.cover` framing until quality review agrees a change. A thumbnail bounding box is a maximum; inspect actual manifest dimensions.

For positive intrinsic dimensions `Iw, Ih`, logical slot `Lw, Lh` and DPR `d`, keep required full-image demand separate from the bounded decode target:

```text
Pw = ceil(Lw * d); Ph = ceil(Lh * d)
s_contain = min(Pw / Iw, Ph / Ih)
s_cover = max(Pw / Iw, Ph / Ih)
required_scale = selected_contain_or_cover_scale
Rw = ceil(Iw * required_scale); Rh = ceil(Ih * required_scale)
source_shortage = required_scale > 1
decode_scale = min(1, required_scale)
Dw = min(Iw, ceil(Iw * decode_scale))
Dh = min(Ih, ceil(Ih * decode_scale))
```

This is geometric demand, not a sharpness or memory guarantee. `Rw, Rh` remain unclamped for rendition qualification; only decoding is capped at source resolution. A 100×100 source in a 200×200 physical slot remains a shortage, even though its bounded decode is 100×100. `cover` crops for presentation; decoding the full source may require more than the slot on one axis. `ResizeImagePolicy.fit` expresses containment and cannot substitute for coverage. Two unrelated `exact` decode dimensions can distort aspect ratio. Integer rounding does not supply missing source detail.

Choose the smallest quality-qualified available rendition whose observed dimensions cover `Rw, Rh`, not the clamped decode dimensions. Derive bounded aspect-preserving decode targets from that rendition, respecting source resolution and measured resource limits. If `source_shortage` is true or no candidate qualifies, expose source/profile/resource shortage and the agreed recovery state; do not silently lower demand, upscale, switch framing or use an original to report success.

Recompute for rotation, split/window changes and layout changes. Qualify a small variant ladder and target-size buckets/hysteresis to avoid fragmentation and repeated downloads for tiny size changes. Exact bucket thresholds and role-specific limits are OQ-2 decisions. Listing, detail, future reader, alpha/static listening and animation require their own qualification; first-slice success does not certify all roles.

## Cache and request identity

| Layer | Required identity and behavior |
| --- | --- |
| Encoded disk object | Immutable rendition/revision/profile plus language, format and account/access scope where applicable; one key never denotes different bytes or admission scopes. Integrity verification precedes promotion. |
| Decoded provider | Encoded content identity plus actual geometry/resize policy and relevant provider configuration. Precache/display must resolve the same final bounded provider/target. |
| Transfer attempt | Separate request/attempt and account/session generation; renewed signed URLs address the same admitted immutable rendition without redefining persistent content identity. Obsolete completions cannot promote or mutate current state. |

Preserve owned-transfer timeouts, retries, cancellation, corruption handling, revision checks and account-switch teardown. A stable key does not bypass server admission or renewal policy. Cache clearing/widget disposal alone does not prove HTTP cancellation; verify the existing custom transport path. Client disk resizing still requires the first encoded transfer and may allocate an unbounded intermediate; it is not a substitute for a server rendition or proof of low peak memory.

## Residency and scheduling

Keep encoded disk, decoded LRU, live/pending owners and total process memory separate. The current 48-entry/32 MiB LRU remains a control until evidence justifies changing it. `W * H * 4` estimates one assumed 32-bit SDR pixel plane; it excludes intermediates, encoded buffers, renderer resources and other app allocations. Smaller final dimensions do not establish proportional transient-peak savings.

Prioritize visible artwork; jointly bound active transfers, active decodes and estimated decoded bytes in flight. Evaluate one versus two active decodes and reader current-only versus current-plus-one-next as experimental arms, not universal safe defaults. Retaining adjacent encoded files need not pin decoded frames. Final budgets and preload windows remain OQ-2.

Discard obsolete speculative queues on navigation, account/session change and pressure; release offscreen listeners/owners while retaining media required by the admitted active session. Account teardown remains composition-owned. A smaller LRU may increase redecoding while images are still live; measure journey churn and return-to-idle drift.

`precacheImage` completion establishes neither successful loading, retention nor first paint. Observe failures explicitly, verify matching provider identity/retention and release ownership; successful listener removal occurs after the frame, while error removal is immediate. Do not rely on automatic pending-load reinsertion after clearing: retrieved Flutter prose and implementation differ. Never infer transport cancellation from cache bookkeeping.

Pressure handlers provide cleanup opportunities. Apple warning delivery is best effort; older Android foreground trim levels stopped arriving from API 34 and are deprecated at API 35. Prevention must rely on proactive admission/residency bounds rather than warning delivery. A synthetic hook proves handler behavior only, and an error widget cannot recover a terminated process.

## Protected delivery policy

Resolve response `Cache-Control` alongside access, renewal and retained revisions in OQ-5. HTTP `private` restricts shared-cache storage, `no-cache` requires validation before reuse, and `no-store` prohibits storage/reuse; these are different policies. Preserve admitted local caching only where the chosen response/access contract permits it. Neither cache directives nor a cached file replace authorization.

Signed URLs alone do not establish Cloud Storage built-in/public cache eligibility. Response metadata must describe actual objects, while ETag/Content-Type cannot replace SHA/MIME/dimension/decode verification. No CDN is required by this work.

## Acceptance

Use the physical-device protocol in `rendition-evaluation.md`. Cover tall/wide sources, embedded titles, high DPR, orientation/window changes and insufficient sources. Script rapid list reversal, detail/back overlap, future page turns, URL renewal, corruption/oversize, offline misses, cache turnover, account switch and background/resume. Record actual selected IDs, decode geometry, requests, failures, owners, peak/settled memory and visible-frame latency for identical control/candidate journeys. Missing instrumentation remains explicitly unavailable; a finite passing matrix does not prove every old device crash-free.
