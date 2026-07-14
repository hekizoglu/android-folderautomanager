[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$qaRoot = Join-Path $projectRoot "qa"
$reportsRoot = Join-Path $qaRoot "reports"
$timestampToken = Get-Date -Format "yyyyMMdd-HHmmss"
$timestampText = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$scanRoot = Join-Path $reportsRoot "logic-audit-deep-$timestampToken"
$sectionDir = Join-Path $scanRoot "sections"
$toolDir = Join-Path $scanRoot "tool-outputs"

New-Item -ItemType Directory -Force -Path $scanRoot, $sectionDir, $toolDir | Out-Null

function New-Finding {
    param(
        [string]$Section,
        [string]$Severity,
        [string]$Id,
        [string]$Title,
        [string]$Description,
        [string]$WhyItMatters,
        [string]$Path,
        [int]$Line,
        [string]$Snippet,
        [string]$Recommendation
    )

    [PSCustomObject]@{
        section = $Section
        severity = $Severity
        id = $Id
        title = $Title
        description = $Description
        why_it_matters = $WhyItMatters
        path = $Path
        line = $Line
        snippet = $Snippet
        recommendation = $Recommendation
    }
}

function Get-RelativePath {
    param([string]$FullPath)

    $uriRoot = [Uri](([IO.Path]::GetFullPath($projectRoot).TrimEnd('\')) + '\')
    $uriFile = [Uri]([IO.Path]::GetFullPath($FullPath))
    $relative = $uriRoot.MakeRelativeUri($uriFile).ToString()
    return [Uri]::UnescapeDataString($relative).Replace('/', '\')
}

function Get-LineInfo {
    param(
        [string[]]$Lines,
        [int]$Index
    )

    $lineNumber = $Index + 1
    $snippet = $Lines[$Index].Trim()
    return @{ Line = $lineNumber; Snippet = $snippet }
}

function Add-PatternFinding {
    param(
        [System.Collections.Generic.List[object]]$Bucket,
        [hashtable]$Rule,
        [string]$SectionName,
        [string]$RelativePath,
        [string[]]$Lines
    )

    $content = [string]::Join([Environment]::NewLine, $Lines)
    $matches = [regex]::Matches($content, $Rule.Pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    foreach ($match in $matches) {
        $lineNumber = ($content.Substring(0, $match.Index) -split "`r?`n").Count
        $lineText = if ($lineNumber -gt 0 -and $lineNumber -le $Lines.Count) { $Lines[$lineNumber - 1].Trim() } else { "" }
        $Bucket.Add((New-Finding `
            -Section $SectionName `
            -Severity $Rule.Severity `
            -Id $Rule.Id `
            -Title $Rule.Title `
            -Description $Rule.Description `
            -WhyItMatters $Rule.WhyItMatters `
            -Path $RelativePath `
            -Line $lineNumber `
            -Snippet $lineText `
            -Recommendation $Rule.Recommendation))
    }
}

function Add-SectionReport {
    param(
        [string]$SectionName,
        [string]$Label,
        [string[]]$Patterns,
        [hashtable[]]$Rules,
        [System.Collections.Generic.List[object]]$Findings,
        [System.Collections.Generic.List[object]]$SectionSummaries
    )

    $files = foreach ($pattern in $Patterns) {
        Get-ChildItem -Path (Join-Path $projectRoot $pattern) -Recurse -File -Include *.kt, *.kts -ErrorAction SilentlyContinue
    }
    $files = @($files | Sort-Object FullName -Unique)

    Write-Host "[logic-audit-deep] Taraniyor: $Label ($($files.Count) dosya)" -ForegroundColor Cyan

    $sectionFindings = New-Object System.Collections.Generic.List[object]

    foreach ($file in $files) {
        $relativePath = Get-RelativePath $file.FullName
        $lines = Get-Content $file.FullName
        foreach ($rule in $Rules) {
            Add-PatternFinding -Bucket $sectionFindings -Rule $rule -SectionName $Label -RelativePath $relativePath -Lines $lines
        }

        if ($relativePath -match 'Screen\.kt$' -or $relativePath -match 'Section\.kt$') {
            $buttonCount = ([regex]::Matches(([string]::Join("`n", $lines)), '\b(Button|FilledTonalButton|OutlinedButton|IconButton|TextButton|FloatingActionButton)\s*\(')).Count
            if ($buttonCount -ge 8) {
                $sectionFindings.Add((New-Finding `
                    -Section $Label `
                    -Severity "P3" `
                    -Id "UI001" `
                    -Title "Yogun buton kullanimli ekran" `
                    -Description "Ekranda cok sayida buton var; tek tek durum/disabled/loading kontrolu manuel QA gerektirir." `
                    -WhyItMatters "Buton sayisi arttikca click akisi, state gecisi ve yanlis yonlendirme riski buyur." `
                    -Path $relativePath `
                    -Line 1 `
                    -Snippet "Toplam button benzeri composable sayisi: $buttonCount" `
                    -Recommendation "Bu ekran icin QA checklist ve UI testi ekleyin; her butonun enabled/disabled ve loading senaryosunu dogrulayin."))
            }
        }
    }

    $sectionReport = @()
    $sectionReport += "# $Label"
    $sectionReport += ""
    $sectionReport += "- Tarama zamani: $timestampText"
    $sectionReport += "- Taranan dosya sayisi: $($files.Count)"
    $sectionReport += "- Bulgu sayisi: $($sectionFindings.Count)"
    $sectionReport += ""

    if ($sectionFindings.Count -eq 0) {
        $sectionReport += "Bu bolumde otomatik mantik/kod hatasi tespit edilmedi."
    } else {
        foreach ($finding in ($sectionFindings | Sort-Object severity, id, path, line)) {
            $sectionReport += "## $($finding.severity) - $($finding.id) - $($finding.title)"
            $sectionReport += "- Konum: $($finding.path):$($finding.line)"
            $sectionReport += "- Aciklama: $($finding.description)"
            $sectionReport += "- Neden onemli: $($finding.why_it_matters)"
            $sectionReport += "- AI baglami: `"$($finding.snippet)`""
            $sectionReport += "- Oneri: $($finding.recommendation)"
            $sectionReport += ""
        }
    }

    [System.IO.File]::WriteAllLines((Join-Path $sectionDir "$SectionName.md"), $sectionReport, [System.Text.Encoding]::UTF8)

    foreach ($finding in $sectionFindings) {
        $Findings.Add($finding)
    }

    $SectionSummaries.Add([PSCustomObject]@{
        section = $Label
        key = $SectionName
        file_count = $files.Count
        finding_count = $sectionFindings.Count
    })
}

function Invoke-GradleAudit {
    param(
        [string]$TaskName,
        [string]$OutputFile
    )

    Write-Host "[logic-audit-deep] Gradle gorevi calisiyor: $TaskName" -ForegroundColor Yellow
    Push-Location $projectRoot
    try {
        $command = ".\gradlew.bat $TaskName --console=plain > `"$OutputFile`" 2>&1"
        & cmd.exe /c $command | Out-Null
        return $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

$sharedRules = @(
    @{
        Id = "LS001"; Severity = "P1"; Title = "combine stale state riski"
        Pattern = 'combine\([^\)]*$'
        Description = "Combine kurulumu ek state degiskenlerini disarida birakiyorsa UI stale kalabilir."
        WhyItMatters = "Ekran secimleri veya filtreler degisince liste guncellenmeyebilir; kullanici eski veriyi gormeye devam eder."
        Recommendation = "State uretimine etki eden tum Flow ve MutableState alanlarini ayni combine zincirine dahil edin."
    },
    @{
        Id = "LS002"; Severity = "P1"; Title = "Snapshot secim ile toplu islem"
        Pattern = 'selectedApps\.toList\(\)'
        Description = "Toplu islem aninda anlik snapshot kullanimi stale secimle islem yapabilir."
        WhyItMatters = "Kullanici farkli uygulamalari secmis olsa da islem eski secime uygulanabilir."
        Recommendation = "Aksiyon icinde source-of-truth state'i yeniden okuyun veya ViewModel tarafinda atomik islem yapin."
    },
    @{
        Id = "LS007"; Severity = "P2"; Title = "cancel plus KEEP WorkManager yarisi"
        Pattern = 'cancelUniqueWork|ExistingPeriodicWorkPolicy\.KEEP'
        Description = "Ayni akista iptal edip hemen KEEP ile enqueue etmek zamanlama yarisi dogurabilir."
        WhyItMatters = "Worker hic baslamayabilir veya eski plan beklenmedik sekilde kalabilir."
        Recommendation = "Iptal ve yeni enqueue akisini tek sahipte toplayin; gerekiyorsa REPLACE kullanin."
    },
    @{
        Id = "NULL001"; Severity = "P2"; Title = "Non-null assertion kullanimi"
        Pattern = '!!'
        Description = "Kodda zorlayici non-null assertion var."
        WhyItMatters = "Beklenmeyen null durumda uygulama dogrudan coker."
        Recommendation = "Null durumunu erken handle edin veya guvenli fallback tanimlayin."
    },
    @{
        Id = "COR001"; Severity = "P2"; Title = "IO dispatcheri olmadan agir coroutine"
        Pattern = 'viewModelScope\.launch\s*\{'
        Description = "ViewModel coroutine icinde agir repository/disk islemleri ana threadde kalmis olabilir."
        WhyItMatters = "UI takilmasi, jank ve ANR riski olusur."
        Recommendation = "Agir isleri repository/usecase icinde IO dispatcher'a tasiyin veya acik context belirleyin."
    }
)

$sectionDefinitions = @(
    @{ Key = "settings"; Label = "Ayarlar"; Patterns = @("app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens"); Rules = ($sharedRules + @(
        @{
            Id = "SET001"; Severity = "P2"; Title = "remember ile AppPrefs okunuyor"
            Pattern = 'remember\s*\{[^}]*AppPrefs'
            Description = "remember ile preference okunup degisim dinlenmiyorsa ayar ekrani stale kalabilir."
            WhyItMatters = "Kullanici toggle degistirse bile alt bilgi veya preview aninda guncellenmeyebilir."
            Recommendation = "Preference listener, Flow veya DisposableEffect tabanli reaktif yapi kullanin."
        },
        @{
            Id = "SET002"; Severity = "P2"; Title = "Settings butonu navigate ile bagli"
            Pattern = 'onClick\s*=\s*\{[^}]*navigate'
            Description = "Buton veya satir tiklamasi yeni ekrana gidiyor; hedef route manuel QA gerektirir."
            WhyItMatters = "Ozellikle settings hub ve alt sayfalarda kopuk route ya da yanlis arguman sessiz bozulma yaratir."
            Recommendation = "Bu bulguyu kontrol listesine alin ve route/arguman eslesmesini AppNavigation ile dogrulayin."
        }
    )) },
    @{ Key = "stats_reports"; Label = "Istatistikler ve Raporlar"; Patterns = @(
        "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens",
        "app\src\main\java\com\armutlu\apporganizer\presentation\viewmodel",
        "app\src\main\java\com\armutlu\apporganizer\domain\usecase"
    ); Rules = ($sharedRules + @(
        @{
            Id = "STAT001"; Severity = "P1"; Title = "Rapor metni ile metrik uyumsuzlugu"
            Pattern = 'bugun|en cok actigin|haftalik|son 7 gun'
            Description = "Kullaniciya zaman/miktar iddiasi veren metin bulundu; hesap mantigiyle birebir eslesme kontrol edilmeli."
            WhyItMatters = "Yanlis istatistik guveni dusurur ve rapor ekranlarini anlamsiz hale getirir."
            Recommendation = "Ayni dosya veya ViewModel'de metnin beslendigi metrikle birebir uyumu test edin."
        },
        @{
            Id = "STAT002"; Severity = "P2"; Title = "Siralama olmadan first secimi"
            Pattern = '\.first\(\)|\.firstOrNull\(\)'
            Description = "Raporlanan birincil oge acik sort olmadan seciliyor olabilir."
            WhyItMatters = "Liste sirasi kaynak bagimli ise kullanici her acilista farkli sonuc gorebilir."
            Recommendation = "Metrige gore explicit sortedBy/sortedByDescending kullanin."
        }
    )) },
    @{ Key = "launcher_home"; Label = "Launcher ve Ana Ekran"; Patterns = @(
        "app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher",
        "app\src\main\java\com\armutlu\apporganizer\presentation\navigation"
    ); Rules = ($sharedRules + @(
        @{
            Id = "NAV001"; Severity = "P1"; Title = "putExtra ile route kopuklugu"
            Pattern = 'putExtra\('
            Description = "Intent extra gonderimi var; hedef activity/nav graph bunu tuketmiyorsa akis kopar."
            WhyItMatters = "Bildirim veya kisayol tiklamalari kullaniciyi beklenen tab/ekrana goturmez."
            Recommendation = "Gonderilen extra adlarini nav graph ve activity intent handling ile capraz kontrol edin."
        }
    )) },
    @{ Key = "data_workers"; Label = "Veri, Repository ve Worker"; Patterns = @(
        "app\src\main\java\com\armutlu\apporganizer\data",
        "app\src\main\java\com\armutlu\apporganizer\workers",
        "app\src\main\java\com\armutlu\apporganizer\utils"
    ); Rules = ($sharedRules + @(
        @{
            Id = "DATA001"; Severity = "P1"; Title = "Sync only insert-delete paterni"
            Pattern = 'getAllPackageNames\(\)|existingPackages'
            Description = "Senkronizasyon akisi mevcut kayitlari update etmeden sadece ekle-sil yapiyor olabilir."
            WhyItMatters = "App adi, icon, sistem flagi gibi alanlar stale kalir."
            Recommendation = "Insert/delete yanina update diff mantigi ekleyin."
        },
        @{
            Id = "DATA002"; Severity = "P2"; Title = "Siralama olmadan yeni app secimi"
            Pattern = 'newApps\.first\(\)|recommended.*first\('
            Description = "Yeni veya onerilen uygulama ilk elemanla seciliyor."
            WhyItMatters = "Repo sirasi deterministik degilse farkli cihazlarda farkli tavsiye uretilir."
            Recommendation = "Install time, score veya usage metric ile explicit sort kullanin."
        },
        @{
            Id = "DATA003"; Severity = "P2"; Title = "NEW_TASK eksik activity acilisi"
            Pattern = 'context\.startActivity\('
            Description = "Application context ile activity aciliyorsa NEW_TASK bayragi yoksa cihaz bazli crash olabilir."
            WhyItMatters = "Ozellikle worker/service/util katmanindan ekran acilisinda ActivityNotFound veya AndroidRuntimeException gorulebilir."
            Recommendation = "Call site'i kontrol edin; activity olmayan context icin intent'e FLAG_ACTIVITY_NEW_TASK ekleyin."
        }
    )) }
)

$allFindings = New-Object System.Collections.Generic.List[object]
$sectionSummaries = New-Object System.Collections.Generic.List[object]

foreach ($section in $sectionDefinitions) {
    Add-SectionReport `
        -SectionName $section.Key `
        -Label $section.Label `
        -Patterns $section.Patterns `
        -Rules $section.Rules `
        -Findings $allFindings `
        -SectionSummaries $sectionSummaries
}

$detektExit = Invoke-GradleAudit -TaskName ":app:detekt" -OutputFile (Join-Path $toolDir "detekt.txt")
$lintExit = Invoke-GradleAudit -TaskName ":app:lintDebug" -OutputFile (Join-Path $toolDir "lintDebug.txt")
$testExit = Invoke-GradleAudit -TaskName ":app:testDebugUnitTest" -OutputFile (Join-Path $toolDir "testDebugUnitTest.txt")

$summary = @()
$summary += "# Logic Audit Deep"
$summary += ""
$summary += "- Tarih: $timestampText"
$summary += "- Rapor klasoru: $scanRoot"
$summary += "- Toplam bolum: $($sectionSummaries.Count)"
$summary += "- Toplam bulgu: $($allFindings.Count)"
$summary += "- Gradle sonuc kodlari: detekt=$detektExit, lint=$lintExit, test=$testExit"
$summary += ""
$summary += "## Bolum Ozetleri"
$summary += ""
foreach ($sectionSummary in $sectionSummaries) {
    $summary += "- $($sectionSummary.section): $($sectionSummary.file_count) dosya, $($sectionSummary.finding_count) bulgu"
}
$summary += ""
$summary += "## Kritik Bulgular"
$summary += ""
$critical = @($allFindings | Where-Object { $_.severity -eq "P1" } | Sort-Object id, path, line)
if ($critical.Count -eq 0) {
    $summary += "Kritik P1 bulgu tespit edilmedi."
} else {
    foreach ($finding in $critical) {
        $summary += "- $($finding.id) | $($finding.path):$($finding.line) | $($finding.description)"
    }
}
$summary += ""
$summary += "## Uretilen Dosyalar"
$summary += ""
$summary += "- sections\\*.md : her bolum icin detayli tarama notlari"
$summary += "- findings.json : makinece islenebilir tum bulgular"
$summary += "- tool-outputs\\*.txt : detekt, lint ve test konsol ciktilari"

[System.IO.File]::WriteAllLines((Join-Path $scanRoot "summary.md"), $summary, [System.Text.Encoding]::UTF8)

$findingsJsonPath = Join-Path $scanRoot "findings.json"
$allFindings | Sort-Object severity, id, path, line | ConvertTo-Json -Depth 6 | Set-Content -Path $findingsJsonPath -Encoding UTF8

Write-Host "[logic-audit-deep] Tarama tamamlandi." -ForegroundColor Green
Write-Host "[logic-audit-deep] Klasor: $scanRoot" -ForegroundColor Green
Write-Host "[logic-audit-deep] Bulgu: $($allFindings.Count)" -ForegroundColor Cyan

if ($detektExit -ne 0 -or $lintExit -ne 0 -or $testExit -ne 0 -or (@($critical).Count -gt 0)) {
    exit 2
}

exit 0
