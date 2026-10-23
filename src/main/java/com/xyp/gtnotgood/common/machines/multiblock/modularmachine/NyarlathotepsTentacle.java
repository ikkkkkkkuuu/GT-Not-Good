// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.blocks.assemblyplant.AssemblyPlantCasing;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.common.blocks.tentacle.TentacleCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.NyarlathotepsTentacleGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiModeMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.NyarlathotepsTentacleStructure;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import tectech.thing.metaTileEntity.hatch.MTEHatchDataItemsInput;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class NyarlathotepsTentacle extends GTNGMultiModeMachineBase<NyarlathotepsTentacle>
    implements ISurvivalConstructable {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "fieldextruder/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active_glow");

    private final List<MTEHatchDataAccess> dataHatches = new ArrayList<>();
    private IStructureDefinition<NyarlathotepsTentacle> definition;
    private HeatingCoilLevel coilLevel = HeatingCoilLevel.None;

    public NyarlathotepsTentacle(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public NyarlathotepsTentacle(String name) {
        super(name);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.tentacle.name
        // # Nyarlathotep's Tentacle
        // # zh_CN 奈亚拉托提普之触
        return "gtng.tentacle.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new NyarlathotepsTentacle(mName);
    }

    @Override
    public IStructureDefinition<NyarlathotepsTentacle> getStructureDefinition() {
        if (definition == null) {
            ItemStack ruby = GTOreDictUnificator.get(OrePrefixes.block, Materials.Ruby, 1);
            definition = StructureDefinition.<NyarlathotepsTentacle>builder()
                .addShape("main", transpose(NyarlathotepsTentacleStructure.shape()))
                .addElement('A',
                    ofChain(
                        GTStructureUtility.ofHatchAdder(NyarlathotepsTentacle::addModularHatchToMachineList,
                            TentacleCasing.MolecularCasing.getTextureId(), 1),
                        GTStructureUtility.ofHatchAdder(NyarlathotepsTentacle::addDataHatch,
                            TentacleCasing.MolecularCasing.getTextureId(), 1),
                        GTStructureUtility.ofHatchAdder(NyarlathotepsTentacle::addMaintenanceToMachineList,
                            TentacleCasing.MolecularCasing.getTextureId(), 1),
                        buildHatchAdder(NyarlathotepsTentacle.class)
                            .atLeast(InputBus, OutputBus, InputHatch, Energy.or(ExoticEnergy))
                            .casingIndex(TentacleCasing.MolecularCasing.getTextureId()).hint(1)
                            .buildAndChain(TentacleCasing.MolecularCasing.asElement())))
                .addElement('B', GTStructureUtility.activeCoils(KerrNewmanCasing.AdvancedFusionCoil.asElement()))
                .addElement('C', TentacleCasing.LaserCasing.asElement())
                .addElement('D', GTStructureUtility.activeCoils(TentacleCasing.UruiumCoil.asElement()))
                .addElement('E', GTStructureUtility.activeCoils(LightHunterCasing.HyperCore.asElement()))
                .addElement('F', GTStructureUtility.ofFrame(Materials.Neutronium))
                .addElement('G', TentacleCasing.SpacetimeAssemblyLineCasing.asElement())
                .addElement('H', KerrNewmanCasing.ExtremeDensityCasing.asElement())
                .addElement('I', DimensionalFocusCasing.FusionGlass.asElement())
                .addElement('J', TentacleCasing.LaserCoolingCasing.asElement())
                .addElement('K', KerrNewmanCasing.QuantumGlass.asElement())
                .addElement('L', TentacleCasing.NaquadahAlloyCasing.asElement())
                .addElement('M', HeartCasing.TemporalAnchorFieldCasing.asElement())
                .addElement('N', TentacleCasing.SpacetimeAssemblyLineUnit.asElement())
                .addElement('O', TentacleCasing.MolecularCasing.asElement())
                .addElement('P', TentacleCasing.ContainmentFieldGenerator.asElement())
                .addElement('Q',
                    GTStructureUtility.activeCoils(GTStructureUtility.ofCoil(NyarlathotepsTentacle::setCoilLevel,
                        NyarlathotepsTentacle::getCoilLevel)))
                .addElement('R', TentacleCasing.SterilizingFilterCasing.asElement())
                .addElement('S', TentacleCasing.ElectronPermeableGlass.asElement())
                .addElement('T', TentacleCasing.HollowCasing.asElement())
                .addElement('U', KerrNewmanCasing.AmprosiumActiveCasing.asElement())
                .addElement('V', GTStructureUtility.ofFrame(Materials.Mithril))
                .addElement('W', ofBlock(Block.getBlockFromItem(ruby.getItem()), ruby.getItemDamage()))
                .addElement('X', GTStructureUtility.activeCoils(AssemblyPlantCasing.ElectrolyticCell.asElement()))
                .addElement('Y', GTStructureUtility.ofFrame(Materials.Trinium))
                .addElement('Z', LightHunterCasing.HighPowerCasing.asElement())
                .addElement('[', KerrNewmanCasing.DimensionalBridgeCasing.asElement())
                .addElement('b', TentacleCasing.RestraintDevice.asElement())
                .addElement(']', TentacleCasing.QuarkPipe.asElement())
                .addElement('^', DimensionalFocusCasing.MolecularCoil.asElement())
                .addElement('_', TentacleCasing.DegenerateRheniumConstrainedCasing.asElement())
                .addElement('`', DimensionalFocusCasing.DimensionallyTranscendent.asElement()).build();
        }
        return definition;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        dataHatches.clear();
    }

    private boolean addDataHatch(IGregTechTileEntity tile, int texture) {
        if (
            tile == null || !(tile.getMetaTileEntity() instanceof MTEHatchDataAccess hatch)
                || hatch instanceof MTEHatchDataItemsInput
        ) return false;
        hatch.updateTexture(texture);
        hatch.updateCraftingIcon(getMachineCraftingIcon());
        if (!dataHatches.contains(hatch)) dataHatches.add(hatch);
        return true;
    }

    public List<MTEHatchDataAccess> getDataHatches() {
        return Collections.unmodifiableList(dataHatches);
    }

    public HeatingCoilLevel getCoilLevel() {
        return coilLevel;
    }

    public void setCoilLevel(HeatingCoilLevel level) {
        coilLevel = level;
    }

    @Override
    protected int getBaseParallelRecipes() {
        return DimensionalFocusEngravingArray.parallelForHeat(coilLevel.getHeat());
    }

    @Override
    protected String getBaseParallelDescription() {
        // #tr gtng.tentacle.parallel
        // # Base parallel: 2^(coil temperature / 900), rounded down
        // # zh_CN 基础并行为 2^(线圈温度 / 900)，向下取整
        return StatCollector.translateToLocal("gtng.tentacle.parallel");
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, NyarlathotepsTentacleStructure.OFFSET_X,
            NyarlathotepsTentacleStructure.OFFSET_Y, NyarlathotepsTentacleStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, NyarlathotepsTentacleStructure.OFFSET_X,
                NyarlathotepsTentacleStructure.OFFSET_Y, NyarlathotepsTentacleStructure.OFFSET_Z, budget, env, false,
                true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        coilLevel = HeatingCoilLevel.None;
        if (
            !checkPiece("main", NyarlathotepsTentacleStructure.OFFSET_X, NyarlathotepsTentacleStructure.OFFSET_Y,
                NyarlathotepsTentacleStructure.OFFSET_Z, errors)
        ) return;
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasInputHatch(errors);
        checkHasAnyEnergy(errors);
        checkModularHatches(errors);
    }

    @Override
    protected int getMachineModeCount() {
        return 2;
    }

    @Override
    protected RecipeMap<?> getRecipeMap(int mode) {
        return mode == 1 ? GTNGRecipeMaps.TentacleCircuitAssemblyRecipes : GTNGRecipeMaps.TentacleAssemblyRecipes;
    }

    @Override
    public String getMachineModeKey() {
        // #tr gtng.tentacle.mode.circuit
        // # Circuit Assembly Line
        // # zh_CN 电路装配线
        if (machineMode == 1) return "gtng.tentacle.mode.circuit";
        // #tr gtng.tentacle.mode.suprachronal
        // # Suprachronal Assembly Line
        // # zh_CN 超时空装配线
        return "gtng.tentacle.mode.suprachronal";
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setBoolean("tentacleUnifiedAssembly", true);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        if (!tag.getBoolean("tentacleUnifiedAssembly")) {
            // Old modes 0 (UIV+) and 1 (all tiers) now share one assembly pool; old mode 2 was circuits.
            tag = (NBTTagCompound) tag.copy();
            tag.setInteger("machineMode", tag.getInteger("machineMode") == 2 ? 1 : 0);
        }
        super.loadNBTData(tag);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new NyarlathotepsTentacleGui(this);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = TentacleCasing.MolecularCasing.getTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE_GLOW : FRONT_GLOW).extFacing().glow().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.tentacle.type
        // # Assembly Line, Circuit Assembly Line
        // # zh_CN 装配线，电路装配线
        addModularMachineType(tt, StatCollector.translateToLocal("gtng.tentacle.type"))
            // #tr gtng.tentacle.research
            // # Assembly recipes require no Data Stick or Data Access Hatch
            // # zh_CN 装配配方无需闪存或数据访问仓
            .addInfo(StatCollector.translateToLocal("gtng.tentacle.research"))
            // #tr gtng.tentacle.unordered
            // # Inputs may be supplied in any order; supports ME Pattern Buffers
            // # zh_CN 原料无需按装配线顺序输入，支持 ME 样板总成
            .addInfo(StatCollector.translateToLocal("gtng.tentacle.unordered"))
            // #tr gtng.tentacle.imprint
            // # Circuit assembly requires no Circuit Imprint
            // # zh_CN 电路装配无需电路印刻
            .addInfo(StatCollector.translateToLocal("gtng.tentacle.imprint"));
        addModularTooltip(tt);
        tt.beginStructureBlock(165, 48, 49, false)
            // #tr gtng.tentacle.controller
            // # Front center, 24th layer from the top
            // # zh_CN 正面中央，自顶向下第 24 层
            .addController(StatCollector.translateToLocal("gtng.tentacle.controller"))
            // #tr gtng.tentacle.structure
            // # 62 hatch positions; 424 identical heating coils; use the Structure Hologram Projector
            // # zh_CN 62 处舱位；424 个同种加热线圈；使用结构投影仪查看完整结构
            .addStructureInfo(StatCollector.translateToLocal("gtng.tentacle.structure"))
            .addInputBus("1+", TentacleCasing.MolecularCasing.getStack(1).getDisplayName(), 1)
            .addInputHatch("1+", TentacleCasing.MolecularCasing.getStack(1).getDisplayName(), 1)
            .addOutputBus("1+", TentacleCasing.MolecularCasing.getStack(1).getDisplayName(), 1)
            .addEnergyHatch("1+", TentacleCasing.MolecularCasing.getStack(1).getDisplayName(), 1)
            // #tr gtng.tentacle.data_hatch
            // # Data Access Hatches are optional; a fluid-capable ME Pattern Buffer can supply all inputs
            // # zh_CN 数据访问仓可选；支持流体的 ME 样板总成可提供全部输入
            .addStructureInfo(StatCollector.translateToLocal("gtng.tentacle.data_hatch"));
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }
}
