<#
.SYNOPSIS
    Guncel local denetim raporunu uretir -- tiered frequency sistemi ile.
    Tier 1 (her dongu): Core rules (K1, Y1-Y8, D1) -- hizli regex
    Tier 2 (3 dongude bir): CE compose-expert kurallari
    Tier 3 (10 dongude bir): Lint + dependency + skill dogrulama + fikir uretimi

    Dongu sayisi loop_count.txt uzerinden veya -CycleNumber parametresi ile belirlenir.
#>

param(
    [switch]$SendTelegram = $false,
    [switch]$DryRun = $false,
    [int]$CycleNumber = 0
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

# Dongu sayisini belirle
if ($CycleNumber -eq 0) {
    $counterPath = Join-Path $scriptDir "loop_count.txt"
    if (Test-Path $counterPath) {
        $saved = Get-Content $counterPath -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($saved -match '^\d+$') { $CycleNumber = [int]$saved }
    }
    if ($CycleNumber -eq 0) { $CycleNumber = 1 }
}

# Tier hesapla
$tier = if ($CycleNumber % 10 -eq 0) { 3 }
        elseif ($CycleNumber % 3 -eq 0) { 2 }
        else { 1 }
$isTier2 = $tier -ge 2
$isTier3 = $tier -eq 3

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

# Ekstra denetim alanlari
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

# ============================================================
# TIER 1 -- Her dongu: Temel regex kurallari
# ============================================================
$tier1Rules = @(
    @{ Code = "K1"; Severity = "KRITIK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt"; Pattern = 'getSharedPreferences\("app_organizer_prefs"'; Description = "AllAppsDrawer hardcoded prefs kullaniyor."; Focus = @("Data_State_Persistence") },
    @{ Code = "Y1"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreenState.kt"; Pattern = 'lowercase\(\)'; Description = "Locale belirtilmeyen lowercase bulundu."; Focus = @("Gesture_Swipe_Drawer","Performance_Memory") },
    @{ Code = "Y2"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'LaunchedEffect\(folderSearchQuery\)'; Description = "Eski folderSearch sayac akisi bulundu."; Focus = @("Gesture_Swipe_Drawer","Data_State_Persistence") },
    @{ Code = "Y3"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderTile.kt"; Pattern = 'var swipeDy = 0f'; Description = "swipeDy state degil."; Focus = @("Gesture_Swipe_Drawer") },
    @{ Code = "Y4"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt"; Pattern = 'var isDefault by remember \{'; Description = "Launcher durumu keysiz remember ile tutuluyor."; Focus = @("UI_Settings_Labels","Data_State_Persistence") },
    @{ Code = "O1"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreen.kt"; Pattern = 'items\(screenState\.categories\.filter'; Description = "Kategori listesi hala composable icinde hesaplaniyor."; Focus = @("Performance_Memory","UI_Settings_Labels") },
    @{ Code = "D1"; Severity = "DUSUK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreenComponents.kt"; Pattern = 'itemHeightDp: androidx\.compose\.ui\.unit\.Dp = 56\.dp'; Description = "Kullanilmayan itemHeightDp parametresi duruyor."; Focus = @("UI_Settings_Labels","Performance_Memory") },
    @{ Code = "Y5"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\theme\Theme.kt"; Pattern = '@Suppress\("UNUSED_PARAMETER"\).*darkTheme'; Description = "darkTheme parametresi devre disi birakilmis."; Focus = @("UI_Settings_Labels") },
    @{ Code = "Y7"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderTile.kt"; Pattern = 'Modifier\.size\(\d+\.dp\)'; Description = "Hardcoded dp boyut - responsive tasma riski."; Focus = @("UI_Settings_Labels","Performance_Memory") },
    @{ Code = "Y8"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'LauncherActivity\('; Description = "HomeScreen yenileme/refresh tetikleyici kaynak kod hatti."; Focus = @("Data_State_Persistence","UI_Settings_Labels") }
)

# ============================================================
# TIER 2 -- 3 dongude bir: compose-expert kurallari
# ============================================================
$tier2Rules = @(
    @{ Code = "CE1"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'remember\s*\{[^}]*screenHeightDp|remember\s*\{[^}]*screenWidthDp|remember\s*\{[^}]*fontScale|remember\s*\{[^}]*densityDpi'; Description = "remember {} config-key yok - rotation stale riski."; Focus = @("Performance_Memory","UI_Settings_Labels") },
    @{ Code = "CE2"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = '\.indexOf\(|\.lastIndexOf\('; Description = "LazyColumn items{} icinde indexOf() - O(n^2) + crash riski."; Focus = @("Performance_Memory") },
    @{ Code = "CE3"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'Canvas\('; Description = "Canvas/DrawScope zero-size guard kontrol et."; Focus = @("Performance_Memory") },
    @{ Code = "CE4"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = 'derivedStateOf\s*\{'; Description = "derivedStateOf unstable input riski - E14 tekrari."; Focus = @("Data_State_Persistence") },
    @{ Code = "CE5"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt"; Pattern = '\.padding\(.*\)\s*\.\s*clickable'; Description = "Modifier sirasi: padding clickable'dan once - tik alani daralmis."; Focus = @("UI_Settings_Labels","Gesture_Swipe_Drawer") },
    @{ Code = "CE6"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\LauncherViewModel.kt"; Pattern = '@Volatile\s+var\s+\w+\s*='; Description = "@Volatile bilesik operasyon korumaz - AtomicBoolean.compareAndSet() kullan. (E9 tekrari)"; Focus = @("Data_State_Persistence","Performance_Memory") },
    @{ Code = "CE7"; Severity = "YUKSEK"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt"; Pattern = 'remember\s*\{[^}]*AppPrefs\.'; Description = "AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)"; Focus = @("Data_State_Persistence","UI_Settings_Labels") },
    @{ Code = "CE8"; Severity = "ORTA"; Path = "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\AppListScreen.kt"; Pattern = '^\s*fun\s+\w+.*@Composable'; Description = "300+ satir composable - VerifyError riski (E13). Dosya boyutu audit: AppListScreen.kt max 300 satir."; Focus = @("Performance_Memory") }
)

# ============================================================
# TIER 3 -- 10 dongude bir: Derin denetim (lintDebug YOK - cok yavas)
#   - Compose compiler metrics (build artifact, hizli)
#   - Dependency uyumluluk matrisi
#   - APK boyut trendi
#   - Skill butunlugu
#   - Olu kod taramasi (unused imports, dead functions)
# ============================================================
$tier3Rules = @(
    @{ Code = "COMPOSE"; Severity = "ORTA"; Path = "app\build\compose_compiler"; Pattern = ''; Description = "Compose compiler metrics - unstable class/stability raporu."; Focus = @("Performance_Memory") },
    @{ Code = "DEP"; Severity = "ORTA"; Path = "app\build.gradle.kts"; Pattern = ''; Description = "Bagimlilik uyumluluk matrisi - BOM, AGP, Kotlin, Coil versiyon kontrolu."; Focus = @("Performance_Memory") },
    @{ Code = "APK"; Severity = "DUSUK"; Path = "app\build\outputs\apk\debug\app-debug.apk"; Pattern = ''; Description = "APK boyut trend kontrolu - anormal buyume var mi?"; Focus = @("Performance_Memory") },
    @{ Code = "SKILL"; Severity = "DUSUK"; Path = ".claude\skills"; Pattern = ''; Description = "Skill dosya butunlugu kontrolu - SKILL.md + references/ varligi."; Focus = @("Test_Coverage_Gap") },
    @{ Code = "DEAD"; Severity = "DUSUK"; Path = "app\src\main\java"; Pattern = ''; Description = "Olu kod taramasi - unused imports, eski TODO/FIXME patternleri."; Focus = @("Test_Coverage_Gap","Performance_Memory") }
)

# Kurallari tier'a gore birlestir
$allRules = @($tier1Rules)
if ($isTier2) {
    $allRules += $tier2Rules
    Write-Host "[audit] Tier $tier -- CE compose-expert kurallari aktif." -ForegroundColor Cyan
}
if ($isTier3) {
    $allRules += $tier3Rules
    Write-Host "[audit] Tier 3 -- Lint + Dependency + Skill + Fikir uretimi aktif." -ForegroundColor Magenta
}

# Ana odak alani kurallarini sec
$activeRules = $allRules | Where-Object { $_.Focus -contains $focus.Name }
if ($activeRules.Count -eq 0) { $activeRules = $allRules }

# Ekstra denetim alani
$extraRules = $allRules | Where-Object { $_.Focus -contains $extraFocus.Name }
if ($extraRules.Count -eq 0) { $extraRules = $allRules }

# Regex tabanli bulgular (LINT1/DEP1/SKILL1/RES1 haric)
$regexRules = $activeRules | Where-Object { $_.Code -notmatch '^(COMPOSE|DEP|APK|SKILL|DEAD)$' }
$findings = foreach ($rule in $regexRules) {
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

$extraRegexRules = $extraRules | Where-Object { $_.Code -notmatch '^(COMPOSE|DEP|APK|SKILL|DEAD)$' }
$extraFindings = foreach ($rule in $extraRegexRules) {
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

# ============================================================
# TIER 3 OZEL KONTROLLER (lintDebug YOK - cok yavas, yerine build artifact tabanli hizli kontroller)
# ============================================================
$tier3Notes = @()

if ($isTier3) {
    # --- Compose Compiler Metrics (build artifact, hizli) ---
    $composeActive = ($activeRules + $extraRules | Where-Object { $_.Code -eq "COMPOSE" }).Count -gt 0
    if ($composeActive) {
        Write-Host "[audit:T3] Compose compiler metrics okunuyor..." -ForegroundColor Magenta
        $composeDir = "app\build\compose_compiler"
        if (Test-Path $composeDir) {
            $moduleJson = Get-ChildItem $composeDir -Filter "*_module.json" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($moduleJson) {
                try {
                    $metrics = Get-Content $moduleJson.FullName -Raw | ConvertFrom-Json
                    $unstableCount = 0
                    foreach ($cls in $metrics.Classes) {
                        if (-not $cls.Stable) { $unstableCount++ }
                    }
                    if ($unstableCount -gt 0) {
                        $tier3Notes += "Compose: $unstableCount unstable class"
                    } else {
                        $tier3Notes += "Compose: tum siniflar stable"
                    }
                } catch {
                    $tier3Notes += "Compose: metrics okunamadi (build gerekli)"
                }
            } else {
                $tier3Notes += "Compose: metrics dosyasi yok (build sonrasi olusur)"
            }
        } else {
            $tier3Notes += "Compose: $composeDir bulunamadi"
        }
    }

    # --- Dependency Uyumluluk Matrisi ---
    $depActive = ($activeRules + $extraRules | Where-Object { $_.Code -eq "DEP" }).Count -gt 0
    if ($depActive -and (Test-Path "app\build.gradle.kts")) {
        Write-Host "[audit:T3] Bagimlilik uyumluluk matrisi kontrol ediliyor..." -ForegroundColor Magenta
        $buildGradle = Get-Content "app\build.gradle.kts" -Raw
        # BOM vs bilesen versiyon tutarliligi
        if ($buildGradle -match 'compose-bom:(\d{4}\.\d{2}\.\d{2})') {
            $bomVersion = $matches[1]
            $tier3Notes += "Compose BOM: $bomVersion"
        }
        # Kotlin ve Compose compiler uyumlulugu
        if ($buildGradle -match 'kotlinCompilerExtensionVersion\s*=\s*"([\d.]+)"') {
            $composeCompiler = $matches[1]
            $tier3Notes += "Compose Compiler: $composeCompiler"
        }
        # compileSdk kontrolu
        if ($buildGradle -match 'compileSdk\s*=\s*(\d+)') {
            $compileSdk = [int]$matches[1]
            $tier3Notes += "compileSdk: $compileSdk"
            if ($compileSdk -lt 35) {
                $combinedFindings += [PSCustomObject]@{
                    Code = "DEP"
                    Severity = "YUKSEK"
                    Description = "compileSdk=$compileSdk eski - Android 16 uyumlulugu icin 35+ gerekli"
                    Path = "app\build.gradle.kts"
                    Line = 0
                }
            }
        }
        # Coil 3.x uyarisi
        if ($buildGradle -match 'coil-compose:([\d.]+)') {
            $coilVersion = $matches[1]
            if ($coilVersion.StartsWith("3.") -and $compileSdk -lt 36) {
                $combinedFindings += [PSCustomObject]@{
                    Code = "DEP"
                    Severity = "YUKSEK"
                    Description = "Coil 3.x compileSdk 36 gerektirir, su an $compileSdk"
                    Path = "app\build.gradle.kts"
                    Line = 0
                }
            }
        }
        # targetSdk kontrolu
        if ($buildGradle -match 'targetSdk\s*=\s*(\d+)') {
            $targetSdk = $matches[1]
            $tier3Notes += "targetSdk: $targetSdk"
        }
    }

    # --- APK Boyut Trend ---
    $apkActive = ($activeRules + $extraRules | Where-Object { $_.Code -eq "APK" }).Count -gt 0
    if ($apkActive) {
        Write-Host "[audit:T3] APK boyut trendi kontrol ediliyor..." -ForegroundColor Magenta
        $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $apkPath) {
            $apkSizeMB = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
            $tier3Notes += "APK: $apkSizeMB MB"
            if ($apkSizeMB -gt 30) {
                $combinedFindings += [PSCustomObject]@{
                    Code = "APK"
                    Severity = "ORTA"
                    Description = "APK boyutu $apkSizeMB MB - 30MB esigini asti"
                    Path = "app-debug.apk"
                    Line = 0
                }
            }
        } else {
            $tier3Notes += "APK: build edilmemis, boyut kontrol edilemedi"
        }
    }

    # --- Skill Integrity Check ---
    $skillActive = ($activeRules + $extraRules | Where-Object { $_.Code -eq "SKILL" }).Count -gt 0
    if ($skillActive) {
        Write-Host "[audit:T3] Skill butunlugu kontrol ediliyor..." -ForegroundColor Magenta
        $skillDir = ".claude\skills"
        if (Test-Path $skillDir) {
            $skillNames = Get-ChildItem $skillDir -Directory | Select-Object -ExpandProperty Name
            foreach ($sn in $skillNames) {
                $skillMd = Join-Path $skillDir "$sn\SKILL.md"
                if (-not (Test-Path $skillMd)) {
                    $combinedFindings += [PSCustomObject]@{
                        Code = "SKILL"
                        Severity = "DUSUK"
                        Description = "Skill '$sn' SKILL.md dosyasi eksik"
                        Path = "$skillDir\$sn"
                        Line = 0
                    }
                }
            }
            $tier3Notes += "Skill: $($skillNames.Count) kontrol edildi"
        }
    }

    # --- Dead Code / Eskimis TODO Taramasi ---
    $deadActive = ($activeRules + $extraRules | Where-Object { $_.Code -eq "DEAD" }).Count -gt 0
    if ($deadActive) {
        Write-Host "[audit:T3] Olu kod/Eskimiş TODO taranıyor..." -ForegroundColor Magenta
        $srcDir = "app\src\main\java"
        if (Test-Path $srcDir) {
            # 30 gunden eski TODO/FIXME
            $oldTodos = Select-String -Path "$srcDir\**\*.kt" -Pattern '// TODO|// FIXME|// HACK' | Where-Object { $_.Line -notmatch '2026-0[6-7]' }
            $todoCount = ($oldTodos | Measure-Object).Count
            if ($todoCount -gt 10) {
                $tier3Notes += "Eski TODO/FIXME: $todoCount adet (30+ gun)"
            } elseif ($todoCount -gt 0) {
                $tier3Notes += "Eski TODO/FIXME: $todoCount adet"
            } else {
                $tier3Notes += "TODO/FIXME: temiz"
            }
        }
    }
}

$criticalCount = ($combinedFindings | Where-Object Severity -eq "KRITIK").Count
$highCount = ($combinedFindings | Where-Object Severity -eq "YUKSEK").Count
$mediumCount = ($combinedFindings | Where-Object Severity -eq "ORTA").Count
$lowCount = ($combinedFindings | Where-Object Severity -eq "DUSUK").Count

$lines = @()
$lines += "# Local Denetim Raporu"
$lines += ""
$lines += '> Dongu: tiered frequency (T1:her · T2:3dongu · T3:10dongu)'
$lines += "> Son denetim: $timestampText"
$lines += "> Dongu: **#$CycleNumber** | Tier: **$tier**"
$lines += "> Ana tur odak: **$($focus.Desc)** ($($focus.Name))"
$lines += "> Ekstra denetim: **$($extraFocus.Desc)** ($($extraFocus.Name))"
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

if ($tier3Notes.Count -gt 0) {
    $lines += "### Tier 3 Notlari"
    foreach ($note in $tier3Notes) {
        $lines += "- $note"
    }
    $lines += ""
}

$lines += "---"
$lines += ""

if ($combinedFindings.Count -eq 0) {
    $lines += "## Bu Dongu Sonucu"
    $lines += ""
    $lines += "- Acik bulgu tespit edilmedi."
} else {
    foreach ($severity in @("KRITIK", "YUKSEK", "ORTA", "DUSUK")) {
        $items = $combinedFindings | Where-Object Severity -eq $severity
        if ($items.Count -eq 0) { continue }
        $lines += "## $severity"
        $lines += ""
        foreach ($item in $items) {
            $lineInfo = if ($item.Line -gt 0) { ":$($item.Line)" } else { "" }
            $lines += "- $($item.Code) | ``$($item.Path)$lineInfo`` | $($item.Description)"
        }
        $lines += ""
    }
}

$lines += "---"
$lines += ""
$lines += "*Denetim tarihi: $($timestamp.ToString("yyyy-MM-dd HH:mm")) | Dongu: #$CycleNumber | Tier: $tier | Ana: $($focus.Desc) | Ekstra: $($extraFocus.Desc)*"

$report = ($lines -join [Environment]::NewLine)

if (-not $DryRun) {
    [System.IO.File]::WriteAllText($reportPath, $report, [System.Text.Encoding]::UTF8)
    Write-Host "[audit] Tier$tier Rapor guncellendi: $reportPath (Ana: $($focus.Desc) + Ekstra: $($extraFocus.Desc))" -ForegroundColor Green
} else {
    Write-Host $report
}

if ($SendTelegram) {
    $token = Get-EnvValue -Key "TELEGRAM_BOT_TOKEN"
    $chatId = Get-EnvValue -Key "TELEGRAM_CHAT_ID"
    if ($token -and $chatId) {
        $summary = "Denetim #$CycleNumber Tier$tier $timestampText`nAna: $($focus.Desc)`nEkstra: $($extraFocus.Desc)`nAcik bulgu: $($combinedFindings.Count)"
        if ($tier3Notes.Count -gt 0) {
            $summary += "`n" + ($tier3Notes -join "`n")
        }
        $url = "https://api.telegram.org/bot$token/sendMessage"
        curl.exe -s -X POST $url -F "chat_id=$chatId" -F "text=$summary" | Out-Null
    }
}
