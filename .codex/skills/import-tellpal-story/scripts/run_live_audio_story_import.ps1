$ErrorActionPreference = "Stop"

$Host.UI.RawUI.WindowTitle = "TellPal audio-story import"
$env:TELLPAL_API_BASE_URL = "https://tellpal-be-production.up.railway.app"
$env:TELLPAL_ADMIN_USERNAME = "tellpal_yonet"

$python = "C:\Users\gurha\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
$importer = "C:\github\tellpalv2\.codex\skills\import-tellpal-story\scripts\import_audio_stories.py"
$csv = "C:\github\tellpalv2\cms\yuklenecek_hikayeler\audio_stories\audio_stories.csv"
$serviceAccount = "C:\dev\keys\tellpal-v2-firebase-storage-pk.json"

foreach ($path in @($python, $importer, $csv, $serviceAccount)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required file not found: $path"
    }
}

Set-Location -LiteralPath "C:\github\tellpalv2"
Write-Host "Starting TellPal audio-story importer..." -ForegroundColor Cyan
Write-Host "Python: $python"
Write-Host "CSV: $csv"
Write-Host "Service account: $serviceAccount"
Write-Host ""

& $python -B $importer $csv --service-account-json $serviceAccount
$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "Importer exited with code $exitCode." -ForegroundColor Yellow
Read-Host "Press Enter to close this window"
exit $exitCode
