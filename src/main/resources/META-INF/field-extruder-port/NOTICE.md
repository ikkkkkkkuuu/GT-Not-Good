# Field Extruder Factory port

## Structure and processing

The structure is adapted from GNSW/GTLCore revision `6443c848264fb15c90cf50e031f146522616a8b9`,
`src/main/java/org/gtlcore/gtlcore/common/data/machines/structure/MultiBlockMachineAStructureC.java`,
`FIELD_EXTRUDER_FACTORY`. Its machine registration credits the structure to Twist Space Technology (TST).
The inspected GTLCore snapshot is LGPL-3.0; `FieldExtruderStructure.java` retains this license.

Destination: `com/xyp/gtnotgood/common/machines/multiblock/structure/FieldExtruderStructure.java`.
Repeated slices are stored once, expanded into horizontal layers and reversed front-to-back for StructureLib.
The complete 15 x 9 x 63 layout and unconstrained spaces are preserved. Reversing the transformation reproduces
the upstream row data, SHA-256 `d073706a31514a4d12b38985b3bf2d29aebed30a3610b5a263ff2c9e938efbbd`.

The extrusion and compression recipe maps are referenced from that revision's `MultiBlockMachineA.java`.
At the user's request, `FieldExtruderFactory` uses the shared `GTNGModularMachineBase` and
`ModularProcessingLogic`, as do the other modular machine ports. Each execution core runs an independent
task for one recipe, with parallel capacity controlled by the machine's limit and installed modules.
Input validation, energy checks, output protection and task persistence follow that shared processing path.
Non-consumed inputs follow the native recipe definitions. The dedicated mixed-recipe batch scheduler,
64-times batch multiplier, total-energy duration conversion and 20-tick minimum have been removed.
Texture page 126 remains reserved because GregTech hatches persist and synchronize numeric casing texture IDs.

## Survival recipes

`FieldExtruderRecipes.java` adapts data from the official GregTech Leisure 1.4.5.3 archive's
`.minecraft/kubejs/server_scripts/gtceu.js`, under its included CC BY-NC-SA 4.0 license.
Controller: UEV assembly line, four UHV extruders, four UHV compressors, UEV components and four UIV circuits.
Iridium casing remains LuV; molecular, hollow and containment components remain UV. The original low-EU
hyper mechanical casing recipe retains its naquadah alloy frame and naquadria plate prerequisites.

Material/API substitutions for GTNH:

- Station research becomes native assembly-line `RESEARCH_ITEM` and `SCANNING` (1,200 ticks at the source tier).
- Controller: rhenium nanites become neutronium nanites, stress-proof casings become advanced iridium casings,
  dalisenite plates become neutronium plates, mutated living solder becomes Indalloy 140,
  and uranium-rhodium-dinaquadide becomes UV superconductor base.
- Iridium casing: tanmolyium plate becomes Tantalloy 61 plate.
- Molecular casing: darmstadtium rings become neutronium rings. The native six-slot assembler combines
  48 original alloy foils into 48 ruridit foils and 14 noble-metal plate equivalents into 14 rhodium plates;
  UV power, cleanroom requirement and 400-tick duration are retained.
- Hollow casing: apatite rods become naquadah alloy rods; modern UV superconductor fluid becomes GTNH UV base.
- Containment generator: modern superconductor wire/plates become UV base, the 16A transformer becomes
  `WetTransformer_UV_ZPM`, and molybdenum disilicide fluid becomes molten molybdenum.
- Cleanroom glass has a local EV assembler recipe using borosilicate glass, clean stainless steel casing
  and silicone rubber plates. It is a structure block and does not create a working cleanroom.

## Visual assets

These assets retain their upstream licenses and are excluded from GT Not Good's general MIT grant.

- GNSW/GTLCore contributors: iridium, hyper mechanical and molecular casing textures, including the two
  original connected-texture atlases. Source revision `6443c848264fb15c90cf50e031f146522616a8b9`, LGPL-3.0.
  See `LICENSE-GTLCore-LGPL-3.0.txt` and the incorporated GPL terms in `LICENSE-LDLib-GPL-3.0.txt`.
- GregTechCEu/GregTech-Modern contributors: cleanroom glass, its connected-texture atlas, and fusion reactor
  controller overlays. Tag `1.20.1-1.4.4`, revision `e3844ba943e2388c0856617ae4f8b473a0ae1b64`, LGPL-3.0.
  See `LICENSE-GTCEu-LGPL-3.0.txt` and the incorporated GPL terms in `LICENSE-LDLib-GPL-3.0.txt`.
- GregTech Leisure contributors: hollow casing and containment field generator textures, including the original
  generator animation metadata. Official `GregTech-Leisure-1.4.5.3.zip`, CurseForge file `9238465`,
  <https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465>.
  The actual archive's root license is CC BY-NC-SA 4.0; this takes precedence over its download-page label.
  See `LICENSE-GregTech-Leisure-CC-BY-NC-SA-4.0.txt`. These assets are noncommercial and share alike.

Original PNGs and animation metadata are unchanged. LDLib connection metadata is retained under `source-metadata/`;
the 1.7.10 renderer instead uses 47 PNGs per connected material, composed by losslessly selecting the original
quarter tiles. No artwork is redrawn, no colors are replaced, and the isolated case preserves the base image pixels.
Derived connected textures retain the LGPL-3.0 license of their source artwork.

The offline layout generator adapts `Connections.fillSubmaps` and `Submap.uvs` from KilaBash / Low-Drag-MC's
LDLib-MultiLoader revision `1d4e4390ae58d16038845c651e122185c29f334b` under GPL-3.0. Its source is distributed
at `reference/field-extruder-port/generate-connected-textures.py`; no LDLib rendering code is included in this module.
No modern model loader, bloom implementation, shader or entity animation is imported.

Exact source paths, revisions, archive hash and byte hashes are listed in `ASSET_MANIFEST.json`.
`CTM_MANIFEST.json` records every derived texture and connection mask.
