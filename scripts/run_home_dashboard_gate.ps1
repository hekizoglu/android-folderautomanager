# Ana ekran / dashboard yayin kapisi.
# Kullanim:
#   .\scripts\run_home_dashboard_gate.ps1
#   .\scripts\run_home_dashboard_gate.ps1 -SkipLint -SkipAssemble
#   .\scripts\run_home_dashboard_gate.ps1 -SkipAndroidTestApk
#   .\scripts\run_home_dashboard_gate.ps1 -SkipLockClear

[CmdletBinding()]
param(
    [switch] $SkipLockClear,
    [switch] $SkipLint,
    [switch] $SkipAssemble,
    [switch] $SkipAndroidTestApk
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
$gradlew = Join-Path $repoRoot "gradlew.bat"
$lockClearScript = Join-Path $PSScriptRoot "clear_test_locks.ps1"

if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "gradlew.bat bulunamadi: $gradlew"
}

Push-Location $repoRoot
try {
    if (-not $SkipLockClear -and (Test-Path -LiteralPath $lockClearScript)) {
        Write-Host "Build/test kilitleri temizleniyor..." -ForegroundColor Cyan
        & $lockClearScript
    }

    $testFilters = @(
        "com.armutlu.apporganizer.domain.home.HomeIntelligenceHealthReportTest",
        "com.armutlu.apporganizer.domain.home.HomeIntelligenceCoordinatorTest",
        "com.armutlu.apporganizer.domain.usecase.missions.MissionEngineTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.DashboardLayoutPolicyTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.HomeAdaptiveLayoutPolicyTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.HomeCommandResolverTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.HomeGestureCoordinatorTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.HomePageTelemetryPolicyTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.HomeScreenNavigationContractTest",
        "com.armutlu.apporganizer.presentation.ui.launcher.GlobalSearchHostTest",
        "com.armutlu.apporganizer.telemetry.TelemetryEventValidatorTest",
        "com.armutlu.apporganizer.telemetry.HomePageTelemetryEventValidatorTest",
        "com.armutlu.apporganizer.utils.DiagnosticsReportManagerTest",
        "com.armutlu.apporganizer.utils.SearchCacheTest",
        "com.armutlu.apporganizer.utils.SuggestionCoordinatorTest",
        "com.armutlu.apporganizer.utils.TickerComposerTest",
        "com.armutlu.apporganizer.utils.TaskScoreManagerTest"
    )

    $testArgs = @(":app:testDebugUnitTest", "--console=plain")
    foreach ($filter in $testFilters) {
        $testArgs += "--tests"
        $testArgs += $filter
    }

    Write-Host "Odakli ana ekran/dashboard unit testleri calisiyor..." -ForegroundColor Cyan
    & $gradlew @testArgs

    if (-not $SkipLint) {
        Write-Host "Lint kapisi calisiyor..." -ForegroundColor Cyan
        & $gradlew ":app:lintDebug" "--console=plain"
    }

    if (-not $SkipAssemble) {
        Write-Host "Debug APK kapisi calisiyor..." -ForegroundColor Cyan
        & $gradlew ":app:assembleDebug" "--console=plain"
    }

    if (-not $SkipAndroidTestApk) {
        Write-Host "Android test APK kapisi calisiyor..." -ForegroundColor Cyan
        & $gradlew ":app:assembleDebugAndroidTest" "--console=plain"
    }
} finally {
    Pop-Location
}
