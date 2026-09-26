@echo off
echo =========================================================
echo   Building SafeSphere Native Android APK
echo =========================================================

cd android
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo =========================================================
    echo [SUCCESS] Android APK built successfully!
    echo Location: android\app\build\outputs\apk\debug\app-debug.apk
    echo =========================================================
) else (
    echo.
    echo [ERROR] Android APK build failed with code %ERRORLEVEL%
)

cd ..
