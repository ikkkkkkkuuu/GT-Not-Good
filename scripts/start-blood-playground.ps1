param([switch]$NewWorld)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions -Djdk.net.unixdomain.tmpdir=C:/gtng-unix-socket-fallback-missing"
    $launchArgs = @('-I', 'scripts/blood-playground.init.gradle', 'runClient', '--console=plain')
    if (!$NewWorld -and (Test-Path -LiteralPath 'run/client/blood-playground-save.txt')) {
        $savedWorld = (Get-Content -LiteralPath 'run/client/blood-playground-save.txt' -Raw).Trim()
        if ($savedWorld -notmatch '^blood-manual-[0-9]+$') { throw 'Invalid playground save name' }
        $launchArgs += "-PbloodPlaygroundResume=$savedWorld"
    }
    & .\gradlew.bat @launchArgs
    if ($LASTEXITCODE -ne 0) { throw 'Blood altar playground failed to launch' }
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    Pop-Location
}
