# Large Industrial Apiary

Upstream: GTNewHorizons/GT5-Unofficial `5.09.54.183`.
Pinned commit: `f8453bce28793713d314f6dcfa5ac551d700efd6`, confirmed through the upstream tag API.
https://github.com/GTNewHorizons/GT5-Unofficial/tree/f8453bce28793713d314f6dcfa5ac551d700efd6

Original authors: kuba6000 and KubaTech/GT5-Unofficial contributors.
The controller and KubaTech base retain Copyright (C) 2022-2024 kuba6000.
The pinned README, LICENSE.txt and source headers were inspected before adaptation.
The controller's explicit grant and the repository grant are LGPL-3.0-or-later.
The upstream GUI has no separate file-level license notice; the repository grant applies.
The adapted controller and GUI remain LGPL-3.0-or-later and are excluded from
GT-Not-Good's general MIT grant. The unchanged upstream LGPL text and the incorporated
GPL version 3 terms accompany this notice. Distributions must provide corresponding
source for the adapted code and preserve these notices.

| Upstream path under `src/main/java/` | Source SHA-256 | Local destination under `src/main/java/` |
| --- | --- | --- |
| `kubatech/tileentity/gregtech/multiblock/MTEMegaIndustrialApiary.java` | `10a39acff0bc2358921c03a1086669178c8899e6c4a40a729832b006a2287e58` | `com/xyp/gtnotgood/common/machines/multiblock/LargeIndustrialApiary.java` |
| `kubatech/gui/modularui2/MTEMegaIndustrialApiaryGui.java` | `52c3b1b1b2b527054e3c97b9a444b4459d1e97d39a26ea68cb3ee8c9820ae74c` | `com/xyp/gtnotgood/common/gui/modularui/multiblock/LargeIndustrialApiaryGui.java` |

Additional adapted behavior source:
`kubatech/api/implementations/KubaTechGTMultiBlockBase.java`, SHA-256
`10b00d99485693e9c583a391ca79e5924b733f8627d2a584feb19ead8fe5efef`.
Its fractional voltage-tier formula is adapted in `LargeIndustrialApiary.java`.
The dependency source artifact is `GT5-Unofficial-5.09.54.183-sources.jar`,
SHA-1 `b0f3eb42604cfc3ad88d56c50d0dbfa375d89419`.

Adaptations dated 2026-10-15:
- Integrate the controller and paginated bee GUI with local machine registration,
  localization and the existing GTNG electric multiblock base.
- Preserve the 15 x 17 x 15 upstream structure, production and swarming modes,
  queen storage, product simulation and royal-jelly bonus.
- Remove the minimum LuV energy-hatch tier and use only standard Borosilicate Glass
  Blocks (`bartworks:BW_TieredGlass`, metadata 0; `ItemRegistry.bw_realglas`).
  Glass does not restrict energy tier. Accept flowers independently of each queen's
  flower-provider requirement.
- Use LV power per stored queen and eight times the capacity computed at LV power.
- Adapt the KubaTech base's fractional voltage-tier calculation; use local
  multiblock integration and checked queen ejection through GT's item-output helper.
- Register a local MV assembler controller recipe using an MV hull, Forestry
  apiaries, steel frames and plates, MV circuits, motors and pistons, and soldering alloy.
- Reuse dependency textures, overlays, icons and other visual resources at runtime;
  no upstream visual assets are copied into this port.

Extracted references stay under ignored `build/tmp/apiary-study` and never participate
in compilation or resource packaging.
