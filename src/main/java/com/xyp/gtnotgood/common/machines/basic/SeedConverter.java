package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;

public final class SeedConverter extends MTEBasicMachine {

    public SeedConverter(int id, String name, String localizedName) {
        super(id, name, localizedName, 1, 1, description(), 1, 1);
    }

    private SeedConverter(String name, String[] description, ITexture[][][] textures) {
        super(name, 1, 1, description, textures, 1, 1);
    }

    private static String[] description() {
        return new String[] {
            // #tr tooltip.seed_converter.conversion
            // # Converts registered alternative seeds into CropsNH seeds at a 1:1 ratio
            // # zh_CN 将已注册的替代种子按 1:1 转化为 CropsNH 种子
            StatCollector.translateToLocal("tooltip.seed_converter.conversion"),
            // #tr tooltip.seed_converter.processing
            // # Base duration: 20 ticks at 16 EU/t; follows global recipe speed settings
            // # zh_CN 基础耗时 20 tick，16 EU/t；遵循全局配方提速设置
            StatCollector.translateToLocal("tooltip.seed_converter.processing"),
            // #tr tooltip.seed_converter.stats
            // # Output seed stats follow the existing crop stat configuration
            // # zh_CN 输出种子的属性遵循现有作物属性配置
            StatCollector.translateToLocal("tooltip.seed_converter.stats") };
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new SeedConverter(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.SeedConversionRecipes;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEBasicMachineBaseGui<>(this, getUIProperties()).useGregTechLogo(true).build(data, syncManager,
            settings);
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public ITexture[] getFrontFacingActive(byte color) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1],
            TextureFactory.of(Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_EXTRACTOR_ACTIVE) };
    }

    @Override
    public ITexture[] getFrontFacingInactive(byte color) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1],
            TextureFactory.of(Textures.BlockIcons.OVERLAY_FRONT_INDUSTRIAL_EXTRACTOR) };
    }
}
