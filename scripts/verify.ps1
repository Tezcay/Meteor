[CmdletBinding()]
param(
    [switch]$RequireContainers
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $repositoryRoot 'backend'
$frontendRoot = Join-Path $repositoryRoot 'frontend'
$mavenWrapper = Join-Path $backendRoot 'mvnw.cmd'

function Assert-LastExitCode {
    param(
        [Parameter(Mandatory)]
        [string]$Step
    )

    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed with exit code $LASTEXITCODE."
    }
}

function Write-Step {
    param(
        [Parameter(Mandatory)]
        [string]$Message
    )

    Write-Host ""
    Write-Host "==> $Message"
}

Write-Step 'Backend tests'
Push-Location $backendRoot
try {
    & $mavenWrapper test
    Assert-LastExitCode -Step 'Backend tests'
} finally {
    Pop-Location
}

$reportDirectory = Join-Path $backendRoot 'target/surefire-reports'
$reports = @(Get-ChildItem -LiteralPath $reportDirectory -Filter 'TEST-*.xml' -File)
if ($reports.Count -eq 0) {
    throw "No Surefire XML reports were found in $reportDirectory."
}

$testCount = 0
$failureCount = 0
$errorCount = 0
$skippedCount = 0
foreach ($report in $reports) {
    [xml]$document = Get-Content -Raw -LiteralPath $report.FullName
    $suite = $document.testsuite
    $testCount += [int]$suite.tests
    $failureCount += [int]$suite.failures
    $errorCount += [int]$suite.errors
    $skippedCount += [int]$suite.skipped
}

Write-Host "Backend report: tests=$testCount failures=$failureCount errors=$errorCount skipped=$skippedCount"
if ($failureCount -ne 0 -or $errorCount -ne 0) {
    throw 'Surefire reports contain backend test failures or errors.'
}

if ($RequireContainers) {
    $integrationReportPath = Join-Path $reportDirectory `
        'TEST-dev.meteor.integration.PurchaseFlowMySqlIntegrationTest.xml'
    if (-not (Test-Path -LiteralPath $integrationReportPath)) {
        throw 'The required Testcontainers report is missing.'
    }

    [xml]$integrationDocument = Get-Content -Raw -LiteralPath $integrationReportPath
    $integrationSuite = $integrationDocument.testsuite
    $integrationTests = [int]$integrationSuite.tests
    $integrationSkipped = [int]$integrationSuite.skipped
    if ($integrationTests -ne 5 -or $integrationSkipped -ne 0) {
        throw "Testcontainers gate failed: expected tests=5 and skipped=0, got tests=$integrationTests and skipped=$integrationSkipped."
    }

    Write-Host 'Testcontainers gate: tests=5 skipped=0'
} elseif ($skippedCount -gt 0) {
    Write-Warning 'Some backend tests were skipped. Run with -RequireContainers before claiming Testcontainers verification.'
}

Write-Step 'Frontend dependency check'
Push-Location $frontendRoot
try {
    if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules'))) {
        & npm.cmd ci
        Assert-LastExitCode -Step 'Frontend dependency installation'
    }

    Write-Step 'Frontend formatting'
    & npm.cmd run format:check
    Assert-LastExitCode -Step 'Frontend formatting'

    Write-Step 'Frontend type check and production build'
    & npm.cmd run build
    Assert-LastExitCode -Step 'Frontend build'
} finally {
    Pop-Location
}

Write-Step 'Markdown relative links'
$brokenLinks = @()
$markdownFiles = @(
    Get-ChildItem -LiteralPath $repositoryRoot -Recurse -Filter '*.md' -File |
        Where-Object { $_.FullName -notmatch '\\(node_modules|target|\.git)\\' }
)
foreach ($file in $markdownFiles) {
    $content = Get-Content -Raw -LiteralPath $file.FullName
    foreach ($match in [regex]::Matches($content, '\[[^\]]+\]\(([^)]+)\)')) {
        $link = $match.Groups[1].Value.Trim('<', '>')
        if ($link -match '^(https?://|mailto:|#|codex:)') {
            continue
        }

        $pathPart = ($link -split '#')[0]
        if ([string]::IsNullOrWhiteSpace($pathPart)) {
            continue
        }

        $decodedPath = [uri]::UnescapeDataString($pathPart)
        $target = Join-Path $file.DirectoryName $decodedPath
        if (-not (Test-Path -LiteralPath $target)) {
            $brokenLinks += "$($file.FullName): $link"
        }
    }
}

Write-Host "Markdown report: files=$($markdownFiles.Count) brokenLinks=$($brokenLinks.Count)"
if ($brokenLinks.Count -gt 0) {
    $brokenLinks | ForEach-Object { Write-Host $_ }
    throw 'Markdown relative-link check failed.'
}

Write-Step 'Git whitespace check'
& git -C $repositoryRoot diff --check
Assert-LastExitCode -Step 'Git whitespace check'

& git -C $repositoryRoot diff --cached --check
Assert-LastExitCode -Step 'Staged Git whitespace check'

$textExtensions = @(
    '.css', '.html', '.java', '.js', '.json', '.md', '.properties', '.ps1', '.py',
    '.sh', '.sql', '.ts', '.txt', '.vue', '.xml', '.yaml', '.yml'
)
$untrackedWhitespace = @()
$untrackedFiles = @(& git -c core.quotepath=false -C $repositoryRoot ls-files --others --exclude-standard)
Assert-LastExitCode -Step 'Untracked file listing'
foreach ($relativePath in $untrackedFiles) {
    $extension = [IO.Path]::GetExtension($relativePath).ToLowerInvariant()
    if ($extension -notin $textExtensions) {
        continue
    }

    $absolutePath = Join-Path $repositoryRoot $relativePath
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $absolutePath) {
        $lineNumber++
        if ($line -match '[ \t]+$') {
            $untrackedWhitespace += "${relativePath}:${lineNumber}: trailing whitespace"
        }
    }
}

if ($untrackedWhitespace.Count -gt 0) {
    $untrackedWhitespace | ForEach-Object { Write-Host $_ }
    throw 'Untracked text files contain trailing whitespace.'
}

Write-Host ""
Write-Host 'Meteor verification completed successfully.'
