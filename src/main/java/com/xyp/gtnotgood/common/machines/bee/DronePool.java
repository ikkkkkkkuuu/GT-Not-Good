package com.xyp.gtnotgood.common.machines.bee;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Persistent archive of bee species unlocked by the breeder.
 * <p>
 * The breeder never consumes parent drones after a species has been registered, so retaining complete bee
 * {@link ItemStack}s only inflated machine NBT and made the old "drone count" misleading. The archive therefore stores
 * one canonical Forestry UID per unlocked species.
 */
public class DronePool {

    private static final int DATA_VERSION = 2;

    private final Set<String> unlockedSpecies = new LinkedHashSet<>();

    /**
     * Registers the primary species carried by a drone.
     *
     * @return {@code true} when this call unlocked a new species
     */
    public boolean addDrone(ItemStack droneStack) {
        if (droneStack == null || !BeeBreedingHelper.isDrone(droneStack)) return false;
        return unlockSpecies(BeeBreedingHelper.getBeeUID(droneStack));
    }

    /**
     * Registers a known Forestry species UID.
     *
     * @return {@code true} when this call unlocked a new species
     */
    public boolean unlockSpecies(String species) {
        if (species == null || species.isEmpty()) return false;
        String canonicalUID = BeeBreedingHelper.getCanonicalUID(species);
        if (canonicalUID == null || canonicalUID.isEmpty()) return false;
        return unlockedSpecies.add(canonicalUID);
    }

    public boolean hasDrone(String species) {
        if (species == null || species.isEmpty()) return false;
        return unlockedSpecies.contains(species);
    }

    /**
     * Kept for source compatibility with older callers. An archived species is a capability, not an item count.
     */
    public int getDroneCount(String species) {
        return hasDrone(species) ? 1 : 0;
    }

    public Set<String> getAvailableSpecies() {
        return Collections.unmodifiableSet(unlockedSpecies);
    }

    public void clear() {
        unlockedSpecies.clear();
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("version", DATA_VERSION);

        NBTTagList speciesList = new NBTTagList();
        for (String uid : unlockedSpecies) {
            NBTTagCompound speciesTag = new NBTTagCompound();
            speciesTag.setString("uid", uid);
            speciesList.appendTag(speciesTag);
        }
        tag.setTag("unlockedSpecies", speciesList);
        return tag;
    }

    public static DronePool fromNBT(NBTTagCompound tag) {
        DronePool pool = new DronePool();
        if (tag == null) return pool;

        if (tag.hasKey("unlockedSpecies", 9)) {
            NBTTagList speciesList = tag.getTagList("unlockedSpecies", 10);
            for (int i = 0; i < speciesList.tagCount(); i++) {
                pool.unlockSpecies(
                    speciesList.getCompoundTagAt(i)
                        .getString("uid"));
            }
            return pool;
        }

        // Version 1 migration: each species entry held a list of complete drone ItemStacks.
        NBTTagList oldInventory = tag.getTagList("inventory", 10);
        for (int i = 0; i < oldInventory.tagCount(); i++) {
            pool.unlockSpecies(
                oldInventory.getCompoundTagAt(i)
                    .getString("species"));
        }
        return pool;
    }
}
