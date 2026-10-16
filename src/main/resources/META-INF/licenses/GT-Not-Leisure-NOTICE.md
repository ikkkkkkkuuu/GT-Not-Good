# GT Not Leisure compact AE machine port

Upstream: https://github.com/ABKQPO/GT-Not-Leisure
Commit: 6cbc6927af4f44c445ea7a879796b4764b00988d
License: GNU Lesser General Public License version 3.
Copyright: ABKQPO and GT Not Leisure contributors.

Adapted files: QuantumComputer, AssemblerMatrix, their ModularUI GUIs, ECraftingCPUCluster,
DireCraftingPatternDetails, LargeInventoryCrafting, AssemblerMatrixPatternState, CraftingBatchPlanner,
CraftingBatchPlannerImpl and the attributed mixins under appliedenergistics/compact.

Changes: GTNH Java 8/API compatibility, local registries and GUI assets, 3x2x1 structures,
576 matrix pattern slots, fixed maximum performance, one-tick assembly, removal of energy costs,
and overflow/persistence/interruption fixes. Translation text was moved into Java declarations.
No upstream image assets were copied. Derived source files retain LGPL-3.0 and are excluded from
this addon's general MIT grant. The full source is included in the corresponding sources artifact.

See GT-Not-Leisure-LGPL-3.0.txt and GT-Not-Leisure-GPL-3.0.txt in this directory.

## Wireless energy cover recipes

Source: AssemblerRecipes#loadWirelessHatch and utils/item/ItemUtils from the same upstream,
commit 2abe879191443af5db2ae653a9087d34ad0432dc (LGPL-3.0).
Destination: com/xyp/gtnotgood/common/recipe/gregtech/WirelessEnergyCoverRecipes.java.
Changes: retain the fourteen native 1 A cover recipes and native solder alternatives; use GT's
recipe builder directly and skip registration when GT Not Leisure is installed. No upstream
assets, custom 4 A covers, hatches or disassembly recipe maps are included.
