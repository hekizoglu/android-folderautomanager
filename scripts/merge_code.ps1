# merge_code.ps1 — Tum Kotlin kaynak kodlarini all_code.txt'de birlestirir
$root = "C:\Users\hekizoglu\Documents\AppOrganizer\app\src\main\java"
$out  = "C:\Users\hekizoglu\Documents\AppOrganizer\docs\all_code.txt"

$files = Get-ChildItem -Path $root -Recurse -Filter "*.kt" | Sort-Object FullName
$total = $files.Count
$sb = [System.Text.StringBuilder]::new()

foreach ($f in $files) {
    $rel = $f.FullName.Replace($root, "").TrimStart("\")
    $null = $sb.AppendLine("// ===== $rel =====")
    $null = $sb.AppendLine((Get-Content $f.FullName -Raw -Encoding UTF8))
    $null = $sb.AppendLine()
}

$sb.ToString() | Out-File -FilePath $out -Encoding utf8
$size = [math]::Round((Get-Item $out).Length / 1KB)
Write-Host "Tamamlandi: $total dosya -> all_code.txt ($size KB)"
