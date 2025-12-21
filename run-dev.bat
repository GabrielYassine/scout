@echo off
setlocal

cd /d "%~dp0"

cd frontend
call npm install
start cmd /k "npm run dev"

cd ..
start cmd /k ".\gradlew.bat :backend:bootRun"

endlocal