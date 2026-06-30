# AppOrganizer build helper
# Usage:
#   .\build.ps1
#   .\build.ps1 -Clean
#   .\build.ps1 -Release

param(
    [switch]$Clean,
    [switch]$Release
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$project = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $project

$target = if ($Release) { "bundleRelease" } else { "assembleDebug" }
$gradleArgs = @($target, "--build-cache", "--parallel")

function Remove-BuildSubdirs {
    param(
        [string]$ProjectRoot,
        [string[]]$RelativePaths
    )

    $empty = New-Item -ItemType Directory -Force (Join-Path $env:TEMP "apporganizer_empty_robocopy_placeholder")
    try {
        foreach ($relativePath in $RelativePaths) {
            $targetDir = Join-Path $ProjectRoot $relativePath
            if (-not (Test-Path $targetDir)) {
                continue
            }

            $resolved = (Resolve-Path $targetDir).Path
            if (-not $resolved.StartsWith($ProjectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
                throw "Clean target is outside project: $resolved"
            }

            robocopy $empty $resolved /MIR /NFL /NDL /NJH /NJS /NC /NS 2>$null | Out-Null
            Remove-Item -LiteralPath $resolved -Recurse -Force -ErrorAction SilentlyContinue
        }
    } finally {
        Remove-Item -LiteralPath $empty -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if ($Clean) {
    Write-Host "[build.ps1] Stopping Gradle daemon..." -ForegroundColor Yellow
    & .\gradlew.bat --stop 2>$null
    Start-Sleep -Seconds 3

    Remove-BuildSubdirs -ProjectRoot $project -RelativePaths @(
        "app\build\tmp\kapt3",
        "app\build\kotlin",
        "app\build\generated\hilt",
        "app\build\intermediates\hilt",
        "app\build\intermediates\packaged_res",
        "app\build\intermediates\merged_res"
    )

    Write-Host "[build.ps1] Cleaned known lock-prone build dirs." -ForegroundColor Green
}

Write-Host "[build.ps1] Build starting: $target ..." -ForegroundColor Cyan
& .\gradlew.bat @gradleArgs
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        $sizeMB = [math]::Round((Get-Item $apk).Length / 1MB, 1)
        Write-Host "[build.ps1] BUILD OK - APK: $sizeMB MB" -ForegroundColor Green
    } else {
        Write-Host "[build.ps1] BUILD OK" -ForegroundColor Green
    }
    exit 0
}

Write-Host "[build.ps1] BUILD FAILED - retrying with -Clean ..." -ForegroundColor Red
if (-not $Clean) {
    if ($Release) {
        & $PSCommandPath -Clean -Release
    } else {
        & $PSCommandPath -Clean
    }
    exit $LASTEXITCODE
}

Write-Host "[build.ps1] Clean retry failed. Check Gradle output above." -ForegroundColor Red
exit $exitCode
