$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$manifestPath = Join-Path $projectRoot 'src/main/resources/META-INF/ae2lt-port/ASSET_MANIFEST.json'
$manifest = @(Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json)
if ($manifest.Count -eq 0) { throw 'Empty port manifest' }
$seen = @{}
Add-Type -AssemblyName System.Drawing
foreach ($asset in $manifest) {
    if ($seen.ContainsKey($asset.local)) { throw "Duplicate destination: $($asset.local)" }
    $seen[$asset.local] = $true
    $path = Join-Path $projectRoot $asset.local
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $asset.sha256) { throw "Hash mismatch: $($asset.local)" }
    if ($asset.commit -notmatch '^[a-f0-9]{40}$' -or -not $asset.source -or -not $asset.license) {
        throw "Incomplete provenance: $($asset.local)"
    }
    if ($asset.project -ne 'Applied-Energistics-2') {
        $source = Join-Path $PSScriptRoot "$($asset.project)/$($asset.source)"
        if (Test-Path -LiteralPath $source) {
            if (-not $asset.modified -and (Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant() -ne $hash) {
                throw "Unmodified asset differs from reference: $($asset.local)"
            }
        }
    }
    if ($path.EndsWith('.png')) {
        $bitmap = [System.Drawing.Image]::FromFile($path)
        try { Write-Output "$($asset.local): $($bitmap.Width)x$($bitmap.Height), SHA-256 OK" }
        finally { $bitmap.Dispose() }
    }
}
foreach ($folder in @('textures/blocks/packaged', 'textures/items/packaged', 'textures/gui/packaged')) {
    Get-ChildItem -LiteralPath (Join-Path $projectRoot "src/main/resources/assets/gtnotgood/$folder") -File -Recurse |
        ForEach-Object {
            $relative = $_.FullName.Substring($projectRoot.Length + 1).Replace('\', '/')
            if (-not $seen.ContainsKey($relative)) { throw "Unattributed asset: $relative" }
        }
}
Write-Output "Verified $($manifest.Count) attributed resources. No runtime verification implied."
