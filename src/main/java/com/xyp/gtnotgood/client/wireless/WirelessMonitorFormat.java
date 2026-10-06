package com.xyp.gtnotgood.client.wireless;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;

import gregtech.api.enums.GTValues;

/** Formats exact balances and net power, including values beyond GregTech's highest voltage tier. */
public final class WirelessMonitorFormat {

    private WirelessMonitorFormat() {}

    public static String number(BigDecimal value, boolean scientific, boolean fractional) {
        BigDecimal absolute = value.abs();
        if ((scientific && absolute.compareTo(BigDecimal.valueOf(1000)) >= 0)
            || absolute.precision() - absolute.scale() > 18) {
            BigDecimal rounded = absolute.round(new MathContext(3, RoundingMode.HALF_UP));
            int exponent = rounded.precision() - rounded.scale() - 1;
            return rounded.movePointLeft(exponent)
                .stripTrailingZeros()
                .toPlainString() + "×10^"
                + exponent;
        }
        if (fractional && absolute.signum() > 0 && absolute.compareTo(new BigDecimal("0.01")) < 0) return "<0.01";
        DecimalFormat format = new DecimalFormat(
            fractional ? "#,##0.00" : "#,##0",
            DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(absolute);
    }

    public static String rate(BigDecimal value, boolean scientific) {
        EnumChatFormatting color = value.signum() > 0 ? EnumChatFormatting.GREEN
            : value.signum() < 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY;
        String sign = value.signum() > 0 ? "+" : value.signum() < 0 ? "-" : "";
        BigDecimal absolute = value.abs();
        int tier = 0;
        // GregTech's final voltage entry is an error sentinel, not a displayable tier.
        while (tier + 2 < GTValues.V.length && absolute.compareTo(BigDecimal.valueOf(GTValues.V[tier + 1])) >= 0) {
            tier++;
        }
        BigDecimal amps = absolute.divide(BigDecimal.valueOf(GTValues.V[tier]), 6, RoundingMode.HALF_UP);
        DecimalFormat ampFormat = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
        String amperage = amps.precision() - amps.scale() > 6 ? number(amps, true, true) : ampFormat.format(amps);
        return color + sign
            + number(value, scientific, true)
            + EnumChatFormatting.AQUA
            + " EU/t ("
            + GTValues.TIER_COLORS[tier]
            + amperage
            + "A "
            + GTValues.VN[tier]
            + EnumChatFormatting.AQUA
            + ")";
    }
}
