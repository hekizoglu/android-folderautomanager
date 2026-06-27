# CS-3 Fix (Yontem 4) — UAC Self-Elevation ile Windows Defender Dislama
# Kullanim: Sag tik → "PowerShell ile calistir"  VEYA  cift tikla .ps1 uzerine
# UAC onay kutusu cikacak — "Evet" deyin, gerisi otomatik.

$ErrorActionPreference = "Stop"

# Admin degilsek UAC ile yeniden baslat
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]"Administrator")) {
    Start-Process powershell.exe "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
    exit
}

$paths = @(
    "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\app\build",
    "$env:USERPROFILE\.gradle",
    "$env:USERPROFILE\.android"
)

Write-Host "=== Windows Defender Dislama ===" -ForegroundColor Cyan
foreach ($p in $paths) {
    try {
        Add-MpPreference -ExclusionPath $p
        Write-Host "OK: $p" -ForegroundColor Green
    } catch {
        Write-Host "HATA: $p" -ForegroundColor Red
        Write-Host "  $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "Mevcut dislamalar:" -ForegroundColor Yellow
Get-MpPreference | Select-Object -ExpandProperty ExclusionPath | Where-Object { $_ -ne $null } | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "Tamamlandi. Enter ile kapat." -ForegroundColor Cyan
Read-Host
