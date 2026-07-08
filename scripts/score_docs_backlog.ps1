param(
    [switch]$UpdateRoadmap = $false
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$reportPath = Join-Path $projectRoot "docs\internal\docs_backlog_score.md"
$roadmapPath = Join-Path $projectRoot "ROADMAP.md"
$encoding = New-Object System.Text.UTF8Encoding($false)

Set-Location $projectRoot

function New-Candidate {
    param(
        [string]$Id,
        [string]$Title,
        [string]$Source,
        [int]$KV,
        [int]$U,
        [int]$BR,
        [int]$EA,
        [string]$Recommendation,
        [string]$Status = "Bekliyor"
    )

    [pscustomobject]@{
        Id = $Id
        Title = $Title
        Source = $Source
        KV = $KV
        U = $U
        BR = $BR
        EA = $EA
        Score = ($KV + $U + $BR + $EA)
        Recommendation = $Recommendation
        Status = $Status
    }
}

$candidates = @(
    (New-Candidate `
        -Id "DSR1" `
        -Title "Docs backlog skorlayici donguye eklensin" `
        -Source "docs/time_token_analysis_2026-06-30.md; docs/AI_ORCHESTRATION_PLAN.md" `
        -KV 4 -U 4 -BR 5 -EA 5 `
        -Recommendation "Her dongude docs raporlari puanlansin; 15+ maddeler ROADMAP'teki otomatik blokta yenilensin." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "DSR2" `
        -Title "Arama sonuclari kaynak bazinda gruplansin" `
        -Source "docs/UX_SEARCH_REPORTS_SPEC.md; docs/search-architecture-report.md" `
        -KV 4 -U 5 -BR 4 -EA 4 `
        -Recommendation "AllApps/Search UI sonuclari Uygulamalar, Kategoriler, Kisiler, Dosyalar bolumleriyle gostersin." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "DSR3" `
        -Title "Uygulama arama kaynagi kilitli kalsin" `
        -Source "docs/UX_SEARCH_REPORTS_SPEC.md; docs/search-architecture-report.md" `
        -KV 4 -U 4 -BR 3 -EA 5 `
        -Recommendation "SearchSettings'te Uygulamalar kaynagi acik ve kapatilamaz olsun; bos/yanlis arama durumlari engellensin." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "DSR4" `
        -Title "Permission reddi fallback ve ayar yonlendirme" `
        -Source "docs/UX_SEARCH_REPORTS_SPEC.md; docs/internal/local_denetim_raporu.md" `
        -KV 5 -U 4 -BR 4 -EA 4 `
        -Recommendation "Kisiler/dosya izin reddinde toggle geri kapansin; kalici redde sistem ayarlari deeplink'i gosterilsin." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "DSR5" `
        -Title "Play Store gorsel ve mesaj QA paketi" `
        -Source "docs/competitor_user_research_2026-06-30.md; docs/store_listing.md" `
        -KV 4 -U 4 -BR 5 -EA 3 `
        -Recommendation "Light/dark screenshot seti, privacy-first metin ve QUERY_ALL_PACKAGES aciklamasi tek QA paketinde kontrol edilsin." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "DSR6" `
        -Title "Build warning debt cleanup" `
        -Source "docs/internal/build_benchmark_latest.md; docs/issue_mitigation_research_2026-06-30.md" `
        -KV 3 -U 4 -BR 4 -EA 4 `
        -Recommendation "Deprecated/unused compose ve icon uyarilari temizlenip build ciktisi daha okunur hale getirilsin." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "DSR7" `
        -Title "Token ve sure telemetry logu" `
        -Source "docs/time_token_analysis_2026-06-30.md; docs/issue_mitigation_research_2026-06-30.md" `
        -KV 3 -U 4 -BR 4 -EA 3 `
        -Recommendation "Dongu sonunda yaklasik token/sure notu append eden sade bir log tutulabilsin."),
    (New-Candidate `
        -Id "DSR8" `
        -Title "Rakip referans tasarim karar belgesi" `
        -Source "docs/competitor_user_research_2026-06-30.md" `
        -KV 3 -U 4 -BR 4 -EA 3 `
        -Recommendation "Smart Launcher, Niagara ve Kvaesitso referanslari icin uygulanabilir UI karar listesi cikarilsin."),
    (New-Candidate `
        -Id "DSR9" `
        -Title "Configuration cache guard benchmark" `
        -Source "docs/issue_mitigation_research_2026-06-30.md; docs/internal/build_benchmark_latest.md" `
        -KV 3 -U 3 -BR 4 -EA 3 `
        -Recommendation "Configuration cache sadece benchmark/CLI profilinde kalsin; kalici ayar icin uyumluluk kaniti istensin."),
    (New-Candidate `
        -Id "R1" `
        -Title "Play Store Privacy/Data Safety uyum paketi" `
        -Source "FIKIRLER.md; HISTORY.md Dongu 214-215" `
        -KV 5 -U 5 -BR 5 -EA 3 `
        -Recommendation "Privacy policy, Data Safety, Firebase/Crashlytics/Analytics/FCM, DeepSeek, Drive/SAF ve package inventory beyanlari ayni hikayeye cekilsin. Kod tarafi tamamlandi (Dongu 214-215); Play Console Data Safety formu doldurma dis aksiyon olarak kaliyor." `
        -Status "Bekliyor"),
    (New-Candidate `
        -Id "R2" `
        -Title "Privacy Policy URL ve GitHub Pages dogrulama" `
        -Source "FIKIRLER.md; HISTORY.md Dongu 214" `
        -KV 5 -U 4 -BR 3 -EA 5 `
        -Recommendation "Manifest/store listing URL'i gercek yayin URL'iyle ayni olsun. Dongu 214'te PrivacyPolicyScreen.kt ve store_listing.md'deki 404 veren /docs/ onekli URL duzeltildi, curl ile 200 dogrulandi." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "R3" `
        -Title "Rehber ve bildirim metni privacy policy celiskilerini duzelt" `
        -Source "FIKIRLER.md; HISTORY.md Dongu 214-215" `
        -KV 4 -U 4 -BR 4 -EA 4 `
        -Recommendation "ContactsIndexer ve NotificationListener gercegi policy'deki iddialarla uyumlu hale getirilsin. Dongu 214'te Firebase/kisi rehberi/bildirim metni celiskileri, Dongu 215'te Accessibility Service beyani duzeltildi." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "R4" `
        -Title "Play Store release imza ve submission kapisi" `
        -Source "ROADMAP.md" `
        -KV 5 -U 4 -BR 4 -EA 3 `
        -Recommendation "Release keystore, content rating, QUERY_ALL_PACKAGES declaration ve final AAB temiz committen build akisina baglansin." `
        -Status "Bekliyor"),
    (New-Candidate `
        -Id "R5" `
        -Title "Firebase Analytics veri azaltma ve beyan uyumu" `
        -Source "FIKIRLER.md; HISTORY.md Dongu 214" `
        -KV 4 -U 4 -BR 4 -EA 3 `
        -Recommendation "PackageName/category/query_length eventleri Data Safety ve privacy-first mesajiyla uyumlu hale getirilsin veya azaltilsin. Dongu 214'te AppAnalytics.kt'den package_name kaldirildi (appLaunched/categoryReclassified/shortcutUsed)." `
        -Status "Tamamlandi"),
    (New-Candidate `
        -Id "R6" `
        -Title "Gercek cihaz Play-oncesi QA paketi" `
        -Source "ROADMAP.md" `
        -KV 4 -U 4 -BR 4 -EA 3 `
        -Recommendation "Android 14 NotificationListener, screenshot smoke, backup/restore, worker schedule ve blur/API26 tek kanitli pakette kosulsun." `
        -Status "Bekliyor"),
    (New-Candidate `
        -Id "R7" `
        -Title "Akilli Bildirim Analiz Sistemi" `
        -Source "ROADMAP.md; FIKIRLER.md" `
        -KV 4 -U 3 -BR 4 -EA 4 `
        -Recommendation "NotificationListener + notification_events + NotificationAnalyzer + SmartInsightWorker hatti privacy-first rapor, oneri ve gunluk akilli bildirim sistemine tamamlanmali. Detay ve kabul kriterleri ROADMAP.md'deki Detay bolumunde." `
        -Status "Bekliyor")
)

$now = Get-Date -Format "yyyy-MM-dd HH:mm"
$high = @($candidates | Where-Object { $_.Score -ge 15 -and $_.Status -ne "Tamamlandi" } | Sort-Object @{ Expression = "Score"; Descending = $true }, Id)
$all = @($candidates | Sort-Object @{ Expression = "Score"; Descending = $true }, Id)

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("# Docs Backlog Score")
$lines.Add("")
$lines.Add("> Generated: $now")
$lines.Add("> Rule: KV + U + BR + EA >= 15 goes to ROADMAP.")
$lines.Add("")
$lines.Add("Scoring follows the project idea model:")
$lines.Add("")
$lines.Add("- KV: kullanici veya stratejik deger")
$lines.Add("- U: aciliyet")
$lines.Add("- BR: bagimlilik/risk azaltma")
$lines.Add("- EA: uygulanabilirlik")
$lines.Add("")
$lines.Add("## High Score")
$lines.Add("")
$lines.Add("| # | Score | KV | U | BR | EA | Source | Task | Recommendation | Status |")
$lines.Add("|---|-------|----|---|----|----|--------|------|----------------|--------|")
foreach ($candidate in $high) {
    $lines.Add("| $($candidate.Id) | **$($candidate.Score)** | $($candidate.KV) | $($candidate.U) | $($candidate.BR) | $($candidate.EA) | $($candidate.Source) | $($candidate.Title) | $($candidate.Recommendation) | $($candidate.Status) |")
}

$lines.Add("")
$lines.Add("## All Scored Items")
$lines.Add("")
$lines.Add("| # | Score | Source | Task | Recommendation | Status |")
$lines.Add("|---|-------|--------|------|----------------|--------|")
foreach ($candidate in $all) {
    $lines.Add("| $($candidate.Id) | $($candidate.Score) | $($candidate.Source) | $($candidate.Title) | $($candidate.Recommendation) | $($candidate.Status) |")
}

$lines.Add("")
$lines.Add("## Notes")
$lines.Add("")
$lines.Add("- This file is generated by `scripts/score_docs_backlog.ps1`.")
$lines.Add("- ROADMAP sync is idempotent between `DOCS_SCORE_HIGH_START` and `DOCS_SCORE_HIGH_END`.")
$lines.Add("- Pre-existing docs may be untracked locally; the score still uses the files present in `docs/`.")

[System.IO.File]::WriteAllText($reportPath, ($lines -join [Environment]::NewLine) + [Environment]::NewLine, $encoding)

function Get-RoadmapBlock {
    param([object[]]$Items)

    $block = [System.Collections.Generic.List[string]]::new()
    $block.Add("<!-- DOCS_SCORE_HIGH_START -->")
    $block.Add("### Kirmizi Kritik - Docs/Rapor Skor Taramasi (Otomatik)")
    $block.Add("")
    $block.Add("> Kaynak: docs/internal/docs_backlog_score.md. Kural: KV+U+BR+EA >= 15 ROADMAP'e girer. scripts/score_docs_backlog.ps1 -UpdateRoadmap her dongude bu blogu yeniler.")
    $block.Add("")
    $block.Add("| # | Puan | Kaynak | Gorev | Oneri | Durum |")
    $block.Add("|---|------|--------|-------|-------|-------|")
    foreach ($item in $Items) {
        $block.Add("| **$($item.Id)** | **$($item.Score)** | $($item.Source) | **$($item.Title)** | $($item.Recommendation) | $($item.Status) |")
    }
    $block.Add("<!-- DOCS_SCORE_HIGH_END -->")
    return ($block -join [Environment]::NewLine)
}

function Set-GeneratedRoadmapBlock {
    param([string]$Block)

    $start = "<!-- DOCS_SCORE_HIGH_START -->"
    $end = "<!-- DOCS_SCORE_HIGH_END -->"
    $content = [System.IO.File]::ReadAllText($roadmapPath, $encoding)
    $pattern = [regex]::Escape($start) + ".*?" + [regex]::Escape($end)

    if ($content.Contains($start) -and $content.Contains($end)) {
        $updated = [System.Text.RegularExpressions.Regex]::Replace(
            $content,
            $pattern,
            $Block,
            [System.Text.RegularExpressions.RegexOptions]::Singleline
        )
    } else {
        $anchorMatch = [System.Text.RegularExpressions.Regex]::Match(
            $content,
            "(?m)^### .*Play Store.*$"
        )
        $anchorIndex = if ($anchorMatch.Success) { $anchorMatch.Index } else { -1 }
        if ($anchorIndex -ge 0) {
            $updated = $content.Insert($anchorIndex, $Block + [Environment]::NewLine + [Environment]::NewLine)
        } else {
            $fallbackMatch = [System.Text.RegularExpressions.Regex]::Match(
                $content,
                "(?m)^### .*Puan.*$"
            )
            $fallbackIndex = if ($fallbackMatch.Success) { $fallbackMatch.Index } else { -1 }
            if ($fallbackIndex -ge 0) {
                $updated = $content.Insert($fallbackIndex, $Block + [Environment]::NewLine + [Environment]::NewLine)
            } else {
                $updated = $content.TrimEnd() + [Environment]::NewLine + [Environment]::NewLine + $Block + [Environment]::NewLine
            }
        }
    }

    [System.IO.File]::WriteAllText($roadmapPath, $updated, $encoding)
}

if ($UpdateRoadmap) {
    Set-GeneratedRoadmapBlock -Block (Get-RoadmapBlock -Items $high)
}

Write-Host "Docs backlog scored: $($candidates.Count) items, $($high.Count) high-score items." -ForegroundColor Green
Write-Host "Report: $reportPath" -ForegroundColor Green
if ($UpdateRoadmap) {
    Write-Host "ROADMAP updated: $roadmapPath" -ForegroundColor Green
}
