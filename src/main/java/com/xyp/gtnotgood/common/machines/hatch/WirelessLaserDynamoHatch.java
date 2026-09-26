package com.xyp.gtnotgood.common.machines.hatch;

import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessDynamoMulti;

/**
 * Adds missing voltage variants of GregTech's energized wireless dynamo.
 * Uses the native server-side network transfer and storage implementation without copying its source.
 *
 * @see MTEHatchWirelessDynamoMulti
 */
@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class WirelessLaserDynamoHatch extends MTEHatchWirelessDynamoMulti {

    public WirelessLaserDynamoHatch(int id, String name, int tier, int amperes) {
        super(id, name, "", tier, amperes);
    }

    private WirelessLaserDynamoHatch(String name, int tier, int amperes, String[] description,
        ITexture[][][] textures) {
        super(name, tier, amperes, description, textures);
    }

    @Override
    public String getLocalName() {
        // #tr gtng.wireless.laser.output.name
        // # %s Wireless Laser Dynamo Hatch (%s A)
        // # zh_CN %s 无线激光动力仓 (%s A)
        return StatCollector.translateToLocalFormatted(
            "gtng.wireless.laser.output.name",
            GTValues.VN[mTier],
            NumberFormatUtil.formatNumber(maxAmperes));
    }

    /** Retains the custom family name when GregTech creates a placed tile from the registered prototype. */
    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new WirelessLaserDynamoHatch(mName, mTier, maxAmperes, mDescriptionArray, mTextures);
    }
}
