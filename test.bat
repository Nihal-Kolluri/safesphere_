@echo off
echo =========================================================
echo   Executing SafeSphere Automated Verification Suite
echo =========================================================

if not exist bin (
    echo Building project first...
    call build.bat
)

java -cp "bin;lib/*" com.safesphere.SafeSphereTestSuite
