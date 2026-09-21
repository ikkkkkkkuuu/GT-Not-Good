$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & .\gradlew.bat -I scripts/blood-qa.init.gradle runClient *> build/blood-qa-before-restart.log
    if ($LASTEXITCODE -ne 0) { throw 'Initial blood altar QA failed; inspect build/blood-qa-before-restart.log' }
    $qaSave = (Get-Content -LiteralPath run/client/blood-qa-resume.txt -Raw).Trim()
    if ($qaSave -notmatch '^blood-qa-[0-9]+$') { throw 'Invalid QA save name' }
    & .\gradlew.bat -I scripts/blood-qa.init.gradle "-PbloodResume=$qaSave" runClient *> build/blood-qa-after-restart.log
    if ($LASTEXITCODE -ne 0) { throw 'Restart blood altar QA failed; inspect build/blood-qa-after-restart.log' }
    Write-Output 'Blood altar QA: both client processes passed.'
} finally {
    Pop-Location
}
