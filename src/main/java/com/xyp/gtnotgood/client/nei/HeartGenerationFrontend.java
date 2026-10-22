package com.xyp.gtnotgood.client.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.machines.processing.HeartGenerationRules;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.api.util.GTUtility;
import gregtech.nei.RecipeDisplayInfo;

public final class HeartGenerationFrontend extends RecipeMapFrontend {

    public HeartGenerationFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    public int getDescriptionWidth() {
        return 160;
    }

    @SideOnly(Side.CLIENT)
    public List<String> getGenerationDescriptionLines() {
        List<String> lines = new ArrayList<>();
        // #tr gtng.heart.recipe.energy
        // # Energy/fuel: ~%s EU
        // # zh_CN 每组发电：约 %s EU
        lines.add(StatCollector.translateToLocalFormatted("gtng.heart.recipe.energy",
            GTUtility.scientificFormat(HeartGenerationRules.FUEL_ENERGY)));
        // #tr gtng.heart.recipe.maintenance
        // # Base upkeep: 64A LuV
        // # zh_CN 基础维持耗电：64A LuV
        lines.add(StatCollector.translateToLocal("gtng.heart.recipe.maintenance"));
        lines.add(NumberFormatUtil.formatNumber(HeartGenerationRules.MAINTENANCE_EUT) + " EU/t");
        // #tr gtng.heart.recipe.release
        // # Base release time:
        // # zh_CN 基础释放时间：
        lines.add(StatCollector.translateToLocal("gtng.heart.recipe.release"));
        // #tr gtng.heart.recipe.normal_advanced
        // # Normal / advanced: %s / %s s
        // # zh_CN 普通 / 高级：%s / %s 秒
        lines.add(StatCollector.translateToLocalFormatted("gtng.heart.recipe.normal_advanced",
            HeartGenerationRules.duration(CoreType.Normal, 1, 1) / 20,
            HeartGenerationRules.duration(CoreType.Advanced, 1, 1) / 20));
        // #tr gtng.heart.recipe.perfect
        // # Perfect: %s tick
        // # zh_CN 完美：%s tick
        lines.add(StatCollector.translateToLocalFormatted("gtng.heart.recipe.perfect",
            HeartGenerationRules.duration(CoreType.Perfect, 1, 1)));
        // #tr gtng.heart.recipe.modules
        // # Speed/OC shorten release
        // # zh_CN 加速与超频缩短释放时间
        lines.add(StatCollector.translateToLocal("gtng.heart.recipe.modules"));
        // #tr gtng.heart.recipe.conservation
        // # Fuel energy stays constant
        // # zh_CN 每组燃料总发电量守恒
        lines.add(StatCollector.translateToLocal("gtng.heart.recipe.conservation"));
        List<String> wrapped = new ArrayList<>();
        for (String line : lines) wrapped
            .addAll(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(line, getDescriptionWidth()));
        return wrapped;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawDescription(RecipeDisplayInfo info) {
        info.drawTextMultipleLines(getGenerationDescriptionLines());
        drawMetadataInfo(info);
        drawRecipeOwnerInfo(info);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void drawRecipeOwnerInfo(RecipeDisplayInfo info) {
        if (info.recipe.owners == null) return;
        for (int index = 0; index < info.recipe.owners.size(); index++) {
            String key = info.recipe.owners.size() == 1 ? "GT5U.recipes.recipe_by"
                : index == 0 ? "GT5U.recipes.recipe_by.original" : "GT5U.recipes.recipe_by.modified";
            String text = EnumChatFormatting.ITALIC
                + StatCollector.translateToLocalFormatted(key, info.recipe.owners.get(index).getName());
            info.drawTextMultipleLines(
                Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(text, getDescriptionWidth()));
        }
    }
}
