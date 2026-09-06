package com.xyp.gtnotgood.common.machines.crop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.api.ISeedData;
import com.gtnewhorizon.cropsnh.api.ISeedStats;
import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;
import com.gtnewhorizon.cropsnh.utility.CropsNHUtils;

/**
 * Persistent archive of CropsNH crop seeds known by the large crop breeder.
 *
 * <p>
 * The archive stores one best-known analyzed stat line per crop id. Parent seeds are not kept as complete ItemStacks,
 * which keeps controller NBT small while preserving the Growth/Gain/Resistance data needed to derive offspring stats.
 */
public class CropArchive {

    private static final int DATA_VERSION = 1;

    private final Map<String, ArchivedSeed> seeds = new LinkedHashMap<>();

    public boolean addSeed(ItemStack stack) {
        ISeedData seedData = CropsNHUtils.getSeedData(stack, false, true);
        if (seedData == null || seedData.getCrop() == null || seedData.getStats() == null) return false;
        return unlockCrop(seedData.getCrop(), seedData.getStats());
    }

    public boolean unlockCrop(ICropCard crop, ISeedStats stats) {
        if (crop == null || crop.getId() == null
            || crop.getId()
                .isEmpty()
            || stats == null) {
            return false;
        }
        return unlockCrop(crop.getId(), stats);
    }

    public boolean unlockCrop(String cropId, ISeedStats stats) {
        if (cropId == null || cropId.isEmpty() || stats == null || CropRegistry.instance.get(cropId) == null) {
            return false;
        }
        ArchivedSeed previous = seeds.get(cropId);
        ArchivedSeed next = ArchivedSeed.bestOf(previous, stats);
        if (next.equals(previous)) return false;
        seeds.put(cropId, next);
        return true;
    }

    public boolean hasCrop(String cropId) {
        return cropId != null && seeds.containsKey(cropId);
    }

    public Set<String> getAvailableCropIds() {
        return Collections.unmodifiableSet(seeds.keySet());
    }

    public int size() {
        return seeds.size();
    }

    @Nullable
    public ISeedStats getStats(String cropId) {
        ArchivedSeed seed = seeds.get(cropId);
        return seed == null ? null : seed.toSeedStats();
    }

    @Nullable
    public ItemStack createSeed(String cropId) {
        ICropCard crop = CropRegistry.instance.get(cropId);
        ISeedStats stats = getStats(cropId);
        if (crop == null || stats == null) return null;
        ItemStack stack = crop.getSeedItem(stats);
        if (stack != null) stack.stackSize = 1;
        return stack;
    }

    @Nullable
    public ISeedStats averageStats(Iterable<ICropCard> parents) {
        int growth = 0;
        int gain = 0;
        int resistance = 0;
        int count = 0;
        for (ICropCard parent : parents) {
            if (parent == null) return null;
            ISeedStats stats = getStats(parent.getId());
            if (stats == null) return null;
            growth += stats.getGrowth();
            gain += stats.getGain();
            resistance += stats.getResistance();
            count++;
        }
        if (count == 0) return null;
        return new SeedStats((byte) (growth / count), (byte) (gain / count), (byte) (resistance / count), true);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("version", DATA_VERSION);
        NBTTagList cropList = new NBTTagList();
        for (Map.Entry<String, ArchivedSeed> entry : seeds.entrySet()) {
            NBTTagCompound cropTag = new NBTTagCompound();
            cropTag.setString("id", entry.getKey());
            entry.getValue()
                .writeToNBT(cropTag);
            cropList.appendTag(cropTag);
        }
        tag.setTag("crops", cropList);
        return tag;
    }

    public static CropArchive fromNBT(NBTTagCompound tag) {
        CropArchive archive = new CropArchive();
        if (tag == null) return archive;
        NBTTagList cropList = tag.getTagList("crops", 10);
        for (int i = 0; i < cropList.tagCount(); i++) {
            NBTTagCompound cropTag = cropList.getCompoundTagAt(i);
            String cropId = cropTag.getString("id");
            ArchivedSeed seed = ArchivedSeed.fromNBT(cropTag);
            archive.unlockCrop(cropId, seed.toSeedStats());
        }
        return archive;
    }

    private static final class ArchivedSeed {

        private final int growth;
        private final int gain;
        private final int resistance;

        private ArchivedSeed(int growth, int gain, int resistance) {
            this.growth = clampStat(growth);
            this.gain = clampStat(gain);
            this.resistance = clampStat(resistance);
        }

        private static ArchivedSeed bestOf(@Nullable ArchivedSeed previous, ISeedStats stats) {
            if (previous == null) {
                return new ArchivedSeed(stats.getGrowth(), stats.getGain(), stats.getResistance());
            }
            return new ArchivedSeed(
                Math.max(previous.growth, stats.getGrowth()),
                Math.max(previous.gain, stats.getGain()),
                Math.max(previous.resistance, stats.getResistance()));
        }

        private static ArchivedSeed fromNBT(NBTTagCompound tag) {
            return new ArchivedSeed(tag.getInteger("growth"), tag.getInteger("gain"), tag.getInteger("resistance"));
        }

        private void writeToNBT(NBTTagCompound tag) {
            tag.setInteger("growth", growth);
            tag.setInteger("gain", gain);
            tag.setInteger("resistance", resistance);
        }

        private ISeedStats toSeedStats() {
            return new SeedStats((byte) growth, (byte) gain, (byte) resistance, true);
        }

        private static int clampStat(int stat) {
            return Math.max(0, Math.min(31, stat));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ArchivedSeed other)) return false;
            return growth == other.growth && gain == other.gain && resistance == other.resistance;
        }

        @Override
        public int hashCode() {
            int result = growth;
            result = 31 * result + gain;
            result = 31 * result + resistance;
            return result;
        }
    }
}
