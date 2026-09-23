# Third-Party Notices

## GT5-Unofficial iron fuel rod textures

The active and depleted iron fuel rod textures are color variants of GT5-Unofficial's
four-cell uranium fuel rod textures, version `5.09.54.133`. The original silhouette,
shading, and alpha remain intact; only colored fuel pixels were recolored. GT5-Unofficial
identifies its license as LGPL-3.0-or-later. These two derived textures retain that license
and are outside this project's general MIT grant. Exact source and destination hashes,
modifications, and license text are bundled in `META-INF/iron-fuel-rod/`.

## GT Not Leisure compact AE machines

Quantum Computer, Assembler Matrix, their GUI logic and AE batch/CPU helpers are adapted from
[ABKQPO/GT-Not-Leisure](https://github.com/ABKQPO/GT-Not-Leisure), commit
`6cbc6927af4f44c445ea7a879796b4764b00988d`, under LGPL-3.0. These derived files retain their
upstream license and are outside the project's general MIT grant. No upstream image assets are included.
The port uses compact structures, fixed maximum performance, 144 matrix pattern slots and no machine
energy cost. Source mappings and modifications are recorded in `reference/UPSTREAM_PORT_NOTES.md`.
License texts are bundled as `GT-Not-Leisure-LGPL-3.0.txt` and `GT-Not-Leisure-GPL-3.0.txt`
under `META-INF/licenses/`.

## AdvancedAE Advanced IO Bus

The Advanced IO Bus behavior is adapted from pedroksl/AdvancedAE commit
`5ad43ee1e5f7a8b9fe7a1eacfaebdd44b61b624c` (LGPL-3.0). Three unchanged upstream part textures
are included. These files and adapted code retain their upstream license, outside the project's MIT grant.
Source mappings, adaptation scope, license texts and asset hashes are in `META-INF/advancedio-port/`.
The faithful IO GUI incorporates AE2AddonLib 26.1.3-alpha layout (GPL-3.0) and AE2 v26.1.10-beta artwork
(CC BY-NC-SA 3.0, Ridanisaurus Rid / AlgorithmX2 / contributors). AdvancedIOGui is GPL-3.0-only;
the copied artwork retains CC BY-NC-SA 3.0. Neither is covered by the general MIT grant.

## Microsoft Edge window icons

The optional window icons are Microsoft Edge artwork owned by Microsoft Corporation,
excluded from this project's MIT license. Source version, hash and conversion details
are recorded in `src/main/resources/META-INF/edge-icon/NOTICE.md`.

## Extra Utilities 2 Mechanical User

Four unchanged RWTema textures are included for the Mechanical User and speed upgrade.
Upstream reserves all rights; these assets are excluded from this project's license.
See `src/main/resources/META-INF/mechanical-user/NOTICE.md` for source and file mappings.

## Flux Networks

The Flux plug/point code and original block models/textures are ported from SonarSonic/Flux-Networks
(MIT). The modern GUI code/layout and original GUI artwork are ported from its 1.20 version.
GUI designs/resources are copyright (c) 2019-2021 BloCamLimb and licensed CC BY-NC-SA 4.0;
the adapted GUI design retains those attribution, noncommercial and share-alike conditions.
Exact revisions, file mappings, changes and bundled licenses are documented in
`src/main/resources/META-INF/flux-port/NOTICE.md`.

## LDLib2

The experimental GUI framework subset in `com.xyp.ldlib` is adapted from
the user-supplied LDLib2 2.2.40 source tree (Minecraft 1.21.1), by KilaBash / Low-Drag-MC.
The port changes rendering and input for Minecraft 1.7.10 and supplies Java row/column layouts.
Upstream: https://github.com/Low-Drag-MC/LDLib2. The LGPL-3.0 license and detailed scope/source notice
are bundled in `src/main/resources/META-INF/ldlib-port/`. The incorporated GPL-3.0 terms are also
included at `src/main/resources/META-INF/licenses/MessTech-GPL-3.0.txt`.

## GTCEu Modern GUI

Fancy page contracts and window organization in `com.xyp.ldlib.gui.fancy` are adapted from
GregTechCEu/GregTech-Modern v7.4.0-1.20.1, commit `07ac5207f6f58ba3a2f9cd8b862b382d24300a17`.
The root `background.png` and `button.png` in `assets/gtnotgood/textures/gui/ldlib/`
come from this version's `base/background.png` and `widget/button.png`.
GTCEu's LGPL-3.0 license is bundled in `META-INF/ldlib-port/GTCEu-LICENSE`.
Upstream: https://github.com/GregTechCEu/GregTech-Modern/tree/v7.4.0-1.20.1

## Modernity GUI theme

Slot and side-tab artwork under `assets/gtnotgood/textures/gui/ldlib/modern/` is copied unchanged from
ModernityGTNH/Modernity-GTNH-UI contributors, commit `87fee0aac305ca8f1736278266ad87d94deff3b4`.
These assets are separately licensed under CC BY-NC-SA 4.0 (attribution, noncommercial,
share-alike). The license and exact file mappings are bundled in `META-INF/ldlib-port/`.
Runtime stretching, atlas selection and tinting adapt the assets to the GUI library.
Upstream: https://github.com/ModernityGTNH/Modernity-GTNH-UI/tree/87fee0aac305ca8f1736278266ad87d94deff3b4

## Applied Energistics 2 GUI theme

The wildcard GUI layout is adapted from the user-supplied Wildcard-Pattern 0.1.2
source by LeoDreamer (MIT), including 158x180 pages, 156x25 component rows and fixed
bottom-button positions. Its license is bundled in `META-INF/ldlib-port/Wildcard-Pattern-LICENSE`.
Upstream: https://github.com/LeoDreamer2004/Wildcard-Pattern

The LDLib modern theme background, button states, text-field atlas and small scroller
are copied unchanged from Applied Energistics 2 contributors, Minecraft 1.21.1 branch,
commit `fd8b717a405672ce4f65ba540f1db8c91317daa4`, under LGPL-3.0.
Exact mappings and the upstream license are bundled in `META-INF/ldlib-port/NOTICE.md`
and `META-INF/ldlib-port/AE2-LICENSE`. Runtime slicing adapts these assets for 1.7.10.
Upstream: https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/fd8b717a405672ce4f65ba540f1db8c91317daa4

## XNet

The programmable network block textures under `assets/gtnotgood/textures/blocks/network/` and GUI artwork
under `assets/gtnotgood/textures/gui/network/` are copied from the sibling XNet-1.20 source tree
(`assets/xnet/textures/block/` and `assets/xnet/textures/gui/`). The eight-channel controller concept and
GUI styling are adapted from XNet. Networking, Minecraft 1.7.10 rendering and ModularUI2 screens are
implemented for this addon.
XNet is distributed under the MIT License. The original license is included in
`src/main/resources/META-INF/licenses/XNet-MIT.txt`.
