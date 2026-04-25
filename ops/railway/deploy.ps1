param(
    [string]$ProjectName = "tellpalv2",
    [string]$Environment = "production",
    [string]$BackendService = "tellpal-be",
    [string]$CmsService = "tellpal-cms",
    [string]$PostgresService = "Postgres",
    [string]$EnvFile = "$PSScriptRoot\production.env",
    [switch]$SkipDeploy
)

$ErrorActionPreference = "Stop"

function Assert-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name is not installed or is not available on PATH."
    }
}

function Read-DotEnv($Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing env file: $Path. Copy ops/railway/production.env.example to ops/railway/production.env and fill it in."
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) {
            continue
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $values[$key] = $value
    }
    return $values
}

function Require-Env($Values, $Name) {
    if (-not $Values.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Values[$Name])) {
        throw "Missing required value in production.env: $Name"
    }
    return $Values[$Name]
}

function Invoke-RailwayJson {
    param([string[]]$Arguments)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & railway @Arguments 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -ne 0) {
        throw "railway $($Arguments -join ' ') failed: $output"
    }
    if ([string]::IsNullOrWhiteSpace($output)) {
        return $null
    }
    return $output | ConvertFrom-Json
}

function Invoke-Railway {
    param([string[]]$Arguments)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & railway @Arguments
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -ne 0) {
        throw "railway $($Arguments -join ' ') failed."
    }
}

function Test-ServiceExists($Name) {
    $services = Invoke-RailwayJson @("service", "list", "--environment", $Environment, "--json")
    return $null -ne ($services | Where-Object { $_.name -eq $Name })
}

function Get-ServiceId($Name) {
    $services = Invoke-RailwayJson @("service", "list", "--environment", $Environment, "--json")
    $service = $services | Where-Object { $_.name -eq $Name } | Select-Object -First 1
    if ($null -eq $service) {
        throw "Railway service does not exist: $Name"
    }
    return $service.id
}

function Ensure-Service($Name) {
    if (-not (Test-ServiceExists $Name)) {
        Invoke-Railway @("add", "--service", $Name)
    }
}

function Ensure-Postgres() {
    if (-not (Test-ServiceExists $PostgresService)) {
        Invoke-Railway @("add", "--database", "postgres")
    }
}

function New-NestedConfig($Path, $Value) {
    $parts = $Path.Split(".")
    $node = $Value
    for ($index = $parts.Length - 1; $index -ge 0; $index--) {
        $parent = @{}
        $parent[$parts[$index]] = $node
        $node = $parent
    }
    return $node
}

function Set-ServiceConfig($Service, $Path, $Value, $Message) {
    $serviceId = Get-ServiceId $Service
    $patch = @{
        services = @{
            $serviceId = New-NestedConfig $Path $Value
        }
    } | ConvertTo-Json -Depth 20 -Compress

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = $patch | & railway environment edit --environment $Environment --json --message $Message 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -ne 0) {
        throw "railway environment edit failed: $output"
    }
}

function Set-ServiceVariables($Service, [string[]]$Variables) {
    Invoke-Railway @(
        @("variable", "set") +
        $Variables +
        @("--service", $Service, "--environment", $Environment, "--skip-deploys")
    )
}

function Ensure-RailwayProject() {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $statusOutput = & railway status --json 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -eq 0) {
        return
    }

    $statusText = [string]$statusOutput
    if ($statusText -match "Unauthorized") {
        throw "Railway CLI is not logged in. Run 'railway login' first, then run this script again."
    }

    Invoke-Railway @("init", "--name", $ProjectName)
}

function Get-ServiceDomain($Service) {
    $domain = Invoke-RailwayJson @("domain", "--service", $Service, "--json")
    if ($domain.domain) {
        return "https://$($domain.domain)"
    }
    if ($domain.url) {
        return $domain.url
    }
    if ($domain.host) {
        return "https://$($domain.host)"
    }
    throw "Could not read generated domain for service $Service."
}

$repoRoot = (Resolve-Path "$PSScriptRoot\..\..").Path
$envValues = Read-DotEnv $EnvFile

$adminJwtSecret = Require-Env $envValues "TELLPAL_ADMIN_JWT_SECRET"
if ([Text.Encoding]::UTF8.GetByteCount($adminJwtSecret) -lt 32) {
    throw "TELLPAL_ADMIN_JWT_SECRET must be at least 32 bytes."
}

$firebaseProjectId = Require-Env $envValues "TELLPAL_FIREBASE_PROJECT_ID"
$firebaseBucketName = Require-Env $envValues "TELLPAL_ASSET_STORAGE_FIREBASE_BUCKET_NAME"
$revenueCatHeader = $envValues["TELLPAL_REVENUECAT_AUTHORIZATION_HEADER"]

$firebaseCredentialsB64 = $envValues["FIREBASE_SERVICE_ACCOUNT_JSON_B64"]
if ([string]::IsNullOrWhiteSpace($firebaseCredentialsB64)) {
    $credentialsPath = Require-Env $envValues "FIREBASE_SERVICE_ACCOUNT_JSON_PATH"
    if (-not (Test-Path -LiteralPath $credentialsPath)) {
        throw "Firebase service account JSON file does not exist: $credentialsPath"
    }
    $firebaseCredentialsB64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $credentialsPath).Path))
}

Assert-Command "railway"
Ensure-RailwayProject

Ensure-Postgres
Ensure-Service $BackendService
Ensure-Service $CmsService

Set-ServiceConfig $BackendService "source.rootDirectory" "/be" "configure backend root directory"
Set-ServiceConfig $BackendService "build.builder" "DOCKERFILE" "configure backend Dockerfile builder"
Set-ServiceConfig $BackendService "build.dockerfilePath" "Dockerfile" "configure backend Dockerfile path"
Set-ServiceConfig $BackendService "deploy.healthcheckPath" "/actuator/health" "configure backend healthcheck"
Set-ServiceConfig $BackendService "deploy.startCommand" "sh -c 'echo ""`$FIREBASE_SERVICE_ACCOUNT_JSON_B64"" | base64 -d > /tmp/firebase-service-account.json && java -jar /app/app.jar'" "configure backend start command"

Set-ServiceConfig $CmsService "source.rootDirectory" "/cms" "configure CMS root directory"
Set-ServiceConfig $CmsService "build.builder" "RAILPACK" "configure CMS Railpack builder"
Set-ServiceConfig $CmsService "build.buildCommand" "npm ci && npm run build" "configure CMS build command"

$backendVariables = @(
    "SPRING_PROFILES_ACTIVE=production",
    ('TELLPAL_DB_URL=jdbc:postgresql://${{' + $PostgresService + '.PGHOST}}:${{' + $PostgresService + '.PGPORT}}/${{' + $PostgresService + '.PGDATABASE}}'),
    ('TELLPAL_DB_USERNAME=${{' + $PostgresService + '.POSTGRES_USER}}'),
    ('TELLPAL_DB_PASSWORD=${{' + $PostgresService + '.POSTGRES_PASSWORD}}'),
    "TELLPAL_ADMIN_JWT_SECRET=$adminJwtSecret",
    "TELLPAL_FIREBASE_PROJECT_ID=$firebaseProjectId",
    "TELLPAL_FIREBASE_CREDENTIALS_PATH=/tmp/firebase-service-account.json",
    "TELLPAL_FIREBASE_CHECK_REVOKED=false",
    "TELLPAL_ASSET_STORAGE_FIREBASE_PROJECT_ID=$firebaseProjectId",
    "TELLPAL_ASSET_STORAGE_FIREBASE_BUCKET_NAME=$firebaseBucketName",
    "TELLPAL_ASSET_STORAGE_FIREBASE_CREDENTIALS_PATH=/tmp/firebase-service-account.json",
    "TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX=prod",
    "TELLPAL_ASSET_STORAGE_FIREBASE_FAKE_CLIENT_ENABLED=false",
    "FIREBASE_SERVICE_ACCOUNT_JSON_B64=$firebaseCredentialsB64"
)
if (-not [string]::IsNullOrWhiteSpace($revenueCatHeader)) {
    $backendVariables += "TELLPAL_REVENUECAT_AUTHORIZATION_HEADER=$revenueCatHeader"
}
Set-ServiceVariables $BackendService $backendVariables

$backendUrl = Get-ServiceDomain $BackendService
$cmsUrl = Get-ServiceDomain $CmsService

Set-ServiceVariables $BackendService @("TELLPAL_ADMIN_CORS_ALLOWED_ORIGINS=$cmsUrl")
Set-ServiceVariables $CmsService @(
    "VITE_APP_TITLE=TellPal CMS",
    "VITE_API_BASE_URL=$backendUrl",
    "VITE_DEFAULT_THEME=system",
    "RAILPACK_SPA_OUTPUT_DIR=dist"
)

if (-not $SkipDeploy) {
    Push-Location $repoRoot
    try {
        Invoke-Railway @("up", (Join-Path $repoRoot "be"), "--path-as-root", "--service", $BackendService, "--environment", $Environment, "--detach", "--message", "deploy backend")
        Invoke-Railway @("up", (Join-Path $repoRoot "cms"), "--path-as-root", "--service", $CmsService, "--environment", $Environment, "--detach", "--message", "deploy cms")
    } finally {
        Pop-Location
    }
}

Write-Host "Backend URL: $backendUrl"
Write-Host "CMS URL: $cmsUrl"
Write-Host "Health check: $backendUrl/actuator/health"
