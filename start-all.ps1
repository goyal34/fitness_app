#Requires -Version 5.1
<#
.SYNOPSIS
    Starts the complete fitness application with one command.

.DESCRIPTION
    Starts PostgreSQL, MongoDB, RabbitMQ and Keycloak (Docker), then starts
    every Spring Boot service in the correct order, bootstraps the Keycloak
    realm/demo user, and finally starts the React frontend.

    Run this from the repository root:
        .\start-all.ps1
    or double-click start-all.cmd.

    Logs are written to .logs\. Stop everything with .\stop-all.ps1.

.PARAMETER SkipInfra
    Do not start Docker containers. Use this when PostgreSQL, MongoDB,
    RabbitMQ and Keycloak are already running elsewhere.

.PARAMETER SkipFrontend
    Start only the backend services and infrastructure.

.PARAMETER NoBrowser
    Do not open the frontend in the default browser when startup finishes.

.PARAMETER StartupTimeoutSeconds
    How long to wait for each service to become ready. Default: 300.
#>
[CmdletBinding()]
param(
    [switch]$SkipInfra,
    [switch]$SkipFrontend,
    [switch]$NoBrowser,
    [int]$StartupTimeoutSeconds = 300
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Definition
if (-not $Root) { $Root = (Get-Location).Path }
Set-Location -LiteralPath $Root

$LogDir = Join-Path $Root '.logs'
$RunDir = Join-Path $Root '.run'
$StateFile = Join-Path $RunDir 'services.json'
New-Item -ItemType Directory -Force -Path $LogDir, $RunDir | Out-Null

$script:Started = @{}

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host ">>> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[ok]    $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "[info]  $Message" -ForegroundColor Gray
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[warn]  $Message" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Message)
    Write-Host "[error] $Message" -ForegroundColor Red
}

# ---------------------------------------------------------------------------
# Environment
# ---------------------------------------------------------------------------

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) { return }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }

        if ($trimmed -match '^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim()

            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            # Values already exported in the shell take precedence.
            if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
                [Environment]::SetEnvironmentVariable($name, $value, [EnvironmentVariableTarget]::Process)
            }
        }
    }
}

Import-DotEnv (Join-Path $Root '.env')

if (-not $env:POSTGRES_USER) { $env:POSTGRES_USER = 'postgres' }
if (-not $env:POSTGRES_PASSWORD) {
    $env:POSTGRES_PASSWORD = 'postgres'
    Write-Warn "POSTGRES_PASSWORD is not set; using the local development default 'postgres'."
}
if (-not $env:KEYCLOAK_ADMIN) { $env:KEYCLOAK_ADMIN = 'admin' }
if (-not $env:KEYCLOAK_ADMIN_PASSWORD) { $env:KEYCLOAK_ADMIN_PASSWORD = 'admin' }
if (-not $env:APP_TIMEZONE) { $env:APP_TIMEZONE = 'Asia/Kolkata' }

# ---------------------------------------------------------------------------
# General helpers
# ---------------------------------------------------------------------------

function Test-TcpHost {
    param(
        [string]$Address,
        [int]$Port,
        [int]$TimeoutMilliseconds = 400
    )

    $family = [System.Net.Sockets.AddressFamily]::InterNetwork
    if ($Address.Contains(':')) { $family = [System.Net.Sockets.AddressFamily]::InterNetworkV6 }

    $client = New-Object System.Net.Sockets.TcpClient -ArgumentList $family
    try {
        $task = $client.ConnectAsync($Address, $Port)
        if ($task.Wait($TimeoutMilliseconds)) { return $client.Connected }
        return $false
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Test-Port {
    param(
        [int]$Port,
        [int]$TimeoutMilliseconds = 400
    )

    if (Test-TcpHost -Address '127.0.0.1' -Port $Port -TimeoutMilliseconds $TimeoutMilliseconds) { return $true }
    if (Test-TcpHost -Address '::1' -Port $Port -TimeoutMilliseconds $TimeoutMilliseconds) { return $true }
    return $false
}

function Show-LogTail {
    param(
        [string]$Path,
        [string]$ErrorPath
    )

    if ($Path -and (Test-Path -LiteralPath $Path)) {
        Write-Host "--- $Path (last 30 lines) ---" -ForegroundColor Yellow
        Get-Content -LiteralPath $Path -Tail 30 | ForEach-Object { Write-Host $_ }
    }
    if ($ErrorPath -and (Test-Path -LiteralPath $ErrorPath)) {
        $tail = Get-Content -LiteralPath $ErrorPath -Tail 30
        if ($tail) {
            Write-Host "--- $ErrorPath (last 30 lines) ---" -ForegroundColor Yellow
            $tail | ForEach-Object { Write-Host $_ }
        }
    }
}

function Show-ContainerLogs {
    param([string]$Service)

    try {
        $lines = & docker compose logs --tail 40 $Service 2>&1
        if ($LASTEXITCODE -eq 0 -and $lines) {
            Write-Host "--- docker compose logs $Service (last 40 lines) ---" -ForegroundColor Yellow
            $lines | Select-Object -Last 40 | ForEach-Object { Write-Host $_ }
        }
    }
    catch {
        # Docker may not be available when -SkipInfra is used.
    }
}

function Wait-ForEndpoint {
    param(
        [string]$Name,
        [int]$Port,
        [string]$Url,
        [int]$TimeoutSeconds,
        [int]$ProcessId = 0,
        [string]$LogOut,
        [string]$LogErr
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $announced = $false

    while ((Get-Date) -lt $deadline) {
        if (Test-Port -Port $Port) {
            if ([string]::IsNullOrWhiteSpace($Url)) {
                Write-Ok "$Name is ready (port $Port)."
                return
            }

            try {
                $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 10
                if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                    Write-Ok "$Name is ready (port $Port)."
                    return
                }
            }
            catch {
                # A 401/404 still proves that the HTTP server is listening.
                if ($_.Exception.Response) {
                    Write-Ok "$Name is ready (port $Port)."
                    return
                }
            }
        }

        if ($ProcessId -gt 0 -and -not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            Write-Err "$Name exited before it became ready."
            Show-LogTail -Path $LogOut -ErrorPath $LogErr
            throw "$Name failed to start. See $LogOut and $LogErr."
        }

        if (-not $announced) {
            Write-Info "waiting for $Name on port $Port..."
            $announced = $true
        }

        Start-Sleep -Milliseconds 1500
    }

    Write-Err "Timed out waiting for $Name on port $Port."
    Show-LogTail -Path $LogOut -ErrorPath $LogErr
    if ($Name -eq 'Keycloak') { Show-ContainerLogs -Service 'keycloak' }
    throw "$Name did not become ready within $TimeoutSeconds seconds."
}

# ---------------------------------------------------------------------------
# State (so stop-all.ps1 can stop only the processes this script started)
# ---------------------------------------------------------------------------

function Read-State {
    if (-not (Test-Path -LiteralPath $StateFile)) { return }

    try {
        $state = Get-Content -LiteralPath $StateFile -Raw | ConvertFrom-Json
        foreach ($property in $state.PSObject.Properties) {
            $entry = $property.Value
            if ($entry.pid -and (Get-Process -Id ([int]$entry.pid) -ErrorAction SilentlyContinue)) {
                $script:Started[$property.Name] = @{
                    pid  = [int]$entry.pid
                    port = [int]$entry.port
                    log  = [string]$entry.log
                    err  = [string]$entry.err
                }
            }
        }
    }
    catch {
        Write-Warn "Could not read the previous launcher state; starting fresh."
    }
}

function Save-State {
    $state = @{}
    foreach ($name in $script:Started.Keys) {
        $state[$name] = $script:Started[$name]
    }

    $json = $state | ConvertTo-Json -Depth 5
    $tempFile = "$StateFile.$PID.tmp"

    [System.IO.File]::WriteAllText($tempFile, $json, [System.Text.Encoding]::UTF8)

    $attempts = 0
    while ($true) {
        try {
            Move-Item -LiteralPath $tempFile -Destination $StateFile -Force
            return
        }
        catch {
            $attempts++
            if ($attempts -ge 10) {
                Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
                throw
            }
            Start-Sleep -Milliseconds 200
        }
    }
}

Read-State

# ---------------------------------------------------------------------------
# Infrastructure
# ---------------------------------------------------------------------------

function Start-Infrastructure {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker was not found. Install Docker Desktop, or start the databases yourself and run .\start-all.ps1 -SkipInfra."
    }

    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is installed but not running. Start Docker Desktop and try again."
    }

    Write-Step "Starting infrastructure"

    $infra = @(
        @{ Service = 'postgres'; Label = 'PostgreSQL'; Port = 5432 },
        @{ Service = 'mongodb';  Label = 'MongoDB';    Port = 27017 },
        @{ Service = 'rabbitmq'; Label = 'RabbitMQ';   Port = 5672 },
        @{ Service = 'keycloak'; Label = 'Keycloak';   Port = 8181 }
    )

    foreach ($item in $infra) {
        if (Test-Port -Port $item.Port) {
            Write-Ok "$($item.Label) is already running on port $($item.Port); reusing it."
        }
        else {
            Write-Info "starting $($item.Label) (docker compose service '$($item.Service)')..."
            & docker compose up -d $item.Service
            if ($LASTEXITCODE -ne 0) {
                throw "Could not start $($item.Label) with docker compose."
            }
        }
    }
}

function Wait-ForInfrastructure {
    Write-Step "Waiting for infrastructure"
    Wait-ForEndpoint -Name 'PostgreSQL' -Port 5432 -Url '' -TimeoutSeconds 90
    Wait-ForEndpoint -Name 'MongoDB'    -Port 27017 -Url '' -TimeoutSeconds 90
    Wait-ForEndpoint -Name 'RabbitMQ'   -Port 5672  -Url '' -TimeoutSeconds 90
    Wait-ForEndpoint -Name 'Keycloak'   -Port 8181  -Url 'http://localhost:8181/realms/master' -TimeoutSeconds 600
}

# ---------------------------------------------------------------------------
# Keycloak bootstrap
# ---------------------------------------------------------------------------

function Initialize-Keycloak {
    $base = 'http://localhost:8181'
    Write-Step "Configuring Keycloak"

    $token = $null
    $deadline = (Get-Date).AddSeconds(90)

    while (-not $token -and (Get-Date) -lt $deadline) {
        try {
            $body = @{
                client_id  = 'admin-cli'
                username   = $env:KEYCLOAK_ADMIN
                password   = $env:KEYCLOAK_ADMIN_PASSWORD
                grant_type = 'password'
            }
            $response = Invoke-RestMethod -Method Post `
                -Uri "$base/realms/master/protocol/openid-connect/token" `
                -Body $body `
                -ContentType 'application/x-www-form-urlencoded' `
                -TimeoutSec 15
            $token = $response.access_token
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    if (-not $token) {
        throw "Could not log in to Keycloak as '$($env:KEYCLOAK_ADMIN)'. Check KEYCLOAK_ADMIN and KEYCLOAK_ADMIN_PASSWORD in .env."
    }

    $headers = @{ Authorization = "Bearer $token" }

    # Realm
    $realmExists = $false
    try {
        Invoke-RestMethod -Method Get -Uri "$base/admin/realms/fitness-oauth2" -Headers $headers -TimeoutSec 15 | Out-Null
        $realmExists = $true
    }
    catch {
        $status = 0
        try { $status = [int]$_.Exception.Response.StatusCode } catch { }
        if ($status -ne 404) { throw }
    }

    if (-not $realmExists) {
        $realmBody = @{ realm = 'fitness-oauth2'; enabled = $true } | ConvertTo-Json
        Invoke-RestMethod -Method Post -Uri "$base/admin/realms" -Headers $headers `
            -ContentType 'application/json' -Body $realmBody -TimeoutSec 30 | Out-Null
        Write-Ok "created realm 'fitness-oauth2'."
    }

    # Realm settings, applied on every start so older realms get them too.
    # Self sign-up is on. Email verification and "forgot password" stay off
    # because they need an SMTP server (configure one under Realm settings > Email).
    $realmSettings = @{
        registrationAllowed         = $true
        registrationEmailAsUsername = $false
        loginWithEmailAllowed       = $true
        duplicateEmailsAllowed      = $false
        rememberMe                  = $true
        verifyEmail                 = $false
        resetPasswordAllowed        = $false
        bruteForceProtected         = $true
        passwordPolicy              = 'length(8) and notUsername and notEmail'
    } | ConvertTo-Json
    Invoke-RestMethod -Method Put -Uri "$base/admin/realms/fitness-oauth2" -Headers $headers `
        -ContentType 'application/json' -Body $realmSettings -TimeoutSec 30 | Out-Null
    Write-Ok "self sign-up enabled (minimum password length 8)."

    # PKCE client
    $clients = Invoke-RestMethod -Method Get `
        -Uri "$base/admin/realms/fitness-oauth2/clients?clientId=oauth2-pkce-client" `
        -Headers $headers -TimeoutSec 15
    if ($null -eq $clients) { $clients = @() } else { $clients = @($clients) }

    if ($clients.Count -eq 0) {
        $clientBody = @{
            clientId                  = 'oauth2-pkce-client'
            enabled                   = $true
            publicClient              = $true
            standardFlowEnabled       = $true
            directAccessGrantsEnabled = $true
            redirectUris              = @('http://localhost:5173/*', 'http://localhost:5173')
            webOrigins                = @('http://localhost:5173', '+')
            attributes                = @{ 'pkce.code.challenge.method' = 'S256' }
        } | ConvertTo-Json -Depth 6

        Invoke-RestMethod -Method Post -Uri "$base/admin/realms/fitness-oauth2/clients" `
            -Headers $headers -ContentType 'application/json' -Body $clientBody -TimeoutSec 30 | Out-Null
        Write-Ok "created client 'oauth2-pkce-client'."
        $clients = @(Invoke-RestMethod -Method Get `
            -Uri "$base/admin/realms/fitness-oauth2/clients?clientId=oauth2-pkce-client" `
            -Headers $headers -TimeoutSec 15)
    }

    # Allow Keycloak to send people back to the app after logout ("+" = same as
    # the redirect URIs). Applied every start so older clients get it too.
    $logoutBody = @{
        attributes = @{ 'post.logout.redirect.uris' = '+' }
    } | ConvertTo-Json -Depth 4
    Invoke-RestMethod -Method Put -Uri "$base/admin/realms/fitness-oauth2/clients/$($clients[0].id)" `
        -Headers $headers -ContentType 'application/json' -Body $logoutBody -TimeoutSec 30 | Out-Null

    # Demo user
    $users = Invoke-RestMethod -Method Get `
        -Uri "$base/admin/realms/fitness-oauth2/users?username=testuser&exact=true" `
        -Headers $headers -TimeoutSec 15
    if ($null -eq $users) { $users = @() } else { $users = @($users) }

    if ($users.Count -eq 0) {
        $userBody = @{
            username      = 'testuser'
            email         = 'testuser@fitness.local'
            emailVerified = $true
            firstName     = 'Test'
            lastName      = 'User'
            enabled       = $true
            # Must satisfy the realm's password policy above (8+ characters).
            credentials   = @(@{ type = 'password'; value = 'test1234'; temporary = $false })
        } | ConvertTo-Json -Depth 6

        # The demo account is a convenience; don't stop the whole startup if it fails.
        try {
            Invoke-RestMethod -Method Post -Uri "$base/admin/realms/fitness-oauth2/users" `
                -Headers $headers -ContentType 'application/json' -Body $userBody -TimeoutSec 30 | Out-Null
            Write-Ok "created demo user 'testuser' (password: test1234)."
        }
        catch {
            Write-Warn "Could not create the demo user 'testuser': $($_.Exception.Message). Use 'Create account' instead."
        }
    }

    Write-Ok "Keycloak is ready."
}

# ---------------------------------------------------------------------------
# Spring Boot services
# ---------------------------------------------------------------------------

function Start-BackendService {
    param(
        [string]$Name,
        [string]$Directory,
        [int]$Port,
        [string]$Url,
        [int]$TimeoutSeconds = 300
    )

    if (Test-Port -Port $Port) {
        Write-Ok "$Name already appears to be running on port $Port; leaving it alone."
        return
    }

    $serviceDir = Join-Path $Root $Directory
    if (-not (Test-Path -LiteralPath (Join-Path $serviceDir 'pom.xml'))) {
        throw "Cannot find $serviceDir\pom.xml."
    }

    $outLog = Join-Path $LogDir "$Name.out.log"
    $errLog = Join-Path $LogDir "$Name.err.log"
    Remove-Item -LiteralPath $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $command = 'mvn spring-boot:run'
    if ($env:APP_TIMEZONE -and $env:APP_TIMEZONE -ne 'none') {
        $command += " -Dspring-boot.run.jvmArguments=-Duser.timezone=$($env:APP_TIMEZONE)"
    }

    Write-Info "starting $Name ($command)"
    $process = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList @('/d', '/c', $command) `
        -WorkingDirectory $serviceDir `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden `
        -PassThru

    $script:Started[$Name] = @{
        pid  = $process.Id
        port = $Port
        log  = $outLog
        err  = $errLog
    }
    Save-State

    Wait-ForEndpoint -Name $Name -Port $Port -Url $Url `
        -TimeoutSeconds $TimeoutSeconds `
        -ProcessId $process.Id `
        -LogOut $outLog -LogErr $errLog
}

# ---------------------------------------------------------------------------
# Frontend
# ---------------------------------------------------------------------------

function Start-Frontend {
    $port = 5173
    if (Test-Port -Port $port) {
        Write-Ok "Frontend already appears to be running on port $port; leaving it alone."
        return
    }

    $frontendDir = Join-Path $Root 'fitness-app-frontend'
    if (-not (Test-Path -LiteralPath (Join-Path $frontendDir 'package.json'))) {
        throw "Cannot find $frontendDir\package.json."
    }
    if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
        throw "npm was not found. Install Node.js 18+ and try again."
    }

    if (-not (Test-Path -LiteralPath (Join-Path $frontendDir 'node_modules'))) {
        Write-Step "Installing frontend dependencies (first run only)"
        Push-Location $frontendDir
        try {
            & npm ci
            if ($LASTEXITCODE -ne 0) {
                throw "npm ci failed with exit code $LASTEXITCODE."
            }
        }
        finally {
            Pop-Location
        }
    }

    $outLog = Join-Path $LogDir 'frontend.out.log'
    $errLog = Join-Path $LogDir 'frontend.err.log'
    Remove-Item -LiteralPath $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $command = 'npm run dev -- --port 5173 --strictPort'
    Write-Info "starting frontend ($command)"
    $process = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList @('/d', '/c', $command) `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden `
        -PassThru

    $script:Started['frontend'] = @{
        pid  = $process.Id
        port = $port
        log  = $outLog
        err  = $errLog
    }
    Save-State

    Wait-ForEndpoint -Name 'frontend' -Port $port -Url 'http://localhost:5173/' `
        -TimeoutSeconds 120 -ProcessId $process.Id `
        -LogOut $outLog -LogErr $errLog
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

try {
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host " Fitness App - single command startup" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan

    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "Maven (mvn) was not found. Install Maven 3.9+ or add it to PATH."
    }

    if (-not $SkipInfra) {
        Start-Infrastructure
    }
    else {
        Write-Warn "Skipping Docker infrastructure (-SkipInfra)."
    }

    Wait-ForInfrastructure
    Initialize-Keycloak

    Write-Step "Starting backend services (this can take a few minutes on first run)"
    Start-BackendService -Name 'configserver'   -Directory 'configserver'   -Port 8888 -Url 'http://localhost:8888/user-service/default' -TimeoutSeconds $StartupTimeoutSeconds
    Start-BackendService -Name 'eureka'         -Directory 'eureka'         -Port 8761 -Url 'http://localhost:8761/eureka/apps'            -TimeoutSeconds $StartupTimeoutSeconds
    Start-BackendService -Name 'userservice'    -Directory 'userservice'    -Port 8081 -Url 'http://localhost:8081/'                      -TimeoutSeconds $StartupTimeoutSeconds
    Start-BackendService -Name 'activityservice' -Directory 'activityservice' -Port 8082 -Url 'http://localhost:8082/'                    -TimeoutSeconds $StartupTimeoutSeconds
    Start-BackendService -Name 'aiservice'      -Directory 'aiservice'      -Port 8083 -Url 'http://localhost:8083/'                      -TimeoutSeconds $StartupTimeoutSeconds
    Start-BackendService -Name 'gateway'        -Directory 'gateway'        -Port 8080 -Url 'http://localhost:8080/'                      -TimeoutSeconds $StartupTimeoutSeconds

    if (-not $SkipFrontend) {
        Write-Step "Starting frontend"
        Start-Frontend
    }

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host " The fitness app is running" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  Frontend:       http://localhost:5173"
    Write-Host "  API Gateway:    http://localhost:8080"
    Write-Host "  Eureka:         http://localhost:8761"
    Write-Host "  Config Server:  http://localhost:8888"
    Write-Host "  Keycloak:       http://localhost:8181  (admin / $($env:KEYCLOAK_ADMIN_PASSWORD))"
    Write-Host "  RabbitMQ UI:    http://localhost:15672 (guest / guest)"
    Write-Host "  Demo login:     testuser / test1234"
    Write-Host ""
    Write-Host "  Logs:           $LogDir"
    Write-Host "  Stop:           .\stop-all.ps1"
    Write-Host "============================================================" -ForegroundColor Green

    if (-not $SkipFrontend -and -not $NoBrowser) {
        Start-Process 'http://localhost:5173'
    }
}
catch {
    Write-Host ""
    Write-Err $_.Exception.Message
    Write-Host "Logs are in: $LogDir" -ForegroundColor Yellow
    Write-Host "Stop services started by this script with: .\stop-all.ps1" -ForegroundColor Yellow
    exit 1
}
