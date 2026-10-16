# Beyond the Stars block artwork

Source: https://github.com/v3rysp3d/GTNH-Beyond-the-Stars
Author: V3rySp3d, copyright 2026. License: MIT; see `LICENSE-MIT.txt`.

The supplied `GTNH-Beyond-the-Stars-master` directory was inspected on 2026-10-16.
It contains no Git metadata, so no upstream commit is claimed. The asset manifest
pins the local source snapshot with per-file SHA-256 hashes and an aggregate hash.
The root and packaged licenses, README credits, block implementations and texture
generator were inspected before copying.

All 56 ordinary block PNGs and the 56 controller/hatch overlay PNGs with their 24
animation metadata files are copied byte-for-byte from
`assets/beyondthestars/textures/blocks/` to
`assets/gtnotgood/textures/blocks/stellar/`. The 56 overlays form 28 decorative
panels: 12 controller designs in still/animated versions and four hatch designs.
Each uses the original normal/glow layers and GregTech casing artwork. The five
derived native GT panel backgrounds retain LGPL-3.0-or-later and are recorded
separately in `META-INF/stellar-panel-ctm/ASSET_MANIFEST.json`.
Panels have a north-facing front and no tile entities, recipes or machine behavior.

The block classes under `common/blocks/stellar/` adapt the upstream block texture
selection and visual properties to this addon's registration and item containers.
There are 54 ordinary variants across eight block IDs, including two upstream
legacy module appearances, both fuel-column appearances and both turbine directions.
Existing upstream metadata holes remain unused. Names are translated into Chinese.
The Region Crystal emits light level 9; the four Containment Nodes emit level 7.
Glass retains translucent rendering and adjacent-face culling.

The existing host Vault now uses Eddington Wall Casings for its structure,
controller sides and port background. Its front uses the unchanged Kerr Compressor
normal/active overlays and their glow layers. Only Eddington metadata conducts
GregTech machine updates; decorative variants remain plain blocks. The Vault's
structure tooltip names the required casing. The port uses its own casing texture
override without reserving or replacing a global GregTech texture index.

The host adds its own MV assembler recipe for two Eddington Wall Casings: six steel
plates, one steel frame, four copper foils, circuit 24 and 144 mB molten tin,
processed for a base duration of 10 seconds at 120 EU/t. It is not an upstream recipe.

All 56 ordinary face textures now have 47 native 16x16 connected states each:
2,632 derived MIT tiles in addition to the unchanged 136 imports. The original
47 Eddington tiles and paths remain intact. `scripts/generate-eddington-ctm.py`
reproduces those tiles; `scripts/generate-stellar-ctm.py` derives the other 2,585.
Both preserve isolated mask 0 byte-for-byte and use only original source colors.
The latter preserves central artwork, samples safe backgrounds for core designs,
extends directional bands/fins and rail interiors, and removes 1 px glass frames
using the existing alpha-110 glass color. Exposed frames and concave corner caps
remain visible. Per-texture profiles, generator hashes and every source/destination
hash are recorded separately in `ASSET_MANIFEST.json`; the original upstream
snapshot and its 136 asset records remain unchanged.

Built-in `StellarConnections`, `StellarConnectedIcons` and `StellarConnectedTexture`
select states from eight coplanar neighbours using the vanilla/GT copied-block UVs.
This covers 54 ordinary variants, all 28 panel backgrounds and the existing Vault
controller/port background. Connection identity is the face texture key: fueled
and empty pillars share their top face but keep separate sides; panels with the
same base join across different front graphics, and Extraction panels join the
matching local casing. Ordinary native GT casing renderers are not changed and
those native blocks do not participate. Different glass metadata keeps its shared
boundary; hidden faces behind matching glass do not influence exposed connections.
The 56 MIT face textures and five separately licensed native GT bases together use
61 materials and 2,867 tiles. Front graphics, animation metadata, glow and inventory
appearances remain separate. No MyCTMLib, CTM resource pack or additional dependency
is required; there is no new ticking or networking.

No upstream recipes, inventories, fuel or rotor handling, multiblock controllers,
heating-coil capability, glass-tier registration or hidden stellar render carriers
are included. Descriptions identify the blocks as decoration without claiming the
upstream machines' functionality. The reference project is outside this project's
source and resource sets and is never compiled or packaged.
