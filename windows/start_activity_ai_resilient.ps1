$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir = Join-Path $env:LOCALAPPDATA "ActivityAI\logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$Log = Join-Path $LogDir "collector-launcher.log"

function Write-ActivityLog([string]$Message) {
  Add-Content -Path $Log -Value ("{0:o} {1}" -f (Get-Date), $Message)
}

$MaxRestarts = 5
$WindowSeconds = 300
$RestartTimes = @()

while ($true) {
  $now = Get-Date
  $RestartTimes = @($RestartTimes | Where-Object { ($now - $_).TotalSeconds -lt $WindowSeconds })
  if ($RestartTimes.Count -ge $MaxRestarts) {
    Write-ActivityLog "Crash loop protection triggered; stopping after $MaxRestarts restarts in $WindowSeconds seconds."
    exit 20
  }
  Write-ActivityLog "Starting collector."
  Push-Location $Root
  try {
    python .\run_collector.py
    $code = $LASTEXITCODE
  } finally {
    Pop-Location
  }
  if ($code -eq 0) {
    Write-ActivityLog "Collector exited normally."
    exit 0
  }
  $RestartTimes += (Get-Date)
  Write-ActivityLog "Collector exited with code $code; retrying in 5 seconds."
  Start-Sleep -Seconds 5
}
