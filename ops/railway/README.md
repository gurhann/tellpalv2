# Railway deployment

This folder contains the manual Railway deployment automation for TellPal V2.

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

## Verify

After deployment, check the generated backend health URL printed by the script:

```text
https://<backend-domain>/actuator/health
```

The CMS URL printed by the script should load the Vite app and call the backend URL configured through `VITE_API_BASE_URL`.
