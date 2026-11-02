@echo off
setlocal
cd /d "%~dp0.."
docker compose up -d --build
if errorlevel 1 (
  echo No se pudo iniciar el backend. Revisa Docker Desktop y el mensaje anterior.
  pause
  exit /b 1
)
echo Backend iniciado. Gateway: http://localhost:8080
pause
