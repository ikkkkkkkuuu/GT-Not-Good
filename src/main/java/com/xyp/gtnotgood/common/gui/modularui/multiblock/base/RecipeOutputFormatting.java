package com.xyp.gtnotgood.common.gui.modularui.multiblock.base;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;

import gregtech.api.util.GTUtility;

public final class RecipeOutputFormatting {

    private static final BigInteger million = BigInteger.valueOf(1_000_000);
    private static final int[] ticksPerUnit = { 1, 20, 1200, 72_000, 1_728_000 };
    private static final String[] rateKeys = { "GT5U.gui.text.per_tick", "GT5U.gui.text.per_second",
        "GT5U.gui.text.per_minute", "GT5U.gui.text.per_hour", "GT5U.gui.text.per_day" };

    private RecipeOutputFormatting() {}

    public static String amountLine(NBTTagCompound row) {
        BigInteger amount = amount(row);
        String key = row.hasKey("fluid") ? "GT5U.gui.text.fluid_amount_display" : "GT5U.gui.text.item_amount_display";
        return StatCollector.translateToLocalFormatted(key, "", shortened(amount))
            + shortRate(amount, row.getInteger("ticks"));
    }

    public static String tooltip(NBTTagCompound row) {
        BigInteger amount = amount(row);
        int ticks = row.getInteger("ticks");
        BigInteger denominator = BigInteger.valueOf(Math.max(1, ticks));
        BigInteger rateAmount = ticks > 0 ? amount : BigInteger.ZERO;
        BigInteger perSecond = rateAmount.multiply(BigInteger.valueOf(20));
        String unit = row.hasKey("fluid") ? "L" : "";
        StringBuilder tooltip = new StringBuilder().append(EnumChatFormatting.AQUA)
            .append(WirelessRecipeDisplay.name(row)).append('\n').append(EnumChatFormatting.RESET);
        appendLine(tooltip, "GT5U.gui.text.amount", NumberFormatUtil.formatNumber(amount), unit, null);
        for (int i = 0; i < ticksPerUnit.length; i++) {
            BigInteger numerator = rateAmount.multiply(BigInteger.valueOf(ticksPerUnit[i]));
            BigInteger thresholdRate = i == 0 ? perSecond : numerator;
            String abbreviation = thresholdRate.compareTo(denominator.multiply(million)) > 0
                ? shortened(numerator.divide(denominator))
                : null;
            tooltip.append('\n');
            appendLine(tooltip, rateKeys[i], NumberFormatUtil.formatNumber(roundedRate(numerator, denominator)), unit,
                abbreviation);
        }
        return tooltip.toString();
    }

    private static BigInteger amount(NBTTagCompound row) {
        return new BigInteger(row.getString("amount")).max(BigInteger.ZERO);
    }

    private static String shortRate(BigInteger amount, int ticks) {
        String rate;
        if (ticks <= 0 || amount.signum() == 0) {
            rate = StatCollector.translateToLocalFormatted("GT5U.gui.text.rate_short", "0");
        } else {
            BigInteger perSecond = amount.multiply(BigInteger.valueOf(20));
            BigInteger denominator = BigInteger.valueOf(ticks);
            if (perSecond.compareTo(denominator) <= 0) {
                BigDecimal seconds = new BigDecimal(denominator).divide(new BigDecimal(perSecond), 2,
                    RoundingMode.HALF_EVEN);
                rate = StatCollector.translateToLocalFormatted("GT5U.gui.text.rate_short_slow",
                    new DecimalFormat("0.00").format(seconds));
            } else {
                rate = StatCollector.translateToLocalFormatted("GT5U.gui.text.rate_short",
                    shortened(perSecond.divide(denominator)));
            }
        }
        return " " + rate;
    }

    private static Number roundedRate(BigInteger numerator, BigInteger denominator) {
        if (numerator.compareTo(denominator.multiply(BigInteger.TEN)) >= 0) return numerator.divide(denominator);
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
    }

    private static void appendLine(StringBuilder tooltip, String key, String number, String unit, String abbreviation) {
        tooltip.append(StatCollector.translateToLocal(key)).append(' ').append(EnumChatFormatting.GOLD).append(number)
            .append(unit);
        if (abbreviation != null) tooltip.append(' ')
            .append(StatCollector.translateToLocalFormatted("GT5U.gui.text.rate_large_suffix", abbreviation));
        tooltip.append(EnumChatFormatting.RESET);
    }

    private static String shortened(BigInteger amount) {
        if (amount.bitLength() < 64) return GTUtility.formatShortenedLong(amount.longValueExact());
        int exponent = (amount.toString().length() - 1) / 3;
        if (exponent > 6) return new BigDecimal(amount).round(new MathContext(4)).toEngineeringString();
        BigInteger divisor = BigInteger.TEN.pow(exponent * 3);
        BigInteger[] parts = amount.divideAndRemainder(divisor);
        String number = parts[1].signum() == 0 ? parts[0].toString()
            : new DecimalFormat("0.0")
                .format(new BigDecimal(amount).divide(new BigDecimal(divisor), 1, RoundingMode.HALF_UP));
        return number + "kMGTPE".charAt(exponent - 1);
    }
}
