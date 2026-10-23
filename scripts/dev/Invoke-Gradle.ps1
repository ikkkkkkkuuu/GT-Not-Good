param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArguments
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$savedOptions = $env:JAVA_TOOL_OPTIONS
try {
    if ([Environment]::OSVersion.Platform -eq 'Win32NT' -and $savedOptions -notmatch 'jdk.net.unixdomain.tmpdir=') {
        $socketDirectory = Join-Path $env:SystemRoot 'Temp'
        $env:JAVA_TOOL_OPTIONS = ($savedOptions + ' -Djdk.net.unixdomain.tmpdir=' + $socketDirectory).Trim()
    }
    Push-Location -LiteralPath $projectRoot
    try {
        & (Join-Path $projectRoot 'gradlew.bat') @GradleArguments
        $gradleExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_TOOL_OPTIONS = $savedOptions
}
exit $gradleExitCode
