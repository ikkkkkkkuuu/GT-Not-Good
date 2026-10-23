# Quantum crafting port

Derived from ABKQPO/GT-Not-Leisure, dev-290 commits:
- 5837c5a8f2c2590ceb45573b94660c67fb9eba7d: custom-name editing.
- 2b79458b3074c4d20d3a8a96bd11fe9d60daaef9 (PR #646): quantum planning, cycles, timeout and variant budgets.
- 7b17611117a19b7cc2a921e7213b0578f1513504: pattern optimizer support.

Source: https://github.com/ABKQPO/GT-Not-Leisure/tree/7b17611117a19b7cc2a921e7213b0578f1513504

The derived source and two original GUI images retain LGPL-3.0, independently of the project's MIT license.
See LICENSE.txt and ../licenses/GT-Not-Leisure-GPL-3.0.txt for the license terms.
SOURCE_MANIFEST.json pins copied files and source hashes. Reference downloads are outside source sets.

Existing ECraftingCPUCluster, compact/MixinCraftingGridCache, compact/MixinCraftingCPUCluster,
CraftingBatchPlanner/Impl and QuantumComputer/AssemblerMatrix GUI name handling receive selective changes.
Local zero-energy dispatch, 1024 native dispatch budget, native fluid batches, independent CPU allocation,
structure, machine IDs and saved data are retained. New variant budgets are stored under gtng:variantBudgets.
New packet IDs append to the existing channel; late mixins load by mod presence and client side.

Adaptations use existing Java 8/Jabel, Guava and AE2 rv3-beta-1080-GTNH APIs; no dependency upgrade.
Client preferences and server limits have independent GTNOTGOOD config files. Translations are declared
at Java use sites. Upstream unrelated hatches, general lang cleanup and interface fixes are excluded.
