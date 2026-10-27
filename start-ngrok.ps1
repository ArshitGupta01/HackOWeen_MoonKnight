# TheekHai Full-Stack Ngrok Deployment Script
# Exposes BOTH the Web Frontend (Vite on 5173) AND the Ktor Backend (/api/v1 & /health on 8080)
# through a single unified HTTPS ngrok URL!

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

# 2. Locate ngrok executable (supports PATH or WinGet package folder)
$ngrokCmd = Get-Command ngrok -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
if (-not $ngrokCmd) {
    $ngrokCmd = Get-ChildItem -Path "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Filter "ngrok.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $ngrokCmd) {
    Write-Host "Installing ngrok via WinGet..." -ForegroundColor Yellow
    winget install Ngrok.Ngrok --accept-source-agreements --accept-package-agreements
    $ngrokCmd = Get-ChildItem -Path "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Filter "ngrok.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}

if ($AuthToken) {
    Write-Host "Configuring ngrok authtoken..." -ForegroundColor Cyan
    & $ngrokCmd config add-authtoken $AuthToken | Out-Null
}

# 3. Verify Ktor Backend (port 8080) is running
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

# 4. Verify Vite Web Server (port 5173, which proxies /api and /health -> 8080) is running
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

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "  TheekHai Unified Ngrok Deployment Ready!" -ForegroundColor Green
Write-Host "  - Web App       : https://<your-ngrok-domain>/" -ForegroundColor Green
Write-Host "  - Backend API   : https://<your-ngrok-domain>/api/v1" -ForegroundColor Green
Write-Host "  - Health Check  : https://<your-ngrok-domain>/health" -ForegroundColor Green
Write-Host "  - Ngrok Web UI  : http://127.0.0.1:4040" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Green
Write-Host ""

& $ngrokCmd http $Port
