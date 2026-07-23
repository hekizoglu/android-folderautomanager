<#
.SYNOPSIS
    install_hooks.ps1 — Git hook'larını .git/hooks/ klasörüne kopyalar.
.DESCRIPTION
    .github/hooks/ içindeki hook'ları .git/hooks/'a kopyalar ve executable yapar.
    Her git clone / fresh checkout sonrası bir kez çalıştır.
.EXAMPLE
    .\scripts\install_hooks.ps1
#>

$hooks = @("pre-commit")
$src   = ".github\hooks"
$dst   = ".git\hooks"

foreach ($h in $hooks) {
    $s = Join-Path $src $h
    $d = Join-Path $dst $h
    if (Test-Path $s) {
        Copy-Item $s $d -Force
        # Git Bash'te executable bit gerekli — WSL/Git Bash üzerinden ayarla
        $gitBash = "C:\Program Files\Git\bin\bash.exe"
        if (Test-Path $gitBash) {
            & $gitBash -c "chmod +x .git/hooks/$h"
        }
        Write-Host "✅ $h kuruldu → $d" -ForegroundColor Green
    } else {
        Write-Host "⚠️  $s bulunamadı, atlandı." -ForegroundColor Yellow
    }
}

Write-Host "`n🎉 Hook kurulumu tamamlandı." -ForegroundColor Cyan
Write-Host "   AppClassifier her değiştiğinde duplicate kontrolü otomatik çalışır." -ForegroundColor DarkGray
