package com.xyp.gtnotgood.common.compat;

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.utils.MoldDataManager;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.util.GTUtility;

/** Shared mold catalog validation and zero-size recipe inputs, never exposed as real machine inventory slots. */
public final class VirtualMachineMolds {

    private VirtualMachineMolds() {}

    public static boolean supports(MTEBasicMachine machine) {
        return machine instanceof VirtualMoldMachine && machine.allowSelectCircuit()
            && !machine.isSteampowered()
            && machine.getRecipeMap() != null;
    }

    /** @return the shared catalog index, or -1 for empty/unsupported items */
    public static int indexOf(ItemStack stack) {
        if (stack == null) return -1;
        ItemStack[] molds = MoldDataManager.getMolds();
        for (int i = 0; i < molds.length; i++) {
            if (GTUtility.areStacksEqual(molds[i], stack, false)) return i;
        }
        return -1;
    }

    /** @return a copied, zero-size catalog mold or null; positive consumable stacks are never synthesized */
    public static ItemStack at(int index) {
        ItemStack[] molds = MoldDataManager.getMolds();
        if (index < 0 || index >= molds.length) return null;
        ItemStack copy = molds[index].copy();
        copy.stackSize = 0;
        return copy;
    }

    public static ItemStack get(MTEBasicMachine machine) {
        return supports(machine) ? ((VirtualMoldMachine) machine).gtng$getVirtualMold() : null;
    }

    /** Changes only the virtual configuration; callers enforce any batching/isolation policy before committing. */
    public static void set(MTEBasicMachine machine, ItemStack mold) {
        if (supports(machine)) ((VirtualMoldMachine) machine).gtng$setVirtualMold(mold);
    }

    /** Appends a disposable catalyst while preserving references to real consumable stacks for GT consumption. */
    public static ItemStack[] append(ItemStack[] inputs, ItemStack mold) {
        if (mold == null) return inputs;
        ItemStack[] result = Arrays.copyOf(inputs, inputs.length + 1);
        result[inputs.length] = mold.copy();
        result[inputs.length].stackSize = 0;
        return result;
    }
}
