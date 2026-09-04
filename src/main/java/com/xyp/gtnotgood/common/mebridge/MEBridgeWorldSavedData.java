package com.xyp.gtnotgood.common.mebridge;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import com.xyp.gtnotgood.GTNotGood;

/**
 * 跨维度 ME 网桥频道注册表的持久化。
 * <p>
 * Uses a static singleton plus {@code sLoaded} to avoid repeated loading, and clears before entering a new save.
 * 逐条用 NBTTagList 写(坐标/维度是 int,owner 是字符串),比整表 Java 序列化更稳。
 */
public class MEBridgeWorldSavedData extends WorldSavedData {

    public static MEBridgeWorldSavedData INSTANCE;
    private static boolean sLoaded = false;

    public static final String DATA_NAME = "GTNG_MEBridgeChannelData";
    private static final String KEY_LIST = "channels";

    public MEBridgeWorldSavedData() {
        super(DATA_NAME);
    }

    public MEBridgeWorldSavedData(String name) {
        super(name);
    }

    public static void loadInstance(World world) {
        if (sLoaded) return;
        MEBridgeChannelManager.clear();
        MapStorage storage = world.mapStorage;
        INSTANCE = (MEBridgeWorldSavedData) storage.loadData(MEBridgeWorldSavedData.class, DATA_NAME);
        if (INSTANCE == null) {
            INSTANCE = new MEBridgeWorldSavedData();
            storage.setData(DATA_NAME, INSTANCE);
        }
        sLoaded = true;
    }

    /** 服务器关闭 / 主世界卸载时复位,防止单机切换存档残留。 */
    public static void reset() {
        MEBridgeChannelManager.clear();
        INSTANCE = null;
        sLoaded = false;
    }

    public static void markDirtyIfPresent() {
        if (INSTANCE != null) INSTANCE.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        MEBridgeChannelManager.clear();
        NBTTagList list = nbt.getTagList(KEY_LIST, 10); // 10 = NBTTagCompound
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            try {
                String name = tag.getString("name");
                if (name.isEmpty()) continue;
                int x = tag.getInteger("x");
                int y = tag.getInteger("y");
                int z = tag.getInteger("z");
                int dim = tag.getInteger("dim");
                int color = tag.hasKey("color") ? tag.getInteger("color") : MEBridgeChannelColor.defaultFor(name);
                UUID owner = null;
                if (tag.hasKey("owner")) {
                    try {
                        owner = UUID.fromString(tag.getString("owner"));
                    } catch (RuntimeException ignored) {}
                }
                MEBridgeChannelManager.loadEntry(new MEBridgeChannelInfo(name, x, y, z, dim, color, owner));
            } catch (RuntimeException e) {
                GTNotGood.LOG.warn("[MEBridge] Skipping invalid channel entry {}", i);
            }
        }

    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (MEBridgeChannelInfo info : MEBridgeChannelManager.snapshot()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("name", info.name);
            tag.setInteger("x", info.x);
            tag.setInteger("y", info.y);
            tag.setInteger("z", info.z);
            tag.setInteger("dim", info.dim);
            tag.setInteger("color", info.color);
            if (info.owner != null) tag.setString("owner", info.owner.toString());
            list.appendTag(tag);
        }
        nbt.setTag(KEY_LIST, list);

    }
}
