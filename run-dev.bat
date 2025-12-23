@echo off
setlocal

cd /d "%~dp0"

start cmd /k ".\gradlew.bat :backend:bootRun"

cd frontend
call npm install
start cmd /k "npm run dev"

endlocal
