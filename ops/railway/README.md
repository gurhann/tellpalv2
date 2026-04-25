# Railway deployment standard

This folder contains the manual Railway deployment automation and operational standard for TellPal V2.

## Production topology

The production Railway project is expected to contain exactly these services:

- `Postgres`: Railway managed PostgreSQL.
- `tellpal-be`: Spring Boot backend deployed from `be/` with `be/Dockerfile`.
- `tellpal-cms`: Vite CMS deployed from `cms/` as a static single-page app.

The first production environment uses Railway public domains:

- Backend: `https://tellpal-be-production.up.railway.app`
- CMS: `https://tellpal-cms-production.up.railway.app`

Custom domains can be added later, but the CMS origin must always be present in
`TELLPAL_ADMIN_CORS_ALLOWED_ORIGINS` before browser-based admin calls will work.

## Runtime defaults

Backend production runs with:

- `SPRING_PROFILES_ACTIVE=production`
- Railway managed Postgres over the private Railway network.
- The same Firebase project and bucket used by local development.
- Storage isolation through `TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX=prod`.
- Firebase service account JSON provided as a base64 Railway variable and written to
  `/tmp/firebase-service-account.json` by the Railway start command.
- RevenueCat webhook authorization header optional for now. If
  `TELLPAL_REVENUECAT_AUTHORIZATION_HEADER` is blank, the app starts, but RevenueCat webhook
  requests remain unauthorized.

The backend service must keep this Railway start command:

```sh
sh -c 'echo "$FIREBASE_SERVICE_ACCOUNT_JSON_B64" | base64 -d > /tmp/firebase-service-account.json && java -jar /app/app.jar'
```

`be/Dockerfile` intentionally uses `CMD`, not `ENTRYPOINT`, so Railway can apply that start command.

CMS production runs with:

- `VITE_API_BASE_URL` pointing to the public backend domain.
- `RAILPACK_SPA_OUTPUT_DIR=dist`.
- Vite build output served by Railway Railpack static hosting.

## Prerequisites

Install and authenticate the Railway CLI:

```powershell
npm i -g @railway/cli
railway login
```

Prepare production secrets:

```powershell
Copy-Item ops\railway\production.env.example ops\railway\production.env
notepad ops\railway\production.env
```

`ops/railway/production.env` is ignored by git. Keep Firebase service account JSON and RevenueCat values out of the repository.

## Deploy

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File ops\railway\deploy.ps1
```

The script creates or uses the linked Railway project, ensures these services exist, configures variables and build settings, creates public Railway domains, and deploys both apps:

- `Postgres`
- `tellpal-be`
- `tellpal-cms`

For CLI uploads, the script deploys `be/` and `cms/` with `--path-as-root` so Railway analyzes each app directory instead of the monorepo root.

To configure Railway without deploying:

```powershell
powershell -ExecutionPolicy Bypass -File ops\railway\deploy.ps1 -SkipDeploy
```

## Manual deploy commands

Use these commands when deploying one service by hand:

```powershell
railway up C:\github\tellpalv2\be --path-as-root --service tellpal-be --environment production --detach --message "deploy backend"
railway up C:\github\tellpalv2\cms --path-as-root --service tellpal-cms --environment production --detach --message "deploy cms"
```

Always use `--path-as-root` for this monorepo. Without it, Railway may analyze the repository root
instead of the app directory and choose the wrong build plan.

## GitHub push deploys

The repository includes `.github/workflows/railway-deploy.yml` for production deploys from GitHub
Actions.

The workflow runs on pushes to `main` when one of these paths changes:

- `be/**`
- `cms/**`
- `ops/railway/**`
- `.github/workflows/railway-deploy.yml`

It deploys only the changed service:

- backend-related changes deploy `tellpal-be`
- CMS-related changes deploy `tellpal-cms`
- `ops/railway/**` or workflow changes deploy both services

The workflow can also be started manually from the GitHub Actions UI with independent backend/CMS
toggles.

Required GitHub repository secret:

```text
RAILWAY_TOKEN=<Railway account or project token with deploy access>
```

Create the token in Railway, then add it under GitHub repository settings:

```text
Settings > Secrets and variables > Actions > New repository secret
```

Do not commit Railway tokens or put them in workflow files. The workflow passes the token only as
the `RAILWAY_TOKEN` environment variable for `railway up`.

## Admin bootstrap

Production does not seed an admin user through Flyway migrations.

To create or rotate the initial admin user, temporarily set these backend variables and redeploy:

```text
TELLPAL_ADMIN_BOOTSTRAP_USERNAME=<admin username>
TELLPAL_ADMIN_BOOTSTRAP_PASSWORD=<temporary password>
TELLPAL_ADMIN_BOOTSTRAP_ROLE_CODE=ADMIN
```

On startup, the backend creates the user if missing, updates the password if the user already exists,
activates the user, and assigns the role. After confirming login, delete these bootstrap variables
from Railway so the plaintext password is not retained in service configuration.

Do not put bootstrap credentials into Flyway migrations, committed env files, docs, or test fixtures.

## Verify

After deployment, check the generated backend health URL printed by the script:

```text
https://<backend-domain>/actuator/health
```

The CMS URL printed by the script should load the Vite app and call the backend URL configured through `VITE_API_BASE_URL`.

Required verification for every production deploy:

- `cd be && .\mvnw test`
- `cd cms && npm run build`
- GitHub Actions deploy workflow succeeds when deploy is triggered by pushing to `main`.
- Backend deployment status is `SUCCESS`.
- CMS deployment status is `SUCCESS`.
- `GET https://<backend-domain>/actuator/health` returns `{"status":"UP"}`.
- CMS login screen loads over the public CMS domain.
- CMS API calls do not produce CORS failures.
- Backend logs do not contain Flyway migration, Firebase credential, database connection, or startup
  exceptions.

## Development rules for deployable changes

Follow these rules while developing features that will be deployed to Railway:

- Do not commit secrets, service account JSON, generated public URLs with private tokens, or
  `ops/railway/production.env`.
- Keep Railway-specific behavior environment-driven. Do not hard-code Railway domains, Firebase
  credential paths, bucket names, database URLs, or admin credentials in application code.
- Keep local and production Firebase on the same project and bucket until this standard changes;
  isolate uploaded files only through the configured storage path prefix (`local` or `prod`).
- Any new backend environment variable must be added to `application.yml`, documented here when it
  affects production deploys, and represented in `production.env.example` if operators must supply it.
- Any new CMS environment variable must be documented here when it affects Railway behavior.
- If a backend change affects startup, persistence, Flyway, Firebase, security, or admin auth, run
  `cd be && .\mvnw test` before deploy.
- If a CMS change affects route behavior, API client configuration, or build output, run
  `cd cms && npm run build` before deploy.
- If changing CORS, domains, or service names, update backend `TELLPAL_ADMIN_CORS_ALLOWED_ORIGINS`,
  CMS `VITE_API_BASE_URL`, and this runbook in the same task.
- If changing Dockerfile behavior, preserve the ability for Railway to override the start command and
  write the Firebase credential file before Java starts.
- If changing database configuration, keep backend-to-Postgres traffic on Railway private networking.
