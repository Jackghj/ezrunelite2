[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Capture', 'Restore', 'Verify', 'CheckUpstream')]
    [string] $Mode,

    [string] $RepoRoot
)

$ErrorActionPreference = 'Stop'

if (-not $RepoRoot)
{
    $RepoRoot = Join-Path $PSScriptRoot '..'
}

$RepoRoot = [IO.Path]::GetFullPath($RepoRoot)
$OverlayRoot = [IO.Path]::GetFullPath((Join-Path $RepoRoot 'custom-overlay\files'))
$ManifestPath = Join-Path $PSScriptRoot 'custom-paths.txt'

if (-not (Test-Path -LiteralPath $ManifestPath))
{
    throw "Custom-manifest mangler: $ManifestPath"
}

$Paths = @(
    Get-Content -LiteralPath $ManifestPath |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') }
)

function Get-SafePath([string] $Base, [string] $Relative)
{
    if ([IO.Path]::IsPathRooted($Relative) -or $Relative -match '(^|[\\/])\.\.([\\/]|$)')
    {
        throw "Usikker sti i custom-manifest: $Relative"
    }

    $full = [IO.Path]::GetFullPath((Join-Path $Base ($Relative -replace '/', '\')))
    $prefix = $Base.TrimEnd('\') + '\'
    if (-not $full.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase))
    {
        throw "Stien forlader den tilladte mappe: $Relative"
    }
    return $full
}

function Copy-MirroredPath([string] $SourceBase, [string] $DestinationBase, [string] $Relative)
{
    $source = Get-SafePath $SourceBase $Relative
    $destination = Get-SafePath $DestinationBase $Relative

    if (-not (Test-Path -LiteralPath $source))
    {
        throw "Custom-sti mangler: $Relative"
    }

    $parent = Split-Path -Parent $destination
    New-Item -ItemType Directory -Path $parent -Force | Out-Null

    if (Test-Path -LiteralPath $destination)
    {
        Remove-Item -LiteralPath $destination -Recurse -Force
    }

    Copy-Item -LiteralPath $source -Destination $destination -Recurse -Force
}

function Get-TreeSignature([string] $Base, [string] $Relative)
{
    $root = Get-SafePath $Base $Relative
    if (-not (Test-Path -LiteralPath $root))
    {
        return @('__MISSING__')
    }

    return @(
        Get-ChildItem -LiteralPath $root -Recurse -File |
            Sort-Object FullName |
            ForEach-Object {
                $name = $_.FullName.Substring($root.Length).TrimStart('\')
                $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
                "$name|$hash"
            }
    )
}

switch ($Mode)
{
    'Capture'
    {
        New-Item -ItemType Directory -Path $OverlayRoot -Force | Out-Null
        foreach ($relative in $Paths)
        {
            Copy-MirroredPath $RepoRoot $OverlayRoot $relative
        }
        Write-Host "Custom overlay gemt ($($Paths.Count) stier)."
    }

    'Restore'
    {
        foreach ($relative in $Paths)
        {
            Copy-MirroredPath $OverlayRoot $RepoRoot $relative
        }
        Write-Host "Custom overlay lagt tilbage ($($Paths.Count) stier)."
    }

    'Verify'
    {
        foreach ($relative in $Paths)
        {
            $working = Get-TreeSignature $RepoRoot $relative
            $overlay = Get-TreeSignature $OverlayRoot $relative
            if (Compare-Object $working $overlay)
            {
                throw "Custom overlay matcher ikke working tree: $relative"
            }
        }
        Write-Host "Custom overlay verificeret ($($Paths.Count) stier)."
    }

    'CheckUpstream'
    {
        $collisions = New-Object System.Collections.Generic.List[string]
        foreach ($relative in $Paths)
        {
            $tracked = @(& git -C $RepoRoot ls-tree -r --name-only origin/master -- $relative)
            if ($LASTEXITCODE -ne 0)
            {
                throw "Kunne ikke kontrollere upstream for: $relative"
            }
            if ($tracked.Count -gt 0)
            {
                $collisions.Add($relative)
            }
        }

        if ($collisions.Count -gt 0)
        {
            throw "Officiel upstream bruger nu custom-sti(er): $($collisions -join ', '). Opdateringen er stoppet for at undgaa datatab."
        }
        Write-Host 'Ingen custom-stier kolliderer med officiel upstream.'
    }
}
