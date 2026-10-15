# Integrated chemical recipe port

Upstream: ABKQPO/GT-Not-Leisure and its contributors.
Source: https://github.com/ABKQPO/GT-Not-Leisure/tree/651396c75d108e6ff5399a44c56c12214ad27d3d
Commit: `651396c75d108e6ff5399a44c56c12214ad27d3d`.
License: GNU Lesser General Public License version 3, inspected in upstream `LICENSE.txt`.

| Upstream Java source | Addon Java destination |
| --- | --- |
| `com/science/gtnl/common/recipe/gtnl/FuelRefiningComplexRecipes.java` | `com/xyp/gtnotgood/common/recipe/gtnotgood/FuelRefiningRecipes.java` |
| `com/science/gtnl/common/recipe/gtnl/WoodDistillationRecipes.java` | `com/xyp/gtnotgood/common/recipe/gtnotgood/WoodDistillationRecipes.java` |
| `com/science/gtnl/common/recipe/gtnl/PetrochemicalPlantRecipes.java` | `com/xyp/gtnotgood/common/recipe/gtnotgood/PetrochemicalRecipes.java` |
| `com/science/gtnl/utils/recipes/metadata/FuelRefiningMetadata.java` | `com/xyp/gtnotgood/common/recipe/metadata/FuelRefiningTier.java` |

Changes: local package/class names, static registration, native GTNH `GTRecipeBuilder`,
addon-owned recipe maps and fuel-refining metadata. The 25 fuel recipes, one wood recipe
and 11 petrochemical recipes retain their quantities, durations, energy costs, heat
requirements and refining tiers. The petrochemical recipe using GTNL's own Barnarda C
sap fluid is omitted; no replacement fluid is registered. GTNL registration and builder
classes are removed; the addon registers its own recipes and does not require GTNL.
No upstream artwork is copied for this port.

These adapted sources retain LGPL-3.0 and are excluded from the addon's general MIT grant.
The corresponding source is distributed in this project's source repository and sources
artifact. `LGPL-3.0.txt` reproduces upstream `LICENSE.txt`; `GPL-3.0.txt` provides the
incorporated GNU GPL terms. The ignored upstream reference checkout is excluded from
compilation and resource packaging.
