# Frozen-source regression checks

These offline checks execute existing mobile code without editing any mobile source,
building an application, using devices, or downloading dependencies. Missing cached
prerequisites fail the check; neither runner skips cases.

From the repository root, execute the actual Android memory object:

```sh
tools/image-optimization/.venv/bin/python -B \
  tools/image-optimization/native-tests/run_android_memory_test.py
```

The runner extracts the unchanged `ImageExperimentMemory` object directly from
the production Kotlin file and compiles that exact slice with configurable
`android.os.Debug` and `SystemClock` stubs. Its assertions call the real `sample()`
for positive, zero, maximum signed integer, negative PSS and a throwing OS reader.
It uses cached Kotlin 2.0.21 compiler dependencies and an installed JetBrains Java
runtime, writes compiler outputs only to a temporary directory, and prints the
production source SHA. The host `test_native_android_memory.py` suite also executes
this runner, including compilation and all five cases.

From `app/mobile`, use the existing mobile package configuration for the external
Flutter regression:

```sh
flutter test --no-pub --no-test-assets --reporter expanded \
  ../../tools/image-optimization/native-tests/require_absent_keys_test.dart
```

For both tests and static analysis, run the following from the repository root:

```sh
tools/image-optimization/.venv/bin/python -B \
  tools/image-optimization/native-tests/run_flutter_checks.py
```

This runner temporarily rebases the existing mobile package configuration into
the external test directory so the analyzer can resolve packages there, then
removes it in `finally`. It runs no package installation and modifies no mobile
source or configuration. `flutter analyze` alone cannot resolve this external
file using the mobile directory's package configuration.

The seven tests execute the real `requireAbsentKeys`. Only disk lookups use an
in-memory substitute; pending, live and keepAlive are distinct real Flutter
`imageCache` states. Every present key refuses without an absence record, the
absent case records exactly one bound event, and the real cold target probe
refuses a present immutable key before resolving, decoding or drawing its provider.

Read-only comparison replay now requires a separate trusted `expected_provider`
keyword to `native_artwork_comparison.qualify`. Its exact keys are
`expectedProviderSha256`, `expectedAttemptSha256`, `expectedDecodeWidth`, and
`expectedDecodeHeight`. For new runs, derive it with
`provider_binding(admitted_manifest, issued_envelope, case_id)`; `run` retains it
separately as `expected-provider.json`. The qualify CLI accepts this file through
`--expected-provider` and requires a fresh output directory. Existing run bindings
and result schemas remain unchanged.

For archived runs lacking the original issued envelope, derive expected dimensions
independently from the frozen admitted manifest geometry and use provider/attempt
hashes from the original immutable run binding. Record this provenance in the
separate replay evidence. Never derive admission dimensions or expected hashes
from the ledger being qualified. Calling `qualify` directly returns an unchanged
result dictionary and writes no evidence, allowing byte-preserving comparison
with an original result receipt.
