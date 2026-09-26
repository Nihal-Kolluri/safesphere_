# Build SafeSphere Native Android APK
Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "  Building SafeSphere Native Android APK" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

Push-Location "android"
try {
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "=========================================================" -ForegroundColor Green
        Write-Host "[SUCCESS] Android APK built successfully!" -ForegroundColor Green
        Write-Host "Location: android\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
        Write-Host "=========================================================" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Android APK build failed with code $LASTEXITCODE" -ForegroundColor Red
    }
} finally {
    Pop-Location
}
