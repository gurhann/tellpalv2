# Controlled Story 1.2 probes

This harness emits evidence only when executed. Six authenticated HTTP cases are recorded in `_bmad-output/implementation-artifacts/mobile/library-1-2-http-evidence.json`; the full prerequisite and native acceptance matrix remains pending. It reads the current isolated backend Library response, then checks the selected category/content/language identity, audience presentation, recommendation order and actual hero SHA-256/size. It probes missing/invalid bearer, invalid experience, missing UI language and unavailable content. With a VM log it configures the debug app in memory and compares native detail state with an independent current HTTP response.

Use Node 22+ for VM probes. Always use the existing Firebase UID selected in `tools/mobile-test-account.json`: `jLRypTmoBrhhX21HsDJojurcKTC3`. The user authorized this identity on 2026-10-07. Verify that it exists and is enabled before obtaining a current Firebase ID token, and keep the token in a private file outside the repository. Admit only this UID to the isolated backend. Never put credentials in command arguments, Dart defines, screenshots or evidence. The harness accepts a private token **file path**, and emits no response bodies, signed URLs, tokens, debugger URLs or error stacks. It only admits loopback backend addresses. It does not upload or mutate remote storage.

From `app/mobile/`, start the existing controlled-development entry point on a booted device:

```sh
flutter run -d DEVICE_ID -t lib/main_library_dev.dart > /private/tmp/tellpal-story-1-2-native.log 2>&1
```

From the repository root, run a probe. Replace the placeholders with the actual private token path, current category/content IDs and device. Omit `--category`/`--content` to select the first current row/item. Omit `--vm-log` for HTTP-only evidence.

```sh
node tools/library-1-2/acceptance.mjs \
  --token PRIVATE_TOKEN_FILE \
  --base-url http://127.0.0.1:8082 \
  --language tr \
  --category CURRENT_CATEGORY_ID \
  --content CURRENT_CONTENT_ID \
  --vm-log /private/tmp/tellpal-story-1-2-native.log \
  --scale 1.0 \
  --output /private/tmp/tellpal-story-1-2-tr-phone-probe.json
```

Each output is one case and mode 0600. Keep private outputs separate for language, mapped row, audience, free/locked selection, scale and device. After inspecting the redacted fields, consolidate their actual executed results into `library-1-2-http-evidence.json`; unsuccessful or unexecuted cases must remain pending. The current debug action opens detail directly: it does **not** prove the production Library card callback, reference parity, platform semantics/contrast, return scroll, recommendation taps, natural URL expiry, physical memory/latency or all admission fault cases. Complete those separately using the companion matrix. Enable the prepared `LibraryView(detailEnabled: true)` callback only after the real backend/fixture prerequisite gate passes, then verify the actual card journey. Update the old native Library assertion that cards are disabled when that gate is opened; unrelated disabled controls must stay disabled.

The existing ephemeral fixture runner uses backend 8082, PostgreSQL 5442 and verified existing remote objects. Do not switch to the user's ordinary database or reuse fixture credentials in the repository. Renew temporary sessions only for the designated existing account; do not create users or change roles, claims or subscriptions. Failed admission does not authorize selecting another identity. Subscriber variants use isolated server configuration with this same UID.
