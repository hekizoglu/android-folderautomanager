<#
.SYNOPSIS
    Guncel local denetim raporunu uretir.
#>

param(
    [switch]$SendTelegram = $false,
    [switch]$DryRun = $false
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$reportPath = Join-Path $projectRoot "local_denetim_raporu.md"
$envPath = Join-Path $projectRoot ".env"
$timestamp = Get-Date
$timestampText = $timestamp.ToString("yyyy-MM-dd HH:mm")

Set-Location $projectRoot

function Get-EnvValue {
    param([string]$Key)
    if (-not (Test-Path $envPath)) { return $null }
    $line = Get-Content $envPath | Where-Object { $_ -match "^[\s]*$Key[\s]*=" } | Select-Object -First 1
    if (-not $line) { return $null }
    return ($line -replace "^[\s]*$Key[\s]*=[\s]*", "").Trim().Trim('"')
}

$rules = @(
    @{ Code = "K1"; Severity = "KRITIK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt"; Pattern = 'getSharedPreferences\("app_organizer_prefs"'; Description = "AllAppsDrawer hardcoded prefs kullaniyor." },
    @{ Code = "Y1"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreenState.kt"; Pattern = 'lowercase\(\)'; Description = "Locale belirtilmeyen lowercase bulundu." },
    @{ Code = "Y2"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'LaunchedEffect\(folderSearchQuery\)'; Description = "Eski folderSearch sayaç akisi bulundu." },
    @{ Code = "Y3"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderTile.kt"; Pattern = 'var swipeDy = 0f'; Description = "swipeDy state degil." },
    @{ Code = "Y4"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt"; Pattern = 'var isDefault by remember \{'; Description = "Launcher durumu keysiz remember ile tutuluyor." },
    @{ Code = "O1"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreen.kt"; Pattern = 'items\(screenState\.categories\.filter'; Description = "Kategori listesi hala composable icinde hesaplanıyor." },
    @{ Code = "O2"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt"; Pattern = '\$\{app\.packageName\}_48_\$iconPackPkg'; Description = "Icon cache key icinde lastUpdatedTime eksik." },
    @{ Code = "O3"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\domain\usecase\classify\AppClassifier.kt"; Pattern = 'var manufacturerClassifyEnabled'; Description = "AppClassifier global mutable state tasiyor." },
    @{ Code = "O5"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreenState.kt"; Pattern = 'val filteredApps: List<AppInfo>\s*[\r\n]+\s*get\(\)'; Description = "filteredApps getter bazli hesaplanıyor." },
    @{ Code = "D1"; Severity = "DUSUK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreenComponents.kt"; Pattern = 'itemHeightDp: androidx\.compose\.ui\.unit\.Dp = 56\.dp'; Description = "Kullanilmayan itemHeightDp parametresi duruyor." }
)

$findings = foreach ($rule in $rules) {
    $filePath = Join-Path $projectRoot $rule.Path
    if (-not (Test-Path $filePath)) { continue }
    $match = Select-String -Path $filePath -Pattern $rule.Pattern
    if ($match) {
        [PSCustomObject]@{
            Code = $rule.Code
            Severity = $rule.Severity
            Description = $rule.Description
            Path = $rule.Path
            Line = $match[0].LineNumber
        }
    }
}

$criticalCount = ($findings | Where-Object Severity -eq "KRITIK").Count
$highCount = ($findings | Where-Object Severity -eq "YUKSEK").Count
$mediumCount = ($findings | Where-Object Severity -eq "ORTA").Count
$lowCount = ($findings | Where-Object Severity -eq "DUSUK").Count

$lines = @()
$lines += "# Local Denetim Raporu"
$lines += ""
$lines += "> Son dongu: ``$timestampText``"
$lines += "> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir."
$lines += ""
$lines += "---"
$lines += ""
$lines += "## Denetim Ozeti"
$lines += ""
$lines += "| Oncelik | Sayi | Aciklama |"
$lines += "|---------|------|----------|"
$lines += "| KRITIK | $criticalCount | Acik kritik bulgu |"
$lines += "| YUKSEK | $highCount | Acik yuksek bulgu |"
$lines += "| ORTA | $mediumCount | Acik orta bulgu |"
$lines += "| DUSUK | $lowCount | Acik dusuk bulgu |"
$lines += "| TOPLAM | $($findings.Count) | |"
$lines += ""
$lines += "---"
$lines += ""

if ($findings.Count -eq 0) {
$lines += "## Bu Dongu Sonucu"
$lines += ""
$lines += "- Acik bulgu tespit edilmedi."
$lines += "- Tamamlanan maddeler icin `local_denetim_tamamlananlar.md` dosyasina bak."
} else {
    foreach ($severity in @("KRITIK", "YUKSEK", "ORTA", "DUSUK")) {
        $items = $findings | Where-Object Severity -eq $severity
        if ($items.Count -eq 0) { continue }
        $lines += "## $severity"
        $lines += ""
        foreach ($item in $items) {
            $lines += "- $($item.Code) | `"$($item.Path):$($item.Line)`" | $($item.Description)"
        }
        $lines += ""
    }
}

$lines += "---"
$lines += ""
$lines += "Not: Bu script agirlikli olarak statik ve otomatik taranabilen kurallari kontrol eder."
$lines += "Buton adi ile yaptigi isin tutarliligi gibi anlamsal UI denetimleri manuel veya yari otomatik kod okumasi gerektirir."
$lines += "Manuel semantik tur icin `local_denetim_manuel_checklist.md` kullan."
$lines += ""
$lines += "---"
$lines += ""
$lines += "*Denetim tarihi: $($timestamp.ToString("yyyy-MM-dd"))*"

$report = ($lines -join [Environment]::NewLine)

if (-not $DryRun) {
    [System.IO.File]::WriteAllText($reportPath, $report, [System.Text.Encoding]::UTF8)
    Write-Host "[audit] Rapor guncellendi: $reportPath" -ForegroundColor Green
} else {
    Write-Host $report
}

if ($SendTelegram) {
    $token = Get-EnvValue -Key "TELEGRAM_BOT_TOKEN"
    $chatId = Get-EnvValue -Key "TELEGRAM_CHAT_ID"
    if ($token -and $chatId) {
        $summary = "Denetim $timestampText - Acik bulgu: $($findings.Count)"
        $url = "https://api.telegram.org/bot$token/sendMessage"
        curl.exe -s -X POST $url -F "chat_id=$chatId" -F "text=$summary" | Out-Null
    }
}
