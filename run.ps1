# SafeSphere Runner Script for PowerShell
param (
    [string]$Mode = "--demo"
)

if (-not (Test-Path "bin")) {
    Write-Host "Compiling SafeSphere project first..." -ForegroundColor Yellow
    & .\build.ps1
}

java -cp "bin;lib/*" com.safesphere.Main $Mode
