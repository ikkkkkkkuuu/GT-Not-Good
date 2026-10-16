package com.xyp.gtnotgood.utils.text;

import net.minecraft.util.StatCollector;

import gregtech.api.enums.GTValues;
import gregtech.api.util.tooltip.TooltipHelper;

public final class EnergyHatchTooltip {

    private EnergyHatchTooltip() {}

    public static String[] format(String description, boolean dynamo, int tier, int amperes) {
        // #tr tooltip.gtnotgood.energy.throughput
        // # Throughput: %s EU/t
        // # zh_CN 传输功率：%s EU/t
        String throughput = StatCollector.translateToLocalFormatted("tooltip.gtnotgood.energy.throughput",
            TooltipHelper.euText(GTValues.V[tier] * amperes));
        String voltage;
        if (dynamo) {
            // #tr tooltip.gtnotgood.energy.voltage_out
            // # Output voltage: %s
            // # zh_CN 输出电压：%s
            voltage = StatCollector.translateToLocalFormatted("tooltip.gtnotgood.energy.voltage_out",
                TooltipHelper.voltageText(GTValues.V[tier]));
        } else {
            // #tr tooltip.gtnotgood.energy.voltage_in
            // # Input voltage: %s
            // # zh_CN 输入电压：%s
            voltage = StatCollector.translateToLocalFormatted("tooltip.gtnotgood.energy.voltage_in",
                TooltipHelper.voltageText(GTValues.V[tier]));
        }
        // #tr tooltip.gtnotgood.energy.amperage
        // # Amperage: %s A
        // # zh_CN 电流：%s A
        String current = StatCollector.translateToLocalFormatted("tooltip.gtnotgood.energy.amperage",
            TooltipHelper.ampText(amperes));
        return new String[] { description, throughput, voltage, current };
    }
}
