@echo off
setlocal

if not defined EZLITE_REPO set "EZLITE_REPO=%~dp0.."
pushd "%EZLITE_REPO%" 2>nul || (
  echo ERROR: Kan ikke finde RuneLite-mappen: "%EZLITE_REPO%"
  endlocal & exit /b 1
)

where git.exe >nul 2>&1
if errorlevel 1 (
  echo ERROR: Git mangler. Koer scripts\install.bat foerst.
  set "RESULT=1"
  goto done
)

echo [1/5] Sikrer dine utrackede custom plugins...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0custom-overlay.ps1" -Mode Capture -RepoRoot "%CD%"
if errorlevel 1 (
  echo ERROR: Custom plugins kunne ikke sikres. Intet er opdateret.
  set "RESULT=1"
  goto done
)

echo [2/5] Henter nyeste officielle RuneLite...
git fetch --prune origin master
if errorlevel 1 (
  echo ADVARSEL: Officiel RuneLite kunne ikke kontaktes. Beholder eksisterende version.
  set "RESULT=2"
  goto done
)

set "BEHIND=0"
for /f "delims=" %%C in ('git rev-list --count HEAD..origin/master 2^>nul') do set "BEHIND=%%C"
if "%BEHIND%"=="0" (
  echo Du koerer allerede nyeste officielle RuneLite. Ingen compile noedvendig.
  set "RESULT=0"
  goto done
)

echo Fundet %BEHIND% nye officielle commits.
echo [3/5] Kontrollerer at opdateringen er sikker...
git merge-base --is-ancestor HEAD origin/master
if errorlevel 1 (
  echo ERROR: Den officielle branch kan ikke fast-forwardes. Ingen filer er aendret.
  set "RESULT=1"
  goto done
)

git diff --quiet --ignore-submodules --
if errorlevel 1 (
  echo ERROR: Officielle tracked filer har lokale aendringer. Ingen filer er overskrevet.
  set "RESULT=1"
  goto done
)
git diff --cached --quiet --ignore-submodules --
if errorlevel 1 (
  echo ERROR: Der ligger staged tracked aendringer. Ingen filer er overskrevet.
  set "RESULT=1"
  goto done
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0custom-overlay.ps1" -Mode CheckUpstream -RepoRoot "%CD%"
if errorlevel 1 (
  echo ERROR: Upstream kolliderer med et custom plugin. Beholder eksisterende version.
  set "RESULT=1"
  goto done
)

echo [4/5] Fast-forwarder til officiel origin/master...
git merge --ff-only origin/master
if errorlevel 1 (
  echo ERROR: Fast-forward fejlede. Custom plugins er stadig gemt i custom-overlay.
  set "RESULT=1"
  goto done
)

echo [5/5] Laegger custom plugins tilbage og compiler...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0custom-overlay.ps1" -Mode Restore -RepoRoot "%CD%"
if errorlevel 1 (
  echo ERROR: Custom plugins kunne ikke laegges tilbage. Stopper foer build.
  set "RESULT=1"
  goto done
)

call "%~dp0build.bat"
set "RESULT=%ERRORLEVEL%"

:done
popd
if not defined EZLITE_AUTO pause
endlocal & exit /b %RESULT%
