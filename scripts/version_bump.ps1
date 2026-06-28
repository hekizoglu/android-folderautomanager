# version_bump.ps1 — Semantic version artırma yardımcısı
# Kullanım:
#   .\scripts\version_bump.ps1 patch   # 1.0.0 → 1.0.1
#   .\scripts\version_bump.ps1 minor   # 1.0.0 → 1.1.0
#   .\scripts\version_bump.ps1 major   # 1.0.0 → 2.0.0

param(
    [ValidateSet("patch","minor","major")]
    [string]$Type = "patch"
)

$gradle = "app\build.gradle.kts"
$content = Get-Content $gradle -Raw -Encoding utf8

# Mevcut değerleri oku
if ($content -match 'versionCode\s*=\s*(\d+)') { $code = [int]$Matches[1] } else { Write-Error "versionCode bulunamadı"; exit 1 }
if ($content -match 'versionName\s*=\s*"(\d+)\.(\d+)\.(\d+)"') {
    $major = [int]$Matches[1]; $minor = [int]$Matches[2]; $patch = [int]$Matches[3]
} else { Write-Error "versionName bulunamadı"; exit 1 }

# Yeni versiyon hesapla
switch ($Type) {
    "patch" { $patch++ }
    "minor" { $minor++; $patch = 0 }
    "major" { $major++; $minor = 0; $patch = 0 }
}
$newCode = $code + 1
$newName = "$major.$minor.$patch"

# Güncelle
$content = $content -replace "versionCode\s*=\s*\d+", "versionCode = $newCode"
$content = $content -replace 'versionName\s*=\s*"\d+\.\d+\.\d+"', "versionName = `"$newName`""
[System.IO.File]::WriteAllText((Resolve-Path $gradle), $content, [System.Text.Encoding]::UTF8)

Write-Output "versionCode: $code → $newCode"
Write-Output "versionName: $major.$(if($Type -eq 'patch'){$minor}else{if($Type -eq 'minor'){$minor-1}else{$major-1}}).$(if($Type -eq 'patch'){$patch-1}else{$patch}) → $newName"
Write-Output "Güncellendi: $gradle"
