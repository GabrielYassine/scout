#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "Project root: $ROOT"

echo "Starting backend..."
osascript <<APPLESCRIPT
tell application "Terminal"
  activate
  do script "cd \"$ROOT\" && ./gradlew :backend:bootRun"
end tell
APPLESCRIPT

echo "Waiting for backend on port 8080..."
while ! (echo > /dev/tcp/127.0.0.1/8080) >/dev/null 2>&1; do
  printf "."
  sleep 1
done
echo " Backend ready."

echo "Starting frontend..."
osascript <<APPLESCRIPT
tell application "Terminal"
  activate
  do script "cd \"$ROOT/frontend\" && npm install && npm install echarts echarts-for-react && npm run dev"
end tell
APPLESCRIPT

echo "Done. Backend and frontend should be running in Terminal."