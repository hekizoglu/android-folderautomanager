[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDir = Join-Path $projectRoot "qa\reports"
if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir | Out-Null
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$reportFile = Join-Path $reportDir "logic-audit-fast-$(Get-Date -Format 'yyyyMMdd-HHmmss').md"

$rules = @(
    @{
        Id = "LS001"
        Severity = "P1"
        Description = "combine ekran state'i uretiyor ama selected/ephemeral state combine'a dahil degil"
        Path = "app\src\main\java\com\armutlu\apporganizer\presentation\viewmodel\AppListViewModel.kt"
        Pattern = 'combine\(_apps,\s*_categories,\s*_searchQuery,\s*_selectedCategory,\s*_sortBy,\s*_showSystem'
    },
    @{
        Id = "LS002"
        Severity = "P1"
        Description = "UI aksiyonu screenState.selectedApps snapshot'i ile toplu islem yapiyor"
        Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreen.kt"
        Pattern = 'screenState\.selectedApps\.toList\(\)'
    },
    @{
        Id = "LS003"
        Severity = "P1"
        Description = "launchApp analytics veya usage update yapmadan activity baslatiyor"
        Path = "app\src\main\java\com\armutlu\apporganizer\presentation\viewmodel\AppListViewModel.kt"
        Pattern = 'fun launchApp\(app: AppInfo\)'
    },
    @{
        Id = "LS004"
        Severity = "P1"
        Description = "syncInstalledApps mevcut uygulamalari update etmeden yalnizca insert-delete yapiyor"
        Path = "app\src\main\java\com\armutlu\apporganizer\data\repository\AppRepository.kt"
        Pattern = 'suspend fun syncInstalledApps\('
    },
    @{
        Id = "LS005"
        Severity = "P1"
        Description = "bildirim mesaji 'bugun/en cok actigin' diyor ama usageCount bazli secim yapiyor"
        Path = "app\src\main\java\com\armutlu\apporganizer\workers\SmartInsightWorker.kt"
        Pattern = 'maxByOrNull \{ it\.usageCount \}'
    },
    @{
        Id = "LS006"
        Severity = "P1"
        Description = "bildirim tap extra'si veriliyor ama uygulama tarafinda route olarak tuketilmiyor olabilir"
        Path = "app\src\main\java\com\armutlu\apporganizer\workers\SmartInsightWorker.kt"
        Pattern = 'putExtra\("open_tab", "dashboard"\)'
    },
    @{
        Id = "LS007"
        Severity = "P2"
        Description = "cancelUniqueWork hemen ardindan KEEP ile enqueue edilmis"
        Path = "app\src\main\java\com\armutlu\apporganizer\workers\SmartInsightWorker.kt"
        Pattern = 'cancelUniqueWork\(WORK_NAME\)'
    },
    @{
        Id = "LS008"
        Severity = "P2"
        Description = "yeni/onerilen secimi explicit siralama olmadan first() ile yapiliyor"
        Path = "app\src\main\java\com\armutlu\apporganizer\workers\SmartInsightWorker.kt"
        Pattern = 'newApps\.first\(\)'
    },
    @{
        Id = "LS009"
        Severity = "P2"
        Description = "uygulama context'inden NEW_TASK olmadan activity baslatiliyor olabilir"
        Path = "app\src\main\java\com\armutlu\apporganizer\utils\PackageManagerHelper.kt"
        Pattern = 'context\.startActivity\(launchIntent\)'
    },
    @{
        Id = "LS010"
        Severity = "P2"
        Description = "biyometrik koruma hata halinde fail-open davraniyor olabilir"
        Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt"
        Pattern = 'biometricUnlocked = true'
    }
)

$findings = @()
foreach ($rule in $rules) {
    $filePath = Join-Path $projectRoot $rule.Path
    if (-not (Test-Path $filePath)) { continue }
    $match = Select-String -Path $filePath -Pattern $rule.Pattern
    if ($match) {
        $findings += [PSCustomObject]@{
            Severity = $rule.Severity
            Id = $rule.Id
            Description = $rule.Description
            Path = $rule.Path
            Line = $match[0].LineNumber
        }
    }
}

$sorted = $findings | Sort-Object Severity, Id

$lines = @()
$lines += "# Logic Audit Fast"
$lines += ""
$lines += "- Tarih: $timestamp"
$lines += "- Toplam bulgu: $($sorted.Count)"
$lines += "- Kural seti: qa/logic-rules.md"
$lines += ""

if ($sorted.Count -eq 0) {
    $lines += "Bulgu bulunamadi."
} else {
    foreach ($finding in $sorted) {
        $lines += "- $($finding.Severity) | $($finding.Id) | $($finding.Path):$($finding.Line) | $($finding.Description)"
    }
}

[System.IO.File]::WriteAllLines($reportFile, $lines, [System.Text.Encoding]::UTF8)

Write-Host "[logic-audit-fast] Bulgu: $($sorted.Count)" -ForegroundColor Cyan
Write-Host "[logic-audit-fast] Rapor: $reportFile" -ForegroundColor Green

if (($sorted | Where-Object { $_.Severity -eq "P1" }).Count -gt 0) {
    exit 2
}

exit 0
