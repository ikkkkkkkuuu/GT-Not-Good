$ErrorActionPreference = 'Stop'
$enc = [Text.UTF8Encoding]::new($false)
$root = (Get-Location).Path
$base = Join-Path $root 'reference/RTSbuilding-official-forge-1.7.10'
$revision = git -C $base rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $revision -ne 'c8bdff25ea9aa692c641fee231671afe58057a39') {
    throw 'The official reference checkout must match the pinned import commit.'
}
$entries = @()
foreach ($part in @('main','uiCore','uiKit')) {
    $src = Join-Path $base "src/$part/java"
    foreach ($file in Get-ChildItem $src -Recurse -File) {
        $relative = $file.FullName.Substring($src.Length + 1).Replace('\','/')
        # JEI APIs are not part of the 1.7.10 NEI runtime; keep these only in the pinned reference.
        if ($relative -match '^com/rtsbuilding/rtsbuilding/compat/jei/' -or
                $relative -eq 'com/rtsbuilding/rtsbuilding/mixin/RecipeRegistryOverlayTransferMixin.java') {
            continue
        }
        $destination = "src/vendor/rtsbuilding/java/$relative"
        if ($relative -match '^com/rtsbuilding/rtsbuilding/mixin/(ForgeHooksRemoteContainerMixin|ChestMenuMixin|GuiScreenOverlayInputMixin|BlockRenderDispatcherMixin|BlockEntityRenderDispatcherMixin)\.java$') {
            $destination = "src/main/java/com/xyp/gtnotgood/mixins/early/rts/$($Matches[1]).java"
        }
        $upstreamHash = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $currentHash = (Get-FileHash -LiteralPath (Join-Path $root $destination) -Algorithm SHA256).Hash.ToLowerInvariant()
        $entries += [ordered]@{ source="src/$part/java/$relative"; destination=$destination; license='LGPL-3.0-only'; upstreamSha256=$upstreamHash; modified=($upstreamHash -ne $currentHash) }
    }
}
foreach ($folder in @('assets','data','META-INF')) {
    $src = Join-Path $base "src/main/resources/$folder"
    foreach ($file in Get-ChildItem $src -Recurse -File) {
        $relative = $file.FullName.Substring($src.Length + 1).Replace('\','/')
        $lic = 'LGPL-3.0-only'
        if ($relative -match '^rtsbuilding/(textures|sounds)/') { $lic = 'LICENSE-ASSETS (All rights reserved)' }
        if ($relative -match 'pinyin/|PinIn-LICENSE') { $lic = 'MIT (PinIn)' }
        $entries += [ordered]@{ source="src/main/resources/$folder/$relative"; destination="src/vendor/rtsbuilding/resources/$folder/$relative"; license=$lic; upstreamSha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant(); modified=$false }
    }
}
$testSource = 'src/test/port1710/java/com/rtsbuilding/rtsbuilding/network/builder/NullableItemStackPayloadTest.java'
$entries += [ordered]@{
    source=$testSource
    destination='src/test/java/com/xyp/gtnotgood/client/rts/OfficialRtsNullableItemStackPayloadTest.java'
    license='LGPL-3.0-only'
    upstreamSha256=(Get-FileHash -LiteralPath (Join-Path $base $testSource) -Algorithm SHA256).Hash.ToLowerInvariant()
    modified=$true
    modifications='Relocated package and class; JUnit 5 assertions converted to the host JUnit 4 runner.'
}
$manifest = [ordered]@{ source='https://github.com/Hcrab/RTSbuilding'; branch='forge-1.7.10'; commit='c8bdff25ea9aa692c641fee231671afe58057a39'; imported='2026-09-24'; entries=$entries }
[IO.File]::WriteAllText((Join-Path $root 'src/main/resources/META-INF/rts-port/OFFICIAL_IMPORT_MANIFEST.json'),($manifest | ConvertTo-Json -Depth 8)+"`n",$enc)
