# Small-JPEG Experiment Findings

Status: bounded local experiment completed on 2026-10-08. Full P0 and production profile selection remain open. No application, publication, storage or cache behavior changed.

## Frozen scope and evidence

The [frozen corpus](experiments/corpus.json) contains six small legacy covers matching retained Story 1.2 source-byte evidence and one additional local Display P3 diagnostic cover. These are already compressed JPEGs, not high-resolution upload masters. Their longest edge is at most 909 pixels. No reader-page source was available.

The unchanged current Java listing and page converters were compiled from source in an isolated private directory. Seven sources produced 63 candidate files: listing phone/tablet quality 75/80/85 and source-resolution quality 80/85/90. The latter uses the page converter with edge 960 as a **cover proxy**, not a new detail implementation or reader qualification. Detail 960/1440/1920 and page 1080/1440/1920 all resolve to source dimensions for this corpus; the larger aliases were not separately encoded.

[Raw results](experiments/small-jpeg-results.json) record exact source/output hashes, MIME, dimensions, bytes, ICC/EXIF metadata, source/harness/class/dependency hashes, runtime and ImageIO provider identities. Result SHA-256: `43a72a7dbf0b77d9dbdde84090c5e05123ca8db7cc2c95ffebe43419d8384d19`.

Validation passed for all 63 files after independent disk reread and Pillow decode. Original and copied source hashes remained unchanged; no image was upscaled. Two warmups and five shuffled measured calls per candidate produced 441 deterministic outputs. Existing-output refusal was exercised. Images, compiled classes and private paths remain outside Git in restricted local directories; public evidence contains relative identifiers and no credentials or signed URLs. [Reproduction instructions](experiments/README.md) accompany the evidence.

## File-size results

The six Story 1.2 controls total **269,651 bytes**. The table counts one output for each of those six fixed sources for each candidate. It is an offline corpus comparison, not an observed download journey, storage total or estimate of the production image distribution. Phone and tablet rows are alternatives and must not be added together as network savings.

Here, `change = 100 × (output_bytes − source_bytes) / source_bytes`; positive means growth. Raw JSON uses the opposite `saved_percent` convention and preserves every regression.

| Candidate | Six output files, bytes | Change from six sources | Files larger than source |
| --- | ---: | ---: | ---: |
| Listing phone q75 | 154,244 | −42.80% | 2/6 |
| Listing phone q80 | 176,191 | −34.66% | 2/6 |
| Listing phone q85, existing control | 207,255 | −23.14% | 2/6 |
| Listing tablet q75 | 281,350 | +4.34% | 5/6 |
| Listing tablet q80 | 315,703 | +17.08% | 5/6 |
| Listing tablet q85, existing control | 371,916 | +37.92% | 5/6 |
| Source resolution q80 | 340,969 | +26.45% | 5/6 |
| Source resolution q85 | 383,718 | +42.30% | 6/6 |
| Source resolution q90 | 438,346 | +62.56% | 6/6 |

Relative to the **same listing role's existing q85 control**, phone q80 reduces these six output byte totals by 14.99%; tablet q80 by 15.11%. Those are candidate-size comparisons only, not quality-approved changes to the current profiles. Lower quality also grows some already-small source files: `pt/story1.jpg` grows from 16,053 to 19,963 bytes for phone q80 and 29,640 bytes at source-resolution q85. Forcing a new JPEG encode does not guarantee optimization.

Keep the **174,427-byte Display P3 diagnostic** separate. Its source-resolution q85 output is 81,093 bytes. Including this one larger file changes tablet q85's aggregate from 37.92% growth in the six controls to 2.06% reduction across all seven, obscuring the small-source regressions. Its local-file provenance does not establish a Firebase source binding.

## Color, timing and visual boundaries

All generated JPEGs contain no ICC or EXIF metadata. Metadata removal alone does not establish whether decoded colors were managed. The separate [color diagnostic](experiments/color-diagnostic/README.md), using the current `setInput(input,true,true)/read(0)` sequence, observed sRGB decoded pixels for all seven samples; same-size `TYPE_INT_RGB` drawing preserved those pixels. For the P3 sample, Java pixels differed from an explicit Pillow 12.3.0/LittleCMS 2.19 P3→sRGB reference by mean absolute channel error 0.014528/255 and maximum 1. Unmanaged RGB differed by 5.478285/255 and maximum 24.

That color result is **sample-specific, before resizing and lossy encoding**. It does not approve wide-gamut preservation, arbitrary ICC/EXIF inputs or final JPEG appearance. Untagged images have no independently established original color space. Device quality approval and an explicit normalization policy remain necessary.

Timing covers the direct converter `process(...)` call with complete source bytes available. Constructor setup, source reads, hashing, independent validation and output-file writes are excluded; internal ImageIO cache I/O is included (`ImageIO.use_cache=true`). The report retains five raw observations and median/min/max per candidate. It is warmed local elapsed time with uncontrolled OS cache, not encoding-only CPU, cold-run, job, cloud, mobile or P95 evidence. The 512 MiB Java heap flag is configuration, not a measured peak-memory result.

A private self-contained viewer compares original or same-role q85 control with each candidate at equal candidate geometry, with zoom and corresponding crops. Its browser scale/DPR and uncontrolled color-reference limits are explicit. Source/role/quality selection and image loading were checked. The viewer supports human inspection; **human, native-device and production-profile approval remain false** in the evidence.

## Consequences for the plan

- Keep existing production profiles while evaluating alternatives. A uniform “encode every image as JPEG q85” rule is unsupported by these results.
- Resolve OQ-3 explicitly: evaluate verified reuse/pass-through of an already compliant encoded object, or a different qualified profile, without silent raw fallback or repeated derivative degradation. This is a proposal, not a policy decision.
- Obtain high-resolution masters and real text-dense pages before selecting detail/page resolution. Qualify alpha, EXIF rotation, further color cases and animation separately. The current no-upscale corpus cannot distinguish the proposed larger edge limits.
- WebP remains a conditional future comparison with a real packaged encoder, identical normalized reference pixels and supported client MIME handling; it was not measured here.
- Complete native phone/tablet legibility and transfer/paint/memory measurements, worker resource budgets and publication/readiness decisions before dependent implementation. P0 is not closed by this bounded slice.

The approved rollout remains new publications first, with existing content handled by a separate controlled transition. Notify the human before long implementation; this experiment grants no production deployment or backfill approval.
