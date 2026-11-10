#Requires -Version 5.1
<#
.SYNOPSIS
    Stops the services started by start-all.ps1.

.PARAMETER KeepInfra
    Stop the Spring Boot/frontend processes but leave Docker containers
    (PostgreSQL, MongoDB, RabbitMQ, Keycloak) running.
#>
[CmdletBinding()]
param(
    [switch]$KeepInfra
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Definition
if (-not $Root) { $Root = (Get-Location).Path }
Set-Location -LiteralPath $Root

$StateFile = Join-Path $Root '.run\services.json'

function Write-Info {
    param([string]$Message)
    Write-Host "[info]  $Message" -ForegroundColor Gray
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[ok]    $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[warn]  $Message" -ForegroundColor Yellow
}

function Stop-ProcessTree {
    param(
        [int]$ProcessId,
        [string]$Name
    )

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if (-not $process) { return }

    & cmd.exe /d /c "taskkill.exe /PID $ProcessId /T /F >nul 2>&1"
    $taskkillExitCode = $LASTEXITCODE
    if ($taskkillExitCode -eq 0) {
        Write-Ok "stopped $Name (PID $ProcessId)."
    }
    elseif (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        Write-Ok "stopped $Name (PID $ProcessId)."
    }
    else {
        Write-Warn "could not stop $Name (PID $ProcessId); it may have already exited."
    }
}

Write-Host ""
Write-Host ">>> Stopping fitness app services" -ForegroundColor Cyan

$stoppedSomething = $false

if (Test-Path -LiteralPath $StateFile) {
    try {
        $state = Get-Content -LiteralPath $StateFile -Raw | ConvertFrom-Json

        foreach ($property in $state.PSObject.Properties) {
            $name = $property.Name
            $entry = $property.Value
            $processId = 0
            if ($entry.pid) { $processId = [int]$entry.pid }
            if ($processId -le 0) { continue }

            $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
            if (-not $process) { continue }

            $commandLine = ''
            try {
                $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
                if ($processInfo) { $commandLine = [string]$processInfo.CommandLine }
            }
            catch { }

            if (-not $commandLine) {
                Write-Warn "skipping PID $processId ($name): could not verify the process command line."
                continue
            }

            if ($commandLine -notmatch 'spring-boot:run|npm run dev|vite') {
                Write-Warn "skipping PID $processId ($name): it does not look like this app's process."
                continue
            }

            Stop-ProcessTree -ProcessId $processId -Name $name
            $stoppedSomething = $true
        }

        Remove-Item -LiteralPath $StateFile -Force -ErrorAction SilentlyContinue
    }
    catch {
        Write-Warn "Could not read .run\services.json: $($_.Exception.Message)"
    }
}

# Also find leftovers even when the launcher state file exists. Maven can
# re-parent its child JVMs, so a recorded cmd.exe PID may be gone while the
# Spring Boot process is still listening.
Write-Info "Checking for leftover fitness app processes..."

$patterns = @(
    'com\.fitness\.\w+\.\w+Application',
    'com\.server\.eureka\.EurekaApplication',
    'fitness-app-microservices-main.*spring-boot:run',
    'fitness-app-frontend[\\/].*vite'
)

$candidates = @()
try {
    $candidates = Get-CimInstance Win32_Process | Where-Object {
        if ([int]$_.ProcessId -eq $PID) { return $false }
        $commandLine = [string]$_.CommandLine
        if (-not $commandLine) { return $false }
        foreach ($pattern in $patterns) {
            if ($commandLine -match $pattern) { return $true }
        }
        return $false
    }
}
catch {
    Write-Warn "Could not scan running processes: $($_.Exception.Message)"
}

foreach ($candidate in $candidates) {
    Stop-ProcessTree -ProcessId ([int]$candidate.ProcessId) -Name $candidate.Name
    $stoppedSomething = $true
}

if (-not $stoppedSomething) {
    Write-Info "No fitness app processes were running."
}

if (-not $KeepInfra) {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        & docker info *> $null
        if ($LASTEXITCODE -eq 0) {
            Write-Info "stopping Docker infrastructure..."
            & docker compose stop
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "Docker infrastructure stopped."
            }
            else {
                Write-Warn "docker compose stop reported an error."
            }
        }
        else {
            Write-Warn "Docker is not running; skipped stopping infrastructure."
        }
    }
    else {
        Write-Warn "Docker was not found; skipped stopping infrastructure."
    }
}
else {
    Write-Info "Leaving Docker infrastructure running (-KeepInfra)."
}

Write-Host ""
Write-Ok "Done."
