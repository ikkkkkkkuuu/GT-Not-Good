$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & .\gradlew.bat -I scripts/packaged-qa.init.gradle runClient *> build/packaged-qa-before-restart.log
    if ($LASTEXITCODE -ne 0) { throw 'Initial client checks failed; see build/packaged-qa-before-restart.log' }
    $qaSave = (Get-Content -LiteralPath run/client/packaged-qa-resume.txt -Raw).Trim()
    if ($qaSave -notmatch '^packaged-qa-[0-9]+$') { throw 'Invalid QA save name' }
    & .\gradlew.bat -I scripts/packaged-qa.init.gradle "-PpackagedResume=$qaSave" runClient *> build/packaged-qa-after-restart.log
    if ($LASTEXITCODE -ne 0) { throw 'Restart/client checks failed; see build/packaged-qa-after-restart.log' }
    Write-Output 'Packaged Provider QA: both client processes passed.'
} finally {
    Pop-Location
}
