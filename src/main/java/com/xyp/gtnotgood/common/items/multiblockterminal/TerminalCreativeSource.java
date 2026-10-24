package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;

public final class TerminalCreativeSource implements IItemSource {

    private final List<ItemStack> preferred = new ArrayList<>();
    private final boolean noHatches;

    public TerminalCreativeSource(BlocksToPlace candidates, boolean noHatches) {
        this.noHatches = noHatches;
        if (candidates != null && candidates.getStacks() != null) {
            for (ItemStack candidate : candidates.getStacks()) {
                if (candidate == null || candidate.getItem() == null) continue;
                ItemStack stack = candidate.copy();
                stack.stackSize = 1;
                preferred.add(stack);
            }
        }
    }

    @Override
    @Nonnull
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
        if (predicate == null) throw new IllegalArgumentException();
        Map<ItemStack, Integer> result = new ItemStackMap<>(true);
        if (count <= 0) return result;
        ItemStack candidate = takeOne(predicate, simulate);
        if (candidate != null) result.put(candidate, count);
        return result;
    }

    @Override
    public ItemStack takeOne(Predicate<ItemStack> predicate, boolean simulate) {
        if (predicate == null) throw new IllegalArgumentException();
        for (ItemStack candidate : preferred)
            if (allowed(candidate) && predicate.test(candidate)) return candidate.copy();
        for (ItemStack candidate : RegisteredMachines.stacks)
            if (allowed(candidate) && predicate.test(candidate)) return candidate.copy();
        return null;
    }

    @Override
    public boolean takeOne(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null || stack.stackSize != 1) throw new IllegalArgumentException();
        return allowed(stack);
    }

    @Override
    public boolean takeAll(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException();
        return stack.stackSize > 0 && allowed(stack);
    }

    private boolean allowed(ItemStack stack) {
        if (!noHatches || stack.getItem() != Item.getItemFromBlock(GregTechAPI.sBlockMachines)) return true;
        int id = stack.getItemDamage();
        return id < 0 || id >= GregTechAPI.METATILEENTITIES.length
            || !(GregTechAPI.METATILEENTITIES[id] instanceof MTEHatch);
    }

    private static final class RegisteredMachines {

        private static final List<ItemStack> stacks = collect();

        private static List<ItemStack> collect() {
            List<ItemStack> result = new ArrayList<>();
            for (IMetaTileEntity machine : GregTechAPI.METATILEENTITIES) {
                if (machine == null) continue;
                ItemStack stack = machine.getStackForm(1);
                if (stack != null && stack.getItem() != null) result.add(stack);
            }
            return List.copyOf(result);
        }
    }
}
