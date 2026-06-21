<#
.SYNOPSIS
    cycle.ps1 — AppOrganizer döngü otomasyon orchestrator'ı.

.DESCRIPTION
    Bir döngünün rutin adımlarını tek komutta yürütür:
      1. Encoding kontrolü (fix_encoding.py --scan)
      2. AppClassifier duplicate kontrolü (check_duplicates.py)
      3. Build (her -BuildEvery döngüde bir; -Build ile zorla)
      4. Git add + commit + push origin main
      5. Telegram bildirim (+ APK her build'de)

    CLAUDE.md §1 "Döngü Otomasyonu" akışını uygular.
    Döngü ritmi: her döngü commit+push · her 6 döngü build+APK · her 18 döngü emülatör (manuel).

.PARAMETER Message
    Commit mesajı + Telegram bildirimi (zorunlu).

.PARAMETER CycleNum
    Döngü numarası — build ve test ritmini belirler.

.PARAMETER Build
    Bu döngüde build'i zorla (ritimden bağımsız).

.PARAMETER NoPush
    Sadece commit, push etme (test için).

.PARAMETER BuildEvery
    Kaç döngüde bir build (varsayılan 6).

.EXAMPLE
    .\scripts\cycle.ps1 -Message "AppClassifier +60 oyun paketi" -CycleNum 85

.EXAMPLE
    .\scripts\cycle.ps1 -Message "BUILD #18 - widget resize" -CycleNum 90 -Build
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Message,
    [int]$CycleNum = 0,
    [switch]$Build,
    [switch]$NoPush,
    [int]$BuildEvery = 6,
    [string]$Classifier = "app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/AppClassifier.kt"
)

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot
$python = "python3"
if (-not (Get-Command python3 -ErrorAction SilentlyContinue)) { $python = "python" }

function Step($n, $t) { Write-Host "`n[$n] $t" -ForegroundColor Yellow }

# Context doluluk kontrolu — flag varsa 15 dk bekle, sonra devam et
& "$scriptDir\check_context.ps1"

# Build gerekli mi?
$doBuild = $Build -or ($CycleNum -gt 0 -and $BuildEvery -gt 0 -and ($CycleNum % $BuildEvery -eq 0))

Write-Host "═══════════════════════════════════════════" -ForegroundColor DarkCyan
Write-Host " DÖNGÜ $CycleNum — cycle.ps1" -ForegroundColor Cyan
Write-Host " Build: $(if($doBuild){'EVET'}else{'hayır'})  |  Push: $(if($NoPush){'hayır'}else{'EVET'})" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════" -ForegroundColor DarkCyan

# --- 1. Encoding kontrolü ---
Step 1 "Encoding taraması..."
& $python "$scriptDir\fix_encoding.py" --scan "app/src" 2>$null
# --scan yazmaz; sorun varsa elle fix_encoding.py <dosya> çalıştır

# --- 2. AppClassifier duplicate ---
if (Test-Path $Classifier) {
    Step 2 "AppClassifier duplicate kontrolü..."
    & $python "$scriptDir\check_duplicates.py" $Classifier
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Duplicate bulundu! Önce temizle:" -ForegroundColor Red
        Write-Host "   $python scripts/dedup_classifier.py $Classifier" -ForegroundColor Red
        exit 1
    }
} else {
    Step 2 "AppClassifier bulunamadı, duplicate kontrolü atlandı."
}

# --- 3. Build ---
if ($doBuild) {
    Step 3 "Build: .\gradlew assembleDebug ..."
    & .\gradlew assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build başarısız! Hata çözüm kuralı devrede (CLAUDE.md §2)." -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Build başarılı." -ForegroundColor Green
} else {
    Step 3 "Build bu döngüde atlandı (ritim: her $BuildEvery döngüde bir)."
}

# --- 4. Git ---
Step 4 "Git add + commit..."
& git add -A
$status = & git status --porcelain
if (-not $status) {
    Write-Host "ℹ️  Commit edilecek değişiklik yok." -ForegroundColor DarkGray
} else {
    & git commit -m "Döngü $CycleNum`: $Message"
    if (-not $NoPush) {
        Write-Host "📤 Push origin main..." -ForegroundColor Cyan
        & git push origin main
        if ($LASTEXITCODE -ne 0) { Write-Host "⚠️ Push başarısız." -ForegroundColor Red }
    }
}

# --- 5. Telegram ---
Step 5 "Telegram bildirim..."
$apk = "app\build\outputs\apk\debug\app-debug.apk"

# Detaylı rapor mesajı oluştur
$buildStatus = if ($doBuild) { "✅ BUILD ALINDI" } else { "⏭ Build atlandı" }
$nextCycle = $CycleNum + 1
$nextBuild = $BuildEvery - ($nextCycle % $BuildEvery)
$gitLog = (git log --oneline -3 2>$null) -join "`n"
$roadmapNext = ""
try {
    $roadmap = Get-Content "ROADMAP.md" -ErrorAction SilentlyContinue
    $inNew = $false
    foreach ($line in $roadmap) {
        if ($line -match "Döngüden Gelen") { $inNew = $true; continue }
        if ($inNew -and $line -match "^\- \[ \]") {
            $roadmapNext = $line -replace "^\- \[ \] ", ""
            break
        }
    }
} catch {}

$detailedMsg = @"
🔄 DÖNGÜ $CycleNum TAMAMLANDI
━━━━━━━━━━━━━━━━━━━━━━
📋 Değişenler: $Message
$buildStatus
━━━━━━━━━━━━━━━━━━━━━━
📝 Son commitler:
$gitLog
━━━━━━━━━━━━━━━━━━━━━━
⏭ Sonraki döngü: #$nextCycle
🏗 Build: $nextBuild döngü sonra
📌 Öncelik: $roadmapNext
"@

try {
    if ($doBuild -and (Test-Path $apk)) {
        & "$scriptDir\telegram_notify.ps1" -Message $detailedMsg -File $apk
    } else {
        & "$scriptDir\telegram_notify.ps1" -Message $detailedMsg
    }
} catch {
    Write-Host "⚠️ Telegram atlandı: $_" -ForegroundColor DarkYellow
}

Write-Host "`n✅ Döngü $CycleNum tamamlandı." -ForegroundColor Green
Write-Host "   Hatırlatma: HISTORY.md güncelle (3 satır zorunlu)." -ForegroundColor DarkGray
