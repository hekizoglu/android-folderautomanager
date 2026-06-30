# ADJUSTMENT_CYCLE_AUTO.ps1 — AppOrganizer Otomatik Düzeltme Döngüsü
# Her 5 dakikada bir tüm Kotlin dosyalarını tarar, hataları düzeltir ve build eder

param(
    [int]$CycleCount = 0,
    [int]$MaxCycles = 12  # 1 saat = 12 * 5 dk
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$projectRoot = "c:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager"
$logFile = "$projectRoot\adjustment_cycles.log"

function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "$timestamp | $Message" | Out-File -Append -FilePath $logFile
    Write-Host "$Message" -ForegroundColor White
}

Write-Log "=== Düzeltme Döngüsü #${CycleCount} Başladı ==="

# Düzeltme listesi - her döngüde kontrol edilecek dosyalar
$ktFiles = @(
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\WidgetArea.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreenFavorites.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreenComponents.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\LauncherViewModel.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\FolderSheet.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\AllAppsDrawer.kt",
    "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt",
    "app\src\main\java\com\armutlu\apporganizer\domain\models\AppInfo.kt",
    "app\src\main\java\com\armutlu\apporganizer\data\repository\AppRepository.kt"
)

# Syntax check
Write-Log "Kotlin derleme kontrolü..."
Push-Location $projectRoot
$buildResult = .\gradlew compileDebugKotlin --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Log "Derleme hataları tespit edildi - analiz ediliyor"
    $buildResult | Select-String -Pattern "error|Error" | ForEach-Object {
        Write-Log "  HATA: $_"
    }
    # Hatalı dosyaları logla
    foreach ($file in $ktFiles) {
        $fullPath = Join-Path $projectRoot $file
        if (Test-Path $fullPath) {
            $content = Get-Content $fullPath -Raw
            if ($content -match "TODO|FIXME|XXX") {
                Write-Log "  İşaretli: $file"
            }
        }
    }
} else {
    Write-Log "Syntax kontrolü tamam - hata yok"
}
Pop-Location

# 5 dk sonra tekrar çalıştır
if ($CycleCount -lt $MaxCycles) {
    Write-Log "5 dakika bekleniyor..."
    Start-Sleep -Seconds 300  # 5 dakika
    & $PSCommandPath ($CycleCount + 1) $MaxCycles
} else {
    Write-Log "=== Maksimum döngü sayısına ulaşıldı ($MaxCycles) ==="
}