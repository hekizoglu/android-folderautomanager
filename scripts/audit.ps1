<#
.SYNOPSIS
    audit.ps1 — AppOrganizer local denetim scripti.
    15 dakikada bir calistirilir, bulgulari local_denetim_raporu.md'ye ekler ve Telegram'a gonderir.
#>

param(
    [switch]$SendTelegram = $true,
    [switch]$DryRun = $false
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "SilentlyContinue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$raporPath   = Join-Path $projectRoot "local_denetim_raporu.md"
$envPath     = Join-Path $projectRoot ".env"
$timestamp   = Get-Date -Format "yyyy-MM-dd HH:mm"

Set-Location $projectRoot

function Get-EnvValue {
    param([string]$Key)
    if (-not (Test-Path $envPath)) {
        Write-Host "  [DEBUG] .env bulunamadi: $envPath" -ForegroundColor Red
        return $null
    }
    $line = Get-Content $envPath | Where-Object { $_ -match "^[\s]*$Key[\s]*=" } | Select-Object -First 1
    if (-not $line) {
        Write-Host "  [DEBUG] $Key bulunamadi" -ForegroundColor Red
        return $null
    }
    $val = ($line -replace "^[\s]*$Key[\s]*=[\s]*", "").Trim().Trim('"')
    Write-Host "  [DEBUG] $Key = $val" -ForegroundColor Gray
    return $val
}

$token  = Get-EnvValue -Key "TELEGRAM_BOT_TOKEN"
$chatId = Get-EnvValue -Key "TELEGRAM_CHAT_ID"

$findings = @()

$k1 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt" -Pattern '"app_organizer_prefs"' -SimpleMatch
if ($k1) { $findings += "[K1] AllAppsDrawer ikinci SharedPreferences dosyasi kullaniliyor (satir $($k1.LineNumber))" }

$y1 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreenState.kt" -Pattern 'lowercase()' -SimpleMatch
if ($y1) { $findings += "[Y1] fuzzySearch locale duyarsiz lowercase() kullaniliyor (satir $($y1.LineNumber))" }

$y2 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt" -Pattern 'folderSearchCountdown' -SimpleMatch
if ($y2) { $findings += "[Y2] folderSearchCountdown race condition riski (satir $($y2.LineNumber))" }

$y3 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderTile.kt" -Pattern 'var swipeDy = 0f' -SimpleMatch
if ($y3) { $findings += "[Y3] FolderTile swipeDy non-state (satir $($y3.LineNumber))" }

$y4 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt" -Pattern 'fun isDefaultLauncher' -SimpleMatch
if ($y4) { $findings += "[Y4] SettingsScreen isDefaultLauncher() onbellege alinmamis (satir $($y4.LineNumber))" }

$o1 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreen.kt" -Pattern 'items(screenState.categories.filter' -SimpleMatch
if ($o1) { $findings += "[O1] AppListScreen kategori listesi her recomposition'da yeniden hesaplaniyor (satir $($o1.LineNumber))" }

$o2 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt" -Pattern '${app.packageName}_48_${iconPackPkg}' -SimpleMatch
if ($o2) { $findings += "[O2] AllApps drawer ikon cache anahtari lastUpdatedTime eksik (satir $($o2.LineNumber))" }

$d1 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreenComponents.kt" -Pattern 'itemHeightDp: androidx.compose.ui.unit.Dp = 56.dp' -SimpleMatch
if ($d1) { $findings += "[D1] Kullanilmayan itemHeightDp parametresi (satir $($d1.LineNumber))" }

$d3 = Select-String -Path "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt" -Pattern 'val isLoading = folders.isEmpty() && allApps.isEmpty()' -SimpleMatch
if ($d3) { $findings += "[D3] isLoading degiskeni kullanilmiyor (satir $($d3.LineNumber))" }

if ($findings.Count -eq 0) {
    $summary = "Temiz - bulgu yok"
} else {
    $summary = "$($findings.Count) bulgu tespit edildi"
}

$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("# Local Denetim Raporu - $timestamp")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("Sonuc: $summary")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("## Bulgular")
[void]$sb.AppendLine("")

if ($findings.Count -eq 0) {
    [void]$sb.AppendLine("Tum kontroller gecti.")
} else {
    foreach ($f in $findings) {
        [void]$sb.AppendLine("- $f")
    }
}

[void]$sb.AppendLine("")
[void]$sb.AppendLine("---")
[void]$sb.AppendLine("Denetim: Otomatik tarama | Kurallar: local_denetim_kurallari.md")

$report = $sb.ToString()

if (-not $DryRun) {
    [System.IO.File]::WriteAllText($raporPath, $report, [System.Text.Encoding]::UTF8)
    Write-Host "[audit.ps1] Rapor guncellendi: $raporPath" -ForegroundColor Green
    Write-Host "[audit.ps1] $summary" -ForegroundColor $(if ($findings.Count -eq 0) { "Green" } else { "Yellow" })
    foreach ($f in $findings) { Write-Host "  $f" -ForegroundColor Yellow }
} else {
    Write-Host "[audit.ps1] DryRun - degisiklik yapilmadi" -ForegroundColor Cyan
    Write-Host "[audit.ps1] $summary"
    foreach ($f in $findings) { Write-Host "  $f" -ForegroundColor Yellow }
}

if ($SendTelegram -and $token -and $chatId) {
    $msg = "Denetim $timestamp`n$summary"
    if ($findings.Count -gt 0) {
        $msg += "`nIlk 3 bulgu:`n"
        $findings | Select-Object -First 3 | ForEach-Object { $msg += "- $_`n" }
    }
    $url = "https://api.telegram.org/bot$token/sendMessage"
    $result = curl.exe -s -X POST $url -F "chat_id=$chatId" -F "text=$msg"
    Write-Host "[audit.ps1] Telegram sonuc: $result" -ForegroundColor Gray
    Write-Host "[audit.ps1] Telegram gonderildi." -ForegroundColor Green
} else {
    Write-Host "[audit.ps1] Telegram gonderimi atlandi (token=$token chatId=$chatId)." -ForegroundColor Yellow
}
