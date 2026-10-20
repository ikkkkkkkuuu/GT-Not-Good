package com.xyp.gtnotgood.mixins.late.gregtech;

import java.util.List;

import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtnotgood.config.MainConfig;
import com.xyp.gtnotgood.utils.machine.MultiAmpEnergyHatchElement;

import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.tileentities.machines.multi.MTEElectricBlastFurnace;

@Mixin(value = MTEElectricBlastFurnace.class, remap = false)
public abstract class ElectricBlastFurnaceMultiAmpMixin
    extends MTEExtendedPowerMultiBlockBase<MTEElectricBlastFurnace> {

    protected ElectricBlastFurnaceMultiAmpMixin(String name) {
        super(name);
    }

    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/HatchElementBuilder;atLeast([Lgregtech/api/interfaces/IHatchElement;)Lgregtech/api/util/HatchElementBuilder;",
            ordinal = 1),
        index = 0,
        require = 1,
        allow = 1)
    private static IHatchElement<?>[] gtnotgood$allowMultiAmpEnergy(IHatchElement<?>[] elements) {
        MainConfig.ensureLoaded();
        if (MainConfig.electricBlastFurnaceMultiAmpEnergy) {
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] == HatchElement.Energy)
                    elements[i] = HatchElement.Energy.or(MultiAmpEnergyHatchElement.MultiAmpEnergy);
            }
        }
        return elements;
    }

    @Redirect(
        method = "checkMachine",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/common/tileentities/machines/multi/MTEElectricBlastFurnace;checkHasEnergyHatch(Ljava/util/List;)V"),
        require = 1,
        allow = 1)
    private void gtnotgood$checkEnergy(MTEElectricBlastFurnace machine, List<StructureError> errors) {
        if (MainConfig.electricBlastFurnaceMultiAmpEnergy) checkHasAnyEnergy(errors);
        else checkHasEnergyHatch(errors);
    }

    @Redirect(
        method = "createTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/MultiblockTooltipBuilder;toolTipFinisher([Ljava/lang/String;)Lgregtech/api/util/MultiblockTooltipBuilder;"),
        require = 1,
        allow = 1)
    private MultiblockTooltipBuilder gtnotgood$describeMultiAmpEnergy(MultiblockTooltipBuilder tooltip,
        String[] authors) {
        if (MainConfig.electricBlastFurnaceMultiAmpEnergy) {
            // #tr gtng.ebf.multi_amp_energy
            // # Bottom energy hatch positions also accept multi-amp energy hatches
            // # zh_CN 底层能源仓位置也可使用多安培能源仓
            tooltip.addStructureInfo(StatCollector.translateToLocal("gtng.ebf.multi_amp_energy"));
        }
        return tooltip.toolTipFinisher(authors);
    }
}
