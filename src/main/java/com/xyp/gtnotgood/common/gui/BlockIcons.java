package com.xyp.gtnotgood.common.gui;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.IIconContainer;

/** Registers the mod's block overlays with GregTech's deferred texture-loading list. */
public enum BlockIcons implements IIconContainer, Runnable {

    EddingtonCasing("stellar/sl_eddington"),
    OverlayFrontSingularityDataHub("iconsets/OVERLAY_FRONT_SINGULARITY_DATA_HUB"),
    OverlayFrontSingularityDataHubActive("iconsets/OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE"),
    OverlayFrontSingularityDataHubActiveGlow("iconsets/OVERLAY_FRONT_SINGULARITY_DATA_HUB_ACTIVE_GLOW"),
    OverlayFrontKerrCompressor("stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR"),
    OverlayFrontKerrCompressorGlow("stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_GLOW"),
    OverlayFrontKerrCompressorActive("stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE"),
    OverlayFrontKerrCompressorActiveGlow("stellar/iconsets/OVERLAY_FRONT_KERR_COMPRESSOR_ACTIVE_GLOW"),
    OverlayFrontItemVaultPortHatch("iconsets/OVERLAY_FRONT_ITEMVAULTPORTHATCH"),
    OverlayEnergyMonitor("iconsets/OVERLAY_ENERGY_MONITOR"),
    FragmentCollectorFront("basicmachines/fragment_collector/overlay_front"),
    FragmentCollectorFrontActive("basicmachines/fragment_collector/overlay_front_active"),
    FragmentCollectorSide("basicmachines/fragment_collector/overlay_side"),
    FragmentCollectorTop("basicmachines/fragment_collector/overlay_top"),
    FragmentCollectorBottom("basicmachines/fragment_collector/overlay_bottom"),
    FragmentCollectorBack("basicmachines/fragment_collector/overlay_back"),
    CrystalResonatorLV("basicmachines/crystal_resonator/lv_front"),
    CrystalResonatorMV("basicmachines/crystal_resonator/mv_front"),
    CrystalResonatorHV("basicmachines/crystal_resonator/hv_front"),
    CrystalResonatorEV("basicmachines/crystal_resonator/ev_front"),
    CrystalResonatorIV("basicmachines/crystal_resonator/iv_front"),
    CrystalResonatorLuV("basicmachines/crystal_resonator/luv_front"),
    CrystalResonatorZPM("basicmachines/crystal_resonator/zpm_front"),
    CrystalResonatorUV("basicmachines/crystal_resonator/uv_front"),
    CrystalResonatorUHV("basicmachines/crystal_resonator/uhv_front"),
    CrystalResonatorUEV("basicmachines/crystal_resonator/uev_front"),
    CrystalResonatorUIV("basicmachines/crystal_resonator/uiv_front"),
    CrystalResonatorUMV("basicmachines/crystal_resonator/uxv_front"),
    CrystalResonatorUXV("basicmachines/crystal_resonator/opv_front"),
    CrystalResonatorMAX("basicmachines/crystal_resonator/max_front");

    public static final String RES_PATH = ModList.GTNotGood.getResourcePath("");
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
        mIcon = GregTechAPI.sBlockIcons.registerIcon(ModList.GTNotGood.getResourcePath(iconName));
    }
}
