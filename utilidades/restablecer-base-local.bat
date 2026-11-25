@echo off
setlocal
cd /d "%~dp0.."
echo ATENCION: esto elimina las bases de datos locales y TODOS los datos de prueba.
echo Las imagenes ya construidas se conservan.
set /p CONFIRM=Escribe BORRAR para continuar:
if /I not "%CONFIRM%"=="BORRAR" (echo Cancelado.& pause & exit /b 0)
docker compose down -v
if errorlevel 1 (echo No se pudo detener Docker Compose.& pause & exit /b 1)
docker compose up -d
if errorlevel 1 (echo Error al iniciar. Ejecuta primero utilidades\iniciar-en-windows.bat para construir las imagenes.& pause & exit /b 1)
echo Bases de datos recreadas. Prueba http://localhost:8080/api/v1/salud
pause
