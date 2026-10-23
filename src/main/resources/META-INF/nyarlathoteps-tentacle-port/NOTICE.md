# Nyarlathotep's Tentacle port

Original structure, textures and adapted acquisition recipe layouts:
GregTech Odyssey contributors, GTOCore-Main commit
156835a0e66a90cbc4e60c3e95a36c35212b97d6, LGPL-3.0.
Sterilizing filter textures: the instance's bundled GregTech Odyssey GTM 26.10.53,
LGPL-3.0. These works are not covered by GTNG's general MIT grant.

Original assets retain their bytes. The compressed structure is unchanged.
The StructureLib adapter changes axes and reserves a separate controller symbol.
The native controller adapts the machine to GTNG modular processing and GTNH
materials and original assembly recipes. This machine's recipe copies no longer
require research media or circuit imprints; native machines remain unchanged.
No protected GTOLib implementation is included.

CTM sprite layout conversion adapts Low-Drag-MC/LDLib-MultiLoader commit
1d4e4390ae58d16038845c651e122185c29f334b under GPL-3.0.
The modular controller incorporates existing GTNG GPL-3.0-only integration.
Retain the separately included license notices when redistributing.

Per-asset source, revision, modification and hashes: ASSET_MANIFEST.json.
Interpolated playback sprites are LGPL-3.0 derivatives of those original assets.
Their RGB frames are expanded per tick for Minecraft 1.7.10, retaining source
alpha, frame order and timing; duplicate frames share storage. Original files
remain unchanged. See interpolatedAnimations in the manifest.
Full source/mapping notes: reference/nyarlathoteps-tentacle-port/PORT.md.

SuprachronalAssemblyFrontend adapts AssemblyLineFrontend from GT5-Unofficial
5.09.54.205, GTNewHorizons contributors, LGPL-3.0. It retains native slot positions
and the first two progress bars, omitting the research slot and its arrow.
See RECIPE_FRONTEND.json and the included LGPL-3.0 and GPL-3.0 license texts.
