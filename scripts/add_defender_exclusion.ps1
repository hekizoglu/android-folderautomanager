# CS-3 Fix: Windows Defender exclusion — Admin yetkisi olmadan Task Scheduler ile SYSTEM olarak calistir
# Kullanim: Normal PS penceresinde calistir (yonetici gerektirmez)
#
# Ne yapar: Task Scheduler uzerinden SYSTEM hesabiyla Add-MpPreference calistirir.
# SYSTEM hesabi her zaman Defender yonetim yetkisine sahiptir.

$paths = @(
    "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\app\build",
    "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\app\build\intermediates",
    "$env:USERPROFILE\.gradle",
    "$env:USERPROFILE\.android"
)

$taskName = "AppOrganizerDefenderFix"

foreach ($path in $paths) {
    $escapedPath = $path -replace "'", "''"
    $action = New-ScheduledTaskAction `
        -Execute "PowerShell.exe" `
        -Argument "-NonInteractive -WindowStyle Hidden -Command `"Add-MpPreference -ExclusionPath '$escapedPath'`""

    $trigger  = New-ScheduledTaskTrigger -Once -At (Get-Date).AddSeconds(3)
    $settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Minutes 1)
    $principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest

    Register-ScheduledTask `
        -TaskName "$taskName`_$(Split-Path $path -Leaf)" `
        -Action $action `
        -Trigger $trigger `
        -Settings $settings `
        -Principal $principal `
        -Force | Out-Null

    Start-ScheduledTask -TaskName "$taskName`_$(Split-Path $path -Leaf)"
    Write-Host "Ekleniyor: $path"
    Start-Sleep -Seconds 2

    Unregister-ScheduledTask -TaskName "$taskName`_$(Split-Path $path -Leaf)" -Confirm:$false
}

Write-Host ""
Write-Host "Tamamlandi. Mevcut exclusion listesi:"
Get-MpPreference | Select-Object -ExpandProperty ExclusionPath | Where-Object { $_ -ne $null }
