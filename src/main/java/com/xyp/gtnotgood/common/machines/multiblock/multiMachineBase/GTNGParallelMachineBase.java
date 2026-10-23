// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.machines.hatch.lighthunter.SatelliteThreadModifierHatch;
import com.xyp.gtnotgood.common.machines.hatch.lighthunter.ThreadModifierHost;
import com.xyp.gtnotgood.common.machines.hatch.parallel.ParallelControlHatch;
import com.xyp.gtnotgood.common.machines.processing.ModuleInputPreflight;
import com.xyp.gtnotgood.common.machines.processing.ParallelBatchProcessingLogic;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;
import com.xyp.gtnotgood.common.wireless.WirelessRecipeDisplay;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.shutdown.ShutDownReason;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public abstract class GTNGParallelMachineBase<T extends GTNGParallelMachineBase<T>> extends GTNGMultiBlockBase<T>
    implements ThreadModifierHost {

    private final List<ParallelControlHatch> parallelHatches = new ArrayList<>();
    private final List<SatelliteThreadModifierHatch> threadHatches = new ArrayList<>();
    private WirelessOutputs batchOutputs, pendingBatch;
    private boolean outputReady, recipeTransactionOpen;
    private long batchParallels;
    private int batchRecipeCount;

    protected GTNGParallelMachineBase(int id, String name, String regional) {
        super(id, name, regional);
    }

    protected GTNGParallelMachineBase(String name) {
        super(name);
    }

    protected abstract int getBaseParallel();

    protected abstract int getBaseThreads();

    public int getThreads() {
        return (int) Math.min(Integer.MAX_VALUE, (long) getBaseThreads()
            + (threadHatches.size() == 1 ? threadHatches.get(0).getAdditionalThreads(this) : 0));
    }

    @Override
    public int getMaxParallelRecipes() {
        return (int) Math.min(Integer.MAX_VALUE,
            (long) getBaseParallel() * (parallelHatches.size() == 1 ? parallelHatches.get(0).getParallel() : 1));
    }

    @Override
    public long getMaxInputVoltage() {
        long voltage = 0;
        for (var hatch : mEnergyHatches)
            if (hatch.isValid()) voltage = Math.max(voltage, hatch.getBaseMetaTileEntity().getInputVoltage());
        return voltage;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ParallelBatchProcessingLogic(this::getTrueParallel, this::getThreads);
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(getMaxInputVoltage()).setAvailableAmperage(1).setAmperageOC(false);
    }

    public boolean addParallelHatch(IGregTechTileEntity tile, int texture) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof ParallelControlHatch hatch)) return false;
        if (!parallelHatches.contains(hatch)) {
            hatch.updateTexture(texture);
            parallelHatches.add(hatch);
        }
        return true;
    }

    public boolean addThreadHatch(IGregTechTileEntity tile, int texture) {
        if (
            tile == null || !(tile.getMetaTileEntity() instanceof SatelliteThreadModifierHatch hatch)
                || !hatch.bind(this)
        ) return false;
        if (!threadHatches.contains(hatch)) {
            hatch.updateTexture(texture);
            threadHatches.add(hatch);
        }
        return true;
    }

    @Override
    public boolean isThreadStructureFormed() {
        return mMachine;
    }

    @Override
    public boolean hasThreadModifier(SatelliteThreadModifierHatch hatch) {
        return threadHatches.contains(hatch);
    }

    @Override
    public void clearHatches() {
        for (var hatch : threadHatches) hatch.unbind(this);
        threadHatches.clear();
        parallelHatches.clear();
        super.clearHatches();
    }

    protected void checkParallelHatches(List<StructureError> errors) {
        int count = Math.max(parallelHatches.size(), threadHatches.size());
        if (count > 1) {
            // #tr gtng.parallel_machine.optional_hatch
            // # Parallel / thread control hatch
            // # zh_CN 并行／线程控制仓
            errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY,
                TranslatableText.lang("gtng.parallel_machine.optional_hatch"), count, 1));
        }
    }

    public long getBatchParallels() {
        return batchParallels;
    }

    public int getBatchRecipeCount() {
        return batchRecipeCount;
    }

    public List<NBTTagCompound> getBatchOutputRows() {
        List<NBTTagCompound> rows = new ArrayList<>();
        if (batchOutputs != null)
            WirelessRecipeDisplay.appendRows(rows, batchOutputs, outputReady ? 0 : mMaxProgresstime, 64);
        return rows;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("parallelMachineThreads", getThreads());
        tag.setInteger("parallelMachineParallel", getMaxParallelRecipes());
        tag.setInteger("parallelMachineUsedThreads",
            batchOutputs != null && !outputReady && mMaxProgresstime > 0 && getBaseMetaTileEntity().isActive()
                ? batchRecipeCount
                : 0);
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey("parallelMachineThreads")) return;
        // #tr gtng.parallel_machine.status
        // # Threads: %s / %s; base parallel: %s
        // # zh_CN 线程：%s / %s；基准并行：%s
        lines.add(StatCollector.translateToLocalFormatted("gtng.parallel_machine.status",
            tag.getInteger("parallelMachineUsedThreads"), tag.getInteger("parallelMachineThreads"),
            tag.getInteger("parallelMachineParallel")));
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
    public CheckRecipeResult checkProcessing() {
        if (batchOutputs != null) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        ParallelBatchProcessingLogic logic = (ParallelBatchProcessingLogic) processingLogic;
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
            if (tick % 20 == 0 && mMaxProgresstime == 0 && tile.isAllowedToWork()) scheduleRecipeCheckImmediate();
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
        tag.setBoolean("parallelMachineOutputReady", outputReady);
        tag.setLong("parallelMachineBatchParallels", batchParallels);
        tag.setInteger("parallelMachineBatchRecipes", batchRecipeCount);
        if (batchOutputs != null) tag.setTag("parallelMachineBatchOutputs", batchOutputs.save());
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        outputReady = tag.getBoolean("parallelMachineOutputReady");
        batchParallels = Math.max(0, tag.getLong("parallelMachineBatchParallels"));
        batchRecipeCount = Math.max(0, tag.getInteger("parallelMachineBatchRecipes"));
        batchOutputs = tag.hasKey("parallelMachineBatchOutputs", 10)
            ? WirelessOutputs.load(tag.getCompoundTag("parallelMachineBatchOutputs"))
            : null;
    }

}
