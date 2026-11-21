@echo off
setlocal EnableDelayedExpansion

if not defined EZLITE_REPO set "EZLITE_REPO=%~dp0.."
pushd "%EZLITE_REPO%" 2>nul || (
  echo ERROR: Kan ikke finde RuneLite-mappen: "%EZLITE_REPO%"
  endlocal & exit /b 1
)

set "EZLITE_AUTO=1"
if not defined KEEP_JARS set "KEEP_JARS=3"

echo ============================================================
echo [1/4] Tjekker officiel RuneLite for opdateringer...
echo ============================================================
call "%~dp0update.bat"
set "UPDATE_RESULT=!ERRORLEVEL!"
if not "!UPDATE_RESULT!"=="0" (
  echo Fortsaetter med en eksisterende build, hvis en findes.
)

set "CLIENT_VERSION="
for /f "tokens=2 delims==" %%V in ('findstr /b /c:"project.build.version=" gradle.properties') do set "CLIENT_VERSION=%%V"
if not defined CLIENT_VERSION (
  echo ERROR: Kunne ikke laese version fra gradle.properties.
  popd & endlocal & exit /b 1
)

set "EXPECTED_JAR=runelite-client\build\libs\client-!CLIENT_VERSION!-shaded.jar"
if not exist "!EXPECTED_JAR!" (
  echo Den aktuelle jar mangler. Proever at bygge den nu...
  call "%~dp0build.bat"
)

echo ============================================================
echo [2/4] Beholder de !KEEP_JARS! nyeste builds...
echo ============================================================
call :retention

set "JAR="
set "USING_FALLBACK="
if exist "!EXPECTED_JAR!" set "JAR=!EXPECTED_JAR!"
if not defined JAR (
  for /f "delims=" %%F in ('dir /b /o-d "runelite-client\build\libs\client-*-shaded.jar" 2^>nul') do (
    if not defined JAR set "JAR=runelite-client\build\libs\%%F"
  )
  if defined JAR set "USING_FALLBACK=1"
)

if not defined JAR (
  echo ERROR: Ingen fungerende klient-jar blev fundet.
  echo Koer scripts\install.bat og derefter scripts\build.bat.
  popd & endlocal & exit /b 1
)

echo ============================================================
echo [3/4] Opdaterer Plugin Hub-version...
echo ============================================================
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0get-pluginhub-version.ps1"

set "PLUGINHUB_VERSION="
if defined USING_FALLBACK if exist "!JAR!.pluginhub-version" set /p "PLUGINHUB_VERSION="<"!JAR!.pluginhub-version"
if not defined PLUGINHUB_VERSION if exist "%~dp0pluginhub.version" set /p "PLUGINHUB_VERSION="<"%~dp0pluginhub.version"

if not defined USING_FALLBACK if defined PLUGINHUB_VERSION (
  >"!JAR!.pluginhub-version" echo !PLUGINHUB_VERSION!
)

echo ============================================================
echo [4/4] Starter RuneLite...
echo ============================================================
echo Client jar       : !JAR!
if defined USING_FALLBACK echo Status           : FALLBACK - nyeste build kunne ikke bruges
if defined PLUGINHUB_VERSION echo Plugin Hub      : !PLUGINHUB_VERSION!
echo Developer mode   : ON
echo Debug logging    : ON

if defined EZLITE_NO_LAUNCH (
  echo Testtilstand: klienten bliver ikke startet.
  popd & endlocal & exit /b 0
)

where java.exe >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java mangler. Koer scripts\install.bat.
  popd & endlocal & exit /b 1
)

if defined PLUGINHUB_VERSION (
  java -ea "-Drunelite.pluginhub.version=!PLUGINHUB_VERSION!" -jar "!JAR!" --debug --developer-mode
) else (
  echo ADVARSEL: Starter uden Plugin Hub, fordi ingen gyldig version kunne findes.
  java -ea -jar "!JAR!" --debug --developer-mode
)

set "RESULT=!ERRORLEVEL!"
popd
endlocal & exit /b %RESULT%

:retention
if not exist "runelite-client\build\libs" goto :eof
pushd "runelite-client\build\libs" 2>nul || goto :eof
set /a COUNT=0
for /f "delims=" %%F in ('dir /b /o-d client-*-shaded.jar 2^>nul') do (
  set /a COUNT+=1
  if !COUNT! LEQ %KEEP_JARS% (
    echo   [BEHOLD] %%F
  ) else (
    echo   [SLET]   %%F
    del /f /q "%%F" 2>nul
    del /f /q "%%F.pluginhub-version" 2>nul
  )
)
popd
goto :eof
