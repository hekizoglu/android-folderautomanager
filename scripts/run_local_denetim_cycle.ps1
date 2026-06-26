param(
    [string]$CommitMessage = "",
    [switch]$SendTelegram = $false
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$counterPath = Join-Path $scriptDir "loop_count.txt"
$auditScript = Join-Path $scriptDir "audit.ps1"

Set-Location $projectRoot

if (-not (Test-Path $counterPath)) {
    Set-Content -Path $counterPath -Value "0" -Encoding UTF8
}

$current = 0
$parsed = Get-Content $counterPath -ErrorAction SilentlyContinue | Select-Object -First 1
if ($parsed -match '^\d+$') {
    $current = [int]$parsed
}

$cycleNumber = $current + 1
Set-Content -Path $counterPath -Value $cycleNumber -Encoding UTF8

& $auditScript -SendTelegram:$($SendTelegram.IsPresent)
if (-not $?) {
    throw "audit.ps1 basarisiz oldu."
}

$shouldBuild = ($cycleNumber % 3 -eq 0)
if ($shouldBuild) {
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "assembleDebug basarisiz oldu."
    }
}

git add -A
git diff --cached --quiet
if (-not $?) {
    $message = if ($CommitMessage) {
        $CommitMessage
    } else {
        $suffix = if ($shouldBuild) { " + build" } else { "" }
        "chore: local denetim cycle $cycleNumber$suffix"
    }
    git commit -m $message
    if ($LASTEXITCODE -ne 0) {
        throw "git commit basarisiz oldu."
    }
    git push origin main
    if ($LASTEXITCODE -ne 0) {
        throw "git push basarisiz oldu."
    }
} else {
    Write-Host "Commit edilecek degisiklik yok." -ForegroundColor Yellow
}

Write-Host "Dongu tamamlandi: $cycleNumber" -ForegroundColor Green
