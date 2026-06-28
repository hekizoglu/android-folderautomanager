# fix_denetim_encoding.ps1
# KiloCode'un her 15 dakikada bir bozduğu local denetim dosyalarını düzeltir.
# Kullanım: .\scripts\fix_denetim_encoding.ps1

$files = @(
    "local_denetim_otomatik_rapor.md",
    "local_denetim_raporu.md",
    "qa\local_denetim_raporu.md"
)

foreach ($f in $files) {
    if (Test-Path $f) {
        python scripts\fix_encoding.py $f
    }
}

# .bak dosyalarını temizle
Get-ChildItem -Filter "*.bak" -Recurse | Where-Object { $_.DirectoryName -notlike "*\.git*" } | Remove-Item -Force
Write-Output "Denetim dosyalari temizlendi."
