# Repomix runner — npx ile çalıştır (ilk çalıştırmada indirir)
# Kullanım: .\scripts\repomix-run.ps1
# Çıktı: repomix-output.xml (AI bağlamı için hazır)

Set-Location (Split-Path $PSScriptRoot)

Write-Host "Repomix çalıştırılıyor..." -ForegroundColor Cyan
npx repomix@latest --config repomix.config.json

if ($LASTEXITCODE -eq 0) {
    $size = (Get-Item repomix-output.xml).Length / 1KB
    Write-Host "repomix-output.xml oluşturuldu ($([math]::Round($size, 1)) KB)" -ForegroundColor Green
} else {
    Write-Host "Repomix hatası" -ForegroundColor Red
}
