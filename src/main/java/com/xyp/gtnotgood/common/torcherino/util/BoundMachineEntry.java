package com.xyp.gtnotgood.common.torcherino.util;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Serializable position record for one machine bound to a wireless Torcherino.
 */
public class BoundMachineEntry {

    public final int x;
    public final int y;
    public final int z;
    public final int dim;
    public int perMachineSpeed;

    /**
     * Creates a bound-machine record with a per-machine speed override.
     *
     * @param x               target X coordinate
     * @param y               target Y coordinate
     * @param z               target Z coordinate
     * @param dim             target dimension id
     * @param perMachineSpeed speed level override, or {@code 0} to use the global wireless speed
     */
    public BoundMachineEntry(int x, int y, int z, int dim, int perMachineSpeed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.perMachineSpeed = perMachineSpeed;
    }

    /**
     * Creates a bound-machine record that uses the wireless torch's global speed.
     *
     * @param x   target X coordinate
     * @param y   target Y coordinate
     * @param z   target Z coordinate
     * @param dim target dimension id
     */
    public BoundMachineEntry(int x, int y, int z, int dim) {
        this(x, y, z, dim, 0);
    }

    /**
     * Resolves a localized name for the target machine without trusting saved NBT display text.
     *
     * @param world current world
     * @return localized machine or block name, or {@code ???} when the target cannot be resolved
     */
    public String getLocalizedName(World world) {
        if (world == null || world.provider == null || world.provider.dimensionId != this.dim) {
            return "???";
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof BaseMetaTileEntity) {
            BaseMetaTileEntity baseMetaTileEntity = (BaseMetaTileEntity) te;
            if (baseMetaTileEntity.getMetaTileEntity() != null) {
                String name = baseMetaTileEntity.getMetaTileEntity()
                    .getLocalName();
                if (name != null && !name.isEmpty()) {
                    String translated = StatCollector.translateToLocal(name);
                    return translated.isEmpty() ? name : translated;
                }
            }
        }

        Block block = world.getBlock(x, y, z);
        if (block != null) {
            String name = block.getLocalizedName();
            if (name != null && !name.isEmpty()) {
                String translated = StatCollector.translateToLocal(name);
                return translated.isEmpty() ? name : translated;
            }
        }

        return "???";
    }

    /**
     * Writes this entry to NBT.
     *
     * @return serialized entry tag
     */
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setInteger("dim", dim);
        tag.setInteger("speed", perMachineSpeed);
        return tag;
    }

    /**
     * Reads a bound machine from NBT, clamping any saved speed override to the current config limit.
     *
     * @param tag serialized entry tag
     * @return restored bound-machine entry
     */
    public static BoundMachineEntry fromNBT(NBTTagCompound tag) {
        int speed = Math.max(0, Math.min(tag.getInteger("speed"), Config.torcherinoMaxSpeedLevel));
        return new BoundMachineEntry(
            tag.getInteger("x"),
            tag.getInteger("y"),
            tag.getInteger("z"),
            tag.getInteger("dim"),
            speed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BoundMachineEntry)) return false;
        BoundMachineEntry other = (BoundMachineEntry) obj;
        return x == other.x && y == other.y && z == other.z && dim == other.dim;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        result = 31 * result + dim;
        return result;
    }

    @Override
    public String toString() {
        return "BoundMachine{" + x + ", " + y + ", " + z + ", dim=" + dim + ", speed=" + perMachineSpeed + "}";
    }
}
