[CmdletBinding()]
param(
    [string] $Serial,
    [string] $Apk = "app\build\outputs\apk\debug\app-debug.apk",
    [string] $ReportDir = "build\reports\real-device-smoke",
    [int] $WaitSeconds = 4,
    [switch] $SkipInstall
)

$ErrorActionPreference = "Stop"
$packageName = "com.armutlu.apporganizer"
$launcherActivity = "$packageName/.presentation.ui.launcher.LauncherActivity"
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\")).Path
$apkPath = (Resolve-Path -LiteralPath (Join-Path $repoRoot $Apk)).Path
$reportPath = Join-Path $repoRoot $ReportDir

function Invoke-Adb {
    param([Parameter(Mandatory = $true)][string[]] $Arguments)
    & $script:adb @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ADB failed ($LASTEXITCODE): adb $($Arguments -join ' ')"
    }
}

function Save-Screenshot {
    param([Parameter(Mandatory = $true)][string] $Name)
    $target = Join-Path $reportPath "$Name.png"
    $quotedAdb = '"' + $script:adb + '"'
    $quotedTarget = '"' + $target + '"'
    cmd.exe /c "$quotedAdb -s $script:serial exec-out screencap -p > $quotedTarget"
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $target)) {
        throw "Screenshot failed: $target"
    }
    return $target
}

$adbCandidates = @(
    (Join-Path ${env:LOCALAPPDATA} "Android\Sdk\platform-tools\adb.exe"),
    "adb"
)
$script:adb = $adbCandidates | Where-Object {
    $_ -eq "adb" -or (Test-Path -LiteralPath $_)
} | Select-Object -First 1
if (-not $script:adb) { throw "adb.exe bulunamadi." }

New-Item -ItemType Directory -Path $reportPath -Force | Out-Null
$devices = @(& $script:adb devices | Select-String "\sdevice$")
if (-not $Serial) {
    $Serial = ($devices | Select-Object -First 1).ToString().Split("`t")[0]
}
if (-not $Serial) { throw "Bagli ve yetkili Android cihaz bulunamadi." }
$script:serial = $Serial

if (-not $SkipInstall) {
    Invoke-Adb @("-s", $script:serial, "install", "-r", $apkPath)
}

Invoke-Adb @("-s", $script:serial, "shell", "svc", "power", "stayon", "true")
Invoke-Adb @("-s", $script:serial, "shell", "input", "keyevent", "224")
Invoke-Adb @("-s", $script:serial, "shell", "am", "force-stop", $packageName)
Invoke-Adb @("-s", $script:serial, "shell", "am", "start", "-n", $launcherActivity)
Start-Sleep -Seconds $WaitSeconds
$homeScreenshot = Save-Screenshot "home"

Invoke-Adb @("-s", $script:serial, "shell", "input", "swipe", "1050", "1200", "150", "1200", "800")
Start-Sleep -Seconds 2
$folderScreenshot = Save-Screenshot "folder-page"
$uiDumpPath = Join-Path $reportPath "folder-ui.xml"
Invoke-Adb @("-s", $script:serial, "shell", "uiautomator", "dump", "/sdcard/apporganizer-home-smoke.xml") | Out-Null
& $script:adb -s $script:serial shell cat /sdcard/apporganizer-home-smoke.xml | Set-Content -LiteralPath $uiDumpPath -Encoding UTF8
$folderMarker = Select-String -Path $uiDumpPath -Pattern 'content-desc="Klas' -Quiet

$packageInfo = (& $script:adb -s $script:serial shell dumpsys package $packageName | Select-String "versionCode|versionName") -join " | "
$fatalLines = @(& $script:adb -s $script:serial logcat -d -v brief -t 1200 | Select-String "FATAL EXCEPTION")
$hash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
$summary = @(
    "AppOrganizer real device home smoke",
    "Timestamp: $timestamp",
    "Serial: $script:serial",
    "APK: $apkPath",
    "APK SHA256: $hash",
    "Package: $packageInfo",
    "Home screenshot: $homeScreenshot",
    "Folder screenshot: $folderScreenshot",
    "Folder UI marker: $folderMarker",
    "Fatal log lines: $($fatalLines.Count)"
)
if ($fatalLines.Count -gt 0) {
    $summary += "--- fatal log ---"
    $summary += $fatalLines
}
$summaryPath = Join-Path $reportPath "summary.txt"
$summary | Set-Content -LiteralPath $summaryPath -Encoding UTF8
$summary | Write-Output

if ($fatalLines.Count -gt 0) {
    throw "Uygulama fatal log uretti; ayrintilar: $summaryPath"
}
if (-not $folderMarker) {
    throw "Klasor sayfasi UI marker bulunamadi; screenshot: $folderScreenshot"
}
