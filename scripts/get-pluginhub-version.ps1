$ErrorActionPreference = 'Stop'
$cachePath = Join-Path $PSScriptRoot 'pluginhub.version'
$url = 'https://raw.githubusercontent.com/runelite/plugin-hub/refs/heads/master/runelite.version'
$pattern = '^\d+\.\d+\.\d+$'

try
{
    [Net.ServicePointManager]::SecurityProtocol =
        [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 20
    $version = ([string] $response.Content).Trim()
    if ($version -notmatch $pattern)
    {
        throw "Ugyldig Plugin Hub-version: $version"
    }
    [IO.File]::WriteAllText($cachePath, $version, [Text.Encoding]::ASCII)
    Write-Host "Plugin Hub-version opdateret: $version"
    exit 0
}
catch
{
    if (Test-Path -LiteralPath $cachePath)
    {
        $cached = (Get-Content -LiteralPath $cachePath -Raw).Trim()
        if ($cached -match $pattern)
        {
            Write-Host "Offline: bruger gemt Plugin Hub-version $cached"
            exit 0
        }
    }

    Write-Error "Kunne ikke hente eller finde en gemt Plugin Hub-version: $($_.Exception.Message)"
    exit 1
}
