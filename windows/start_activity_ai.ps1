$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Write-Host 'Starting Activity AI Milestone 1 Windows collector...'
python .\run_collector.py
