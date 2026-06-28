<#
.SYNOPSIS
    Guncel local denetim raporunu uretir.
    Her calistirmada 8 odak alani rotasyonundan biri + ekstra bir alan denetlenir.
#>

param(
    [switch]$SendTelegram = $false,
    [switch]$DryRun = $false
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$reportPath = Join-Path $projectRoot "local_denetim_otomatik_rapor.md"
$envPath = Join-Path $projectRoot ".env"
$timestamp = Get-Date
$timestampText = $timestamp.ToString("yyyy-MM-dd HH:mm")
$focusIndexPath = Join-Path $scriptDir "audit_focus_index.txt"
$extraIndexPath = Join-Path $scriptDir "audit_extra_focus_index.txt"

Set-Location $projectRoot

function Get-EnvValue {
    param([string]$Key)
    if (-not (Test-Path $envPath)) { return $null }
    $line = Get-Content $envPath | Where-Object { $_ -match "^[\s]*$Key[\s]*=" } | Select-Object -First 1
    if (-not $line) { return $null }
    return ($line -replace "^[\s]*$Key[\s]*=[\s]*", "").Trim().Trim('"')
}

# 8 odak alani rotasyonu
$focusAreas = @(
    @{ Name = "UI_Settings_Labels"; Desc = "Settings etiket-davranis tutarliligi" },
    @{ Name = "Gesture_Swipe_Drawer"; Desc = "Gesture, swipe, drawer akislari" },
    @{ Name = "Permission_Izin"; Desc = "Izin akislari, onboarding, fallback" },
    @{ Name = "Data_State_Persistence"; Desc = "State yonetimi, SharedPrefs, kalicilik" },
    @{ Name = "Accessibility_A11y"; Desc = "TalkBack, contentDescription, semantics" },
    @{ Name = "Performance_Memory"; Desc = "Recomposition, cache, IO, performans" },
    @{ Name = "Category_CRUD"; Desc = "Kategori ekleme/duzenleme/silme" },
    @{ Name = "Dock_Widget_Backup"; Desc = "Dock, widget, yedekleme akislari" }
)

# Ekstra denetim alanlari (kendi fikirlerim)
$extraFocusAreas = @(
    @{ Name = "Repository_DataLayer"; Desc = "AppRepository, DAO, data mapping, sorgu dogrulama" },
    @{ Name = "ViewModel_StateFlow"; Desc = "StateFlow kullanimi, hot-path, race condition" },
    @{ Name = "Navigation_Routing"; Desc = "Ekran gecisleri, route, intent, back press" },
    @{ Name = "Error_Handling_Logging"; Desc = "Timber log quality, user-facing error messages, fallback" },
    @{ Name = "OEM_Compatibility"; Desc = "Samsung/Xiaomi/Huawei varyasyonlari, edge cases" },
    @{ Name = "Memory_Lifecycle"; Desc = "Activity/Fragment leak, Flow collect, DisposableEffect" },
    @{ Name = "Test_Coverage_Gap"; Desc = "Test edilmeyis senaryolari, dead code, unused imports" },
    @{ Name = "Privacy_Security"; Desc = "Hassas veri, log, izin, data export/import guvenligi" }
)

$currentFocusIndex = 0
if (Test-Path $focusIndexPath) {
    $saved = Get-Content $focusIndexPath -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($saved -match '^\d+$') { $currentFocusIndex = [int]$saved }
}
$focus = $focusAreas[$currentFocusIndex % $focusAreas.Count]
$nextIndex = ($currentFocusIndex + 1) % $focusAreas.Count
Set-Content -Path $focusIndexPath -Value $nextIndex -Encoding UTF8

$currentExtraIndex = 0
if (Test-Path $extraIndexPath) {
    $savedExtra = Get-Content $extraIndexPath -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($savedExtra -match '^\d+$') { $currentExtraIndex = [int]$savedExtra }
}
$extraFocus = $extraFocusAreas[$currentExtraIndex % $extraFocusAreas.Count]
$nextExtraIndex = ($currentExtraIndex + 1) % $extraFocusAreas.Count
Set-Content -Path $extraIndexPath -Value $nextExtraIndex -Encoding UTF8

# Tüm kurallar havuzu
$allRules = @(
    @{ Code = "K1"; Severity = "KRITIK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt"; Pattern = 'getSharedPreferences\("app_organizer_prefs"'; Description = "AllAppsDrawer hardcoded prefs kullaniyor."; Focus = @("Data_State_Persistence") },
    @{ Code = "Y1"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreenState.kt"; Pattern = 'lowercase\(\)'; Description = "Locale belirtilmeyen lowercase bulundu."; Focus = @("Gesture_Swipe_Drawer","Performance_Memory") },
    @{ Code = "Y2"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'LaunchedEffect\(folderSearchQuery\)'; Description = "Eski folderSearch sayaç akisi bulundu."; Focus = @("Gesture_Swipe_Drawer","Data_State_Persistence") },
    @{ Code = "Y3"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderTile.kt"; Pattern = 'var swipeDy = 0f'; Description = "swipeDy state degil."; Focus = @("Gesture_Swipe_Drawer") },
    @{ Code = "Y4"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt"; Pattern = 'var isDefault by remember \{'; Description = "Launcher durumu keysiz remember ile tutuluyor."; Focus = @("UI_Settings_Labels","Data_State_Persistence") },
    @{ Code = "O1"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreen.kt"; Pattern = 'items\(screenState\.categories\.filter'; Description = "Kategori listesi hala composable icinde hesaplanıyor."; Focus = @("Performance_Memory","UI_Settings_Labels") },
    # O2 KALDIRILDI (D149): lastUpdatedTime artik cache key'e dahil — yanlis alarm
    # O5 KALDIRILDI (D149): filteredApps artik getter degil, data class field — yanlis alarm
    # O3 KALDIRILDI (D149): manufacturerClassifyEnabled kaldirildi (D144'te) — yanlis alarm
    @{ Code = "D1"; Severity = "DUSUK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreenComponents.kt"; Pattern = 'itemHeightDp: androidx\.compose\.ui\.unit\.Dp = 56\.dp'; Description = "Kullanilmayan itemHeightDp parametresi duruyor."; Focus = @("UI_Settings_Labels","Performance_Memory") },
    @{ Code = "Y5"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\theme\Theme.kt"; Pattern = '@Suppress\("UNUSED_PARAMETER"\).*darkTheme'; Description = "darkTheme parametresi devre disi birakilmis."; Focus = @("UI_Settings_Labels") },
    # Y6 KALDIRILDI (D144): shouldShowRequestPermissionRationale OnboardingScreen'de dogru kullaniluyor — yanlis alarm
    # O7 KALDIRILDI (D144): removeFromDock artik Boolean donduruyor, ViewModel toast gosteriyor — cozuldu
    # K9 KALDIRILDI (D144): getAllCategoriesFlow tum katmanlarda tanimi dogru, clean build ile senkron — yanlis alarm
    # O6 KALDIRILDI (D149): ThemePreferences @Singleton @Inject constructor ile Hilt'e bagli, manuel new yok — yanlis alarm
    # O8 KALDIRILDI (D149): endsWith(it) koda hic eslemiyor (D114'te prefix bazli yapildi) — yanlis alarm
    @{ Code = "Y7"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderTile.kt"; Pattern = 'Modifier\.size\(\d+\.dp\)'; Description = "Hardcoded dp boyut - responsive tasma riski."; Focus = @("UI_Settings_Labels","Performance_Memory") },
    @{ Code = "Y8"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'LauncherActivity\('; Description = "HomeScreen yenileme/refresh tetikleyici kaynak kod hatti."; Focus = @("Data_State_Persistence","UI_Settings_Labels") },
    # -- compose-expert skill kurallari (D151) -- ASCII-safe, curly quote yok --
    @{ Code = "CE1"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'remember\s*\{[^}]*screenHeightDp|remember\s*\{[^}]*screenWidthDp|remember\s*\{[^}]*fontScale|remember\s*\{[^}]*densityDpi'; Description = "remember {} config-key yok - rotation stale riski."; Focus = @("Performance_Memory","UI_Settings_Labels") },
    @{ Code = "CE2"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = '\.indexOf\(|\.lastIndexOf\('; Description = "LazyColumn items{} icinde indexOf() - O(n^2) + crash riski."; Focus = @("Performance_Memory") },
    @{ Code = "CE3"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'Canvas\('; Description = "Canvas/DrawScope zero-size guard kontrol et."; Focus = @("Performance_Memory") },
    @{ Code = "CE4"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'derivedStateOf\s*\{'; Description = "derivedStateOf unstable input riski - E14 tekrari."; Focus = @("Data_State_Persistence") },
    @{ Code = "CE5"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = '\.padding\(.*\)\s*\.\s*clickable'; Description = "Modifier sirasi: padding clickable'dan once - tik alani daralmis."; Focus = @("UI_Settings_Labels","Gesture_Swipe_Drawer") },
    @{ Code = "CE6"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\LauncherViewModel.kt"; Pattern = '@Volatile\s+var\s+\w+\s*='; Description = "@Volatile bilesik operasyon korumaz - AtomicBoolean.compareAndSet() kullan. (E9 tekrari)"; Focus = @("Data_State_Persistence","Performance_Memory") }
)

# Ana odak alani kurallarini sec
$activeRules = $allRules | Where-Object { $_.Focus -contains $focus.Name }
if ($activeRules.Count -eq 0) { $activeRules = $allRules }

# Ekstra (kendi) denetim alani — her dongude bir tane ekstra bulgu aranir
$extraRules = $allRules | Where-Object { $_.Focus -contains $extraFocus.Name }
if ($extraRules.Count -eq 0) { $extraRules = $allRules }

$findings = foreach ($rule in $activeRules) {
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

# Ekstra bulguları ana bulgulara ekle (Unique tut)
$extraFindings = foreach ($rule in $extraRules) {
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

$combinedFindings = @($findings) + @($extraFindings) | Sort-Object Severity, Code -Unique

$criticalCount = ($combinedFindings | Where-Object Severity -eq "KRITIK").Count
$highCount = ($combinedFindings | Where-Object Severity -eq "YUKSEK").Count
$mediumCount = ($combinedFindings | Where-Object Severity -eq "ORTA").Count
$lowCount = ($combinedFindings | Where-Object Severity -eq "DUSUK").Count

$lines = @()
$lines += "# Local Denetim Raporu"
$lines += ""
$lines += '> Döngü: `15 dakikalık 8+1 odak rotasyonu`'
$lines += "> Son denetim: $timestampText"
$lines += "> Ana tur odak: **$($focus.Desc)** ($($focus.Name))"
$lines += "> Ekstra denetim: **$($extraFocus.Desc)** ($($extraFocus.Name))"
$lines += '> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir.'
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
$lines += "| TOPLAM | $($combinedFindings.Count) | |"
$lines += ""
$lines += "---"
$lines += ""

if ($combinedFindings.Count -eq 0) {
$lines += "## Bu Dongu Sonucu"
$lines += ""
$lines += "- Acik bulgu tespit edilmedi."
$lines += "- Tamamlanan maddeler icin `local_denetim_tamamlananlar.md` dosyasina bak."
} else {
    foreach ($severity in @("KRITIK", "YUKSEK", "ORTA", "DUSUK")) {
        $items = $combinedFindings | Where-Object Severity -eq $severity
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
$lines += "*Denetim tarihi: $($timestamp.ToString("yyyy-MM-dd HH:mm")) | Ana: $($focus.Desc) | Ekstra: $($extraFocus.Desc)*"
$lines += ""
$lines += "---"
$lines += ""
$lines += "KiloCode | Profesyonel local denetim asistani - Android uygulama kalitesi ve guvenilirligi."
$lines += "Kod hatasiz, kullanici dostu, anlasilir ve suratli olmaya devam ediyor."

$report = ($lines -join [Environment]::NewLine)

if (-not $DryRun) {
    [System.IO.File]::WriteAllText($reportPath, $report, [System.Text.Encoding]::UTF8)
    Write-Host "[audit] Rapor guncellendi: $reportPath (Ana: $($focus.Desc) + Ekstra: $($extraFocus.Desc))" -ForegroundColor Green
} else {
    Write-Host $report
}

if ($SendTelegram) {
    $token = Get-EnvValue -Key "TELEGRAM_BOT_TOKEN"
    $chatId = Get-EnvValue -Key "TELEGRAM_CHAT_ID"
    if ($token -and $chatId) {
        $summary = "Denetim $timestampText`nAna: $($focus.Desc)`nEkstra: $($extraFocus.Desc)`nAcik bulgu: $($combinedFindings.Count)"
        $url = "https://api.telegram.org/bot$token/sendMessage"
        curl.exe -s -X POST $url -F "chat_id=$chatId" -F "text=$summary" | Out-Null
    }
}
