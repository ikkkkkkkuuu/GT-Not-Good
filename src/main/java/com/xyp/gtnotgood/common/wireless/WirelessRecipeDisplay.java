package com.xyp.gtnotgood.common.wireless;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.metatileentity.implementations.gui.ItemDisplayKey;

/** Read-only display snapshots. Native output and EU fields remain owned by recipe execution. */
public final class WirelessRecipeDisplay {

    private WirelessRecipeDisplay() {}

    static List<NBTTagCompound> rows(List<WirelessWork> tasks, int limit) {
        List<NBTTagCompound> rows = new ArrayList<>();
        for (WirelessWork task : tasks) {
            appendRows(rows, task.outputs, task.finished() ? 0 : task.duration, limit);
        }
        return rows;
    }

    public static void appendRows(List<NBTTagCompound> rows, WirelessOutputs outputs, int ticks, int limit) {
        if (rows.size() >= limit) return;
        Map<ItemDisplayKey, BigInteger> itemAmounts = new LinkedHashMap<>();
        for (int i = 0; i < outputs.items.length; i++) {
            if (outputs.items[i] == null || outputs.itemAmounts[i].signum() <= 0) continue;
            ItemStack item = outputs.items[i];
            itemAmounts.merge(new ItemDisplayKey(item.getItem(), item.getItemDamage(), item.getTagCompound()),
                outputs.itemAmounts[i], BigInteger::add);
        }
        for (Map.Entry<ItemDisplayKey, BigInteger> entry : itemAmounts.entrySet()) {
            if (rows.size() >= limit) return;
            ItemDisplayKey key = entry.getKey();
            ItemStack item = new ItemStack(key.item(), 1, key.damage());
            item.setTagCompound(key.nbt());
            NBTTagCompound row = row(ticks, entry.getValue());
            row.setTag("item", item.writeToNBT(new NBTTagCompound()).copy());
            rows.add(row);
        }
        Map<FluidStack, BigInteger> fluidAmounts = new LinkedHashMap<>();
        for (int i = 0; i < outputs.fluids.length; i++) {
            if (outputs.fluids[i] == null || outputs.fluidAmounts[i].signum() <= 0) continue;
            fluidAmounts.merge(outputs.fluids[i], outputs.fluidAmounts[i], BigInteger::add);
        }
        for (Map.Entry<FluidStack, BigInteger> entry : fluidAmounts.entrySet()) {
            if (rows.size() >= limit) return;
            NBTTagCompound row = row(ticks, entry.getValue());
            row.setTag("fluid", entry.getKey().writeToNBT(new NBTTagCompound()).copy());
            rows.add(row);
        }
    }

    private static NBTTagCompound row(int ticks, BigInteger amount) {
        NBTTagCompound row = new NBTTagCompound();
        row.setString("amount", amount.toString());
        row.setInteger("ticks", ticks);
        return row;
    }

    public static String name(NBTTagCompound row) {
        if (row.hasKey("item")) {
            ItemStack item = ItemStack.loadItemStackFromNBT(row.getCompoundTag("item"));
            return item == null ? "" : item.getDisplayName();
        }
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(row.getCompoundTag("fluid"));
        return fluid == null ? "" : fluid.getLocalizedName();
    }

    public static String amount(NBTTagCompound row, boolean compact) {
        BigInteger amount = new BigInteger(row.getString("amount"));
        return number(amount, compact) + (row.hasKey("fluid") ? " L" : "");
    }

    public static String rate(NBTTagCompound row, boolean compact) {
        int ticks = row.getInteger("ticks");
        BigDecimal rate = ticks <= 0 ? BigDecimal.ZERO
            : new BigDecimal(row.getString("amount")).multiply(BigDecimal.valueOf(20)).divide(BigDecimal.valueOf(ticks),
                2, RoundingMode.HALF_UP);
        return number(rate, compact) + (row.hasKey("fluid") ? " L/s" : "/s");
    }

    public static String number(Number number, boolean compact) {
        BigDecimal decimal = new BigDecimal(number.toString());
        if (compact && decimal.precision() - decimal.scale() > 18)
            return decimal.round(new MathContext(4)).toEngineeringString();
        return new DecimalFormat("#,##0.##").format(number);
    }
}
