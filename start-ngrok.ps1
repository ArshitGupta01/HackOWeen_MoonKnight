# TheekHai Full-Stack Ngrok Deployment Script
# Exposes BOTH the Web Frontend (Vite on 5173) AND the Ktor Backend (/api/v1 & /health on 8080)
# through a single unified HTTPS ngrok URL using the official @ngrok/ngrok SDK!

param(
    [string]$AuthToken = $env:NGROK_AUTHTOKEN,
    [int]$Port = 5173
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RepoRoot

# 1. Read NGROK_AUTHTOKEN from .env if present
if (-not $AuthToken -and (Test-Path ".env")) {
    $envLine = Get-Content ".env" | Where-Object { $_ -match "^\s*NGROK_AUTHTOKEN\s*=" } | Select-Object -First 1
    if ($envLine) {
        $AuthToken = ($envLine -split "=", 2)[1].Trim().Trim('"').Trim("'")
    }
}

if ($AuthToken) {
    $env:NGROK_AUTHTOKEN = $AuthToken
}
$env:NGROK_PORT = "$Port"

# 2. Verify Ktor Backend (port 8080) is running
$backendAlive = $false
try {
    $resp = Invoke-WebRequest -Uri "http://127.0.0.1:8080/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
    if ($resp.StatusCode -eq 200) { $backendAlive = $true }
} catch {}

if (-not $backendAlive) {
    Write-Host "Starting Ktor Backend Server on port 8080..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$RepoRoot'; .\gradlew.bat :server:run --no-configuration-cache"
    Write-Host "Waiting for Ktor Backend (http://127.0.0.1:8080/health)..." -ForegroundColor Yellow
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        try {
            $r = Invoke-WebRequest -Uri "http://127.0.0.1:8080/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($r.StatusCode -eq 200) { $backendAlive = $true; break }
        } catch {}
    }
}

# 3. Verify Vite Web Server (port 5173, which proxies /api and /health -> 8080) is running
$webAlive = $false
try {
    $respWeb = Invoke-WebRequest -Uri "http://127.0.0.1:5173" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
    if ($respWeb.StatusCode -eq 200) { $webAlive = $true }
} catch {}

if (-not $webAlive) {
    Write-Host "Starting Vite Web Frontend + API Proxy on port 5173..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$RepoRoot\web'; npm run dev"
    Start-Sleep -Seconds 3
}

# 4. Launch @ngrok/ngrok tunnel via web/ngrok-tunnel.mjs
Set-Location "$RepoRoot\web"
node ngrok-tunnel.mjs
