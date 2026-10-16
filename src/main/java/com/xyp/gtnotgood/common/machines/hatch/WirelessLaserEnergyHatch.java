package com.xyp.gtnotgood.common.machines.hatch;

import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.ArrayUtils;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.utils.text.EnergyHatchTooltip;

import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

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
        return StatCollector.translateToLocalFormatted("gtng.wireless.laser.input.name", GTValues.VN[mTier],
            NumberFormatUtil.formatNumber(maxAmperes));
    }

    @Override
    public String[] getDescription() {
        // #tr gtng.wireless.laser.input.description
        // # Draws EU from the owner's GT wireless network to power a multiblock
        // # zh_CN 从仓主的 GT 无线电网提取 EU，为多方块机器供能
        String description = StatCollector.translateToLocal("gtng.wireless.laser.input.description");
        String[] details = EnergyHatchTooltip.format(description, false, mTier, maxAmperes);
        // #tr gtng.wireless.laser.input.capacity
        // # Internal energy capacity: %s EU
        // # zh_CN 内部能量容量：%s EU
        String capacity = StatCollector.translateToLocalFormatted("gtng.wireless.laser.input.capacity",
            NumberFormatUtil.formatNumber(maxEUStore()));
        // #tr gtng.wireless.laser.input.network
        // # Requires enough wireless EU for each transfer; does not accept cables
        // # zh_CN 无线电网须有足够 EU 完成每次传输；不接受线缆供电
        String network = StatCollector.translateToLocal("gtng.wireless.laser.input.network");
        // #tr gtng.wireless.laser.input.controls
        // # Right-click with a screwdriver to adjust amperage
        // # zh_CN 螺丝刀右键调整电流
        String controls = StatCollector.translateToLocal("gtng.wireless.laser.input.controls");
        return ArrayUtils.addAll(details, capacity, network, controls);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new WirelessLaserEnergyHatch(mName, mTier, maxAmperes, mDescriptionArray, mTextures);
    }
}
