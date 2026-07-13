param(
    [ValidateSet("list", "recover", "save", "mcp")]
    [string]$Mode = "list",
    [string]$ShortId = "",
    [string]$JsonFile = ""
)

$ErrorActionPreference = "Stop"

function Run-Step([string]$Title, [scriptblock]$Block) {
    Write-Host ""
    Write-Host "== $Title ==" -ForegroundColor Cyan
    & $Block
}

switch ($Mode) {
    "list" {
        Run-Step "Memory Palace List" { memory-palace list }
    }
    "recover" {
        if ([string]::IsNullOrWhiteSpace($ShortId)) {
            throw "recover modunda -ShortId zorunlu."
        }
        Run-Step "Memory Palace Recover" { memory-palace recover $ShortId }
    }
    "save" {
        if ([string]::IsNullOrWhiteSpace($JsonFile)) {
            throw "save modunda -JsonFile zorunlu."
        }
        Run-Step "Memory Palace Save" { memory-palace save $JsonFile }
    }
    "mcp" {
        Run-Step "Memory Palace MCP" { memory-palace mcp }
    }
}
