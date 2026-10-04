package com.xyp.gtnotgood.common.wireless;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.objects.XSTR;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.ParallelHelper;

/** Compact output ledger: stack objects describe types, while quantities never pass through int or long. */
public final class WirelessOutputs {

    final ItemStack[] items;
    final FluidStack[] fluids;
    final BigInteger[] itemAmounts;
    final BigInteger[] fluidAmounts;

    public WirelessOutputs(ItemStack[] items, FluidStack[] fluids) {
        this.items = items == null ? new ItemStack[0] : new ItemStack[items.length];
        this.fluids = fluids == null ? new FluidStack[0] : new FluidStack[fluids.length];
        itemAmounts = new BigInteger[this.items.length];
        fluidAmounts = new BigInteger[this.fluids.length];
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = items[i] == null ? null : items[i].copy();
            itemAmounts[i] = BigInteger.valueOf(items[i] == null ? 0 : Math.max(0, items[i].stackSize));
            if (this.items[i] != null) this.items[i].stackSize = 1;
        }
        for (int i = 0; i < this.fluids.length; i++) {
            this.fluids[i] = fluids[i] == null ? null : fluids[i].copy();
            fluidAmounts[i] = BigInteger.valueOf(fluids[i] == null ? 0 : Math.max(0, fluids[i].amount));
            if (this.fluids[i] != null) this.fluids[i].amount = 1;
        }
    }

    public static WirelessOutputs forRecipe(GTRecipe recipe) {
        return forRecipe(recipe, recipe.mOutputs.length, recipe.mFluidOutputs.length);
    }

    public static WirelessOutputs forRecipe(GTRecipe recipe, int itemLimit, int fluidLimit) {
        WirelessOutputs result = new WirelessOutputs(
            Arrays.copyOf(recipe.mOutputs, Math.min(itemLimit, recipe.mOutputs.length)),
            Arrays.copyOf(recipe.mFluidOutputs, Math.min(fluidLimit, recipe.mFluidOutputs.length)));
        Arrays.fill(result.itemAmounts, BigInteger.ZERO);
        Arrays.fill(result.fluidAmounts, BigInteger.ZERO);
        return result;
    }

    /** Uses native chance rolls per native input batch; multiplication and accumulation remain exact. */
    public void addRecipeBatch(GTRecipe recipe, BigInteger parallels) {
        for (int i = 0; i < items.length; i++) if (items[i] != null) {
            BigInteger rolls = roll(recipe.getOutputChance(i), parallels);
            itemAmounts[i] = itemAmounts[i].add(rolls.multiply(BigInteger.valueOf(recipe.mOutputs[i].stackSize)));
        }
        for (int i = 0; i < fluids.length; i++) if (fluids[i] != null) {
            BigInteger rolls = roll(recipe.getFluidOutputChance(i), parallels);
            fluidAmounts[i] = fluidAmounts[i].add(rolls.multiply(BigInteger.valueOf(recipe.mFluidOutputs[i].amount)));
        }
    }

    private static BigInteger roll(int chance, BigInteger count) {
        if (chance <= 0) return BigInteger.ZERO;
        // Certain outputs never go through floating point, including counts beyond long range.
        BigInteger guaranteed = count.multiply(BigInteger.valueOf(chance / 10000));
        int fraction = chance % 10000;
        if (fraction == 0) return guaranteed;
        if (count.bitLength() < 32) return guaranteed.add(
            BigInteger
                .valueOf(ParallelHelper.calculateIntegralChancedOutputMultiplier(fraction, count.intValueExact())));
        // Native GT also uses a normal approximation at large sample sizes. Keep the mean and variance in big
        // arithmetic so extremely large counts cannot become infinity or lose their low digits via a double cast.
        BigInteger mean = count.multiply(BigInteger.valueOf(fraction))
            .divide(BigInteger.valueOf(10000));
        BigInteger variance = count.multiply(BigInteger.valueOf((long) fraction * (10000 - fraction)))
            .divide(BigInteger.valueOf(100000000));
        BigInteger stdDev = sqrt(variance);
        BigInteger noise = new BigDecimal(stdDev).multiply(BigDecimal.valueOf(XSTR.XSTR_INSTANCE.nextGaussian()))
            .toBigInteger();
        return guaranteed.add(
            mean.add(noise)
                .max(BigInteger.ZERO)
                .min(count));
    }

    private static BigInteger sqrt(BigInteger value) {
        if (value.signum() == 0) return BigInteger.ZERO;
        BigInteger current = BigInteger.ONE.shiftLeft((value.bitLength() + 1) / 2);
        while (true) {
            BigInteger next = current.add(value.divide(current))
                .shiftRight(1);
            if (next.compareTo(current) >= 0) return current;
            current = next;
        }
    }

    private BigInteger largest() {
        BigInteger largest = BigInteger.ZERO;
        for (BigInteger n : itemAmounts) largest = largest.max(n);
        for (BigInteger n : fluidAmounts) largest = largest.max(n);
        return largest;
    }

    /**
     * Delivers a bounded slice using native transactions. A blocked fluid output cannot commit only the items.
     * Remaining amounts are reduced only after both output transactions commit, and persist in the task NBT.
     */
    public boolean flush(MTEMultiBlockBase machine) {
        BigInteger largest = largest();
        if (largest.signum() == 0) return true;
        int high = largest.min(BigInteger.valueOf(Integer.MAX_VALUE))
            .intValueExact();
        if (transfer(machine, largest, high, false)) {
            transfer(machine, largest, high, true);
            return largest().signum() == 0;
        }
        if (high == 1 || !transfer(machine, largest, 1, false)) return false;
        int low = 1;
        high--;
        while (low < high) {
            int middle = (int) (((long) low + high + 1) / 2);
            if (transfer(machine, largest, middle, false)) low = middle;
            else high = middle - 1;
        }
        if (low > 0) transfer(machine, largest, low, true);
        return largest().signum() == 0;
    }

    private static int slice(BigInteger amount, BigInteger largest, int units) {
        if (amount.signum() == 0) return 0;
        return amount.multiply(BigInteger.valueOf(units))
            .divide(largest)
            .max(BigInteger.ONE)
            .intValueExact();
    }

    private boolean transfer(MTEMultiBlockBase machine, BigInteger largest, int units, boolean commit) {
        ItemEjectionHelper itemHelper = new ItemEjectionHelper(machine.getOutputBusses(), machine.protectsExcessItem());
        FluidEjectionHelper fluidHelper = new FluidEjectionHelper(
            machine.getOutputHatches(),
            machine.protectsExcessFluid());
        int[] itemSlice = new int[items.length];
        int[] fluidSlice = new int[fluids.length];
        for (int i = 0; i < items.length; i++) {
            itemSlice[i] = slice(itemAmounts[i], largest, units);
            if (itemSlice[i] > 0
                && itemHelper.ejectItems(Collections.singletonList(items[i]), itemSlice[i]) != itemSlice[i])
                return false;
        }
        for (int i = 0; i < fluids.length; i++) {
            fluidSlice[i] = slice(fluidAmounts[i], largest, units);
            if (fluidSlice[i] > 0
                && fluidHelper.ejectFluids(Collections.singletonList(fluids[i]), fluidSlice[i]) != fluidSlice[i])
                return false;
        }
        if (commit) {
            itemHelper.commit();
            fluidHelper.commit();
            for (int i = 0; i < items.length; i++)
                itemAmounts[i] = itemAmounts[i].subtract(BigInteger.valueOf(itemSlice[i]));
            for (int i = 0; i < fluids.length; i++)
                fluidAmounts[i] = fluidAmounts[i].subtract(BigInteger.valueOf(fluidSlice[i]));
        }
        return true;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList itemTags = new NBTTagList();
        for (int i = 0; i < items.length; i++) if (items[i] != null && itemAmounts[i].signum() > 0) {
            NBTTagCompound entry = new NBTTagCompound();
            GTUtility.saveItem(entry, "stack", items[i]);
            entry.setString("amount", itemAmounts[i].toString());
            itemTags.appendTag(entry);
        }
        tag.setTag("items", itemTags);
        NBTTagList fluidTags = new NBTTagList();
        for (int i = 0; i < fluids.length; i++) if (fluids[i] != null && fluidAmounts[i].signum() > 0) {
            NBTTagCompound entry = fluids[i].writeToNBT(new NBTTagCompound());
            entry.setString("bigAmount", fluidAmounts[i].toString());
            fluidTags.appendTag(entry);
        }
        tag.setTag("fluids", fluidTags);
        return tag;
    }

    public static WirelessOutputs load(NBTTagCompound tag) {
        NBTTagList itemTags = tag.getTagList("items", 10), fluidTags = tag.getTagList("fluids", 10);
        ItemStack[] items = new ItemStack[itemTags.tagCount()];
        FluidStack[] fluids = new FluidStack[fluidTags.tagCount()];
        for (int i = 0; i < items.length; i++) items[i] = GTUtility.loadItem(itemTags.getCompoundTagAt(i), "stack");
        for (int i = 0; i < fluids.length; i++)
            fluids[i] = FluidStack.loadFluidStackFromNBT(fluidTags.getCompoundTagAt(i));
        WirelessOutputs result = new WirelessOutputs(items, fluids);
        for (int i = 0; i < items.length; i++) if (itemTags.getCompoundTagAt(i)
            .hasKey("amount", 8))
            result.itemAmounts[i] = new BigInteger(
                itemTags.getCompoundTagAt(i)
                    .getString("amount"));
        for (int i = 0; i < fluids.length; i++) if (fluidTags.getCompoundTagAt(i)
            .hasKey("bigAmount", 8))
            result.fluidAmounts[i] = new BigInteger(
                fluidTags.getCompoundTagAt(i)
                    .getString("bigAmount"));
        return result;
    }
}
