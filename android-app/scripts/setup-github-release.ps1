<#
.SYNOPSIS
    Interactive release helper for HatchDroid maintainers.
#>

[CmdletBinding()]
param()

$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Base64File = Join-Path $RepoRoot "android-app\KEYSTORE_BASE64.txt"
$DistApk = Join-Path $RepoRoot "dist\HatchDroid-v0.1.0-arm64-v8a.apk"
$ShaFile = Join-Path $RepoRoot "dist\SHA256SUMS.txt"

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "       HatchDroid: Open-Source Release Assistant        " -ForegroundColor Green
Write-Host "========================================================`n" -ForegroundColor Cyan

Write-Host "Local release artifacts ready:" -ForegroundColor Yellow
if (Test-Path $DistApk) {
    Write-Host "  APK: $DistApk ($([math]::Round((Get-Item $DistApk).Length / 1MB, 2)) MB)" -ForegroundColor White
}
if (Test-Path $ShaFile) {
    Write-Host "  SHA256: $(Get-Content $ShaFile)" -ForegroundColor White
}

Write-Host "`n--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "STEP 1: Configure GitHub Repository Secrets" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Open this URL in your browser:" -ForegroundColor White
Write-Host "  https://github.com/rsshir60/hatchdroid/settings/secrets/actions`n" -ForegroundColor Cyan

Write-Host "Add the following 4 repository secrets:" -ForegroundColor White
Write-Host "  1. KEYSTORE_BASE64   -> (Copied from android-app\KEYSTORE_BASE64.txt)" -ForegroundColor Gray
Write-Host "  2. KEYSTORE_PASSWORD -> hatchzip123" -ForegroundColor Gray
Write-Host "  3. KEY_ALIAS         -> hatchzip" -ForegroundColor Gray
Write-Host "  4. KEY_PASSWORD      -> hatchzip123" -ForegroundColor Gray

if (Test-Path $Base64File) {
    Set-Clipboard -Path $Base64File
    Write-Host "`n[OK] KEYSTORE_BASE64 has been automatically copied to your clipboard!" -ForegroundColor Green
}

Write-Host "`n--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "STEP 2: Push Commits and Release Tag" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Run these two git commands to trigger the automated release:" -ForegroundColor White
Write-Host "  git push origin main" -ForegroundColor Cyan
Write-Host "  git push origin v0.1.0`n" -ForegroundColor Cyan

Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "STEP 3: Submit to IzzyOnDroid (F-Droid Staging)" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Once GitHub Actions finishes building the release, open:" -ForegroundColor White
Write-Host "  https://gitlab.com/IzzyOnDroid/repo/-/issues/new?issuable_template=App%20Inclusion" -ForegroundColor Cyan
Write-Host "And paste the contents of packaging\izzyondroid\inclusion_request.md`n" -ForegroundColor White
