param(
    [ValidateSet("status", "search", "wake", "mine-app", "mine-docs", "mine-scripts", "sync")]
    [string]$Mode = "status",
    [string]$Query = ""
)

$ErrorActionPreference = "Stop"

function Run-Step([string]$Title, [scriptblock]$Block) {
    Write-Host ""
    Write-Host "== $Title ==" -ForegroundColor Cyan
    & $Block
}

switch ($Mode) {
    "status" {
        Run-Step "MemPalace Status" { mempalace status }
    }
    "search" {
        if ([string]::IsNullOrWhiteSpace($Query)) {
            throw "search modunda -Query zorunlu."
        }
        Run-Step "AppOrganizer Search" {
            mempalace search $Query --wing apporganizer
        }
    }
    "wake" {
        Run-Step "AppOrganizer Wake-Up" {
            mempalace wake-up --wing apporganizer
        }
    }
    "mine-app" {
        Run-Step "Mine app/" {
            mempalace mine app --wing apporganizer --mode projects --max-chunks-per-file 2000
        }
    }
    "mine-docs" {
        Run-Step "Mine docs/" {
            mempalace mine docs --wing apporganizer --mode projects --max-chunks-per-file 1000
        }
    }
    "mine-scripts" {
        Run-Step "Mine scripts/" {
            mempalace mine scripts --wing apporganizer --mode projects --max-chunks-per-file 1000
        }
    }
    "sync" {
        Run-Step "Dry Run Sync" {
            mempalace sync --wing apporganizer --dry-run
        }
    }
}
