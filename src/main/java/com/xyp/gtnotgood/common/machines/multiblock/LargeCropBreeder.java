package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.cropsnh.init.CropsNHBlockTextures.OVERLAY_FRONT_CROP_BREEDER;
import static com.gtnewhorizon.cropsnh.init.CropsNHBlockTextures.OVERLAY_FRONT_CROP_BREEDER_ACTIVE;
import static com.gtnewhorizon.cropsnh.init.CropsNHBlockTextures.OVERLAY_FRONT_CROP_BREEDER_ACTIVE_GLOW;
import static com.gtnewhorizon.cropsnh.init.CropsNHBlockTextures.OVERLAY_FRONT_CROP_BREEDER_GLOW;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.GregTechAPI.sBlockCasings2;
import static gregtech.api.GregTechAPI.sBlockFrames;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ICropMutation;
import com.gtnewhorizon.cropsnh.api.IMutationPool;
import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.api.ISeedStats;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.MutationRegistry;
import com.gtnewhorizon.cropsnh.tileentity.TileEntityCropSticks;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeCropBreederGui;
import com.xyp.gtnotgood.common.machines.crop.CropArchive;
import com.xyp.gtnotgood.common.machines.crop.CropBreedingPlanner;
import com.xyp.gtnotgood.common.machines.crop.CropMutationMachineRequirements;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * Large electric CropsNH crop breeder.
 * <p>
 * This controller preserves the original archive-driven crop breeding workflow from the steam machine while replacing
 * the steam hatch model with normal GregTech EU hatches. Analyzed seeds inserted into input buses unlock crop ids in a
 * persistent archive; each inserted seed also adds one pending output request. The target input then drives either a
 * deterministic mutation chain or the CropsNH mutation pool fallback until the requested target can be output.
 */
public class LargeCropBreeder extends GTNGMultiBlockBase<LargeCropBreeder> implements ISurvivalConstructable {

    private static final int MODE_DETERMINISTIC = 0;
    private static final int MODE_MUTATION_POOL = 1;
    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFF_SET = 2;
    private static final int VERTICAL_OFF_SET = 2;
    private static final int DEPTH_OFF_SET = 0;
    private static final int POOL_BREEDING_EUT = 32;
    private static final int POOL_BREEDING_DURATION = 20 * 20;
    private static final int MAX_ARCHIVE_STEPS_PER_CYCLE = 1;
    private static final byte OPERATION_NONE = 0;
    private static final byte OPERATION_ARCHIVE_BREEDING = 1;
    private static final byte OPERATION_OUTPUT = 2;
    private static final String NBT_TARGET_CROP_ID = "targetCropId";

    // 5 wide (x), 4 tall (y), 3 deep (z), following the CropsNH Industrial Farm greenhouse silhouette.
    // A = glass, B = steel casing and hatches, C = steel pipe casing/seed bed, D = steel frame.
    private static final String[][] SHAPE = new String[][] { { " DBD ", " ACA ", " DBD " },
        { "DBBBD", "A   A", "DBBBD" }, { "DB~BD", "DCCCD", "DBBBD" }, { "D   D", "     ", "D   D" } };

    private int casingCount;
    private IStructureDefinition<LargeCropBreeder> structureDefinition;
    private String targetCropId = "";
    private boolean targetInputValid = true;
    private CropArchive cropArchive = new CropArchive();
    private CropBreedingPlanner.Plan breedingPlan = CropBreedingPlanner.Plan.empty("");
    private List<CropBreedingPlanner.Step> breedingChain = new ArrayList<>();
    private boolean breedingPlanDirty = true;
    private byte activeOperation = OPERATION_NONE;
    private String activeCropId = "";
    private int activeGrowth;
    private int activeGain;
    private int activeResistance;
    private int pendingSeedOutputs;
    private String missingCropId = "";
    private boolean allTasksBlocked;
    private boolean missingBreedingRequirements;
    private int chainTotalSteps;
    private int chainCompletedSteps;
    private int syncedArchiveSize;
    private String syncedMissingInfo = "";
    private List<String> syncedArchiveCrops = Collections.emptyList();
    private List<NBTTagCompound> syncedChainSteps = Collections.emptyList();
    private boolean displayDirty = true;

    public LargeCropBreeder(String name) {
        super(name);
    }

    public LargeCropBreeder(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new LargeCropBreeder(this.mName);
    }

    public String getMachineType() {
        // #tr LargeCropBreederMachineType
        // # Crop Breeding
        // # zh_CN 作物杂交
        return StatCollector.translateToLocal("LargeCropBreederMachineType");
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeCropBreederGui(this);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public String getMachineModeName() {
        if (machineMode == MODE_MUTATION_POOL) {
            // #tr LargeCropBreederModeMutationPool
            // # Mutation Pool Breeding
            // # zh_CN 杂交池杂交
            return StatCollector.translateToLocal("LargeCropBreederModeMutationPool");
        }
        // #tr LargeCropBreederModeDeterministic
        // # Deterministic Breeding
        // # zh_CN 确定性杂交
        return StatCollector.translateToLocal("LargeCropBreederModeDeterministic");
    }

    @Override
    public void setMachineMode(int index) {
        super.setMachineMode(index == MODE_MUTATION_POOL ? MODE_MUTATION_POOL : MODE_DETERMINISTIC);
        markDisplayDirty();
    }

    @Override
    public IStructureDefinition<LargeCropBreeder> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<LargeCropBreeder>builder()
                .addShape(STRUCTURE_PIECE_MAIN, transpose(SHAPE))
                .addElement(
                    'B',
                    ofChain(
                        buildHatchAdder(LargeCropBreeder.class)
                            .atLeast(InputBus, OutputBus, Energy.or(ExoticEnergy), Maintenance)
                            .casingIndex(getSteelCasingTextureId())
                            .hint(1)
                            .buildAndChain(),
                        onElementPass(machine -> ++machine.casingCount, ofBlock(sBlockCasings2, 0))))
                .addElement('A', ofBlock(ItemRegistry.bw_realglas, 0))
                .addElement('C', ofBlock(sBlockCasings2, 13))
                .addElement('D', ofBlock(sBlockFrames, (int) Materials.Steel.mMetaItemSubID))
                .build();
        }
        return structureDefinition;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity baseTileEntity, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET, errors)) return;
        checkCasingMin(errors, casingCount, 3);
        if (mInputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, InputBus, 0, 1));
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputBus, 0, 1));
        int energyHatchCount = mEnergyHatches.size() + mExoticEnergyHatches.size();
        if (energyHatchCount < 1)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, energyHatchCount, 1));
        if (shouldCheckMaintenance() && mMaintenanceHatches.isEmpty())
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Maintenance, 0, 1));
        markDisplayDirty();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return null;
    }

    @Override
    public int getMaxParallelRecipes() {
        return 1;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return false;
    }

    @Override
    public boolean doesBindPlayerInventory() {
        return true;
    }

    @Override
    public int getGUIHeight() {
        return 192;
    }

    @Override
    public void onPostTick(IGregTechTileEntity baseTileEntity, long tick) {
        super.onPostTick(baseTileEntity, tick);
        if (baseTileEntity.isServerSide() && mMachine && tick % 20 == 0) {
            scanInputBuses();
            refreshBreedingPlan();
            if (displayDirty) {
                syncedArchiveSize = cropArchive.size();
                syncedArchiveCrops = new ArrayList<>(cropArchive.getAvailableCropIds());
                Collections.sort(syncedArchiveCrops);
                syncedChainSteps = buildStructuredChainSteps();
                syncedMissingInfo = allTasksBlocked && missingCropId != null ? missingCropId : "";
                displayDirty = false;
            }
        }
    }

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        activeOperation = OPERATION_NONE;
        activeCropId = "";
        missingBreedingRequirements = false;
        scanInputBuses();

        ICropCard targetCrop = getTargetCrop();
        if (targetCropId != null && !targetCropId.isEmpty() && targetCrop == null) {
            targetInputValid = false;
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        if (targetCrop == null) {
            refreshBreedingPlan();
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        refreshBreedingPlan();
        if (pendingSeedOutputs <= 0) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        if (cropArchive.hasCrop(targetCrop.getId())) {
            ItemStack output = cropArchive.createSeed(targetCrop.getId());
            if (output == null) {
                allTasksBlocked = true;
                missingCropId = targetCrop.getId();
                missingBreedingRequirements = false;
                markDisplayDirty();
                // #tr GT5U.gui.text.recipe_result.CropBreeder_unreachable_target
                // # Current crop archive cannot create the target seed!
                // # zh_CN 当前作物档案无法生成目标种子！
                return SimpleCheckRecipeResult.ofFailure("CropBreeder_unreachable_target");
            }
            if (!canOutputAll(new ItemStack[] { output })) {
                return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            }
            allTasksBlocked = false;
            missingCropId = "";
            missingBreedingRequirements = false;
            mOutputItems = new ItemStack[] { output };
            activeOperation = OPERATION_OUTPUT;
            prepareArchiveCycle(20, 0L);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }

        if (machineMode == MODE_MUTATION_POOL) {
            CheckRecipeResult poolResult = tryPrepareArchivedPoolMutation(targetCrop);
            if (poolResult != null) return poolResult;
        } else {
            List<CropBreedingPlanner.Step> readySteps = breedingPlan
                .getReadySteps(cropArchive.getAvailableCropIds(), MAX_ARCHIVE_STEPS_PER_CYCLE);
            if (!readySteps.isEmpty()) {
                CheckRecipeResult stepResult = tryPrepareArchivedDeterministicMutation(readySteps.get(0));
                if (stepResult != null) return stepResult;
                allTasksBlocked = true;
                missingCropId = "";
                missingBreedingRequirements = true;
                markDisplayDirty();
                // #tr GT5U.gui.text.recipe_result.CropBreeder_missing_requirements
                // # Missing the catalyst or condition required by the next crop mutation!
                // # zh_CN 缺少下一步作物杂交所需的催化物或条件！
                return SimpleCheckRecipeResult.ofFailure("CropBreeder_missing_requirements");
            }

            CheckRecipeResult poolFallbackResult = tryPrepareArchivedPoolFallback(targetCrop);
            if (poolFallbackResult != null) return poolFallbackResult;
        }

        allTasksBlocked = true;
        missingCropId = breedingPlan.getFirstMissingCrop();
        if (missingCropId.isEmpty()) missingCropId = targetCrop.getId();
        missingBreedingRequirements = false;
        markDisplayDirty();
        // #tr GT5U.gui.text.recipe_result.CropBreeder_missing_crop
        // # Crop archive is missing a required parent seed!
        // # zh_CN 作物档案中缺少必要亲本种子！
        return SimpleCheckRecipeResult.ofFailure("CropBreeder_missing_crop");
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (mProgresstime >= mMaxProgresstime - 1) {
            settleActiveOperation();
        }
        return super.onRunningTick(stack);
    }

    @Nullable
    private CheckRecipeResult tryPrepareArchivedDeterministicMutation(CropBreedingPlanner.Step step) {
        ICropMutation mutation = step.getMutation();
        long mutationEUt = Math.max(1L, mutation.getBreedingMachineRecipeEUt());
        if (mutationEUt > getMaxInputVoltage()) {
            return CheckRecipeResultRegistry.insufficientVoltage(mutationEUt);
        }

        ItemStack[] catalystSlots = getCatalystSlots();
        ArrayList<ICropCard> parentCards = new ArrayList<>(mutation.getParents());
        int[] catalystConsumption = CropMutationMachineRequirements
            .canBreedIgnoringBlockUnder(mutation, parentCards, getBaseMetaTileEntity(), catalystSlots);
        if (catalystConsumption == null) return null;

        ISeedStats outputStats = cropArchive.averageStats(mutation.getParents());
        if (outputStats == null) return null;

        int duration = Math.max(1, mutation.getBreedingMachineRecipeDuration());
        consumeCatalysts(catalystSlots, catalystConsumption);
        updateSlots();
        prepareArchiveBreeding(
            mutation.getOutput()
                .getId(),
            outputStats,
            duration,
            mutationEUt);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Nullable
    private CheckRecipeResult tryPrepareArchivedPoolMutation(ICropCard targetCrop) {
        if (POOL_BREEDING_EUT > getMaxInputVoltage()) {
            return CheckRecipeResultRegistry.insufficientVoltage(POOL_BREEDING_EUT);
        }
        ArrayList<ICropCard> participatingParents = findArchivedPoolParents(targetCrop);
        if (participatingParents == null) return null;

        ISeedStats outputStats = variedArchivedStats(participatingParents);
        if (outputStats == null) return null;

        prepareArchiveBreeding(targetCrop.getId(), outputStats, POOL_BREEDING_DURATION, POOL_BREEDING_EUT);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Nullable
    private CheckRecipeResult tryPrepareArchivedPoolFallback(ICropCard targetCrop) {
        ICropCard poolTarget = getPoolFallbackTarget(targetCrop);
        return poolTarget == null ? null : tryPrepareArchivedPoolMutation(poolTarget);
    }

    @Nullable
    private ICropCard getPoolFallbackTarget(ICropCard targetCrop) {
        ICropCard missingCrop = getFirstMissingCropCard();
        if (missingCrop != null && hasArchivedPoolMutationForTarget(missingCrop)) return missingCrop;
        return hasArchivedPoolMutationForTarget(targetCrop) ? targetCrop : null;
    }

    @Nullable
    private ICropCard getFirstMissingCropCard() {
        String missing = breedingPlan.getFirstMissingCrop();
        return missing == null || missing.isEmpty() ? null : CropRegistry.instance.get(missing);
    }

    private boolean hasArchivedPoolMutationForTarget(ICropCard targetCrop) {
        return findArchivedPoolParents(targetCrop) != null;
    }

    @Nullable
    private ArrayList<ICropCard> findArchivedPoolParents(ICropCard targetCrop) {
        if (targetCrop == null || targetCrop.getTier() > getCropTierLimit()) return null;

        ArrayList<ICropCard> archivedCards = getArchivedCropCards();
        if (archivedCards.size() < 2) return null;

        List<IMutationPool> matchingPools = MutationRegistry.instance.getPossiblePoolMutations(archivedCards);
        if (matchingPools == null || matchingPools.isEmpty()) return null;

        matchingPools.sort(Comparator.comparing(IMutationPool::getUnlocalisedName));
        for (IMutationPool pool : matchingPools) {
            if (!pool.contains(targetCrop)) continue;

            ArrayList<ICropCard> participatingParents = new ArrayList<>();
            for (ICropCard parent : archivedCards) {
                if (pool.contains(parent)) participatingParents.add(parent);
                if (participatingParents.size() >= 4) break;
            }
            if (participatingParents.size() >= 2) return participatingParents;
        }
        return null;
    }

    private void prepareArchiveBreeding(String cropId, ISeedStats stats, int duration, long eut) {
        activeOperation = OPERATION_ARCHIVE_BREEDING;
        activeCropId = cropId;
        activeGrowth = stats.getGrowth();
        activeGain = stats.getGain();
        activeResistance = stats.getResistance();
        mOutputItems = null;
        prepareArchiveCycle(duration, eut);
    }

    private void prepareArchiveCycle(int duration, long eut) {
        mMaxProgresstime = Math.max(1, duration);
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        lEUt = eut <= 0L ? 0L : -eut;
    }

    private void settleActiveOperation() {
        if (activeOperation == OPERATION_NONE) return;

        boolean changed = false;
        if (activeOperation == OPERATION_ARCHIVE_BREEDING) {
            changed = cropArchive.unlockCrop(activeCropId, activeSeedStats());
            if (changed) invalidateBreedingPlan();
        } else if (activeOperation == OPERATION_OUTPUT && pendingSeedOutputs > 0) {
            pendingSeedOutputs--;
            changed = true;
        }

        activeOperation = OPERATION_NONE;
        activeCropId = "";
        if (changed) {
            markDisplayDirty();
            markBaseTileDirty();
        }
    }

    private ISeedStats activeSeedStats() {
        return new SeedStats((byte) activeGrowth, (byte) activeGain, (byte) activeResistance, true);
    }

    private boolean scanInputBuses() {
        ArrayList<ItemStack> inputs = getStoredInputs();
        boolean stateChanged = false;
        for (ItemStack stack : inputs) {
            if (stack == null) continue;
            ISeedData seedData = CropsNHUtils.getAnalyzedSeedData(stack);
            if (seedData == null) continue;

            cropArchive.addSeed(stack);
            long pending = (long) pendingSeedOutputs + Math.max(1, stack.stackSize);
            pendingSeedOutputs = (int) Math.min(Integer.MAX_VALUE, pending);
            stack.stackSize = 0;
            stateChanged = true;
        }

        if (stateChanged) {
            updateSlots();
            invalidateBreedingPlan();
            markBaseTileDirty();
        }
        return stateChanged;
    }

    private void refreshBreedingPlan() {
        boolean previousBlocked = allTasksBlocked;
        String previousMissing = missingCropId;
        boolean previousMissingRequirements = missingBreedingRequirements;
        int previousCompleted = chainCompletedSteps;

        if (targetCropId == null || targetCropId.isEmpty()) {
            breedingPlan = CropBreedingPlanner.Plan.empty("");
            breedingChain = new ArrayList<>();
            breedingPlanDirty = false;
            allTasksBlocked = false;
            missingCropId = "";
            missingBreedingRequirements = false;
        } else {
            if (breedingPlanDirty) {
                breedingPlan = CropBreedingPlanner.plan(targetCropId, cropArchive.getAvailableCropIds());
                breedingChain = new ArrayList<>(breedingPlan.getSteps());
                breedingPlanDirty = false;
            }

            boolean targetUnlocked = cropArchive.hasCrop(targetCropId);
            boolean hasReadyStep = !breedingPlan.getReadySteps(cropArchive.getAvailableCropIds(), 1)
                .isEmpty();
            ICropCard targetCrop = CropRegistry.instance.get(targetCropId);
            boolean hasReadyPoolFallback = !targetUnlocked && getPoolFallbackTarget(targetCrop) != null;
            allTasksBlocked = pendingSeedOutputs > 0 && !targetUnlocked && !hasReadyStep && !hasReadyPoolFallback;
            missingCropId = allTasksBlocked ? breedingPlan.getFirstMissingCrop() : "";
            if (allTasksBlocked && missingCropId.isEmpty()) missingCropId = targetCropId;
            missingBreedingRequirements = false;
        }

        updateChainDisplayInfo();
        if (previousBlocked != allTasksBlocked || !java.util.Objects.equals(previousMissing, missingCropId)
            || previousMissingRequirements != missingBreedingRequirements
            || previousCompleted != chainCompletedSteps) {
            markDisplayDirty();
        }
    }

    private void updateChainDisplayInfo() {
        chainTotalSteps = breedingChain.size();
        chainCompletedSteps = countCompletedSteps();
    }

    private int countCompletedSteps() {
        int count = 0;
        for (CropBreedingPlanner.Step step : breedingChain) {
            if (cropArchive.hasCrop(step.result)) count++;
        }
        return count;
    }

    private List<NBTTagCompound> buildStructuredChainSteps() {
        if (breedingChain.isEmpty()) return Collections.emptyList();
        List<NBTTagCompound> result = new ArrayList<>(breedingChain.size());
        for (CropBreedingPlanner.Step step : breedingChain) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("result", step.result);
            tag.setInteger("parentCount", step.parents.size());
            for (int i = 0; i < step.parents.size(); i++) {
                tag.setString("parent" + i, step.parents.get(i));
            }
            tag.setInteger("duration", step.duration);
            tag.setInteger("eut", step.eut);
            if (cropArchive.hasCrop(step.result)) {
                tag.setByte("status", (byte) 2);
            } else if (cropArchive.getAvailableCropIds()
                .containsAll(step.parents)) {
                    tag.setByte("status", (byte) 1);
                } else {
                    tag.setByte("status", (byte) 0);
                }
            result.add(tag);
        }
        return result;
    }

    private ArrayList<ICropCard> getArchivedCropCards() {
        ArrayList<ICropCard> crops = new ArrayList<>();
        for (String cropId : cropArchive.getAvailableCropIds()) {
            ICropCard crop = CropRegistry.instance.get(cropId);
            if (crop != null) crops.add(crop);
        }
        crops.sort(Comparator.comparing(ICropCard::getId));
        return crops;
    }

    private ItemStack[] getCatalystSlots() {
        ArrayList<ItemStack> inputs = getStoredInputs();
        ArrayList<ItemStack> catalysts = new ArrayList<>();
        for (ItemStack stack : inputs) {
            if (stack == null) continue;
            if (CropsNHUtils.getAnalyzedSeedData(stack) == null) catalysts.add(stack);
        }
        return catalysts.toArray(new ItemStack[0]);
    }

    private static void consumeCatalysts(ItemStack[] catalystSlots, int[] catalystConsumption) {
        for (int i = 0; i < catalystConsumption.length; i++) {
            if (catalystConsumption[i] <= 0 || catalystSlots[i] == null) continue;
            catalystSlots[i].stackSize -= catalystConsumption[i];
        }
    }

    @Nullable
    private ISeedStats variedArchivedStats(Collection<ICropCard> requiredParents) {
        ArrayList<ISeedStats> parentStats = new ArrayList<>();
        for (ICropCard parent : requiredParents) {
            ISeedStats stats = cropArchive.getStats(parent.getId());
            if (stats == null) return null;
            parentStats.add(stats);
        }
        if (parentStats.size() < 2) return null;
        return new SeedStats(
            TileEntityCropSticks.variateStat(false, parentStats, ISeedStats::getGrowth),
            TileEntityCropSticks.variateStat(false, parentStats, ISeedStats::getGain),
            TileEntityCropSticks.variateStat(false, parentStats, ISeedStats::getResistance),
            true);
    }

    @Nullable
    private ICropCard getTargetCrop() {
        if (targetCropId == null || targetCropId.isEmpty()) return null;
        return CropRegistry.instance.get(targetCropId);
    }

    @Nullable
    private static ICropCard resolveCropInput(String input) {
        String candidate = input == null ? "" : input.trim();
        if (candidate.isEmpty()) return null;

        ICropCard exact = CropRegistry.instance.get(candidate);
        if (exact != null) return exact;

        String normalized = candidate.toLowerCase(java.util.Locale.ROOT);
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop == null) continue;
            if (crop.getId()
                .equalsIgnoreCase(candidate)) {
                return crop;
            }
            String unlocalizedName = crop.getUnlocalizedName();
            if (unlocalizedName != null && unlocalizedName.toLowerCase(java.util.Locale.ROOT)
                .contains(normalized)) {
                return crop;
            }
            String localizedName = getCropDisplayName(crop);
            if (localizedName != null && localizedName.toLowerCase(java.util.Locale.ROOT)
                .contains(normalized)) {
                return crop;
            }
        }
        return null;
    }

    public String getTargetCropId() {
        return targetCropId == null ? "" : targetCropId;
    }

    public void setTargetCropId(String cropInput) {
        String candidate = cropInput == null ? "" : cropInput.trim();
        if (candidate.isEmpty()) {
            targetInputValid = true;
            if (targetCropId != null && !targetCropId.isEmpty()) {
                targetCropId = "";
                invalidateBreedingPlan();
                markBaseTileDirty();
            }
            return;
        }

        ICropCard resolved = resolveCropInput(candidate);
        targetInputValid = resolved != null;
        if (resolved == null) {
            markDisplayDirty();
            return;
        }

        String resolvedId = resolved.getId();
        if (!resolvedId.equals(targetCropId)) {
            targetCropId = resolvedId;
            invalidateBreedingPlan();
            markBaseTileDirty();
        }
    }

    public boolean isTargetInputValid() {
        return targetInputValid;
    }

    public int getSyncedArchiveSize() {
        return syncedArchiveSize;
    }

    public int getPendingSeedOutputs() {
        return pendingSeedOutputs;
    }

    public int getChainTotalSteps() {
        return chainTotalSteps;
    }

    public int getChainCompletedSteps() {
        return chainCompletedSteps;
    }

    public boolean isAllTasksBlocked() {
        return allTasksBlocked;
    }

    public String getSyncedMissingInfo() {
        return allTasksBlocked && missingCropId != null ? missingCropId : "";
    }

    public boolean isMissingBreedingRequirements() {
        return allTasksBlocked && missingBreedingRequirements;
    }

    public List<String> getSyncedArchiveCrops() {
        return syncedArchiveCrops;
    }

    public void setSyncedArchiveCrops(List<String> crops) {
        syncedArchiveCrops = crops == null ? Collections.emptyList() : crops;
    }

    public List<NBTTagCompound> getSyncedChainSteps() {
        return syncedChainSteps;
    }

    public void setSyncedChainSteps(List<NBTTagCompound> steps) {
        syncedChainSteps = steps == null ? Collections.emptyList() : steps;
    }

    public long getPoolBreedingEUt() {
        return POOL_BREEDING_EUT;
    }

    public int getPoolBreedingDuration() {
        return POOL_BREEDING_DURATION;
    }

    public static String getCropDisplayName(String cropId) {
        ICropCard crop = CropRegistry.instance.get(cropId);
        return crop == null ? cropId : getCropDisplayName(crop);
    }

    public static String getCropDisplayName(ICropCard crop) {
        if (crop == null) return "";
        String localized = StatCollector.translateToLocal(crop.getUnlocalizedName());
        return localized == null || localized.isEmpty() ? crop.getId() : localized;
    }

    private int getCropTierLimit() {
        return Math.max(1, GTUtility.getTier(getMaxInputVoltage()));
    }

    private void markDisplayDirty() {
        displayDirty = true;
    }

    private void invalidateBreedingPlan() {
        breedingPlanDirty = true;
        markDisplayDirty();
    }

    private void markBaseTileDirty() {
        IGregTechTileEntity baseTile = getBaseMetaTileEntity();
        if (baseTile != null) baseTile.markDirty();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setString(NBT_TARGET_CROP_ID, getTargetCropId());
        aNBT.setInteger("pendingSeedOutputs", pendingSeedOutputs);
        aNBT.setTag("cropArchive", cropArchive.toNBT());
        aNBT.setByte("cropBreederActiveOperation", activeOperation);
        aNBT.setString("cropBreederActiveCrop", activeCropId == null ? "" : activeCropId);
        aNBT.setInteger("cropBreederActiveGrowth", activeGrowth);
        aNBT.setInteger("cropBreederActiveGain", activeGain);
        aNBT.setInteger("cropBreederActiveResistance", activeResistance);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        aNBT.setString(NBT_TARGET_CROP_ID, getTargetCropId());
        aNBT.setInteger("pendingSeedOutputs", pendingSeedOutputs);
        aNBT.setTag("cropArchive", cropArchive.toNBT());
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        targetCropId = aNBT.getString(NBT_TARGET_CROP_ID);
        pendingSeedOutputs = aNBT.getInteger("pendingSeedOutputs");
        cropArchive = aNBT.hasKey("cropArchive") ? CropArchive.fromNBT(aNBT.getCompoundTag("cropArchive"))
            : new CropArchive();
        activeOperation = aNBT.getByte("cropBreederActiveOperation");
        activeCropId = aNBT.getString("cropBreederActiveCrop");
        activeGrowth = aNBT.getInteger("cropBreederActiveGrowth");
        activeGain = aNBT.getInteger("cropBreederActiveGain");
        activeResistance = aNBT.getInteger("cropBreederActiveResistance");
        if (activeOperation != OPERATION_ARCHIVE_BREEDING && activeOperation != OPERATION_OUTPUT) {
            activeOperation = OPERATION_NONE;
            activeCropId = "";
        }
        breedingPlan = CropBreedingPlanner.Plan.empty(targetCropId);
        breedingPlanDirty = true;
        breedingChain.clear();
        markDisplayDirty();
        targetInputValid = targetCropId == null || targetCropId.isEmpty()
            || CropRegistry.instance.get(targetCropId) != null;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType(getMachineType());
        // #tr Tooltip_LargeCropBreeder_00
        // # A large electric automatic CropsNH crop breeding machine
        // # zh_CN 大型电力 CropsNH 作物自动杂交机
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_00"));
        // #tr Tooltip_LargeCropBreeder_01
        // # Input analyzed seeds to unlock crop ids in the internal archive
        // # zh_CN 投入已分析种子，将作物 ID 永久记入内部档案
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_01"));
        // #tr Tooltip_LargeCropBreeder_02
        // # Each input seed adds one pending target seed output request
        // # zh_CN 每个输入种子都会增加一次目标种子输出请求
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_02"));
        // #tr Tooltip_LargeCropBreeder_03
        // # Deterministic mode runs the planned mutation chain when all parents are known
        // # zh_CN 确定性模式会在亲本齐全时执行规划杂交链
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_03"));
        // #tr Tooltip_LargeCropBreeder_04
        // # Pool mode uses CropsNH mutation pools when the requested crop is in a reachable pool
        // # zh_CN 池模式会使用 CropsNH 杂交池尝试可到达目标
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_04"));
        // #tr Tooltip_LargeCropBreeder_05
        // # Deterministic mutations use their CropsNH recipe EU/t and duration
        // # zh_CN 确定性杂交使用 CropsNH 配方自身的 EU/t 与时长
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_05"));
        // #tr Tooltip_LargeCropBreeder_06
        // # The structure is fixed to steel casing, steel pipe casing, and steel frames
        // # zh_CN 结构固定为钢机器外壳、钢级管道外壳和钢框架
        tt.addInfo(StatCollector.translateToLocal("Tooltip_LargeCropBreeder_06"));
        // #tr Tooltip_LargeCropBreeder_Controller
        // # Front center on the seed-bed layer
        // # zh_CN 种床层正面中心
        String controller = StatCollector.translateToLocal("Tooltip_LargeCropBreeder_Controller");
        // #tr Tooltip_LargeCropBreeder_BorosilicateGlass
        // # Borosilicate glass greenhouse wall
        // # zh_CN 硼硅玻璃温室墙
        String glass = StatCollector.translateToLocal("Tooltip_LargeCropBreeder_BorosilicateGlass");
        // #tr Tooltip_LargeCropBreeder_Casing
        // # Steel machine casing
        // # zh_CN 钢机器外壳
        String casing = StatCollector.translateToLocal("Tooltip_LargeCropBreeder_Casing");
        // #tr Tooltip_LargeCropBreeder_SeedBed
        // # Steel pipe casing seed bed
        // # zh_CN 钢级管道外壳种床
        String seedBed = StatCollector.translateToLocal("Tooltip_LargeCropBreeder_SeedBed");
        tt.beginStructureBlock(5, 4, 3, false)
            .addController(controller)
            .addOtherStructurePart(glass, "A", 2)
            .addInputBus(casing, 1)
            .addOutputBus(casing, 1)
            .addEnergyHatch(casing, 1)
            .addMaintenanceHatch(casing, 1)
            .addOtherStructurePart(seedBed, "C", 3)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        int casingId = getSteelCasingTextureId();
        if (side == facing) {
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(casingId), TextureFactory.builder()
                .addIcon(active ? OVERLAY_FRONT_CROP_BREEDER_ACTIVE : OVERLAY_FRONT_CROP_BREEDER)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(active ? OVERLAY_FRONT_CROP_BREEDER_ACTIVE_GLOW : OVERLAY_FRONT_CROP_BREEDER_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(casingId) };
    }

    @Override
    public String[] getInfoData() {
        String[] superInfo = super.getInfoData();
        String[] info = new String[superInfo.length + 5];
        System.arraycopy(superInfo, 0, info, 0, superInfo.length);

        // #tr info.gtnotgood.largeCropBreeder.target
        // # Target:
        // # zh_CN 目标：
        info[superInfo.length] = EnumChatFormatting.YELLOW
            + StatCollector.translateToLocal("info.gtnotgood.largeCropBreeder.target")
            + EnumChatFormatting.GREEN
            + (targetCropId.isEmpty() ? StatCollector.translateToLocal("gt.blockmachines.multimachine.notset")
                : getCropDisplayName(targetCropId));
        // #tr info.gtnotgood.largeCropBreeder.archive
        // # Archive Crops:
        // # zh_CN 档案作物：
        info[superInfo.length + 1] = EnumChatFormatting.YELLOW
            + StatCollector.translateToLocal("info.gtnotgood.largeCropBreeder.archive")
            + EnumChatFormatting.GREEN
            + cropArchive.size();
        // #tr info.gtnotgood.largeCropBreeder.progress
        // # Chain Progress:
        // # zh_CN 杂交链进度：
        info[superInfo.length + 2] = EnumChatFormatting.YELLOW
            + StatCollector.translateToLocal("info.gtnotgood.largeCropBreeder.progress")
            + EnumChatFormatting.GREEN
            + countCompletedSteps()
            + "/"
            + breedingChain.size();
        // #tr info.gtnotgood.largeCropBreeder.pending
        // # Pending Output:
        // # zh_CN 待输出：
        info[superInfo.length + 3] = EnumChatFormatting.YELLOW
            + StatCollector.translateToLocal("info.gtnotgood.largeCropBreeder.pending")
            + EnumChatFormatting.GREEN
            + pendingSeedOutputs;
        // #tr info.gtnotgood.largeCropBreeder.pool_eut
        // # Pool EU/t:
        // # zh_CN 池模式 EU/t：
        info[superInfo.length + 4] = EnumChatFormatting.YELLOW
            + StatCollector.translateToLocal("info.gtnotgood.largeCropBreeder.pool_eut")
            + EnumChatFormatting.GREEN
            + NumberFormatUtil.formatNumber(POOL_BREEDING_EUT);
        return info;
    }

    private static int getSteelCasingTextureId() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }
}
