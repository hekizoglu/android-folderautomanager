param(
    [string]$TaskName = "AppOrganizer_FullAudit_Roadmap_10Min",
    [string]$RoadmapFile = "GUNCEL_TAM_KAPSAMLI_DENETIM_VE_DUZELTME_ROADMAP_2026-07-18.md",
    [string]$PromptFile = "scripts/full_audit_10min_prompt.md",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$automationDir = Join-Path $projectRoot ".full-audit-cron"
$logDir = Join-Path $automationDir "logs"
$stateFile = Join-Path $automationDir "state.json"
$lockFile = Join-Path $automationDir "run.lock"
$lastMessageFile = Join-Path $automationDir "last_codex_message.txt"
$roadmapPath = Join-Path $projectRoot $RoadmapFile
$promptPath = Join-Path $projectRoot $PromptFile
$telegramScript = Join-Path $scriptDir "telegram_notify.ps1"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $logDir "full-audit-$runId.log"

function Write-Log([string]$Message) {
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -LiteralPath $logFile -Value $line -Encoding UTF8
    Write-Host $line
}

function Save-State([string]$Status, [string]$Message, [string]$CurrentItem = "", [int]$ExitCode = 0) {
    [ordered]@{
        task_name = $TaskName
        roadmap = $RoadmapFile
        run_id = $runId
        status = $Status
        current_item = $CurrentItem
        message = $Message
        exit_code = $ExitCode
        updated_at = (Get-Date).ToString("o")
        log_file = $logFile
    } | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding UTF8
}

function Send-Notice([string]$Message) {
    if (-not (Test-Path $telegramScript)) { return }
    try {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $telegramScript `
            -Message $Message -EnvPath (Join-Path $projectRoot ".env") | Out-Null
    } catch { Write-Log "Telegram skipped: $($_.Exception.Message)" }
}

function Get-GitStatus {
    $lines = & git -C $projectRoot status --porcelain 2>$null
    if ($lines) { return ($lines -join "`n").Trim() }
    return ""
}

function Get-FirstPendingItem {
    $lines = Get-Content -LiteralPath $roadmapPath -Encoding UTF8
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\*\*Durum:\*\*.*Bekliyor\b') {
            for ($j = $i; $j -ge 0; $j--) {
                if ($lines[$j] -match '^##\s+(R\d+)\s+[—-]\s+(.+)$') {
                    return [pscustomobject]@{ Id = $Matches[1]; Title = $Matches[2].Trim(); Line = $i + 1 }
                }
            }
        }
    }
    return $null
}

function Resolve-Codex {
    $command = Get-Command codex -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $fallback = Join-Path $env:USERPROFILE ".vscode\extensions\openai.chatgpt-26.5715.31925-win32-x64\bin\windows-x86_64\codex.exe"
    if (Test-Path $fallback) { return $fallback }
    throw "codex.exe bulunamadi."
}

try {
    if (-not (Test-Path $roadmapPath)) {
        $previousStatus = ""
        if (Test-Path $stateFile) {
            try { $previousStatus = (Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json).status } catch { }
        }
        Save-State "retired" "Roadmap insan kabulüyle silindi; denetim görevi kapatıldı."
        if ($previousStatus -ne "retired") {
            Send-Notice "AppOrganizer full audit kapatildi: roadmap insan kabulüyle silindi; bekleyen madde yok."
        }
        exit 0
    }
    if (-not (Test-Path $promptPath)) { throw "Prompt bulunamadi: $promptPath" }
    if (Test-Path $lockFile) {
        Save-State "locked" "Onceki tur hala aktif." "" 2
        exit 2
    }
    @{ pid = $PID; started_at = (Get-Date).ToString("o"); run_id = $runId } |
        ConvertTo-Json | Set-Content -LiteralPath $lockFile -Encoding UTF8

    $dangerMarkers = @("MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "BISECT_LOG") |
        ForEach-Object { Join-Path (Join-Path $projectRoot ".git") $_ }
    if ($dangerMarkers | Where-Object { Test-Path $_ }) {
        Save-State "blocked_git_state" "Git merge/rebase durumu var." "" 2
        Send-Notice "AppOrganizer full audit durdu: riskli Git durumu var."
        exit 2
    }

    $dirty = Get-GitStatus
    if ($dirty) {
        Save-State "blocked_dirty_worktree" "Onceki degisiklikler korunuyor; temiz calisma agaci bekleniyor." "" 2
        Write-Log "Dirty worktree; no files touched.`n$dirty"
        Send-Notice "AppOrganizer full audit bekliyor: calisma agaci kirli. Onceki degisiklikler korunuyor; yeni R maddesine dokunulmadi."
        exit 2
    }

    $item = Get-FirstPendingItem
    if (-not $item) {
        Save-State "finished" "Bekleyen R maddesi kalmadi; roadmap silinmedi, son insan kabulune birakildi."
        Send-Notice "AppOrganizer full audit tamamlandi: bekleyen R maddesi yok. Roadmap insan kabulunden sonra silinecek."
        exit 0
    }

    $codex = Resolve-Codex
    $prompt = @"
Automation run: $runId
Project: $projectRoot
Roadmap: $RoadmapFile
Current item: $($item.Id) - $($item.Title)
Status line: $($item.Line)

$(Get-Content -LiteralPath $promptPath -Raw -Encoding UTF8)
"@
    Save-State "running" "Tek R maddesi icin Codex baslatildi." "$($item.Id) - $($item.Title)"
    Send-Notice "AppOrganizer full audit basladi: $($item.Id) - $($item.Title)"
    Set-Content -LiteralPath $lastMessageFile -Value "" -Encoding UTF8

    if ($DryRun) {
        Save-State "dry_run" "Dry run; Codex calistirilmadi." "$($item.Id) - $($item.Title)"
        exit 0
    }

    $codexArgs = @("exec", "--cd", $projectRoot, "--sandbox", "danger-full-access", "--dangerously-bypass-approvals-and-sandbox", "--output-last-message", $lastMessageFile, "-")
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $prompt | & $codex @codexArgs 2>&1 | Tee-Object -FilePath $logFile -Append
        $exitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }

    $message = if (Test-Path $lastMessageFile) { (Get-Content $lastMessageFile -Raw).Trim() } else { "" }
    if ($exitCode -eq 0) {
        Save-State "completed" ($message | Select-Object -First 1) "$($item.Id) - $($item.Title)"
        Send-Notice "AppOrganizer full audit turu bitti: $($item.Id). Sonraki tur 10 dakika icinde."
        exit 0
    }
    Save-State "failed" "Codex exit=$exitCode. $message" "$($item.Id) - $($item.Title)" $exitCode
    Send-Notice "AppOrganizer full audit hata verdi: $($item.Id), exit=$exitCode. Log: $logFile"
    exit $exitCode
} catch {
    Save-State "error" $_.Exception.Message "" 1
    Send-Notice "AppOrganizer full audit fatal hata: $($_.Exception.Message)"
    exit 1
} finally {
    Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
}
