# Story 1.1 Local Acceptance Harness

These tools are limited to the approved isolated local Library fixture. They refuse a database other than `tellpal_library` on local port 5442; HTTP is fixed to loopback backend 8082. They never authorize production mutation. Original sources must exist in each declared TR/EN/PT language. Publication/processing eligibility is administered directly in the disposable database; the fixture does not claim complete protected-story playback.

## Real fixture and refreshed HTTP baseline

Prepare the private runtime JSON with `admin_username`, `admin_password`, `db_user`, `db_name`, `db_password`, and a separate real verified Firebase ID token file outside the repository. Start the local backend with real Firebase storage, stub auth disabled, `local-library-1-1` storage prefix (without leading/trailing slashes) and the server UID allowlist. Never print credential/token files or add them to build defines.

```sh
python3 tools/library-1-1/fixture.py \
  --runtime /private/tmp/tellpal-library-runtime/runtime.json \
  --token /private/tmp/tellpal-library-runtime/firebase-id-token.txt \
  --artwork /path/to/existing/tellpal/assets/images/landing \
  --output _bmad-output/implementation-artifacts/mobile/library-1-1-http-evidence.json \
  --private-output /private/tmp/tellpal-library-runtime/catalogs
```

The initial seed refuses existing categories or unrelated content. After CMS saves, run the same command with `--verify-only` to refresh actual ordered summaries and real downloaded JPEG evidence. Repository output omits signed URLs/expiry and identity; private catalog files contain signed URLs and use restrictive permissions.

## Actual CMS reorder/conflict recovery

From `cms/`, run against the isolated backend. The test signs in as the local admin, reads the private real Firebase token and keeps traces/videos off. It restores the fixture's initial TR order, creates a genuine stale-write conflict, verifies draft retention/reload, atomically exchanges occupied positions and verifies final TR [3,2,1,4], unchanged EN [1,2,4,3], and unchanged cards.

```sh
LIBRARY_RUNTIME_FILE=/private/tmp/tellpal-library-runtime/runtime.json \
LIBRARY_ID_TOKEN_FILE=/private/tmp/tellpal-library-runtime/firebase-id-token.txt \
VITE_API_BASE_URL=http://127.0.0.1:8082 \
npx playwright test e2e/library-real.spec.ts --workers=1
```

Refresh HTTP evidence afterward. Redacted `library-1-1-cms-real.json` and a main-content screenshot are produced. The test is skipped when private runtime paths are absent.

## Native integration matrix

From `app/mobile/`, select an already prepared emulator/simulator. The host-side Flutter driver injects the private token directly into a development-only integration binding in memory; no token asset/file/build define is created in the app. Communication logging is disabled.

```sh
LIBRARY_ID_TOKEN_FILE=/private/tmp/tellpal-library-runtime/firebase-id-token.txt \
LIBRARY_BASE_URL=http://10.0.2.2:8082 \
LIBRARY_NATIVE_EVIDENCE_DIR=/private/tmp/tellpal-library-runtime/android-phone-library \
flutter drive --target integration_test/library_live_test.dart \
  --driver test_driver/library_live_driver.dart -d emulator-5554
```

Use `http://127.0.0.1:8082` for iOS Simulator. Evidence records the actual device dimensions/platform, TR/EN/PT at scale 1.0/1.3, ordered preview/cover identities and server locks, decoded-cache observations after repeated scrolling, disabled tab taps, Library reselection and root back. Six screenshots and one redacted JSON are written to the specified directory. The matrix resolves the server audience before asserting: non-subscriber TR rows `[3,2,1,4]` and EN/PT `[1,2,4,3]`; subscriber TR `[3,2,1]` and EN/PT `[1,2,3]`, all unlocked and without the premium banner. Both actual audience runs are recorded. Unlisted identity is checked separately through HTTP; these controlled contexts do not establish production authorization.

## Interactive development entry

Run `lib/main_library_dev.dart`; it waits for ephemeral VM configuration. The VM client requires Node.js 22 or newer and uses its built-in WebSocket, with no CMS dependency. It reads a private Flutter-run log and token file rather than exposing them in the shell command.

```sh
node tools/library-1-1/vm.mjs --vm-log /private/tmp/private-flutter-run.log \
  --mode configure --base-url http://10.0.2.2:8082 \
  --token /private/tmp/tellpal-library-runtime/firebase-id-token.txt --language tr
node tools/library-1-1/vm.mjs --vm-log /private/tmp/private-flutter-run.log \
  --mode context --language pt --scale 1.3
node tools/library-1-1/vm.mjs --vm-log /private/tmp/private-flutter-run.log \
  --mode observe --output /private/tmp/redacted-library-observation.json
```

`main.dart` and its existing PoC tests remain independent. This development entry fails closed in release mode. Signed storage URLs/token contents must never appear in screenshots, repository fixtures or logs. Generated goldens and emulated results do not establish physical-device, unavailable OS-profile or store-release acceptance.
