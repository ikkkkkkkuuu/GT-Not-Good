$ErrorActionPreference = 'Stop'
$projects = @(
    @('AE2-Lightning-Tech', 'https://github.com/bfzds/AE2-Lightning-Tech.git', '1d4589b6bd50672051f78d766505530beacfebc0'),
    @('AE2LT-Packaged-Pattern-Provider', 'https://github.com/bfzds/AE2LT-Packaged-Pattern-Provider.git', '1d3f183ecf258aff0001567d742ed30d6038d77b')
)
foreach ($project in $projects) {
    $destination = Join-Path $PSScriptRoot $project[0]
    if (Test-Path -LiteralPath $destination) {
        $revision = git -C $destination rev-parse HEAD
        if ($LASTEXITCODE -ne 0 -or $revision -ne $project[2]) {
            throw "Existing checkout does not match pinned revision: $destination. No files changed."
        }
        $changes = git -C $destination status --porcelain
        if ($LASTEXITCODE -ne 0 -or $changes) {
            throw "Existing checkout is not clean: $destination. No files changed."
        }
        continue
    }
    git -c credential.interactive=never clone $project[1] $destination
    if ($LASTEXITCODE -ne 0) { throw "Clone failed: $($project[0])" }
    git -C $destination checkout --detach $project[2]
    if ($LASTEXITCODE -ne 0) { throw "Pinned checkout failed: $($project[0])" }
}
