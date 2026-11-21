@echo off
setlocal

if not defined EZLITE_REPO set "EZLITE_REPO=%~dp0.."
pushd "%EZLITE_REPO%" 2>nul || (
  echo ERROR: Kan ikke finde RuneLite-mappen: "%EZLITE_REPO%"
  endlocal & exit /b 1
)

where java.exe >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java mangler. Koer scripts\install.bat foerst.
  set "RESULT=1"
  goto done
)

echo Gemmer aktuelle custom plugin-filer i det lokale overlay...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0custom-overlay.ps1" -Mode Capture -RepoRoot "%CD%"
if errorlevel 1 (
  echo ERROR: Custom plugins kunne ikke sikres. Build er stoppet.
  set "RESULT=1"
  goto done
)

set "CLIENT_VERSION="
for /f "tokens=2 delims==" %%V in ('findstr /b /c:"project.build.version=" gradle.properties') do set "CLIENT_VERSION=%%V"
if not defined CLIENT_VERSION (
  echo ERROR: Kunne ikke laese version fra gradle.properties.
  set "RESULT=1"
  goto done
)

echo.
echo Compiler RuneLite %CLIENT_VERSION% med custom plugins...
call gradlew.bat :client:shadowJar -x test -x :client:pmdMain --console=plain
if errorlevel 1 (
  echo.
  echo BUILD FAILED. En eksisterende jar bliver ikke slettet.
  set "RESULT=1"
  goto done
)

set "EXPECTED_JAR=runelite-client\build\libs\client-%CLIENT_VERSION%-shaded.jar"
if not exist "%EXPECTED_JAR%" (
  echo ERROR: Gradle sluttede uden den forventede jar: %EXPECTED_JAR%
  set "RESULT=1"
  goto done
)

echo.
echo BUILD OK: %EXPECTED_JAR%
set "RESULT=0"

:done
popd
if not defined EZLITE_AUTO pause
endlocal & exit /b %RESULT%
