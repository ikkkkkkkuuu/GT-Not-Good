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
        // # %s Wireless Laser Energy Hatch (%s A)
        // # zh_CN %s 无线激光能源仓 (%s A)
        return StatCollector.translateToLocalFormatted(
            "gtng.wireless.laser.input.name",
            GTValues.VN[mTier],
            NumberFormatUtil.formatNumber(maxAmperes));
    }

    /**
     * Creates a fresh tile with the registered ceiling; saved adjustable amperage is restored by native NBT loading.
     */
    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new WirelessLaserEnergyHatch(mName, mTier, maxAmperes, mDescriptionArray, mTextures);
    }
}
