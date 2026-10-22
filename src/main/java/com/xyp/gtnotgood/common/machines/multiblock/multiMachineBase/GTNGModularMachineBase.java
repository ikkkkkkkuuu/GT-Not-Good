// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase;

import static gregtech.api.util.GTUtility.validMTEList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModularHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModuleType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ParameterModuleHatch;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;
import com.xyp.gtnotgood.common.machines.processing.ModuleInputPreflight;
import com.xyp.gtnotgood.common.machines.processing.ModuleWork;
import com.xyp.gtnotgood.common.machines.processing.ModuleWorkDisplay;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.api.util.tooltip.TooltipHelper;
import gregtech.common.misc.GlobalEnergyWorldSavedData;
import gregtech.common.misc.WirelessNetworkManager;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

/** Independent execution cores share structure inputs; only accepted, funded recipes commit their input snapshots. */
public abstract class GTNGModularMachineBase<T extends GTNGModularMachineBase<T>> extends GTNGMultiBlockBase<T> {

    private final List<ModularHatch> modularHatches = new ArrayList<>();
    private final List<ModuleWork> moduleWorks = new ArrayList<>();
    private CoreSlot preparingCore;
    private boolean preparingFullPower;
    private ModuleWork pendingWork;
    private boolean recipeTransactionOpen;
    private boolean legacyRun;
    private boolean paused;
    private long lastModuleTick = Long.MIN_VALUE;

    protected GTNGModularMachineBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GTNGModularMachineBase(String name) {
        super(name);
    }

    @Override
    public final boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public final boolean shouldCheckMaintenance() {
        return false;
    }

    @Override
    public final void checkMaintenance() {
        fixAllIssues();
    }

    @Override
    public final int getRepairStatus() {
        return getIdealStatus();
    }

    @Override
    protected final ProcessingLogic createProcessingLogic() {
        return createMachineProcessingLogic();
    }

    protected ProcessingLogic createMachineProcessingLogic() {
        return new ModularProcessingLogic().setMaxParallelSupplier(this::getTrueParallel).setMaxTierSkips(0);
    }

    protected int getBaseParallelRecipes() {
        return 1;
    }

    protected double getBaseSpeedMultiplier() {
        return 1;
    }

    protected boolean hasPerfectBaseOverclock() {
        return false;
    }

    @Override
    public long getMaxInputVoltage() {
        long voltage = 0;
        for (var hatch : validMTEList(getExoticAndNormalEnergyHatchList()))
            voltage = Math.max(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
        return voltage;
    }

    @Override
    public final int getMaxParallelRecipes() {
        ParameterModuleHatch parallel = parameter(ModuleType.Parallel);
        long multiplier = parallel == null ? 1 : 1L + parallel.getParameter();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, getBaseParallelRecipes()) * multiplier);
    }

    private int getCoreParallel() {
        if (preparingCore != null && preparingCore.type == CoreType.Perfect) return Integer.MAX_VALUE;
        int total = getTrueParallel();
        if (preparingCore != null && preparingCore.wireless != null) {
            BigInteger cap = preparingCore.wireless.getParallelLimit();
            if (cap.signum() > 0) total = Math.min(total, cap.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue());
        } else if (preparingCore != null && preparingCore.type == CoreType.Normal) {
            int cores = 1;
            for (ModularHatch hatch : modularHatches)
                if (hatch instanceof ExecutionCoreHatch core && core.getCoreType() == CoreType.Normal) cores++;
            total = Math.max(1, total / cores);
        }
        return total;
    }

    private ParameterModuleHatch parameter(ModuleType type) {
        for (ModularHatch hatch : modularHatches)
            if (hatch.getModuleType() == type && hatch instanceof ParameterModuleHatch parameter) return parameter;
        return null;
    }

    public final double getModuleSpeedMultiplier() {
        ParameterModuleHatch speed = parameter(ModuleType.Speed);
        return getBaseSpeedMultiplier() * (speed == null ? 1 : speed.getParameter());
    }

    public final double getModulePowerMultiplier() {
        ParameterModuleHatch power = parameter(ModuleType.PowerConsumption);
        return power == null ? 1 : power.getPowerMultiplier();
    }

    public final int getModuleTimeReduction() {
        ParameterModuleHatch overclock = parameter(ModuleType.Overclock);
        return overclock == null ? (hasPerfectBaseOverclock() ? 4 : 2) : overclock.getTimeReduction();
    }

    public final int getModulePowerIncrease() {
        ParameterModuleHatch overclock = parameter(ModuleType.Overclock);
        return overclock == null ? 4 : overclock.getPowerIncrease();
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ModularHatch hatch : modularHatches) {
            hatch.setWorking(false);
            if (hatch instanceof ExecutionCoreHatch core) core.setExecutionInfo(null, true);
        }
        modularHatches.clear();
    }

    public final boolean addModularHatchToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ModularHatch hatch)) return false;
        if (!modularHatches.contains(hatch)) {
            hatch.updateTexture(casingIndex);
            hatch.updateCraftingIcon(getMachineCraftingIcon());
            modularHatches.add(hatch);
        }
        return true;
    }

    public final void checkModularHatches(List<StructureError> errors) {
        Map<ModuleType, Integer> counts = new EnumMap<>(ModuleType.class);
        for (ModularHatch hatch : modularHatches) counts.merge(hatch.getModuleType(), 1, Integer::sum);
        for (Map.Entry<ModuleType, Integer> entry : counts.entrySet()) {
            if (entry.getKey() != ModuleType.ExecutionCore && entry.getValue() > 1) {
                // #tr gtng.modular.parameter_module
                // # Parameter module
                // # zh_CN 参数模块仓
                errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY,
                    TranslatableText.lang("gtng.modular.parameter_module"), entry.getValue(), 1));
            }
        }
    }

    public final List<ModularHatch> getModularHatches() {
        return Collections.unmodifiableList(modularHatches);
    }

    public final List<ModuleWork> getModuleWorks() {
        return Collections.unmodifiableList(moduleWorks);
    }

    public final List<NBTTagCompound> getDisplayedModuleOutputs(int limit) {
        List<NBTTagCompound> rows = new ArrayList<>();
        Map<String, CoreSlot> bound = boundSlots();
        for (ModuleWork work : moduleWorks) {
            int ticks = paused || work.finished() || !isAllowedToWork() || !isBound(work, bound) ? 0 : work.duration;
            WirelessRecipeDisplay.appendRows(rows, work.outputs, ticks, limit);
            if (rows.size() >= limit) break;
        }
        return rows;
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }

    public final List<NBTTagCompound> getDisplayedModuleTasks(int outputLimit) {
        Map<String, CoreSlot> bound = boundSlots();
        Map<String, CoreType> cores = new TreeMap<>();
        Map<String, ModuleWork> workByCore = new HashMap<>();
        for (CoreSlot slot : bound.values()) cores.put(slot.key, slot.type);
        for (ModularHatch hatch : modularHatches) if (hatch instanceof ExecutionCoreHatch core && core.isValid())
            cores.put(coreKey(core.getBaseMetaTileEntity(), core.getCoreType()), core.getCoreType());
        for (ModuleWork work : moduleWorks) {
            cores.putIfAbsent(work.core, work.coreType);
            workByCore.put(work.core, work);
        }
        cores.remove("controller");
        List<NBTTagCompound> tasks = new ArrayList<>();
        NBTTagCompound controller = moduleDisplayTask("controller", CoreType.Normal, 0, workByCore.get("controller"),
            bound, outputLimit);
        if (legacyRun && moduleWorks.isEmpty() && mMaxProgresstime > 0) {
            controller.setInteger("duration", mMaxProgresstime);
            controller.setBoolean("paused", paused || !isAllowedToWork());
            controller.setBoolean("finished", mProgresstime >= mMaxProgresstime);
            List<NBTTagCompound> outputs = new ArrayList<>();
            WirelessRecipeDisplay.appendRows(outputs, new WirelessOutputs(mOutputItems, mOutputFluids),
                controller.getBoolean("paused") || controller.getBoolean("finished") ? 0 : mMaxProgresstime,
                outputLimit);
            NBTTagList outputTags = new NBTTagList();
            for (NBTTagCompound output : outputs) outputTags.appendTag(output);
            controller.setTag("outputs", outputTags);
        }
        tasks.add(controller);
        int remaining = Math.max(0, outputLimit - controller.getTagList("outputs", 10).tagCount());
        int index = 0;
        for (Map.Entry<String, CoreType> entry : cores.entrySet()) {
            if (!entry.getKey().startsWith("wireless:")) index++;
            NBTTagCompound task = moduleDisplayTask(entry.getKey(), entry.getValue(), index,
                workByCore.get(entry.getKey()), bound, remaining);
            tasks.add(task);
            remaining = Math.max(0, remaining - task.getTagList("outputs", 10).tagCount());
        }
        return tasks;
    }

    private NBTTagCompound moduleDisplayTask(String core, CoreType type, int index, ModuleWork work,
        Map<String, CoreSlot> bound, int outputLimit) {
        return ModuleWorkDisplay.task(core, type, index, work, paused || !isAllowedToWork() || !isBound(work, bound),
            outputLimit);
    }

    public final NBTTagCompound getDisplayedModuleProgress() {
        NBTTagCompound progress = new NBTTagCompound();
        if (legacyRun && moduleWorks.isEmpty() && mMaxProgresstime > 0)
            progress.setInteger("controller", mProgresstime);
        for (ModuleWork work : moduleWorks) progress.setInteger(work.core, work.progress());
        return progress;
    }

    protected long getModuleComputationCapacity() {
        return Long.MAX_VALUE;
    }

    protected long getStartedModuleComputation() {
        return 0;
    }

    public final long getModuleUsedComputation() {
        Map<String, CoreSlot> bound = boundSlots();
        long used = 0;
        for (ModuleWork work : moduleWorks)
            if (!work.finished() && isBound(work, bound)) used += Math.min(Long.MAX_VALUE - used, work.computation);
        return used;
    }

    protected final long getAvailableModuleComputation() {
        return Math.max(0, getModuleComputationCapacity() - getModuleUsedComputation());
    }

    private long getUsedModulePower() {
        Map<String, CoreSlot> bound = boundSlots();
        long used = 0;
        for (ModuleWork work : moduleWorks) if (work.controllerPower && !work.finished() && isBound(work, bound))
            used += Math.min(Long.MAX_VALUE - used, work.eut);
        return used;
    }

    protected final long getModuleAvailablePower() {
        if (preparingCore == null) return Math.max(0, getMaxInputEu());
        if (preparingCore.type == CoreType.Perfect || preparingCore.wireless != null) return Long.MAX_VALUE;
        if (preparingCore.type == CoreType.Advanced) return Math.max(0, getMaxInputEu());
        long remaining = Math.max(0, getMaxInputEu() - getUsedModulePower());
        if (preparingFullPower) return remaining;
        Set<String> occupied = new HashSet<>();
        for (ModuleWork work : moduleWorks) occupied.add(work.core);
        int idle = 0;
        for (CoreSlot slot : getCoreSlots())
            if (slot.type == CoreType.Normal && slot.wireless == null && !occupied.contains(slot.key)) idle++;
        return remaining / Math.max(1, idle);
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        long power = getModuleAvailablePower();
        boolean wireless = preparingCore != null
            && (preparingCore.type == CoreType.Perfect || preparingCore.wireless != null);
        long voltage = wireless ? Long.MAX_VALUE : Math.min(power, Math.max(0, getMaxInputVoltage()));
        logic.setAvailableVoltage(voltage).setAvailableAmperage(voltage <= 0 ? 0 : Math.max(1, power / voltage))
            .setAmperageOC(false).setMaxTierSkips(0);
        logic.setMaxParallelSupplier(this::getCoreParallel);
        logic.setSpeedBonus(1D / getModuleSpeedMultiplier()).setEuModifier(getModulePowerMultiplier())
            .setOverclock(getModuleTimeReduction(), getModulePowerIncrease());
        logic.setRecipeLocking(this, false);
        ((ModularProcessingLogic) logic).prepareModuleRecipe(power, wireless);
    }

    @Override
    @Nonnull
    public final CheckRecipeResult checkProcessing() {
        if (!(processingLogic instanceof ModularProcessingLogic logic)) return CheckRecipeResultRegistry.INTERNAL_ERROR;
        boolean direct = preparingCore == null;
        if (direct) preparingCore = getCoreSlots().get(0);
        CoreSlot slot = preparingCore;
        try {
            if (workFor(slot.key) != null) return CheckRecipeResultRegistry.NO_RECIPE;
            setupProcessingLogic(logic);
            CheckRecipeResult result = postCheckRecipe(doCheckRecipe(), logic);
            if (
                !result.wasSuccessful() && slot.type == CoreType.Normal
                    && slot.wireless == null
                    && getModuleAvailablePower() < Math.max(0, getMaxInputEu() - getUsedModulePower())
            ) {
                logic.finishModuleRecipe();
                preparingFullPower = true;
                setupProcessingLogic(logic);
                result = postCheckRecipe(doCheckRecipe(), logic);
            }
            if (!result.wasSuccessful()) return result;
            long eut = logic.getCalculatedEut();
            int calculatedDuration = Math.max(1, logic.getDuration());
            long computation = Math.max(0, getStartedModuleComputation());
            if (computation > getAvailableModuleComputation()) return CheckRecipeResultRegistry.NO_RECIPE;
            if (eut < 0 || !logic.canCommitInputs()) return CheckRecipeResultRegistry.INTERNAL_ERROR;
            if (!ModuleInputPreflight.canCommit(this, logic)) {
                // #tr GT5U.gui.text.recipe_result.gtng_modular_me_unavailable
                // # Insufficient ME stock or network energy
                // # zh_CN ME 库存或网络能源不足
                return SimpleCheckRecipeResult.ofFailure("gtng_modular_me_unavailable");
            }
            boolean controllerPower = slot.type == CoreType.Normal && slot.wireless == null;
            if (controllerPower && eut > getModuleAvailablePower())
                return CheckRecipeResultRegistry.insufficientPower(eut);
            BigInteger payment = controllerPower ? BigInteger.ZERO
                : BigInteger.valueOf(eut).multiply(BigInteger.valueOf(calculatedDuration));
            if (
                payment.signum() > 0 && (GlobalEnergyWorldSavedData.INSTANCE == null
                    || !WirelessNetworkManager.addEUToGlobalEnergyMap(slot.owner, payment.negate()))
            ) return CheckRecipeResultRegistry.insufficientStartupPower(payment);
            if (!logic.commitInputs()) {
                if (payment.signum() > 0) WirelessNetworkManager.addEUToGlobalEnergyMap(slot.owner, payment);
                return CheckRecipeResultRegistry.INTERNAL_ERROR;
            }
            int duration = slot.type == CoreType.Perfect ? 20
                : slot.wireless != null ? slot.wireless.getDuration() : calculatedDuration;
            ModuleWork work = new ModuleWork(slot.key, slot.type, slot.owner, eut, computation, duration,
                Math.max(1, logic.getCurrentParallels()), controllerPower, payment,
                new WirelessOutputs(logic.getOutputItems(), logic.getOutputFluids()), getModuleTimeReduction(),
                getModulePowerIncrease());
            moduleWorks.add(work);
            if (recipeTransactionOpen) pendingWork = work;
            legacyRun = false;
            mEfficiency = Math.max(1000, 10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            mEfficiencyIncrease = 10000;
            updateSlots();
            markDirty();
            displayModuleWork();
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } finally {
            logic.finishModuleRecipe();
            preparingFullPower = false;
            if (direct) preparingCore = null;
        }
    }

    private ModuleWork workFor(String key) {
        for (ModuleWork work : moduleWorks) if (work.core.equals(key)) return work;
        return null;
    }

    private List<CoreSlot> getCoreSlots() {
        List<CoreSlot> slots = new ArrayList<>();
        IGregTechTileEntity controller = getBaseMetaTileEntity();
        UUID owner = controller == null ? new UUID(0, 0) : controller.getOwnerUuid();
        CrossRecipeWirelessEnergyHatch wireless = null;
        for (var hatch : mEnergyHatches) if (hatch instanceof CrossRecipeWirelessEnergyHatch cross) {
            wireless = cross;
            owner = cross.getBaseMetaTileEntity().getOwnerUuid();
            break;
        }
        slots.add(new CoreSlot("controller", CoreType.Normal, owner, wireless));
        for (ModularHatch hatch : modularHatches) if (hatch instanceof ExecutionCoreHatch core && core.isValid()) {
            IGregTechTileEntity tile = core.getBaseMetaTileEntity();
            if (!tile.isAllowedToWork()) continue;
            slots.add(new CoreSlot(coreKey(tile, core.getCoreType()), core.getCoreType(),
                core.getCoreType() == CoreType.Normal ? owner : tile.getOwnerUuid(),
                core.getCoreType() == CoreType.Normal ? wireless : null));
        }
        if (wireless != null) for (int i = 1; i < wireless.getTaskLimit(); i++)
            slots.add(new CoreSlot("wireless:" + i, CoreType.Normal, owner, wireless));
        return slots;
    }

    private Map<String, CoreSlot> boundSlots() {
        Map<String, CoreSlot> bound = new HashMap<>();
        for (CoreSlot slot : getCoreSlots()) bound.put(slot.key, slot);
        return bound;
    }

    private static String coreKey(IGregTechTileEntity tile, CoreType type) {
        return type.name() + ":" + tile.getXCoord() + ":" + tile.getYCoord() + ":" + tile.getZCoord();
    }

    private boolean isBound(ModuleWork work, Map<String, CoreSlot> slots) {
        if (work == null) return false;
        CoreSlot slot = slots.get(work.core);
        return slot != null && slot.type == work.coreType
            && slot.owner.equals(work.owner)
            && work.controllerPower == (slot.type == CoreType.Normal && slot.wireless == null);
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        if (legacyRun && moduleWorks.isEmpty() && mMaxProgresstime > 0) {
            super.runMachine(tile, tick);
            return;
        }
        legacyRun = false;
        if (lastModuleTick == tick) return;
        lastModuleTick = tick;
        List<CoreSlot> slots = getCoreSlots();
        Map<String, CoreSlot> bound = new HashMap<>();
        for (CoreSlot slot : slots) bound.put(slot.key, slot);
        paused = !tile.isAllowedToWork();
        if (paused) {
            displayModuleWork();
            return;
        }
        List<ModuleWork> running = new ArrayList<>();
        for (ModuleWork work : moduleWorks) if (!work.finished() && isBound(work, bound)) running.add(work);
        long used = 0;
        long computation = 0;
        long powerCapacity = Math.max(0, getMaxInputEu());
        long computationCapacity = Math.max(0, getModuleComputationCapacity());
        boolean exceedsCapacity = false;
        for (ModuleWork work : running) {
            if (work.controllerPower) {
                exceedsCapacity |= work.eut > powerCapacity - used;
                used += Math.min(Long.MAX_VALUE - used, work.eut);
            }
            exceedsCapacity |= work.computation > computationCapacity - computation;
            computation += Math.min(Long.MAX_VALUE - computation, work.computation);
        }
        paused = exceedsCapacity;
        int[] boosts = new int[running.size()];
        if (!paused && !running.isEmpty()) {
            mEfficiency = Math.max(1000, 10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            long extra = Math.max(0, getMaxInputEu() - used);
            for (int i = 0; i < running.size(); i++) {
                ModuleWork work = running.get(i);
                if (work.coreType == CoreType.Perfect || work.eut <= 0 || work.remaining() <= 1) continue;
                long budget = extra / (running.size() - i);
                int boost = 0;
                long boostCost = 0;
                if (work.timeReduction >= work.powerIncrease) {
                    boost = (int) Math.min(work.remaining() - 1L, budget / work.eut);
                    boostCost = work.eut * boost;
                } else {
                    long multiplier = 1;
                    long time = 1;
                    while (time < work.remaining() && multiplier <= Long.MAX_VALUE / work.powerIncrease) {
                        long next = multiplier * work.powerIncrease;
                        if (next - 1 > budget / work.eut) break;
                        multiplier = next;
                        time = Math.min(Integer.MAX_VALUE, time * work.timeReduction);
                    }
                    boost = (int) Math.min(work.remaining() - 1L, time - 1);
                    boostCost = work.eut * (multiplier - 1);
                }
                boosts[i] = boost;
                used += boostCost;
                extra -= boostCost;
            }
            lEUt = -used;
            if (
                !doRandomMaintenanceDamage() || !onRunningTick(getControllerSlot())
                    || !polluteEnvironment(getPollutionPerTick(getControllerSlot()))
            ) paused = true;
            if (!paused) for (int i = 0; i < running.size(); i++) running.get(i).advance(1 + boosts[i]);
            markDirty();
        }
        boolean completed = false;
        for (Iterator<ModuleWork> it = moduleWorks.iterator(); it.hasNext();) {
            ModuleWork work = it.next();
            if (work.finished() && isBound(work, bound) && work.outputs.flush(this)) {
                it.remove();
                recipesDone += work.parallels;
                outputAfterRecipe();
                completed = true;
                markDirty();
            }
        }
        if (
            !paused && tile.isAllowedToWork()
                && (completed || tick % 5 == 0
                    || tile.hasInventoryBeenModified()
                    || tile.hasWorkJustBeenEnabled()
                    || shouldCheckRecipeThisTick(tick))
        ) {
            int attempts = 0;
            // Prepaid cores do not reserve the controller's ordinary EU/t budget.
            slots.sort((a, b) -> Integer.compare(b.type.ordinal(), a.type.ordinal()));
            for (CoreSlot slot : slots) {
                if (workFor(slot.key) != null) continue;
                if (slot.wireless != null) {
                    long tasks = moduleWorks.stream()
                        .filter(work -> !work.controllerPower && work.coreType == CoreType.Normal).count();
                    if (tasks >= slot.wireless.getTaskLimit()) continue;
                }
                preparingCore = slot;
                startRecipeProcessing();
                CheckRecipeResult result;
                try {
                    result = checkProcessing();
                    setCheckRecipeResult(result);
                } finally {
                    endRecipeProcessing();
                    preparingCore = null;
                }
                if (++attempts >= 8) break;
            }
        }
        displayModuleWork();
    }

    private void displayModuleWork() {
        Map<String, CoreSlot> bound = boundSlots();
        Map<String, ModuleWork> workByCore = new HashMap<>();
        for (ModuleWork work : moduleWorks) workByCore.put(work.core, work);
        mOutputItems = null;
        mOutputFluids = null;
        mEUt = 0;
        lEUt = -getUsedModulePower();
        mProgresstime = 0;
        mMaxProgresstime = 0;
        if (!paused) for (ModuleWork work : moduleWorks) if (!work.finished() && isBound(work, bound)) {
            mProgresstime = work.progress();
            mMaxProgresstime = work.duration;
            break;
        }
        for (ModularHatch hatch : modularHatches) {
            boolean working = false;
            if (!paused && hatch instanceof ExecutionCoreHatch core && core.isValid()) {
                ModuleWork work = workByCore.get(coreKey(core.getBaseMetaTileEntity(), core.getCoreType()));
                working = work != null && !work.finished() && isBound(work, bound);
                core.setExecutionInfo(work, paused || !isBound(work, bound));
            } else if (hatch instanceof ExecutionCoreHatch core && core.isValid()) {
                core.setExecutionInfo(workByCore.get(coreKey(core.getBaseMetaTileEntity(), core.getCoreType())), true);
            }
            hatch.setWorking(working);
        }
    }

    @Override
    public void startRecipeProcessing() {
        super.startRecipeProcessing();
        recipeTransactionOpen = true;
        pendingWork = null;
    }

    @Override
    public void endRecipeProcessing() {
        try {
            super.endRecipeProcessing();
            if (pendingWork != null && !getCheckRecipeResult().wasSuccessful()) {
                moduleWorks.remove(pendingWork);
                if (pendingWork.prepaidEU.signum() > 0)
                    WirelessNetworkManager.addEUToGlobalEnergyMap(pendingWork.owner, pendingWork.prepaidEU);
                markDirty();
                displayModuleWork();
            }
        } finally {
            pendingWork = null;
            recipeTransactionOpen = false;
        }
    }

    @Override
    public void stopMachine(@Nonnull ShutDownReason reason) {
        super.stopMachine(reason);
        paused = true;
        for (ModularHatch hatch : modularHatches) hatch.setWorking(false);
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
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        NBTTagList works = new NBTTagList();
        for (ModuleWork work : moduleWorks) works.appendTag(work.save());
        tag.setTag("gtngModuleWork", works);
        tag.setBoolean("gtngModuleLegacyRun", legacyRun);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        moduleWorks.clear();
        NBTTagList works = tag.getTagList("gtngModuleWork", 10);
        for (int i = 0; i < works.tagCount(); i++) moduleWorks.add(ModuleWork.load(works.getCompoundTagAt(i)));
        legacyRun = tag.hasKey("gtngModuleWork") ? tag.getBoolean("gtngModuleLegacyRun") : mMaxProgresstime > 0;
        if (legacyRun && mMaxProgresstime > 0) mEfficiency = 10000;
        lastModuleTick = Long.MIN_VALUE;
        if (!legacyRun) displayModuleWork();
    }

    @Override
    public String[] getInfoData() {
        String[] original = super.getInfoData();
        String[] result = new String[original.length + 2];
        System.arraycopy(original, 0, result, 0, original.length);
        // #tr gtng.modular.info.cores
        // # Execution tasks: %s
        // # zh_CN 执行任务：%s
        result[original.length] = StatCollector.translateToLocalFormatted("gtng.modular.info.cores",
            moduleWorks.size());
        // #tr gtng.modular.info.overclock
        // # Overclock: duration /%s, EU/t x%s
        // # zh_CN 超频：耗时 ÷%s，EU/t ×%s
        result[original.length + 1] = StatCollector.translateToLocalFormatted("gtng.modular.info.overclock",
            getModuleTimeReduction(), getModulePowerIncrease());
        return result;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("gtngModuleTasks", moduleWorks.size());
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        if (accessor.getNBTData().hasKey("gtngModuleTasks")) lines.add(StatCollector
            .translateToLocalFormatted("gtng.modular.info.cores", accessor.getNBTData().getInteger("gtngModuleTasks")));
    }

    protected String getBaseParallelDescription() {
        // #tr gtng.modular.tooltip.base_parallel
        // # Base parallel limit: %s
        // # zh_CN 基础并行上限：%s
        return StatCollector.translateToLocalFormatted("gtng.modular.tooltip.base_parallel",
            TooltipHelper.parallelText(getBaseParallelRecipes()));
    }

    protected final MultiblockTooltipBuilder addModularTooltip(MultiblockTooltipBuilder tooltip) {
        tooltip.addSeparator();
        // #tr gtng.modular.tooltip.cores
        // # The controller includes {\\YELLOW}1{\\RESET} normal execution core
        // # zh_CN 主机内置 {\\YELLOW}1{\\RESET} 个普通执行核心
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.cores"));
        // #tr gtng.modular.tooltip.tasks
        // # Added cores run independent tasks with shared inputs and outputs
        // # zh_CN 附加执行核心独立处理配方，共享主机的输入与输出
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.tasks"));
        // #tr gtng.modular.tooltip.modules
        // # Accepts all overclock, speed, EU/t and parallel modules
        // # zh_CN 支持全部超频、加速、能耗及并行模块仓
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.modules"));
        tooltip.addInfo(getBaseParallelDescription());
        // #tr gtng.modular.tooltip.parallel
        // # Parallel module: base limit x {\\YELLOW}(1 + parameter){\\RESET}
        // # zh_CN 并行模块：基础上限 × {\\YELLOW}（1＋参数）{\\RESET}
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.parallel"));
        // #tr gtng.modular.tooltip.normal_cores
        // # Locally powered normal cores share parallel capacity and power
        // # zh_CN 本地供电的普通核心分摊并行额度与可用功率
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.normal_cores"));
        // #tr gtng.modular.tooltip.voltage
        // # Local tasks and advanced cores require sufficient energy hatch voltage
        // # zh_CN 本地供电任务及高级核心的配方等级受能源仓电压限制
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.voltage"));
        tooltip.addSeparator();
        // #tr gtng.modular.tooltip.overclock
        // # Default overclock: time /{\\AQUA}2{\\RESET}, EU/t x{\\YELLOW}4{\\RESET}
        // # zh_CN 默认有损超频：耗时 ÷{\\AQUA}2{\\RESET}，EU/t ×{\\YELLOW}4{\\RESET}
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.overclock"));
        // #tr gtng.modular.tooltip.overclock_low
        // # Low-speed overclock: time /{\\AQUA}%s{\\RESET}, EU/t x{\\YELLOW}%s{\\RESET}
        // # zh_CN 低速无损超频仓：耗时 ÷{\\AQUA}%s{\\RESET}，EU/t ×{\\YELLOW}%s{\\RESET}
        tooltip.addInfo(StatCollector.translateToLocalFormatted("gtng.modular.tooltip.overclock_low",
            ParameterModuleHatch.getTimeReduction(1), ParameterModuleHatch.getPowerIncrease(1)));
        // #tr gtng.modular.tooltip.overclock_perfect
        // # Perfect overclock: time /{\\AQUA}%s{\\RESET}, EU/t x{\\YELLOW}%s{\\RESET}
        // # zh_CN 无损超频仓：耗时 ÷{\\AQUA}%s{\\RESET}，EU/t ×{\\YELLOW}%s{\\RESET}
        tooltip.addInfo(StatCollector.translateToLocalFormatted("gtng.modular.tooltip.overclock_perfect",
            ParameterModuleHatch.getTimeReduction(2), ParameterModuleHatch.getPowerIncrease(2)));
        // #tr gtng.modular.tooltip.overclock_singularity
        // # Singularity overclock: time /{\\AQUA}%s{\\RESET}, EU/t x{\\YELLOW}%s{\\RESET}
        // # zh_CN 奇点无损超频仓：耗时 ÷{\\AQUA}%s{\\RESET}，EU/t ×{\\YELLOW}%s{\\RESET}
        tooltip.addInfo(StatCollector.translateToLocalFormatted("gtng.modular.tooltip.overclock_singularity",
            ParameterModuleHatch.getTimeReduction(3), ParameterModuleHatch.getPowerIncrease(3)));
        // #tr gtng.modular.tooltip.changes
        // # Module settings affect new tasks; removing or disabling a core pauses its task
        // # zh_CN 参数修改仅影响新任务；拆下或禁用核心暂停对应任务
        tooltip.addInfo(StatCollector.translateToLocal("gtng.modular.tooltip.changes"));
        return tooltip;
    }

    protected final MultiblockTooltipBuilder addModularStructureTooltip(MultiblockTooltipBuilder tooltip) {
        // #tr gtng.modular.tooltip.parameters
        // # One of each parameter module; execution cores may repeat
        // # zh_CN 每类参数模块仓限一个；执行核心可重复安装
        tooltip.addStructureInfo(StatCollector.translateToLocal("gtng.modular.tooltip.parameters"));
        // #tr gtng.modular.tooltip.location
        // # Module hatches replace casings at hint {\\YELLOW}1{\\RESET}
        // # zh_CN 模块仓可替换提示点 {\\YELLOW}1{\\RESET} 处的外壳
        tooltip.addStructureInfo(StatCollector.translateToLocal("gtng.modular.tooltip.location"));
        // #tr gtng.modular.tooltip.energy_hatches
        // # Requires at least one normal or exotic energy hatch; count is limited by hatch positions
        // # zh_CN 至少需要 1 个普通或特殊能源仓；数量仅受结构舱位限制
        tooltip.addStructureInfo(StatCollector.translateToLocal("gtng.modular.tooltip.energy_hatches"));
        return tooltip;
    }

    private static final class CoreSlot {

        private final String key;
        private final CoreType type;
        private final UUID owner;
        private final CrossRecipeWirelessEnergyHatch wireless;

        private CoreSlot(String key, CoreType type, UUID owner, CrossRecipeWirelessEnergyHatch wireless) {
            this.key = key;
            this.type = type;
            this.owner = owner == null ? new UUID(0, 0) : owner;
            this.wireless = wireless;
        }
    }
}
