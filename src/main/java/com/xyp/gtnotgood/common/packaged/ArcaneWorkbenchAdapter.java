package com.xyp.gtnotgood.common.packaged;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileMagicWorkbench;

/**
 * Synchronous TC4 arcane crafting at a bound, empty workbench. The saved grid is only a layout: every dispatch
 * rechecks the owner's research, supplied ingredients, actual native result and wand payment on the server.
 * Results and container items are committed to the persistent provider buffer in the same tick as the wand.
 * No preview result is treated as an already-crafted item, and no offline player is impersonated.
 */
public final class ArcaneWorkbenchAdapter implements PackagedCoreRegistry.Adapter {

    @Override
    public boolean accepts(TileEntity target) {
        return target instanceof TileArcaneWorkbench;
    }

    @Override
    public boolean returnsImmediately() {
        return true;
    }

    @Override
    public IInventory output(TileEntity target) {
        return null;
    }

    @Override
    public ItemStack dispatch(TilePackagedProvider provider, PackagedTarget target, ICraftingPatternDetails pattern,
        InventoryCrafting ingredients) {
        provider.arcaneMissingAspect = "";
        provider.arcaneMissingUnits = 0;
        provider.altarStatus = AltarStatus.UNLOADED;
        if (!(target.resolve(provider.getWorldObj()) instanceof TileArcaneWorkbench workbench)) return null;
        EntityPlayer owner = provider.getOwnerPlayer();
        provider.altarStatus = AltarStatus.ARCANE_OWNER;
        if (owner == null || owner.worldObj != workbench.getWorldObj()
            || !owner.worldObj.canMineBlock(owner, target.x, target.y, target.z)) return null;
        provider.altarStatus = AltarStatus.ARCANE_OCCUPIED;
        if (workbench.eventHandler != null) return null;
        for (int i = 0; i < 10; i++) if (workbench.getStackInSlot(i) != null) return null;
        provider.altarStatus = AltarStatus.ARCANE_WAND;
        ItemStack installed = workbench.getStackInSlot(10);
        if (installed == null || !(installed.getItem() instanceof ItemWandCasting wand) || wand.isStaff(installed))
            return null;
        provider.altarStatus = AltarStatus.ARCANE_RECIPE;
        TileMagicWorkbench grid = ArcaneWorkbenchPatterns.grid(pattern.getPattern(), owner.worldObj);
        if (grid == null || !matchesIngredients(grid, ingredients)) return null;
        grid.xCoord = workbench.xCoord;
        grid.yCoord = workbench.yCoord;
        grid.zCoord = workbench.zCoord;
        grid.setInventorySlotContentsSoftly(10, installed.copy());
        ItemStack result = ThaumcraftCraftingManager.findMatchingArcaneRecipe(grid, owner);
        AspectList cost = ThaumcraftCraftingManager.findMatchingArcaneRecipeAspects(grid, owner);
        var outputs = pattern.getCondensedOutputs();
        if (result == null || result.stackSize <= 0
            || cost == null
            || outputs == null
            || outputs.length != 1
            || outputs[0] == null
            || !TilePackagedProvider.sameItem(result, outputs[0].getItemStack())
            || result.stackSize != outputs[0].getStackSize()) return null;

        List<ItemStack> returns = new ArrayList<>();
        returns.add(result.copy());
        for (int i = 0; i < 9; i++) {
            ItemStack input = grid.getStackInSlot(i);
            if (input == null || !input.getItem()
                .hasContainerItem(input)) continue;
            ItemStack container = input.getItem()
                .getContainerItem(input.copy());
            if (container != null && container.stackSize > 0
                && (!container.isItemStackDamageable() || container.getItemDamage() <= container.getMaxDamage())) {
                returns.add(container.copy());
            }
        }
        provider.altarStatus = AltarStatus.ARCANE_RETURNS_FULL;
        ItemStack[] planned = provider.planArcaneReturns(returns);
        if (planned == null) return null;

        ItemStack paidWand = installed.copy();
        AspectList missing = new AspectList();
        provider.altarStatus = AltarStatus.ARCANE_CAPACITY;
        if (cost.size() > 0) for (Aspect aspect : cost.getAspects()) {
            if (aspect == null || !aspect.isPrimal() || cost.getAmount(aspect) < 0) return null;
            double raw = (double) cost.getAmount(aspect) * 100;
            if (raw > Integer.MAX_VALUE) return null;
            // Match TC4's float multiplication and truncation, including player/cap/sceptre discounts.
            int required = (int) ((float) (int) raw * wand.getConsumptionModifier(paidWand, owner, aspect, true));
            if (required < 0 || required > wand.getMaxVis(paidWand)) return null;
            int deficit = Math.max(0, required - wand.getVis(paidWand, aspect));
            if (deficit > 0) missing.add(aspect, deficit);
        }
        provider.altarStatus = AltarStatus.ARCANE_ESSENTIA;
        if (missing.size() > 0) {
            if (!ModList.ThaumicEnergistics.isModLoaded() || !ThaumicEnergisticsSupply
                .reserveArcane(provider, missing, Math.max(1, Math.min(1000, Config.arcaneVisPerEssentia)) * 100))
                return null;
            for (Aspect aspect : missing.getAspects()) {
                if (wand.addRealVis(paidWand, aspect, missing.getAmount(aspect), true) != 0) return null;
            }
        }
        // Debit a copy through the native API before committing anything to the real workbench.
        if (cost.size() > 0 && !wand.consumeAllVisCrafting(paidWand, owner, cost, true)) return null;
        if (missing.size() > 0) for (Aspect aspect : missing.getAspects()) {
            String key = aspect.getTag();
            long left = provider.arcaneVisCredit.get(key) - missing.getAmount(aspect);
            if (left == 0) provider.arcaneVisCredit.remove(key);
            else provider.arcaneVisCredit.put(key, left);
        }
        workbench.setInventorySlotContentsSoftly(10, paidWand);
        workbench.markDirty();
        owner.worldObj.markBlockForUpdate(target.x, target.y, target.z);
        provider.commitArcaneReturns(planned);
        // The AE CPU relinquishes the supplied items only when this dispatch returns success.
        result.onCrafting(owner.worldObj, owner, result.stackSize);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .firePlayerCraftingEvent(owner, result.copy(), grid);
        return result;
    }

    /** Exact multiset check: AE processing inputs may be condensed/reordered; extra items are never swallowed. */
    static boolean matchesIngredients(TileMagicWorkbench grid, InventoryCrafting supplied) {
        int[] remaining = new int[supplied.getSizeInventory()];
        int total = 0;
        for (int i = 0; i < remaining.length; i++) {
            if (supplied instanceof appeng.util.inv.MEInventoryCrafting nativeTable) {
                var stack = nativeTable.getAEStackInSlot(i);
                if (stack != null && (!stack.isItem() || stack.getStackSize() <= 0 || stack.getStackSize() > 9))
                    return false;
            }
            ItemStack stack = supplied.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.stackSize <= 0 || stack.stackSize > 9) return false;
            remaining[i] = stack.stackSize;
            total += stack.stackSize;
            if (total > 9) return false;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack required = grid.getStackInSlot(i);
            if (required == null) continue;
            int match = -1;
            for (int j = 0; j < remaining.length; j++) {
                if (remaining[j] > 0 && TilePackagedProvider.sameItem(required, supplied.getStackInSlot(j))) {
                    match = j;
                    break;
                }
            }
            if (match < 0) return false;
            remaining[match]--;
            total--;
        }
        return total == 0;
    }
}
