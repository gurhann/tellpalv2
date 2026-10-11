# Operator-owned local QA login

This helper prepares an ID token for the existing account selected in `tools/mobile-test-account.json`. The user explicitly authorized preparing the local test login flow on 2026-10-11. Running it still requires the operator's exact, explicitly selected private credential/configuration paths. The native entry driver never invokes this helper or obtains credentials implicitly. A login result is not fixture purpose acceptance, device qualification, a native verification receipt, production access, or Story 1.6 admission.

Use the existing locked `tools/image-optimization` environment; no backend/mobile authentication integration or dependency update is required. The current mobile PoC has no production sign-in flow. Browser Google/Apple and Android Streaming logins do not supply the selected Firebase QA identity's ID token.

## Explicit private inputs

Provide three absolute paths outside the checkout, with no symlink component:

- An existing, operator-selected Google `service_account` Admin JSON file, owned by the current user and mode `0600`. Its `project_id` must equal the explicitly accepted Firebase project in the second input. Its `token_uri` must be `https://oauth2.googleapis.com/token`, its optional `auth_uri` must be `https://accounts.google.com/o/oauth2/auth`, its optional `universe_domain` must be `googleapis.com`, and its service-account email must belong to that project's `iam.gserviceaccount.com` domain. The helper does not locate, create, download or export a private key; it does not change IAM. The credential needs permission to read the existing Firebase user and check token revocation, plus a valid signing key for custom-token creation.
- A private `0600` Web API configuration file, explicitly selected for the same project. It contains exactly the JSON keys below. Obtain its Web API key from that project's authorized configuration; do not infer a project from its display name or select another project after a refusal.
- A nonexistent unique ID-token output filename beneath an existing, owned `0700` private directory. Existing files, repository paths, symlinks and overlap with either input refuse. The helper does not overwrite or delete the input files.

```json
{
  "schemaVersion": 1,
  "firebaseProjectId": "REPLACE_EXPLICIT_ACCEPTED_PROJECT_ID",
  "webApiKey": "REPLACE_SELECTED_PROJECT_WEB_API_KEY"
}
```

The example is incomplete. Do not put actual values into the repository. Pass file paths only; never put a token, private key, password or API key in command arguments, environment variables, Dart defines, screenshots or console output. Run the following commands from `tools/image-optimization/`.

```sh
uv run --locked --offline --python 3.11 python -B native_test_login.py \
  --admin-credential "$PRIVATE_ADMIN_CREDENTIAL_FILE" \
  --web-api-config "$PRIVATE_WEB_API_CONFIGURATION_FILE" \
  --id-token-output "$PRIVATE_NEW_ID_TOKEN_FILE"
```

## When private inputs are unavailable

Confirm the accepted TellPal project ID before retrieving configuration; a display name or an existing browser login does not establish the project or its permissions. In Firebase **Project settings > General > Your apps**, select an existing app and obtain its configuration: `google-services.json` contains the Android app's `current_key`, `GoogleService-Info.plist` contains `API_KEY`, and a web configuration contains `apiKey`. An existing compatible key may be sufficient; creating a new API key is not the default. Check that its restrictions permit the helper's Firebase Authentication exchange without changing production restrictions. Firebase documents [finding the matched API key](https://firebase.google.com/docs/projects/api-keys#finding_your_api_keys). Fill the private `0600` JSON above outside the checkout; keep actual values out of chat, terminal output, command arguments, repository files and logs.

The r1 helper requires a `service_account` JSON containing a signing key. If none is available, do not create a long-lived key automatically. Such a key allows authentication as the service account and normally remains valid until deleted; Google recommends [alternatives to user-managed keys](https://docs.cloud.google.com/iam/docs/best-practices-for-managing-service-account-keys). Before proposing key creation, inspect the selected service account, exact project and existing permissions, then obtain explicit operator approval for that concrete persistent credential and its private storage/lifecycle. Preparing the login helper is not approval to create a key or grant IAM roles.

A possible keyless alternative is [local ADC with service-account impersonation](https://docs.cloud.google.com/docs/authentication/use-service-account-impersonation#adc), which requests short-lived service-account credentials using an authenticated principal. Verify the actual impersonation permissions and Service Account Credentials API state; custom-token remote signing also needs [`iam.serviceAccounts.signBlob`](https://firebase.google.com/docs/auth/admin/create-custom-tokens). Neither the r1 login helper nor the native fixture broker currently accepts this credential mode: both initialize `credentials.Certificate` from an explicitly selected JSON. An explicit adapter and tests for project/UID binding, bounded workers and sanitized output are required before use. Do not substitute an access token for a Firebase ID token or discover ADC implicitly from the home directory.

## Authentication and authority limits

The owned worker verifies that the canonical account exists, is enabled and has the exact selected UID before minting a custom token without additional claims. It uses only the fixed Google HTTPS `accounts:signInWithCustomToken` endpoint with normal TLS verification, capped JSON and no redirect following. The returned ID token is verified with `check_revoked=True`; exact UID, subject, project, issuer and current expiration must match. A new-account indication, mismatch or refusal produces no output token. All Admin and REST work shares a ten-second worker deadline; cleanup kills only the owned process group with a PID fallback and a bounded join. SDK exception text and raw responses are not exported. The worker removes emulator, implicit ADC, proxy and unrelated secret environment settings before importing the SDK.

Only the verified ID token is written to the unique `0600` output. Custom and refresh tokens remain temporary worker-memory values; the refresh token is discarded and is never persisted or used for renewal. Guaranteed memory zeroization is not claimed. Normal Firebase sign-in may update authentication login metadata. The helper makes no explicit account creation/update/deletion, role/custom-claim/subscription change or global revocation call. If the canonical account is absent or disabled, stop; do not substitute another identity or repair the remote account as part of this flow. Firebase documents [custom-token sign-in](https://firebase.google.com/docs/auth/admin/create-custom-tokens) and the [REST exchange](https://firebase.google.com/docs/reference/rest/auth#section-verify-custom-token).

The existing/enabled preflight is an observable check, not an atomic existing-account guarantee. The [Google Identity Platform REST contract](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v1/accounts/signInWithCustomToken) permits sign-in or sign-up and provides no existing-only exchange option. If another operator deletes the account between preflight and exchange, the service can recreate it. Rejecting `isNewUser=true` withholds the output token but cannot undo that remote side effect. The helper never attempts an automatic account deletion or revocation rollback. Coordinate account maintenance outside this flow; this limitation is not fixture authorization.

Successful CLI output contains only fixed status/scope fields and leaves `nativeEntryGate=open`. It does not export identity, project, key, token, file contents or private paths. A failed run cannot be promoted to a successful login or fixture receipt.

## Remaining local fixture prerequisites

The real local/test backend must use an isolated database, an explicit loopback listener/profile and a fresh startup receipt bound to its actual source/artifact/config/PID. Override local defaults to disable stub tokens, configure the same project's real Firebase verifier with revocation checking, and allowlist only the canonical UID through `TELLPAL_LIBRARY_TEST_ALLOWED_FIREBASE_UIDS`. Subscriber fixture configuration is separate and unnecessary for this raster entry. Avoid starting background asset processing or using the ordinary database.

The existing Library admission service resolves or creates a local `app_users` record and primary profile before reading the catalog. Qualify an already prepared local record/profile before treating the admission probe as read-only; do not assume an empty new database supplies that prerequisite. No Firebase user, claim or subscription change is required for the local allowlist.

The ID-token file then becomes an explicit input to the native entry configuration. Fresh account/token verification, admitted raster manifest/purpose receipt, pinned broker TLS, both token-free network legs, exact signed/installed artifacts and the complete private physical prerequisites still apply. The helper does not start a backend/broker, install or launch apps, create a reservation or run a measurement batch.

## Synthetic verification

```sh
uv run --locked --offline --python 3.11 python -B -m unittest test_native_test_login -v
```

Authentication and HTTPS connections in this suite are mocked. It verifies canonical identity/admission ordering, missing/disabled/wrong users, project/credential/token mismatches, private-file/output rules, fixed TLS/no redirects, bounded responses/deadlines, worker cleanup and sanitized outputs. Synthetic local child processes also check actual spawn/IPC success and timeout cleanup without authentication. It obtains no real credentials and makes no Firebase or backend request.
