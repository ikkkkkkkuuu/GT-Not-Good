package com.xyp.gtnotgood.common.items.compass;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import com.xyp.gtnotgood.utils.enums.ModList;

/** Per-dimension discovery index. Entries are historical locations, not promises that loot remains. */
public final class StructureLocations extends WorldSavedData {

    private static final String KEY = ModList.GTNotGood.getID() + "_structures";
    private final List<Location> locations = new ArrayList<>();

    public StructureLocations(String name) {
        super(name);
    }

    public static StructureLocations get(World world) {
        StructureLocations data = (StructureLocations) world.perWorldStorage.loadData(StructureLocations.class, KEY);
        if (data == null) {
            data = new StructureLocations(KEY);
            world.perWorldStorage.setData(KEY, data);
        }
        return data;
    }

    /** Merges the old-map chest marker with a generation-time entrance within the same house. */
    public void add(int kind, int x, int y, int z) {
        for (Location p : locations) {
            if (p.kind == kind && Math.abs((long) p.x - x) <= 16 && Math.abs((long) p.z - z) <= 16) return;
        }
        locations.add(new Location(kind, x, y, z));
        markDirty();
    }

    public Location nearest(int kind, double x, double z, double radius) {
        Location best = null;
        double distance = radius * radius;
        for (Location p : locations) {
            double d = p.distanceSquared(x, z);
            if (p.kind == kind && d <= distance) {
                best = p;
                distance = d;
            }
        }
        return best;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        locations.clear();
        NBTTagList list = nbt.getTagList("Locations", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int kind = tag.getInteger("Kind");
            if (kind < 0 || kind > 1) continue;
            locations.add(new Location(kind, tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z")));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Location p : locations) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Kind", p.kind);
            tag.setInteger("X", p.x);
            tag.setInteger("Y", p.y);
            tag.setInteger("Z", p.z);
            list.appendTag(tag);
        }
        nbt.setTag("Locations", list);
    }

    /** Immutable position whose dimension is provided by its owning world index. */
    public static final class Location {

        public final int kind, x, y, z;

        public Location(int kind, int x, int y, int z) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double distanceSquared(double originX, double originZ) {
            double dx = x + .5 - originX, dz = z + .5 - originZ;
            return dx * dx + dz * dz;
        }
    }
}
