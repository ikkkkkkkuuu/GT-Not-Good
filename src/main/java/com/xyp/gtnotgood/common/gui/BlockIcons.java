package com.xyp.gtnotgood.common.gui;

import static com.xyp.gtnotgood.GTNotGood.RESOURCE_ROOT_ID;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.IIconContainer;

/** Registers the mod's block overlays with GregTech's deferred texture-loading list. */
public enum BlockIcons implements IIconContainer, Runnable {

    OVERLAY_FRONT_SINGULARITY_DATA_HUB,
    OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE,
    OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE_GLOW,
    OVERLAY_FRONT_ITEMVAULTPORTHATCH,
    OVERLAY_ENERGY_MONITOR;

    public static final String RES_PATH = RESOURCE_ROOT_ID + ":";
    private IIcon mIcon;

    BlockIcons() {
        GregTechAPI.sGTBlockIconload.add(this);
    }

    /**
     * Initializes the enum during client setup so every icon is queued before GregTech loads textures.
     * Calling this static method triggers the enum constructors exactly once; repeated calls do not add icons again.
     *
     * @see com.xyp.gtnotgood.ClientProxy#init(cpw.mods.fml.common.event.FMLInitializationEvent)
     */
    public static void initialize() {
        // Class initialization performs the registration; no additional work is required here.
    }

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
        mIcon = GregTechAPI.sBlockIcons.registerIcon(RES_PATH + "iconsets/" + this);
    }
}
