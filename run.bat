@echo off
echo =========================================================
echo   Launching SafeSphere Live Presentation Demo Suite
echo =========================================================

if not exist bin (
    echo Building project first...
    call build.bat
)

java -cp "bin;lib/*" com.safesphere.Main %*
