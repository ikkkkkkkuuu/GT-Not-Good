package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.GregTechAPI.sBlockCasings2;
import static gregtech.api.GregTechAPI.sBlockFrames;
import static gregtech.api.enums.GTValues.emptyItemStackArray;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_DRILL;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_DRILL_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_DRILL_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_ORE_DRILL_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.getCasingTextureForId;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.api.gui.OreEntryInfo;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidMinerConfigGuiFactory;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidMinerGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.common.utils.OreCrushedUtil;
import com.xyp.gtnotgood.common.utils.VoidMinerUtilityShim;

import bwcrossmod.galacticgreg.VoidMinerUtility;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.GTMockWorld;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.ores.OreInfo;
import gregtech.common.ores.OreManager;
import gregtech.common.tileentities.machines.outputme.MTEHatchOutputBusME;

/**
 * Electric Large Void Miner using the old large steam void miner body and Crust Matter Aggregator ore logic.
 * <p>
 * The physical structure remains the 7x9x7 steel drill frame from GT-Not-Cool, but the machine is a normal GregTech
 * electric multiblock: its work cost is EU/t from energy or exotic-energy hatches, and UU-Matter is only drained from
 * fluid input hatches when directional mode is enabled.
 */
public class LargeVoidMiner extends GTNGMultiBlockBase<LargeVoidMiner> implements ISurvivalConstructable {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int HORIZONTAL_OFF_SET = 3;
    private static final int VERTICAL_OFF_SET = 7;
    private static final int DEPTH_OFF_SET = 1;
    private static final int DIMENSION_SLOT_COUNT = 25;

    public static final int CYCLE_TICKS = 400;
    public static final long[] BASE_EUT = { 30L, 120L, 480L };
    private static final double[] GRADE_COEF = { 0.5d, 1.0d, 2.0d };
    private static final double[] OUTPUT_COEF = { 0.5d, 2.0d, 5.0d };
    private static final double ORES_PER_UNIT = 10.0d;
    public static final double[] ORE_MODE_ENERGY_BONUS = { 0.0d, 1.0d, 2.0d };
    public static final int[] FORTUNE_LEVELS = { 3, 5, 7, 9, 11, 13, 15 };
    public static final double[] FORTUNE_ENERGY_BONUS = { 0.0d, 0.5d, 1.0d, 1.5d, 2.0d, 2.5d, 3.0d };
    public static final double SLOT_ENERGY_PER_EXTRA = 0.2d;

    private static final String ITEM_DIM_DISPLAY_CLASS = "gtneioreplugin.plugin.item.ItemDimensionDisplay";
    private static final Map<String, String> ABBR_TO_DIM_NAME = new HashMap<>();

    static {
        ABBR_TO_DIM_NAME.put("Ow", "Overworld");
        ABBR_TO_DIM_NAME.put("Ne", "Nether");
        ABBR_TO_DIM_NAME.put("ED", "The End");
        ABBR_TO_DIM_NAME.put("TF", "Twilight Forest");
        ABBR_TO_DIM_NAME.put("EA", "EndAsteroid");
        ABBR_TO_DIM_NAME.put("Eg", "dimensionDarkWorld");
        ABBR_TO_DIM_NAME.put("Mo", "moon");
        ABBR_TO_DIM_NAME.put("Ma", "mars");
        ABBR_TO_DIM_NAME.put("As", "asteroids");
        ABBR_TO_DIM_NAME.put("De", "deimos");
        ABBR_TO_DIM_NAME.put("Ph", "phobos");
        ABBR_TO_DIM_NAME.put("Ca", "callisto");
        ABBR_TO_DIM_NAME.put("Ce", "ceres");
        ABBR_TO_DIM_NAME.put("Eu", "europa");
        ABBR_TO_DIM_NAME.put("Ga", "ganymed");
        ABBR_TO_DIM_NAME.put("Rb", "ross128b");
        ABBR_TO_DIM_NAME.put("Io", "iojupiter");
        ABBR_TO_DIM_NAME.put("Me", "mercury");
        ABBR_TO_DIM_NAME.put("Ve", "venus");
        ABBR_TO_DIM_NAME.put("En", "enceladus");
        ABBR_TO_DIM_NAME.put("Mi", "miranda");
        ABBR_TO_DIM_NAME.put("Ob", "oberon");
        ABBR_TO_DIM_NAME.put("Ti", "titan");
        ABBR_TO_DIM_NAME.put("Ra", "ross128ba");
        ABBR_TO_DIM_NAME.put("Pr", "proteus");
        ABBR_TO_DIM_NAME.put("Tr", "triton");
        ABBR_TO_DIM_NAME.put("Ha", "haumea");
        ABBR_TO_DIM_NAME.put("KB", "kuiperbelt");
        ABBR_TO_DIM_NAME.put("MM", "makemake");
        ABBR_TO_DIM_NAME.put("Pl", "pluto");
        ABBR_TO_DIM_NAME.put("BC", "barnarda2");
        ABBR_TO_DIM_NAME.put("BE", "barnarda4");
        ABBR_TO_DIM_NAME.put("BF", "barnarda5");
        ABBR_TO_DIM_NAME.put("CB", "centauribb");
        ABBR_TO_DIM_NAME.put("TE", "tcetie");
        ABBR_TO_DIM_NAME.put("VB", "vega1");
        ABBR_TO_DIM_NAME.put("An", "anubis");
        ABBR_TO_DIM_NAME.put("Ho", "horus");
        ABBR_TO_DIM_NAME.put("Mh", "maahes");
        ABBR_TO_DIM_NAME.put("MB", "asteroidbeltmehen");
        ABBR_TO_DIM_NAME.put("Np", "neper");
        ABBR_TO_DIM_NAME.put("Se", "seth");
        ABBR_TO_DIM_NAME.put("DD", "Underdark");
    }

    private static Boolean pluginLoaded;
    private static Class<?> itemDimDisplayClass;
    private static java.lang.reflect.Method getDimensionMethod;
    private static IStructureDefinition<LargeVoidMiner> STRUCTURE_DEFINITION;

    private static final String[][] SHAPE = new String[][] {
        { "       ", "       ", "       ", "   B   ", "       ", "       ", "       " },
        { "       ", "       ", "       ", "   B   ", "       ", "       ", "       " },
        { "       ", "       ", "       ", "   B   ", "       ", "       ", "       " },
        { "       ", "       ", "   B   ", "  BCB  ", "   B   ", "       ", "       " },
        { "       ", "       ", "   B   ", "  BCB  ", "   B   ", "       ", "       " },
        { "       ", "       ", "   B   ", "  BCB  ", "   B   ", "       ", "       " },
        { "       ", " B   B ", "  DAD  ", "  ACA  ", "  DAD  ", " B   B ", "       " },
        { "  D D  ", " BA~AB ", " A   A ", " B C B ", " A   A ", " BABAB ", "       " },
        { "  E E  ", " BBBBB ", "EB   BE", " B C B ", "EB   BE", " BBBBB ", "  E E  " } };

    private int mCountCasing;

    public String lastDimAbbr = "None";
    public String mLastOreName = "";
    public boolean dropMapValid;
    public int mCurrentDimId;
    public int mOreMode;
    public int mFortuneLevel = 3;

    private boolean mDefaultDimSupported;
    private final ItemStack[] mPluginSlots = new ItemStack[DIMENSION_SLOT_COUNT];
    private final Set<GTUtility.ItemId> mFilteredOres = new HashSet<>();
    private boolean mDirectionalMode;
    private final Set<GTUtility.ItemId> mDirectionalOres = new HashSet<>();
    private double mUuAccumulator;
    private final List<PoolDim> mPool = new ArrayList<>();
    private boolean mPoolDirty;
    private IInventory mPluginSlotInventory;
    private int mActiveGrade = -1;
    private double mOreAccumulator;

    public LargeVoidMiner(String aName) {
        super(aName);
    }

    public LargeVoidMiner(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new LargeVoidMiner(this.mName);
    }

    @Override
    public IStructureDefinition<LargeVoidMiner> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<LargeVoidMiner>builder()
                .addShape(STRUCTURE_PIECE_MAIN, transpose(SHAPE))
                .addElement('A', ofBlock(sBlockCasings2, 13))
                .addElement('B', buildSteelCasingElement(1))
                .addElement('C', onElementPass(t -> ++t.mCountCasing, ofBlock(sBlockCasings2, 0)))
                .addElement('D', ofBlock(sBlockFrames, Materials.Steel.mMetaItemSubID))
                .addElement('E', buildSteelCasingElement(2))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    private static com.gtnewhorizon.structurelib.structure.IStructureElement<LargeVoidMiner> buildSteelCasingElement(
        int hint) {
        return ofChain(
            buildHatchAdder(LargeVoidMiner.class)
                .atLeast(Energy.or(ExoticEnergy), InputBus, InputHatch, OutputBus, Maintenance)
                .casingIndex(getSteelCasingTextureId())
                .hint(hint)
                .build(),
            onElementPass(t -> ++t.mCountCasing, ofBlock(sBlockCasings2, 0)));
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
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
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        mCountCasing = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET, errors)) return;
        checkCasingMin(errors, mCountCasing, 3);
        if (mOutputBusses.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputBus, 0, 1));
        int energyHatchCount = mEnergyHatches.size() + mExoticEnergyHatches.size();
        if (energyHatchCount < 1)
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Energy, energyHatchCount, 1));
        if (shouldCheckMaintenance() && mMaintenanceHatches.isEmpty())
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, Maintenance, 0, 1));
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
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        if (aBaseMetaTileEntity.getWorld() != null) {
            mCurrentDimId = aBaseMetaTileEntity.getWorld().provider.dimensionId;
        }
        rebuildPool();
    }

    private static synchronized boolean isPluginLoaded() {
        if (pluginLoaded == null) {
            if (!Loader.isModLoaded("gtneioreplugin")) {
                pluginLoaded = false;
            } else {
                try {
                    itemDimDisplayClass = Class
                        .forName(ITEM_DIM_DISPLAY_CLASS, true, LargeVoidMiner.class.getClassLoader());
                    getDimensionMethod = itemDimDisplayClass.getMethod("getDimension", ItemStack.class);
                    pluginLoaded = true;
                } catch (ClassNotFoundException | NoClassDefFoundError | NoSuchMethodException e) {
                    pluginLoaded = false;
                }
            }
        }
        return pluginLoaded;
    }

    public static boolean isDimensionDisplayItem(ItemStack stack) {
        if (stack == null) return false;
        if (!isPluginLoaded()) return false;
        Item item = stack.getItem();
        return item != null && itemDimDisplayClass != null && itemDimDisplayClass.isInstance(item);
    }

    private String readDimensionAbbrFromStack(ItemStack stack) {
        if (!isDimensionDisplayItem(stack)) return null;
        try {
            Object result = getDimensionMethod.invoke(null, stack);
            if (result instanceof String) return (String) result;
        } catch (Exception e) {
            GTNotGood.LOG.warn("Failed to read GTNEIOrePlugin dimension display item", e);
        }
        return null;
    }

    private List<ItemStack> getDimensionStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        Collections.addAll(stacks, mPluginSlots);
        return stacks;
    }

    private List<String> collectDimensionAbbrs() {
        List<String> abbrs = new ArrayList<>();
        for (ItemStack stack : getDimensionStacks()) {
            String abbr = readDimensionAbbrFromStack(stack);
            if (abbr == null || "None".equals(abbr) || abbrs.contains(abbr)) continue;
            abbrs.add(abbr);
        }
        return abbrs;
    }

    private PoolDim createPoolDim(String dimAbbr, String dimName) {
        if (dimName == null) {
            return new PoolDim(dimAbbr, null, new VoidMinerUtility.DropMap(), new VoidMinerUtility.DropMap());
        }
        VoidMinerUtility.DropMap dropMap = VoidMinerUtilityShim.getDropMap(dimName);
        VoidMinerUtility.DropMap extraDropMap = VoidMinerUtilityShim.getExtraDropMap(dimName);
        dropMap.isDistributionCached(extraDropMap);
        return new PoolDim(dimAbbr, dimName, dropMap, extraDropMap);
    }

    private void rebuildPool() {
        mPoolDirty = false;
        mPool.clear();
        List<String> abbrs = collectDimensionAbbrs();
        if (abbrs.isEmpty()) {
            lastDimAbbr = "None";
            mDefaultDimSupported = false;
            String dimName = VoidMinerUtilityShim.dimIdToName(mCurrentDimId);
            if (dimName != null) {
                mDefaultDimSupported = true;
                mPool.add(createPoolDim("None", dimName));
            }
        } else {
            mDefaultDimSupported = false;
            StringBuilder summary = new StringBuilder();
            for (String abbr : abbrs) {
                if (summary.length() > 0) summary.append("+");
                summary.append(abbr);
                String dimName = ABBR_TO_DIM_NAME.get(abbr);
                if (dimName == null) {
                    GTNotGood.LOG.warn("Unknown GTNEIOrePlugin dimension abbreviation: {}", abbr);
                }
                mPool.add(createPoolDim(abbr, dimName));
            }
            lastDimAbbr = summary.toString();
        }
        float totalWeight = 0.0f;
        for (PoolDim pd : mPool) {
            totalWeight += pd.dropMap.getTotalWeight();
        }
        dropMapValid = totalWeight > 0.0f;
    }

    public void markPoolDirty() {
        mPoolDirty = true;
    }

    public void forceRefreshPool() {
        markPoolDirty();
        rebuildPool();
        markBaseDirty();
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot == 1) return 1;
        return super.getSlotLimit(slot);
    }

    private boolean isPoolCurrent() {
        List<String> current = collectDimensionAbbrs();
        if (current.isEmpty()) {
            World world = getBaseMetaTileEntity() == null ? null : getBaseMetaTileEntity().getWorld();
            if (world != null && world.provider.dimensionId != mCurrentDimId) return false;
            if (mPool.isEmpty()) return !mDefaultDimSupported;
            return mPool.size() == 1 && "None".equals(mPool.get(0).dimAbbr);
        }
        if (mPool.size() != current.size()) return false;
        for (PoolDim pd : mPool) {
            if (!current.contains(pd.dimAbbr)) return false;
        }
        return true;
    }

    private void rebuildPoolIfNeeded() {
        if (mPoolDirty || !isPoolCurrent()) {
            World world = getBaseMetaTileEntity() == null ? null : getBaseMetaTileEntity().getWorld();
            if (world != null) mCurrentDimId = world.provider.dimensionId;
            rebuildPool();
        }
    }

    public String getDimensionDisplayName() {
        if (mPool.isEmpty()) {
            String dimName = VoidMinerUtilityShim.dimIdToName(mCurrentDimId);
            return dimName != null ? dimName : "Dim " + mCurrentDimId;
        }
        StringBuilder sb = new StringBuilder();
        for (PoolDim pd : mPool) {
            if (sb.length() > 0) sb.append("+");
            sb.append("None".equals(pd.dimAbbr) ? pd.dimName : pd.dimAbbr);
        }
        return sb.toString();
    }

    private boolean hasUsableDimension() {
        return !mPool.isEmpty();
    }

    public boolean getDirectionalMode() {
        return mDirectionalMode;
    }

    public void toggleDirectionalMode(EntityPlayer player) {
        mDirectionalMode = !mDirectionalMode;
        forceRefreshPool();
        mMaxProgresstime = 0;
        mProgresstime = 0;
        if (player != null) {
            // #tr chat.gtnotgood.largeVoidMiner.directional.on
            // # Directional mode enabled
            // # zh_CN 定向模式已开启
            // #tr chat.gtnotgood.largeVoidMiner.directional.off
            // # Directional mode disabled
            // # zh_CN 定向模式已关闭
            GTUtility.sendChatToPlayer(
                player,
                StatCollector.translateToLocal(
                    mDirectionalMode ? "chat.gtnotgood.largeVoidMiner.directional.on"
                        : "chat.gtnotgood.largeVoidMiner.directional.off"));
        }
    }

    public boolean isOreFiltered(GTUtility.ItemId id) {
        return id != null && mFilteredOres.contains(id);
    }

    public void setOreFiltered(GTUtility.ItemId id, boolean filtered) {
        if (id == null) return;
        if (filtered) {
            mFilteredOres.add(id);
        } else {
            mFilteredOres.remove(id);
        }
        markBaseDirty();
    }

    public boolean isOreAimed(GTUtility.ItemId id) {
        return id != null && mDirectionalOres.contains(id);
    }

    public void setOreAimed(GTUtility.ItemId id, boolean aimed) {
        if (id == null) return;
        if (aimed) {
            mDirectionalOres.add(id);
        } else {
            mDirectionalOres.remove(id);
        }
        markBaseDirty();
    }

    public float getFilteredWeightSum() {
        float sum = 0.0f;
        for (PoolDim pd : mPool) {
            for (Map.Entry<GTUtility.ItemId, Float> entry : pd.dropMap.getInternalMap()
                .entrySet()) {
                if (mFilteredOres.contains(entry.getKey())) sum += entry.getValue();
            }
        }
        return sum;
    }

    public float getDirectionalWeightSum() {
        float sum = 0.0f;
        for (PoolDim pd : mPool) {
            for (Map.Entry<GTUtility.ItemId, Float> entry : pd.dropMap.getInternalMap()
                .entrySet()) {
                if (mDirectionalOres.contains(entry.getKey())) sum += entry.getValue();
            }
        }
        return sum;
    }

    private double getDirectionalLowestWeightSum() {
        List<Float> weights = new ArrayList<>();
        for (PoolDim pd : mPool) {
            for (Map.Entry<GTUtility.ItemId, Float> entry : pd.dropMap.getInternalMap()
                .entrySet()) {
                if (mDirectionalOres.contains(entry.getKey())) weights.add(entry.getValue());
            }
        }
        if (weights.isEmpty()) return 0.0d;
        Collections.sort(weights);
        double sum = 0.0d;
        for (int i = 0; i < Math.min(3, weights.size()); i++) {
            sum += weights.get(i);
        }
        return sum;
    }

    public double getDirectionalFactor() {
        double sum = getDirectionalLowestWeightSum();
        return sum <= 0.0d ? 0.0d : 1.0d + 25.0d / sum;
    }

    public double getWeightIncreasePercent() {
        if (mDirectionalMode) {
            double sum = getDirectionalLowestWeightSum();
            return sum <= 0.0d ? 0.0d : 2500.0d / sum;
        }
        return getFilterCostIncrease();
    }

    public double getDimensionIncreasePercent() {
        int slotCount = getDimensionSlotCount();
        int extra = Math.max(0, slotCount - 1);
        return mDirectionalMode ? 200.0d + 20.0d * extra : 20.0d * extra;
    }

    public double getUUMultiplier() {
        return mDirectionalMode
            ? (1.0d + ORE_MODE_ENERGY_BONUS[clampOreMode()] + FORTUNE_ENERGY_BONUS[getFortuneIndex(mFortuneLevel)])
                * getDirectionalFactor()
            : 0.0d;
    }

    public double getUURatePerSecond() {
        return getUUMultiplier();
    }

    public void clearCurrentModeConfig() {
        if (mDirectionalMode) {
            mDirectionalOres.clear();
        } else {
            mFilteredOres.clear();
        }
        markBaseDirty();
    }

    public List<OreEntryInfo> getOreEntries() {
        List<OreEntryInfo> entries = new ArrayList<>();
        if (mPool.isEmpty()) return entries;
        Map<GTUtility.ItemId, OreEntryInfo> byId = new LinkedHashMap<>();
        for (PoolDim pd : mPool) {
            for (Map.Entry<GTUtility.ItemId, Float> entry : pd.dropMap.getInternalMap()
                .entrySet()) {
                OreEntryInfo info = byId.get(entry.getKey());
                if (info == null) {
                    info = new OreEntryInfo(
                        entry.getKey()
                            .getItemStack(),
                        entry.getValue(),
                        new ArrayList<>(),
                        mFilteredOres.contains(entry.getKey()),
                        mDirectionalOres.contains(entry.getKey()));
                    byId.put(entry.getKey(), info);
                } else {
                    info.weight += entry.getValue();
                }
                info.dimAbbrs.add(pd.dimAbbr);
            }
        }
        entries.addAll(byId.values());
        return entries;
    }

    public IInventory getPluginSlotInventory() {
        if (mPluginSlotInventory == null) mPluginSlotInventory = new PluginSlotInventory();
        return mPluginSlotInventory;
    }

    public int getActiveGrade() {
        return mActiveGrade < 0 ? getVoltageGrade() : mActiveGrade;
    }

    public int getVoltageGrade() {
        int voltageTier = GTUtility.getTier(getMaxInputVoltage());
        if (voltageTier >= 3) return 2;
        if (voltageTier >= 2) return 1;
        return 0;
    }

    public long getEnergyCostPerTick() {
        return Math.max(1L, Math.round(BASE_EUT[getActiveGrade()] * getEnergyMultiplier()));
    }

    public long getEnergyCostPerSecond() {
        return getEnergyCostPerTick() * 20L;
    }

    public double getFilterCostIncrease() {
        int k = mFilteredOres.size();
        return getFilteredWeightSum() + 5.0d * k * (k - 1) / 2.0d;
    }

    public double getEnergyMultiplier() {
        double modeBonus = ORE_MODE_ENERGY_BONUS[clampOreMode()];
        double fortuneBonus = FORTUNE_ENERGY_BONUS[getFortuneIndex(mFortuneLevel)];
        if (mDirectionalMode) {
            return (1.0d + modeBonus + fortuneBonus)
                * (3.0d + SLOT_ENERGY_PER_EXTRA * Math.max(0, getDimensionSlotCount() - 1))
                * getDirectionalFactor();
        }
        return (1.0d + modeBonus + fortuneBonus)
            * (1.0d + SLOT_ENERGY_PER_EXTRA * Math.max(0, getDimensionSlotCount() - 1))
            * (1.0d + getFilterCostIncrease() / 100.0d);
    }

    private int getDimensionSlotCount() {
        int slotCount = 0;
        for (ItemStack stack : getDimensionStacks()) {
            if (isDimensionDisplayItem(stack)) slotCount++;
        }
        return slotCount;
    }

    public void cycleOreMode() {
        mOreMode = (mOreMode + 1) % 3;
        if (mOreMode == 0) mFortuneLevel = 3;
        markBaseDirty();
    }

    public void cycleFortuneLevel() {
        if (mOreMode == 0) {
            mFortuneLevel = 3;
        } else {
            int idx = getFortuneIndex(mFortuneLevel);
            mFortuneLevel = FORTUNE_LEVELS[(idx + 1) % FORTUNE_LEVELS.length];
        }
        markBaseDirty();
    }

    private int clampOreMode() {
        return Math.min(Math.max(mOreMode, 0), 2);
    }

    private static int getFortuneIndex(int level) {
        return Math.min(Math.max((level - 3) / 2, 0), 6);
    }

    @Override
    @NotNull
    public CheckRecipeResult checkProcessing() {
        rebuildPoolIfNeeded();
        if (!hasUsableDimension()) {
            // #tr gui.gtnotgood.largeVoidMiner.no_dimension
            // # No supported dimension
            // # zh_CN 没有可用维度
            return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidMiner.no_dimension");
        }
        if (!dropMapValid) {
            // #tr gui.gtnotgood.largeVoidMiner.no_ores
            // # No ores in selected dimension
            // # zh_CN 当前维度没有矿石
            return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidMiner.no_ores");
        }
        if (mDirectionalMode && getDirectionalWeightSum() <= 0.0f) {
            // #tr gui.gtnotgood.largeVoidMiner.no_direction
            // # No targeted ores
            // # zh_CN 没有定向矿石
            return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidMiner.no_direction");
        }
        if (mDirectionalMode && getUUMatterTotal() <= 0) {
            // #tr gui.gtnotgood.largeVoidMiner.no_uumatter
            // # No UU-Matter
            // # zh_CN 没有UU物质
            return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidMiner.no_uumatter");
        }
        mActiveGrade = getVoltageGrade();
        lEUt = -getEnergyCostPerTick();
        mMaxProgresstime = CYCLE_TICKS;
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        mOutputItems = emptyItemStackArray;
        updateSlots();
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected void outputAfterRecipe() {
        if (mPool.isEmpty() || !dropMapValid || mActiveGrade < 0) {
            updateSlots();
            return;
        }
        mOreAccumulator += ORES_PER_UNIT * GRADE_COEF[mActiveGrade] * OUTPUT_COEF[mActiveGrade];
        int out = (int) Math.floor(mOreAccumulator);
        for (int i = 0; i < out; i++) {
            GTUtility.ItemId oreId = extractNextOre();
            if (oreId == null) break;
            ItemStack oreStack = oreId.getItemStack();
            if (oreStack == null) break;
            if (!outputOre(oreStack)) break;
            mLastOreName = oreStack.getDisplayName();
            mOreAccumulator -= 1.0d;
        }
        updateSlots();
    }

    private GTUtility.ItemId extractNextOre() {
        double total = 0.0d;
        for (PoolDim pd : mPool) {
            for (Map.Entry<GTUtility.ItemId, Float> entry : pd.dropMap.getInternalMap()
                .entrySet()) {
                if (mFilteredOres.contains(entry.getKey())
                    || (mDirectionalMode && !mDirectionalOres.contains(entry.getKey()))) continue;
                total += entry.getValue();
            }
        }
        if (total <= 0.0d) return null;
        double random = ThreadLocalRandom.current()
            .nextDouble() * total;
        for (PoolDim pd : mPool) {
            for (Map.Entry<GTUtility.ItemId, Float> entry : pd.dropMap.getInternalMap()
                .entrySet()) {
                if (mFilteredOres.contains(entry.getKey())
                    || (mDirectionalMode && !mDirectionalOres.contains(entry.getKey()))) continue;
                random -= entry.getValue();
                if (random < 0.0d) return entry.getKey();
            }
        }
        return null;
    }

    private boolean outputOre(ItemStack rawOre) {
        ItemStack out;
        if (mOreMode == 0) {
            out = rawOre;
        } else {
            MinedDrop mined = mineCrudeDrops(rawOre);
            if (mined == null) {
                out = rawOre;
            } else if (mOreMode == 1) {
                out = GTUtility.copyAmount(mined.count, mined.template);
            } else if (OreCrushedUtil.isProcessedForm(mined.template)) {
                out = GTUtility.copyAmount(mined.count, mined.template);
            } else {
                ItemStack product = OreCrushedUtil.getCrushedProduct(mined.template);
                int perCrude = 2;
                if (product == null) {
                    Materials material = getOreMaterial(mined.template);
                    product = material == null ? null : GTOreDictUnificator.get(OrePrefixes.crushed, material, 1);
                } else {
                    perCrude = product.stackSize;
                }
                if (product == null) {
                    out = rawOre;
                } else {
                    product.stackSize = (int) Math.round(mined.count * perCrude * 1.5d);
                    out = product;
                }
            }
        }
        if (!getVoidingMode().protectItem) {
            addOutputPartial(out);
            return true;
        }
        return tryOutputOre(out);
    }

    private boolean tryOutputOre(ItemStack ore) {
        if (GTUtility.isStackInvalid(ore)) return false;
        for (MTEHatchOutputBus bus : GTUtility.validMTEList(mOutputBusses)) {
            ItemStack probe = GTUtility.copyOrNull(ore);
            if (storePartial(bus, probe, true)) {
                storePartial(bus, ore, false);
                return true;
            }
        }
        return false;
    }

    private static boolean storePartial(MTEHatchOutputBus bus, ItemStack stack, boolean simulate) {
        if (bus instanceof MTEHatchOutputBusME meBus && simulate) {
            boolean ok = meBus.storePartial(stack, true);
            if (!ok && meBus.isFilteredToItem(GTUtility.ItemId.createNoCopy(stack))) {
                stack.stackSize = 0;
                return true;
            }
            if (ok && stack.stackSize > 0) {
                stack.stackSize = 0;
            }
            return ok;
        }
        return bus.storePartial(stack, simulate);
    }

    private MinedDrop mineCrudeDrops(ItemStack rawOre) {
        try {
            Item item = rawOre.getItem();
            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).field_150939_a;
                int meta = rawOre.getItemDamage();
                try (OreInfo<?> info = OreManager.getOreInfo(block, meta)) {
                    if (info != null) {
                        boolean originalNatural = info.isNatural;
                        info.isNatural = true;
                        List<ItemStack> drops;
                        try {
                            drops = OreManager.getAdapter(info)
                                .getOreDrops(ThreadLocalRandom.current(), info, false, 0);
                        } finally {
                            info.isNatural = originalNatural;
                        }
                        if (drops == null || drops.isEmpty()) return null;
                        int extra = Math.max(
                            0,
                            ThreadLocalRandom.current()
                                .nextInt(mFortuneLevel + 2) - 1);
                        return new MinedDrop(drops.get(0), drops.size() + extra);
                    }
                    GTMockWorld mockWorld = new GTMockWorld();
                    mockWorld.clear();
                    mockWorld.setBlock(0, 0, 0, block, meta, 0);
                    List<ItemStack> drops = block.getDrops(mockWorld, 0, 0, 0, meta, mFortuneLevel);
                    if (drops == null || drops.isEmpty()) return null;
                    return new MinedDrop(drops.get(0), drops.get(0).stackSize);
                }
            }
        } catch (Throwable t) {
            GTNotGood.LOG.warn("Failed to resolve mined ore drops for Large Void Miner", t);
        }
        return null;
    }

    private Materials getOreMaterial(ItemStack stack) {
        ItemData data = GTOreDictUnificator.getItemData(stack);
        if (data == null || data.mMaterial == null || data.mMaterial.mMaterial == null) return null;
        return data.mMaterial.mMaterial;
    }

    private boolean depleteUUMatterForTick() {
        mUuAccumulator += getUURatePerSecond() / 20.0d;
        int toDrain = (int) Math.floor(mUuAccumulator);
        if (toDrain <= 0) return true;
        FluidStack request = getUUMatterRequest(1);
        if (request == null) return false;
        int remaining = toDrain;
        for (MTEHatch hatch : GTUtility.validMTEList(mInputHatches)) {
            if (remaining <= 0) break;
            int available = 0;
            FluidTankInfo[] tanks = hatch.getTankInfo(ForgeDirection.UNKNOWN);
            if (tanks != null) {
                for (FluidTankInfo tank : tanks) {
                    if (tank != null && tank.fluid != null && tank.fluid.isFluidEqual(request)) {
                        available += tank.fluid.amount;
                    }
                }
            }
            if (available <= 0) continue;
            int toTake = Math.min(remaining, available);
            FluidStack drainReq = request.copy();
            drainReq.amount = toTake;
            FluidStack drained = hatch.drain(ForgeDirection.UNKNOWN, drainReq, true);
            remaining -= drained == null ? 0 : drained.amount;
        }
        mUuAccumulator -= toDrain - remaining;
        return remaining <= 0;
    }

    private static FluidStack getUUMatterRequest(int amount) {
        FluidStack request = FluidRegistry.getFluidStack("ic2uumatter", amount);
        return request != null ? request : FluidRegistry.getFluidStack("uumatter", amount);
    }

    private int getUUMatterTotal() {
        FluidStack request = getUUMatterRequest(1);
        if (request == null) return 0;
        int total = 0;
        for (MTEHatch hatch : GTUtility.validMTEList(mInputHatches)) {
            FluidTankInfo[] tanks = hatch.getTankInfo(ForgeDirection.UNKNOWN);
            if (tanks == null) continue;
            for (FluidTankInfo tank : tanks) {
                if (tank != null && tank.fluid != null && tank.fluid.amount > 0 && tank.fluid.isFluidEqual(request)) {
                    total += tank.fluid.amount;
                }
            }
        }
        return total;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && mDirectionalMode
            && aBaseMetaTileEntity.isAllowedToWork()
            && mMaxProgresstime > 0
            && !depleteUUMatterForTick()) {
            mMaxProgresstime = 0;
            mProgresstime = 0;
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setDouble("oreAccumulator", mOreAccumulator);
        aNBT.setString("lastDimAbbr", lastDimAbbr);
        aNBT.setString("mLastOreName", mLastOreName);
        aNBT.setBoolean("dropMapValid", dropMapValid);
        aNBT.setInteger("mCurrentDimId", mCurrentDimId);
        aNBT.setInteger("mOreMode", mOreMode);
        aNBT.setInteger("mFortuneLevel", mFortuneLevel);
        aNBT.setInteger("mActiveGrade", mActiveGrade);
        NBTTagList pluginSlots = new NBTTagList();
        for (ItemStack stack : mPluginSlots) {
            NBTTagCompound slotTag = new NBTTagCompound();
            if (stack != null) stack.writeToNBT(slotTag);
            pluginSlots.appendTag(slotTag);
        }
        aNBT.setTag("mPluginSlots", pluginSlots);
        aNBT.setTag("mFilteredOres", writeOreIdList(mFilteredOres));
        aNBT.setBoolean("mDirectionalMode", mDirectionalMode);
        aNBT.setTag("mDirectionalOres", writeOreIdList(mDirectionalOres));
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        mOreAccumulator = aNBT.getDouble("oreAccumulator");
        lastDimAbbr = aNBT.getString("lastDimAbbr");
        mLastOreName = aNBT.getString("mLastOreName");
        dropMapValid = aNBT.getBoolean("dropMapValid");
        mCurrentDimId = aNBT.getInteger("mCurrentDimId");
        mOreMode = aNBT.getInteger("mOreMode");
        mFortuneLevel = aNBT.getInteger("mFortuneLevel");
        mActiveGrade = aNBT.getInteger("mActiveGrade");
        if (mOreMode < 0 || mOreMode > 2) mOreMode = 0;
        if (mFortuneLevel < 3 || mFortuneLevel > 15 || mFortuneLevel % 2 == 0) mFortuneLevel = 3;
        NBTTagList pluginSlots = aNBT.getTagList("mPluginSlots", 10);
        for (int i = 0; i < pluginSlots.tagCount() && i < mPluginSlots.length; i++) {
            mPluginSlots[i] = ItemStack.loadItemStackFromNBT(pluginSlots.getCompoundTagAt(i));
        }
        migrateLegacyControllerDimensionSlot();
        readOreIdList(aNBT.getTagList("mFilteredOres", 10), mFilteredOres);
        mDirectionalMode = aNBT.getBoolean("mDirectionalMode");
        readOreIdList(aNBT.getTagList("mDirectionalOres", 10), mDirectionalOres);
        rebuildPool();
    }

    /**
     * Moves dimension display items saved by the early config GUI out of the controller inventory.
     * <p>
     * The config terminal now owns all 25 dimension slots in {@link #mPluginSlots}; this keeps existing worlds from
     * continuing to show the first dimension item in the normal controller GUI slot.
     */
    private void migrateLegacyControllerDimensionSlot() {
        if (mInventory == null || mInventory.length <= 1 || !isDimensionDisplayItem(mInventory[1])) return;
        if (mPluginSlots[0] == null) mPluginSlots[0] = mInventory[1];
        mInventory[1] = null;
    }

    private static NBTTagList writeOreIdList(Set<GTUtility.ItemId> ores) {
        NBTTagList list = new NBTTagList();
        for (GTUtility.ItemId id : ores) {
            if (id == null) continue;
            ItemStack stack = id.getItemStack(1);
            if (stack != null) list.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        return list;
    }

    private static void readOreIdList(NBTTagList list, Set<GTUtility.ItemId> target) {
        target.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
            if (stack != null) {
                GTUtility.ItemId id = GTUtility.ItemId.create(stack);
                if (id != null) target.add(id);
                continue;
            }
            Item item = findItemByName(tag.getString("item"));
            if (item != null) target.add(GTUtility.ItemId.createNoCopy(item, tag.getShort("meta"), null));
        }
    }

    private static Item findItemByName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return null;
        String[] parts = itemName.split(":", 2);
        if (parts.length != 2) return null;
        return GameRegistry.findItem(parts[0], parts[1]);
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (getBaseMetaTileEntity() == null || getBaseMetaTileEntity().isClientSide()) return;
        toggleDirectionalMode(aPlayer);
    }

    public void openConfigGui(EntityPlayer player) {
        LargeVoidMinerConfigGuiFactory.open(player, this);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr recipe.gtnotgood.largeVoidMiner
        // # Large Void Miner
        // # zh_CN 大型虚空矿机
        String machineType = StatCollector.translateToLocal("recipe.gtnotgood.largeVoidMiner");
        tt.addMachineType(machineType)
            // #tr tooltip.gtnotgood.largeVoidMiner.0
            // # Mines GalacticGreg void ores from the selected dimension.
            // # zh_CN 从选定维度的维度虚空矿池采矿。
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.0"))
            // #tr tooltip.gtnotgood.largeVoidMiner.1
            // # Uses EU and normal GT energy hatches; no steam hatches are required.
            // # zh_CN 使用 EU 和普通 GT 能源仓，不需要蒸汽仓。
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.1"))
            // #tr tooltip.gtnotgood.largeVoidMiner.2
            // # The config terminal supports dimensions, filtering, directional mining, fortune, and ore modes.
            // # zh_CN 配置终端支持维度、过滤、定向、时运和矿石模式。
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.2"))
            // #tr tooltip.gtnotgood.largeVoidMiner.3
            // # Directional mode consumes UU-Matter from input hatches.
            // # zh_CN 定向模式会消耗 UU 物质。
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.3"))
            .beginStructureBlock(7, 9, 7, false)
            // #tr tooltip.gtnotgood.largeVoidMiner.controller
            // # Front center, second layer
            // # zh_CN 正面中心，第二层
            .addController(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.controller"))
            // #tr tooltip.gtnotgood.largeVoidMiner.casing
            // # Steel void miner casing
            // # zh_CN 脱氧钢机器外壳
            .addInputBus(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.casing"), 1)
            .addInputHatch(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.casing"), 1)
            .addOutputBus(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.casing"), 1)
            .addEnergyHatch(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.casing"), 1)
            .addMaintenanceHatch(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidMiner.casing"), 1)
            .toolTipFinisher();
        return tt;
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeVoidMinerGui(this);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        int casingId = getSteelCasingTextureId();
        if (side == aFacing) {
            return new ITexture[] { getCasingTextureForId(casingId), TextureFactory.builder()
                .addIcon(aActive ? OVERLAY_FRONT_ORE_DRILL_ACTIVE : OVERLAY_FRONT_ORE_DRILL)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(aActive ? OVERLAY_FRONT_ORE_DRILL_ACTIVE_GLOW : OVERLAY_FRONT_ORE_DRILL_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { getCasingTextureForId(casingId) };
    }

    private static int getSteelCasingTextureId() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> info = new ArrayList<>();
        // #tr gui.gtnotgood.largeVoidMiner.type
        // # Large Void Miner
        // # zh_CN 大型虚空矿机
        info.add(EnumChatFormatting.BLUE + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.type"));
        if (!mMachine) {
            // #tr gui.gtnotgood.largeVoidMiner.incomplete
            // # Structure incomplete
            // # zh_CN 结构未成型
            info.add(
                EnumChatFormatting.RED + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.incomplete"));
            return info.toArray(new String[0]);
        }
        // #tr gui.gtnotgood.largeVoidMiner.dimension
        // # Dimension:
        // # zh_CN 维度：
        info.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.dimension")
                + EnumChatFormatting.GREEN
                + getDimensionDisplayName());
        // #tr gui.gtnotgood.largeVoidMiner.energy_cost
        // # Energy:
        // # zh_CN 能耗：
        info.add(
            EnumChatFormatting.YELLOW + StatCollector.translateToLocal("gui.gtnotgood.largeVoidMiner.energy_cost")
                + EnumChatFormatting.WHITE
                + NumberFormatUtil.formatNumber(getEnergyCostPerTick())
                + " EU/t");
        return info.toArray(new String[0]);
    }

    private void markBaseDirty() {
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    private static class PoolDim {

        final String dimAbbr;
        final String dimName;
        final VoidMinerUtility.DropMap dropMap;
        final VoidMinerUtility.DropMap extraDropMap;

        PoolDim(String dimAbbr, String dimName, VoidMinerUtility.DropMap dropMap,
            VoidMinerUtility.DropMap extraDropMap) {
            this.dimAbbr = dimAbbr;
            this.dimName = dimName;
            this.dropMap = dropMap;
            this.extraDropMap = extraDropMap;
        }
    }

    private static class MinedDrop {

        final ItemStack template;
        final int count;

        MinedDrop(ItemStack template, int count) {
            this.template = template;
            this.count = count;
        }
    }

    private class PluginSlotInventory implements IInventory {

        @Override
        public int getSizeInventory() {
            return mPluginSlots.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= mPluginSlots.length) return null;
            return mPluginSlots[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            ItemStack stack = getStackInSlot(slot);
            if (stack == null) return null;
            ItemStack result;
            if (stack.stackSize <= amount) {
                result = stack;
                mPluginSlots[slot] = null;
            } else {
                result = stack.splitStack(amount);
            }
            onPluginSlotChanged();
            return result;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return getStackInSlot(slot);
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot < 0 || slot >= mPluginSlots.length) return;
            if (stack != null && !isDimensionDisplayItem(stack)) return;
            mPluginSlots[slot] = stack;
            onPluginSlotChanged();
        }

        @Override
        public String getInventoryName() {
            return "gtnotgood.large_void_miner.pluginSlots";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return true;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            onPluginSlotChanged();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return isDimensionDisplayItem(stack);
        }

        private void onPluginSlotChanged() {
            markPoolDirty();
            LargeVoidMiner.this.markBaseDirty();
        }
    }
}
