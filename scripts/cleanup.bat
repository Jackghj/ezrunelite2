@echo off
setlocal

if not defined EZLITE_REPO set "EZLITE_REPO=%~dp0.."
pushd "%EZLITE_REPO%" 2>nul || (
  echo ERROR: Kan ikke finde RuneLite-mappen: "%EZLITE_REPO%"
  endlocal & exit /b 1
)

echo Rydder genererede filer, men beholder kildekode, custom overlay og shaded jars...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $root=[IO.Path]::GetFullPath((Get-Location).Path);" ^
  "$dirs=@('.gradle','.idea','.lwjgl-rl','out','build','cache/.gradle','cache/build','cache/target','runelite-api/.gradle','runelite-api/build','runelite-api/target','runelite-client/target','runelite-jshell/build','runelite-jshell/target','runelite-gradle-plugin/.gradle','runelite-gradle-plugin/build');" ^
  "foreach($rel in $dirs){$p=[IO.Path]::GetFullPath((Join-Path $root $rel)); if($p.StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $p)){Remove-Item -LiteralPath $p -Recurse -Force; Write-Host ('  Slettet: '+$rel)}};" ^
  "$clientBuild=Join-Path $root 'runelite-client/build'; if(Test-Path -LiteralPath $clientBuild){Get-ChildItem -LiteralPath $clientBuild -Force | Where-Object Name -ne 'libs' | ForEach-Object {Remove-Item -LiteralPath $_.FullName -Recurse -Force; Write-Host ('  Slettet: runelite-client/build/'+$_.Name)}}"

set "RESULT=%ERRORLEVEL%"
if "%RESULT%"=="0" echo Cleanup OK. Dine plugins og backup-jars er bevaret.
popd
if not defined EZLITE_AUTO pause
endlocal & exit /b %RESULT%
