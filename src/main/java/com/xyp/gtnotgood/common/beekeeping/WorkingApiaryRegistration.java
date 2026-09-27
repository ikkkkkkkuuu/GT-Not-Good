package com.xyp.gtnotgood.common.beekeeping;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import forestry.apiculture.blocks.BlockApicultureType;
import forestry.core.items.ItemBlockForestry;
import forestry.plugins.PluginApiculture;

/** Keeps optional Forestry implementation types behind the mod-loaded registration guard. */
public final class WorkingApiaryRegistration {

    private WorkingApiaryRegistration() {}

    public static void register() {
        BlockWorkingApiary block = new BlockWorkingApiary();
        GameRegistry.registerBlock(block, ItemBlockForestry.class, "working_apiary");
        block.init();
        GTNGItemList.WorkingApiary.set(new ItemStack(block));
    }

    /** Converts an existing apiary, preserving the pack's original acquisition requirements. */
    public static void registerRecipe() {
        if (PluginApiculture.blocks == null || PluginApiculture.blocks.apiculture == null) return;
        GameRegistry.addShapelessRecipe(
            GTNGItemList.WorkingApiary.get(1),
            PluginApiculture.blocks.apiculture.get(BlockApicultureType.APIARY));
    }
}
