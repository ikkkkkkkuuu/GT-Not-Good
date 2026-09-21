package com.xyp.gtnotgood.common.packaged;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import WayofTime.alchemicalWizardry.api.altarRecipeRegistry.AltarRecipeRegistry;
import WayofTime.alchemicalWizardry.common.bloodAltarUpgrade.UpgradedAltars;
import WayofTime.alchemicalWizardry.common.tileEntity.TEAltar;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.util.inv.MEInventoryCrafting;

/**
 * Dispatches one real Blood Magic recipe into an empty altar. LP supply, tier checks and processing remain native.
 * Patterns describe one recipe step; orb charging, fluid ingredients and implicit multi-step chains are rejected.
 */
public final class BloodAltarAdapter implements PackagedCoreRegistry.Adapter {

    @Override
    public boolean accepts(TileEntity target) {
        return target instanceof TEAltar && target instanceof BloodAltarAccess;
    }

    @Override
    public ItemStack dispatch(TilePackagedProvider provider, PackagedTarget target, ICraftingPatternDetails pattern,
        InventoryCrafting ingredients) {
        provider.altarStatus = AltarStatus.UNLOADED;
        if (!(target.resolve(provider.getWorldObj()) instanceof TEAltar altar)
            || !(altar instanceof BloodAltarAccess access)) return null;
        provider.altarStatus = AltarStatus.OWNER;
        var owner = provider.getOwnerPlayer();
        var world = altar.getWorldObj();
        if (owner == null || owner.worldObj != world || !world.canMineBlock(owner, target.x, target.y, target.z))
            return null;
        provider.altarStatus = AltarStatus.OCCUPIED;
        if (altar.getStackInSlot(0) != null || altar.isActive()) return null;
        provider.altarStatus = AltarStatus.UNLOADED;
        // Check the actual registered structure coordinates before Blood Magic examines world blocks.
        for (int tier = 2; tier <= UpgradedAltars.highestAltar; tier++) {
            var components = UpgradedAltars.getAltarUpgradeListForTier(tier);
            if (components == null) continue;
            for (var component : components) {
                if (!world.getChunkProvider()
                    .chunkExists((target.x + component.x()) >> 4, (target.z + component.z()) >> 4)) return null;
            }
        }
        provider.altarStatus = AltarStatus.BLOOD_RECIPE;
        ItemStack input = planInput(ingredients);
        if (input == null || pattern.isCraftable()) return null;
        altar.checkAndSetAltar();
        ItemStack expected = matchResult(input, altar.getTier(), pattern);
        if (expected == null) return null;
        access.gtnotgood$holdResult(expected);
        altar.setInventorySlotContents(0, input.copy());
        altar.markDirty();
        // Let the altar's normal tick start processing, retaining its native cooldown and LP consumption.
        return expected;
    }

    /** Combines only identical item ingredients, without mutating the AE dispatch table or accepting fluid packets. */
    static ItemStack planInput(InventoryCrafting ingredients) {
        ItemStack input = null;
        for (int slot = 0; slot < ingredients.getSizeInventory(); slot++) {
            if (ingredients instanceof MEInventoryCrafting nativeTable) {
                var stack = nativeTable.getAEStackInSlot(slot);
                if (stack != null && (!stack.isItem() || stack.getStackSize() <= 0 || stack.getStackSize() > 64))
                    return null;
            }
            ItemStack stack = ingredients.getStackInSlot(slot);
            if (stack == null) continue;
            if (stack.stackSize <= 0 || stack.stackSize > Math.min(64, stack.getMaxStackSize())) return null;
            if (input == null) input = stack.copy();
            else {
                if (!TilePackagedProvider.sameItem(input, stack)
                    || input.stackSize > Math.min(64, input.getMaxStackSize()) - stack.stackSize) return null;
                input.stackSize += stack.stackSize;
            }
        }
        return input;
    }

    /** Uses the first native matching recipe, including its NBT rules and multiplied output count. */
    static ItemStack matchResult(ItemStack input, int tier, ICraftingPatternDetails pattern) {
        var recipe = AltarRecipeRegistry.getAltarRecipeForItemAndTier(input, tier);
        if (recipe == null || recipe.getCanBeFilled()) return null;
        ItemStack result = ItemStack.copyItemStack(recipe.getResult());
        var outputs = pattern.getCondensedOutputs();
        if (result == null || result.stackSize <= 0
            || outputs == null
            || outputs.length != 1
            || outputs[0] == null
            || !outputs[0].isItem()) return null;
        long count = (long) result.stackSize * input.stackSize;
        if (count > Math.min(64, result.getMaxStackSize()) || count != outputs[0].getStackSize()
            || !TilePackagedProvider.sameItem(result, outputs[0].getItemStack())
            || TilePackagedProvider.sameItem(input, result)) return null;
        result.stackSize = (int) count;
        return result;
    }

    @Override
    public IInventory output(TileEntity target) {
        return target instanceof TEAltar altar && !altar.isActive() ? altar : null;
    }

    @Override
    public boolean canRelease(TileEntity target, ItemStack expected) {
        // A starved or cooling altar is still a live job. Require the owner to empty it before recovery.
        return !(target instanceof TEAltar altar) || altar.getStackInSlot(0) == null;
    }

    @Override
    public boolean interrupt(TilePackagedProvider provider, TileEntity target, ItemStack expected) {
        if (!canRelease(target, expected)) return false;
        collected(target);
        if (target instanceof TEAltar altar) altar.setActive();
        return true;
    }

    @Override
    public void collected(TileEntity target) {
        if (target instanceof BloodAltarAccess access) access.gtnotgood$holdResult(null);
    }
}
