# Gradle unit-test kilitlerini temizler.
# Kullanim:
#   .\scripts\clear_test_locks.ps1
#   .\scripts\clear_test_locks.ps1 -SkipGradleStop
#   .\scripts\clear_test_locks.ps1 -WhatIf

[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [switch] $SkipGradleStop
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
$gradlew = Join-Path $repoRoot "gradlew.bat"

$targets = @(
    "app\build\compose_compiler",
    "app\build\crashlytics",
    "app\build\generated",
    "app\build\gmpAppId",
    "app\build\intermediates",
    "app\build\kotlin",
    "app\build\kspCaches",
    "app\build\outputs",
    "app\build\snapshot",
    "app\build\tmp",
    "app\build\intermediates\classes\debug\transformDebugClassesWithAsm\dirs",
    "app\build\intermediates\classes\debugUnitTest\transformDebugUnitTestClassesWithAsm\dirs",
    "app\build\test-results\testDebugUnitTest\binary",
    "app\build\intermediates\hilt\component_classes\debug",
    "app\build\generated\hilt\component_sources\debug"
)

function Resolve-ExistingTarget {
    param([Parameter(Mandatory = $true)][string] $RelativePath)

    $fullPath = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $fullPath)) {
        return $null
    }

    $resolved = (Resolve-Path -LiteralPath $fullPath).Path
    if (-not $resolved.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Guvenlik nedeniyle repo disi path reddedildi: $resolved"
    }
    if ($resolved -notlike (Join-Path $repoRoot "app\build\*")) {
        throw "Guvenlik nedeniyle app\build disi path reddedildi: $resolved"
    }
    return $resolved
}

if (-not $SkipGradleStop) {
    if (Test-Path -LiteralPath $gradlew) {
        Write-Host "Gradle daemon durduruluyor..." -ForegroundColor Cyan
        Push-Location $repoRoot
        try {
            & $gradlew --stop | Out-Host
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "gradlew.bat bulunamadi; daemon stop atlandi." -ForegroundColor Yellow
    }
}

$removed = 0
foreach ($relativeTarget in $targets) {
    $target = Resolve-ExistingTarget -RelativePath $relativeTarget
    if ($null -eq $target) {
        Write-Host "Yok: $relativeTarget" -ForegroundColor DarkGray
        continue
    }

    if ($PSCmdlet.ShouldProcess($target, "Remove generated Gradle test output")) {
        Write-Host "Siliniyor: $target" -ForegroundColor Yellow
        Get-ChildItem -LiteralPath $target -Recurse -Force -ErrorAction SilentlyContinue |
            ForEach-Object { $_.Attributes = [System.IO.FileAttributes]::Normal }
        (Get-Item -LiteralPath $target -Force).Attributes = [System.IO.FileAttributes]::Normal
        Remove-Item -LiteralPath $target -Recurse -Force
        $removed += 1
    }
}

Write-Host ""
Write-Host "Tamam. Silinen hedef sayisi: $removed" -ForegroundColor Green
Write-Host "Tekrar dene: .\gradlew.bat :app:testDebugUnitTest --console=plain" -ForegroundColor Green
