param(
    [switch]$CheckOnly
)

# CS-3 fix: UAC self-elevation ile Windows Defender dislama.
# Kullanim:
#   .\scripts\add_defender_exclusion.ps1 -CheckOnly
#   .\scripts\add_defender_exclusion.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$paths = @(
    "$repoRoot\app\build",
    "$env:USERPROFILE\.gradle",
    "$env:USERPROFILE\.android"
)

Write-Host "=== Windows Defender Dislama Kontrolu ===" -ForegroundColor Cyan
Write-Host "Repo: $repoRoot"
foreach ($p in $paths) {
    $exists = Test-Path $p
    $status = if ($exists) { "var" } else { "yok; gerekirse olusturulacak" }
    Write-Host "  $p ($status)"
}

if ($CheckOnly) {
    Write-Host "CheckOnly tamamlandi; Defender ayari degistirilmedi." -ForegroundColor Green
    exit 0
}

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]"Administrator"
)

if (-not $isAdmin) {
    Start-Process powershell.exe "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

Write-Host ""
Write-Host "=== Windows Defender Dislama Ekleniyor ===" -ForegroundColor Cyan
foreach ($p in $paths) {
    try {
        if (-not (Test-Path $p)) {
            New-Item -ItemType Directory -Force -Path $p | Out-Null
        }
        Add-MpPreference -ExclusionPath $p
        Write-Host "OK: $p" -ForegroundColor Green
    } catch {
        Write-Host "HATA: $p" -ForegroundColor Red
        Write-Host "  $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "Mevcut dislamalar:" -ForegroundColor Yellow
Get-MpPreference |
    Select-Object -ExpandProperty ExclusionPath |
    Where-Object { $_ -ne $null } |
    ForEach-Object { Write-Host "  $_" }

Write-Host ""
Write-Host "Tamamlandi. Enter ile kapat." -ForegroundColor Cyan
Read-Host
