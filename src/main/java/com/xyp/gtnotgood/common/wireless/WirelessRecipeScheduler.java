package com.xyp.gtnotgood.common.wireless;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.common.misc.GlobalEnergyWorldSavedData;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * Independent, bounded recipe lanes for one controller. No EU is prefetched and each task retains its original
 * payer. Missing hatches, insufficient EU and disabled controllers pause work without consuming more inputs.
 */
public final class WirelessRecipeScheduler {

    private final List<WirelessWork> tasks = new ArrayList<>();
    private long lastTick = Long.MIN_VALUE;
    private int status;
    private BigInteger displayedEU = BigInteger.ZERO;

    public BigInteger displayedEU() {
        return displayedEU;
    }

    public List<NBTTagCompound> displayedOutputs(int limit) {
        return WirelessRecipeDisplay.rows(tasks, limit);
    }

    public int size() {
        return tasks.size();
    }

    public int status() {
        return status;
    }

    public BigInteger parallelCount() {
        BigInteger total = BigInteger.ZERO;
        for (WirelessWork work : tasks) total = total.add(work.parallels);
        return total;
    }

    boolean contains(String key) {
        for (WirelessWork work : tasks) if (work.recipeKey.equals(key)) return true;
        return false;
    }

    public static CrossRecipeWirelessEnergyHatch find(MTEMultiBlockBase machine) {
        for (MTEHatchEnergy hatch : machine.mEnergyHatches) {
            if (hatch instanceof CrossRecipeWirelessEnergyHatch wireless && wireless.isValid()) return wireless;
        }
        return null;
    }

    /**
     * Called from each supported native runMachine entry after its structure/maintenance checks.
     * 
     * @return true when the wireless scheduler owns this tick; false leaves the original loop untouched
     */
    public boolean tick(MTEMultiBlockBase machine, IGregTechTileEntity tile, long tick) {
        CrossRecipeWirelessEnergyHatch hatch = find(machine);
        if (hatch == null && tasks.isEmpty()) return false;
        if (!tile.isServerSide()) return false;
        if (lastTick == tick) return true;
        lastTick = tick;
        displayedEU = BigInteger.ZERO;
        WirelessControllerAccess access = (WirelessControllerAccess) machine;
        if (hatch == null || GlobalEnergyWorldSavedData.INSTANCE == null) {
            status = 2;
            display(machine);
            return true;
        }
        if (!WirelessCompatibility.supports(machine, access.gtng$getProcessingLogic())) {
            status = 3;
            machine.setCheckRecipeResult(CheckRecipeResultRegistry.NO_RECIPE);
            return true;
        }
        // A recipe started before this hatch was installed must finish through its original lifecycle.
        if (tasks.isEmpty() && machine.mMaxProgresstime > 0) return false;
        if (!tile.isAllowedToWork()) {
            display(machine);
            return true;
        }
        status = 0;
        UUID owner = hatch.getBaseMetaTileEntity()
            .getOwnerUuid();
        if (!tasks.isEmpty()) {
            display(machine);
            boolean running = false;
            for (WirelessWork work : tasks) if (!work.finished() && work.owner.equals(owner)) running = true;
            if (running) {
                if (!access.gtng$maintenance() || !machine.onRunningTick(machine.getControllerSlot())) return true;
                if (!machine.polluteEnvironment(machine.getPollutionPerTick(machine.getControllerSlot()))) return true;
            }
            for (Iterator<WirelessWork> it = tasks.iterator(); it.hasNext();) {
                WirelessWork work = it.next();
                if (!work.owner.equals(owner)) {
                    status = 2;
                    continue;
                }
                if (!work.finished() && !work.tick(cost -> {
                    boolean paid = WirelessNetworkManager.addEUToGlobalEnergyMap(work.owner, cost.negate());
                    if (paid) displayedEU = displayedEU.add(cost);
                    return paid;
                })) {
                    status = 1;
                    continue;
                }
                machine.markDirty();
                if (work.finished()) {
                    if (!work.outputs.flush(machine)) {
                        status = 4;
                        continue;
                    }
                    it.remove();
                    access.gtng$outputAfterRecipe();
                }
            }
        }
        // A bounded search burst avoids rescanning every recipe map every tick while allowing staggered starts.
        if (tasks.size() < hatch.getTaskLimit() && (tick % 5 == 0 || tile.hasInventoryBeenModified())) {
            int attempts = Math.min(8, hatch.getTaskLimit() - tasks.size());
            for (int i = 0; i < attempts; i++) {
                BigInteger budget = WirelessNetworkManager.getUserEU(owner);
                for (WirelessWork work : tasks)
                    if (work.owner.equals(owner)) budget = budget.subtract(work.remainingCost());
                WirelessRecipeAttempt attempt = new WirelessRecipeAttempt(machine, this, hatch, budget);
                clearNativeRecipe(machine);
                WirelessRecipeAttempt.enter(attempt);
                boolean accepted;
                try {
                    accepted = access.gtng$checkRecipe();
                } finally {
                    WirelessRecipeAttempt.leave();
                }
                if (!accepted || !attempt.committed || attempt.prepared == null) break;
                tasks.add(attempt.prepared);
                machine.markDirty();
            }
        }
        display(machine);
        if (!tasks.isEmpty() && status == 0) machine.setCheckRecipeResult(CheckRecipeResultRegistry.SUCCESSFUL);
        return true;
    }

    private static void clearNativeRecipe(MTEMultiBlockBase machine) {
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.mEUt = 0;
        if (machine instanceof MTEExtendedPowerMultiBlockBase<?>extended) extended.lEUt = 0;
        machine.mMaxProgresstime = 0;
        machine.mProgresstime = 0;
    }

    private void display(MTEMultiBlockBase machine) {
        clearNativeRecipe(machine);
        if (tasks.isEmpty()) return;
        WirelessWork first = tasks.get(0);
        machine.mMaxProgresstime = first.duration;
        machine.mProgresstime = first.progress();
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (WirelessWork work : tasks) list.appendTag(work.save());
        tag.setTag("tasks", list);
        return tag;
    }

    public void load(NBTTagCompound tag) {
        tasks.clear();
        lastTick = Long.MIN_VALUE;
        displayedEU = BigInteger.ZERO;
        NBTTagList list = tag.getTagList("tasks", 10);
        for (int i = 0; i < list.tagCount(); i++) tasks.add(WirelessWork.load(list.getCompoundTagAt(i)));
    }
}
