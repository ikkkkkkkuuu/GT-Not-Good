// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.hatch.modular;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

public abstract class ModularHatch extends MTEHatch {

    private static final Map<String, IIconContainer> OVERLAYS = new HashMap<>();
    private final ModuleType moduleType;

    protected ModularHatch(int id, String name, int tier, ModuleType moduleType) {
        super(id, name, "", tier, 0, new String[0]);
        this.moduleType = moduleType;
    }

    protected ModularHatch(String name, int tier, ModuleType moduleType, ITexture[][][] textures) {
        super(name, tier, 0, new String[0], textures);
        this.moduleType = moduleType;
    }

    public final ModuleType getModuleType() {
        return moduleType;
    }

    public final void setWorking(boolean working) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide() && tile.isActive() != working) tile.setActive(working);
    }

    protected abstract String getOverlayName(boolean active);

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        String prefix = ModList.GTNotGood.getResourceLocation() + ":modular/";
        registerOverlay(prefix, getOverlayName(true));
        registerOverlay(prefix, getOverlayName(false));
        super.registerIcons(register);
    }

    @SideOnly(Side.CLIENT)
    private static void registerOverlay(String prefix, String name) {
        if (!OVERLAYS.containsKey(name)) OVERLAYS.put(name, Textures.BlockIcons.custom(prefix + name));
    }

    @Override
    public ITexture[] getTexturesActive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAYS.get(getOverlayName(true))) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture base) {
        return new ITexture[] { base, TextureFactory.of(OVERLAYS.get(getOverlayName(false))) };
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isValidSlot(int index) {
        return false;
    }

    @Override
    public boolean isLiquidInput(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack fluid) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack) {
        return false;
    }

    @Override
    public boolean willExplodeInRain() {
        return false;
    }
}
