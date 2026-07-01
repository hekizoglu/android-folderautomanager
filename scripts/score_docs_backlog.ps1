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
        -Recommendation "Configuration cache sadece benchmark/CLI profilinde kalsin; kalici ayar icin uyumluluk kaniti istensin.")
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
