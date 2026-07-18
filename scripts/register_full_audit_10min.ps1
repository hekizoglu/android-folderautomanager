param(
    [string]$TaskName = "AppOrganizer_FullAudit_Roadmap_10Min",
    [int]$EveryMinutes = 10,
    [switch]$StartNow
)

$ErrorActionPreference = "Stop"
if ($EveryMinutes -lt 1 -or $EveryMinutes -gt 1439) { throw "EveryMinutes 1-1439 arasinda olmali." }
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runner = Join-Path $scriptDir "run_full_audit_10min.ps1"
if (-not (Test-Path $runner)) { throw "Runner bulunamadi: $runner" }

Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
$startAt = (Get-Date).AddMinutes(1)
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$runner`"" -WorkingDirectory $projectRoot
$trigger = New-ScheduledTaskTrigger -Once -At $startAt -RepetitionInterval (New-TimeSpan -Minutes $EveryMinutes) -RepetitionDuration (New-TimeSpan -Days 3650)
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 2)

try {
    Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Description "AppOrganizer tam kapsamli denetim roadmap maddelerini 10 dakikada bir tek tek cozer." -Force | Out-Null
} catch {
    $taskRun = "powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$runner`""
    & schtasks.exe /create /f /sc minute /mo $EveryMinutes /tn $TaskName /tr $taskRun /st $startAt.ToString("HH:mm") | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Scheduled Task kaydi basarisiz: exit=$LASTEXITCODE" }
}
Write-Host "Task: $TaskName" -ForegroundColor Green
Write-Host "Interval: $EveryMinutes dakika" -ForegroundColor Cyan
Write-Host "First run: $($startAt.ToString('yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Cyan
if ($StartNow) {
    Start-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    if ($LASTEXITCODE -ne 0) { & schtasks.exe /run /tn $TaskName | Out-Null }
}
