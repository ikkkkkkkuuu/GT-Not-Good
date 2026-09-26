package com.xyp.gtnotgood.common.packaged;

import java.util.Collections;

import net.minecraft.block.material.Material;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.util.inv.MEInventoryCrafting;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.container.InventoryFake;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileCrucible;

/**
 * One catalyst per processing pattern, at a real hot crucible. Native recipe selection checks research and
 * competing recipes; local aspects are spent first and missing aspects may be reserved from AE storage.
 * Payment and persistent output buffering occur on the server in the same tick, without loose item entities.
 */
public final class ThaumcraftCrucibleAdapter implements PackagedCoreRegistry.Adapter {

    @Override
    public boolean accepts(TileEntity target) {
        return target instanceof TileCrucible;
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
        provider.altarStatus = AltarStatus.UNLOADED;
        if (provider.getWorldObj() == null || provider.getWorldObj().isRemote
            || !(target.resolve(provider.getWorldObj()) instanceof TileCrucible crucible)) return null;
        var owner = provider.getOwnerPlayer();
        provider.altarStatus = AltarStatus.CRUCIBLE_OWNER;
        if (owner == null || owner.worldObj != crucible.getWorldObj()
            || !owner.worldObj.canMineBlock(owner, target.x, target.y, target.z)) return null;
        provider.altarStatus = AltarStatus.CRUCIBLE_HEAT;
        if (!isHeated(crucible)) return null;
        provider.altarStatus = AltarStatus.CRUCIBLE_RECIPE;
        ItemStack catalyst = singleCatalyst(ingredients);
        var outputs = pattern.getCondensedOutputs();
        if (catalyst == null || outputs == null || outputs.length != 1 || outputs[0] == null || !outputs[0].isItem())
            return null;
        ItemStack expected = outputs[0].getItemStack();
        if (expected == null || outputs[0].getStackSize() <= 0
            || outputs[0].getStackSize() > expected.getMaxStackSize()) return null;
        for (Object entry : ThaumcraftApi.getCraftingRecipes()) {
            if (!(entry instanceof CrucibleRecipe recipe) || recipe.aspects == null
                || recipe.aspects.size() == 0
                || !recipe.catalystMatches(catalyst)) continue;
            ItemStack result = recipe.getRecipeOutput();
            if (!TilePackagedProvider.sameItem(result, expected) || result.stackSize != outputs[0].getStackSize())
                continue;
            AspectList supplied = crucible.aspects.copy();
            AspectList missing = new AspectList();
            for (Aspect aspect : recipe.aspects.getAspects()) {
                int deficit = recipe.aspects.getAmount(aspect) - supplied.getAmount(aspect);
                if (deficit > 0) {
                    missing.add(aspect, deficit);
                    supplied.add(aspect, deficit);
                }
            }
            // Extra local aspects can select a different native recipe. Never debit for that situation.
            if (ThaumcraftCraftingManager.findMatchingCrucibleRecipe(owner.getCommandSenderName(), supplied, catalyst)
                != recipe) continue;
            provider.altarStatus = AltarStatus.CRUCIBLE_RETURNS_FULL;
            ItemStack[] planned = provider.planArcaneReturns(Collections.singletonList(result.copy()));
            if (planned == null) return null;
            provider.altarStatus = AltarStatus.CRUCIBLE_ESSENTIA;
            if (missing.size() > 0 && (!ModList.ThaumicEnergistics.isModLoaded()
                || !ThaumicEnergisticsSupply.reserveCrucible(provider, missing))) return null;

            // All checks have passed. Spend only actual reserved units, retaining unrelated local aspects.
            if (missing.size() > 0) for (Aspect aspect : missing.getAspects()) {
                String key = aspect.getTag();
                long left = provider.crucibleEssentiaCredit.get(key) - missing.getAmount(aspect);
                if (left == 0) provider.crucibleEssentiaCredit.remove(key);
                else provider.crucibleEssentiaCredit.put(key, left);
            }
            crucible.aspects = recipe.removeMatching(supplied);
            ((CrucibleCooldownAccess) crucible).gtnotgood$resetCraftingCooldown();
            crucible.markDirty();
            owner.worldObj.markBlockForUpdate(target.x, target.y, target.z);
            owner.worldObj.addBlockEvent(target.x, target.y, target.z, crucible.getBlockType(), 2, 5);
            provider.commitArcaneReturns(planned);
            provider.altarStatus = AltarStatus.CRUCIBLE_READY;
            cpw.mods.fml.common.FMLCommonHandler.instance()
                .firePlayerCraftingEvent(owner, result.copy(), new InventoryFake(new ItemStack[] { catalyst.copy() }));
            return result.copy();
        }
        return null;
    }

    /**
     * TC4 cannot raise its heat counter without water. A dry packaged crucible therefore checks the native
     * heat source directly; otherwise the normal boiling temperature applies. No water or heat is fabricated.
     */
    private static boolean isHeated(TileCrucible crucible) {
        if (crucible.heat > 150) return true;
        if (crucible.tank.getFluidAmount() > 0 || crucible.yCoord <= 0) return false;
        var world = crucible.getWorldObj();
        var below = world.getBlock(crucible.xCoord, crucible.yCoord - 1, crucible.zCoord);
        return below.getMaterial() == Material.lava || below.getMaterial() == Material.fire
            || below == thaumcraft.common.config.ConfigBlocks.blockAiry
                && world.getBlockMetadata(crucible.xCoord, crucible.yCoord - 1, crucible.zCoord) == 1;
    }

    /** Rejects extra catalysts, sacrifice items and native fluid stacks without consuming any supplied input. */
    static ItemStack singleCatalyst(InventoryCrafting ingredients) {
        ItemStack result = null;
        for (int slot = 0; slot < ingredients.getSizeInventory(); slot++) {
            if (ingredients instanceof MEInventoryCrafting table) {
                var nativeStack = table.getAEStackInSlot(slot);
                if (nativeStack != null && (!nativeStack.isItem() || nativeStack.getStackSize() != 1)) return null;
            }
            ItemStack stack = ingredients.getStackInSlot(slot);
            if (stack == null) continue;
            if (result != null || stack.stackSize != 1) return null;
            result = stack;
        }
        return result;
    }
}
