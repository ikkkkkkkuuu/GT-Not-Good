package com.xyp.gtnotgood.loader;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.addItemTooltip;

import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.machines.basic.SteamTurbine;
import com.xyp.gtnotgood.common.machines.basic.UniversalFluidPump;
import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtnotgood.common.machines.hatch.VaultPortHatch;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternBuffer;
import com.xyp.gtnotgood.common.machines.hatch.me.MEDataAccessHatch;
import com.xyp.gtnotgood.common.machines.hatch.me.MaxCapacityMEOutputBus;
import com.xyp.gtnotgood.common.machines.hatch.me.MaxCapacityMEOutputHatch;
import com.xyp.gtnotgood.common.machines.hatch.me.SuperAdvancedMEInputBus;
import com.xyp.gtnotgood.common.machines.hatch.me.SuperAdvancedMEInputHatch;
import com.xyp.gtnotgood.common.machines.multiblock.LargeBeeBreeder;
import com.xyp.gtnotgood.common.machines.multiblock.LargeCropBreeder;
import com.xyp.gtnotgood.common.machines.multiblock.LargeOreProcessor;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;
import com.xyp.gtnotgood.utils.text.AnimatedText;

/**
 * Registers this mod's GregTech meta-tile machines and their item tooltip credits.
 * <p>
 * This mirrors the GT-Not-Cool registration style: instantiate each controller with its stable meta-tile ID, assign it
 * into {@link GTNGItemList}, and immediately attach the animated mod-credit tooltip to the resulting stack.
 */
public class MachineLoader {

    /**
     * Registers all multiblock and single-block machines owned by GT Not Good.
     * <p>
     * New machines should be added here rather than scattered through proxy lifecycle methods. This keeps ID
     * assignment,
     * creative-tab insertion through {@link GTNGItemList#set(gregtech.api.interfaces.metatileentity.IMetaTileEntity)},
     * and tooltip credit registration in one predictable place.
     */
    public static void registerMachines() {
        // #tr NameCircuitMEPatternBuffer
        // # Circuit Pattern Buffer (ME)
        // # zh_CN 电路样板总成 (ME)
        GTNGItemList.CircuitMEPatternBuffer.set(
            new CircuitMEPatternBuffer(
                GTNGMachineID.CircuitMEPatternBuffer.id,
                "CircuitMEPatternBuffer",
                StatCollector.translateToLocal("NameCircuitMEPatternBuffer")));
        addItemTooltip(GTNGItemList.CircuitMEPatternBuffer.get(1), AnimatedText.GT_NOT_GOOD);
        // #tr NameSuperAdvancedMEInputHatch
        // # Super Advanced Stocking Input Hatch (ME)
        // # zh_CN 超级进阶存储输入仓 (ME)
        GTNGItemList.SuperAdvancedMEInputHatch.set(
            new SuperAdvancedMEInputHatch(
                GTNGMachineID.SuperAdvancedMEInputHatch.id,
                "SuperAdvancedMEInputHatch",
                StatCollector.translateToLocal("NameSuperAdvancedMEInputHatch")));
        addItemTooltip(GTNGItemList.SuperAdvancedMEInputHatch.get(1), AnimatedText.GT_NOT_GOOD);
        // #tr NameSuperAdvancedMEInputBus
        // # Super Advanced Stocking Input Bus (ME)
        // # zh_CN 超级进阶存储输入总线 (ME)
        GTNGItemList.SuperAdvancedMEInputBus.set(
            new SuperAdvancedMEInputBus(
                GTNGMachineID.SuperAdvancedMEInputBus.id,
                "SuperAdvancedMEInputBus",
                StatCollector.translateToLocal("NameSuperAdvancedMEInputBus")));
        addItemTooltip(GTNGItemList.SuperAdvancedMEInputBus.get(1), AnimatedText.GT_NOT_GOOD);
        GTNGItemList.CrossRecipeWirelessEnergyHatch.set(
            new CrossRecipeWirelessEnergyHatch(
                GTNGMachineID.CrossRecipeWirelessEnergyHatch.id,
                "CrossRecipeWirelessEnergyHatch"));
        addItemTooltip(GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1), AnimatedText.GT_NOT_GOOD);
        // #tr gtng.comb.name
        // # Comb Processor
        // # zh_CN 蜂窝处理机
        GTNGItemList.LargeCombProcessor.set(
            new com.xyp.gtnotgood.common.machines.multiblock.LargeCombProcessor(
                GTNGMachineID.LargeCombProcessor.id,
                "LargeCombProcessor",
                StatCollector.translateToLocal("gtng.comb.name")));
        addItemTooltip(GTNGItemList.LargeCombProcessor.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr gtng.LargeTransmutationMachine.name
        // # Large Transmutation Machine
        // # zh_CN 大型嬗变机
        GTNGItemList.LargeTransmutationMachine.set(
            new com.xyp.gtnotgood.common.machines.multiblock.LargeTransmutationMachine(
                GTNGMachineID.LargeTransmutationMachine.id,
                "LargeTransmutationMachine",
                StatCollector.translateToLocal("gtng.LargeTransmutationMachine.name")));
        addItemTooltip(GTNGItemList.LargeTransmutationMachine.get(1), AnimatedText.GT_NOT_GOOD);
        // #tr gtng.QuantumComputer.name
        // # QuantumComputer
        // # zh_CN 量子计算机
        GTNGItemList.QuantumComputer.set(
            new com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer(
                GTNGMachineID.QuantumComputer.id,
                "QuantumComputer",
                StatCollector.translateToLocal("gtng.QuantumComputer.name")));
        addItemTooltip(GTNGItemList.QuantumComputer.get(1), AnimatedText.GT_NOT_GOOD);
        // #tr gtng.AssemblerMatrix.name
        // # AssemblerMatrix
        // # zh_CN 装配矩阵
        GTNGItemList.AssemblerMatrix.set(
            new com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix(
                GTNGMachineID.AssemblerMatrix.id,
                "AssemblerMatrix",
                StatCollector.translateToLocal("gtng.AssemblerMatrix.name")));
        addItemTooltip(GTNGItemList.AssemblerMatrix.get(1), AnimatedText.GT_NOT_GOOD);
        GTNGItemList.IntegratedProductionFactory.set(
            new com.xyp.gtnotgood.common.machines.multiblock.IntegratedProductionFactory(
                GTNGMachineID.IntegratedProductionFactory.id,
                "IntegratedProductionFactory",
                FactoryText.NAME.text()));
        addItemTooltip(GTNGItemList.IntegratedProductionFactory.get(1), AnimatedText.GT_NOT_GOOD);
        // #tr NameLargeOreProcessor
        // # Large Ore Processor
        // # zh_CN 大型矿石处理器
        GTNGItemList.LargeOreProcessor.set(
            new LargeOreProcessor(
                GTNGMachineID.LargeOreProcessor.id,
                "LargeOreProcessor",
                StatCollector.translateToLocal("NameLargeOreProcessor")));
        addItemTooltip(GTNGItemList.LargeOreProcessor.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameLargeVoidMiner
        // # Large Void Miner
        // # zh_CN 大型虚空矿机
        GTNGItemList.LargeVoidMiner.set(
            new LargeVoidMiner(
                GTNGMachineID.LargeVoidMiner.id,
                "LargeVoidMiner",
                StatCollector.translateToLocal("NameLargeVoidMiner")));
        addItemTooltip(GTNGItemList.LargeVoidMiner.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameLargeBeeBreeder
        // # Large Bee Breeder
        // # zh_CN 大型蜜蜂杂交机
        GTNGItemList.LargeBeeBreeder.set(
            new LargeBeeBreeder(
                GTNGMachineID.LargeBeeBreeder.id,
                "LargeBeeBreeder",
                StatCollector.translateToLocal("NameLargeBeeBreeder")));
        addItemTooltip(GTNGItemList.LargeBeeBreeder.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameLargeCropBreeder
        // # Large Crop Breeder
        // # zh_CN 大型作物杂交机
        GTNGItemList.LargeCropBreeder.set(
            new LargeCropBreeder(
                GTNGMachineID.LargeCropBreeder.id,
                "LargeCropBreeder",
                StatCollector.translateToLocal("NameLargeCropBreeder")));
        addItemTooltip(GTNGItemList.LargeCropBreeder.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameMaxCapacityMEOutputBus
        // # Max Capacity ME Output Bus
        // # zh_CN 最大容量ME输出总线
        GTNGItemList.MaxCapacityMEOutputBus.set(
            new MaxCapacityMEOutputBus(
                GTNGMachineID.MaxCapacityMEOutputBus.id,
                "MaxCapacityMEOutputBus",
                StatCollector.translateToLocal("NameMaxCapacityMEOutputBus")));

        // #tr NameMaxCapacityMEOutputHatch
        // # Max Capacity ME Output Hatch
        // # zh_CN 最大容量ME输出仓
        GTNGItemList.MaxCapacityMEOutputHatch.set(
            new MaxCapacityMEOutputHatch(
                GTNGMachineID.MaxCapacityMEOutputHatch.id,
                "MaxCapacityMEOutputHatch",
                StatCollector.translateToLocal("NameMaxCapacityMEOutputHatch")));

        // #tr NameMEDataAccessHatch
        // # Data Access Hatch (ME, IV)
        // # zh_CN 数据访问仓 (ME, IV)
        GTNGItemList.MEDataAccessHatch.set(
            new MEDataAccessHatch(
                GTNGMachineID.MEDataAccessHatch.id,
                "MEDataAccessHatch",
                StatCollector.translateToLocal("NameMEDataAccessHatch")));
        addItemTooltip(GTNGItemList.MEDataAccessHatch.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSuperMTEHatchCraftingInputBusME
        // # Super Pattern Input Bus (ME)
        // # zh_CN 超级样板输入总线 (ME)
        GTNGItemList.SuperMTEHatchCraftingInputBusME.set(
            new SuperMTEHatchCraftingInputME(
                GTNGMachineID.SuperCraftingInputBusME.id,
                "SuperMTEHatchCraftingInputBusME",
                StatCollector.translateToLocal("NameSuperMTEHatchCraftingInputBusME"),
                false));
        addItemTooltip(GTNGItemList.SuperMTEHatchCraftingInputBusME.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSuperMTEHatchCraftingInputME
        // # Super Pattern Input Hatch (ME)
        // # zh_CN 超级样板输入总成 (ME)
        GTNGItemList.SuperMTEHatchCraftingInputME.set(
            new SuperMTEHatchCraftingInputME(
                GTNGMachineID.SuperCraftingInputME.id,
                "SuperMTEHatchCraftingInputME",
                StatCollector.translateToLocal("NameSuperMTEHatchCraftingInputME"),
                true));
        addItemTooltip(GTNGItemList.SuperMTEHatchCraftingInputME.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameCompactSuperMTEHatchCraftingInputME
        // # Compact Super Pattern Input Hatch (ME)
        // # zh_CN 缩小超级样板输入总成 (ME)
        GTNGItemList.CompactSuperMTEHatchCraftingInputME.set(
            new SuperMTEHatchCraftingInputME(
                GTNGMachineID.CompactSuperCraftingInputME.id,
                "CompactSuperMTEHatchCraftingInputME",
                StatCollector.translateToLocal("NameCompactSuperMTEHatchCraftingInputME"),
                true,
                1));
        addItemTooltip(GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSuperMTEHatchCraftingInputSlave
        // # Super Pattern Input Mirror (ME)
        // # zh_CN 超级样板输入镜像 (ME)
        GTNGItemList.SuperMTEHatchCraftingInputSlave.set(
            new SuperMTEHatchCraftingInputSlave(
                GTNGMachineID.SuperCraftingInputSlave.id,
                "SuperCraftingInputProxy",
                StatCollector.translateToLocal("NameSuperMTEHatchCraftingInputSlave")).getStackForm(1L));
        addItemTooltip(GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSingularityDataHub
        // # Singularity Data Hub
        // # zh_CN 奇点数据枢纽
        GTNGItemList.SingularityDataHub.set(
            new SingularityDataHub(
                GTNGMachineID.SingularityDataHub.id,
                "SingularityDataHub",
                StatCollector.translateToLocal("NameSingularityDataHub")));
        addItemTooltip(GTNGItemList.SingularityDataHub.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameVaultPortHatch
        // # Vault Port Hatch
        // # zh_CN 仓库端口仓
        GTNGItemList.VaultPortHatch.set(
            new VaultPortHatch(
                GTNGMachineID.VaultPortHatch.id,
                "VaultPortHatch",
                StatCollector.translateToLocal("NameVaultPortHatch")));
        addItemTooltip(GTNGItemList.VaultPortHatch.get(1), AnimatedText.GT_NOT_GOOD);

    }

    public static void registerbasicMachine() {
        // #tr gtng.pump.name
        // # High-Speed Universal Fluid Pump (LV)
        // # zh_CN 高速通用流体泵 (LV)
        GTNGItemList.UniversalFluidPump.set(
            new UniversalFluidPump(
                GTNGMachineID.UniversalFluidPump.id,
                "UniversalFluidPump",
                StatCollector.translateToLocal("gtng.pump.name")));
        addItemTooltip(GTNGItemList.UniversalFluidPump.get(1), AnimatedText.GT_NOT_GOOD);

        if (com.xyp.gtnotgood.utils.enums.ModList.ThaumicEnergistics.isModLoaded()
            && com.xyp.gtnotgood.utils.enums.ModList.Thaumcraft.isModLoaded()) {
            // #tr gtng.EssentiaDisassembler.name
            // # Advanced Essentia Disassembler
            // # zh_CN 高级源质分解机
            GTNGItemList.EssentiaDisassembler.set(
                new com.xyp.gtnotgood.common.machines.basic.EssentiaDisassembler(
                    GTNGMachineID.EssentiaDisassembler.id,
                    "essentia_disassembler",
                    StatCollector.translateToLocal("gtng.EssentiaDisassembler.name")));
        }

        // #tr SteamTurbineLV
        // # Steam Turbine LV
        // # zh_CN 基础蒸汽轮机
        GTNGItemList.SteamTurbineLV.set(
            new SteamTurbine(
                GTNGMachineID.SteamTurbineLV.id,
                "SteamTurbineLV",
                StatCollector.translateToLocal("SteamTurbineLV"),
                1));
        addItemTooltip(GTNGItemList.SteamTurbineLV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineMV
        // # Steam Turbine MV
        // # zh_CN 进阶蒸汽轮机
        GTNGItemList.SteamTurbineMV.set(
            new SteamTurbine(
                GTNGMachineID.SteamTurbineMV.id,
                "SteamTurbineMV",
                StatCollector.translateToLocal("SteamTurbineMV"),
                2));
        addItemTooltip(GTNGItemList.SteamTurbineMV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineHV
        // # Steam Turbine HV
        // # zh_CN 进阶蒸汽轮机 II
        GTNGItemList.SteamTurbineHV.set(
            new SteamTurbine(
                GTNGMachineID.SteamTurbineHV.id,
                "SteamTurbineHV",
                StatCollector.translateToLocal("SteamTurbineHV"),
                3));
        addItemTooltip(GTNGItemList.SteamTurbineHV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineEV
        // # Steam Turbine EV
        // # zh_CN 进阶蒸汽轮机 III
        GTNGItemList.SteamTurbineEV.set(
            new SteamTurbine(
                GTNGMachineID.SteamTurbineEV.id,
                "SteamTurbineEV",
                StatCollector.translateToLocal("SteamTurbineEV"),
                4));
        addItemTooltip(GTNGItemList.SteamTurbineEV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineIV
        // # Steam Turbine IV
        // # zh_CN 进阶蒸汽轮机 IV
        GTNGItemList.SteamTurbineIV.set(
            new SteamTurbine(
                GTNGMachineID.SteamTurbineIV.id,
                "SteamTurbineIV",
                StatCollector.translateToLocal("SteamTurbineIV"),
                5));
        addItemTooltip(GTNGItemList.SteamTurbineIV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineLuV
        // # Steam Turbine V
        // # zh_CN 进阶蒸汽轮机 V
        GTNGItemList.SteamTurbineLuV.set(
            new SteamTurbine(
                GTNGMachineID.SteamTurbineLuV.id,
                "SteamTurbineLuV",
                StatCollector.translateToLocal("SteamTurbineLuV"),
                6));
        addItemTooltip(GTNGItemList.SteamTurbineLuV.get(1), AnimatedText.GT_NOT_GOOD);

    }

    /**
     * Entry point used by the common proxy to register every machine-related object.
     * <p>
     * This name follows the GT-Not-Cool loader pattern, where a single {@code registry()} method fans out to machine,
     * hatch, basic-machine, and cover registration as the mod grows.
     */
    public static void registry() {
        registerMachines();
        registerbasicMachine();
        WirelessLaserLoader.register();
    }
}
