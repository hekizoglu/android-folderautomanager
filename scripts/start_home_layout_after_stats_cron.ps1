param(
    [string]$WatcherTaskName = "AppOrganizer_StatsToHomeLayout_Handoff",
    [string]$StatsTaskName = "AppOrganizer_StatsHealthRoadmap_10Min",
    [string]$HomeTaskName = "AppOrganizer_HomeLayoutRoadmap_10Min",
    [string]$StatsRoadmapFile = "ISTATISTIK_TELEMETRI_VE_SAGLIK_ROADMAP.md",
    [string]$HomeRoadmapFile = "ROADMAP_HOME_SCREEN_LAYOUT_EDITOR.md",
    [string]$HomePromptFile = "scripts/home_screen_layout_roadmap_cron_prompt.md",
    [int]$HomeEveryMinutes = 10
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$automationDir = Join-Path $projectRoot ".roadmap-ai-audit-cron"
$stateFile = Join-Path $automationDir "home-layout-handoff-state.json"
$telegramScript = Join-Path $scriptDir "telegram_notify.ps1"
$registerScript = Join-Path $scriptDir "register_roadmap_ai_audit_cron.ps1"
$apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"

New-Item -ItemType Directory -Force -Path $automationDir | Out-Null

function Save-HandoffState {
    param(
        [string]$Status,
        [string]$Message
    )
    [ordered]@{
        status = $Status
        message = $Message
        updated_at = (Get-Date).ToString("o")
        stats_task = $StatsTaskName
        home_task = $HomeTaskName
    } | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8
}

function Send-HandoffTelegram {
    param(
        [string]$Message,
        [string]$File = ""
    )
    if (-not (Test-Path $telegramScript)) { return }
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $telegramScript,
        "-Message", $Message,
        "-EnvPath", (Join-Path $projectRoot ".env")
    )
    if (-not [string]::IsNullOrWhiteSpace($File)) {
        $args += @("-File", $File)
    }
    & powershell.exe @args | Out-Null
}

function Get-PendingCount {
    param([string]$RoadmapFile)
    $path = Join-Path $projectRoot $RoadmapFile
    if (-not (Test-Path $path)) { return 0 }
    return (Select-String -Path $path -Pattern '^\*\*Durum:\*\*\s*(?:\S+\s*)?Bekliyor\b').Count
}

function Get-GitStatusText {
    return ((& git -C $projectRoot status --porcelain) -join "`n").Trim()
}

$statsTask = Get-ScheduledTask -TaskName $StatsTaskName -ErrorAction SilentlyContinue
$statsRunning = $statsTask -and $statsTask.State -eq "Running"
$statsPending = Get-PendingCount -RoadmapFile $StatsRoadmapFile

if ($statsRunning -or $statsPending -gt 0) {
    Save-HandoffState -Status "waiting" -Message "Stats cron still active or pending. running=$statsRunning pending=$statsPending"
    exit 0
}

$dirty = Get-GitStatusText
if (-not [string]::IsNullOrWhiteSpace($dirty)) {
    Save-HandoffState -Status "waiting_dirty" -Message "Stats cron appears done, but worktree is dirty."
    Send-HandoffTelegram "AppOrganizer handoff bekliyor: stats cron bitti gibi, ama worktree dirty. Home cron baslatilmadi."
    exit 0
}

if (-not (Test-Path (Join-Path $projectRoot $HomeRoadmapFile))) {
    throw "Home roadmap not found: $HomeRoadmapFile"
}
if (-not (Test-Path (Join-Path $projectRoot $HomePromptFile))) {
    throw "Home prompt not found: $HomePromptFile"
}

$homePending = Get-PendingCount -RoadmapFile $HomeRoadmapFile
if ($homePending -le 0) {
    Save-HandoffState -Status "nothing_to_start" -Message "Home roadmap has no pending items."
    Send-HandoffTelegram "AppOrganizer handoff: Home layout roadmap icin bekleyen madde yok. Cron baslatilmadi."
    Unregister-ScheduledTask -TaskName $WatcherTaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
    exit 0
}

if (Test-Path $apkPath) {
    Send-HandoffTelegram "AppOrganizer stats cron tamamlandi. Son debug APK ekte. Home layout cron baslatiliyor. Home kalan Bekliyor: $homePending" -File $apkPath
} else {
    Send-HandoffTelegram "AppOrganizer stats cron tamamlandi. APK bulunamadi, Home layout cron baslatiliyor. Home kalan Bekliyor: $homePending"
}

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $registerScript `
    -TaskName $HomeTaskName `
    -EveryMinutes $HomeEveryMinutes `
    -RoadmapFile $HomeRoadmapFile `
    -PromptFile $HomePromptFile `
    -Description "Completes home screen layout editor roadmap pending items one by one with Codex." `
    -StartNow | Out-Null

Save-HandoffState -Status "started_home" -Message "Home layout cron started. pending=$homePending"
Send-HandoffTelegram "AppOrganizer home layout cron basladi. Task=$HomeTaskName. Kalan Bekliyor: $homePending"
Unregister-ScheduledTask -TaskName $WatcherTaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
