# Reproduce the small-JPEG comparison

This folder contains local experimental tooling and redacted evidence, not application code or an accepted production profile. Original content images and generated image bytes are intentionally outside Git.

## Inputs and runtime

- A legacy TellPal `assets/images/landing` source directory containing the seven exact files/checksums in `corpus.json`. The runner refuses mismatched sources.
- Python with Pillow; observed Python 3.14.6/Pillow 12.3.0.
- JDK 25 `java` and `javac` on PATH; observed Homebrew OpenJDK 25.0.2.
- Existing local Maven `spring-beans` and `spring-context` 7.0.5 jars under the standard user Maven repository. They satisfy annotation imports during isolated current-source compilation; no Spring service is started or dependency downloaded.
- The current repository converter sources. Changed sources/runtime may change output bytes; compare recorded provenance before calling a replay equivalent.

From the repository root, choose a **new** private directory outside the repository and source tree and a new results filename:

```sh
python3 _bmad-output/specs/spec-publish-time-image-optimization/experiments/run-small-jpeg.py \
  --source-root /absolute/path/to/legacy-tellpal/assets/images/landing \
  --output-root /private/tmp/tellpal-image-p0-replay \
  --results /private/tmp/tellpal-image-p0-replay-results.json
```

Existing output/results paths are refused. The runner verifies originals, compiles unchanged converters plus `SmallJpegExperiment.java` into private classes, executes the frozen matrix and rereads outputs with Pillow. Files use 0600 and directories 0700. No backend, database, Firebase or mobile session is required.

The accepted local run produced `small-jpeg-results.json`; its private images are under `/private/tmp/tellpal-image-p0-20261008`. Raw TSV observations and manifests remain there. The public JSON includes relative image names and hashes, never private URLs or tokens. All 63 pairs, negative source deltas and same-role q85 controls are retained. The seven-sample aggregate is only descriptive: report the six matched controls separately from the larger P3 diagnostic to avoid selection bias.

## Private visual review

Create a new self-contained HTML filename from the verified files:

```sh
python3 _bmad-output/specs/spec-publish-time-image-optimization/experiments/render-comparison.py \
  --results /private/tmp/tellpal-image-p0-replay-results.json \
  --output-root /private/tmp/tellpal-image-p0-replay \
  --html /private/tmp/tellpal-image-p0-replay/viewer/comparison.html
```

The renderer checks file hashes and embeds image bytes locally. Keep the generated HTML private; it contains the artwork. Open it locally, or serve only its viewer directory on loopback for browser inspection. Normalized display/crops are browser review aids, not controlled color or native-device measurements. The separate [color diagnostic](color-diagnostic/README.md) records a no-resize/pre-encode managed reference comparison.

## Measurement limits

`transform_ms` measures the direct converter call with source bytes ready. It excludes constructor setup, original file reads, hashes, independent output verification and final file writes; internal ImageIO cache I/O remains inside the call. Two warmups precede five seeded/interleaved observations per candidate. No cold-cache, pure encoding, CPU, memory, cloud, publication or mobile benchmark is claimed. A small source cannot qualify a larger render resolution through upscaling. Human/native/profile approvals remain false.

See [findings and planning consequences](../p0-small-jpeg-findings.md) for byte accounting, cohort separation, unresolved small-source policy and remaining full-P0 gates.
