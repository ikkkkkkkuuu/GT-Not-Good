package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/** Encodes installed production lines using GTNH 2.9 native item/fluid stacks, without legacy fluid packets. */
public final class FactoryPatternExport {

    private FactoryPatternExport() {}

    /** Uses the exact same preview model and ordering as the GUI, including the user's current parallel. */
    public static ItemStack create(FactoryGraph graph) {
        return create(FactoryPreview.describe(graph));
    }

    /** Shares validation with the UI so visible warnings and server export decisions cannot diverge. */
    public static ItemStack create(FactoryPreview.Snapshot snapshot) {
        if (snapshot.exportIssue() != null) throw new IllegalArgumentException("Unreliable production promise");
        if (snapshot.inputs.isEmpty() || snapshot.outputs.isEmpty())
            throw new IllegalArgumentException("Empty pattern boundary");
        List<FactoryPreview.Ingredient> all = new ArrayList<>(snapshot.inputs);
        all.addAll(snapshot.outputs);
        double[] rates = all.stream()
            .mapToDouble(entry -> entry.rate)
            .toArray();
        long[] counts = integerCounts(rates);
        List<IAEStack<?>> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            FactoryPreview.Ingredient ingredient = all.get(i);
            IAEStack<?> stack = ingredient.fluid == null ? AEItemStack.create(ingredient.item)
                : AEFluidStack.create(ingredient.fluid);
            stack.setStackSize(counts[i]);
            (i < snapshot.inputs.size() ? inputs : outputs).add(stack);
        }
        boolean fluid = hasFluids(inputs, outputs);
        // Legacy ordinary patterns read item quantities through an int; native ultimate patterns support long counts.
        if (!fluid) for (long count : counts)
            if (count > Integer.MAX_VALUE) throw new ArithmeticException("Ordinary pattern amount overflow");
        ItemStack pattern = (fluid ? AEApi.instance()
            .definitions()
            .items()
            .encodedUltimatePattern()
            : AEApi.instance()
                .definitions()
                .items()
                .encodedPattern()).maybeStack(1)
                    .orNull();
        if (pattern == null) throw new IllegalArgumentException("Pattern item unavailable");
        pattern.setTagCompound(encode(inputs, outputs, fluid));
        return pattern;
    }

    /** Preserves all preview ratios while scaling fractional counts by one common, minimal integer multiplier. */
    static long[] integerCounts(double[] rates) {
        java.math.BigDecimal[] decimals = new java.math.BigDecimal[rates.length];
        java.math.BigInteger multiplier = java.math.BigInteger.ONE;
        for (int i = 0; i < rates.length; i++) {
            if (!Double.isFinite(rates[i]) || rates[i] <= 0) throw new IllegalArgumentException("Invalid rate");
            decimals[i] = java.math.BigDecimal.valueOf(rates[i])
                .stripTrailingZeros();
            java.math.BigInteger denominator = java.math.BigInteger.TEN.pow(Math.max(0, decimals[i].scale()));
            denominator = denominator.divide(denominator.gcd(decimals[i].unscaledValue()));
            multiplier = multiplier.divide(multiplier.gcd(denominator))
                .multiply(denominator);
        }
        long[] counts = new long[rates.length];
        for (int i = 0; i < rates.length; i++) counts[i] = decimals[i].multiply(new java.math.BigDecimal(multiplier))
            .longValueExact();
        return counts;
    }

    /** Both input and output fluids require an ultimate pattern. */
    static boolean hasFluids(List<IAEStack<?>> inputs, List<IAEStack<?>> outputs) {
        return inputs.stream()
            .anyMatch(s -> s instanceof appeng.api.storage.data.IAEFluidStack)
            || outputs.stream()
                .anyMatch(s -> s instanceof appeng.api.storage.data.IAEFluidStack);
    }

    /** Ordinary processing patterns use legacy item NBT; ultimate patterns include each native stack type. */
    static NBTTagCompound encode(List<IAEStack<?>> inputs, List<IAEStack<?>> outputs, boolean ultimate) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("in", encodeStacks(inputs, ultimate));
        tag.setTag("out", encodeStacks(outputs, ultimate));
        tag.setBoolean("crafting", false);
        tag.setBoolean("substitute", false);
        tag.setBoolean("beSubstitute", false);
        return tag;
    }

    private static NBTTagList encodeStacks(List<IAEStack<?>> stacks, boolean ultimate) {
        NBTTagList list = new NBTTagList();
        for (IAEStack<?> stack : stacks) {
            if (stack == null || stack.getStackSize() <= 0)
                throw new IllegalArgumentException("Invalid pattern amount");
            NBTTagCompound tag = new NBTTagCompound();
            if (ultimate) stack.writeToNBTGeneric(tag);
            else stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }

}
