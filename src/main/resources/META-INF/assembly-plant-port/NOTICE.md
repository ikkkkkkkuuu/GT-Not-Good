# Mage Assembler and Superconducting Electromagnetic Factory

The external sources and assets listed here retain their recorded licenses and are excluded from
GT Not Good's general MIT grant.

## Structure sources

`MageAssemblerStructure.java` preserves `MultiBlockMachineAStructureA.MAGE_ASSEMBLER` (9×10×33), and
`SuperconductingElectromagneticStructure.java` preserves
`MultiBlockMachineAStructureC.SUPERCONDUCTING_ELECTROMAGNETISM` (15×21×15), from
[GNSW/GTLCore commit 6443c848264fb15c90cf50e031f146522616a8b9](https://github.com/GNSW/GTLCore/tree/6443c848264fb15c90cf50e031f146522616a8b9),
LGPL-3.0-only. Slice grouping, controller substitution and StructureLib coordinate conversion preserve
the original geometry. GTLCore credits the Mage Assembler structure to GTNH.
See `reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md` for the exact source links, counts and conversion.

## Recipe sources

- `MageAssemblerRecipes.java` adapts official GregTech Leisure 1.4.5.3 `gtceu.js`, lines 2170–2194,
  3110–3118 and 11998–12017.
- `SuperconductingElectromagneticRecipes.java` adapts the same script, lines 2248–2269 and 17211–17215.
- The actual official archive, [CurseForge file 9238465](https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465),
  contains CC BY-NC-SA 4.0 as its root `LICENSE`. These adapted recipe files retain CC BY-NC-SA 4.0.
- `LightningProcessorRecipes.java` adapts the ender-pearl recipe in GTLCore `data/recipe/Misc.java`,
  lines 192–198 at the commit above, LGPL-3.0-only.
- `AssemblyPlantCasingRecipes.java` adapts GTCEu Modern `data/recipe/misc/GCyMRecipes.java`,
  lines 209–223 at commit `e3844ba943e2388c0856617ae4f8b473a0ae1b64`, LGPL-3.0-only.
  It maps the electrolytic cell and molybdenum-disilicide coil to native GTNH materials and provides
  conversions for native blocks whose original GTL/GTCEu visuals are preserved by this port.

The port uses installed GTNH materials and standard GT energy and exotic hatches, including native wireless
energy hatches. The controller has no wireless-network mode and does not debit the owner's energy account directly.
Cleanroom-only recipes validate the native cleanroom capability: a valid room with positive cleanness.
No random output loss is introduced by this check.
See `reference/assembly-plant-port/RECIPE_ADAPTATION.md` for all native ingredient mappings and changes.

## Original visual assets

- GNSW/GTLCore contributors: advanced assembly line unit casing PNG and original inactive/active models,
  revision `6443c848264fb15c90cf50e031f146522616a8b9`, LGPL-3.0.
- GregTechCEu/GregTech-Modern contributors: filter casing, laminated glass, tempered glass, PTFE pipe casing,
  molybdenum-disilicide coil and its emissive layer, electrolytic cell and its active animation, the assembly
  line emissive animation, and the large-assembler controller overlays/animations. Original CTM atlases,
  metadata and models are retained as recorded in the asset manifest. Source tag `1.20.1-1.4.4`, revision
  `e3844ba943e2388c0856617ae4f8b473a0ae1b64`, LGPL-3.0.
- GregTech Leisure contributors: spacetime assembly line unit PNG from the official 1.4.5.3 archive,
  CC BY-NC-SA 4.0. This asset remains noncommercial and share alike.

Existing Iridium casing, Lafium casing, accelerated pipeline, improved superconductor coil and fusion-reactor
controller overlays are reused without duplicate artwork. Their original records and licenses remain under
`META-INF/field-extruder-port/`, `META-INF/particle-collider-port/` and `META-INF/dimensional-focus-port/`.
The existing improved-superconductor block gains the native active-offset interface; its original resource
paths, item metadata and visuals are unchanged.

Original resource files remain byte-for-byte unchanged. The five connected layers use 235 16×16 tiles
composed losslessly from the original quarter tiles. Their artwork retains LGPL-3.0.
The offline layout adapter references KilaBash / Low-Drag-MC's LDLib-MultiLoader,
revision `1d4e4390ae58d16038845c651e122185c29f334b`, `Connections.fillSubmaps` / `Submap.uvs`, GPL-3.0.
No modern renderer, shader or Shimmer bloom post-processing is included. Original sprite-frame animation
metadata is retained; the renderer uses native GT textures and emissive rendering.

The new casing texture page is fixed at 123 and checked for conflicts. Active assembly units, coils and
electrolytic cells use GT5 `activeCoils` / `GTCoilTracker` and `IBlockWithActiveOffset=8`.
Inactive server metadata is 0, 6 and 7; synchronized client active appearances are 8, 14 and 15.
The reused improved-superconductor coil keeps inactive metadata 4 and active appearance 12.

The complete applicable terms are packaged in `LICENSE-GTLCore-LGPL-3.0.txt`,
`LICENSE-GTCEu-LGPL-3.0.txt`, `LICENSE-LDLib-GPL-3.0.txt` and
`LICENSE-GregTech-Leisure-CC-BY-NC-SA-4.0.txt`. The GPL text also supplies the terms incorporated by LGPL 3.0.
Exact source/destination hashes, source revisions, modifications and code records are in `ASSET_MANIFEST.json`.
All derived tile hashes and masks are in `CTM_MANIFEST.json`; its paths are relative to
`assets/gtnotgood/textures/blocks/assemblyplant/`.
