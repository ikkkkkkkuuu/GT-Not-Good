/* SPDX-License-Identifier: LGPL-3.0-or-later */
package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.MultiAmpEnergy;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.utils.item.CombinedInvWrapper;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.gtnewhorizon.cropsnh.api.CropsNHStructureChannels;
import com.gtnewhorizon.cropsnh.api.IGrowthRequirement;
import com.gtnewhorizon.cropsnh.api.IMachineGrowthRequirement;
import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.blocks.BlockAdvancedHarvestingUnit;
import com.gtnewhorizon.cropsnh.blocks.BlockEnvironmentalEnhancementUnit;
import com.gtnewhorizon.cropsnh.blocks.BlockFertilizerUnit;
import com.gtnewhorizon.cropsnh.blocks.BlockGrowthAccelerationUnit;
import com.gtnewhorizon.cropsnh.blocks.BlockOverclockedGrowthAccelerationUnit;
import com.gtnewhorizon.cropsnh.blocks.BlockSeedBed;
import com.gtnewhorizon.cropsnh.blocks.abstracts.CropsNHBlockIndustrialFarmTiredComponent;
import com.gtnewhorizon.cropsnh.farming.requirements.SubSoilRequirement;
import com.gtnewhorizon.cropsnh.init.CropsNHBlocks;
import com.gtnewhorizon.cropsnh.items.ItemEnvironmentalModule;
import com.gtnewhorizon.cropsnh.reference.Reference;
import com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarm;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;
import com.gtnewhorizon.cropsnh.utility.IFDropTable;
import com.gtnewhorizon.cropsnh.utility.WorldUtils;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeIndustrialFarmGui;
import com.xyp.gtnotgood.common.machines.crop.IndustrialFarmFluids;
import com.xyp.gtnotgood.common.machines.crop.IndustrialFarmInventory;
import com.xyp.gtnotgood.common.machines.crop.SelectedCropInventory;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

@IMetaTileEntity.SkipGenerateName
public final class LargeIndustrialFarm extends MTEIndustrialFarm {

    private static final String MACHINE_NAME = "multimachine.largeindustrialfarm";
    private static final String STRUCTURE_MAIN = "main";
    private static final int SLICES = BlockSeedBed.MAX_TIER - BlockSeedBed.MIN_TIER + 1;
    private static final String NBT_CROPS = "industrialFarmPlots";
    private final IndustrialFarmInventory cropInventory = new IndustrialFarmInventory(this);
    private final IItemHandlerModifiable cropInventoryWrapper = new CombinedInvWrapper(cropInventory, inventoryHandler);
    private final String[] cropStatusKeys = new String[IndustrialFarmInventory.CROP_COUNT];
    private final List<ItemStack> migrationReturns = new ArrayList<>();
    private IStructureDefinition<MTEIndustrialFarm> structureDefinition;
    private int activeCrop = -1;
    private int selectedCrop;
    private int clientUsedSeedCount = -1;
    private int outputCursor;
    private double environmentalStrength;
    private double growthAccelerationStrength;
    private double fertilizerStrength;
    private double harvestingStrength;
    private double overclockStrength;

    public LargeIndustrialFarm(int id) {
        super(id);
        ifStackHandler = new SelectedCropInventory(this);
    }

    private LargeIndustrialFarm() {
        super(MACHINE_NAME);
        ifStackHandler = new SelectedCropInventory(this);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tileEntity) {
        return new LargeIndustrialFarm();
    }

    @Override
    public String getMetaName() {
        return MACHINE_NAME;
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.industrial_farm.name
        // # Large Industrial Farm
        // # zh_CN 大型工业农场
        return "gtng.industrial_farm.name";
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeIndustrialFarmGui(this);
    }

    public IndustrialFarmInventory getCropInventory() {
        return cropInventory;
    }

    public int getSelectedCrop() {
        return selectedCrop;
    }

    public void setSelectedCrop(int crop) {
        if (!canEditCrops()) return;
        syncSelectedCrop(crop);
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    public void syncSelectedCrop(int crop) {
        selectedCrop = Math.floorMod(crop, IndustrialFarmInventory.CROP_COUNT);
    }

    private int currentCrop() {
        return activeCrop < 0 ? selectedCrop : activeCrop;
    }

    @Override
    public IItemHandlerModifiable getInventoryHandler() {
        return cropInventoryWrapper;
    }

    public ItemStack getCropSeed(int crop) {
        if (isSelectedClientCrop(crop)) return ifStackHandler.getStackInSlot(SLOT_SEED);
        return cropInventory.getStackInSlot(IndustrialFarmInventory.slot(crop, SLOT_SEED));
    }

    public void setCropSeed(int crop, ItemStack stack) {
        cropInventory.setStackInSlot(IndustrialFarmInventory.slot(crop, SLOT_SEED), stack);
    }

    public ItemStack getCropEnvironment(int crop, int index) {
        if (isSelectedClientCrop(crop)) return ifStackHandler.getStackInSlot(SLOT_ENV_CARD_START + index);
        return cropInventory.getStackInSlot(IndustrialFarmInventory.slot(crop, SLOT_ENV_CARD_START + index));
    }

    private boolean isSelectedClientCrop(int crop) {
        return crop == selectedCrop && getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isClientSide();
    }

    public void setCropEnvironment(int crop, int index, ItemStack stack) {
        cropInventory.setStackInSlot(IndustrialFarmInventory.slot(crop, SLOT_ENV_CARD_START + index), stack);
    }

    @Override
    public ItemStack getSeedStack() {
        return getCropSeed(currentCrop());
    }

    @Override
    public void setSeedStack(ItemStack stack) {
        setCropSeed(currentCrop(), stack);
    }

    @Override
    public ItemStack getSubSoilStack() {
        return null;
    }

    @Override
    public void setSubSoilStack(ItemStack stack) {}

    @Override
    public ItemStack getEnvironmentalModuleStack(int index) {
        return getCropEnvironment(currentCrop(), index);
    }

    public int getUsedSeedCount() {
        if (clientUsedSeedCount >= 0 && getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isClientSide())
            return clientUsedSeedCount;
        long total = 0;
        for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++) {
            ItemStack seed = getCropSeed(crop);
            if (CropsNHUtils.isStackValid(seed)) total += seed.stackSize;
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public void syncUsedSeedCount(int count) {
        clientUsedSeedCount = Math.max(0, count);
    }

    public long getTotalSeedCapacity() {
        return (long) Math.max(0, seedCapacity) * IndustrialFarmInventory.CROP_COUNT;
    }

    public int getOccupiedCropCount() {
        int count = 0;
        for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++)
            if (CropsNHUtils.isStackValid(getCropSeed(crop))) count++;
        return count;
    }

    public boolean canEditCrops() {
        return !isAllowedToWork() && mMaxProgresstime == 0;
    }

    public boolean isCropSeedValid(int crop, ItemStack stack) {
        ISeedData seed = CropsNHUtils.getSeedData(stack, false, true);
        if (seed == null || seed.getCrop().getMinSeedBedTier() > upgradeTier) return false;
        if (getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isClientSide()) return true;
        for (int other = 0; other < IndustrialFarmInventory.CROP_COUNT; other++) {
            ItemStack otherSeed = getCropSeed(other);
            if (
                other != crop && CropsNHUtils.isStackValid(otherSeed)
                    && GTUtility.areStacksEqual(otherSeed, stack, false)
            ) return false;
        }
        int previousCrop = activeCrop;
        boolean previousFertilizer = hasFertilizer;
        try {
            activeCrop = crop;
            hasFertilizer = true;
            return getGrowthSpeedUnscaled(seed) > 0;
        } finally {
            activeCrop = previousCrop;
            hasFertilizer = previousFertilizer;
        }
    }

    public boolean isCropEnvironmentValid(int crop, int index, ItemStack stack) {
        return index >= 0 && index < environmentalEnhancementUnitCount
            && index < 2
            && CropsNHUtils.isStackValid(stack)
            && stack.getItem() instanceof ItemEnvironmentalModule
            && ItemEnvironmentalModule.getBiomeTag(CropsNHUtils.getItemMeta(stack)) != null;
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (cropStatusKeys != null && slot >= 0 && slot < cropInventory.getSlots())
            cropStatusKeys[slot / IndustrialFarmInventory.SLOTS_PER_CROP] = null;
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setTag(NBT_CROPS, cropInventory.serializeNBT());
        tag.setInteger("industrialFarmOutputCursor", outputCursor);
        tag.setInteger("industrialFarmSelectedCrop", selectedCrop);
        NBTTagList returns = new NBTTagList();
        for (ItemStack stack : migrationReturns) {
            NBTTagCompound item = stack.writeToNBT(new NBTTagCompound());
            item.setInteger("Count", stack.stackSize);
            returns.appendTag(item);
        }
        tag.setTag("industrialFarmReturns", returns);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        SelectedCropInventory selected = (SelectedCropInventory) ifStackHandler;
        selected.clearLegacy();
        super.loadNBTData(tag);
        syncSelectedCrop(tag.getInteger("industrialFarmSelectedCrop"));
        migrationReturns.clear();
        NBTTagList returns = tag.getTagList("industrialFarmReturns", 10);
        for (int index = 0; index < returns.tagCount(); index++) {
            NBTTagCompound item = returns.getCompoundTagAt(index);
            ItemStack stack = ItemStack.loadItemStackFromNBT(item);
            if (stack == null) continue;
            stack.stackSize = item.getInteger("Count");
            if (CropsNHUtils.isStackValid(stack)) migrationReturns.add(stack);
        }
        cropInventory.deserializeNBT(new NBTTagCompound());
        if (tag.hasKey(NBT_CROPS, 10)) {
            NBTTagCompound stored = tag.getCompoundTag(NBT_CROPS);
            if (stored.getInteger("Size") == IndustrialFarmInventory.CROP_COUNT * 4) migrateLegacyCrops(stored, 4);
            else if (stored.getInteger("Size") == IndustrialFarmInventory.CROP_COUNT * 2 + 2)
                migrateLegacyCrops(stored, 2);
            else if (stored.getInteger("Size") == IndustrialFarmInventory.CROP_COUNT + 2)
                migrateSharedEnvironment(stored);
            else cropInventory.deserializeNBT(stored);
        } else {
            for (int slot = 0; slot < ifStackHandler.getSlots(); slot++) {
                ItemStack stack = selected.getLegacyStack(slot);
                if (!CropsNHUtils.isStackValid(stack)) continue;
                if (slot == SLOT_SUB_SOIL) migrationReturns.add(stack.copy());
                else cropInventory.setStackInSlot(IndustrialFarmInventory.slot(0, slot), stack.copy());
            }
        }
        selected.clearLegacy();
        outputCursor = Math.floorMod(tag.getInteger("industrialFarmOutputCursor"), IndustrialFarmInventory.CROP_COUNT);
        activeCrop = -1;
        Arrays.fill(cropStatusKeys, null);
    }

    private void migrateLegacyCrops(NBTTagCompound stored, int slotsPerCrop) {
        ItemStackHandler legacy = new ItemStackHandler(stored.getInteger("Size"));
        legacy.deserializeNBT(stored);
        for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++) {
            for (int kind = 0; kind < slotsPerCrop; kind++) {
                ItemStack stack = legacy.getStackInSlot(crop * slotsPerCrop + kind);
                if (!CropsNHUtils.isStackValid(stack)) continue;
                if (kind == SLOT_SUB_SOIL) {
                    migrationReturns.add(stack);
                    continue;
                }
                int slot = IndustrialFarmInventory.slot(crop, kind);
                if (cropInventory.getStackInSlot(slot) == null) cropInventory.setStackInSlot(slot, stack);
                else migrationReturns.add(stack);
            }
        }
        if (slotsPerCrop == 2) {
            for (int index = 0; index < 2; index++) {
                ItemStack stack = legacy.getStackInSlot(IndustrialFarmInventory.CROP_COUNT * 2 + index);
                setCropEnvironment(selectedCrop, index, stack);
            }
        }
    }

    private void migrateSharedEnvironment(NBTTagCompound stored) {
        ItemStackHandler legacy = new ItemStackHandler(IndustrialFarmInventory.CROP_COUNT + 2);
        legacy.deserializeNBT(stored);
        for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++) {
            setCropSeed(crop, legacy.getStackInSlot(crop));
        }
        for (int index = 0; index < 2; index++) {
            setCropEnvironment(selectedCrop, index, legacy.getStackInSlot(IndustrialFarmInventory.CROP_COUNT + index));
        }
    }

    @Override
    public void onBlockDestroyed() {
        var tile = getBaseMetaTileEntity();
        if (tile != null) {
            for (int slot = 0; slot < cropInventory.getSlots(); slot++) {
                dropStack(cropInventory.getStackInSlot(slot));
                cropInventory.setStackInSlot(slot, null);
            }
            migrationReturns.forEach(this::dropStack);
            migrationReturns.clear();
        }
        super.onBlockDestroyed();
    }

    private void dropStack(ItemStack stack) {
        if (!CropsNHUtils.isStackValid(stack)) return;
        var tile = getBaseMetaTileEntity();
        int remaining = stack.stackSize;
        while (remaining > 0) {
            ItemStack dropped = stack.copy();
            dropped.stackSize = Math.min(remaining, Math.max(1, stack.getMaxStackSize()));
            WorldUtils.dropItem(tile.getWorld(), tile.getXCoord(), tile.getYCoord(), tile.getZCoord(), dropped);
            remaining -= dropped.stackSize;
        }
    }

    private static String[][] createShape() {
        String[][] shape = new String[4][SLICES + 2];
        String[] front = { " cCc ", "cCCCc", "cC~Cc", "c   c" };
        String[] middle = { " gUg ", "g   g", "csssc", "     " };
        String[] back = { " cDc ", "cDDDc", "cDDDc", "c   c" };
        for (int row = 0; row < shape.length; row++) {
            shape[row][0] = front[row];
            for (int slice = 1; slice <= SLICES; slice++) shape[row][slice] = middle[row];
            shape[row][SLICES + 1] = back[row];
        }
        return shape;
    }

    @Override
    public IStructureDefinition<MTEIndustrialFarm> getStructureDefinition() {
        if (structureDefinition == null) {
            int casingTexture = GTUtility.getCasingTextureIndex(CropsNHBlocks.blockCasings1, 0);
            structureDefinition = StructureDefinition.<MTEIndustrialFarm>builder()
                .addShape(STRUCTURE_MAIN, transpose(createShape()))
                .addElement('C',
                    buildHatchAdder(MTEIndustrialFarm.class)
                        .atLeast(InputBus, InputHatch, OutputBus, Maintenance, MultiAmpEnergy.or(Energy))
                        .casingIndex(casingTexture).allowOnly(ForgeDirection.NORTH).hint(1)
                        .buildAndChain(ofBlock(CropsNHBlocks.blockCasings1, 0)))
                .addElement('D',
                    buildHatchAdder(MTEIndustrialFarm.class)
                        .atLeast(InputBus, InputHatch, OutputBus, Maintenance, MultiAmpEnergy.or(Energy))
                        .casingIndex(casingTexture).allowOnly(ForgeDirection.SOUTH).shouldSkip((machine, tile) -> true)
                        .hint(1).buildAndChain(ofBlock(CropsNHBlocks.blockCasings1, 0)))
                .addElement('c', ofBlock(CropsNHBlocks.blockCasings1, 0))
                .addElement('g', ofBlock(ItemRegistry.bw_realglas, 0))
                .addElement('s',
                    withChannel(CropsNHStructureChannels.IFTier.get(),
                        ofBlocksTiered(
                            (block, meta) -> block == CropsNHBlocks.blockSeedBed ? ((BlockSeedBed) block).getTier(meta)
                                : null,
                            ((BlockSeedBed) CropsNHBlocks.blockSeedBed).getStructureBlocks(), -1,
                            (machine, tier) -> machine.upgradeTier = tier, machine -> machine.upgradeTier)))
                .addElement('U',
                    ofChain(ofFrame(Materials.Wood),
                        upgrade(CropsNHBlocks.blockEnvironmentalEnhancementUnit,
                            machine -> machine.environmentalEnhancementUnitCount++),
                        upgrade(CropsNHBlocks.blockGrowthAccelerationUnit,
                            machine -> machine.growthAccelerationUnitCount++),
                        upgrade(CropsNHBlocks.blockFertilizerUnit, machine -> machine.fertilizerUnitCount++),
                        upgrade(CropsNHBlocks.blockAdvancedHarvestingUnit,
                            machine -> machine.advancedHarvestingUnitCount++),
                        upgrade(CropsNHBlocks.blockOverclockedGrowthAccelerationUnit,
                            machine -> machine.overclockedGrowthAccelerationUnitCount++)))
                .build();
        }
        return structureDefinition;
    }

    @SuppressWarnings("unchecked")
    private static IStructureElement<MTEIndustrialFarm> upgrade(Block block, Consumer<MTEIndustrialFarm> onPass) {
        var component = (CropsNHBlockIndustrialFarmTiredComponent) block;
        IStructureElement<MTEIndustrialFarm>[] tiers = component.getStructureBlocks().stream()
            .map(pair -> ofBlock(pair.getLeft(), pair.getRight())).toArray(IStructureElement[]::new);
        for (int index = 0; index < tiers.length; index++) {
            int tier = component.getStructureBlocks().get(index).getRight();
            double strength = 1.0 + 0.25 * (tier - component.minTier);
            tiers[index] = onElementPass(machine -> ((LargeIndustrialFarm) machine).addUpgradeStrength(block, strength),
                tiers[index]);
        }
        return onElementPass(onPass, ofChain(tiers));
    }

    private void addUpgradeStrength(Block block, double strength) {
        if (block == CropsNHBlocks.blockEnvironmentalEnhancementUnit) environmentalStrength += strength;
        else if (block == CropsNHBlocks.blockGrowthAccelerationUnit) growthAccelerationStrength += strength;
        else if (block == CropsNHBlocks.blockFertilizerUnit) fertilizerStrength += strength;
        else if (block == CropsNHBlocks.blockAdvancedHarvestingUnit) harvestingStrength += strength;
        else if (block == CropsNHBlocks.blockOverclockedGrowthAccelerationUnit) overclockStrength += strength;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(STRUCTURE_MAIN, trigger, hintsOnly, 2, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece(STRUCTURE_MAIN, trigger, 2, 2, 0, elementBudget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tileEntity, ItemStack stack, List<StructureError> errors) {
        upgradeTier = -1;
        glassTier = -1;
        environmentalEnhancementUnitCount = 0;
        growthAccelerationUnitCount = 0;
        fertilizerUnitCount = 0;
        advancedHarvestingUnitCount = 0;
        overclockedGrowthAccelerationUnitCount = 0;
        environmentalStrength = 0;
        growthAccelerationStrength = 0;
        fertilizerStrength = 0;
        harvestingStrength = 0;
        overclockStrength = 0;
        seedCapacity = 0;
        expectedOCs = 0;
        expectedEUt = 0;
        if (!checkPiece(STRUCTURE_MAIN, 2, 2, 0, errors)) return;
        checkHasAnyEnergy(errors);
        checkHasMaintenanceHatch(errors);
        checkHasInputHatch(errors);
        checkHasOutputBus(errors);
        if (environmentalEnhancementUnitCount > BlockEnvironmentalEnhancementUnit.MAX_UPGRADE_COUNT)
            errors.add(farmError("EEUCount"));
        if (fertilizerUnitCount > BlockFertilizerUnit.MAX_UPGRADE_COUNT) errors.add(farmError("FUCount"));
        if (advancedHarvestingUnitCount > BlockAdvancedHarvestingUnit.MAX_UPGRADE_COUNT)
            errors.add(farmError("AHUCount"));
        if (overclockedGrowthAccelerationUnitCount > BlockOverclockedGrowthAccelerationUnit.MAX_UPGRADE_COUNT)
            errors.add(farmError("OCGAUCount"));
        if (mExoticEnergyHatches.size() > MAX_MULTIAMP_EHATCH_AMOUNT) errors.add(farmError("MultiAmpEHatchCount"));
        if (!mExoticEnergyHatches.isEmpty() && !mEnergyHatches.isEmpty()) errors.add(farmError("EHatchTypeNoMix"));
        for (MTEHatch hatch : mExoticEnergyHatches) {
            if (hatch.getConnectionType() == MTEHatch.ConnectionType.LASER) errors.add(farmError("LaserNotAllowed"));
        }
        if (!errors.isEmpty()) return;
        long powerUsage = getFarmPowerUsage();
        if (overclockedGrowthAccelerationUnitCount > 0) {
            var calculator = new OverclockCalculator().setRecipeEUt(powerUsage).setEUt(getMaxInputEu())
                .setDuration(Integer.MAX_VALUE).calculate();
            expectedOCs = calculator.getPerformedOverclocks();
            expectedEUt = calculator.getConsumption();
        } else {
            expectedEUt = powerUsage;
        }
        seedCapacity = SLICES * BlockSeedBed.getCapacity(upgradeTier);
    }

    public double getEnvironmentalStrength() {
        return environmentalStrength;
    }

    public double getGrowthAccelerationStrength() {
        return growthAccelerationStrength;
    }

    public double getFertilizerStrength() {
        return fertilizerStrength;
    }

    public double getHarvestingStrength() {
        return harvestingStrength;
    }

    public double getPerfectOverclockMultiplier() {
        return Math.pow(4, expectedOCs) * Math.max(1, overclockStrength);
    }

    public long getWaterRequirement(int seeds) {
        if (seeds <= 0) return 0;
        return boundedCeil(seeds * CYCLE_TICK_RATE_SCALAR * getPerfectOverclockMultiplier());
    }

    public long getFertilizerRequirement(int seeds) {
        if (seeds <= 0) return 0;
        return boundedCeil(
            seeds * CYCLE_TICK_RATE_SCALAR * getPerfectOverclockMultiplier() / Math.max(1, fertilizerStrength));
    }

    private static long boundedCeil(double value) {
        return value >= Long.MAX_VALUE || !Double.isFinite(value) ? Long.MAX_VALUE
            : (long) Math.ceil(Math.max(0, value));
    }

    @Override
    public int getNutrientScore(ISeedData crop) {
        double multiplier = 1 + Math.max(0, environmentalStrength - environmentalEnhancementUnitCount);
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(super.getNutrientScore(crop) * multiplier));
    }

    @Override
    public double getGrowthProgressPerCycle(ISeedData crop) {
        if (crop == null) return -1;
        int speed = getGrowthSpeedUnscaled(crop);
        int duration = crop.getCrop().getGrowthDuration();
        if (speed <= 0 || duration <= 0) return -1;
        return (double) speed / duration
            * CYCLE_TICK_RATE_SCALAR
            * (1 + growthAccelerationStrength * BlockGrowthAccelerationUnit.GROWTH_SPEED_BONUS)
            * (1 + fertilizerStrength * BlockFertilizerUnit.GROWTH_SPEED_MULTIPLIER)
            * getPerfectOverclockMultiplier();
    }

    @Override
    public IFDropTable getDropsPerCycle(ISeedData crop) {
        IFDropTable nativeDrops = super.getDropsPerCycle(crop);
        if (nativeDrops == null) return null;
        double nativeRounds = (1 + BlockSeedBed.getHarvestRoundBonus(upgradeTier)
            + fertilizerUnitCount * BlockFertilizerUnit.HARVEST_ROUND_BONUS)
            * (1 + advancedHarvestingUnitCount * BlockAdvancedHarvestingUnit.HARVEST_ROUND_MULTIPLIER);
        double upgradedRounds = (1 + BlockSeedBed.getHarvestRoundBonus(upgradeTier)
            + fertilizerStrength * BlockFertilizerUnit.HARVEST_ROUND_BONUS)
            * (1 + harvestingStrength * BlockAdvancedHarvestingUnit.HARVEST_ROUND_MULTIPLIER);
        IFDropTable drops = new IFDropTable();
        nativeDrops.addTo(drops, upgradedRounds / nativeRounds);
        return drops;
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        try {
            return switch (machineMode) {
                case MODE_INPUT -> inputSeeds();
                case MODE_FARM -> farmCrops();
                case MODE_OUTPUT -> outputSeeds();
                default -> CheckRecipeResultRegistry.NO_RECIPE;
            };
        } finally {
            activeCrop = -1;
        }
    }

    private CheckRecipeResult inputSeeds() {
        if (seedCapacity <= 0) return CheckRecipeResultRegistry.NONE;
        List<ItemStack> inputs = getStoredInputs();
        boolean inserted = false;
        CheckRecipeResult failure = CheckRecipeResultRegistry.NO_RECIPE;
        for (ItemStack candidate : inputs) {
            ISeedData seed = CropsNHUtils.getSeedData(candidate, false, true);
            if (seed == null || !CropsNHUtils.isStackValid(candidate)) continue;
            int crop = findInputCrop(candidate);
            if (crop < 0) {
                failure = CHECK_RECIPE_RESULT_SEEDS_FULL;
                continue;
            }
            activeCrop = crop;
            if (seed.getCrop().getMinSeedBedTier() > upgradeTier) {
                failure = CHECK_RECIPE_RESULT_SEED_BED_TIER_TOO_LOW;
                cropStatusKeys[crop] = bedTierStatus();
                continue;
            }
            if (!isCropSeedValid(crop, candidate)) {
                failure = CHECK_RECIPE_RESULT_CANNOT_GROW;
                cropStatusKeys[crop] = growthStatus();
                continue;
            }
            ItemStack existing = getCropSeed(crop);
            int existingCount = CropsNHUtils.isStackValid(existing) ? existing.stackSize : 0;
            if (existingCount >= seedCapacity) {
                failure = CHECK_RECIPE_RESULT_SEEDS_FULL;
                cropStatusKeys[crop] = capacityStatus();
                continue;
            }
            int amount = countMatching(inputs, candidate, seedCapacity - existingCount);
            if (amount <= 0) continue;
            ItemStack newSeed = candidate.copy();
            newSeed.stackSize = existingCount + amount;
            consumeMatching(inputs, candidate, amount);
            setCropSeed(crop, newSeed);
            cropStatusKeys[crop] = readyStatus();
            inserted = true;
        }
        if (!inserted) return failure;
        updateSlots();
        startCycle(5, 0);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private int findInputCrop(ItemStack seedStack) {
        int empty = -1;
        for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++) {
            ItemStack existing = getCropSeed(crop);
            if (CropsNHUtils.isStackValid(existing)) {
                if (GTUtility.areStacksEqual(existing, seedStack, false)) return crop;
            } else if (empty < 0) empty = crop;
        }
        for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++) {
            if (!CropsNHUtils.isStackValid(getCropSeed(crop)) && isCropSeedValid(crop, seedStack)) return crop;
        }
        return empty;
    }

    private static int countMatching(List<ItemStack> inputs, ItemStack template, int maximum) {
        int found = 0;
        for (ItemStack input : inputs) {
            if (!CropsNHUtils.isStackValid(input) || !GTUtility.areStacksEqual(input, template, false)) continue;
            found += Math.min(maximum - found, input.stackSize);
            if (found >= maximum) break;
        }
        return found;
    }

    private static void consumeMatching(List<ItemStack> inputs, ItemStack template, int amount) {
        for (ItemStack input : inputs) {
            if (amount <= 0) return;
            if (!CropsNHUtils.isStackValid(input) || !GTUtility.areStacksEqual(input, template, false)) continue;
            int consumed = Math.min(amount, input.stackSize);
            input.stackSize -= consumed;
            amount -= consumed;
        }
    }

    private CheckRecipeResult farmCrops() {
        if (upgradeTier < BlockSeedBed.MIN_TIER || seedCapacity <= 0) return CheckRecipeResultRegistry.NO_RECIPE;
        boolean previousFertilizer = hasFertilizer;
        boolean completed = false;
        try {
            hasFertilizer = true;
            List<Integer> candidates = new ArrayList<>();
            CheckRecipeResult failure = CheckRecipeResultRegistry.NO_RECIPE;
            for (int crop = 0; crop < IndustrialFarmInventory.CROP_COUNT; crop++) {
                activeCrop = crop;
                ISeedData seed = CropsNHUtils.getSeedData(getCropSeed(crop), false, true);
                if (seed == null) {
                    cropStatusKeys[crop] = emptyStatus();
                    continue;
                }
                CheckRecipeResult result = validateCrop(seed);
                if (!result.wasSuccessful()) {
                    failure = result;
                    continue;
                }
                candidates.add(crop);
            }
            if (candidates.isEmpty()) return failure;
            int count = countSeeds(candidates);
            var fluids = getStoredFluids();
            var supplies = IndustrialFarmFluids.plan(fluids, getWaterRequirement(count),
                getFertilizerRequirement(count), fertilizerUnitCount > 0);
            if (!supplies.hasWater()) {
                candidates.forEach(crop -> cropStatusKeys[crop] = waterStatus());
                return NOT_ENOUGH_WATER;
            }
            if (fertilizerUnitCount > 0 && !supplies.hasFertilizer()) {
                candidates.forEach(crop -> cropStatusKeys[crop] = fertilizerStatus());
                return NOT_ENOUGH_Enriched_FERTILIZER;
            }
            hasFertilizer = supplies.hasFertilizer();
            IFDropTable nextTracker = new IFDropTable(outputTracker.save());
            List<Integer> growing = new ArrayList<>();
            for (int crop : candidates) {
                activeCrop = crop;
                ISeedData seed = CropsNHUtils.getSeedData(getCropSeed(crop), false, true);
                IFDropTable drops = getDropsPerCycle(seed);
                if (drops == null) {
                    cropStatusKeys[crop] = growthStatus();
                    continue;
                }
                drops.addTo(nextTracker, seed.getStack().stackSize);
                growing.add(crop);
            }
            if (growing.isEmpty()) return CHECK_RECIPE_RESULT_CANNOT_GROW;
            if (growing.size() != candidates.size()) {
                count = countSeeds(growing);
                supplies = IndustrialFarmFluids.plan(fluids, getWaterRequirement(count),
                    getFertilizerRequirement(count), fertilizerUnitCount > 0);
            }
            ItemStack[] drops = collectDrops(nextTracker, true);
            if (
                voidingMode.protectItem && drops.length > 0
                    && new ItemEjectionHelper(getOutputBusses(), true, true).ejectItems(Arrays.asList(drops), 1) <= 0
            ) {
                growing.forEach(crop -> cropStatusKeys[crop] = outputStatus());
                return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            }
            if (!supplies.commit(hasFertilizer)) return NOT_ENOUGH_WATER;
            outputTracker = nextTracker;
            mOutputItems = collectDrops(outputTracker, false);
            growing.forEach(crop -> cropStatusKeys[crop] = readyStatus());
            startCycle(CYCLE_DURATION, -expectedEUt);
            updateSlots();
            completed = true;
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } finally {
            if (!completed) hasFertilizer = previousFertilizer;
        }
    }

    private CheckRecipeResult validateCrop(ISeedData seed) {
        if (seed.getStack().stackSize > seedCapacity) {
            cropStatusKeys[activeCrop] = capacityStatus();
            return CHECK_RECIPE_RESULT_SEED_OVERFLOW;
        }
        if (seed.getCrop().getMinSeedBedTier() > upgradeTier) {
            cropStatusKeys[activeCrop] = bedTierStatus();
            return CHECK_RECIPE_RESULT_SEED_BED_TIER_TOO_LOW;
        }
        for (IGrowthRequirement requirement : seed.getCrop().getGrowthRequirements()) {
            if (
                !(requirement instanceof SubSoilRequirement)
                    && requirement instanceof IMachineGrowthRequirement machineRequirement
                    && !machineRequirement.canGrow(seed, getBaseMetaTileEntity(), new ItemStack[0])
            ) {
                cropStatusKeys[activeCrop] = growthStatus();
                return CHECK_RECIPE_RESULT_CANNOT_GROW;
            }
        }
        if (getGrowthSpeedUnscaled(seed) <= 0) {
            cropStatusKeys[activeCrop] = growthStatus();
            return CHECK_RECIPE_RESULT_CANNOT_GROW;
        }
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private int countSeeds(List<Integer> crops) {
        int count = 0;
        for (int crop : crops) count += getCropSeed(crop).stackSize;
        return count;
    }

    private static ItemStack[] collectDrops(IFDropTable tracker, boolean simulate) {
        List<ItemStack> drops = new ArrayList<>();
        for (var entry : tracker.entrySet()) {
            int amount = (int) Math.min(Integer.MAX_VALUE, Math.floor(entry.getValue()));
            if (amount <= 0) continue;
            ItemStack stack = entry.getKey().copy();
            stack.stackSize = amount;
            drops.add(stack);
            if (!simulate) tracker.setItemAmount(entry.getKey(), entry.getValue() - amount);
        }
        return drops.toArray(new ItemStack[0]);
    }

    private CheckRecipeResult outputSeeds() {
        if (!migrationReturns.isEmpty()) {
            ItemStack source = migrationReturns.get(0);
            ItemStack returned = source.copy();
            returned.stackSize = 1;
            int amount = new ItemEjectionHelper(getOutputBusses(), true).ejectItems(Arrays.asList(returned),
                source.stackSize);
            if (amount <= 0) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            returned.stackSize = amount;
            source.stackSize -= amount;
            if (source.stackSize <= 0) migrationReturns.remove(0);
            mOutputItems = new ItemStack[] { returned };
            startCycle(5, 0);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
        IFDropTable retained = outputTracker;
        try {
            for (int checked = 0; checked < IndustrialFarmInventory.CROP_COUNT; checked++) {
                activeCrop = (outputCursor + checked) % IndustrialFarmInventory.CROP_COUNT;
                if (!CropsNHUtils.isStackValid(getSeedStack())) continue;
                outputTracker = new IFDropTable();
                CheckRecipeResult result = super.checkProcessing();
                if (result.wasSuccessful()) {
                    outputCursor = (activeCrop + 1) % IndustrialFarmInventory.CROP_COUNT;
                    return result;
                }
                cropStatusKeys[activeCrop] = outputStatus();
                return result;
            }
            return CheckRecipeResultRegistry.NO_RECIPE;
        } finally {
            outputTracker = retained;
        }
    }

    private void startCycle(int duration, long eut) {
        mMaxProgresstime = duration;
        lEUt = eut;
        mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        mEfficiencyIncrease = 10000;
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    public String getCropStatus(int crop) {
        return StatCollector.translateToLocal(getCropStatusKey(crop));
    }

    public String getCropStatusKey(int crop) {
        return cropStatusKeys[crop] == null
            ? CropsNHUtils.isStackValid(getCropSeed(crop)) ? readyStatus() : emptyStatus()
            : cropStatusKeys[crop];
    }

    private static String emptyStatus() {
        // #tr gtng.industrial_farm.plot.empty
        // # Empty
        // # zh_CN 空闲
        return "gtng.industrial_farm.plot.empty";
    }

    private static String readyStatus() {
        // #tr gtng.industrial_farm.plot.ready
        // # Ready
        // # zh_CN 就绪
        return "gtng.industrial_farm.plot.ready";
    }

    private static String capacityStatus() {
        // #tr gtng.industrial_farm.plot.capacity
        // # Page capacity reached
        // # zh_CN 本页容量已满
        return "gtng.industrial_farm.plot.capacity";
    }

    private static String bedTierStatus() {
        // #tr gtng.industrial_farm.plot.bed_tier
        // # Bed tier
        // # zh_CN 苗床等级不足
        return "gtng.industrial_farm.plot.bed_tier";
    }

    private static String growthStatus() {
        // #tr gtng.industrial_farm.plot.growth
        // # Conditions
        // # zh_CN 生长条件不符
        return "gtng.industrial_farm.plot.growth";
    }

    private static String waterStatus() {
        // #tr gtng.industrial_farm.plot.water
        // # Water
        // # zh_CN 缺水
        return "gtng.industrial_farm.plot.water";
    }

    private static String fertilizerStatus() {
        // #tr gtng.industrial_farm.plot.fertilizer
        // # Fertilizer
        // # zh_CN 缺富集肥料
        return "gtng.industrial_farm.plot.fertilizer";
    }

    private static String outputStatus() {
        // #tr gtng.industrial_farm.plot.output
        // # Output full
        // # zh_CN 输出已满
        return "gtng.industrial_farm.plot.output";
    }

    private static StructureError farmError(String suffix) {
        String key = Reference.MOD_ID + "_tooltip.industrialFarm.structure.error." + suffix;
        return switch (suffix) {
            case "EEUCount" -> StructureErrors.of(key,
                TranslatableText.literal(BlockEnvironmentalEnhancementUnit.MAX_UPGRADE_COUNT));
            case "AHUCount" -> StructureErrors.of(key,
                TranslatableText.literal(BlockAdvancedHarvestingUnit.MAX_UPGRADE_COUNT));
            default -> StructureErrors.of(key);
        };
    }

    private long getFarmPowerUsage() {
        long basePower = BlockSeedBed.getBaseEUt(upgradeTier);
        return basePower
            + (long) (basePower * BlockEnvironmentalEnhancementUnit.BASE_POWER_INCREASE
                * environmentalEnhancementUnitCount)
            + (long) (basePower * BlockGrowthAccelerationUnit.BASE_POWER_INCREASE * growthAccelerationUnitCount)
            + (long) (basePower * BlockFertilizerUnit.BASE_POWER_INCREASE * fertilizerUnitCount)
            + (long) (basePower * BlockAdvancedHarvestingUnit.BASE_POWER_INCREASE * advancedHarvestingUnitCount);
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>();
        // #tr gtng.industrial_farm.scanner.capacity
        // # Total seeds: %s / %s
        // # zh_CN 种子总量：%s / %s
        lines.add(StatCollector.translateToLocalFormatted("gtng.industrial_farm.scanner.capacity", getUsedSeedCount(),
            getTotalSeedCapacity()));
        // #tr gtng.industrial_farm.scanner.page_capacity
        // # Independent capacity per seed type: %s
        // # zh_CN 每种种子独立容量：%s
        lines.add(StatCollector.translateToLocalFormatted("gtng.industrial_farm.scanner.page_capacity", seedCapacity));
        // #tr gtng.industrial_farm.scanner.plots
        // # Seed types: %s / 36
        // # zh_CN 种子种类：%s / 36
        lines
            .add(StatCollector.translateToLocalFormatted("gtng.industrial_farm.scanner.plots", getOccupiedCropCount()));
        // #tr gtng.industrial_farm.scanner.power
        // # Farming power: %s EU/t
        // # zh_CN 种植功率：%s EU/t
        lines.add(StatCollector.translateToLocalFormatted("gtng.industrial_farm.scanner.power", expectedEUt));
        // #tr gtng.industrial_farm.scanner.overclock
        // # Perfect overclocks: %s | Throughput: x%s
        // # zh_CN 完美超频：%s 级 | 超频吞吐倍率：x%s
        lines.add(StatCollector.translateToLocalFormatted("gtng.industrial_farm.scanner.overclock", expectedOCs,
            getPerfectOverclockMultiplier()));
        // #tr gtng.industrial_farm.scanner.modules
        // # Effective modules: environment %s, growth %s, fertilizer %s, harvesting %s
        // # zh_CN 模块效力：环境 %s，生长 %s，施肥 %s，收获 %s
        lines.add(StatCollector.translateToLocalFormatted("gtng.industrial_farm.scanner.modules", environmentalStrength,
            growthAccelerationStrength, fertilizerStrength, harvestingStrength));
        return lines.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.industrial_farm.end_casing
        // # Front or rear Agricultural Casing
        // # zh_CN 前后端农业机械方块
        String endCasing = StatCollector.translateToLocal("gtng.industrial_farm.end_casing");
        tt.addMachineType(StatCollector.translateToLocal(getLocalNameKey()))
            // #tr gtng.industrial_farm.workflow
            // # Grows CropsNH crops with independent environment cards
            // # zh_CN 为 CropsNH 作物提供独立环境并自动种植
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.workflow"))
            // #tr gtng.industrial_farm.capacity
            // # Grows up to 36 seed types, each with 12x the same-tier Industrial Farm capacity
            // # zh_CN 最多种植 36 种种子，每种容量为同阶工业农场的 12 倍
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.capacity"))
            // #tr gtng.industrial_farm.module_tiers
            // # Each module tier above its minimum adds 25% effectiveness
            // # zh_CN 升级模块每高于自身最低等级 1 级，效力增加 25%
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.module_tiers"))
            // #tr gtng.industrial_farm.perfect_overclock
            // # Each perfect overclock gives 4x EU/t and throughput; stronger OC modules add throughput
            // # zh_CN 每级无损超频使功耗与吞吐乘 4；高阶超频模块额外提高吞吐
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.perfect_overclock"))
            // #tr gtng.industrial_farm.consumption
            // # Water and fertilizer scale with seeds and throughput; stronger fertilizer units reduce fertilizer use
            // # zh_CN 水肥消耗随种子数量及吞吐增加；高阶施肥模块降低肥料消耗
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.consumption")).addSeparator()
            // #tr gtng.industrial_farm.plot_inventory
            // # Seeds sort automatically; each seed type has two environment cards and needs no sub-soil
            // # zh_CN 种子自动归集，每种拥有两个独立环境卡槽，无需底土
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.plot_inventory"))
            // #tr gtng.industrial_farm.upgrades
            // # Uniform seed beds set capacity, power and crop tier; upgrade modules may use different tiers
            // # zh_CN 苗床须同级，决定容量、功耗与作物等级；升级模块可单独选择等级
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.upgrades"))
            // #tr gtng.industrial_farm.upgrade_limits
            // # Industrial Farm upgrade limits apply; normal and overclocked growth units stack
            // # zh_CN 沿用工业农场的升级数量限制；普通与超频生长加速可叠加
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.upgrade_limits"))
            // #tr gtng.industrial_farm.continuous_growth
            // # Retains partial growth and harvests; blocked crops do not stop other crops
            // # zh_CN 保留生长进度与小数产出，条件不符的作物不影响其他作物
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.continuous_growth"))
            // #tr gtng.industrial_farm.energy
            // # Multi-amp hatches need no upgrade; glass does not limit energy hatch tiers
            // # zh_CN 多安能源仓无需升级；玻璃不限制能源仓等级
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.energy"))
            // #tr gtng.industrial_farm.energy_limits
            // # At most one multi-amp hatch; cannot mix with normal energy hatches; no laser hatches
            // # zh_CN 最多 1 个多安能源仓，不能与普通能源仓混用；不支持激光仓
            .addInfo(StatCollector.translateToLocal("gtng.industrial_farm.energy_limits"))
            .beginStructureBlock(5, 4, SLICES + 2, true)
            // #tr gtng.industrial_farm.controller
            // # Front center on the seed-bed layer
            // # zh_CN 种床层正面中央
            .addController(StatCollector.translateToLocal("gtng.industrial_farm.controller"))
            // #tr gtng.industrial_farm.casing
            // # Agricultural Casing
            // # zh_CN 农业机械方块
            .addCasing("40+", StatCollector.translateToLocal("gtng.industrial_farm.casing"), false)
            // #tr gtng.industrial_farm.fixed_size
            // # 12 growing sections, 36 uniform seed beds, 12 upgrade positions
            // # zh_CN 12 个种植段、36 个同级苗床、12 个升级位置
            .addStructureInfo(StatCollector.translateToLocal("gtng.industrial_farm.fixed_size"))
            // #tr gtng.industrial_farm.glass
            // # 48 Borosilicate Glass blocks (standard variant)
            // # zh_CN 48 块硼玻璃（普通型号）
            .addStructureInfo(StatCollector.translateToLocal("gtng.industrial_farm.glass"))
            // #tr gtng.industrial_farm.roof
            // # Each of the 12 roof wooden frames may be replaced by an upgrade module
            // # zh_CN 顶部 12 个木框架均可替换为升级模块
            .addStructureInfo(StatCollector.translateToLocal("gtng.industrial_farm.roof"))
            .addEnergyHatch("1+", endCasing, 1).addMaintenanceHatch("1", endCasing, 1).addInputBus("0+", endCasing, 1)
            .addInputHatch("1+", endCasing, 1).addOutputBus("1+", endCasing, 1)
            // #tr gtng.industrial_farm.channel
            // # Subchannel if_tier sets the industrial farm tier
            // # zh_CN 子信道 if_tier 决定工业农场等级
            .addStructureInfo(StatCollector.translateToLocal("gtng.industrial_farm.channel")).toolTipFinisher();
        return tt;
    }
}
