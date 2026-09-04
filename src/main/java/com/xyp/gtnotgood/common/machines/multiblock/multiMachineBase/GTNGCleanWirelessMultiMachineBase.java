package com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase;

import static gregtech.common.misc.WirelessNetworkManager.addEUToGlobalEnergyMap;
import static gregtech.common.misc.WirelessNetworkManager.getUserEU;
import static gregtech.common.misc.WirelessNetworkManager.processInitialSettings;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/**
 * Minimal owner-bound wireless-EU base for GT Not Good multiblocks.
 * <p>
 * This base inherits the project's normal {@link GTNGMultiBlockBase} GUI and capability policy, but replaces its
 * processing logic with wireless-aware hooks. The durable player request for wireless mode is kept separate from
 * transient structure-derived availability, so a controller may expose its wireless toggle after a partial structure
 * check without requiring an energy hatch first.
 * <p>
 * Cross-recipe parallel processing is available as a bounded opt-in hook. It is disabled by default; subclasses must
 * explicitly return {@code true} from {@link #supportsCrossRecipeParallel()} to use it.
 *
 * @param <T> concrete controller type
 */
public abstract class GTNGCleanWirelessMultiMachineBase<T extends GTNGCleanWirelessMultiMachineBase<T>>
    extends GTNGMultiBlockBase<T> {

    private static final String NBT_WIRELESS_ENABLED = "gtngWirelessEnabled";
    private static final String NBT_WIRELESS_PARALLEL = "gtngWirelessParallel";
    private static final String NBT_OWNER_UUID = "gtngWirelessOwner";
    protected UUID ownerUUID;
    private boolean wirelessModeAvailable;
    private boolean wirelessModeEnabled;
    protected int wirelessParallel = 1;
    protected BigInteger costingEU = BigInteger.ZERO;
    protected String costingEUText = "0";
    private boolean inCrossRecipeProcessing;

    protected GTNGCleanWirelessMultiMachineBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GTNGCleanWirelessMultiMachineBase(String name) {
        super(name);
    }

    /** @return whether the current structure and controller item expose wireless operation */
    public final boolean isWirelessModeAvailable() {
        return wirelessModeAvailable;
    }

    /** @return the persisted player request, independent of current structure validity */
    public final boolean isWirelessModeEnabled() {
        return wirelessModeEnabled;
    }

    /** @return whether wireless operation is both requested and currently available */
    public final boolean isWirelessModeActive() {
        return wirelessModeEnabled && wirelessModeAvailable;
    }

    /** Compatibility accessor for machine processing code. */
    public final boolean isEnableWireless() {
        return isWirelessModeActive();
    }

    /**
     * Stores the player's wireless request. Structure validation remains authoritative for whether the request is
     * effective.
     */
    public void setWirelessModeEnabled(boolean enabled) {
        if (wirelessModeEnabled == enabled) return;
        wirelessModeEnabled = enabled;
        markWirelessStateDirty();
    }

    /** Updates the transient structure-derived wireless capability without clearing the persisted request. */
    protected final void setWirelessModeAvailable(boolean available) {
        wirelessModeAvailable = available;
    }

    public final int getWirelessParallel() {
        return wirelessParallel;
    }

    public void setWirelessParallel(int parallel) {
        wirelessParallel = Math.max(1, parallel);
        markWirelessStateDirty();
    }

    public final boolean areEnergyHatchesEmpty() {
        return mEnergyHatches.isEmpty() && mExoticEnergyHatches.isEmpty();
    }

    private void markWirelessStateDirty() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null && base.isServerSide()) base.markDirty();
    }

    @Override
    public void onFirstTick(IGregTechTileEntity base) {
        super.onFirstTick(base);
        initializeWirelessOwner(base);
    }

    @Override
    public void onPreTick(IGregTechTileEntity base, long tick) {
        super.onPreTick(base, tick);
        if (base.isServerSide() && ownerUUID == null) initializeWirelessOwner(base);
    }

    private void initializeWirelessOwner(IGregTechTileEntity base) {
        if (base != null && base.isServerSide()) ownerUUID = processInitialSettings(base);
    }

    protected boolean isEnablePerfectOverclock() {
        return false;
    }

    protected float getEuModifier() {
        return 1.0F;
    }

    protected float getSpeedBonus() {
        return 1.0F;
    }

    protected boolean useNoOverclockInWirelessMode() {
        return true;
    }

    protected double getOverclockTimeReduction() {
        return isEnablePerfectOverclock() ? 4.0D : 2.0D;
    }

    protected double getOverclockPowerIncrease() {
        return 4.0D;
    }

    protected int getLimitedMaxParallel() {
        return Math.max(1, getMaxParallelRecipes());
    }

    /** Returns the single-recipe parallel cap after recipe selection. */
    protected int getMaxParallelForRecipe(GTRecipe recipe) {
        return getLimitedMaxParallel();
    }

    /** Machine-specific validation hook used after the recipe is selected. */
    protected CheckRecipeResult validateRecipeForMachine(GTRecipe recipe) {
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            public CheckRecipeResult process() {
                setEuModifier(getEuModifier());
                setSpeedBonus(getSpeedBonus());
                setOverclock(getOverclockTimeReduction(), getOverclockPowerIncrease());
                return super.process();
            }

            @NotNull
            @Override
            protected ParallelHelper createParallelHelper(@NotNull GTRecipe recipe) {
                ParallelHelper helper = super.createParallelHelper(recipe);
                if (isWirelessModeActive()) helper.setConsumption(false);
                return helper;
            }

            @Override
            protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> map) {
                return filterRecipeMatches(super.findRecipeMatches(map));
            }

            @NotNull
            @Override
            protected OverclockCalculator createOverclockCalculator(@NotNull GTRecipe recipe) {
                if (isWirelessModeActive() && useNoOverclockInWirelessMode()) {
                    return OverclockCalculator.ofNoOverclock(recipe)
                        .setEUtDiscount(getEuModifier())
                        .setDurationModifier(getSpeedBonus());
                }
                return super.createOverclockCalculator(recipe);
            }

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@NotNull GTRecipe recipe) {
                maxParallel = Math.max(1, getMaxParallelForRecipe(recipe));
                CheckRecipeResult machineResult = validateRecipeForMachine(recipe);
                if (!machineResult.wasSuccessful() || !isWirelessModeActive()) return machineResult;

                long recipeEUt = Math.max(1L, (long) Math.ceil(recipe.mEUt * getEuModifier()));
                int recipeDuration = Math.max(1, (int) Math.ceil(recipe.mDuration * getSpeedBonus()));
                BigInteger recipeEU = BigInteger.valueOf(recipeEUt)
                    .multiply(BigInteger.valueOf(recipeDuration));
                if (ownerUUID == null || getUserEU(ownerUUID).compareTo(recipeEU) < 0) {
                    return CheckRecipeResultRegistry.insufficientStartupPower(recipeEU);
                }
                maxParallel = getUserEU(ownerUUID).divide(recipeEU)
                    .min(BigInteger.valueOf(maxParallel))
                    .intValue();
                return validateWirelessPowerForRecipe(recipeEUt, recipeDuration, maxParallel);
            }

            @NotNull
            @Override
            protected CheckRecipeResult onRecipeStart(@NotNull GTRecipe recipe) {
                if (isWirelessModeActive()) {
                    CheckRecipeResult wirelessResult = startWirelessRecipe(
                        recipe,
                        calculatedParallels,
                        calculatedEut,
                        duration,
                        inputFluids,
                        inputItems);
                    if (!wirelessResult.wasSuccessful()) return wirelessResult;
                    overwriteCalculatedEut(0);
                }
                return super.onRecipeStart(recipe);
            }
        }.setMaxParallelSupplier(this::getLimitedMaxParallel)
            .setUnlimitedTierSkips();
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        if (isWirelessModeActive()) {
            logic.setAvailableVoltage(Long.MAX_VALUE);
            logic.setAvailableAmperage(1);
            logic.setAmperageOC(false);
            logic.setUnlimitedTierSkips();
        } else {
            super.setProcessingLogicPower(logic);
        }
    }

    @Override
    public long getMaxInputVoltage() {
        return isWirelessModeActive() ? Long.MAX_VALUE : super.getMaxInputVoltage();
    }

    /** @return true only for subclasses intentionally enabling bounded cross-recipe processing */
    protected boolean supportsCrossRecipeParallel() {
        return false;
    }

    /** Allows a controller to restrict the recipes visible to the shared processing logic. */
    protected Stream<GTRecipe> filterRecipeMatches(Stream<GTRecipe> recipes) {
        return recipes;
    }

    /** @return configured number of recipe selections attempted in one wireless batch */
    protected int getCrossRecipeParallelLimit() {
        return Config.wirelessCrossRecipeParallelLimit;
    }

    /** @return configured fixed completion time for one wireless cross-recipe batch */
    protected int getWirelessCrossRecipeDuration() {
        return Config.wirelessCrossRecipeDurationTicks;
    }

    @Nonnull
    @Override
    public CheckRecipeResult checkProcessing() {
        costingEU = BigInteger.ZERO;
        costingEUText = "0";
        if (!isWirelessModeActive() || !supportsCrossRecipeParallel() || inCrossRecipeProcessing) {
            return super.checkProcessing();
        }
        return checkProcessingCrossRecipe();
    }

    private CheckRecipeResult checkProcessingCrossRecipe() {
        inCrossRecipeProcessing = true;
        try {
            CheckRecipeResult firstResult = super.checkProcessing();
            if (!firstResult.wasSuccessful()) return firstResult;

            ArrayList<ItemStack> accumulatedItems = copyItems(mOutputItems);
            ArrayList<FluidStack> accumulatedFluids = copyFluids(mOutputFluids);
            long maximumEUt = Math.abs(lEUt);
            int limit = Math.max(1, getCrossRecipeParallelLimit());

            for (int iteration = 1; iteration < limit; iteration++) {
                CheckRecipeResult result = super.checkProcessing();
                if (!result.wasSuccessful()) break;
                maximumEUt = Math.max(maximumEUt, Math.abs(lEUt));
                appendItems(accumulatedItems, mOutputItems);
                appendFluids(accumulatedFluids, mOutputFluids);
            }

            mOutputItems = accumulatedItems.toArray(new ItemStack[0]);
            mOutputFluids = accumulatedFluids.toArray(new FluidStack[0]);
            mMaxProgresstime = Math.max(1, getWirelessCrossRecipeDuration());
            lEUt = isWirelessModeActive() ? 0 : -maximumEUt;
            mEfficiency = 10000;
            mEfficiencyIncrease = 10000;
            costingEUText = NumberFormatUtil.formatNumber(costingEU);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } finally {
            inCrossRecipeProcessing = false;
        }
    }

    protected final CheckRecipeResult validateWirelessPowerForRecipe(long eut, int duration, int parallel) {
        if (!isWirelessModeActive()) return CheckRecipeResultRegistry.SUCCESSFUL;
        BigInteger required = calculateWirelessCost(eut, duration).multiply(BigInteger.valueOf(Math.max(1, parallel)));
        if (ownerUUID == null || getUserEU(ownerUUID).compareTo(required) < 0) {
            return CheckRecipeResultRegistry.insufficientStartupPower(required);
        }
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    /** Deducts wireless EU before consuming recipe inputs. */
    protected final CheckRecipeResult startWirelessRecipe(GTRecipe recipe, int parallel, long eut, int duration,
        FluidStack[] fluidInputs, ItemStack[] itemInputs) {
        if (!isWirelessModeActive()) return CheckRecipeResultRegistry.SUCCESSFUL;
        BigInteger required = calculateWirelessCost(eut, duration);
        if (ownerUUID == null || !addEUToGlobalEnergyMap(ownerUUID, required.negate())) {
            return CheckRecipeResultRegistry.insufficientStartupPower(required);
        }
        costingEU = costingEU.add(required);
        costingEUText = NumberFormatUtil.formatNumber(costingEU);
        recipe.consumeInput(Math.max(1, parallel), fluidInputs, itemInputs);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private static BigInteger calculateWirelessCost(long eut, int duration) {
        return BigInteger.valueOf(eut)
            .abs()
            .multiply(BigInteger.valueOf(Math.max(1, duration)));
    }

    private static ArrayList<ItemStack> copyItems(ItemStack[] items) {
        ArrayList<ItemStack> result = new ArrayList<>();
        appendItems(result, items);
        return result;
    }

    private static void appendItems(List<ItemStack> target, ItemStack[] items) {
        if (items == null) return;
        for (ItemStack item : items) if (item != null) target.add(item.copy());
    }

    private static ArrayList<FluidStack> copyFluids(FluidStack[] fluids) {
        ArrayList<FluidStack> result = new ArrayList<>();
        appendFluids(result, fluids);
        return result;
    }

    private static void appendFluids(List<FluidStack> target, FluidStack[] fluids) {
        if (fluids == null) return;
        for (FluidStack fluid : fluids) if (fluid != null) target.add(fluid.copy());
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack stack) {
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setBoolean(NBT_WIRELESS_ENABLED, wirelessModeEnabled);
        tag.setInteger(NBT_WIRELESS_PARALLEL, wirelessParallel);
        if (ownerUUID != null) tag.setString(NBT_OWNER_UUID, ownerUUID.toString());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        wirelessModeEnabled = tag.getBoolean(NBT_WIRELESS_ENABLED);
        wirelessParallel = Math.max(1, tag.getInteger(NBT_WIRELESS_PARALLEL));
        if (tag.hasKey(NBT_OWNER_UUID)) {
            try {
                ownerUUID = UUID.fromString(tag.getString(NBT_OWNER_UUID));
            } catch (IllegalArgumentException ignored) {
                ownerUUID = null;
            }
        }
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean("gtngWirelessMode", isWirelessModeActive());
        tag.setString("gtngWirelessCost", costingEUText);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.getBoolean("gtngWirelessMode")) return;
        // #tr machine.gtnotgood.wireless.mode
        // # Wireless Mode
        // # zh_CN 无线模式
        currentTip
            .add(EnumChatFormatting.LIGHT_PURPLE + StatCollector.translateToLocal("machine.gtnotgood.wireless.mode"));
        // #tr machine.gtnotgood.wireless.cost
        // # EU Cost
        // # zh_CN 耗电
        currentTip.add(
            EnumChatFormatting.AQUA + StatCollector.translateToLocal("machine.gtnotgood.wireless.cost")
                + EnumChatFormatting.RESET
                + ": "
                + EnumChatFormatting.GOLD
                + tag.getString("gtngWirelessCost")
                + EnumChatFormatting.RESET
                + " EU");
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }
}
