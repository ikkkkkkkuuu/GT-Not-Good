# Heart of the Universe: structure and block art

Structure: Dragonators/GTLAdditions, commit 34fb152886164c56a07fc9d1b10e056f0c22b7ab,
scripts/structure_sources/multiblock/annihilate_generator.json, GPL-3.0.
The original factory pattern is encoded as GZIP MBS for Minecraft 1.7.10.
All 121 x 112 x 109 coordinates, 37,144 non-space positions and arbitrary spaces are preserved.

GTLCore block art: GNSW/GTLCore, commit 6443c848264fb15c90cf50e031f146522616a8b9,
LGPL-3.0. The temporal anchor block retains its original sps_casing texture reference.
Connected textures use lossless assembly of the original LDLib quarter tiles.
The offline compositor adapts LDLib-MultiLoader 1d4e4390ae58d16038845c651e122185c29f334b,
Connections.fillSubmaps / Submap.uvs, GPL-3.0.

The controller uses GTCEu Modern e3844ba943e2388c0856617ae4f8b473a0ae1b64
high_power_casing art (LGPL-3.0), GTLCore cosmos_simulation front/active/emissive art
(LGPL-3.0), and GTLAdditions cosmos_simulation top/active art (GPL-3.0).
Their original 16-tick animation metadata is retained.

KubeJS block art: official GregTech Leisure 1.4.5.3, CurseForge file 9238465,
https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465,
CC BY-NC-SA 4.0, copyright GregTech Leisure contributors.
The archive's root license governs annihilate_core, dimensional_stability_casing,
dyson_deployment_magnet and its side texture. Images and the stability casing animation are unchanged.

The reused hyper mechanical casing, fusion glass and dimension injection casing retain their
field-extruder-port / dimensional-focus-port asset provenance and licenses.
See STRUCTURE_ASSET_MANIFEST.json for source paths, destinations, modifications and hashes.
These upstream works are excluded from this project's general MIT grant.
Unmodified source snapshots are retained under reference/heart-of-the-universe-port/upstream
and are excluded from compilation and resource packaging.
