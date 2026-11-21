# NEMRUNELITE

En ren, shallow kopi af officiel RuneLite med lokale custom plugins ovenpaa.
De lokale plugins bliver aldrig committed eller pushed.

## Daglig brug

1. Pak hele mappen ud.
2. Dobbeltklik `NEMRUNELITE.cmd`.
3. Vaelg **5. Install** paa en ny Windows 10/11-computer.
4. Vaelg derefter **4. Run**.

`run.bat` goer automatisk foelgende:

- henter opdateringer fra officielle `runelite/runelite`;
- compiler kun ved en ny officiel version eller hvis den aktuelle jar mangler;
- bevarer op til tre shaded jars som fallback;
- henter og validerer nyeste officielle Plugin Hub-version;
- bruger en gemt Plugin Hub-version, hvis computeren er offline;
- starter med assertions, debug logging og developer mode.

## Sikker opdatering

Custom kildekode findes to steder:

- de normale RuneLite-kildemapper, hvor Gradle compiler dem;
- `custom-overlay\files`, som er en lokal sikkerhedskopi.

Stierne staar i `scripts\custom-paths.txt`. Foer en opdatering gemmes aktuelle
pluginfiler i overlayet. Opdateringen accepteres kun som en ren fast-forward fra
officiel `origin/master`. Hvis officiel RuneLite en dag opretter en af de samme
pluginstier, stopper scriptet uden at overskrive noget.

Der bruges ikke `git commit`, `git push`, `git merge -X theirs`, `git clean` eller
automatisk sletning af custom plugins.

## Scripts

- `ezlite.cmd`: menuen.
- `run.bat`: anbefalet daglig start; update ved behov og start klienten.
- `update.bat`: sikker officiel fast-forward og compile kun ved opdatering.
- `build.bat`: manuel compile.
- `cleanup.bat`: fjerner cache/build-skrald, men beholder shaded jars.
- `install.bat`: installerer/reparerer Git og Java 21 uden winget/choco.

## Manuel build

Koer `scripts\build.bat`. Gradle-wrapperen henter selv Gradle og dependencies.
Der bruges Java 21.

## Plugin Hub og startflag

Plugin Hub-versionen hentes fra:

`https://raw.githubusercontent.com/runelite/plugin-hub/refs/heads/master/runelite.version`

Klienten startes svarende til:

```text
java -ea -Drunelite.pluginhub.version=<version> -jar <client.jar> --debug --developer-mode
```
