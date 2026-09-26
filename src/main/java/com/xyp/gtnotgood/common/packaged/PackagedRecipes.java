// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.packaged;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

/** GTNG-specific acquisition recipes, retaining the user-selected AE interface and TC infusion-matrix prerequisites. */
public final class PackagedRecipes {

    private PackagedRecipes() {}

    /** Registers ordinary crafting upgrades without replacing any upstream or modpack recipe. */
    public static void register() {
        var definitions = AEApi.instance()
            .definitions();
        var materials = definitions.materials();
        GameRegistry.addRecipe(
            GTNGItemList.BasicPackagedCore.get(1),
            "IFI",
            "FEF",
            "IFI",
            'I',
            Items.iron_ingot,
            'F',
            materials.fluixCrystal()
                .maybeStack(1)
                .get(),
            'E',
            materials.engProcessor()
                .maybeStack(1)
                .get());
        GameRegistry.addShapelessRecipe(
            GTNGItemList.WirelessPackagedPatternProvider.get(1),
            definitions.blocks()
                .iface()
                .maybeStack(1)
                .get(),
            GTNGItemList.BasicPackagedCore.get(1),
            materials.wireless()
                .maybeStack(1)
                .get());
        GameRegistry.addRecipe(
            GTNGItemList.ItemWirelessConnector.get(1),
            " W",
            "F ",
            'W',
            materials.wireless()
                .maybeStack(1)
                .get(),
            'F',
            materials.fluixCrystal()
                .maybeStack(1)
                .get());
        if (ModList.Thaumcraft.isModLoaded()) registerInfusion();
        if (ModList.BloodMagic.isModLoaded()) registerBloodAltar();
        GameRegistry.addShapelessRecipe(
            GTNGItemList.AssemblyLineCore.get(1),
            GTNGItemList.BasicPackagedCore.get(1),
            gregtech.api.enums.ItemList.Machine_Multi_Assemblyline.get(1));
        GameRegistry.addShapelessRecipe(
            GTNGItemList.AdvancedAssemblyLineCore.get(1),
            GTNGItemList.BasicPackagedCore.get(1),
            ggfab.GGItemList.AdvAssLine.get(1));
    }

    /** Kept separate so an installation without TC never resolves a TC item while registering common recipes. */
    private static void registerInfusion() {
        GameRegistry.addShapelessRecipe(
            GTNGItemList.ThaumcraftCrucibleCore.get(1),
            GTNGItemList.BasicPackagedCore.get(1),
            new ItemStack(thaumcraft.common.config.ConfigBlocks.blockMetalDevice, 1, 0));
        GameRegistry.addShapelessRecipe(
            GTNGItemList.ArcaneWorkbenchCore.get(1),
            GTNGItemList.BasicPackagedCore.get(1),
            new ItemStack(thaumcraft.common.config.ConfigBlocks.blockTable, 1, 15));
        GameRegistry.addShapelessRecipe(
            GTNGItemList.ThaumcraftInfusionCore.get(1),
            GTNGItemList.BasicPackagedCore.get(1),
            new ItemStack(thaumcraft.common.config.ConfigBlocks.blockStoneDevice, 1, 2));
    }

    /** Requires the actual altar, preserving its existing progression gate. */
    private static void registerBloodAltar() {
        GameRegistry.addShapelessRecipe(
            GTNGItemList.BloodAltarCore.get(1),
            GTNGItemList.BasicPackagedCore.get(1),
            new ItemStack(WayofTime.alchemicalWizardry.ModBlocks.blockAltar));
    }
}
