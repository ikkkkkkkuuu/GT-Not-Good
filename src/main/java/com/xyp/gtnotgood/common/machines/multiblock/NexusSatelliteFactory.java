// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.util.GTRecipeConstants.CLEANROOM;
import static gregtech.api.util.GTRecipeConstants.LOW_GRAVITY;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.NexusSatelliteFactoryGui;
import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;
import com.xyp.gtnotgood.common.machines.hatch.lighthunter.SatelliteThreadModifierHatch;
import com.xyp.gtnotgood.common.machines.hatch.lighthunter.ThreadModifierHost;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.NexusSatelliteFactoryStructure;
import com.xyp.gtnotgood.common.machines.processing.LightHunterBatchProcessingLogic;
import com.xyp.gtnotgood.common.machines.processing.LightHunterEnergy;
import com.xyp.gtnotgood.common.machines.processing.ModuleInputPreflight;
import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModList;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.GTMod;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class NexusSatelliteFactory extends GTNGMultiBlockBase<NexusSatelliteFactory>
    implements ThreadModifierHost, ISurvivalConstructable {

    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "lighthunter/module_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "lighthunter/module_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "lighthunter/module_front_active_glow");
    private static final IIconContainer FRONT_IDLE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "lighthunter/module_front_glow");
    private static IStructureDefinition<NexusSatelliteFactory> definition;
    private final int factoryType;
    private LightHunterSpaceStation host;
    private long nextHostScan;
    private int minimumDuration = 20;
    private WirelessOutputs batchOutputs;
    private boolean outputReady;
    private long batchParallels;
    private int batchRecipeCount;
    private boolean recipeTransactionOpen;
    private WirelessOutputs pendingBatch;
    private SatelliteThreadModifierHatch threadModifier;

    public NexusSatelliteFactory(int id, String name, String regionalName, int factoryType) {
        super(id, name, regionalName);
        this.factoryType = factoryType;
    }

    public NexusSatelliteFactory(String name, int factoryType) {
        super(name);
        this.factoryType = factoryType;
    }

    @Override
    public String getLocalName() {
        // #tr gtng.light_hunter.satellite.name
        // # Nexus Satellite Factory Mk%s
        // # zh_CN 枢纽卫星工厂 Mk%s
        return StatCollector.translateToLocalFormatted("gtng.light_hunter.satellite.name",
            new String[] { "I", "II", "III", "IV" }[factoryType]);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new NexusSatelliteFactory(mName, factoryType);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public IStructureDefinition<NexusSatelliteFactory> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<NexusSatelliteFactory>builder().addShape("main",
                transpose(NexusSatelliteFactoryStructure.shape()));
            for (char symbol : NexusSatelliteFactoryStructure.SYMBOLS.toCharArray()) if (
                symbol != 'O'
            ) builder.addElement(symbol,
                symbol == 'E' ? GTStructureUtility.activeCoils(NexusSatelliteFactoryStructure.element(symbol))
                    : NexusSatelliteFactoryStructure.element(symbol));
            builder.addElement('O',
                ofChain(
                    GTStructureUtility.ofHatchAdder(NexusSatelliteFactory::addThreadModifier,
                        LightHunterCasing.HighPowerCasing.getTextureId(), 1),
                    GTStructureUtility.ofHatchAdder(NexusSatelliteFactory::addMaintenanceToMachineList,
                        LightHunterCasing.HighPowerCasing.getTextureId(), 1),
                    buildHatchAdder(NexusSatelliteFactory.class)
                        .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Energy.or(ExoticEnergy))
                        .casingIndex(LightHunterCasing.HighPowerCasing.getTextureId()).hint(1)
                        .buildAndChain(LightHunterCasing.HighPowerCasing.asElement())));
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 2, 10, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 2, 10, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece("main", 2, 10, 0, errors)) return;
        checkHasAnyEnergy(errors);
        checkHatchExact(errors, Maintenance, 1);
        int energyHatches = LightHunterEnergy.countEnergyHatches(mEnergyHatches, mExoticEnergyHatches);
        int laserHatches = LightHunterEnergy.countLaserHatches(mExoticEnergyHatches);
        if (energyHatches > 2) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, Energy, energyHatches, 2));
        if (laserHatches > 1) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, ExoticEnergy, laserHatches, 1));
    }

    @Override
    public boolean addEnergyInputToMachineList(IGregTechTileEntity tile, int texture) {
        return !(tile.getMetaTileEntity() instanceof CrossRecipeWirelessEnergyHatch)
            && super.addEnergyInputToMachineList(tile, texture);
    }

    private boolean addThreadModifier(IGregTechTileEntity tile, int texture) {
        if (!(tile.getMetaTileEntity() instanceof SatelliteThreadModifierHatch hatch) || threadModifier != null)
            return false;
        if (!hatch.bind(this)) return false;
        threadModifier = hatch;
        hatch.updateTexture(texture);
        return true;
    }

    @Override
    public boolean isThreadStructureFormed() {
        return mMachine;
    }

    public boolean hasThreadModifier(SatelliteThreadModifierHatch hatch) {
        return threadModifier == hatch;
    }

    public int getAdditionalThreads() {
        return threadModifier == null ? 0 : threadModifier.getAdditionalThreads(this);
    }

    @Override
    public void clearHatches() {
        if (threadModifier != null) threadModifier.unbind(this);
        threadModifier = null;
        super.clearHatches();
    }

    @Override
    public void onUnload() {
        if (threadModifier != null) threadModifier.unbind(this);
        host = null;
        super.onUnload();
    }

    @Override
    public void onRemoval() {
        if (threadModifier != null) threadModifier.unbind(this);
        host = null;
        super.onRemoval();
    }

    public LightHunterSpaceStation getHost() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null) return null;
        if (host != null && host.acceptsModule(this)) return host;
        host = null;
        long tick = tile.getWorld().getTotalWorldTime();
        if (tick < nextHostScan) return null;
        nextHostScan = tick + 20;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH,
            ForgeDirection.WEST, ForgeDirection.EAST })
            for (int side : new int[] { -9, 9 }) for (int distance = 41; distance <= 113; distance += 8) {
                int[] offset = LightHunterSpaceStation.moduleOffset(facing, distance, side);
                int x = tile.getXCoord() - offset[0], z = tile.getZCoord() - offset[2], y = tile.getYCoord();
                if (!tile.getWorld().blockExists(x, y, z)) continue;
                TileEntity candidate = tile.getWorld().getTileEntity(x, y, z);
                if (
                    candidate instanceof IGregTechTileEntity gt
                        && gt.getMetaTileEntity() instanceof LightHunterSpaceStation station
                        && station.acceptsModule(this)
                ) return host = station;
            }
        return null;
    }

    public boolean isUnrestricted() {
        LightHunterSpaceStation station = getHost();
        return station != null && station.getAstralArrayCount() == 64;
    }

    private RecipeMap<?>[] getFactoryRecipeMaps() {
        return switch (factoryType) {
            case 1 -> new RecipeMap<?>[] { GTNGRecipeMaps.LightHunterRockBreakingRecipes, RecipeMaps.oreWasherRecipes,
                RecipeMaps.centrifugeNonCellRecipes, RecipeMaps.electrolyzerNonCellRecipes, RecipeMaps.sifterRecipes,
                RecipeMaps.maceratorRecipes, RecipeMaps.chemicalDehydratorNonCellRecipes,
                RecipeMaps.thermalCentrifugeRecipes, RecipeMaps.electroMagneticSeparatorRecipes };
            case 2 -> new RecipeMap<?>[] { RecipeMaps.chemicalDehydratorNonCellRecipes, RecipeMaps.autoclaveRecipes,
                RecipeMaps.extractorRecipes, RecipeMaps.fluidExtractionRecipes, RecipeMaps.brewingRecipes,
                RecipeMaps.fermentingRecipes, RecipeMaps.distilleryRecipes, RecipeMaps.distillationTowerRecipes,
                RecipeMaps.fluidHeaterRecipes, RecipeMaps.fluidSolidifierRecipes, RecipeMaps.chemicalBathRecipes };
            case 3 -> new RecipeMap<?>[] { RecipeMaps.cannerRecipes, RecipeMaps.arcFurnaceRecipes,
                GTNGRecipeMaps.LightningProcessorRecipes, RecipeMaps.assemblerRecipes,
                GoodGeneratorRecipeMaps.preciseAssemblerRecipes, RecipeMaps.circuitAssemblerRecipes };
            default -> new RecipeMap<?>[] { RecipeMaps.latheRecipes, RecipeMaps.benderRecipes,
                RecipeMaps.compressorRecipes, RecipeMaps.hammerRecipes, RecipeMaps.cutterRecipes,
                RecipeMaps.extruderRecipes, RecipeMaps.mixerNonCellRecipes, RecipeMaps.wiremillRecipes,
                RecipeMaps.formingPressRecipes, RecipeMaps.polarizerRecipes };
        };
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        RecipeMap<?>[] maps = getFactoryRecipeMaps();
        return maps[Math.max(0, Math.min(maps.length - 1, machineMode))];
    }

    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(getFactoryRecipeMaps());
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new LightHunterBatchProcessingLogic(this::isUnrestricted, () -> minimumDuration,
            this::getAdditionalThreads) {

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (
                    recipe.mSpecialValue == -200 || recipe.mSpecialValue == -300
                        || recipe.getMetadataOrDefault(CLEANROOM, false)
                ) {
                    ICleanroom room = cleanroomReference.getCleanroom();
                    if (room == null || !room.isValidCleanroom() || room.getCleanness() <= 0)
                        return SimpleCheckRecipeResult.ofFailure("gtng.cleanroom_required");
                }
                if (
                    GTMod.proxy.mLowGravProcessing
                        && (recipe.mSpecialValue == -100 || recipe.mSpecialValue == -300
                            || recipe.getMetadataOrDefault(LOW_GRAVITY, false))
                        && !MTEBasicMachine.isValidForLowGravity(recipe,
                            getBaseMetaTileEntity().getWorld().provider.dimensionId)
                ) {
                    // #tr GT5U.gui.text.recipe_result.gtng_light_hunter_low_gravity
                    // # Requires a low-gravity environment
                    // # zh_CN 需要低重力环境
                    return SimpleCheckRecipeResult.ofFailure("gtng_light_hunter_low_gravity");
                }
                if (getRecipeMap() == GoodGeneratorRecipeMaps.preciseAssemblerRecipes && recipe.mSpecialValue > 4)
                    return CheckRecipeResultRegistry.insufficientMachineTier(recipe.mSpecialValue);
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        };
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(LightHunterEnergy.totalInputPower(mEnergyHatches, mExoticEnergyHatches))
            .setAvailableAmperage(1).setAmperageOC(false);
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        if (batchOutputs != null) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        LightHunterSpaceStation station = getHost();
        if (station == null || !station.isStationWorking()) {
            // #tr GT5U.gui.text.recipe_result.gtng_light_hunter_host
            // # Requires an active Light Hunter Space Station
            // # zh_CN 需要运行中的猎光号空间站
            return SimpleCheckRecipeResult.ofFailure("gtng_light_hunter_host");
        }
        LightHunterBatchProcessingLogic logic = (LightHunterBatchProcessingLogic) processingLogic;
        setupProcessingLogic(logic);
        logic.prepareModuleRecipe(Long.MAX_VALUE, true);
        try {
            CheckRecipeResult result = doCheckRecipe();
            if (!result.wasSuccessful()) return result;
            if (!logic.canCommitInputs() || !ModuleInputPreflight.canCommit(this, logic))
                return CheckRecipeResultRegistry.NO_RECIPE;
            WirelessOutputs outputs = logic.getBatchOutputs();
            if (outputs == null || !outputs.canFlush(this)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            if (!logic.commitInputs()) return CheckRecipeResultRegistry.INTERNAL_ERROR;
            batchOutputs = outputs;
            if (recipeTransactionOpen) pendingBatch = outputs;
            outputReady = false;
            batchParallels = logic.getBatchParallelCount();
            batchRecipeCount = logic.getBatchRecipeCount();
            lEUt = -logic.getCalculatedEut();
            mMaxProgresstime = logic.getDuration();
            mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
            mEfficiencyIncrease = 10000;
            updateSlots();
            markDirty();
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } finally {
            logic.finishModuleRecipe();
        }
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        return lEUt >= 0 || drainEnergyInput(getActualEnergyUsage());
    }

    @Override
    public void startRecipeProcessing() {
        super.startRecipeProcessing();
        recipeTransactionOpen = true;
        pendingBatch = null;
    }

    @Override
    public void endRecipeProcessing() {
        try {
            super.endRecipeProcessing();
            if (pendingBatch != null && !getCheckRecipeResult().wasSuccessful() && batchOutputs == pendingBatch) {
                batchOutputs = null;
                mMaxProgresstime = mProgresstime = 0;
                lEUt = 0;
                batchParallels = 0;
                batchRecipeCount = 0;
                markDirty();
            }
        } finally {
            pendingBatch = null;
            recipeTransactionOpen = false;
        }
    }

    @Override
    protected void outputAfterRecipe() {
        outputReady = batchOutputs != null;
        flushBatch();
    }

    private void flushBatch() {
        if (outputReady && batchOutputs != null && batchOutputs.flush(this)) {
            batchOutputs = null;
            outputReady = false;
            scheduleRecipeCheckImmediate();
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && mMachine) {
            if (outputReady) flushBatch();
            if (tick % 20 == 0 && mMaxProgresstime == 0 && getHost() != null && host.isStationWorking())
                scheduleRecipeCheckImmediate();
        }
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        if (!outputReady) batchOutputs = null;
        super.stopMachine(reason);
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("lightHunterMinimumDuration", minimumDuration);
        tag.setBoolean("lightHunterOutputReady", outputReady);
        tag.setLong("lightHunterBatchParallels", batchParallels);
        tag.setInteger("lightHunterBatchRecipes", batchRecipeCount);
        if (batchOutputs != null) tag.setTag("lightHunterBatchOutputs", batchOutputs.save());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        minimumDuration = tag.hasKey("lightHunterMinimumDuration")
            ? Math.max(1, tag.getInteger("lightHunterMinimumDuration"))
            : 20;
        outputReady = tag.getBoolean("lightHunterOutputReady");
        batchParallels = Math.max(0, tag.getLong("lightHunterBatchParallels"));
        batchRecipeCount = Math.max(0, tag.getInteger("lightHunterBatchRecipes"));
        batchOutputs = tag.hasKey("lightHunterBatchOutputs", 10)
            ? WirelessOutputs.load(tag.getCompoundTag("lightHunterBatchOutputs"))
            : null;
        machineMode = Math.max(0, Math.min(getFactoryRecipeMaps().length - 1, machineMode));
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return (machineMode + 1) % getFactoryRecipeMaps().length;
    }

    @Override
    public void setMachineMode(int mode) {
        super.setMachineMode(Math.floorMod(mode, getFactoryRecipeMaps().length));
        for (var hatch : mInputHatches) hatch.mRecipeMap = getRecipeMap();
        for (var bus : mInputBusses) bus.mRecipeMap = getRecipeMap();
        setSingleRecipeCheck(null);
    }

    @Override
    public String getMachineModeName() {
        return StatCollector.translateToLocal(getRecipeMap().unlocalizedName);
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (getBaseMetaTileEntity().isClientSide()) return;
        if (player.isSneaking()) super.onScrewdriverRightClick(side, player, x, y, z, tool);
        else {
            setMachineMode(nextMachineMode());
            GTUtility.sendChatTrans(player, "gt.interact.desc.mb.mode", getMachineModeName());
        }
    }

    public int getMinimumDuration() {
        return minimumDuration;
    }

    public List<NBTTagCompound> getBatchOutputRows() {
        List<NBTTagCompound> rows = new ArrayList<>();
        if (batchOutputs != null)
            WirelessRecipeDisplay.appendRows(rows, batchOutputs, outputReady ? 0 : mMaxProgresstime, 128);
        return rows;
    }

    public void setMinimumDuration(int ticks) {
        minimumDuration = Math.max(1, ticks);
        markDirty();
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new NexusSatelliteFactoryGui(this);
    }

    @Override
    public int getMaxParallelRecipes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = LightHunterCasing.HighPowerCasing.getTexture();
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
            TextureFactory.builder().addIcon(active ? FRONT_GLOW : FRONT_IDLE_GLOW).extFacing().glow().build() };
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        int totalThreads = LightHunterRecipeRules.totalThreads(getAdditionalThreads());
        tag.setInteger("satelliteThreads", totalThreads);
        tag.setInteger("satelliteUsedThreads", getUsedThreads(totalThreads));
        tag.setInteger("satelliteMinimumDuration", minimumDuration);
        tag.setInteger("satelliteRecipes", batchOutputs == null ? 0 : batchRecipeCount);
        tag.setLong("satelliteParallels", batchOutputs == null ? 0 : batchParallels);
        tag.setBoolean("satelliteOutputReady", outputReady);
    }

    private int getUsedThreads(int totalThreads) {
        if (
            batchOutputs == null || outputReady
                || batchParallels <= 0
                || mMaxProgresstime <= 0
                || getBaseMetaTileEntity() == null
                || !getBaseMetaTileEntity().isActive()
        ) return 0;
        // Threads represent shared parallel capacity; unrestricted batches can exceed that capacity.
        long usedThreads = 1 + (batchParallels - 1) / Integer.MAX_VALUE;
        return (int) Math.min(totalThreads, usedThreads);
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey("satelliteThreads")) return;
        // #tr gtng.light_hunter.satellite.batch
        // # Batch: %s recipes, %s parallels
        // # zh_CN 当前批次：%s 种配方，%s 并行
        lines.add(StatCollector.translateToLocalFormatted("gtng.light_hunter.satellite.batch",
            tag.getInteger("satelliteRecipes"), NumberFormatUtil.formatNumber(tag.getLong("satelliteParallels"))));
        // #tr gtng.light_hunter.satellite.threads
        // # Threads (used / total): %s / %s
        // # zh_CN 线程（已用/总数）：%s / %s
        lines.add(StatCollector.translateToLocalFormatted("gtng.light_hunter.satellite.threads",
            tag.getInteger("satelliteUsedThreads"), tag.getInteger("satelliteThreads")));
        // #tr gtng.light_hunter.satellite.minimum_duration_value
        // # Minimum batch duration: %s ticks
        // # zh_CN 最短批次时间：%s tick
        lines.add(StatCollector.translateToLocalFormatted("gtng.light_hunter.satellite.minimum_duration_value",
            NumberFormatUtil.formatNumber(tag.getInteger("satelliteMinimumDuration"))));
        if (tag.getBoolean("satelliteOutputReady"))
            lines.add(StatCollector.translateToLocal("gtng.modular.gui.waiting_output"));
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.light_hunter.satellite.recipe_pools
        // # Available recipe pools:
        // # zh_CN 可用配方池：
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.recipe_pools"));
        for (RecipeMap<?> recipeMap : getFactoryRecipeMaps()) {
            tt.addInfo("  " + EnumChatFormatting.YELLOW
                + StatCollector.translateToLocal(recipeMap.unlocalizedName)
                + EnumChatFormatting.RESET);
        }
        if (factoryType == 3) {
            // #tr gtng.light_hunter.satellite.precise_tier
            // # Supports Precise Assembler recipes up to Mk-4
            // # zh_CN 精密组装配方最高支持 Mk-4
            tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.precise_tier"));
        }
        tt.addSeparator();
        // #tr gtng.light_hunter.satellite.purpose
        // # Combines matching recipes in the selected mode into one batch
        // # zh_CN 将所选模式下匹配的多种配方合为一批处理
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.purpose"));
        // #tr gtng.light_hunter.satellite.host
        // # New batches require an active Light Hunter Space Station
        // # zh_CN 需要运行中的猎光号空间站才能启动新批次
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.host"));
        // #tr gtng.light_hunter.satellite.allocation
        // # Base: 128 threads; matching recipes share 274,877,906,816 parallels before 64 host arrays
        // # zh_CN 基础 128 线程；主站未满 64 星阵时，匹配配方分摊 274,877,906,816 并行
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.allocation"));
        // #tr gtng.light_hunter.satellite.thread_modifier
        // # Accepts one optional thread modifier; inserted arrays raise the shared parallel quota
        // # zh_CN 可选装 1 个线程修正仓；仓内星阵提高共享并行配额
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.thread_modifier"));
        // #tr gtng.light_hunter.satellite.energy
        // # Retains recipe energy cost; batch time depends on total input power and minimum duration
        // # zh_CN 保留配方总能耗；按总输入功率和最短批次时间计算耗时
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.energy"));
        // #tr gtng.light_hunter.satellite.controls
        // # Screwdriver: cycle modes; parallel settings: minimum batch duration (default 20 ticks)
        // # zh_CN 螺丝刀轮换模式；并行设置中调整最短批次时间（默认 20 tick）
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.satellite.controls"));
        tt.beginStructureBlock(5, 21, 13, false);
        // #tr gtng.light_hunter.satellite.hatches
        // # High Power Casings; up to 2 normal/multi-amp Energy Hatches and 1 Laser Hatch; 1 Maintenance Hatch
        // # zh_CN 高功率机械方块位置；最多 2 个普通/多安能源仓和 1 个激光仓；1 个维护仓
        String positions = StatCollector.translateToLocal("gtng.light_hunter.satellite.hatches");
        return tt.addInputBus(positions, 1).addOutputBus(positions, 1).addInputHatch(positions, 1)
            .addOutputHatch(positions, 1).addEnergyHatch(positions, 1).addMaintenanceHatch(positions, 1)
            .toolTipFinisher();
    }
}
