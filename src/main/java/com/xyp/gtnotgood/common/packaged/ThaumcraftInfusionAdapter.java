// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

/**
 * Real TC4 infusion: plans into empty pedestals and starts the normal matrix. TC4 controls consumption timing;
 * the selected source supplies each requested unit either from the altar surroundings or the Provider's AE network.
 * Only ordinary ItemStack-output infusion recipes are accepted; enchantments and dynamic NBT recipes are not
 * advertised as supported. The placing player's research is checked by TC4's recipe matcher.
 */
public final class ThaumcraftInfusionAdapter implements PackagedCoreRegistry.Adapter {

    @Override
    public boolean accepts(TileEntity target) {
        return target instanceof TileInfusionMatrix;
    }

    @Override
    public ItemStack dispatch(TilePackagedProvider provider, PackagedTarget target, ICraftingPatternDetails pattern,
        InventoryCrafting ingredients) {
        provider.altarStatus = AltarStatus.UNLOADED;
        if (!(target.resolve(provider.getWorldObj()) instanceof TileInfusionMatrix matrix)) return null;
        provider.altarStatus = matrix.crafting ? AltarStatus.RUNNING : AltarStatus.INACTIVE;
        if (!matrix.active || matrix.crafting) return null;
        World world = matrix.getWorldObj();
        EntityPlayer owner = provider.getOwnerPlayer();
        provider.altarStatus = AltarStatus.OWNER;
        if (owner == null || owner.worldObj != world || !world.canMineBlock(owner, target.x, target.y, target.z))
            return null;
        // TC4 scans +/-12 during craftingStart. Require these chunks before invoking its world lookups.
        provider.altarStatus = AltarStatus.UNLOADED;
        for (int cx = (target.x - 12) >> 4; cx <= (target.x + 12) >> 4; cx++) {
            for (int cz = (target.z - 12) >> 4; cz <= (target.z + 12) >> 4; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) return null;
            }
        }
        provider.altarStatus = AltarStatus.OCCUPIED;
        if (!matrix.validLocation()
            || !(world.getTileEntity(target.x, target.y - 2, target.z) instanceof TilePedestal center)
            || center.getStackInSlot(0) != null) return null;
        // Revalidate immediately before transfer: automation can add pedestals without a Forge placement event.
        // Reusing a stale topology here could let TC select a different recipe. Provider backoff bounds scans.
        List<TilePedestal> pedestals = findPedestals(world, target);
        for (TilePedestal pedestal : pedestals) {
            if (pedestal.getStackInSlot(0) != null
                || !world.canMineBlock(owner, pedestal.xCoord, pedestal.yCoord, pedestal.zCoord)) return null;
        }
        if (!world.canMineBlock(owner, center.xCoord, center.yCoord, center.zCoord)) return null;
        provider.altarStatus = AltarStatus.RECIPE;
        ArrayList<ItemStack> units = new ArrayList<>();
        for (int slot = 0; slot < ingredients.getSizeInventory(); slot++) {
            ItemStack stack = ingredients.getStackInSlot(slot);
            if (stack == null) continue;
            if (stack.stackSize <= 0 || stack.stackSize > pedestals.size() + 1 - units.size()) return null;
            for (int i = 0; i < stack.stackSize; i++) {
                ItemStack unit = stack.copy();
                unit.stackSize = 1;
                units.add(unit);
            }
        }
        if (units.size() < 2 || units.size() - 1 > pedestals.size()) return null;
        var outputs = pattern.getCondensedOutputs();
        if (outputs == null || outputs.length != 1 || outputs[0] == null) return null;
        ItemStack expected = outputs[0].getItemStack();
        if (expected == null || outputs[0].getStackSize() > expected.getMaxStackSize()) return null;
        for (int centerIndex = 0; centerIndex < units.size(); centerIndex++) {
            ItemStack input = units.get(centerIndex);
            ArrayList<ItemStack> components = new ArrayList<>(units);
            components.remove(centerIndex);
            InfusionRecipe recipe = ThaumcraftCraftingManager.findMatchingInfusionRecipe(components, input, owner);
            if (recipe == null || !(recipe.getRecipeOutput(input) instanceof ItemStack result)
                || !TilePackagedProvider.sameItem(result, expected)
                || result.stackSize != expected.stackSize) continue;
            center.setInventorySlotContents(0, input.copy());
            for (int i = 0; i < components.size(); i++) pedestals.get(i)
                .setInventorySlotContents(
                    0,
                    components.get(i)
                        .copy());
            matrix.craftingStart(owner);
            if (!matrix.crafting) {
                center.setInventorySlotContents(0, null);
                for (int i = 0; i < components.size(); i++) pedestals.get(i)
                    .setInventorySlotContents(0, null);
                return null;
            }
            DirectEssentiaSupply.attach(matrix, provider);
            center.markDirty();
            world.markBlockForUpdate(center.xCoord, center.yCoord, center.zCoord);
            for (int i = 0; i < components.size(); i++) {
                TilePedestal pedestal = pedestals.get(i);
                pedestal.markDirty();
                world.markBlockForUpdate(pedestal.xCoord, pedestal.yCoord, pedestal.zCoord);
            }
            matrix.markDirty();
            return expected;
        }
        return null;
    }

    /** Matches TC4's first pedestal per X/Z column below the matrix, excluding the central column. */
    private static List<TilePedestal> findPedestals(World world, PackagedTarget target) {
        List<TilePedestal> result = new ArrayList<>();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                if (x == 0 && z == 0) continue;
                for (int down = 1; down <= 10; down++) {
                    if (target.y - down < 0) break;
                    if (world
                        .getTileEntity(target.x + x, target.y - down, target.z + z) instanceof TilePedestal pedestal) {
                        result.add(pedestal);
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public IInventory output(TileEntity target) {
        if (!(target instanceof TileInfusionMatrix matrix) || matrix.crafting) return null;
        TileEntity center = matrix.getWorldObj()
            .getTileEntity(matrix.xCoord, matrix.yCoord - 2, matrix.zCoord);
        return center instanceof TilePedestal pedestal ? pedestal : null;
    }

    @Override
    public boolean canRelease(TileEntity target, ItemStack expected) {
        if (!(target instanceof TileInfusionMatrix matrix)) return true;
        if (matrix.crafting) return false;
        IInventory output = output(target);
        return output == null || !TilePackagedProvider.sameItem(output.getStackInSlot(0), expected);
    }
}
