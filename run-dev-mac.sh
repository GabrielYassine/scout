#!/usr/bin/env bash
set -euo pipefail

# Absolute path to the folder where THIS script lives (your project root)
ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "Project root: $ROOT"

# Start backend in a new Terminal tab/window
echo "Starting backend..."
osascript <<APPLESCRIPT
tell application "Terminal"
  activate
  do script "cd \"$ROOT\" && ./gradlew :backend:bootRun"
end tell
APPLESCRIPT

# Wait for backend on port 8080
echo "Waiting for backend on port 8080..."
while ! (echo > /dev/tcp/127.0.0.1/8080) >/dev/null 2>&1; do
  printf "."
  sleep 1
done
echo " Backend ready."

# Frontend in a new Terminal tab/window
echo "Starting frontend..."
osascript <<APPLESCRIPT
tell application "Terminal"
  activate
  do script "cd \"$ROOT/frontend\" && npm install && npm i recharts && npm run dev"
end tell
APPLESCRIPT

echo "Done. Backend and frontend should be running in Terminal."
