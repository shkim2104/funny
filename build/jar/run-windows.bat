@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo Java is not installed. Please install Java 11+ from https://adoptium.net and try again.
    pause
    exit /b 1
)
start "" javaw -jar "NotebookRPG.jar"
