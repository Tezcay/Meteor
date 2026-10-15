[CmdletBinding()]
param(
    # Prepare without launching; useful when a failed start has already shown a popup.
    [switch]$PrepareOnly
)

$ErrorActionPreference = 'Stop'
$dockerCommand = Get-Command docker -ErrorAction Stop
$engineVersion = & $dockerCommand.Source info --format '{{.ServerVersion}}' 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Docker Engine is already available: $engineVersion"
    exit 0
}

# A running backend may still own these endpoints. Never kill it or move live sockets.
$dockerProcesses = @(Get-Process -ErrorAction SilentlyContinue | Where-Object {
    $_.ProcessName -eq 'Docker Desktop' -or $_.ProcessName -like 'com.docker.*'
})
if ($dockerProcesses.Count -gt 0) {
    throw 'Docker Desktop is still running or starting. Quit it normally, then run this script once; no automatic retry was attempted.'
}

$localData = [IO.Path]::GetFullPath($env:LOCALAPPDATA)
$knownEndpoints = @{
    'Docker/run' = @('dockerInference', 'dockerEthernetVfkit', 'userAnalyticsOtlpHttp.sock', 'sailor-ingest.sock')
    'docker-secrets-engine' = @('engine.sock')
}
$stamp = Get-Date -Format 'yyyyMMdd-HHmmssfff'
foreach ($relativeDirectory in $knownEndpoints.Keys) {
    $runtimeDirectory = [IO.Path]::GetFullPath((Join-Path $localData $relativeDirectory))
    $expectedDirectory = [IO.Path]::GetFullPath((Join-Path $env:LOCALAPPDATA $relativeDirectory))
    if ($runtimeDirectory -ne $expectedDirectory -or
        -not $runtimeDirectory.StartsWith($localData + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Runtime directory escaped the expected local application-data directory.'
    }
    if (-not (Test-Path -LiteralPath $runtimeDirectory)) { continue }
    $parent = Get-Item -LiteralPath $runtimeDirectory -Force
    if (-not $parent.PSIsContainer -or ($parent.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw "Unexpected runtime directory type: $runtimeDirectory"
    }
    $entries = @(Get-ChildItem -LiteralPath $runtimeDirectory -Force)
    foreach ($entry in $entries) {
        if ($entry.Name -notin $knownEndpoints[$relativeDirectory] -or $entry.PSIsContainer -or
            $entry.Length -ne 0 -or -not ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Unexpected runtime content; nothing in this directory was moved: $($entry.Name)"
        }
    }
    if ($entries.Count -eq 0) { continue }
    $backupDirectory = "$runtimeDirectory.stale-$stamp"
    if (Test-Path -LiteralPath $backupDirectory) { throw 'Runtime backup already exists.' }
    # Windows error 1920 can block access to the socket itself; retain its parent instead.
    Move-Item -LiteralPath $runtimeDirectory -Destination $backupDirectory -ErrorAction Stop
    Write-Host "Retained stale runtime endpoints: $backupDirectory"
}

if ($PrepareOnly) {
    Write-Host 'Runtime preparation finished. Docker was not launched; no settings, images, containers or volumes were changed.'
    exit 0
}

$launcher = Join-Path $localData 'Programs/DockerDesktop/Docker Desktop.exe'
if (-not (Test-Path -LiteralPath $launcher)) {
    $launcher = Join-Path $env:ProgramFiles 'Docker/Docker/Docker Desktop.exe'
}
if (-not (Test-Path -LiteralPath $launcher)) { throw 'Docker Desktop launcher was not found.' }
Start-Process -FilePath $launcher -WindowStyle Hidden
Write-Host 'Launched Docker Desktop once. After it starts, run docker info. A launch is not proof that the Engine is ready; this script does not retry.'
