package com.xyp.gtnotgood.loader;

import static com.xyp.gtnotgood.utils.text.AnimatedTooltipHandler.addItemTooltip;

import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.machines.basicMachine.SteamTurbine;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtnotgood.common.machines.hatch.VaultPortHatch;
import com.xyp.gtnotgood.common.machines.hatch.me.MaxCapacityMEOutputBus;
import com.xyp.gtnotgood.common.machines.hatch.me.MaxCapacityMEOutputHatch;
import com.xyp.gtnotgood.common.machines.multiblock.AssemblyFactory;
import com.xyp.gtnotgood.common.machines.multiblock.DimensionallyTranscendentPlasmaFusionComputer;
import com.xyp.gtnotgood.common.machines.multiblock.LargeBeeBreeder;
import com.xyp.gtnotgood.common.machines.multiblock.LargeCropBreeder;
import com.xyp.gtnotgood.common.machines.multiblock.LargeOreProcessor;
import com.xyp.gtnotgood.common.machines.multiblock.LargeVoidMiner;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
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
        // #tr NameLargeOreProcessor
        // # Large Ore Processor
        // # zh_CN 大型矿石处理器
        GTNGItemList.LargeOreProcessor.set(
            new LargeOreProcessor(
                GTNGMachineID.LARGE_ORE_PROCESSOR.ID,
                "LargeOreProcessor",
                StatCollector.translateToLocal("NameLargeOreProcessor")));
        addItemTooltip(GTNGItemList.LargeOreProcessor.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameLargeVoidMiner
        // # Large Void Miner
        // # zh_CN 大型虚空矿机
        GTNGItemList.LargeVoidMiner.set(
            new LargeVoidMiner(
                GTNGMachineID.LARGE_VOID_MINER.ID,
                "LargeVoidMiner",
                StatCollector.translateToLocal("NameLargeVoidMiner")));
        addItemTooltip(GTNGItemList.LargeVoidMiner.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameLargeBeeBreeder
        // # Large Bee Breeder
        // # zh_CN 大型蜜蜂杂交机
        GTNGItemList.LargeBeeBreeder.set(
            new LargeBeeBreeder(
                GTNGMachineID.LARGE_BEE_BREEDER.ID,
                "LargeBeeBreeder",
                StatCollector.translateToLocal("NameLargeBeeBreeder")));
        addItemTooltip(GTNGItemList.LargeBeeBreeder.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameLargeCropBreeder
        // # Large Crop Breeder
        // # zh_CN 大型作物杂交机
        GTNGItemList.LargeCropBreeder.set(
            new LargeCropBreeder(
                GTNGMachineID.LARGE_CROP_BREEDER.ID,
                "LargeCropBreeder",
                StatCollector.translateToLocal("NameLargeCropBreeder")));
        addItemTooltip(GTNGItemList.LargeCropBreeder.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr machine.gtnotgood.dtpf.name
        // # Dimensionally Transcendent Plasma Fusion Computer
        // # zh_CN 超维度等离子聚变堆
        GTNGItemList.DimensionallyTranscendentPlasmaFusionComputer.set(
            new DimensionallyTranscendentPlasmaFusionComputer(
                GTNGMachineID.DIMENSIONALLY_TRANSCENDENT_PLASMA_FUSION_COMPUTER.ID,
                "DimensionallyTranscendentPlasmaFusionComputer",
                StatCollector.translateToLocal("machine.gtnotgood.dtpf.name")));
        addItemTooltip(GTNGItemList.DimensionallyTranscendentPlasmaFusionComputer.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr machine.gtnotgood.assembly_factory.name
        // # Assembly Factory
        // # zh_CN 全能原初装配矩阵
        GTNGItemList.AssemblyFactory.set(
            new AssemblyFactory(
                GTNGMachineID.ASSEMBLY_FACTORY.ID,
                "AssemblyFactory",
                StatCollector.translateToLocal("machine.gtnotgood.assembly_factory.name")));
        addItemTooltip(GTNGItemList.AssemblyFactory.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameMaxCapacityMEOutputBus
        // # Max Capacity ME Output Bus
        // # zh_CN 最大容量ME输出总线
        GTNGItemList.MaxCapacityMEOutputBus.set(
            new MaxCapacityMEOutputBus(
                GTNGMachineID.MAX_CAPACITY_ME_OUTPUT_BUS.ID,
                "MaxCapacityMEOutputBus",
                StatCollector.translateToLocal("NameMaxCapacityMEOutputBus")));

        // #tr NameMaxCapacityMEOutputHatch
        // # Max Capacity ME Output Hatch
        // # zh_CN 最大容量ME输出仓
        GTNGItemList.MaxCapacityMEOutputHatch.set(
            new MaxCapacityMEOutputHatch(
                GTNGMachineID.MAX_CAPACITY_ME_OUTPUT_HATCH.ID,
                "MaxCapacityMEOutputHatch",
                StatCollector.translateToLocal("NameMaxCapacityMEOutputHatch")));

        // #tr NameSuperMTEHatchCraftingInputBusME
        // # Super Pattern Input Bus (ME)
        // # zh_CN 超级样板输入总线 (ME)
        GTNGItemList.SuperMTEHatchCraftingInputBusME.set(
            new SuperMTEHatchCraftingInputME(
                GTNGMachineID.SUPER_CRAFTING_INPUT_BUS_ME.ID,
                "SuperMTEHatchCraftingInputBusME",
                StatCollector.translateToLocal("NameSuperMTEHatchCraftingInputBusME"),
                false));
        addItemTooltip(GTNGItemList.SuperMTEHatchCraftingInputBusME.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSuperMTEHatchCraftingInputME
        // # Super Pattern Input Hatch (ME)
        // # zh_CN 超级样板输入总成 (ME)
        GTNGItemList.SuperMTEHatchCraftingInputME.set(
            new SuperMTEHatchCraftingInputME(
                GTNGMachineID.SUPER_CRAFTING_INPUT_ME.ID,
                "SuperMTEHatchCraftingInputME",
                StatCollector.translateToLocal("NameSuperMTEHatchCraftingInputME"),
                true));
        addItemTooltip(GTNGItemList.SuperMTEHatchCraftingInputME.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSuperMTEHatchCraftingInputSlave
        // # Super Pattern Input Mirror (ME)
        // # zh_CN 超级样板输入镜像 (ME)
        GTNGItemList.SuperMTEHatchCraftingInputSlave.set(
            new SuperMTEHatchCraftingInputSlave(
                GTNGMachineID.SUPER_CRAFTING_INPUT_SLAVE.ID,
                "SuperCraftingInputProxy",
                StatCollector.translateToLocal("NameSuperMTEHatchCraftingInputSlave")).getStackForm(1L));
        addItemTooltip(GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameSingularityDataHub
        // # Singularity Data Hub
        // # zh_CN 奇点数据枢纽
        GTNGItemList.SingularityDataHub.set(
            new SingularityDataHub(
                GTNGMachineID.SINGULARITY_DATA_HUB.ID,
                "SingularityDataHub",
                StatCollector.translateToLocal("NameSingularityDataHub")));
        addItemTooltip(GTNGItemList.SingularityDataHub.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr NameVaultPortHatch
        // # Vault Port Hatch
        // # zh_CN 仓库端口仓
        GTNGItemList.VaultPortHatch.set(
            new VaultPortHatch(
                GTNGMachineID.VAULT_PORT_HATCH.ID,
                "VaultPortHatch",
                StatCollector.translateToLocal("NameVaultPortHatch")));
        addItemTooltip(GTNGItemList.VaultPortHatch.get(1), AnimatedText.GT_NOT_GOOD);

    }

    public static void registerbasicMachine() {

        // #tr SteamTurbineLV
        // # Steam Turbine LV
        // # zh_CN 基础蒸汽轮机
        GTNGItemList.SteamTurbineLV.set(
            new SteamTurbine(
                GTNGMachineID.STEAM_TURBINE_LV.ID,
                "SteamTurbineLV",
                StatCollector.translateToLocal("SteamTurbineLV"),
                1));
        addItemTooltip(GTNGItemList.SteamTurbineLV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineMV
        // # Steam Turbine MV
        // # zh_CN 进阶蒸汽轮机
        GTNGItemList.SteamTurbineMV.set(
            new SteamTurbine(
                GTNGMachineID.STEAM_TURBINE_MV.ID,
                "SteamTurbineMV",
                StatCollector.translateToLocal("SteamTurbineMV"),
                2));
        addItemTooltip(GTNGItemList.SteamTurbineMV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineHV
        // # Steam Turbine HV
        // # zh_CN 进阶蒸汽轮机 II
        GTNGItemList.SteamTurbineHV.set(
            new SteamTurbine(
                GTNGMachineID.STEAM_TURBINE_HV.ID,
                "SteamTurbineHV",
                StatCollector.translateToLocal("SteamTurbineHV"),
                3));
        addItemTooltip(GTNGItemList.SteamTurbineHV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineEV
        // # Steam Turbine EV
        // # zh_CN 进阶蒸汽轮机 III
        GTNGItemList.SteamTurbineEV.set(
            new SteamTurbine(
                GTNGMachineID.STEAM_TURBINE_EV.ID,
                "SteamTurbineEV",
                StatCollector.translateToLocal("SteamTurbineEV"),
                4));
        addItemTooltip(GTNGItemList.SteamTurbineEV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineIV
        // # Steam Turbine IV
        // # zh_CN 进阶蒸汽轮机 IV
        GTNGItemList.SteamTurbineIV.set(
            new SteamTurbine(
                GTNGMachineID.STEAM_TURBINE_IV.ID,
                "SteamTurbineIV",
                StatCollector.translateToLocal("SteamTurbineIV"),
                5));
        addItemTooltip(GTNGItemList.SteamTurbineIV.get(1), AnimatedText.GT_NOT_GOOD);

        // #tr SteamTurbineLuV
        // # Steam Turbine V
        // # zh_CN 进阶蒸汽轮机 V
        GTNGItemList.SteamTurbineLuV.set(
            new SteamTurbine(
                GTNGMachineID.STEAM_TURBINE_LUV.ID,
                "SteamTurbineLuV",
                StatCollector.translateToLocal("SteamTurbineLuV"),
                6));
        addItemTooltip(GTNGItemList.SteamTurbineLuV.get(1), AnimatedText.GT_NOT_GOOD);

    }

    /**
     * Backward-compatible alias for older call sites that still use the ExampleMod-style method name.
     *
     * @see #registry()
     */
    public static void loadMachines() {
        registry();
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
    }
}
