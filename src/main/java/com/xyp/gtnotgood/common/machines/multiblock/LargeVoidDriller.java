package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.GregTechAPI.sBlockCasings2;
import static gregtech.api.GregTechAPI.sBlockFrames;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_DRILL;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_DRILL_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_DRILL_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_DRILL_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.getCasingTextureForId;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.api.gui.FluidEntryInfo;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidDrillerConfigGuiFactory;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidDrillerGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.machine.VoidDrillerFluidPool;
import com.xyp.gtnotgood.utils.machine.VoidDrillerFluidPool.Entry;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IHatchElement;
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
import gregtech.common.misc.WirelessNetworkManager;

public class LargeVoidDriller extends GTNGMultiBlockBase<LargeVoidDriller> implements ISurvivalConstructable {

    public static final int CYCLE_TICKS = 400;
    private static final int MAX_OVERCLOCK_LEVEL = 4;
    private static final String[] GAS_DIMENSIONS = { "Jupiter", "Saturn", "Uranus", "Neptune" };
    private static final String MAIN = "main";
    private static final String[][] SHAPE = {
        { "       ", "       ", "       ", "   B   ", "       ", "       ", "       " },
        { "       ", "       ", "       ", "   B   ", "       ", "       ", "       " },
        { "       ", "       ", "       ", "   B   ", "       ", "       ", "       " },
        { "       ", "       ", "   B   ", "  BCB  ", "   B   ", "       ", "       " },
        { "       ", "       ", "   B   ", "  BCB  ", "   B   ", "       ", "       " },
        { "       ", "       ", "   B   ", "  BCB  ", "   B   ", "       ", "       " },
        { "       ", " B   B ", "  DAD  ", "  ACA  ", "  DAD  ", " B   B ", "       " },
        { "  D D  ", " BA~AB ", " A   A ", " B C B ", " A   A ", " BABAB ", "       " },
        { "  E E  ", " BBBBB ", "EB   BE", " B C B ", "EB   BE", " BBBBB ", "  E E  " } };
    private static IStructureDefinition<LargeVoidDriller> structureDefinition;

    private int casingCount;
    private int overclockLevel;
    private int gasDimensionMask;
    private int currentDimension = Integer.MIN_VALUE;
    private boolean poolDirty = true;
    private boolean directional;
    private UUID wirelessOwner;
    private List<Entry> pool = new ArrayList<>();
    private final Set<String> filteredFluids = new HashSet<>();
    private final Set<String> targetedFluids = new HashSet<>();
    private FluidStack activeOutput;
    private FluidStack pendingOutput;
    private final InventoryBasic dimensionSlots = new InventoryBasic(
        ModList.GTNotGood.getID() + ".void_driller.dimensions", false, 25) {

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return isDimensionDisplayItem(stack);
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (stack == null || isDimensionDisplayItem(stack)) super.setInventorySlotContents(slot, stack);
        }

        @Override
        public void markDirty() {
            super.markDirty();
            poolDirty = true;
            clearIdleSelection();
            markBaseDirty();
        }
    };

    public LargeVoidDriller(int id, String name, String localizedName) {
        super(id, name, localizedName);
    }

    public LargeVoidDriller(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LargeVoidDriller(mName);
    }

    @Override
    public IStructureDefinition<LargeVoidDriller> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = StructureDefinition.<LargeVoidDriller>builder().addShape(MAIN, transpose(SHAPE))
                .addElement('A', ofBlock(sBlockCasings2, 13)).addElement('B', steelCasing(1))
                .addElement('C', onElementPass(t -> ++t.casingCount, ofBlock(sBlockCasings2, 0)))
                .addElement('D', ofBlock(sBlockFrames, Materials.Steel.mMetaItemSubID)).addElement('E', steelCasing(2))
                .build();
        }
        return structureDefinition;
    }

    private static IStructureElement<LargeVoidDriller> steelCasing(int hint) {
        Map<IHatchElement<? super LargeVoidDriller>, Integer> hatches = new LinkedHashMap<>();
        hatches.put(Energy.or(ExoticEnergy), 0);
        hatches.put(InputHatch, 0);
        hatches.put(OutputHatch, 1);
        hatches.put(Maintenance, 0);
        return ofChain(
            buildHatchAdder(LargeVoidDriller.class).atLeast(hatches).casingIndex(casingTexture()).hint(hint).build(),
            onElementPass(t -> ++t.casingCount, ofBlock(sBlockCasings2, 0)));
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece(MAIN, stack, hintsOnly, 3, 7, 1);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment environment) {
        if (mMachine) return -1;
        return survivalBuildPiece(MAIN, stack, 3, 7, 1, budget, environment, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        casingCount = 0;
        if (!checkPiece(MAIN, 3, 7, 1, errors)) return;
        checkCasingMin(errors, casingCount, 3);
        if (mOutputHatches.isEmpty()) errors.add(StructureErrors.hatchCount(ErrorType.TOO_FEW, OutputHatch, 0, 1));
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
    public boolean drainEnergyInput(long eu) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (eu < 0 || tile == null || !tile.isServerSide() || tile.getOwnerUuid() == null) return false;
        if (!tile.getOwnerUuid().equals(wirelessOwner))
            wirelessOwner = WirelessNetworkManager.processInitialSettings(tile);
        return eu == 0 || WirelessNetworkManager.addEUToGlobalEnergyMap(wirelessOwner, -eu);
    }

    public static boolean isDimensionDisplayItem(ItemStack stack) {
        return VoidDrillerFluidPool.isDimensionDisplayItem(stack);
    }

    public IInventory getPluginSlotInventory() {
        return dimensionSlots;
    }

    private List<String> selectedDimensions() {
        Set<String> dimensions = new LinkedHashSet<>();
        for (int slot = 0; slot < dimensionSlots.getSizeInventory(); slot++) {
            String dimension = VoidDrillerFluidPool.readDimensionAbbr(dimensionSlots.getStackInSlot(slot));
            if (dimension != null && !dimension.isEmpty()) dimensions.add(dimension);
        }
        for (int index = 0; index < GAS_DIMENSIONS.length; index++) {
            if ((gasDimensionMask & (1 << index)) != 0) dimensions.add(GAS_DIMENSIONS[index]);
        }
        return new ArrayList<>(dimensions);
    }

    public int getGasDimensionMask() {
        return gasDimensionMask;
    }

    public void toggleGasDimension(int index) {
        if (index < 0 || index >= GAS_DIMENSIONS.length) return;
        gasDimensionMask ^= 1 << index;
        forceRefreshPool();
    }

    private void updatePool() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() == null) return;
        int dimension = tile.getWorld().provider.dimensionId;
        if (!poolDirty && currentDimension == dimension) return;
        currentDimension = dimension;
        pool = VoidDrillerFluidPool.getEntries(dimension, selectedDimensions());
        poolDirty = false;
    }

    public void forceRefreshPool() {
        poolDirty = true;
        clearIdleSelection();
        updatePool();
        markBaseDirty();
    }

    public boolean isPoolValid() {
        updatePool();
        return !pool.isEmpty();
    }

    public String getDimensionDisplayName() {
        updatePool();
        List<String> dimensions = selectedDimensions();
        return dimensions.isEmpty() ? VoidDrillerFluidPool.getDimensionLabel(currentDimension)
            : String.join("+", dimensions);
    }

    public List<FluidEntryInfo> getFluidEntries() {
        updatePool();
        Map<String, List<Entry>> merged = new LinkedHashMap<>();
        for (Entry entry : pool) merged.computeIfAbsent(entry.fluidName, name -> new ArrayList<>()).add(entry);
        List<FluidEntryInfo> entries = new ArrayList<>();
        for (Map.Entry<String, List<Entry>> group : merged.entrySet()) {
            double weight = 0;
            int min = Integer.MAX_VALUE;
            int max = 0;
            Set<String> dimensions = new LinkedHashSet<>();
            for (Entry entry : group.getValue()) {
                weight += entry.weight;
                min = Math.min(min, entry.minAmount);
                max = Math.max(max, entry.maxAmount);
                dimensions.add(entry.dimension);
            }
            FluidStack fluid = FluidRegistry.getFluidStack(group.getKey(), 1);
            if (fluid != null) entries.add(new FluidEntryInfo(fluid, weight, new ArrayList<>(dimensions), min, max,
                filteredFluids.contains(group.getKey()), targetedFluids.contains(group.getKey())));
        }
        return entries;
    }

    public boolean getDirectionalMode() {
        return directional;
    }

    public void toggleDirectionalMode() {
        directional = !directional;
        clearIdleSelection();
        markBaseDirty();
    }

    public void toggleFluidSelection(String fluidName) {
        updatePool();
        if (pool.stream().noneMatch(entry -> entry.fluidName.equals(fluidName))) return;
        Set<String> selection = directional ? targetedFluids : filteredFluids;
        if (!selection.remove(fluidName)) selection.add(fluidName);
        clearIdleSelection();
        markBaseDirty();
    }

    public void clearCurrentModeConfig() {
        (directional ? targetedFluids : filteredFluids).clear();
        clearIdleSelection();
        markBaseDirty();
    }

    private boolean eligible(Entry entry) {
        return directional ? targetedFluids.contains(entry.fluidName) : !filteredFluids.contains(entry.fluidName);
    }

    private void clearIdleSelection() {
        if (mMaxProgresstime <= 0) activeOutput = null;
    }

    private double eligibleWeight() {
        return pool.stream().filter(this::eligible).mapToDouble(entry -> entry.weight).sum();
    }

    public double getWeightIncreasePercent() {
        updatePool();
        double all = pool.stream().mapToDouble(entry -> entry.weight).sum();
        double allowed = eligibleWeight();
        return allowed <= 0 ? 0 : Math.max(0, all / allowed - 1) * 100;
    }

    public double getDimensionIncreasePercent() {
        return (directional ? 200 : 0) + 20 * Math.max(0, selectedDimensions().size() - 1);
    }

    public double getEnergyMultiplier() {
        return (1 << (overclockLevel * 2)) * (1 + getDimensionIncreasePercent() / 100)
            * (1 + getWeightIncreasePercent() / 100);
    }

    public long getEnergyCostPerTick() {
        return Math.max(1, Math.round(480 * getEnergyMultiplier()));
    }

    private int getUUMatterPerCycle() {
        return directional
            ? (int) Math.min(Integer.MAX_VALUE,
                Math.ceil((1 + getWeightIncreasePercent() / 100) * getCycleDurationTicks() / 20.0))
            : 0;
    }

    public double getUURatePerSecond() {
        return getUUMatterPerCycle() * 20.0 / getCycleDurationTicks();
    }

    public int getOverclockLevel() {
        return overclockLevel;
    }

    public int getCycleDurationTicks() {
        return Math.max(1, CYCLE_TICKS / (1 << (overclockLevel * 2)));
    }

    public void cycleOverclockLevel() {
        overclockLevel = (overclockLevel + 1) % (MAX_OVERCLOCK_LEVEL + 1);
        markBaseDirty();
    }

    @Override
    @NotNull
    public CheckRecipeResult checkProcessing() {
        if (!flushPendingOutput()) return CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
        updatePool();
        if (pool.isEmpty()) {
            // #tr gui.gtnotgood.largeVoidDriller.no_fluids
            // # No fluids in selected dimensions
            // # zh_CN 选定维度没有可钻取的流体
            return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidDriller.no_fluids");
        }
        double weight = eligibleWeight();
        if (weight <= 0) {
            // #tr gui.gtnotgood.largeVoidDriller.no_targets
            // # No eligible fluids; check filters or directional targets
            // # zh_CN 没有可用流体，请检查过滤或定向选择
            return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidDriller.no_targets");
        }
        if (activeOutput == null) {
            double roll = ThreadLocalRandom.current().nextDouble(weight);
            for (Entry entry : pool) {
                if (!eligible(entry)) continue;
                roll -= entry.weight;
                if (roll < 0) {
                    int amount = (int) ThreadLocalRandom.current().nextLong(entry.minAmount,
                        (long) entry.maxAmount + 1);
                    activeOutput = FluidRegistry.getFluidStack(entry.fluidName, amount);
                    break;
                }
            }
        }
        if (activeOutput == null) return CheckRecipeResultRegistry.NO_RECIPE;
        if (activeOutput.amount > 0 && !canOutputAll(new FluidStack[] { activeOutput }))
            return CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
        int uuAmount = getUUMatterPerCycle();
        if (uuAmount > 0) {
            FluidStack uu = FluidRegistry.getFluidStack("ic2uumatter", uuAmount);
            if (uu == null) uu = FluidRegistry.getFluidStack("uumatter", uuAmount);
            if (uu == null || !depleteInput(uu, true)) {
                // #tr gui.gtnotgood.largeVoidDriller.no_uu
                // # Insufficient UU-Matter for a drilling cycle
                // # zh_CN UU 物质不足以完成一个钻取周期
                return SimpleCheckRecipeResult.ofFailure("gui.gtnotgood.largeVoidDriller.no_uu");
            }
            depleteInput(uu);
        }
        lEUt = -getEnergyCostPerTick();
        mMaxProgresstime = getCycleDurationTicks();
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        updateSlots();
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected void outputAfterRecipe() {
        pendingOutput = activeOutput;
        activeOutput = null;
        flushPendingOutput();
    }

    private boolean flushPendingOutput() {
        if (pendingOutput == null) return true;
        if (pendingOutput.amount > 0 && !addOutputAtomic(pendingOutput.copy())) return false;
        pendingOutput = null;
        markBaseDirty();
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("overclockLevel", overclockLevel);
        tag.setInteger("gasDimensions", gasDimensionMask);
        tag.setBoolean("directional", directional);
        NBTTagList slots = new NBTTagList();
        for (int slot = 0; slot < dimensionSlots.getSizeInventory(); slot++) {
            ItemStack stack = dimensionSlots.getStackInSlot(slot);
            if (stack == null) continue;
            NBTTagCompound item = stack.writeToNBT(new NBTTagCompound());
            item.setInteger("Slot", slot);
            slots.appendTag(item);
        }
        tag.setTag("dimensions", slots);
        tag.setTag("filteredFluids", saveNames(filteredFluids));
        tag.setTag("targetedFluids", saveNames(targetedFluids));
        if (activeOutput != null) tag.setTag("activeFluid", activeOutput.writeToNBT(new NBTTagCompound()));
        else tag.removeTag("activeFluid");
        if (pendingOutput != null) tag.setTag("pendingFluid", pendingOutput.writeToNBT(new NBTTagCompound()));
        else tag.removeTag("pendingFluid");
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        overclockLevel = Math.max(0, Math.min(MAX_OVERCLOCK_LEVEL, tag.getInteger("overclockLevel")));
        gasDimensionMask = tag.getInteger("gasDimensions") & 15;
        directional = tag.getBoolean("directional");
        for (int slot = 0; slot < dimensionSlots.getSizeInventory(); slot++)
            dimensionSlots.setInventorySlotContents(slot, null);
        NBTTagList slots = tag.getTagList("dimensions", 10);
        for (int index = 0; index < slots.tagCount(); index++) {
            NBTTagCompound item = slots.getCompoundTagAt(index);
            int slot = item.getInteger("Slot");
            if (slot >= 0 && slot < dimensionSlots.getSizeInventory())
                dimensionSlots.setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(item));
        }
        loadNames(tag.getTagList("filteredFluids", 8), filteredFluids);
        loadNames(tag.getTagList("targetedFluids", 8), targetedFluids);
        activeOutput = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("activeFluid"));
        pendingOutput = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("pendingFluid"));
        poolDirty = true;
    }

    private static NBTTagList saveNames(Set<String> names) {
        NBTTagList tag = new NBTTagList();
        for (String name : names) tag.appendTag(new NBTTagString(name));
        return tag;
    }

    private static void loadNames(NBTTagList tag, Set<String> names) {
        names.clear();
        for (int index = 0; index < tag.tagCount(); index++) names.add(tag.getStringTagAt(index));
    }

    @Override
    public void onBlockDestroyed() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide()) {
            for (int slot = 0; slot < dimensionSlots.getSizeInventory(); slot++) {
                ItemStack stack = dimensionSlots.getStackInSlotOnClosing(slot);
                if (stack != null) tile.getWorld().spawnEntityInWorld(new EntityItem(tile.getWorld(),
                    tile.getXCoord() + 0.5, tile.getYCoord() + 0.5, tile.getZCoord() + 0.5, stack));
            }
        }
        super.onBlockDestroyed();
    }

    public void openConfigGui(EntityPlayer player) {
        LargeVoidDrillerConfigGuiFactory.open(player, this);
    }

    private void markBaseDirty() {
        if (getBaseMetaTileEntity() != null) getBaseMetaTileEntity().markDirty();
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder();
        // #tr recipe.gtnotgood.largeVoidDriller
        // # Large Void Driller
        // # zh_CN 大型虚空钻机
        tooltip.addMachineType(StatCollector.translateToLocal("recipe.gtnotgood.largeVoidDriller"))
            // #tr tooltip.gtnotgood.largeVoidDriller.sources
            // # Extracts underground fluids, planetary oceans and gas giant gases from selected dimensions
            // # zh_CN 从选定维度钻取地下流体、行星海洋及气态巨行星气体
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.sources"))
            // #tr tooltip.gtnotgood.largeVoidDriller.power
            // # Uses the owner's wireless grid; base consumption: 480 EU/t, 20 s per cycle
            // # zh_CN 使用拥有者的无线电网；基础功耗 480 EU/t，每周期 20 秒
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.power"))
            // #tr tooltip.gtnotgood.largeVoidDriller.overclock
            // # Four overclock levels: 4x EU/t per level, down to 1 tick per cycle
            // # zh_CN 支持 4 级超频，每级功耗乘 4，周期最低 1 tick
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.overclock")).addSeparator()
            // #tr tooltip.gtnotgood.largeVoidDriller.selection
            // # Select up to 25 dimensions; defaults to local when no dimension or gas giant is selected
            // # zh_CN 最多选择 25 个维度；未选择维度及气态巨行星时使用当前维度
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.selection"))
            // #tr tooltip.gtnotgood.largeVoidDriller.gas
            // # Select gas giants with the four planet buttons in the terminal
            // # zh_CN 使用终端内的四个行星按钮选择气态巨行星
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.gas"))
            // #tr tooltip.gtnotgood.largeVoidDriller.filter
            // # Filters exclude fluids; directional mode extracts only marked fluids and consumes UU-Matter
            // # zh_CN 过滤模式排除标记流体；定向模式只钻取标记流体并消耗 UU 物质
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.filter"))
            // #tr tooltip.gtnotgood.largeVoidDriller.cost
            // # More dimensions and narrower selections raise EU/t; output ranges are shown in the terminal
            // # zh_CN 维度越多、筛选越精细，功耗越高；产量范围见终端
            .addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.cost"))
            .beginStructureBlock(7, 9, 7, false)
            // #tr tooltip.gtnotgood.largeVoidDriller.controller
            // # Front center, second layer, one block behind the front edge
            // # zh_CN 正面中央，第 2 层，距最前沿向内 1 格
            .addController(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.controller"));
        // #tr tooltip.gtnotgood.largeVoidDriller.casing
        // # Solid Steel Machine Casing
        // # zh_CN 坚实钢机械方块
        String casing = StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.casing");
        return tooltip.addCasing("3+", casing, false)
            // #tr tooltip.gtnotgood.largeVoidDriller.structure
            // # 12 steel pipe casings and 6 steel frames; hatches replace outer steel casings
            // # zh_CN 12 个钢管机械方块、6 个钢框架；仓室可替换外围钢外壳
            .addStructureInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeVoidDriller.structure"))
            .addEnergyHatch("0+", casing, 1, 2)
            .addMaintenanceHatch(shouldCheckMaintenance() ? "1+" : "0+", casing, 1, 2).addInputHatch("0+", casing, 1, 2)
            .addOutputHatch("1+", casing, 1, 2).toolTipFinisher();
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LargeVoidDrillerGui(this);
    }

    private static int casingTexture() {
        return GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings2, 0);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        if (side != facing) return new ITexture[] { getCasingTextureForId(casingTexture()) };
        return new ITexture[] { getCasingTextureForId(casingTexture()),
            TextureFactory.builder().addIcon(active ? OVERLAY_FRONT_OIL_DRILL_ACTIVE : OVERLAY_FRONT_OIL_DRILL)
                .extFacing().build(),
            TextureFactory.builder()
                .addIcon(active ? OVERLAY_FRONT_OIL_DRILL_ACTIVE_GLOW : OVERLAY_FRONT_OIL_DRILL_GLOW).extFacing().glow()
                .build() };
    }
}
