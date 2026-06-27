<#
.SYNOPSIS
    QA Build Gates Script — AppOrganizer
    APK uretmeden once tum kalite kapilarini kontrol eder.
#>

param(
    [switch]$DryRun = $false
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$reportDir = Join-Path $projectRoot "qa\reports"
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
$reportFile = Join-Path $reportDir "build-gate-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  QA Build Gates - AppOrganizer" -ForegroundColor Cyan
Write-Host "  Tarih: $timestamp" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$errors = 0

Set-Location $projectRoot

Write-Host ""
Write-Host "[1/4] Android Lint..." -ForegroundColor Yellow
if ($DryRun) {
    Write-Host "  DRYRUN - atlaniyor" -ForegroundColor Gray
} else {
    & .\gradlew.bat lintDebug --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  PASS" -ForegroundColor Green
    } else {
        Write-Host "  FAIL" -ForegroundColor Red
        $errors = $errors + 1
    }
}

Write-Host ""
Write-Host "[2/4] Unit Tests..." -ForegroundColor Yellow
if ($DryRun) {
    Write-Host "  DRYRUN - atlaniyor" -ForegroundColor Gray
} else {
    & .\gradlew.bat testDebugUnitTest --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  PASS" -ForegroundColor Green
    } else {
        Write-Host "  FAIL" -ForegroundColor Red
        $errors = $errors + 1
    }
}

Write-Host ""
Write-Host "[3/4] detekt..." -ForegroundColor Yellow
$detektConfig = Join-Path $projectRoot "detekt.yml"
if (Test-Path $detektConfig) {
    if ($DryRun) {
        Write-Host "  DRYRUN - atlaniyor" -ForegroundColor Gray
    } else {
        & .\gradlew.bat detekt --quiet
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  PASS" -ForegroundColor Green
        } else {
            Write-Host "  FAIL" -ForegroundColor Red
            $errors = $errors + 1
        }
    }
} else {
    Write-Host "  SKIP (detekt.yml bulunamadi)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "[4/4] ktlint..." -ForegroundColor Yellow
if ($DryRun) {
    Write-Host "  DRYRUN - atlaniyor" -ForegroundColor Gray
} else {
    & .\gradlew.bat ktlintCheck --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  PASS" -ForegroundColor Green
    } else {
        Write-Host "  FAIL" -ForegroundColor Red
        $errors = $errors + 1
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
if ($errors -eq 0) {
    Write-Host "  TUM KALITE KAPILARI GECTI" -ForegroundColor Green
    Write-Host "  APK uretilmeye hazir." -ForegroundColor Green
} else {
    Write-Host "  HATA: $errors kalite kapisi basarisiz!" -ForegroundColor Red
    Write-Host "  Build durduruldu." -ForegroundColor Red
}
Write-Host "==========================================" -ForegroundColor Cyan

$status = "PASS"
if ($errors -ne 0) { $status = "FAIL" }

$lines = @()
$lines += "QA Build Gate Report"
$lines += "Date: $timestamp"
$lines += "Errors: $errors"
$lines += "Status: $status"
$reportText = $lines -join "`r`n"
[System.IO.File]::WriteAllText($reportFile, $reportText, [System.Text.Encoding]::UTF8)

Write-Host ""
Write-Host "[qa] Rapor: $reportFile" -ForegroundColor Green

exit $errors
