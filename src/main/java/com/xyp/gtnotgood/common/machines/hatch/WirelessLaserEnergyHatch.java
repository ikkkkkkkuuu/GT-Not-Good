package com.xyp.gtnotgood.common.machines.hatch;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

/**
 * Registers additional voltage tiers using GregTech's existing wireless laser implementation.
 * Network ownership, buffering, adjustable amperage, GUI and textures remain provided by GregTech.
 * Only the localized family name and the factory retaining this subtype are supplied here.
 *
 * @see MTEHatchWirelessMulti
 */
@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class WirelessLaserEnergyHatch extends MTEHatchWirelessMulti {

    public WirelessLaserEnergyHatch(int id, String name, int tier, int amperes) {
        super(id, name, "", tier, amperes);
    }

    private WirelessLaserEnergyHatch(String name, int tier, int amperes, String[] description,
        ITexture[][][] textures) {
        super(name, tier, amperes, description, textures);
    }

    @Override
    public String getLocalName() {
        // #tr gtng.wireless.laser.input.name
        // # %1$s %2$sA Wireless Laser Energy Hatch
        // # zh_CN %2$s安%1$s无线激光能源仓
        return StatCollector.translateToLocalFormatted(
            "gtng.wireless.laser.input.name",
            GTValues.VN[mTier],
            NumberFormatUtil.formatNumber(maxAmperes));
    }

    /**
     * Keeps native energy details and screwdriver help; the loader supplies this variant's animated mod credit.
     * Upstream implementation attribution remains documented separately from the item's added-by line.
     */
    @Override
    public String[] getDescription() {
        return formatEnergyInfoDesc(
            null,
            StatCollector.translateToLocal("gt.blockmachines.hatch.screwdrivertooltip"),
            false,
            mTier,
            maxAmperes,
            "gt.blockmachines.energy_hatch.wireless");
    }

    /**
     * Creates a fresh tile with the registered ceiling; saved adjustable amperage is restored by native NBT loading.
     */
    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new WirelessLaserEnergyHatch(mName, mTier, maxAmperes, mDescriptionArray, mTextures);
    }
}
