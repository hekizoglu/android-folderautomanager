[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

Write-Host "[logic-audit-semantic] detekt calisiyor..." -ForegroundColor Cyan
& .\gradlew.bat :app:detekt --quiet
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "[logic-audit-semantic] hizli logic audit calisiyor..." -ForegroundColor Cyan
& powershell -ExecutionPolicy Bypass -File ".\scripts\logic_audit_fast.ps1"
exit $LASTEXITCODE
