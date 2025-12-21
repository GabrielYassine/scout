@echo off
setlocal

REM Always run from repo root
cd /d "%~dp0"

echo Starting backend...
start "Scout Backend" cmd /k ".\gradlew.bat :backend:bootRun"

echo Starting frontend...
start "Scout Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo Two terminals opened. Close them to stop.
endlocal