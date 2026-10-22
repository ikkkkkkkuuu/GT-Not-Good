// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

/** The controller is the only persistence owner, including when an execution core is removed. */
public final class ModuleWork {

    public final String core;
    public final CoreType coreType;
    public final UUID owner;
    public final long eut;
    public final long computation;
    public final int duration;
    public final int parallels;
    public final boolean controllerPower;
    public final BigInteger prepaidEU;
    public final WirelessOutputs outputs;
    public final int timeReduction;
    public final int powerIncrease;
    private int progress;

    public ModuleWork(String core, CoreType coreType, UUID owner, long eut, long computation, int duration,
        int parallels, boolean controllerPower, BigInteger prepaidEU, WirelessOutputs outputs) {
        this(core, coreType, owner, eut, computation, duration, parallels, controllerPower, prepaidEU, outputs, 2, 4);
    }

    public ModuleWork(String core, CoreType coreType, UUID owner, long eut, long computation, int duration,
        int parallels, boolean controllerPower, BigInteger prepaidEU, WirelessOutputs outputs, int timeReduction,
        int powerIncrease) {
        if (
            core == null || coreType == null
                || owner == null
                || eut < 0
                || computation < 0
                || duration < 1
                || parallels < 1
                || prepaidEU.signum() < 0
                || outputs == null
                || timeReduction < 2
                || powerIncrease < 2
        ) throw new IllegalArgumentException("Invalid module execution");
        this.core = core;
        this.coreType = coreType;
        this.owner = owner;
        this.eut = eut;
        this.computation = computation;
        this.duration = duration;
        this.parallels = parallels;
        this.controllerPower = controllerPower;
        this.prepaidEU = prepaidEU;
        this.outputs = outputs;
        this.timeReduction = timeReduction;
        this.powerIncrease = powerIncrease;
    }

    public int progress() {
        return progress;
    }

    public int remaining() {
        return duration - progress;
    }

    public boolean finished() {
        return progress == duration;
    }

    public void advance(int ticks) {
        if (ticks > 0) progress += Math.min(ticks, remaining());
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("core", core);
        tag.setString("type", coreType.name());
        tag.setString("owner", owner.toString());
        tag.setLong("eut", eut);
        tag.setLong("computation", computation);
        tag.setInteger("duration", duration);
        tag.setInteger("progress", progress);
        tag.setInteger("parallels", parallels);
        tag.setBoolean("controllerPower", controllerPower);
        tag.setString("prepaidEU", prepaidEU.toString());
        tag.setTag("outputs", outputs.save());
        tag.setInteger("timeReduction", timeReduction);
        tag.setInteger("powerIncrease", powerIncrease);
        return tag;
    }

    public static ModuleWork load(NBTTagCompound tag) {
        ModuleWork work = new ModuleWork(tag.getString("core"), CoreType.valueOf(tag.getString("type")),
            UUID.fromString(tag.getString("owner")), tag.getLong("eut"), tag.getLong("computation"),
            tag.getInteger("duration"), tag.getInteger("parallels"), tag.getBoolean("controllerPower"),
            new BigInteger(tag.getString("prepaidEU")), WirelessOutputs.load(tag.getCompoundTag("outputs")),
            tag.hasKey("timeReduction") ? tag.getInteger("timeReduction") : 2,
            tag.hasKey("powerIncrease") ? tag.getInteger("powerIncrease") : 4);
        work.progress = Math.max(0, Math.min(work.duration, tag.getInteger("progress")));
        return work;
    }
}
