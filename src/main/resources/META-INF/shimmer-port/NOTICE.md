# Shimmer recovery port

Upstream: ABKQPO/GT-Not-Leisure, dev-290 commit
f1b74060d2a91b422eb950076075c601a644f882.
https://github.com/ABKQPO/GT-Not-Leisure/tree/f1b74060d2a91b422eb950076075c601a644f882
Copyright ABKQPO and GT-Not-Leisure contributors. LGPL-3.0; see
META-INF/licenses/GT-Not-Leisure-LGPL-3.0.txt and GT-Not-Leisure-GPL-3.0.txt.
The ported code is not covered by this project's general MIT grant.

Source -> destination under com/xyp/gtnotgood:
- utils/recipes/DisassemblerHelper.java -> common/recipe/gtnotgood/ShimmerRecoveryRules.java
- its collection logic and common/recipe/gtnl/ShimmerRecipes.java -> TransmutationRecipes.java
- utils/recipes/ReversedRecipeRegistry.java -> ShimmerCraftingRegistry.java
- mixins/late/gregtech/MixinGTShapedRecipe.java and MixinGTShapelessRecipe.java ->
  mixins/late/Gregtech/TransmutationShapedRecipeMixin.java and TransmutationShapelessRecipeMixin.java

Modifications: package names, powered GT machine recipe registration, native fluid hatch outputs,
defensive copies, removal of debug provenance storage, optional live GTNL conversion-table import.
Fallback preserves assembler/assembly-line/space-assembler/GT-machine-crafting ordering and
upstream blacklist/material replacement rules. Hard overrides for recipes added/changed by GTNL are supplied by its installed conversion table;
without GTNL, recovery follows the recipes registered by the installed pack.
No upstream textures are copied. Reference checkouts are not compiled or packaged.

