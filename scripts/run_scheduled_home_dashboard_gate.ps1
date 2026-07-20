[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
$logDir = Join-Path $repoRoot "artifacts"
$logPath = Join-Path $logDir "scheduled_home_dashboard_gate.log"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null
Set-Location -LiteralPath $repoRoot

& (Join-Path $PSScriptRoot "run_home_dashboard_gate.ps1") -SkipLockClear *>&1 |
    Tee-Object -FilePath $logPath

exit $LASTEXITCODE
