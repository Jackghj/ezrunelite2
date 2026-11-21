@echo off
setlocal
title NEMRUNELITE - installation

where powershell.exe >nul 2>&1
if errorlevel 1 (
  echo FEJL: Windows PowerShell blev ikke fundet.
  pause
  endlocal & exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-deps.ps1"
set "RESULT=%ERRORLEVEL%"
echo.
if "%RESULT%"=="0" (
  echo Installation og kontrol er faerdig.
) else (
  echo Installation fejlede. Se beskeden ovenfor.
)
pause
endlocal & exit /b %RESULT%
