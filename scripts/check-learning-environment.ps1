param(
    [switch]$Verify
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$failures = [System.Collections.Generic.List[string]]::new()
$warnings = [System.Collections.Generic.List[string]]::new()

function Write-CheckResult {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )

    $mark = if ($Passed) { '[OK]' } else { '[--]' }
    Write-Host "$mark $Name - $Detail"
}

function Get-CommandOutput {
    param(
        [string]$Command,
        [string[]]$Arguments
    )

    return (& $Command @Arguments 2>&1 | Out-String).Trim()
}

Write-Host 'Meteor learning environment check'
Write-Host "Project: $projectRoot"
Write-Host ''

$java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $java) {
    $failures.Add('Java is missing. Install Java 21 and reopen the terminal.')
    Write-CheckResult 'Java 21' $false 'java command not found'
} else {
    $javaVersion = Get-CommandOutput $java.Source @('-version')
    $javaFirstLine = ($javaVersion -split "`r?`n")[0]
    $isJava21 = $javaFirstLine -match '(version\s+"21\.|openjdk\s+21\.)'
    Write-CheckResult 'Java 21' $isJava21 $javaFirstLine
    if (-not $isJava21) {
        $failures.Add("Java 21 is required, but the active command reports: $javaFirstLine")
    }
}

$node = Get-Command node -ErrorAction SilentlyContinue
if ($null -eq $node) {
    $failures.Add('Node.js is missing. Install Node.js and reopen the terminal.')
    Write-CheckResult 'Node.js' $false 'node command not found'
} else {
    $nodeVersion = Get-CommandOutput $node.Source @('--version')
    Write-CheckResult 'Node.js' $true $nodeVersion
}

$npm = Get-Command npm -ErrorAction SilentlyContinue
if ($null -eq $npm) {
    $failures.Add('npm is missing. Reinstall Node.js with npm enabled.')
    Write-CheckResult 'npm' $false 'npm command not found'
} else {
    $npmVersion = Get-CommandOutput $npm.Source @('--version')
    Write-CheckResult 'npm' $true $npmVersion
}

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if ($null -eq $mysql) {
    $warnings.Add('mysql command is not on PATH. You can still initialize the database with MySQL Workbench.')
    Write-CheckResult 'MySQL client' $false 'mysql command not found; Workbench is also acceptable'
} else {
    $mysqlVersion = Get-CommandOutput $mysql.Source @('--version')
    Write-CheckResult 'MySQL client' $true $mysqlVersion
}

$wrapperPath = Join-Path $projectRoot 'backend\mvnw.cmd'
$bootstrapPath = Join-Path $projectRoot 'scripts\mysql\bootstrap-local.sql'
$nodeModulesPath = Join-Path $projectRoot 'frontend\node_modules'

$hasWrapper = Test-Path -LiteralPath $wrapperPath -PathType Leaf
Write-CheckResult 'Maven Wrapper' $hasWrapper $wrapperPath
if (-not $hasWrapper) {
    $failures.Add('backend\mvnw.cmd is missing from the checkout.')
}

$hasBootstrap = Test-Path -LiteralPath $bootstrapPath -PathType Leaf
Write-CheckResult 'Database bootstrap script' $hasBootstrap $bootstrapPath
if (-not $hasBootstrap) {
    $failures.Add('scripts\mysql\bootstrap-local.sql is missing from the checkout.')
}

$hasNodeModules = Test-Path -LiteralPath $nodeModulesPath -PathType Container
Write-CheckResult 'Frontend dependencies' $hasNodeModules $(
    if ($hasNodeModules) { 'frontend\node_modules exists' } else { 'run npm install in frontend\ before starting' }
)
if (-not $hasNodeModules) {
    $warnings.Add('Frontend dependencies are not installed yet. Run npm install in frontend\.')
}

$networkProperties = [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties()
$listeningPorts = $networkProperties.GetActiveTcpListeners() | ForEach-Object { $_.Port }

foreach ($port in @(3306, 8080, 5173)) {
    $isListening = $listeningPorts -contains $port
    $passed = switch ($port) {
        3306 { $isListening }
        default { -not $isListening }
    }
    $meaning = switch ($port) {
        3306 {
            if ($isListening) {
                'MySQL appears to be running'
            } else {
                $warnings.Add('Port 3306 is not listening. Start MySQL before the local demo.')
                'start MySQL before the local demo'
            }
        }
        8080 {
            if ($isListening) {
                $warnings.Add('Port 8080 is already in use. Stop the old process or confirm it is the Meteor backend.')
                'backend port is already in use'
            } else {
                'available for the backend'
            }
        }
        5173 {
            if ($isListening) {
                $warnings.Add('Port 5173 is already in use. Stop the old process or confirm it is the Meteor frontend.')
                'frontend port is already in use'
            } else {
                'available for Vite'
            }
        }
    }
    Write-CheckResult "Port $port" $passed $meaning
}

if ($Verify -and $failures.Count -eq 0) {
    Write-Host ''
    Write-Host 'Running the full repository verification...'
    & (Join-Path $projectRoot 'scripts\verify.ps1')
    if ($LASTEXITCODE -ne 0) {
        $failures.Add("scripts\verify.ps1 failed with exit code $LASTEXITCODE.")
    }
}

Write-Host ''
if ($warnings.Count -gt 0) {
    Write-Host 'Warnings:'
    foreach ($warning in $warnings) {
        Write-Host "- $warning"
    }
    Write-Host ''
}

if ($failures.Count -gt 0) {
    Write-Host 'Blocking problems:'
    foreach ($failure in $failures) {
        Write-Host "- $failure"
    }
    exit 1
}

Write-Host 'Environment check completed. No blocking problem was found.'
exit 0
