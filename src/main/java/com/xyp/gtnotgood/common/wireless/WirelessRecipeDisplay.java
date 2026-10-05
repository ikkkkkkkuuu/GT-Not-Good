package com.xyp.gtnotgood.common.wireless;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** Read-only display snapshots. Native output and EU fields remain owned by recipe execution. */
public final class WirelessRecipeDisplay {

    private WirelessRecipeDisplay() {}

    static List<NBTTagCompound> rows(List<WirelessWork> tasks, int limit) {
        List<NBTTagCompound> rows = new ArrayList<>();
        for (WirelessWork task : tasks) {
            WirelessOutputs outputs = task.outputs;
            for (int i = 0; i < outputs.items.length && rows.size() < limit; i++) {
                if (outputs.items[i] == null || outputs.itemAmounts[i].signum() <= 0) continue;
                NBTTagCompound row = row(task, outputs.itemAmounts[i]);
                row.setTag("item", outputs.items[i].writeToNBT(new NBTTagCompound()));
                rows.add(row);
            }
            for (int i = 0; i < outputs.fluids.length && rows.size() < limit; i++) {
                if (outputs.fluids[i] == null || outputs.fluidAmounts[i].signum() <= 0) continue;
                NBTTagCompound row = row(task, outputs.fluidAmounts[i]);
                row.setTag("fluid", outputs.fluids[i].writeToNBT(new NBTTagCompound()));
                rows.add(row);
            }
        }
        return rows;
    }

    private static NBTTagCompound row(WirelessWork task, BigInteger amount) {
        NBTTagCompound row = new NBTTagCompound();
        row.setString("amount", amount.toString());
        row.setInteger("ticks", task.finished() ? 0 : task.duration);
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
            : new BigDecimal(row.getString("amount")).multiply(BigDecimal.valueOf(20))
                .divide(BigDecimal.valueOf(ticks), 2, RoundingMode.HALF_UP);
        return number(rate, compact) + (row.hasKey("fluid") ? " L/s" : "/s");
    }

    public static String number(Number number, boolean compact) {
        BigDecimal decimal = new BigDecimal(number.toString());
        if (compact && decimal.precision() - decimal.scale() > 18) return decimal.round(new MathContext(4))
            .toEngineeringString();
        return new DecimalFormat("#,##0.##").format(number);
    }
}
