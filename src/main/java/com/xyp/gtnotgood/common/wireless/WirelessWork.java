package com.xyp.gtnotgood.common.wireless;

import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

/** One independently timed recipe. Paid ticks and outputs travel together in the controller's world NBT. */
public final class WirelessWork {

    public final UUID owner;
    public final String recipeKey;
    public final BigInteger totalEU;
    public final int duration;
    public final BigInteger parallels;
    public final WirelessOutputs outputs;
    private int progress;

    public WirelessWork(UUID owner, String recipeKey, BigInteger totalEU, int duration, int parallels,
        ItemStack[] items, FluidStack[] fluids) {
        this(owner, recipeKey, totalEU, duration, BigInteger.valueOf(parallels), new WirelessOutputs(items, fluids));
    }

    public WirelessWork(UUID owner, String recipeKey, BigInteger totalEU, int duration, BigInteger parallels,
        WirelessOutputs outputs) {
        if (owner == null || totalEU.signum() < 0 || duration < 1 || parallels.signum() <= 0)
            throw new IllegalArgumentException("Invalid wireless recipe task");
        this.owner = owner;
        this.recipeKey = recipeKey;
        this.totalEU = totalEU;
        this.duration = duration;
        this.parallels = parallels;
        this.outputs = outputs;
    }

    public int progress() {
        return progress;
    }

    public boolean finished() {
        return progress == duration;
    }

    /** Divides the exact integer budget across ticks without losing fractional remainders. */
    public BigInteger nextCost() {
        if (finished()) return BigInteger.ZERO;
        BigInteger ticks = BigInteger.valueOf(duration);
        return totalEU.multiply(BigInteger.valueOf(progress + 1L))
            .divide(ticks)
            .subtract(
                totalEU.multiply(BigInteger.valueOf(progress))
                    .divide(ticks));
    }

    public BigInteger remainingCost() {
        return totalEU.subtract(
            totalEU.multiply(BigInteger.valueOf(progress))
                .divide(BigInteger.valueOf(duration)));
    }

    /** A rejected debit leaves both the progress and the remaining budget unchanged. */
    public boolean tick(Predicate<BigInteger> debit) {
        if (finished()) return false;
        BigInteger cost = nextCost();
        if (cost.signum() != 0 && !debit.test(cost)) return false;
        progress++;
        return true;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("owner", owner.toString());
        tag.setString("recipe", recipeKey);
        tag.setString("eu", totalEU.toString());
        tag.setInteger("duration", duration);
        tag.setInteger("progress", progress);
        tag.setString("parallels", parallels.toString());
        tag.setTag("outputs", outputs.save());
        return tag;
    }

    public static WirelessWork load(NBTTagCompound tag) {
        WirelessWork work = new WirelessWork(
            UUID.fromString(tag.getString("owner")),
            tag.getString("recipe"),
            new BigInteger(tag.getString("eu")),
            tag.getInteger("duration"),
            tag.hasKey("parallels", 8) ? new BigInteger(tag.getString("parallels"))
                : BigInteger.valueOf(tag.getInteger("parallels")),
            WirelessOutputs.load(tag.hasKey("outputs", 10) ? tag.getCompoundTag("outputs") : tag));
        work.progress = Math.max(0, Math.min(work.duration, tag.getInteger("progress")));
        return work;
    }
}
