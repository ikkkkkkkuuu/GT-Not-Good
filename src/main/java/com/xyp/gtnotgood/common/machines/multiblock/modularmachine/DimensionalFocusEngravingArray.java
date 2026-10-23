package com.xyp.gtnotgood.common.machines.multiblock.modularmachine;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.xyp.gtnotgood.client.render.DimensionalFocusCoreRenderer;
import com.xyp.gtnotgood.common.blocks.dimensionalfocus.DimensionalFocusCasing;
import com.xyp.gtnotgood.common.blocks.fieldextruder.FieldExtruderCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.DimensionalFocusEngravingArrayGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGModularMachineBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.DimensionalFocusEngravingStructure;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;
import com.xyp.gtnotgood.common.recipe.machine.DimensionalFocusResearch;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.IGTHatchAdder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.ParallelHelper;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.render.IMTERenderer;
import gtPlusPlus.core.material.MaterialsElements;
import io.netty.buffer.ByteBuf;
import tectech.thing.metaTileEntity.hatch.MTEHatchDataInput;
import tectech.thing.metaTileEntity.hatch.MTEHatchDataItemsInput;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class DimensionalFocusEngravingArray extends GTNGModularMachineBase<DimensionalFocusEngravingArray>
    implements ISurvivalConstructable, IMTERenderer {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "fieldextruder/controller_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_glow");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "fieldextruder/controller_front_active_glow");

    private IStructureDefinition<DimensionalFocusEngravingArray> definition;
    private final List<MTEHatchDataAccess> researchHatches = new ArrayList<>();
    private final List<MTEHatchDataInput> computationHatches = new ArrayList<>();
    private HeatingCoilLevel coilLevel = HeatingCoilLevel.None;
    private long requiredComputation;
    private boolean activeCoils;
    private int[][] superconductorPositions;
    private ExtendedFacing coilFacing;
    private boolean coreRendering;
    private int lastCoreState = -1;
    private int coreRenderPhase;
    private long corePhaseEpoch;

    public DimensionalFocusEngravingArray(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public DimensionalFocusEngravingArray(String name) {
        super(name);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new DimensionalFocusEngravingArrayGui(this);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.dimensional_focus.name
        // # Dimensional Focus Engraving Array
        // # zh_CN 维度聚焦激光蚀刻阵列
        return "gtng.dimensional_focus.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new DimensionalFocusEngravingArray(mName);
    }

    @Override
    public IStructureDefinition<DimensionalFocusEngravingArray> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<DimensionalFocusEngravingArray>builder()
                .addShape("main", transpose(DimensionalFocusEngravingStructure.shape()))
                .addElement('A', DimensionalFocusCasing.DimensionallyTranscendent.asElement())
                .addElement('B', DimensionalFocusCasing.FusionGlass.asElement())
                .addElement('C', DimensionalFocusCasing.DimensionInjection.asElement())
                .addElement('D', DimensionalFocusCasing.MolecularCoil.asElement())
                .addElement('E', FieldExtruderCasing.ContainmentFieldGenerator.asElement())
                .addElement('F', DimensionalFocusCasing.ImprovedSuperconductor.asElement())
                .addElement('G',
                    ofChain(
                        GTStructureUtility.ofHatchAdder(DimensionalFocusEngravingArray::addModularHatchToMachineList,
                            DimensionalFocusCasing.LaserSafe.getTextureId(), 1),
                        GTStructureUtility.ofHatchAdder(DimensionalFocusEngravingArray::addMaintenanceToMachineList,
                            DimensionalFocusCasing.LaserSafe.getTextureId(), 1),
                        buildHatchAdder(DimensionalFocusEngravingArray.class)
                            .atLeast(InputBus, InputHatch, OutputBus, Energy.or(ExoticEnergy), DataHatch.Research,
                                DataHatch.Computation)
                            .casingIndex(DimensionalFocusCasing.LaserSafe.getTextureId()).hint(1)
                            .buildAndChain(DimensionalFocusCasing.LaserSafe.asElement())))
                .addElement('H',
                    GTStructureChannels.HEATING_COIL.use(GTStructureUtility.ofCoil(
                        DimensionalFocusEngravingArray::setCoilLevel, DimensionalFocusEngravingArray::getCoilLevel)))
                .build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, DimensionalFocusEngravingStructure.OFFSET_X,
            DimensionalFocusEngravingStructure.OFFSET_Y, DimensionalFocusEngravingStructure.OFFSET_Z);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1
            : survivalBuildPiece("main", stack, DimensionalFocusEngravingStructure.OFFSET_X,
                DimensionalFocusEngravingStructure.OFFSET_Y, DimensionalFocusEngravingStructure.OFFSET_Z, budget, env,
                false, true);
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        researchHatches.clear();
        computationHatches.clear();
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        coilLevel = HeatingCoilLevel.None;
        if (
            !checkPiece("main", DimensionalFocusEngravingStructure.OFFSET_X,
                DimensionalFocusEngravingStructure.OFFSET_Y, DimensionalFocusEngravingStructure.OFFSET_Z, errors)
        ) return;
        checkHasInputBus(errors);
        checkHasInputHatch(errors);
        checkHasOutputBus(errors);
        checkHasAnyEnergy(errors);
        checkMaximum(errors, InputBus, mInputBusses.size() + mDualInputHatches.size(), 24);
        checkMaximum(errors, InputHatch, mInputHatches.size(), 2);
        checkMaximum(errors, OutputBus, mOutputBusses.size(), 2);
        for (DataHatch hatch : DataHatch.values()) {
            int count = (int) hatch.count(this);
            if (count != 1) errors.add(StructureErrors.hatchCount(ErrorType.NOT_MATCH,
                TranslatableText.lang(hatch.getDescriptionLangKey()), count, 1));
        }
        checkModularHatches(errors);
    }

    @Override
    protected void onStructureCheckFinished(IGregTechTileEntity tile) {
        super.onStructureCheckFinished(tile);
        updateSuperconductors(tile, true);
        updateCoreRendering(tile, tile.getTimer());
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) {
            updateSuperconductors(tile, false);
            updateCoreRendering(tile, tick);
        }
    }

    private void updateCoreRendering(IGregTechTileEntity tile, long tick) {
        coreRendering = mMachine && tile.isActive()
            && mMaxProgresstime > 0
            && coilLevel.getTier() >= 0
            && getAvailableComputation() >= getRequiredComputation();
        coreRenderPhase = (int) Math.floorMod(tick, 100L);
        corePhaseEpoch = tile.getWorld().getTotalWorldTime();
        int state = coilLevel.ordinal() | (coreRendering ? 16 : 0);
        if (state != lastCoreState) {
            lastCoreState = state;
            tile.issueTileUpdate();
        }
    }

    public boolean isCoreRendering() {
        return coreRendering;
    }

    public Vec3Impl getCoreRenderOffset() {
        int[] offset = new int[3];
        getExtendedFacing().getWorldOffset(new int[] { 0, 18, 22 }, offset);
        return new Vec3Impl(offset[0], offset[1], offset[2]);
    }

    public int getCoreRenderRgb() {
        short[] color = switch (coilLevel) {
            case LV -> Materials.Cupronickel.getRGBA();
            case MV -> Materials.Kanthal.getRGBA();
            case HV -> Materials.Nichrome.getRGBA();
            case EV -> Materials.TPV.getRGBA();
            case IV -> Materials.HSSG.getRGBA();
            case LuV -> Materials.HSSS.getRGBA();
            case ZPM -> Materials.Naquadah.getRGBA();
            case UV -> Materials.NaquadahAlloy.getRGBA();
            case UHV -> Materials.Trinium.getRGBA();
            case UEV -> Materials.ElectrumFlux.getRGBA();
            case UIV -> Materials.DraconiumAwakened.getRGBA();
            case UMV -> Materials.Infinity.getRGBA();
            case UXV -> MaterialsElements.STANDALONE.HYPOGEN.getRGBA();
            case MAX -> Materials.Eternity.getRGBA();
            default -> new short[] { 255, 255, 255, 255 };
        };
        return (color[0] & 255) << 16 | (color[1] & 255) << 8 | color[2] & 255;
    }

    public long getCoreRenderTimer() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        // Client tile ticking can be disabled while the world clock keeps advancing.
        return coreRenderPhase
            + (tile == null || tile.getWorld() == null ? 0 : tile.getWorld().getTotalWorldTime() - corePhaseEpoch);
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeByte(coilLevel.ordinal());
        buffer.writeBoolean(coreRendering);
        buffer.writeByte(coreRenderPhase);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        int level = buffer.readUnsignedByte();
        boolean working = buffer.readBoolean();
        coreRenderPhase = buffer.readUnsignedByte() % 100;
        coilLevel = level < HeatingCoilLevel.values().length ? HeatingCoilLevel.values()[level] : HeatingCoilLevel.None;
        coreRendering = working && coilLevel.getTier() >= 0;
        corePhaseEpoch = getBaseMetaTileEntity().getWorld().getTotalWorldTime();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        Vec3Impl offset = getCoreRenderOffset();
        double centerX = x + offset.get0() + 0.5;
        double centerY = y + offset.get1() + 0.5;
        double centerZ = z + offset.get2() + 0.5;
        return AxisAlignedBB.getBoundingBox(Math.min(x, centerX - 8), Math.min(y, centerY - 8),
            Math.min(z, centerZ - 8), Math.max(x + 1, centerX + 8), Math.max(y + 1, centerY + 8),
            Math.max(z + 1, centerZ + 8));
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 256D * 256D;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float partialTicks) {
        if (!coreRendering) return;
        Vec3Impl offset = getCoreRenderOffset();
        DimensionalFocusCoreRenderer.render(x + offset.get0() + 0.5, y + offset.get1() + 0.5, z + offset.get2() + 0.5,
            getCoreRenderRgb(), getCoreRenderTimer(), partialTicks);
    }

    private void updateSuperconductors(IGregTechTileEntity tile, boolean force) {
        boolean active = mMachine && tile.isActive();
        ExtendedFacing facing = getExtendedFacing();
        if (!force && active == activeCoils && facing == coilFacing) return;
        activeCoils = active;
        if (superconductorPositions == null) {
            List<int[]> positions = new ArrayList<>();
            String[][] shape = DimensionalFocusEngravingStructure.shape();
            for (int row = 0; row < shape.length; row++) for (int depth = 0; depth < shape[row].length; depth++)
                for (int col = 0; col < shape[row][depth].length(); col++) if (
                    shape[row][depth].charAt(col) == 'F'
                ) positions.add(new int[] { col - DimensionalFocusEngravingStructure.OFFSET_X,
                    row - DimensionalFocusEngravingStructure.OFFSET_Y,
                    depth - DimensionalFocusEngravingStructure.OFFSET_Z });
            superconductorPositions = positions.toArray(new int[0][]);
        }
        if (coilFacing != null && coilFacing != facing) setSuperconductors(tile, coilFacing, false);
        coilFacing = facing;
        setSuperconductors(tile, facing, active);
    }

    private void setSuperconductors(IGregTechTileEntity tile, ExtendedFacing facing, boolean active) {
        int[] offset = new int[3];
        for (int[] position : superconductorPositions) {
            facing.getWorldOffset(position, offset);
            int x = tile.getXCoord() + offset[0];
            int y = tile.getYCoord() + offset[1];
            int z = tile.getZCoord() + offset[2];
            if (tile.getWorld().blockExists(x, y, z))
                DimensionalFocusCasing.ImprovedSuperconductor.getBlock().setActive(tile.getWorld(), x, y, z, active);
        }
    }

    @Override
    public void onRemoval() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide()) {
            mMachine = false;
            updateSuperconductors(tile, true);
            updateCoreRendering(tile, tile.getTimer());
        }
        super.onRemoval();
    }

    @Override
    public void onUnload() {
        coreRendering = false;
        lastCoreState = -1;
        super.onUnload();
    }

    private static void checkMaximum(List<StructureError> errors, HatchElement hatch, int count, int maximum) {
        if (count > maximum) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, hatch, count, maximum));
    }

    public HeatingCoilLevel getCoilLevel() {
        return coilLevel;
    }

    public void setCoilLevel(HeatingCoilLevel level) {
        coilLevel = level;
    }

    public static int parallelForHeat(long heat) {
        return heat <= 0 ? 1 : (int) Math.min(Integer.MAX_VALUE, Math.pow(2, heat / 900.0));
    }

    @Override
    protected int getBaseParallelRecipes() {
        return parallelForHeat(coilLevel.getHeat());
    }

    @Override
    protected String getBaseParallelDescription() {
        // #tr gtng.dimensional_focus.parallel
        // # Base parallel: 2^(coil temperature / 900), rounded down
        // # zh_CN 基础并行为 2^(线圈温度 / 900)，向下取整
        return StatCollector.translateToLocal("gtng.dimensional_focus.parallel");
    }

    public List<MTEHatchDataAccess> getResearchHatches() {
        return Collections.unmodifiableList(researchHatches);
    }

    public List<MTEHatchDataInput> getComputationHatches() {
        return Collections.unmodifiableList(computationHatches);
    }

    public long getAvailableComputation() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null) return 0;
        Vec3Impl position = new Vec3Impl(tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        long available = 0;
        for (MTEHatchDataInput hatch : validMTEList(computationHatches)) {
            if (hatch.q == null) continue;
            Long value = hatch.q.contentIfNotInTrace(position);
            if (value != null && value > 0) available += Math.min(Long.MAX_VALUE - available, value);
        }
        return available;
    }

    public long getRequiredComputation() {
        return getModuleWorks().isEmpty() && mMaxProgresstime > 0 ? requiredComputation : getModuleUsedComputation();
    }

    @Override
    protected long getModuleComputationCapacity() {
        return getAvailableComputation();
    }

    @Override
    protected long getStartedModuleComputation() {
        return requiredComputation;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTNGRecipeMaps.DimensionalFocusRecipes;
    }

    @Override
    protected ProcessingLogic createMachineProcessingLogic() {
        return new ModularProcessingLogic() {

            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (!DimensionalFocusResearch.hasResearch(recipe, researchHatches))
                    return CheckRecipeResultRegistry.NO_DATA_STICKS;
                long computation = DimensionalFocusResearch.requiredComputation(recipe);
                if (getAvailableModuleComputation() < computation) {
                    // #tr GT5U.gui.text.recipe_result.gtng_focus_computation
                    // # Insufficient computation
                    // # zh_CN 算力不足
                    return SimpleCheckRecipeResult.ofFailure("gtng_focus_computation");
                }
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }

            @Override
            protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
                long computation = DimensionalFocusResearch.requiredComputation(recipe);
                int limit = computation <= 0 ? maxParallel
                    : (int) Math.min(maxParallel, getAvailableModuleComputation() / computation);
                ParallelHelper helper = super.createParallelHelper(recipe).setMaxParallel(limit);
                if (computation > 0) helper.setMaxParallelCalculator((candidate, maximum, fluids, items) -> candidate
                    .maxParallelCalculatedByInputs(Math.min(maximum, limit), fluids, items));
                return helper;
            }

            @Override
            protected CheckRecipeResult onRecipeStart(@Nonnull GTRecipe recipe) {
                requiredComputation = DimensionalFocusResearch.requiredComputation(recipe) * calculatedParallels;
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        }.setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList()))
            voltage = Math.max(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
        logic.setAvailableVoltage(voltage).setAvailableAmperage(getMaxInputAmps()).setAmperageOC(false);
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (getAvailableComputation() < getRequiredComputation()) return false;
        return super.onRunningTick(stack);
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public void setBatchMode(boolean enabled) {
        super.setBatchMode(false);
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean isRecipeLockingEnabled() {
        return false;
    }

    @Override
    public void setRecipeLocking(boolean enabled) {
        super.setRecipeLocking(false);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setLong("focusRequiredComputation", requiredComputation);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        requiredComputation = Math.max(0, tag.getLong("focusRequiredComputation"));
        batchMode = false;
        mLockedToSingleRecipe = false;
        setSingleRecipeCheck(null);
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, super.getInfoData());
        // #tr gtng.dimensional_focus.info
        // # Coil parallel: %s; Computation: %s / %s
        // # zh_CN 线圈并行：%s；算力：%s / %s
        lines.add(StatCollector.translateToLocalFormatted("gtng.dimensional_focus.info", getMaxParallelRecipes(),
            getAvailableComputation(), getRequiredComputation()));
        return lines.toArray(new String[0]);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = DimensionalFocusCasing.LaserSafe.getTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE_GLOW : FRONT_GLOW).extFacing().glow().build() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        addModularMachineType(tt, StatCollector.translateToLocal(getRecipeMap().unlocalizedName))
            // #tr gtng.dimensional_focus.function
            // # Engraves native laser recipes using photoresist
            // # zh_CN 使用光刻胶加工激光蚀刻配方
            .addInfo(StatCollector.translateToLocal("gtng.dimensional_focus.function"))
            // #tr gtng.dimensional_focus.speed
            // # Base performance is relative to original laser engraving recipes; lenses are not consumed
            // # zh_CN 基础性能相对原始激光蚀刻配方；透镜不消耗
            .addInfo(StatCollector.translateToLocal("gtng.dimensional_focus.speed")).addStaticSpeedInfo(5F)
            .addStaticEuEffInfo(4F)
            // #tr gtng.dimensional_focus.research
            // # UHV+ source recipes require scanned output data and computation
            // # zh_CN 原始电压为 UHV 及以上的配方需要产物研究数据与算力
            .addInfo(StatCollector.translateToLocal("gtng.dimensional_focus.research"))
            // #tr gtng.dimensional_focus.scan
            // # Scan one output with %s; store the result in the Data Access Hatch
            // # zh_CN 使用 %s 扫描一份产物，将研究数据放入数据访问仓
            .addInfo(StatCollector.translateToLocalFormatted("gtng.dimensional_focus.scan",
                ItemList.Tool_DataStick.get(1).getDisplayName()))
            // #tr gtng.dimensional_focus.stall
            // # Computation scales with parallel; insufficient computation pauses progress and EU use
            // # zh_CN 算力需求随并行数增加；算力不足时暂停进度与耗电
            .addInfo(StatCollector.translateToLocal("gtng.dimensional_focus.stall"));
        addModularTooltip(tt);
        tt.beginStructureBlock(89, 89, 89, false)
            // #tr gtng.dimensional_focus.controller
            // # Center line, layer 27 from the top, 23 blocks from the front
            // # zh_CN 中央轴线，自顶向下第 27 层，距前端第 23 格
            .addController(StatCollector.translateToLocal("gtng.dimensional_focus.controller"))
            .addCasing("43", DimensionalFocusCasing.DimensionallyTranscendent.getStack(1).getDisplayName(), false)
            .addCasing("976", DimensionalFocusCasing.FusionGlass.getStack(1).getDisplayName(), false)
            .addCasing("1821", DimensionalFocusCasing.DimensionInjection.getStack(1).getDisplayName(), false)
            .addCasing("462", DimensionalFocusCasing.MolecularCoil.getStack(1).getDisplayName(), false)
            .addCasing("96", FieldExtruderCasing.ContainmentFieldGenerator.getStack(1).getDisplayName(), false)
            .addCasing("144", DimensionalFocusCasing.ImprovedSuperconductor.getStack(1).getDisplayName(), false)
            .addCasing("0-3192", DimensionalFocusCasing.LaserSafe.getStack(1).getDisplayName(), false)
            // #tr gtng.dimensional_focus.hatch_positions
            // # 3199 Laser-Safe Engraving Casing or hatch positions in total (hint 1)
            // # zh_CN 共 3199 处激光安全蚀刻机械方块或舱室位置（提示点 1）
            .addStructureInfo(StatCollector.translateToLocal("gtng.dimensional_focus.hatch_positions"))
            // #tr gtng.dimensional_focus.coils
            // # 3312 heating coils, all of the same type
            // # zh_CN 3312 个同种加热线圈
            .addStructureInfo(StatCollector.translateToLocal("gtng.dimensional_focus.coils"))
            .addInputBus("1-24", DimensionalFocusCasing.LaserSafe.getStack(1).getDisplayName(), 1)
            .addInputHatch("1-2", DimensionalFocusCasing.LaserSafe.getStack(1).getDisplayName(), 1)
            .addOutputBus("1-2", DimensionalFocusCasing.LaserSafe.getStack(1).getDisplayName(), 1)
            .addEnergyHatch("1+", DimensionalFocusCasing.LaserSafe.getStack(1).getDisplayName(), 1)
            // #tr gtng.dimensional_focus.data_hatches
            // # Exactly 1 Data Access Hatch and 1 Computation Input Hatch on Laser-Safe Casings
            // # zh_CN 激光安全蚀刻机械方块处恰好安装 1 个数据访问仓和 1 个算力输入仓
            .addStructureInfo(StatCollector.translateToLocal("gtng.dimensional_focus.data_hatches"));
        addModularStructureTooltip(tt).toolTipFinisher();
        return tt;
    }

    private enum DataHatch implements IHatchElement<DimensionalFocusEngravingArray> {

        Research(MTEHatchDataAccess.class,
            // #tr gtng.dimensional_focus.research_hatch
            // # Data Access Hatch
            // # zh_CN 数据访问仓
            "gtng.dimensional_focus.research_hatch"),
        Computation(MTEHatchDataInput.class,
            // #tr gtng.dimensional_focus.computation_hatch
            // # Computation Input Hatch
            // # zh_CN 算力输入仓
            "gtng.dimensional_focus.computation_hatch");

        private final Class<? extends IMetaTileEntity> hatchClass;
        private final String key;

        DataHatch(Class<? extends IMetaTileEntity> hatchClass, String key) {
            this.hatchClass = hatchClass;
            this.key = key;
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return Collections.singletonList(hatchClass);
        }

        @Override
        public IGTHatchAdder<? super DimensionalFocusEngravingArray> adder() {
            return (machine, tile, texture) -> {
                if (tile == null || !matchesHatch(tile.getMetaTileEntity())) return false;
                var hatch = tile.getMetaTileEntity();
                ((MTEHatch) hatch).updateTexture(texture);
                machine.addIfSmartInput(hatch);
                if (this == Research) return machine.researchHatches.add((MTEHatchDataAccess) hatch);
                return machine.computationHatches.add((MTEHatchDataInput) hatch);
            };
        }

        @Override
        public boolean matchesHatch(IMetaTileEntity hatch) {
            return hatchClass.isInstance(hatch) && !(hatch instanceof MTEHatchDataItemsInput);
        }

        @Override
        public long count(DimensionalFocusEngravingArray machine) {
            return this == Research ? machine.researchHatches.size() : machine.computationHatches.size();
        }

        @Override
        public String getDisplayName() {
            return StatCollector.translateToLocal(key);
        }

        @Override
        public String getDescriptionLangKey() {
            return key;
        }
    }
}
