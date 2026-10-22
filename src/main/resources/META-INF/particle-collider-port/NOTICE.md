# Super Particle Collider

The licensed sources and assets listed here are excluded from GT Not Good's general MIT grant.

## Structure source

`src/main/java/com/xyp/gtnotgood/common/machines/multiblock/structure/SuperParticleColliderStructure.java` derives from
GNSW/GTLCore's `MultiBlockMachineAStructureA.java`, `SUPER_PARTICLE_COLLIDER`, commit
`6443c848264fb15c90cf50e031f146522616a8b9`, under LGPL-3.0-only. Its 47×7×47 geometry is preserved through
lossless slice grouping, reversed depth order, controller `a` → `~` and StructureLib horizontal-layer conversion.
The pinned `MultiBlockMachineA.java` maps `d` twice; the second mapping replaces the first, so the port uses
560 accelerated pipelines and no aggregation cores. GTLCore credits the source structure to TST contributors.
See `reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md` for source links and the canonical geometry hash.

## Recipe source

`src/main/java/com/xyp/gtnotgood/common/recipe/machine/SuperParticleColliderRecipes.java` adapts
`.minecraft/kubejs/server_scripts/gtceu.js` from official `GregTech-Leisure-1.4.5.3.zip`, CurseForge file `9238465`:
<https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465>.
The actual archive's root `LICENSE` is CC BY-NC-SA 4.0. The adapted recipe source retains this license.
Source sections are lines 2700–2709, 10408–10421 and 8016–8039. Native GTNH material and fluid mappings preserve
the controller progression and five complete elemental collision recipes; unsupported elemental endpoints and
the antimatter branch are omitted. An existing GTNH superconducting coil converts 1:1 into the visual port.
See `reference/particle-collider-port/RECIPE_ADAPTATION.md` for all mappings and omissions.

## Visual assets

- GNSW/GTLCore contributors: Lafium mechanical casing PNG, LDLib CTM atlas and connection metadata,
  revision `6443c848264fb15c90cf50e031f146522616a8b9`, LGPL-3.0.
- GregTechCEu/GregTech-Modern contributors: superconducting coil, its active emissive layer and original CTM
  atlases/metadata/models, tag `1.20.1-1.4.4`, revision `e3844ba943e2388c0856617ae4f8b473a0ae1b64`, LGPL-3.0.
  The emissive layer's actual connection metadata points to
  `casings/coils/machine_coil_superconductor_bloom_ctm`; that exact atlas is retained.
- GregTech Leisure contributors: accelerated pipeline PNG from the official 1.4.5.3 archive,
  CC BY-NC-SA 4.0. These assets are noncommercial and share alike.
- Existing Field Extruder Factory fusion-reactor controller overlays are referenced without additional copies.
  They originate from the same pinned GTCEu source and retain LGPL-3.0; their source records remain in
  `META-INF/field-extruder-port/ASSET_MANIFEST.json`.

Original files remain byte-for-byte unchanged. The three connected materials use 141 16×16 textures composed
losslessly from the original quarter tiles. Their derived PNGs retain the LGPL-3.0 license of their source artwork.
The offline layout generator reuses the adapter for KilaBash / Low-Drag-MC's LDLib-MultiLoader,
revision `1d4e4390ae58d16038845c651e122185c29f334b`, `Connections.fillSubmaps` / `Submap.uvs`, GPL-3.0.
No modern renderer or shader is included. The active coil uses native GT5 `activeCoils` / `GTCoilTracker`
synchronization and GT emissive rendering, with offset 8 (server metadata 2, client active appearance 10).
The casing texture page is fixed at 124 and checked for conflicts on both sides. No custom world scan,
server-side active-metadata toggling or Shimmer bloom post-processing is added.

See `LICENSE-GTLCore-LGPL-3.0.txt`, `LICENSE-GTCEu-LGPL-3.0.txt`, `LICENSE-LDLib-GPL-3.0.txt` and
`LICENSE-GregTech-Leisure-CC-BY-NC-SA-4.0.txt`. The GPL text also supplies the terms incorporated by LGPL 3.0.
Exact original-file hashes, sources, revisions, adaptations and references are in `ASSET_MANIFEST.json`;
all derived tile hashes and masks are in `CTM_MANIFEST.json`.
Paths in that tile manifest are relative to `assets/gtnotgood/textures/blocks/particlecollider/`.
