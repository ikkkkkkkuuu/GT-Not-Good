# Large ME Dual Interface

The implementation links to the installed GTNH Applied Energistics 2 and AE2 Fluid
Crafting libraries. Native interface inventory, crafting, return, upgrade, wrench,
orientation and settings APIs remain supplied by those libraries.
Empty upgrade tooltips adapt native `GuiUpgradeable` card lookup and supported
limit rules; those source adaptations in `LargeInterfaceGui.java` retain LGPL terms.

- Applied Energistics 2 Unofficial: rv3-beta-1073-GTNH,
  commit `151550f6d558a663eee792f0bfa22e12a53c0e54`, LGPL-3.0-or-later.
  https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial
- AE2FluidCraft-Rework: 1.5.110-gtnh,
  commit `6e8dd013bd4ec123cf74daaecf581534d87f2391`, LGPL-3.0.
  https://github.com/GTNewHorizons/AE2FluidCraft-Rework

The block render layout and setting icon indices were adapted from these native
interfaces. Adapted source files are `client/largeinterface/LargeInterfaceRenderer.java`
and `common/blocks/largeinterface/LargeInterfaceGui.java` under
`src/main/java/com/xyp/gtnotgood/` and are distributed under LGPL-3.0-or-later.
Their changes provide an orange default render, a scrollable 900-slot pattern area,
MUI2 server-authoritative controls and no stock request configuration.

The orange interface block textures and GTNH AE setting icons are resolved from
the user's installed dependencies at runtime. Dependency code is not embedded.
Central pattern slots retain the native GT5-Unofficial 5.09.54.183 MUI2
`PatternSlot` overlay: `GTGuiTextures.OVERLAY_SLOT_PATTERN_ME`, resolved as
`gregtech:textures/gui/overlay_slot/pattern_me.png`. It is composed over the dark
AE2 slot background only when the real pattern slot is empty, matching native
interface hint visibility. Occupied slots keep GT's native output preview and
quantity rendering, including Ultimate Encoded Patterns. The empty hint is tinted
using the dark pack's muted text color on an independent runtime drawable.
No GT overlay PNG is copied; that resource remains covered by the installed
GregTech dependency's upstream terms.

The default dark MUI2 surfaces are unmodified PNGs from AE2 Dark Mode by
Ridanisaurus, version 1.0.6 for Minecraft 1.21, published July 10, 2024:

- https://www.curseforge.com/minecraft/texture-packs/ae2-dark-mode/files/5520011
- https://modrinth.com/resourcepack/ae2-dark-mode/version/BXpYgw04
- Source archive: `AE2DarkMode-V1.0.6.zip`, Modrinth project `UP2ZKKkg`, version `BXpYgw04`.
- Archive SHA-256: `a5a721617a5043b53d7223b102626b878661f53931e638b23dcc7ffdb58c1249`.
- Destination: `assets/gtnotgood/textures/gui/large_interface`.

These dark assets and their runtime crop/composition adaptations are licensed
under Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International:
https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode
The complete license is included as `CC-BY-NC-SA-4.0.txt` beside this notice.
No PNG pixel edits are made. Normal text colors follow the pack's
`screens/common/palette.json`. Hover button text uses the dark outline color from
the same pack's state atlas for contrast on its blue hover surface.

The previously used light AE2 surfaces remain available to other features under
their existing source notices:

- Applied Energistics 2, version 19.2.17, tag `neoforge/v19.2.17`, commit
  `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`.
  Textures and models: (c) 2020 Ridanisaurus Rid; (c) 2013-2020 AlgorithmX2 et al.
  https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a
- Applied Energistics 2, tag `v26.1.12-beta`, commit
  `b7cf5822d9c128a61d9291cb2c1f92319253e4f0`, supplies the already packaged
  ME-stock text-field atlas.
  https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/b7cf5822d9c128a61d9291cb2c1f92319253e4f0

The original AE2 visual assets are licensed under Creative Commons
Attribution-NonCommercial-ShareAlike 3.0 Unported:
https://creativecommons.org/licenses/by-nc-sa/3.0/legalcode
They retain their existing `META-INF/ae2lt-port` and `META-INF/me-stock-port`
attributions and are excluded
from this project's general MIT license grant. No upstream endorsement is implied.

`LargeInterfaceGuiTextures.java` crops and combines these resources at runtime
to draw the enlarged frame, slots, settings sidebar, upgrade panel, buttons and
scroll handle. The original PNG bytes are unchanged; the composed appearance is
an adaptation covered by the artwork license. The upgrade-frame drawable repaints
the four modern slot backgrounds after its frame in the same widget drawing layer,
so it does not obscure MUI2's earlier slot-background pass. Empty upgrade widgets
then draw the pack's 16x16 `BACKGROUND_UPGRADE` chip hint inset by one pixel,
before native hover shading; occupied slots draw their actual cards instead.
Pattern slots disable the underlying GT theme background to preserve dark borders.
`ASSET_MANIFEST.json` records the source release IDs, local paths, hashes and crop
rectangles. The existing
`META-INF/ae2lt-port/AE2-README.md` preserves the official asset-license declaration;
`META-INF/me-stock-port` records the text-field source and license.
Reference sources under ignored `build/reference` are neither compiled nor packaged.

The GNU LGPL and GPL license texts are included in the existing
`META-INF/ae2lt-port/AE2-LGPL-3.0.txt` and
`META-INF/ae2lt-port/GPL-3.0.txt` resources.
