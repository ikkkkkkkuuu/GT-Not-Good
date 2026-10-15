package com.xyp.gtnotgood.common.machines.basic;

import static gregtech.api.util.GTRecipeConstants.COMPRESSION_TIER;
import static gregtech.api.util.GTRecipeConstants.EXPLODE;
import static gregtech.api.util.GTRecipeConstants.ON_FIRE;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.gtnhlib.item.InventoryItemSource;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.GTMod;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachineBronze;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTItemTransfer;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;
import gregtech.common.modularui2.util.SteamTexture;

public abstract class SteamProcessingMachine extends MTEBasicMachineBronze {

    private static final int FLUID_SLOT_COUNT = 6;
    private final FluidStack[] fluidInputs = new FluidStack[FLUID_SLOT_COUNT];
    private final FluidStack[] fluidOutputs = new FluidStack[FLUID_SLOT_COUNT];
    private final FluidStack[] pendingFluidOutputs = new FluidStack[FLUID_SLOT_COUNT];
    private final FluidStackTank[] fluidInputTanks = createFluidTanks(fluidInputs);
    private final FluidStackTank[] fluidOutputTanks = createFluidTanks(fluidOutputs);
    private ForgeDirection autoOutputSide = ForgeDirection.DOWN;
    private boolean processFinished;

    protected SteamProcessingMachine(int id, String name, String localizedName, String description) {
        this(id, name, localizedName, description, 6, 6);
    }

    protected SteamProcessingMachine(int id, String name, String localizedName, String description, int inputSlots,
        int outputSlots) {
        super(id, name, localizedName, description, inputSlots, outputSlots, false);
    }

    protected SteamProcessingMachine(String name, String[] description, ITexture[][][] textures) {
        this(name, description, textures, 6, 6);
    }

    protected SteamProcessingMachine(String name, String[] description, ITexture[][][] textures, int inputSlots,
        int outputSlots) {
        super(name, description, textures, inputSlots, outputSlots, false);
    }

    protected abstract String getMachineOverlay();

    protected boolean hasActiveGlow() {
        return true;
    }

    @Override
    public String[] getDescription() {
        String[] baseDescription = super.getDescription();
        String[] description = Arrays.copyOf(baseDescription, baseDescription.length + 3);
        // #tr tooltip.steam_processing_machine.capacity
        // # Fluid inputs: %s; outputs: %s; 16,000 L each; separate steam buffer: 32,000 L
        // # zh_CN 流体输入 %s 槽，输出 %s 槽；每槽 16,000 L；独立蒸汽缓存 32,000 L
        description[baseDescription.length] = StatCollector.translateToLocalFormatted(
            "tooltip.steam_processing_machine.capacity", getFluidInputSlotCount(), getFluidOutputSlotCount());
        // #tr tooltip.steam_processing_machine.power
        // # Recipes up to 32 EU/t; double duration; 2 L steam per EU
        // # zh_CN 支持至 32 EU/t 的配方；耗时翻倍；每 EU 消耗 2 L 蒸汽
        description[baseDescription.length + 1] = StatCollector
            .translateToLocal("tooltip.steam_processing_machine.power");
        // #tr tooltip.steam_processing_machine.vent
        // # Keep the steam vent unobstructed
        // # zh_CN 蒸汽排气面须保持畅通
        description[baseDescription.length + 2] = StatCollector
            .translateToLocal("tooltip.steam_processing_machine.vent");
        return description;
    }

    @Override
    public boolean allowSelectCircuit() {
        return true;
    }

    @Override
    public int checkRecipe() {
        return checkRecipe(false);
    }

    @Override
    public int checkRecipe(boolean skipOC) {
        if (hasPendingFluidOutputs()) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        ItemStack[] inputs = getAllInputs();
        FluidStack[] fluids = Arrays.copyOf(fluidInputs, getFluidInputSlotCount());
        GTRecipe recipe = getRecipeMap().findRecipeQuery().items(inputs).fluids(fluids).specialSlot(getSpecialSlot())
            .voltage(TierEU.LV).cachedRecipe(mLastRecipe).find();
        if (recipe == null) return DID_NOT_FIND_RECIPE;
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (recipe.getMetadataOrDefault(EXPLODE, false)) {
            tile.doExplosion(TierEU.LV * 4);
            return DID_NOT_FIND_RECIPE;
        }
        if (recipe.getMetadataOrDefault(ON_FIRE, false)) {
            tile.setOnFire();
            return DID_NOT_FIND_RECIPE;
        }
        if (recipe.getMetadataOrDefault(COMPRESSION_TIER, 0) > 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        if (
            GTMod.proxy.mLowGravProcessing && (recipe.mSpecialValue == -100 || recipe.mSpecialValue == -300)
                && !isValidForLowGravity(recipe, tile.getWorld().provider.dimensionId)
        ) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        if (recipe.mCanBeBuffered) mLastRecipe = recipe;
        if (!canOutput(recipe)) {
            mOutputBlocked++;
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        ICleanroom cleanroom = cleanroomReference.getCleanroom();
        boolean needsCleanroom = recipe.mSpecialValue == -200 || recipe.mSpecialValue == -300;
        if (needsCleanroom && (cleanroom == null || !cleanroom.isValidCleanroom() || cleanroom.getCleanness() == 0)) {
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        if (!skipOC) {
            calculateCustomOverclock(recipe);
            if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1) {
                mMaxProgresstime = 0;
                mEUt = 0;
                return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
            }
        }
        if (!recipe.isRecipeInputEqual(true, fluids, inputs)) {
            mMaxProgresstime = 0;
            mEUt = 0;
            return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS;
        }
        for (int i = 0; i < mOutputItems.length; i++) {
            mOutputItems[i] = recipe.rollOutput(tile, i);
            if (needsCleanroom && mOutputItems[i] != null && tile.getRandomNumber(10000) > cleanroom.getCleanness()) {
                mOutputItems[i] = null;
            }
        }
        for (int i = 0; i < pendingFluidOutputs.length; i++) {
            pendingFluidOutputs[i] = tile.getRandomNumber(10000) < recipe.getFluidOutputChance(i)
                ? recipe.getFluidOutput(i)
                : null;
        }
        clearEmptyFluids(fluidInputs);
        markStateChanged();
        return FOUND_AND_SUCCESSFULLY_USED_RECIPE;
    }

    @Override
    protected boolean canOutput(GTRecipe recipe) {
        if (
            recipe == null || recipe.mOutputs.length > mOutputItems.length
                || recipe.mFluidOutputs.length > getFluidOutputSlotCount()
                || !canOutput(recipe.mOutputs)
        ) return false;
        if (recipe.mNeedsEmptyOutput && (!isOutputEmpty() || getDrainableStack() != null)) return false;
        FluidStack[] simulatedOutputs = Arrays.stream(fluidOutputs).map(fluid -> fluid == null ? null : fluid.copy())
            .toArray(FluidStack[]::new);
        for (FluidStack output : recipe.mFluidOutputs) {
            if (output != null && fillTanks(simulatedOutputs, getFluidOutputSlotCount(), output, true) != output.amount)
                return false;
        }
        return true;
    }

    @Override
    public boolean allowToCheckRecipe() {
        return super.allowToCheckRecipe() && (mMaxProgresstime > 0 || !hasPendingFluidOutputs());
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        processFinished = false;
        if (tile.isServerSide() && mMaxProgresstime <= 0 && hasPendingFluidOutputs()) flushPendingFluidOutputs();
        super.onPostTick(tile, tick);
        if (tile.isServerSide() && (processFinished || tick % 20 == 0)) {
            ForgeDirection side = getAutoOutputSide();
            if (mItemTransfer && !isOutputEmpty()) {
                GTItemTransfer transfer = new GTItemTransfer();
                transfer.source(new InventoryItemSource(tile, side));
                transfer.sink(tile.getTileEntityAtSide(side), side.getOpposite());
                transfer.setStacksToTransfer(mOutputItems.length);
                if (transfer.transfer() > 0) markStateChanged();
            }
            if (mFluidTransfer) outputFluids(tile, side);
        }
    }

    @Override
    public void endProcess() {
        flushPendingFluidOutputs();
        super.endProcess();
        processFinished = true;
    }

    @Override
    public boolean doesAutoOutput() {
        return false;
    }

    @Override
    public boolean doesAutoOutputFluids() {
        return false;
    }

    private void outputFluids(IGregTechTileEntity tile, ForgeDirection side) {
        if (getDrainableStack() == null) return;
        IFluidHandler target = tile.getITankContainerAtSide(side);
        if (target == null) return;
        int remaining = Config.recipeSpeedFullFluidOutput ? Integer.MAX_VALUE : 1000;
        for (int i = 0; i < getFluidOutputSlotCount(); i++) {
            FluidStack output = fluidOutputs[i];
            if (output == null || remaining <= 0) continue;
            FluidStack requested = output.copy();
            requested.amount = Math.min(output.amount, remaining);
            FluidStack offered = tile.drain(side, requested, false);
            if (offered == null) continue;
            int accepted = Math.max(0, Math.min(offered.amount, target.fill(side.getOpposite(), offered.copy(), true)));
            if (accepted <= 0) continue;
            FluidStack drained = drainMatchingFluid(offered, accepted, true);
            if (drained != null) remaining -= drained.amount;
        }
    }

    public ForgeDirection getAutoOutputSide() {
        if (isSafeOutputSide(autoOutputSide)) return autoOutputSide;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (isSafeOutputSide(side)) return side;
        }
        return ForgeDirection.UNKNOWN;
    }

    public void setAutoOutputSide(ForgeDirection side) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) return;
        if (!isSafeOutputSide(side)) return;
        autoOutputSide = side;
        markStateChanged();
    }

    public void cycleAutoOutputSide() {
        int currentSide = getAutoOutputSide().ordinal();
        for (int i = 1; i <= ForgeDirection.VALID_DIRECTIONS.length; i++) {
            ForgeDirection side = ForgeDirection.VALID_DIRECTIONS[(currentSide + i) % 6];
            if (isSafeOutputSide(side)) {
                setAutoOutputSide(side);
                return;
            }
        }
    }

    private boolean isSafeOutputSide(ForgeDirection side) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        return side != null && side != ForgeDirection.UNKNOWN
            && side != mMainFacing
            && (tile == null || side != tile.getFrontFacing());
    }

    private boolean hasPendingFluidOutputs() {
        return firstFluid(pendingFluidOutputs) != null;
    }

    private void flushPendingFluidOutputs() {
        boolean changed = false;
        for (int i = 0; i < pendingFluidOutputs.length; i++) {
            FluidStack pending = pendingFluidOutputs[i];
            if (pending == null) continue;
            int filled = fillTanks(fluidOutputs, getFluidOutputSlotCount(), pending, true);
            if (filled <= 0) continue;
            pending.amount -= filled;
            if (pending.amount <= 0) pendingFluidOutputs[i] = null;
            changed = true;
        }
        if (changed) markStateChanged();
    }

    @Override
    public int getCapacity() {
        return 16000;
    }

    public FluidStackTank[] getFluidInputTanks() {
        return Arrays.copyOf(fluidInputTanks, getFluidInputSlotCount());
    }

    public FluidStackTank[] getFluidOutputTanks() {
        return Arrays.copyOf(fluidOutputTanks, getFluidOutputSlotCount());
    }

    protected int getFluidInputSlotCount() {
        return Math.min(FLUID_SLOT_COUNT, getRecipeMap().getFrontend().getUIProperties().maxFluidInputs);
    }

    protected int getFluidOutputSlotCount() {
        return Math.min(FLUID_SLOT_COUNT, getRecipeMap().getFrontend().getUIProperties().maxFluidOutputs);
    }

    @Override
    public FluidStackTank getFluidTank() {
        return fluidInputTanks[0];
    }

    @Override
    public FluidStackTank getFluidOutputTank() {
        return fluidOutputTanks[0];
    }

    private FluidStackTank[] createFluidTanks(FluidStack[] fluids) {
        FluidStackTank[] tanks = new FluidStackTank[fluids.length];
        for (int i = 0; i < tanks.length; i++) {
            int slot = i;
            tanks[i] = new FluidStackTank(() -> fluids[slot], fluid -> {
                fluids[slot] = fluid;
                markStateChanged();
            }, this::getCapacity);
        }
        return tanks;
    }

    private void markStateChanged() {
        markDirty();
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null) tile.markInventoryBeenModified();
    }

    @Override
    public FluidStack getFillableStack() {
        return fluidInputs[0];
    }

    @Override
    public FluidStack setFillableStack(FluidStack fluid) {
        fluidInputs[0] = fluid;
        markStateChanged();
        return fluid;
    }

    @Override
    public FluidStack getDrainableStack() {
        for (int i = 0; i < getFluidOutputSlotCount(); i++) {
            if (fluidOutputs[i] != null && fluidOutputs[i].amount > 0) return fluidOutputs[i];
        }
        return null;
    }

    @Override
    public FluidStack setDrainableStack(FluidStack fluid) {
        fluidOutputs[0] = fluid;
        markStateChanged();
        return fluid;
    }

    private static FluidStack firstFluid(FluidStack[] fluids) {
        for (FluidStack fluid : fluids) {
            if (fluid != null && fluid.amount > 0) return fluid;
        }
        return null;
    }

    private static void clearEmptyFluids(FluidStack[] fluids) {
        for (int i = 0; i < fluids.length; i++) {
            if (fluids[i] != null && fluids[i].amount <= 0) fluids[i] = null;
        }
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack fluid) {
        return fluid != null && getRecipeMap().containsInput(fluid);
    }

    @Override
    public int fill(FluidStack fluid, boolean doFill) {
        if (fluid == null || fluid.amount <= 0 || !isFluidInputAllowed(fluid)) return 0;
        int filled = fillTanks(fluidInputs, getFluidInputSlotCount(), fluid, doFill);
        if (doFill && filled > 0) markStateChanged();
        return filled;
    }

    private int fillTanks(FluidStack[] tanks, int tankCount, FluidStack fluid, boolean doFill) {
        int remaining = fluid.amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < tankCount && remaining > 0; i++) {
                FluidStack stored = tanks[i];
                boolean empty = stored == null || stored.amount <= 0;
                if (pass == 0 ? empty || !stored.isFluidEqual(fluid) : !empty) continue;
                int filled = Math.min(remaining, Math.max(0, getCapacity() - (empty ? 0 : stored.amount)));
                if (filled <= 0) continue;
                if (doFill) {
                    if (empty) {
                        tanks[i] = fluid.copy();
                        tanks[i].amount = filled;
                    } else stored.amount += filled;
                }
                remaining -= filled;
            }
        }
        return fluid.amount - remaining;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack fluid = getDrainableStack();
        return drainMatchingFluid(fluid, maxDrain, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection side, FluidStack fluid, boolean doDrain) {
        return drain(side, fluid, fluid == null ? 0 : fluid.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection side, FluidStack fluid, int amount, boolean doDrain) {
        return drainMatchingFluid(fluid, amount, doDrain);
    }

    private FluidStack drainMatchingFluid(FluidStack requested, int amount, boolean doDrain) {
        if (requested == null || amount <= 0) return null;
        int remaining = amount;
        for (int i = 0; i < getFluidOutputSlotCount() && remaining > 0; i++) {
            FluidStack fluid = fluidOutputs[i];
            if (fluid == null || !fluid.isFluidEqual(requested)) continue;
            int drained = Math.min(remaining, fluid.amount);
            if (doDrain) {
                fluid.amount -= drained;
                if (fluid.amount <= 0) fluidOutputs[i] = null;
            }
            remaining -= drained;
        }
        if (remaining == amount) return null;
        FluidStack drained = requested.copy();
        drained.amount = amount - remaining;
        if (doDrain) markStateChanged();
        return drained;
    }

    @Override
    public boolean canFill(ForgeDirection side, Fluid fluid) {
        if (fluid != null && GTModHandler.isSteam(new FluidStack(fluid, 2))) {
            IGregTechTileEntity tile = getBaseMetaTileEntity();
            return tile.getStoredSteam() < tile.getSteamCapacity();
        }
        return super.canFill(side, fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection side) {
        int inputs = getFluidInputSlotCount();
        int outputs = getFluidOutputSlotCount();
        FluidTankInfo[] tanks = new FluidTankInfo[1 + inputs + outputs];
        tanks[0] = super.getTankInfo(side)[0];
        for (int i = 0; i < inputs; i++) {
            tanks[1 + i] = new FluidTankInfo(fluidInputs[i], getCapacity());
        }
        for (int i = 0; i < outputs; i++) {
            tanks[1 + inputs + i] = new FluidTankInfo(fluidOutputs[i], getCapacity());
        }
        return tanks;
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setInteger("autoOutputSide", getAutoOutputSide().ordinal());
        saveFluids(nbt, "InputFluid", fluidInputs);
        saveFluids(nbt, "OutputFluid", fluidOutputs);
        saveFluids(nbt, "PendingFluidOutput", pendingFluidOutputs);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        autoOutputSide = nbt.hasKey("autoOutputSide") ? ForgeDirection.getOrientation(nbt.getInteger("autoOutputSide"))
            : ForgeDirection.DOWN;
        loadFluids(nbt, "InputFluid", fluidInputs);
        loadFluids(nbt, "OutputFluid", fluidOutputs);
        loadFluids(nbt, "PendingFluidOutput", pendingFluidOutputs);
    }

    @Override
    public void setItemNBT(NBTTagCompound nbt) {
        super.setItemNBT(nbt);
        saveNBTData(nbt);
        nbt.setLong("mStoredSteam", getBaseMetaTileEntity().getStoredSteam());
    }

    private static void saveFluids(NBTTagCompound nbt, String prefix, FluidStack[] fluids) {
        for (int i = 0; i < fluids.length; i++) {
            if (fluids[i] == null) nbt.removeTag(prefix + i);
            else nbt.setTag(prefix + i, fluids[i].writeToNBT(new NBTTagCompound()));
        }
    }

    private static void loadFluids(NBTTagCompound nbt, String prefix, FluidStack[] fluids) {
        for (int i = 0; i < fluids.length; i++) {
            fluids[i] = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag(prefix + i));
        }
        clearEmptyFluids(fluids);
    }

    @Override
    protected BasicUIProperties getUIProperties() {
        return super.getUIProperties().toBuilder().maxFluidInputs(getFluidInputSlotCount())
            .maxFluidOutputs(getFluidOutputSlotCount())
            .slotOverlaysSteamMUI2((index, isFluid, isOutput, isSpecial) -> isSpecial ? SteamTexture.NONE
                : isOutput ? GTGuiTextures.OVERLAY_SLOT_OUT_STEAM : GTGuiTextures.OVERLAY_SLOT_IN_STEAM)
            .progressBarTextureSteamMUI2(GTGuiTextures.PROGRESSBAR_ARROW_STEAM).build();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new SteamProcessingMachineGui(this, getUIProperties()).build(data, syncManager, settings);
    }

    @Override
    public ITexture[] getFrontFacingInactive(byte color) {
        return new ITexture[] { super.getFrontFacingInactive(color)[0],
            TextureFactory.of(Textures.BlockIcons.customOptional(ModList.GregTech.getResourceLocation(),
                "basicmachines/" + getMachineOverlay() + "/OVERLAY_FRONT")) };
    }

    @Override
    public ITexture[] getFrontFacingActive(byte color) {
        ITexture casing = super.getFrontFacingActive(color)[0];
        ITexture overlay = TextureFactory.of(Textures.BlockIcons.customOptional(ModList.GregTech.getResourceLocation(),
            "basicmachines/" + getMachineOverlay() + "/OVERLAY_FRONT_ACTIVE"));
        if (!hasActiveGlow()) return new ITexture[] { casing, overlay };
        return new ITexture[] { casing, overlay,
            TextureFactory.builder().addIcon(Textures.BlockIcons.customOptional(ModList.GregTech.getResourceLocation(),
                "basicmachines/" + getMachineOverlay() + "/OVERLAY_FRONT_ACTIVE_GLOW")).glow().build() };
    }
}
