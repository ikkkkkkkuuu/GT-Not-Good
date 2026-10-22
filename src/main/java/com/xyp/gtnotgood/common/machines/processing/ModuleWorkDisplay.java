package com.xyp.gtnotgood.common.machines.processing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.modularui.api.NumberFormatMUI;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;

public final class ModuleWorkDisplay {

    private static final ThreadLocal<NumberFormatMUI> progressFormat = ThreadLocal.withInitial(NumberFormatMUI::new);

    private ModuleWorkDisplay() {}

    public static NBTTagCompound task(String core, CoreType type, int index, ModuleWork work, boolean paused,
        int outputLimit) {
        NBTTagCompound task = new NBTTagCompound();
        task.setString("core", core);
        task.setString("type", type.name());
        task.setInteger("index", index);
        task.setInteger("duration", work == null ? 0 : work.duration);
        task.setBoolean("paused", work != null && paused);
        task.setBoolean("finished", work != null && work.finished());
        NBTTagList outputTags = new NBTTagList();
        if (work != null) {
            List<NBTTagCompound> rows = new ArrayList<>();
            WirelessRecipeDisplay.appendRows(rows, work.outputs, paused || work.finished() ? 0 : work.duration,
                outputLimit);
            for (NBTTagCompound row : rows) outputTags.appendTag(row);
        }
        task.setTag("outputs", outputTags);
        return task;
    }

    public static String progress(int progress, int duration) {
        int total = Math.max(0, duration);
        int completed = Math.max(0, Math.min(progress, total));
        NumberFormatMUI format = progressFormat.get();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        String current = format.format(completed / 20D);
        String maximum = format.format(total / 20D);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        String percent = format.format(total == 0 ? 0 : completed * 100D / total);
        return StatCollector.translateToLocalFormatted("GT5U.gui.text.progress_recipe", current, maximum, percent);
    }
}
