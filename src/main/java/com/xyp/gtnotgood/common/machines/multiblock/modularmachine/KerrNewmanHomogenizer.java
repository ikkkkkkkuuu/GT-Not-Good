// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.xyp.gtnotgood.client.render.KerrNewmanBlackHoleRenderer;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.blocks.kerrnewman.KerrNewmanCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.KerrNewmanHomogenizerGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.KerrNewmanHomogenizerStructure;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.render.IMTERenderer;
import gtPlusPlus.core.material.MaterialsAlloy;
import io.netty.buffer.ByteBuf;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class KerrNewmanHomogenizer extends GTNGModularMachineBase<KerrNewmanHomogenizer>
    implements ISurvivalConstructable, IMTERenderer {

    private static final int SPEED_MULTIPLIER = 60;
    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "fieldextruder/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active_glow");

    private IStructureDefinition<KerrNewmanHomogenizer> definition;
    private boolean blackHoleWorking;
    private int lastRenderState = -1;
    private static int[][] coilPositions;
    private ExtendedFacing coilFacing;

    public KerrNewmanHomogenizer(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public KerrNewmanHomogenizer(String name) {
        super(name);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new KerrNewmanHomogenizerGui(this);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.kerr_newman.name
        // # Kerr-Newman Homogenizer
        // # zh_CN 克尔-纽曼均质仪
        return "gtng.kerr_newman.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new KerrNewmanHomogenizer(mName);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public IStructureDefinition<KerrNewmanHomogenizer> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<KerrNewmanHomogenizer>builder()
                .addShape("main", transpose(KerrNewmanHomogenizerStructure.shape()))
                .addElement('A', KerrNewmanCasing.EnhancedHyperCasing.asElement())
                .addElement('B', FieldExtruderCasing.Iridium.asElement())
                .addElement('C', FieldExtruderCasing.HyperMechanical.asElement())
                .addElement('D', KerrNewmanCasing.ZirconiaCeramicCasing.asElement())
                .addElement('E', KerrNewmanCasing.PressureContainmentCasing.asElement())
                .addElement('F', GTStructureUtility.ofFrame(Materials.NaquadahAlloy))
                .addElement('G', KerrNewmanCasing.AmprosiumGearbox.asElement())
                .addElement('H', KerrNewmanCasing.AmprosiumPipeCasing.asElement())
                .addElement('I', KerrNewmanCasing.OilGasPipeCasing.asElement())
                .addElement('J', KerrNewmanCasing.HighPressurePipeCasing.asElement())
                .addElement('K',
                    ofChain(
                        GTStructureUtility.ofHatchAdder(KerrNewmanHomogenizer::addModularHatchToMachineList,
                            DimensionalFocusCasing.DimensionInjection.getTextureId(), 1),
                        GTStructureUtility.ofHatchAdder(KerrNewmanHomogenizer::addMaintenanceToMachineList,
                            DimensionalFocusCasing.DimensionInjection.getTextureId(), 1),
                        buildHatchAdder(KerrNewmanHomogenizer.class)
                            .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy))
                            .casingIndex(DimensionalFocusCasing.DimensionInjection.getTextureId()).hint(1)
                            .buildAndChain(DimensionalFocusCasing.DimensionInjection.asElement())))
                .addElement('L', KerrNewmanCasing.GravityStabilizationCasing.asElement())
                .addElement('M', KerrNewmanCasing.IndustrialFramelessGlass.asElement())
                .addElement('N', KerrNewmanCasing.ShieldedAcceleratorCasing.asElement())
                .addElement('O', DimensionalFocusCasing.DimensionInjection.asElement())
                .addElement('P', GTStructureUtility.ofFrame(MaterialsAlloy.HASTELLOY_N))
                .addElement('Q', KerrNewmanCasing.BioactiveCasing.asElement())
                .addElement('R', KerrNewmanCasing.AmprosiumActiveCasing.asElement())
                .addElement('S', KerrNewmanCasing.HighStrengthSupportSpindle.asElement())
                .addElement('T', KerrNewmanCasing.AcceleratorCoilUhv.asElement())
                .addElement('U', KerrNewmanCasing.ExtremeDensityCasing.asElement())
                .addElement('V', KerrNewmanCasing.DimensionalBridgeCasing.asElement())
                .addElement('W', GTStructureUtility.ofFrame(Materials.TungstenCarbide))
                .addElement('X', KerrNewmanCasing.PpsCorrosionResistantCasing.asElement())
                .addElement('Y', KerrNewmanCasing.QuantumGlass.asElement())
                .addElement('Z', KerrNewmanCasing.PlasmaHeaterCasing.asElement())
                .addElement('[', KerrNewmanCasing.AtomicCasing.asElement())
                .addElement('\\', KerrNewmanCasing.RadiationAbsorbentCasing.asElement())
                .addElement(']', KerrNewmanCasing.FusionCasingMkIV.asElement())
                .addElement('^', KerrNewmanCasing.AdvancedFusionCoil.asElement())
                .addElement('_', FieldExtruderCasing.Hollow.asElement())
                .addElement('`', GTStructureUtility.ofFrame(Materials.Trinium))
                .addElement('a', FieldExtruderCasing.ContainmentFieldGenerator.asElement()).build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, KerrNewmanHomogenizerStructure.OFFSET_X,
            KerrNewmanHomogenizerStructure.OFFSET_Y, KerrNewmanHomogenizerStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, KerrNewmanHomogenizerStructure.OFFSET_X,
                KerrNewmanHomogenizerStructure.OFFSET_Y, KerrNewmanHomogenizerStructure.OFFSET_Z, budget, env, false,
                true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (
            !checkPiece("main", KerrNewmanHomogenizerStructure.OFFSET_X, KerrNewmanHomogenizerStructure.OFFSET_Y,
                KerrNewmanHomogenizerStructure.OFFSET_Z, errors)
        ) return;
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
        checkHasInputHatch(errors);
        checkHasOutputHatch(errors);
        checkHasAnyEnergy(errors);
        checkMaximum(errors, InputBus, mInputBusses.size() + mDualInputHatches.size(), 24);
        checkMaximum(errors, OutputBus, mOutputBusses.size(), 24);
        checkMaximum(errors, InputHatch, mInputHatches.size(), 24);
        checkMaximum(errors, OutputHatch, mOutputHatches.size(), 24);
        checkModularHatches(errors);
    }

    private static void checkMaximum(List<StructureError> errors, HatchElement hatch, int count, int maximum) {
        if (count > maximum) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, hatch, count, maximum));
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.mixerNonCellRecipes;
    }

    @Override
    protected ProcessingLogic createMachineProcessingLogic() {
        return new ModularProcessingLogic().setMaxParallelSupplier(this::getTrueParallel).setMaxTierSkips(0);
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList()))
            voltage = Math.max(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
        logic.setAvailableVoltage(voltage).setAvailableAmperage(voltage == 0 ? 0 : getMaxInputEu() / voltage)
            .setAmperageOC(false);
    }

    @Override
    protected double getBaseSpeedMultiplier() {
        return SPEED_MULTIPLIER;
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        if (tile.isServerSide()) {
            updateBlackHole(tile);
            updateCoils(tile, blackHoleWorking);
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) updateBlackHole(tile);
    }

    private void updateBlackHole(IGregTechTileEntity tile) {
        blackHoleWorking = mMachine && tile.isActive() && mMaxProgresstime > 0;
        int state = blackHoleWorking ? 1 : 0;
        if (state != lastRenderState) {
            lastRenderState = state;
            updateCoils(tile, blackHoleWorking);
            tile.issueTileUpdate();
        }
    }

    private void updateCoils(IGregTechTileEntity tile, boolean active) {
        if (coilPositions == null) {
            List<int[]> positions = new ArrayList<>();
            String[][] shape = KerrNewmanHomogenizerStructure.shape();
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
                for (int column = 0; column < shape[row][depth].length(); column++) {
                    char symbol = shape[row][depth].charAt(column);
                    if (
                        symbol == 'T' || symbol == '^'
                    ) positions.add(new int[] { column - KerrNewmanHomogenizerStructure.OFFSET_X,
                        row - KerrNewmanHomogenizerStructure.OFFSET_Y, depth - KerrNewmanHomogenizerStructure.OFFSET_Z,
                        symbol });
                }
            coilPositions = positions.toArray(new int[0][]);
        }
        ExtendedFacing facing = getExtendedFacing();
        if (coilFacing != null && coilFacing != facing) setCoils(tile, coilFacing, false);
        coilFacing = facing;
        setCoils(tile, facing, active);
    }

    private void setCoils(IGregTechTileEntity tile, ExtendedFacing facing, boolean active) {
        int[] offset = new int[3];
        for (int[] position : coilPositions) {
            facing.getWorldOffset(new int[] { position[0], position[1], position[2] }, offset);
            int x = tile.getXCoord() + offset[0];
            int y = tile.getYCoord() + offset[1];
            int z = tile.getZCoord() + offset[2];
            KerrNewmanCasing casing = position[3] == 'T' ? KerrNewmanCasing.AcceleratorCoilUhv
                : KerrNewmanCasing.AdvancedFusionCoil;
            if (tile.getWorld().blockExists(x, y, z)) casing.getBlock().setActive(tile.getWorld(), x, y, z, active);
        }
    }

    public boolean isBlackHoleWorking() {
        return blackHoleWorking;
    }

    public Vec3Impl getBlackHoleOffset() {
        int[] offset = new int[3];
        getExtendedFacing().getWorldOffset(new int[] { 0, 0, 34 }, offset);
        return new Vec3Impl(offset[0], offset[1], offset[2]);
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeBoolean(blackHoleWorking);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        blackHoleWorking = buffer.readBoolean();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        Vec3Impl offset = getBlackHoleOffset();
        double centerX = x + offset.get0() + 0.5;
        double centerY = y + offset.get1() + 0.5;
        double centerZ = z + offset.get2() + 0.5;
        return AxisAlignedBB.getBoundingBox(Math.min(x, centerX - 4.5), Math.min(y, centerY - 4.5),
            Math.min(z, centerZ - 4.5), Math.max(x + 1, centerX + 4.5), Math.max(y + 1, centerY + 4.5),
            Math.max(z + 1, centerZ + 4.5));
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256D * 256D;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float partialTicks) {
        Vec3Impl offset = getBlackHoleOffset();
        KerrNewmanBlackHoleRenderer.render(this, x + offset.get0() + 0.5, y + offset.get1() + 0.5,
            z + offset.get2() + 0.5, blackHoleWorking, getBaseMetaTileEntity().getWorld().getTotalWorldTime(),
            partialTicks);
    }

    @Override
    public void onUnload() {
        stopClientBlackHole();
        blackHoleWorking = false;
        lastRenderState = -1;
        super.onUnload();
    }

    @Override
    public void onRemoval() {
        stopClientBlackHole();
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide()) {
            mMachine = false;
            blackHoleWorking = false;
            updateCoils(tile, false);
        }
        super.onRemoval();
    }

    private void stopClientBlackHole() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide() && tile.getWorld() != null) stopBlackHole();
    }

    @SideOnly(Side.CLIENT)
    private void stopBlackHole() {
        KerrNewmanBlackHoleRenderer.stop(this, getBaseMetaTileEntity().getWorld().getTotalWorldTime());
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = DimensionalFocusCasing.DimensionInjection.getTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE_GLOW : FRONT_GLOW).extFacing().glow().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.kerr_newman.machine_type
        // # Multiblock Mixer
        // # zh_CN 多方块搅拌机
        addModularMachineType(tt, StatCollector.translateToLocal("gtng.kerr_newman.machine_type"))
            // #tr gtng.kerr_newman.function
            // # Processes mixer recipes in parallel
            // # zh_CN 并行处理搅拌机配方
            .addInfo(StatCollector.translateToLocal("gtng.kerr_newman.function")).addStaticSpeedInfo(SPEED_MULTIPLIER)
            // #tr gtng.kerr_newman.render
            // # The central black hole grows during operation and shrinks after stopping
            // # zh_CN 工作时中央黑洞逐渐展开，停机后收缩
            .addInfo(StatCollector.translateToLocal("gtng.kerr_newman.render"));
        addModularTooltip(tt);
        tt.beginStructureBlock(KerrNewmanHomogenizerStructure.WIDTH, KerrNewmanHomogenizerStructure.HEIGHT,
            KerrNewmanHomogenizerStructure.LENGTH, false)
            // #tr gtng.kerr_newman.controller
            // # Centered, 8th block from the front, 13th layer from the top
            // # zh_CN 正面中央，向内第 8 格，自顶向下第 13 层
            .addController(StatCollector.translateToLocal("gtng.kerr_newman.controller"))
            // #tr gtng.kerr_newman.hatch_location
            // # Dimension Injection Casings (hint 1)
            // # zh_CN 维度注入机械方块（提示点 1）
            .addEnergyHatch("1+", StatCollector.translateToLocal("gtng.kerr_newman.hatch_location"), 1)
            .addInputBus("1-24", StatCollector.translateToLocal("gtng.kerr_newman.hatch_location"), 1)
            .addOutputBus("1-24", StatCollector.translateToLocal("gtng.kerr_newman.hatch_location"), 1)
            .addInputHatch("1-24", StatCollector.translateToLocal("gtng.kerr_newman.hatch_location"), 1)
            .addOutputHatch("1-24", StatCollector.translateToLocal("gtng.kerr_newman.hatch_location"), 1);
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }
}
