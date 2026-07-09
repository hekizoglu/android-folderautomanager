<#
.SYNOPSIS
    log_cycle_time.ps1 — harcananvakit.md'ye tek satir zaman/token log'u ekler.

.DESCRIPTION
    Dongu sonunda calisilan islemin baslangic/bitis saatini (veya suresini),
    token maliyet kategorisini ve is tipini harcananvakit.md tablosuna
    mevcut format ile uyumlu tek satir olarak append eder:
    | Tarih | Baslangic | Bitis | Sure | Kategori | Islem | Not |

.PARAMETER StartTime
    Baslangic saati "HH:mm" formatinda (orn. "14:05"). Verilmezse -DurationMinutes zorunlu.

.PARAMETER EndTime
    Bitis saati "HH:mm" formatinda. Verilmezse su anki saat kullanilir.

.PARAMETER DurationMinutes
    Dakika cinsinden sure. StartTime verilmezse EndTime - DurationMinutes ile
    baslangic hesaplanir; StartTime verilmisse sadece "Sure" kolonu icin kullanilir.

.PARAMETER TokenLevel
    Token/maliyet kategorisi: dusuk / orta / yuksek.

.PARAMETER WorkType
    Is tipi: KOD / BUILD / GIT / ORTAM / DOKUMAN / HATA_GIDER / ARASTIRMA / TEST.

.PARAMETER Note
    Kisa aciklama (Islem kolonuna yazilir).

.PARAMETER ExtraNote
    Opsiyonel Not kolonu icerigi.

.PARAMETER Date
    Tarih "yyyy-MM-dd" formatinda. Varsayilan: bugun.

.EXAMPLE
    .\scripts\log_cycle_time.ps1 -StartTime "14:05" -EndTime "14:22" -TokenLevel orta -WorkType BUILD -Note "assembleDebug baseline" -ExtraNote "6dk 40s"

.EXAMPLE
    .\scripts\log_cycle_time.ps1 -DurationMinutes 12 -TokenLevel dusuk -WorkType GIT -Note "rebase + push"
#>
param(
    [string]$StartTime,
    [string]$EndTime,
    [double]$DurationMinutes,
    [Parameter(Mandatory = $true)]
    [ValidateSet("dusuk", "orta", "yuksek")]
    [string]$TokenLevel,
    [Parameter(Mandatory = $true)]
    [ValidateSet("KOD", "BUILD", "GIT", "ORTAM", "DOKUMAN", "HATA_GIDER", "ARASTIRMA", "TEST")]
    [string]$WorkType,
    [Parameter(Mandatory = $true)]
    [string]$Note,
    [string]$ExtraNote = "",
    [string]$Date = (Get-Date -Format "yyyy-MM-dd")
)

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot
$projectRoot = Split-Path -Parent $scriptDir
$logFile = Join-Path $projectRoot "harcananvakit.md"

if (-not (Test-Path $logFile)) {
    throw "harcananvakit.md bulunamadi: $logFile"
}

# Bitis saati verilmezse simdi
if (-not $EndTime) {
    $EndTime = Get-Date -Format "HH:mm"
}

# Baslangic saati yoksa DurationMinutes'tan hesapla
if (-not $StartTime) {
    if (-not $DurationMinutes -or $DurationMinutes -le 0) {
        throw "StartTime verilmediyse -DurationMinutes zorunlu (0'dan buyuk)."
    }
    $endDt = [datetime]::ParseExact($EndTime, "HH:mm", $null)
    $startDt = $endDt.AddMinutes(-$DurationMinutes)
    $StartTime = $startDt.ToString("HH:mm")
}

# Sure metni: DurationMinutes verildiyse onu kullan, yoksa Start/End farkindan hesapla
if ($DurationMinutes -gt 0) {
    $sureText = "~$([math]::Round($DurationMinutes))dk"
} else {
    $s = [datetime]::ParseExact($StartTime, "HH:mm", $null)
    $e = [datetime]::ParseExact($EndTime, "HH:mm", $null)
    if ($e -lt $s) { $e = $e.AddDays(1) }
    $diffMin = ($e - $s).TotalMinutes
    $sureText = "~$([math]::Round($diffMin))dk"
}

$notColumn = "[token:$TokenLevel]"
if ($ExtraNote) { $notColumn = "$ExtraNote | $notColumn" }

$row = "| $Date | $StartTime | $EndTime | $sureText | $WorkType | $Note | $notColumn |"

Add-Content -Path $logFile -Value $row -Encoding UTF8
Write-Host "[log_cycle_time] Eklendi: $row" -ForegroundColor Green
