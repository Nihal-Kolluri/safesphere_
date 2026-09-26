# SafeSphere Automated Verification Suite for PowerShell
if (-not (Test-Path "bin")) {
    Write-Host "Compiling SafeSphere project first..." -ForegroundColor Yellow
    & .\build.ps1
}

java -cp "bin;lib/*" com.safesphere.SafeSphereTestSuite
