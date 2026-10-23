package com.xyp.gtnotgood.common.machines.basic;

import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;

public final class FragmentWorldCollector extends MTEBasicMachine {

    public FragmentWorldCollector(int id, String name, String localizedName) {
        super(id, name, localizedName, 0, 1, description(), 2, 12);
    }

    private FragmentWorldCollector(String name, String[] description, ITexture[][][] textures) {
        super(name, 0, 1, description, textures, 2, 12);
    }

    private static String[] description() {
        return new String[] {
            // #tr tooltip.fragment_world_collector.resources
            // # Collects resources selected by a dimension display block and circuit
            // # zh_CN 使用维度展示方块和电路选择采集资源
            StatCollector.translateToLocal("tooltip.fragment_world_collector.resources"),
            // #tr tooltip.fragment_world_collector.dimension
            // # The dimension block is not consumed; works in any dimension
            // # zh_CN 不消耗维度方块，可在任意维度运行
            StatCollector.translateToLocal("tooltip.fragment_world_collector.dimension"),
            // #tr tooltip.fragment_world_collector.drill
            // # Fluid collection requires a drill head with a chance of consumption
            // # zh_CN 流体采集需要钻头，钻头有概率消耗
            StatCollector.translateToLocal("tooltip.fragment_world_collector.drill"),
            // #tr tooltip.fragment_world_collector.power
            // # Uses 8 EU/t; processing time follows the selected recipe
            // # zh_CN 耗电 8 EU/t，耗时取决于所选配方
            StatCollector.translateToLocal("tooltip.fragment_world_collector.power") };
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new FragmentWorldCollector(mName, mDescriptionArray, mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.FragmentWorldCollectionRecipes;
    }

    @Override
    public boolean allowSelectCircuit() {
        return true;
    }

    @Override
    public int getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean canOutput(FluidStack output) {
        if (output == null) return true;
        FluidStack stored = getDrainableStack();
        return (stored == null || stored.isFluidEqual(output))
            && (long) (stored == null ? 0 : stored.amount) + output.amount <= getCapacity();
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new FragmentWorldCollectorGui(this, getUIProperties()).build(data, syncManager, settings);
    }

    private ITexture[] overlay(byte color, BlockIcons icon) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1], TextureFactory.of(icon) };
    }

    @Override
    public ITexture[] getFrontFacingActive(byte color) {
        return overlay(color, BlockIcons.FragmentCollectorFrontActive);
    }

    @Override
    public ITexture[] getFrontFacingInactive(byte color) {
        return overlay(color, BlockIcons.FragmentCollectorFront);
    }

    @Override
    public ITexture[] getSideFacingActive(byte color) {
        return overlay(color, BlockIcons.FragmentCollectorSide);
    }

    @Override
    public ITexture[] getSideFacingInactive(byte color) {
        return getSideFacingActive(color);
    }

    @Override
    public ITexture[] getTopFacingActive(byte color) {
        return overlay(color, BlockIcons.FragmentCollectorTop);
    }

    @Override
    public ITexture[] getTopFacingInactive(byte color) {
        return getTopFacingActive(color);
    }

    @Override
    public ITexture[] getBottomFacingActive(byte color) {
        return overlay(color, BlockIcons.FragmentCollectorBottom);
    }

    @Override
    public ITexture[] getBottomFacingInactive(byte color) {
        return getBottomFacingActive(color);
    }

    @Override
    public ITexture[] getSideFacingPipeActive(byte color) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[mTier][color + 1],
            TextureFactory.of(BlockIcons.FragmentCollectorBack),
            TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT) };
    }

    @Override
    public ITexture[] getSideFacingPipeInactive(byte color) {
        return getSideFacingPipeActive(color);
    }
}
