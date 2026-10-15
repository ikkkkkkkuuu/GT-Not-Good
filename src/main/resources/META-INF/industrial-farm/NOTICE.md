# Large Industrial Farm

Copyright (c) 2022-2026 the GTNH Team.

The structure, validation and farming calculations in
`com/xyp/gtnotgood/common/machines/multiblock/LargeIndustrialFarm.java`
are adapted from GTNewHorizons/CropsNH `MTEIndustrialFarm.java`, version
`2.0.129`, commit `47debb72b230382c91aa256b92d715062c3a318e`.

Source:
https://github.com/GTNewHorizons/CropsNH/blob/47debb72b230382c91aa256b92d715062c3a318e/src/main/java/com/gtnewhorizon/cropsnh/tileentity/multi/MTEIndustrialFarm.java

Source SHA-256:
`9a3c90241c6b0252b03d0008aab8783f2bd4a0508ce24e07f6d67ba39f4fc795`.

The adapted controller retains LGPL-3.0-or-later and is outside this project's
general MIT grant. CropsNH's pinned README grants LGPL-3.0 or later for GTNH
modifications and records the original Agricraft MIT attribution to
InfinityRaider (2014):
https://github.com/GTNewHorizons/CropsNH/blob/47debb72b230382c91aa256b92d715062c3a318e/README.md#license

Changes: a separate controller with fixed maximum dimensions (5 x 4 x 14),
upgrade modules independent of seed-bed tier, default multi-amp energy support,
and fixed borosilicate glass without a glass-based energy-tier limit.
The controller also provides independent 12-fold capacity per seed type, coexistence of growth
and overclock modules, perfect overclocks with fourfold power and throughput,
tier-scaled module effects, automatic grouping of up to 36 seed types, and
water/fertilizer use based on planted seed count and overclock throughput.
Crops require no soil. Storage contains 108 slots: each of the 36 seed types has
one seed slot and two independent environment-card slots. Every crop uses only
its own cards. Migration from older 38/74-slot shared-card layouts keeps the two
cards on the saved selected type and leaves other types' card slots empty.
Migration from the original 144-slot layout retains all per-type cards.
Previously stored soil is preserved for safe return during migration.

The local inventory in
`com/xyp/gtnotgood/common/machines/crop/IndustrialFarmInventory.java` and bridge in
`com/xyp/gtnotgood/common/machines/crop/SelectedCropInventory.java` expose the
selected seed and its two environment cards through the native slot
indices. The GUI in
`com/xyp/gtnotgood/common/gui/modularui/multiblock/LargeIndustrialFarmGui.java`
inherits CropsNH's `MTEIndustrialFarmGui`, retaining the original panel size,
layout, controls and textures. Only the native soil widget is removed, leaving
three visible slots without a placeholder. Previous/next seed-type selection
and a counter are added. CropsNH crop APIs, environment cards and icons are used
from the installed dependency at runtime.
No upstream GUI source, textures or other visual assets are copied into these
local additions.

`LGPL-3.0.txt` is the unchanged pinned upstream `LICENSE.txt`:
https://github.com/GTNewHorizons/CropsNH/blob/47debb72b230382c91aa256b92d715062c3a318e/LICENSE.txt

Its SHA-256 is
`e3a994d82e644b03a792a930f574002658412f62407f5fee083f2555c5f23118`.
The incorporated GPL terms are included as `GPL-3.0.txt`, obtained from
https://www.gnu.org/licenses/gpl-3.0.txt, SHA-256
`3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986`.

The adapted controller, local inventory/bridge/GUI and these notices are packaged.
Reference sources and cached dependency source archives are outside
compilation/resource inputs.
