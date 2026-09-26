# SafeSphere Build Script for PowerShell
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "  Compiling SafeSphere Emergency Orchestration Platform" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Force -Path "bin" | Out-Null
}

$sources = @(
    (Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java").FullName
    (Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java").FullName
)

javac -encoding UTF-8 -cp "lib/*" -d bin $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Compilation completed successfully! Output in bin/" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
