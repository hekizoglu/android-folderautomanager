param(
    [string]$CommitMessage = "",
    [switch]$SendTelegram = $false,
    [ValidateSet("Full","Resolve")]
    [string]$Mode = "Resolve"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$counterPath = Join-Path $scriptDir "loop_count.txt"
$auditScript = Join-Path $scriptDir "audit.ps1"
$manualChecklistPath = Join-Path $projectRoot "local_denetim_manuel_checklist.md"
$reportPath = Join-Path $projectRoot "local_denetim_raporu.md"
$unresolvedPath = Join-Path $projectRoot "COZULEMEYEN_SORUNLAR.md"
$encoding = [System.Text.Encoding]::UTF8

Set-Location $projectRoot

function Add-TextWithRetry {
    param(
        [string]$Path,
        [string]$Text
    )

    for ($attempt = 1; $attempt -le 10; $attempt++) {
        try {
            $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
            try {
                $writer = New-Object System.IO.StreamWriter($stream, $encoding)
                $writer.Write($Text)
                $writer.Flush()
            } finally {
                if ($writer) { $writer.Dispose() }
                $stream.Dispose()
            }
            return
        } catch {
            if ($attempt -eq 10) { throw }
            Start-Sleep -Milliseconds 500
        }
    }
}

function Append-UniqueQuestion {
    param(
        [string]$Header,
        [string]$Question
    )
    if (-not (Test-Path $manualChecklistPath)) { return $false }
    $content = [System.IO.File]::ReadAllText($manualChecklistPath, $encoding)
    if ($content.Contains($Question)) { return $false }
    $headerIndex = $content.IndexOf($Header)
    if ($headerIndex -lt 0) { return $false }
    $insertAt = $content.IndexOf("##", $headerIndex + $Header.Length)
    if ($insertAt -lt 0) { $insertAt = $content.Length }
    $updated = $content.Insert($insertAt, "- $Question`r`n`r`n")
    [System.IO.File]::WriteAllText($manualChecklistPath, $updated, $encoding)
    return $true
}

function Update-ChecklistFromReport {
    if (-not (Test-Path $reportPath)) { return @() }
    $report = [System.IO.File]::ReadAllText($reportPath, $encoding)
    $added = New-Object System.Collections.Generic.List[string]

    if ($report -match "swipe|gesture|drag") {
        if (Append-UniqueQuestion -Header "## 7. Gesture ve Kapanis Kontrolu" -Question "Dismiss, drawer ve liste scroll hareketleri ayni ekranda tahmin edilebilir bicimde calisiyor mu?") {
            $added.Add("Gesture checklist sorusu genislestirildi.")
        }
    }
    if ($report -match "UsageStats|kullanım erişimi|sistem ekran") {
        if (Append-UniqueQuestion -Header "## 2. Settings Satiri Kontrolu" -Question "Kullaniciyi sistem izin ekranina goturen metin, bunu acikca soyluyor mu?") {
            $added.Add("Sistem izin yonlendirmesi sorusu eklendi.")
        }
    }
    if ($report -match "TalkBack|contentDescription|semantics|bildirim") {
        if (Append-UniqueQuestion -Header "## 4. Accessibility Kontrolu" -Question "Bildirim sayisi ve secili durum gibi baglamsal bilgiler sadece gorsel degil, sesli de aktariliyor mu?") {
            $added.Add("Accessibility bildirim/sayi sorusu eklendi.")
        }
    }

    return $added
}

function Append-RunNote {
    param(
        [string]$Title,
        [string[]]$Lines
    )
    if (-not (Test-Path $reportPath)) { return }
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
    $block = @()
    $block += ""
    $block += "## $Title - $timestamp"
    $block += ""
    foreach ($line in $Lines) {
        $block += "- $line"
    }
    Add-TextWithRetry -Path $reportPath -Text (($block -join "`r`n") + "`r`n")
}

function Append-UnresolvedPlaceholder {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
    $entry = @()
    $entry += ""
    $entry += "### [LD-$timestamp] Otomatik cozum bekleyen denetim maddeleri"
    $entry += "**Tarih:** $timestamp | **Durum:** Otomatik script tarafinda sadece raporlandi"
    $entry += "**Sorun:** `local_denetim_raporu.md` icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor."
    $entry += "**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi."
    $entry += "**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor."
    $entry += "**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak."
    Add-TextWithRetry -Path $unresolvedPath -Text (($entry -join "`r`n") + "`r`n")
}

function Stage-ProjectChanges {
    git add --update
    foreach ($path in @(
        "local_denetim_raporu.md",
        "local_denetim_tamamlananlar.md",
        "local_denetim_manuel_checklist.md",
        "local_denetim_kurallari.md",
        "COZULEMEYEN_SORUNLAR.md",
        "scripts"
    )) {
        if (Test-Path $path) {
            git add -- $path
        }
    }
}

if (-not (Test-Path $counterPath)) {
    Set-Content -Path $counterPath -Value "0" -Encoding UTF8
}

$current = 0
$parsed = Get-Content $counterPath -ErrorAction SilentlyContinue | Select-Object -First 1
if ($parsed -match '^\d+$') {
    $current = [int]$parsed
}

$cycleNumber = $current + 1
Set-Content -Path $counterPath -Value $cycleNumber -Encoding UTF8

& $auditScript -SendTelegram:$($SendTelegram.IsPresent)
if (-not $?) {
    throw "audit.ps1 basarisiz oldu."
}

$checklistAdds = Update-ChecklistFromReport
if ($Mode -eq "Full") {
    $notes = @(
        "Tam denetim kurallari ile otomatik rapor yenilendi.",
        'Manuel checklist referansi: `local_denetim_manuel_checklist.md`'
    )
    if ($checklistAdds.Count -gt 0) {
        $notes += $checklistAdds
    } else {
        $notes += "Checklist icin yeni soru ihtiyaci bulunmadi."
    }
    Append-RunNote -Title "Tam Denetim Turu" -Lines $notes
} else {
    Append-UnresolvedPlaceholder
    Append-RunNote -Title "Cozum Sirasi Hazirligi" -Lines @(
        "Rapor tekrar uretildi ve kalan sorunlar bir sonraki cozum turu icin listelendi.",
        'Cozulemeyen maddeler `COZULEMEYEN_SORUNLAR.md` dosyasina not edildi.'
    )
}

$shouldBuild = ($cycleNumber % 3 -eq 0)
if ($Mode -eq "Resolve" -or $shouldBuild) {
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "assembleDebug basarisiz oldu."
    }
}

Stage-ProjectChanges
git diff --cached --quiet
if (-not $?) {
    $message = if ($CommitMessage) {
        $CommitMessage
    } else {
        $suffix = if ($Mode -eq "Resolve" -or $shouldBuild) { " + build" } else { "" }
        "chore: local denetim $Mode cycle $cycleNumber$suffix"
    }
    git commit -m $message
    if ($LASTEXITCODE -ne 0) {
        throw "git commit basarisiz oldu."
    }
    git push origin main
    if ($LASTEXITCODE -ne 0) {
        throw "git push basarisiz oldu."
    }
} else {
    Write-Host "Commit edilecek degisiklik yok." -ForegroundColor Yellow
}

Write-Host "Dongu tamamlandi: $cycleNumber" -ForegroundColor Green
