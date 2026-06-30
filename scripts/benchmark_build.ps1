# Runs local Gradle build benchmarks and writes a short internal report.

param(
    [switch]$SkipProfile,
    [switch]$SkipConfigurationCache
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$reportPath = Join-Path $projectRoot "docs\internal\build_benchmark_latest.md"
$mutex = [System.Threading.Mutex]::new($false, "Global\AppOrganizerGradleBuild")
$lockAcquired = $false

Set-Location $projectRoot

function Invoke-Step {
    param(
        [string]$Name,
        [string[]]$Arguments
    )

    $started = Get-Date
    Write-Host "[benchmark] $Name" -ForegroundColor Cyan
    & .\gradlew.bat @Arguments | ForEach-Object { Write-Host $_ }
    $exitCode = $LASTEXITCODE
    $elapsed = [math]::Round(((Get-Date) - $started).TotalSeconds, 1)
    [PSCustomObject]@{
        Name = $Name
        ExitCode = $exitCode
        Seconds = $elapsed
    }
}

try {
    $lockAcquired = $mutex.WaitOne([TimeSpan]::FromMinutes(90))
    if (-not $lockAcquired) {
        throw "Timed out waiting for AppOrganizer Gradle build lock."
    }

    $results = @()

    if (-not $SkipProfile) {
        $results += Invoke-Step -Name "profile assembleDebug rerun" -Arguments @(
            "--profile",
            "--rerun-tasks",
            "assembleDebug"
        )
    }

    if (-not $SkipConfigurationCache) {
        $results += Invoke-Step -Name "configuration cache compileDebugKotlin" -Arguments @(
            ":app:compileDebugKotlin",
            "--configuration-cache",
            "--configuration-cache-problems=warn"
        )
    }

    $profileReport = Get-ChildItem -Path (Join-Path $projectRoot "build\reports\profile") -Filter "*.html" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    $lines = @()
    $lines += "# Build Benchmark Latest"
    $lines += ""
    $lines += "> Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    $lines += ""
    $lines += "| Step | Exit | Seconds |"
    $lines += "|------|------|---------|"
    foreach ($result in $results) {
        $lines += "| $($result.Name) | $($result.ExitCode) | $($result.Seconds) |"
    }
    $lines += ""
    if ($profileReport) {
        $relativeProfile = Resolve-Path -Path $profileReport.FullName -Relative
        $lines += "- Latest Gradle profile report: $relativeProfile"
    } else {
        $lines += "- Latest Gradle profile report: not generated"
    }
    $lines += '- Configuration cache is tested from CLI only; gradle.properties remains conservative.'
    $lines += ""

    [System.IO.File]::WriteAllText($reportPath, ($lines -join [Environment]::NewLine), [System.Text.Encoding]::UTF8)
    Write-Host "[benchmark] Report: $reportPath" -ForegroundColor Green

    $failed = $results | Where-Object { $_.ExitCode -ne 0 } | Select-Object -First 1
    if ($failed) {
        exit $failed.ExitCode
    }
    exit 0
} finally {
    if ($lockAcquired) {
        $mutex.ReleaseMutex() | Out-Null
    }
    $mutex.Dispose()
}
