@echo off
echo =========================================================
echo   Compiling SafeSphere Emergency Orchestration Platform
echo =========================================================

if not exist bin mkdir bin

powershell -NoProfile -ExecutionPolicy Bypass -Command "$src = (Get-ChildItem -Path 'src' -Recurse -Filter '*.java').FullName; javac -encoding UTF-8 -cp 'lib/*' -d bin $src"

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully! Output in bin/
) else (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)
