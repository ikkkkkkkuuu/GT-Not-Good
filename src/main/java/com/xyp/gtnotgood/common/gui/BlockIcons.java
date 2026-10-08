package com.xyp.gtnotgood.common.gui;

import static com.xyp.gtnotgood.GTNotGood.RESOURCE_ROOT_ID;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.IIconContainer;

/** Registers the mod's block overlays with GregTech's deferred texture-loading list. */
public enum BlockIcons implements IIconContainer, Runnable {

    OverlayFrontSingularityDataHub("OVERLAY_FRONT_SINGULARITY_DATA_HUB"),
    OverlayFrontSingularityDataHubActive("OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE"),
    OverlayFrontSingularityDataHubActiveGlow("OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE_GLOW"),
    OverlayFrontItemVaultPortHatch("OVERLAY_FRONT_ITEMVAULTPORTHATCH"),
    OverlayEnergyMonitor("OVERLAY_ENERGY_MONITOR");

    public static final String RES_PATH = RESOURCE_ROOT_ID + ":";
    private final String iconName;
    private IIcon mIcon;

    BlockIcons(String iconName) {
        this.iconName = iconName;
        GregTechAPI.sGTBlockIconload.add(this);
    }

    /** Triggers enum construction during client setup, before GregTech loads textures. */
    public static void initialize() {}

    @Override
    public IIcon getIcon() {
        return mIcon;
    }

    @Override
    public IIcon getOverlayIcon() {
        return null;
    }

    @Override
    public ResourceLocation getTextureFile() {
        return TextureMap.locationBlocksTexture;
    }

    @Override
    public void run() {
        mIcon = GregTechAPI.sBlockIcons.registerIcon(RES_PATH + "iconsets/" + iconName);
    }
}
