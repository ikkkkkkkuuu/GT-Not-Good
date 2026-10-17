package com.xyp.gtnotgood.common.machines.basic;

import static gregtech.api.enums.GTValues.V;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAYS_ENERGY_OUT;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BACK;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BACK_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BACK_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BACK_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BOTTOM;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BOTTOM_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BOTTOM_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_BOTTOM_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_FRONT;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_FRONT_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_FRONT_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_FRONT_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_SIDE;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_SIDE_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_SIDE_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_SIDE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_TOP;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_TOP_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_TOP_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.STEAM_TURBINE_TOP_GLOW;

import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.modularui.IAddGregtechLogo;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicGenerator;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTModHandler;

public class SteamTurbine extends MTEBasicGenerator implements IAddGregtechLogo {

    public SteamTurbine(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, new String[0]);
    }

    public SteamTurbine(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Override
    public boolean isOutputFacing(ForgeDirection side) {
        return side == getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new SteamTurbine(this.mName, this.mTier, this.mDescriptionArray, this.mTextures);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public String[] getDescription() {
        return new String[] {
            // #tr Tooltip_SteamTurbine_00
            // # Generates EU from steam without pollution
            // # zh_CN 使用蒸汽发电，无污染
            StatCollector.translateToLocal("Tooltip_SteamTurbine_00"),
            // #tr Tooltip_SteamTurbine_01
            // # Steam consumption: 1 L per 10 EU
            // # zh_CN 蒸汽消耗：每 10 EU 消耗 1 L 蒸汽
            StatCollector.translateToLocal("Tooltip_SteamTurbine_01"),
            // #tr Tooltip_SteamTurbine_02
            // # Fuel efficiency: %s%%
            // # zh_CN 燃料效率：%s%%
            StatCollector.translateToLocalFormatted("Tooltip_SteamTurbine_02", getEfficiency()),
            // #tr Tooltip_SteamTurbine_03
            // # Steam capacity: %s L
            // # zh_CN 蒸汽容量：%s L
            StatCollector.translateToLocalFormatted("Tooltip_SteamTurbine_03",
                NumberFormatUtil.formatNumber(getCapacity())),
            // #tr tooltip.steam_turbine.output
            // # Outputs up to %s EU/t at %s A through the front face
            // # zh_CN 从正面输出，最高 %s EU/t，%s A
            StatCollector.translateToLocalFormatted("tooltip.steam_turbine.output",
                NumberFormatUtil.formatNumber(V[mTier] * maxAmperesOut()), maxAmperesOut()) };
    }

    @Override
    public int getCapacity() {
        // Preserve the existing tanks when increasing fuel efficiency.
        long legacySteamCapacity = V[mTier] * 2560L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(640000L * mTier, legacySteamCapacity));
    }

    @Override
    public int getEfficiency() {
        return 2000;
    }

    @Override
    public int consumedFluidPerOperation(FluidStack aLiquid) {
        return 1;
    }

    @Override
    public long getFuelValue(FluidStack aLiquid, boolean aLong) {
        return getFuelValue(aLiquid);
    }

    @Override
    public int getFuelValue(FluidStack aLiquid) {
        if (aLiquid == null) return 0;
        return GTModHandler.isAnySteam(aLiquid) ? getEfficiency() / 200 : 0;
    }

    @Override
    public ITexture[] getFront(byte aColor) {
        return new ITexture[] { super.getFront(aColor)[0],
            TextureFactory.of(TextureFactory.of(STEAM_TURBINE_FRONT),
                TextureFactory.builder().addIcon(STEAM_TURBINE_FRONT_GLOW).glow().build()),
            OVERLAYS_ENERGY_OUT[this.mTier + 1] };
    }

    @Override
    public ITexture[] getBack(byte aColor) {
        return new ITexture[] { super.getBack(aColor)[0], TextureFactory.of(TextureFactory.of(STEAM_TURBINE_BACK),
            TextureFactory.builder().addIcon(STEAM_TURBINE_BACK_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getBottom(byte aColor) {
        return new ITexture[] { super.getBottom(aColor)[0], TextureFactory.of(TextureFactory.of(STEAM_TURBINE_BOTTOM),
            TextureFactory.builder().addIcon(STEAM_TURBINE_BOTTOM_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getTop(byte aColor) {
        return new ITexture[] { super.getTop(aColor)[0], TextureFactory.of(TextureFactory.of(STEAM_TURBINE_TOP),
            TextureFactory.builder().addIcon(STEAM_TURBINE_TOP_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getSides(byte aColor) {
        return new ITexture[] { super.getSides(aColor)[0], TextureFactory.of(TextureFactory.of(STEAM_TURBINE_SIDE),
            TextureFactory.builder().addIcon(STEAM_TURBINE_SIDE_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getFrontActive(byte aColor) {
        return new ITexture[] { super.getFrontActive(aColor)[0],
            TextureFactory.of(TextureFactory.of(STEAM_TURBINE_FRONT_ACTIVE),
                TextureFactory.builder().addIcon(STEAM_TURBINE_FRONT_ACTIVE_GLOW).glow().build()),
            OVERLAYS_ENERGY_OUT[this.mTier + 1] };
    }

    @Override
    public ITexture[] getBackActive(byte aColor) {
        return new ITexture[] { super.getBackActive(aColor)[0],
            TextureFactory.of(TextureFactory.of(STEAM_TURBINE_BACK_ACTIVE),
                TextureFactory.builder().addIcon(STEAM_TURBINE_BACK_ACTIVE_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getBottomActive(byte aColor) {
        return new ITexture[] { super.getBottomActive(aColor)[0],
            TextureFactory.of(TextureFactory.of(STEAM_TURBINE_BOTTOM_ACTIVE),
                TextureFactory.builder().addIcon(STEAM_TURBINE_BOTTOM_ACTIVE_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getTopActive(byte aColor) {
        return new ITexture[] { super.getTopActive(aColor)[0],
            TextureFactory.of(TextureFactory.of(STEAM_TURBINE_TOP_ACTIVE),
                TextureFactory.builder().addIcon(STEAM_TURBINE_TOP_ACTIVE_GLOW).glow().build()) };
    }

    @Override
    public ITexture[] getSidesActive(byte aColor) {
        return new ITexture[] { super.getSidesActive(aColor)[0],
            TextureFactory.of(TextureFactory.of(STEAM_TURBINE_SIDE_ACTIVE),
                TextureFactory.builder().addIcon(STEAM_TURBINE_SIDE_ACTIVE_GLOW).glow().build()) };
    }

    @Override
    public int getPollution() {
        return 0;
    }

    @Override
    public long maxAmperesOut() {
        return 64;
    }

    @Override
    public long maxEUStore() {
        return Math.max(getEUVar(), V[mTier] * 1280L + getMinimumStoredEU());
    }
}
