@echo off
setlocal

cd /d "%~dp0"
start "backend" cmd /k ".\gradlew.bat :backend:bootRun"

echo Waiting for backend on port 8080...
powershell -Command "while(-not (Test-NetConnection -ComputerName 'localhost' -Port 8080).TcpTestSucceeded){Write-Host '.' -NoNewline; Start-Sleep -Seconds 1}; Write-Host ' Backend ready.'"

cd frontend
call npm install
call npm i recharts
start "frontend" cmd /k "npm run dev"

endlocal
