$ErrorActionPreference = 'Stop'

$projectJdk = 'D:\Environments\JavaJDK\jdk-26_windows-x64_bin\jdk-26'
$mavenCommand = 'D:\Softwares\JetBrains\IntelliJ IDEA\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
$localRepository = 'C:\Users\33661\.m2\repository'

if (-not (Test-Path -LiteralPath $projectJdk)) {
    throw "Project JDK was not found: $projectJdk"
}

if (-not (Test-Path -LiteralPath $mavenCommand)) {
    throw "Maven was not found: $mavenCommand"
}

if (-not (Test-Path -LiteralPath $localRepository)) {
    throw "Maven local repository was not found: $localRepository"
}

$env:JAVA_HOME = $projectJdk
& $mavenCommand "-Dmaven.repo.local=$localRepository" @args
exit $LASTEXITCODE
