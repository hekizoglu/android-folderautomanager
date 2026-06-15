# Context doluluk kontrolu — PreCompact hook flag'ini izler
# Kullanim: .\check_context.ps1 (bloklar, flag yoksa aninda cikis)

$flag = "C:\Users\hekizoglu\.claude\context_full.flag"
$retryMinutes = 15
$checkIntervalSeconds = 30
$elapsed = 0

if (-not (Test-Path $flag)) { exit 0 }

Write-Host "Context dolu (PreCompact flag aktif). $retryMinutes dk beklenecek, sonra tekrar denenecek..."

while (Test-Path $flag) {
    if ($elapsed -ge ($retryMinutes * 60)) {
        Write-Host "$retryMinutes dk doldu, devam ediliyor (flag temizleniyor)."
        Remove-Item $flag -Force -ErrorAction SilentlyContinue
        break
    }
    Start-Sleep $checkIntervalSeconds
    $elapsed += $checkIntervalSeconds
    $mins = [int]($elapsed / 60)
    Write-Host "  $mins/$retryMinutes dk, bekleniyor..."
}

Write-Host "Context mevcut, devam ediliyor."
exit 0
