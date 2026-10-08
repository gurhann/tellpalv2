# Pre-Encode Color Diagnostic

This isolated seven-cover diagnostic observes the current Java JPEG reader and unchanged-size `TYPE_INT_RGB` drawing. It compares the resulting sRGB pixel values with Pillow/LittleCMS references built from each embedded ICC profile. It does not resize or JPEG-encode candidates, approve visual quality, demonstrate preserved Display P3 gamut, or qualify general ICC, PNG, orientation, alpha or animation behavior.

The shared frozen corpus is [../corpus.json](../corpus.json). Source images and raw RGB dumps stay private. The retained [color-comparison.json](color-comparison.json) contains relative source names, checksums, runtime/reader/provider observations and channel-difference metrics, without access URLs or private absolute paths.

## Reproduce

Requirements: Java/Javac and Python with Pillow and LittleCMS support. Run from this directory, supplying the existing legacy `assets/images/landing` source root and a private output directory outside the repository:

```sh
python3 run_color_diagnostic.py \
  --source-root "$P0_SOURCE_ROOT" \
  --output-root "$P0_PRIVATE_COLOR_OUTPUT"
```

Use a fresh output directory for every run. The runner refuses existing output paths and output beneath the source root or repository; the Java probe independently refuses existing pixel output roots and source/repository descendants. Raw RGB and JSON writes use create-new semantics. The runner verifies every source SHA against the corpus before compiling [ColorProbe.java](ColorProbe.java) into the private output directory, then verifies every original SHA and the corpus again before writing `color-comparison.json`. The report records before/after source hashes. Different runtime versions may legitimately change decoding or rounding; record and compare those identities before interpreting differences. The script's source and compiled class hashes are retained in each report.

[refusal-verification.json](refusal-verification.json) records the fresh seven-source replay, explicit existing-output/source-descendant/repository-descendant refusals and direct Java pixel-root refusal. The prior private report and all existing diagnostic output hashes stayed unchanged after refusal; all seven source hashes matched before and after the successful replay.

The observed Java method uses the current converter's `reader.setInput(input, true, true)` and `reader.read(0)` flags. `getRGB` is an sRGB observation; the report additionally records the actual decoded image's color space, buffered-image type, ICC hash and reader/provider identities. The source profile is not inferred from file extension.

Pillow raw RGB is deliberately included as an unmanaged contrast, not as the approved reference for an ICC-tagged input. Managed references use explicit relative-colorimetric and perceptual ICC-to-sRGB transforms at the same dimensions. Metrics describe 8-bit channel differences and carry no appearance threshold. All selected sources lack an EXIF orientation tag, so this run proves no rotation handling.
