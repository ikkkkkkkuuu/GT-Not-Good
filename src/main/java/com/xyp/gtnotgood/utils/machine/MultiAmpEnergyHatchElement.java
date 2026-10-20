package com.xyp.gtnotgood.utils.machine;

import java.util.Collections;
import java.util.List;

import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.IGTHatchAdder;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;

public enum MultiAmpEnergyHatchElement implements IHatchElement<MTEMultiBlockBase> {

    MultiAmpEnergy;

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return Collections.singletonList(MTEHatchEnergyMulti.class);
    }

    @Override
    public IGTHatchAdder<? super MTEMultiBlockBase> adder() {
        return HatchElement.MultiAmpEnergy.adder();
    }

    @Override
    public long count(MTEMultiBlockBase machine) {
        return HatchElement.MultiAmpEnergy.count(machine);
    }

    @Override
    public String getDisplayName() {
        return HatchElement.MultiAmpEnergy.getDisplayName();
    }

    @Override
    public String getDescriptionLangKey() {
        return HatchElement.MultiAmpEnergy.getDescriptionLangKey();
    }

    @Override
    public boolean matchesHatch(IMetaTileEntity hatch) {
        return hatch instanceof MTEHatchEnergyMulti multiAmp && multiAmp.getHatchType() == 1;
    }
}
