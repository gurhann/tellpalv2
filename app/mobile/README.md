# TellPal story media PoC

This isolated Flutter app plays the Turkish `Haberci Nota` story (`contentId=1`) from the protected, revisioned test manifest. It loads one page image and its MP3 at a time, prepares the next image, advances when narration completes, and refreshes short-lived URLs against the same revision.

## Run locally

1. Start the backend with the `local` profile, stub tokens enabled, and a UID in `TELLPAL_STORY_PAGE_MEDIA_TEST_ALLOWED_FIREBASE_UIDS`. The story's `STORY_PAGE_MEDIA` job must have completed.
2. In this directory run `flutter pub get` and `flutter run -d <device-id>`.
3. In the app, enter the backend URL and `stub:<allowlisted-uid>` in the token field. Do not include the `Bearer` prefix; the client adds it.
4. Tap **Hikâyeyi aç**. The story screen shows the pinned revision, image, audio controls, and page navigation.

For an Android emulator use `http://10.0.2.2:8080`. For an iOS simulator use `http://localhost:8080`. A physical device needs the Mac's reachable LAN address and a backend that accepts LAN connections. HTTP is allowed on Android only in debug builds; iOS permits local networking for this PoC.

The test token is entered at runtime and is never persisted or compiled into the app. Do not add Firebase credentials, signed URLs, or real user tokens to the repository. This PoC does not yet implement production sign-in, entitlement checks, offline media, background audio, or the full legacy UI.

## Verify on a simulator

With the local backend running and the test UID allowlisted, run:

```sh
flutter test integration_test/story_media_test.dart -d <device-id> \
  --dart-define=POC_TEST_TOKEN=stub:<allowlisted-uid>
```

The device test opens the protected story, waits for image and audio readiness on all 14 pages, pauses playback, and advances through the story. The stub token is only passed to the test process; do not use a production credential here.

For repeated forward/backward navigation and app lifecycle coverage, run:

```sh
flutter test integration_test/story_media_stress_test.dart -d <device-id> \
  --dart-define=POC_TEST_TOKEN=stub:<allowlisted-uid>
```

The stress test completes three full forward/backward passes and three pause/resume lifecycle cycles while verifying that every page image and audio source becomes ready.

For rapid navigation, backend outage recovery, and expiring URL refresh coverage, run:

```sh
flutter test integration_test/story_media_resilience_test.dart -d <device-id> \
  --dart-define=POC_TEST_TOKEN=stub:<allowlisted-uid>
```
