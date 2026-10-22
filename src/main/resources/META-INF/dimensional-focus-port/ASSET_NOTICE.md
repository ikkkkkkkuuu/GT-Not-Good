# Dimensional Focus Engraving Array visual assets

GTLCore, revision `6443c848264fb15c90cf50e031f146522616a8b9`:
<https://github.com/GNSW/GTLCore>. Root license: LGPL-3.0.
Dimensionally transcendent casings, dimension injection casings and improved superconductor coils retain their
original base PNGs, connected atlases and metadata. Both idle and active coil artwork is retained.

GTCEu Modern, revision `e3844ba943e2388c0856617ae4f8b473a0ae1b64`:
<https://github.com/GregTechCEu/GregTech-Modern>. Root license: LGPL-3.0.
Fusion glass and laser-safe engraving casings retain their original base PNGs, connected atlases and metadata.

GregTech Leisure official 1.4.5.3 archive, CurseForge file `9238465`:
<https://www.curseforge.com/minecraft/modpacks/gregtech-leisure/files/9238465>.
Archive SHA-256: `1c732d6b124732c3e3aede97f353910fc31873d9e26074891b2ca69dbcdee23b`.
The archive's root LICENSE is CC BY-NC-SA 4.0. The molecular coil side and top/bottom PNGs retain this license.
The molecular coil face mapping follows `.minecraft/kubejs/startup_scripts/block.js` from the same archive.

Destination artwork: `assets/gtnotgood/textures/blocks/dimensionalfocus/`.
Original base and atlas PNGs are byte-for-byte copies; no colors, dimensions or artwork are changed.
LDLib-specific metadata is preserved under `source-metadata/`. Runtime CTM sprites are composed losslessly
from the original 8x8 quarter tiles. `CTM_MANIFEST.json` records all 282 derived sprites and hashes.
Derived artwork retains its source artwork's LGPL-3.0 license. The offline generator reuses the project's
GPL-3.0-only LDLib adaptation at `reference/field-extruder-port/generate-connected-textures.py`, based on
LDLib-MultiLoader `1d4e4390ae58d16038845c651e122185c29f334b`, Connections.fillSubmaps / Submap.uvs.

The Java renderer uses local Forge/GT5 APIs. Active improved superconductor coils synchronize their state
through metadata and render the original active artwork at full brightness. Modern Shimmer/GTLBloom shader
implementations are not imported. The original active metadata's self-referencing LDLib connection path is
resolved to the supplied `improved_superconductor_coil_active_ctm.png` atlas during offline composition.

The containment field generator and fusion reactor controller overlays reuse the existing assets under
`fieldextruder/`; see `META-INF/field-extruder-port/ASSET_MANIFEST.json` for their original source records.
Individual upstream asset licenses remain applicable and are not replaced by the project's general MIT license.
