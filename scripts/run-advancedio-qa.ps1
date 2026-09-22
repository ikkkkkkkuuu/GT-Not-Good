$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions -Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing"
    & .\gradlew.bat -I scripts/advancedio-qa.init.gradle runClient --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Advanced IO first-run checks failed' }
    $qaSave = (Get-Content -LiteralPath 'run/client/advancedio-qa-resume.txt' -Raw).Trim()
    if ($qaSave -notmatch '^advancedio-qa-[0-9]+$') { throw 'Invalid Advanced IO QA save name' }
    & .\gradlew.bat -I scripts/advancedio-qa.init.gradle runClient "-PadvancedIOResume=$qaSave" --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Advanced IO restart checks failed' }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    Pop-Location
}
