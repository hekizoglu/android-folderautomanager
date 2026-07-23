param(
    [string]$Alias = "apporganizer",
    [int]$ValidityDays = 10000
)

# Creates local release signing files. Do not commit release.jks or keystore.properties.

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$keystorePath = Join-Path $repoRoot "release.jks"
$propsPath = Join-Path $repoRoot "keystore.properties"

if (Test-Path $keystorePath) {
    throw "release.jks already exists: $keystorePath"
}

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw "keytool bulunamadi. Java JDK bin klasoru PATH icinde olmali."
}

function ConvertTo-PlainText {
    param([securestring]$Value)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

$storePasswordSecure = Read-Host "Release keystore password" -AsSecureString
$keyPasswordSecure = Read-Host "Release key password (bos birakmak icin Enter)" -AsSecureString
$storePassword = ConvertTo-PlainText $storePasswordSecure
$keyPassword = ConvertTo-PlainText $keyPasswordSecure

if ([string]::IsNullOrWhiteSpace($storePassword)) {
    throw "Keystore password bos olamaz."
}

if ([string]::IsNullOrWhiteSpace($keyPassword)) {
    $keyPassword = $storePassword
}

Push-Location $repoRoot
try {
    & keytool `
        -genkeypair `
        -v `
        -keystore $keystorePath `
        -storetype JKS `
        -alias $Alias `
        -keyalg RSA `
        -keysize 2048 `
        -validity $ValidityDays `
        -storepass $storePassword `
        -keypass $keyPassword `
        -dname "CN=AppOrganizer, OU=Release, O=AppOrganizer, L=Armutlu, ST=Yalova, C=TR"

    if ($LASTEXITCODE -ne 0) {
        throw "keytool basarisiz oldu."
    }

    $lines = @(
        "storeFile=release.jks",
        "storePassword=$storePassword",
        "keyAlias=$Alias",
        "keyPassword=$keyPassword"
    )
    [System.IO.File]::WriteAllText($propsPath, ($lines -join [Environment]::NewLine) + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
} finally {
    Pop-Location
}

Write-Host "Release keystore olusturuldu: $keystorePath" -ForegroundColor Green
Write-Host "Signing properties olusturuldu: $propsPath" -ForegroundColor Green
Write-Host "Bu iki dosya .gitignore kapsaminda kalmali ve guvenli yedeklenmeli." -ForegroundColor Yellow
