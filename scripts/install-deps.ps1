$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol =
    [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

function Get-JavaMajor([string] $JavaExe)
{
    if (-not $JavaExe -or -not (Test-Path -LiteralPath $JavaExe))
    {
        return 0
    }
    $commandLine = '"' + $JavaExe + '" -version 2>&1'
    $first = (& $env:ComSpec /d /c $commandLine | Select-Object -First 1) -as [string]
    if ($first -match 'version "(?:1\.)?(\d+)')
    {
        return [int] $Matches[1]
    }
    return 0
}

function Find-Java21
{
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command -and (Get-JavaMajor $command.Source) -ge 21)
    {
        $settingsCommand = '"' + $command.Source + '" -XshowSettings:properties -version 2>&1'
        $settings = @(& $env:ComSpec /d /c $settingsCommand)
        $homeLine = $settings | Where-Object { $_ -match '^\s*java\.home\s*=' } | Select-Object -First 1
        if ($homeLine -and $homeLine -match '=\s*(.+)$')
        {
            $javaHomeCandidate = $Matches[1].Trim()
            if (Test-Path -LiteralPath (Join-Path $javaHomeCandidate 'bin\java.exe'))
            {
                return (Get-Item -LiteralPath $javaHomeCandidate)
            }
        }
    }

    $roots = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
    )
    foreach ($root in $roots)
    {
        $jdk = Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Name -like 'jdk-21*' -and
                (Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe'))
            } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($jdk)
        {
            return $jdk
        }
    }
    return $null
}

function Find-Git
{
    $command = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($command)
    {
        return (Get-Item -LiteralPath $command.Source)
    }

    foreach ($candidate in @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Git\cmd\git.exe'),
        (Join-Path $env:ProgramFiles 'Git\cmd\git.exe')
    ))
    {
        if (Test-Path -LiteralPath $candidate)
        {
            return (Get-Item -LiteralPath $candidate)
        }
    }
    return $null
}

function Add-UserPath([string] $Directory)
{
    $current = [Environment]::GetEnvironmentVariable('Path', 'User')
    if (-not $current) { $current = '' }
    $parts = @($current -split ';' | Where-Object { $_ })
    if (-not ($parts | Where-Object { $_.TrimEnd('\') -ieq $Directory.TrimEnd('\') }))
    {
        [Environment]::SetEnvironmentVariable('Path', ($Directory + ';' + $current), 'User')
    }
    if (-not (($env:Path -split ';') | Where-Object { $_.TrimEnd('\') -ieq $Directory.TrimEnd('\') }))
    {
        $env:Path = $Directory + ';' + $env:Path
    }
}

function Install-Git
{
    Write-Host 'Henter seneste Git for Windows...'
    $release = Invoke-RestMethod -UseBasicParsing -Uri 'https://api.github.com/repos/git-for-windows/git/releases/latest' -TimeoutSec 60
    $asset = $release.assets |
        Where-Object { $_.name -match '64-bit\.exe$' } |
        Select-Object -First 1
    if (-not $asset)
    {
        throw 'Git-installer blev ikke fundet i den seneste release.'
    }

    $installer = Join-Path $env:TEMP 'nemrunelite-git-installer.exe'
    Invoke-WebRequest -UseBasicParsing -Uri $asset.browser_download_url -OutFile $installer -TimeoutSec 900
    $process = Start-Process -FilePath $installer -ArgumentList '/VERYSILENT','/CURRENTUSER','/NORESTART','/NOCANCEL','/SP-','/SUPPRESSMSGBOXES' -WindowStyle Hidden -Wait -PassThru
    if ($process.ExitCode -ne 0)
    {
        throw "Git-installeren sluttede med kode $($process.ExitCode)."
    }
}

function Install-Java21
{
    Write-Host 'Henter Java 21 Temurin som zip (ingen admin noedvendig)...'
    $url = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
    $zipPath = Join-Path $env:TEMP 'nemrunelite-temurin21.zip'
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zipPath -TimeoutSec 1200

    $stream = [IO.File]::OpenRead($zipPath)
    try
    {
        if ($stream.ReadByte() -ne 80 -or $stream.ReadByte() -ne 75)
        {
            throw 'Java-download var ikke en gyldig zip.'
        }
    }
    finally
    {
        $stream.Dispose()
    }

    $stage = Join-Path $env:TEMP 'nemrunelite-temurin21-stage'
    if (Test-Path -LiteralPath $stage)
    {
        Remove-Item -LiteralPath $stage -Recurse -Force
    }
    New-Item -ItemType Directory -Path $stage | Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($zipPath, $stage)

    $source = Get-ChildItem -LiteralPath $stage -Directory |
        Where-Object Name -like 'jdk-21*' |
        Select-Object -First 1
    if (-not $source)
    {
        throw 'Java JDK blev ikke fundet i zip-filen.'
    }

    $destinationRoot = Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'
    New-Item -ItemType Directory -Path $destinationRoot -Force | Out-Null
    $target = Join-Path $destinationRoot $source.Name
    if (Test-Path -LiteralPath $target)
    {
        Remove-Item -LiteralPath $target -Recurse -Force
    }
    Move-Item -LiteralPath $source.FullName -Destination $destinationRoot
}

try
{
    Write-Host '============================================================'
    Write-Host 'NEMRUNELITE - installer/reparer Git og Java 21'
    Write-Host 'Windows 10/11, ingen winget eller choco.'
    Write-Host '============================================================'

    $git = Find-Git
    $java = Find-Java21
    if ($git) { Write-Host "[OK]      Git: $($git.FullName)" } else { Write-Host '[MANGLER] Git for Windows' }
    if ($java) { Write-Host "[OK]      Java 21: $($java.FullName)" } else { Write-Host '[MANGLER] Java JDK 21' }

    if (-not $git -or -not $java)
    {
        [void] (Read-Host 'Tryk ENTER for automatisk download og installation')
    }
    if (-not $git) { Install-Git; $git = Find-Git }
    if (-not $java) { Install-Java21; $java = Find-Java21 }
    if (-not $git) { throw 'Git kunne ikke findes efter installationen.' }
    if (-not $java) { throw 'Java 21 kunne ikke findes efter installationen.' }

    $gitDirectory = Split-Path -Parent $git.FullName
    $javaBin = Join-Path $java.FullName 'bin'
    Add-UserPath $gitDirectory
    Add-UserPath $javaBin
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $java.FullName, 'User')
    $env:JAVA_HOME = $java.FullName

    & $git.FullName --version
    if ($LASTEXITCODE -ne 0) { throw 'Git-validering fejlede.' }
    $javaExe = Join-Path $javaBin 'java.exe'
    & $env:ComSpec /d /c ('"' + $javaExe + '" -version 2>&1')
    if ($LASTEXITCODE -ne 0 -or (Get-JavaMajor $javaExe) -lt 21)
    {
        throw 'Java 21-validering fejlede.'
    }

    Write-Host 'INSTALLATION OK'
    exit 0
}
catch
{
    Write-Error $_.Exception.Message
    exit 1
}
