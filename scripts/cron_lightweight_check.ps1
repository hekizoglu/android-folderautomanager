<#
.SYNOPSIS
    cron_lightweight_check.ps1 - 30 dakikalik cron dongusu icin hafif on-kontrol.

.DESCRIPTION
    Amac: Her 30 dakikada calisan cron gorevinin gereksiz yere tam Kotlin
    compile denemesine girip token/zaman harcamasini onlemek (D2026-07-21,
    "Cron Token Optimizasyonu" gorevi).

    Mantik:
      1. git status ile son commit'ten beri kod degisikligi var mi bak.
      2. Degisiklik yoksa (sadece MD/dokuman dosyalari degismisse de) build
         ATLA - sadece durum ozeti uret.
      3. .kt/.kts/.xml gibi derlenebilir dosya degismisse HAFIF bir sinyal
         olarak compileDebugKotlin --dry-run calistir (gercek derleme yapmaz,
         hangi task'larin calisacagini gosterir). Calisacak task yoksa yine
         ATLA.
      4. Sadece gercekten derlenecek is varsa "AGIR" moda gecilmesi gerektigini
         bildir (asil build cycle.ps1 -Build ile cagrilmali).

    Cikti: stdout'a tek satir JSON-benzeri ozet + exit code.
      exit 0 => build GEREKSIZ, cron hafif modda kalmali (Telegram özet yeter)
      exit 2 => build GEREKLI, agir moda gec (cycle.ps1 -Build)

.EXAMPLE
    .\scripts\cron_lightweight_check.ps1
#>

$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

function Result($needsBuild, $reason) {
    $status = if ($needsBuild) { "AGIR_GEREKLI" } else { "HAFIF_YETERLI" }
    Write-Host "CRON_CHECK: $status - $reason"
    if ($needsBuild) { exit 2 } else { exit 0 }
}

# 1. Son commit'ten beri değişen dosyalar
$changed = & git status --porcelain 2>$null
if (-not $changed) {
    Result $false "Working tree temiz, degisiklik yok."
}

# 2. Sadece derlenebilir dosyalar build tetikler (kod), dokuman/roadmap degil
$compileExtensions = @('.kt', '.kts', '.xml', '.gradle')
$changedFiles = $changed | ForEach-Object { ($_ -split '\s+', 3)[-1] }
$compilableChanged = $changedFiles | Where-Object {
    $ext = [System.IO.Path]::GetExtension($_)
    $compileExtensions -contains $ext
}

if (-not $compilableChanged) {
    Result $false "Sadece dokuman/roadmap/config degisti ($($changedFiles.Count) dosya) - build atlaniyor."
}

# 3. Derlenebilir dosya var - dry-run ile gercekten is olacak mi kontrol et
Write-Host "CRON_CHECK: $($compilableChanged.Count) derlenebilir dosya degismis, dry-run kontrolu..."
$dryRunOutput = & .\gradlew compileDebugKotlin --dry-run --console=plain 2>&1
$willRun = $dryRunOutput | Select-String -Pattern ":compileDebugKotlin\s+SKIPPED" -NotMatch |
           Select-String -Pattern ":compileDebugKotlin"

if (-not $willRun) {
    Result $false "Dry-run: compileDebugKotlin calisacak task listesinde yok - atlaniyor."
}

Result $true "$($compilableChanged.Count) derlenebilir dosya degisti, compileDebugKotlin calisacak - tam build gerekli."
