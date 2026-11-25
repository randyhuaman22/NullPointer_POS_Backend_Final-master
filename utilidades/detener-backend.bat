@echo off
setlocal
cd /d "%~dp0.."
title NullPointer POS - Detener backend
echo Deteniendo los contenedores sin borrar bases de datos ni volumenes...
docker compose stop
if errorlevel 1 (
  echo No se pudo detener la plataforma. Comprueba si Docker Desktop esta encendido.
  pause
  exit /b 1
)
echo.
echo Backend detenido. Los datos persistentes se conservaron.
pause
