# P0b.1 Real Encoder Pilot

Batch: `encoder-pilot-r3-v2`, executed 2026-10-10. **Independent verification: PASS**, zero diagnostics. [Machine evidence](p0b-encoder-pilot.json) retains all fourteen pairs, twenty-eight separately executed arms, exact settings, output hashes/bytes and attempt identities.

## Qualification boundary

The installed local macOS arm64 Python 3.14.6 / Pillow 12.3.0 runtime produced real JPEG, WebP lossy, WebP lossless and PNG bytes from the two admitted v7 references. Geometry and purpose remain source-resolution codec feasibility: 1712×2778 and 1284×2279, without repeating normalization, resizing, cropping or upscaling. The fourteen JPEG q85 controls were encoded separately; identical control hashes do not represent observation reuse.

This qualification covers this installed offline runtime. The verifier runs in separate processes and reopens closed output files using the same pinned Pillow decoder; this is independent rereading, with no decoder-diversity claim. Java/Railway packaging, mobile decoding, human/native quality, worker resources and production profiles remain unqualified. Reader/DPR, real alpha, EXIF rotation/mirroring and OQ-2/3/4/6 remain open. Visual/reader/native verdicts are pending and quality-qualified savings are null.

## Executed evidence

| Observation | Actual result |
|---|---|
| Frozen pairs / initial arms | 14 / 28 |
| Actual attempts / verified arms | 28 / 28 |
| Failed attempts / retries / not-run arms | 0 / 0 / 0 |
| Actual JPEG / WebP / PNG files | 18 / 8 / 2 |
| Lossless RGB round trips | 2 PNG and 2 WebP lossless candidates exactly match admitted RGB pixels |
| Frozen rejection fixtures | 4/4 through common bounded intake |
| Source/reference preservation | All 60 originals, 60 streams and 4 fixtures rehashed unchanged |

The fixture categories are `decode_truncated`, `declared_actual_mime_mismatch`, `source_byte_limit` and `source_pixel_limit`. Excessive bytes are refused before reading; excessive pixels before raster loading. Synthetic tests and fixtures do not qualify excluded real image classes.

## Actual candidate bytes

Each cell is the size of a verified actual file, without a visual-quality verdict or savings claim. The positive same-geometry JPEG q85 control is distinct from each original-source denominator.

| Encoder setting | Content 71 page 1 | Content 19 page 1 |
|---|---:|---:|
| Original source denominator | 1,122,418 | 847,415 |
| Separate normalized JPEG q85 control | 669,608 | 508,346 |
| JPEG q80 | 553,639 | 427,585 |
| JPEG q90 | 907,837 | 692,371 |
| WebP lossy q80 | 217,122 | 176,204 |
| WebP lossy q85 | 294,450 | 244,442 |
| WebP lossy q90 | 439,634 | 373,532 |
| WebP lossless | 2,801,580 | 1,934,590 |
| Opaque PNG | 4,374,954 | 2,969,536 |

Larger outputs are retained rather than excluded. Equal numeric quality is not equal visual quality. WebP lossless keeps the frozen omitted quality setting; installed effective default 80 describes effort. JPEG q80/q85/q90 quantization tables were independently checked in all eighteen JPEG files. JPEG uses subsampling 0, nonprogressive encoding and no optimization; all output formats use normalized sRGB RGB with omitted ICC/EXIF/XMP, exactly as preregistered.

## Integrity and verification

Provider discovery SHA-256: `c0139ede48cf1345a021874eb985efd325655d2ea8ca668f50eb5b49bff26e67`; the [unchanged corpus](p0a-corpus.json) records all twelve matching binary identities, JPEG 6.2, WebP 1.6.0 and zlib 1.3.1.zlib-ng.

Private immutable scope SHA-256: `89fa6fc8465d6cdd1d6446742046c59995c81aab584108337ceda2439ef1f7b1`. Independent public result SHA-256: `fd00e5ccfffc6a4a898284d80e2d98c699359cd337aa6aaf1d3d768560287f3a`. Parent detached freeze remains `freeze-p0a-source-resolution-2026-10-10-r3-v1`, SHA-256 `9c4933e4a53fb42a9eacba7ed6fcec603ddb5aae2719cb39bfc50fdc9a4ad52b`.

**64 synthetic regression tests passed in 3.721 seconds**; none skipped. Stable suite log SHA-256: `a401bcac3305c646f259aafc3754c6ad9d0d900750dc83f637706f103a31be97`. A tiny synthetic actual-worker test exercised all frozen save settings through the LimitedWriter/Pillow path before the real batch; its bytes are separate from qualification evidence. The unchanged corpus CLI and Story 1.1 inventory verifier both passed. Syntax, whitespace, local references and recursive privacy checks cover new/untracked deliverables.

Source bytes, output images, source paths and raw logs are retained outside the repository. Scope and attempt receipts are created exclusively before work; failed/partial output isolation, one retry, deadline and no-overwrite behavior have executed regression coverage. Operational watchdogs are 120 seconds per encode/readback, 64 MiB per output and 300 seconds for preflight, at concurrency one; they establish no production budget.

## Reproduction and next gate

Use [the runner](run-p0b-encoder-pilot.py) with private `--source-root`, `--reference-root`, `--preparation-receipt`, `--freeze-manifest` and a new private `--output-root`. Invoke [the separate verifier](verify-p0b-encoder-pilot.py) with the same inputs and `--batch-root`. Existing batch paths are refused; changed runtime/settings need their own authorized revision. [Tests](test-p0b-encoder-pilot.py) use temporary synthetic inputs and run separately from actual/privacy verification.

Story 1.4 may use these exact identities to compare the frozen candidates and actual worker resource costs. This report selects no production codec, image budget or native quality threshold.

## Review closure

Three fresh parallel reviewers identified ten actionable patch findings. All were fixed and regression-tested before this v2 execution; no intent/spec loopback or deferred work was needed. The v1 batch and its public evidence/diff snapshot remain private historical evidence. All 28 v2 encoded-file hashes match their corresponding v1 hashes; the v2 scope/result bind the reviewed code and stronger observations.

| Review finding | Severity / route | Closure |
|---|---|---|
| Output/log/receipt reread failure erased executed attempts | Medium / patch | Preserve sanitized execution history and actual counts separately from output qualification. |
| Raw parent segments bypassed symlink checks | High / patch | Reject parent segments before path normalization. |
| Direct encoder entry lacked private-root/overlap guards | High / patch | Apply private, exclusive and input-overlap guards before writing bytes. |
| Malformed corpus/schedule left an unrecorded partial batch | Medium / patch | Validate the exact finite schedule before batch creation. |
| Subprocess spawn failure escaped retry handling | Medium / patch | Retain sanitized immutable logs and use the normal finite retry policy. |
| Parent runtime integrity discovery lacked a deadline | Medium / patch | Run parent checks in bounded subprocesses; integrity failure stops the batch. |
| Pillow decompression bomb had the wrong rejection category | Medium / patch | Reject as `source_pixel_limit` before raster loading. |
| Missing success/failure stage logs could qualify an arm | High / patch | Require complete stage log pairs and matching hashes. |
| Attempts could follow a terminal integrity failure | High / patch | Enforce the global stop rule independently across the ledger. |
| Actual JPEG quality settings had no byte-level assertion | High / patch | Independently validate IJG DQT tables and literal q80/q85/q90 worker-test expectations. |
