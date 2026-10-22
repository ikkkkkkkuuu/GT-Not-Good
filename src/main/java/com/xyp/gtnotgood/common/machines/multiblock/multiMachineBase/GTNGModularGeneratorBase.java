// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModularHatch;
import com.xyp.gtnotgood.common.machines.hatch.modular.ModuleType;
import com.xyp.gtnotgood.common.machines.hatch.modular.ParameterModuleHatch;
import com.xyp.gtnotgood.common.machines.processing.GenerationWork;
import com.xyp.gtnotgood.common.machines.processing.HeartGenerationRules;
import com.xyp.gtnotgood.common.machines.processing.ModularProcessingLogic;
import com.xyp.gtnotgood.common.machines.processing.ModuleInputPreflight;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.shutdown.ShutDownReason;
import gregtech.common.misc.GlobalEnergyWorldSavedData;
import gregtech.common.misc.WirelessNetworkManager;

public abstract class GTNGModularGeneratorBase<T extends GTNGModularGeneratorBase<T>> extends GTNGMultiBlockBase<T> {

    private final List<ModularHatch> modules = new ArrayList<>();
    private final List<GenerationWork> works = new ArrayList<>();
    private CoreSlot preparingCore;
    private GenerationWork pendingWork;
    private boolean transactionOpen;
    private boolean paused;
    private int nextCoreCursor;
    private BigInteger generatedLastTick = BigInteger.ZERO;

    protected GTNGModularGeneratorBase(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    protected GTNGModularGeneratorBase(String name) {
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
    protected ProcessingLogic createProcessingLogic() {
        return new ModularProcessingLogic().setMaxParallelSupplier(this::getCoreParallel).setMaxTierSkips(0);
    }

    public final boolean addModularHatchToMachineList(IGregTechTileEntity tile, int casingIndex) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ModularHatch module)) return false;
        if (!modules.contains(module)) {
            module.updateTexture(casingIndex);
            module.updateCraftingIcon(getMachineCraftingIcon());
            modules.add(module);
        }
        return true;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        for (ModularHatch module : modules) {
            module.setWorking(false);
            if (module instanceof ExecutionCoreHatch core) core.setGenerationInfo(null, true);
        }
        modules.clear();
    }

    protected final void checkModularHatches(List<StructureError> errors) {
        Map<ModuleType, Integer> counts = new EnumMap<>(ModuleType.class);
        for (ModularHatch module : modules) counts.merge(module.getModuleType(), 1, Integer::sum);
        for (var entry : counts.entrySet()) if (entry.getKey() != ModuleType.ExecutionCore && entry.getValue() > 1) {
            // #tr gtng.heart.parameter_module
            // # Generator parameter module
            // # zh_CN 发电参数模块仓
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY,
                TranslatableText.lang("gtng.heart.parameter_module"), entry.getValue(), 1));
        }
    }

    private ParameterModuleHatch parameter(ModuleType type) {
        for (ModularHatch module : modules)
            if (module.getModuleType() == type && module instanceof ParameterModuleHatch value) return value;
        return null;
    }

    @Override
    public int getMaxParallelRecipes() {
        ParameterModuleHatch value = parameter(ModuleType.Parallel);
        return value == null ? 1 : (int) Math.min(Integer.MAX_VALUE, 1L + value.getParameter());
    }

    private int getCoreParallel() {
        int parallels = getTrueParallel();
        if (preparingCore == null || preparingCore.type != CoreType.Normal) return parallels;
        int count = 0;
        int index = 0;
        for (CoreSlot slot : slots()) if (slot.type == CoreType.Normal) {
            if (slot.key.equals(preparingCore.key)) index = count;
            count++;
        }
        return parallels / count + (index < parallels % count ? 1 : 0);
    }

    public final long getMaintenanceEUt() {
        ParameterModuleHatch value = parameter(ModuleType.PowerConsumption);
        return HeartGenerationRules.maintenance(value == null ? 1 : value.getPowerMultiplier());
    }

    private int getGenerationDuration(CoreType type) {
        ParameterModuleHatch speed = parameter(ModuleType.Speed);
        ParameterModuleHatch overclock = parameter(ModuleType.Overclock);
        int overclockRate = overclock == null ? 1 : overclock.getTimeReduction() * 4 / overclock.getPowerIncrease();
        return HeartGenerationRules.duration(type, speed == null ? 1 : speed.getParameter(), overclockRate);
    }

    @Override
    protected void setupProcessingLogic(ProcessingLogic logic) {
        super.setupProcessingLogic(logic);
        logic.setAvailableVoltage(Long.MAX_VALUE).setAvailableAmperage(1).setAmperageOC(false);
        ((ModularProcessingLogic) logic).prepareModuleRecipe(Long.MAX_VALUE, true);
    }

    @Override
    public final CheckRecipeResult checkProcessing() {
        if (!(processingLogic instanceof ModularProcessingLogic logic)) return CheckRecipeResultRegistry.INTERNAL_ERROR;
        CoreSlot slot = preparingCore == null ? slots().get(0) : preparingCore;
        if (getCoreParallel() < 1) return CheckRecipeResultRegistry.NO_RECIPE;
        if (findWork(slot.key) != null || GlobalEnergyWorldSavedData.INSTANCE == null)
            return CheckRecipeResultRegistry.NO_RECIPE;
        if (getMaxInputEu() < getMaintenanceEUt())
            return CheckRecipeResultRegistry.insufficientPower(getMaintenanceEUt());
        try {
            setupProcessingLogic(logic);
            CheckRecipeResult result = postCheckRecipe(doCheckRecipe(), logic);
            if (!result.wasSuccessful()) return result;
            if (!logic.canCommitInputs() || !ModuleInputPreflight.canCommit(this, logic))
                return CheckRecipeResultRegistry.NO_RECIPE;
            int parallels = Math.max(1, logic.getCurrentParallels());
            WirelessOutputs outputs = new WirelessOutputs(logic.getOutputItems(), logic.getOutputFluids());
            GenerationWork work = new GenerationWork(slot.key, slot.type, slot.owner, getGenerationDuration(slot.type),
                parallels, HeartGenerationRules.energy(parallels), outputs);
            if (!logic.commitInputs()) return CheckRecipeResultRegistry.INTERNAL_ERROR;
            works.add(work);
            if (transactionOpen) pendingWork = work;
            updateSlots();
            markDirty();
            return CheckRecipeResultRegistry.SUCCESSFUL;
        } finally {
            logic.finishModuleRecipe();
        }
    }

    private List<CoreSlot> slots() {
        List<CoreSlot> result = new ArrayList<>();
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        UUID owner = tile == null ? new UUID(0, 0) : tile.getOwnerUuid();
        result.add(new CoreSlot("controller", CoreType.Normal, owner));
        for (ModularHatch module : modules) if (module instanceof ExecutionCoreHatch core && core.isValid()) {
            IGregTechTileEntity coreTile = core.getBaseMetaTileEntity();
            if (coreTile.isAllowedToWork()) result.add(new CoreSlot(coreKey(core), core.getCoreType(), owner));
        }
        return result;
    }

    private static String coreKey(ExecutionCoreHatch core) {
        IGregTechTileEntity tile = core.getBaseMetaTileEntity();
        return core.getCoreType().name() + ":" + tile.getXCoord() + ":" + tile.getYCoord() + ":" + tile.getZCoord();
    }

    private GenerationWork findWork(String key) {
        for (GenerationWork work : works) if (work.core.equals(key)) return work;
        return null;
    }

    private boolean bound(GenerationWork work, Map<String, CoreSlot> slots) {
        CoreSlot slot = slots.get(work.core);
        return slot != null && slot.type == work.coreType && slot.owner.equals(work.owner);
    }

    @Override
    protected void runMachine(IGregTechTileEntity tile, long tick) {
        generatedLastTick = BigInteger.ZERO;
        List<CoreSlot> slots = slots();
        Map<String, CoreSlot> bound = new HashMap<>();
        for (CoreSlot slot : slots) bound.put(slot.key, slot);
        paused = !tile.isAllowedToWork() || GlobalEnergyWorldSavedData.INSTANCE == null;
        List<GenerationWork> running = new ArrayList<>();
        for (GenerationWork work : works) if (!work.finished() && bound(work, bound)) running.add(work);
        if (!paused && !running.isEmpty()) {
            long maintenance = getMaintenanceEUt();
            paused = getMaxInputEu() < maintenance || !doRandomMaintenanceDamage() || !drainEnergyInput(maintenance);
            if (!paused) for (GenerationWork work : running) {
                BigInteger energy = work.nextEnergy();
                if (WirelessNetworkManager.addEUToGlobalEnergyMap(work.owner, energy)) {
                    work.advance();
                    generatedLastTick = generatedLastTick.add(energy);
                    markDirty();
                } else paused = true;
            }
        }
        boolean completed = false;
        if (tile.isAllowedToWork()) for (Iterator<GenerationWork> iterator = works.iterator(); iterator.hasNext();) {
            GenerationWork work = iterator.next();
            if (work.finished() && bound(work, bound) && work.outputs.flush(this)) {
                iterator.remove();
                recipesDone += work.parallels;
                outputAfterRecipe();
                completed = true;
                markDirty();
            }
        }
        if (
            !paused && (completed || tick % 5 == 0 || tile.hasInventoryBeenModified() || tile.hasWorkJustBeenEnabled())
        ) {
            int attempts = 0;
            int start = Math.floorMod(nextCoreCursor, slots.size());
            for (int offset = 0; offset < slots.size(); offset++) {
                int index = (start + offset) % slots.size();
                CoreSlot slot = slots.get(index);
                if (findWork(slot.key) != null) continue;
                preparingCore = slot;
                if (getCoreParallel() < 1) {
                    preparingCore = null;
                    continue;
                }
                nextCoreCursor = (index + 1) % slots.size();
                startRecipeProcessing();
                try {
                    setCheckRecipeResult(checkProcessing());
                } finally {
                    endRecipeProcessing();
                    preparingCore = null;
                }
                if (++attempts >= 8) break;
            }
        }
        displayWork();
    }

    private void displayWork() {
        mEfficiency = 10000;
        mEfficiencyIncrease = 10000;
        Map<String, CoreSlot> bound = new HashMap<>();
        for (CoreSlot slot : slots()) bound.put(slot.key, slot);
        mOutputItems = null;
        mOutputFluids = null;
        mEUt = 0;
        lEUt = generatedLastTick.signum() > 0 ? -getMaintenanceEUt() : 0;
        mProgresstime = 0;
        mMaxProgresstime = 0;
        if (!paused) for (GenerationWork work : works) if (!work.finished() && bound(work, bound)) {
            mProgresstime = work.progress();
            mMaxProgresstime = work.duration;
            break;
        }
        for (ModularHatch module : modules) {
            boolean working = false;
            if (module instanceof ExecutionCoreHatch core) {
                GenerationWork work = findWork(coreKey(core));
                working = !paused && work != null && !work.finished() && bound(work, bound);
                core.setGenerationInfo(work, !working);
            } else working = !paused && mMaxProgresstime > 0;
            module.setWorking(working);
        }
    }

    @Override
    public void startRecipeProcessing() {
        super.startRecipeProcessing();
        transactionOpen = true;
        pendingWork = null;
    }

    @Override
    public void endRecipeProcessing() {
        try {
            super.endRecipeProcessing();
            if (pendingWork != null && !getCheckRecipeResult().wasSuccessful()) works.remove(pendingWork);
        } finally {
            pendingWork = null;
            transactionOpen = false;
        }
    }

    @Override
    public void stopMachine(ShutDownReason reason) {
        super.stopMachine(reason);
        paused = true;
        generatedLastTick = BigInteger.ZERO;
        displayWork();
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        NBTTagList tasks = new NBTTagList();
        for (GenerationWork work : works) tasks.appendTag(work.save());
        tag.setTag("gtngGenerationWork", tasks);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        works.clear();
        NBTTagList tasks = tag.getTagList("gtngGenerationWork", 10);
        for (int i = 0; i < tasks.tagCount(); i++) works.add(GenerationWork.load(tasks.getCompoundTagAt(i)));
        paused = true;
        generatedLastTick = BigInteger.ZERO;
        mMaxProgresstime = 0;
    }

    public final List<GenerationWork> getGenerationWorks() {
        return Collections.unmodifiableList(works);
    }

    public final BigInteger getGeneratedLastTick() {
        return generatedLastTick;
    }

    @Override
    public String[] getInfoData() {
        String[] base = super.getInfoData();
        String[] result = Arrays.copyOf(base, base.length + 2);
        // #tr gtng.heart.info.generation
        // # Wireless generation: %s EU/t
        // # zh_CN 无线发电：%s EU/t
        result[base.length] = StatCollector.translateToLocalFormatted("gtng.heart.info.generation",
            NumberFormatUtil.formatNumber(generatedLastTick));
        // #tr gtng.heart.info.maintenance
        // # Maintenance demand: %s EU/t; fuel tasks: %s
        // # zh_CN 维持耗电：%s EU/t；燃料任务：%s
        result[base.length + 1] = StatCollector.translateToLocalFormatted("gtng.heart.info.maintenance",
            NumberFormatUtil.formatNumber(getMaintenanceEUt()), works.size());
        return result;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    private static final class CoreSlot {

        private final String key;
        private final CoreType type;
        private final UUID owner;

        private CoreSlot(String key, CoreType type, UUID owner) {
            this.key = key;
            this.type = type;
            this.owner = owner;
        }
    }
}
