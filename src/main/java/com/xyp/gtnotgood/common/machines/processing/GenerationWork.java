// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

public final class GenerationWork {

    public final String core;
    public final CoreType coreType;
    public final UUID owner;
    public final int duration;
    public final int parallels;
    public final BigInteger energy;
    public final WirelessOutputs outputs;
    private int progress;

    public GenerationWork(String core, CoreType type, UUID owner, int duration, int parallels, BigInteger energy,
        WirelessOutputs outputs) {
        if (
            core == null || type == null
                || owner == null
                || duration < 1
                || parallels < 1
                || energy == null
                || energy.signum() <= 0
                || outputs == null
        ) throw new IllegalArgumentException("Invalid generation task");
        this.core = core;
        this.coreType = type;
        this.owner = owner;
        this.duration = duration;
        this.parallels = parallels;
        this.energy = energy;
        this.outputs = outputs;
    }

    public int progress() {
        return progress;
    }

    public boolean finished() {
        return progress == duration;
    }

    public BigInteger remainingEnergy() {
        return energy.subtract(HeartGenerationRules.cumulative(energy, progress, duration));
    }

    public BigInteger nextEnergy() {
        if (finished()) return BigInteger.ZERO;
        return HeartGenerationRules.cumulative(energy, progress + 1, duration)
            .subtract(HeartGenerationRules.cumulative(energy, progress, duration));
    }

    public void advance() {
        if (!finished()) progress++;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("core", core);
        tag.setString("type", coreType.name());
        tag.setString("owner", owner.toString());
        tag.setInteger("duration", duration);
        tag.setInteger("parallels", parallels);
        tag.setString("energy", energy.toString());
        tag.setInteger("progress", progress);
        tag.setTag("outputs", outputs.save());
        return tag;
    }

    public static GenerationWork load(NBTTagCompound tag) {
        GenerationWork work = new GenerationWork(tag.getString("core"), CoreType.valueOf(tag.getString("type")),
            UUID.fromString(tag.getString("owner")), tag.getInteger("duration"), tag.getInteger("parallels"),
            new BigInteger(tag.getString("energy")), WirelessOutputs.load(tag.getCompoundTag("outputs")));
        int progress = tag.getInteger("progress");
        if (progress < 0 || progress > work.duration)
            throw new IllegalArgumentException("Invalid saved generation progress");
        work.progress = progress;
        return work;
    }
}
