@echo off
setlocal
set "HERE=%~dp0"

:menu
cls
echo ==============================================
echo   NEMRUNELITE
echo ==============================================
echo   1. Cleanup - slet cache, behold plugins/jars
echo   2. Update  - officiel update + compile ved behov
echo   3. Build   - compile manuelt
echo   4. Run     - auto-update og start klienten
echo   5. Install - installer/reparer Git og Java 21
echo   6. Exit
echo ==============================================
set /p "CHOICE=Vaelg (1-6): "

if "%CHOICE%"=="1" call "%HERE%cleanup.bat" & goto menu
if "%CHOICE%"=="2" call "%HERE%update.bat" & goto menu
if "%CHOICE%"=="3" call "%HERE%build.bat" & goto menu
if "%CHOICE%"=="4" call "%HERE%run.bat" & goto menu
if "%CHOICE%"=="5" call "%HERE%install.bat" & goto menu
if "%CHOICE%"=="6" goto :eof
goto menu
