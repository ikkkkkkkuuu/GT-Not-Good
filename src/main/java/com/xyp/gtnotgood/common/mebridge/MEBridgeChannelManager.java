package com.xyp.gtnotgood.common.mebridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨维度 ME 网桥的全局频道注册表(纯静态,仅服务端使用)。
 * <p>
 * 频道名(可为中文)→ {@link MEBridgeChannelInfo}。全服全局唯一、所有人可见。
 * 仿照 {@code SteamWirelessNetworkManager} 的静态 Map 模式,持久化交给 {@link MEBridgeWorldSavedData}。
 */
public final class MEBridgeChannelManager {

    /** 频道名 → 频道信息。用 LinkedHashMap 保持注册顺序,GUI 列表显示更稳定。 */
    public static final Map<String, MEBridgeChannelInfo> CHANNELS = new LinkedHashMap<>();
    private static final int BROWSER_SNAPSHOT_INTERVAL = 20;
    private static long browserSnapshotTick = Long.MIN_VALUE;
    private static long browserSnapshotReceiverRevision = Long.MIN_VALUE;
    private static long browserSnapshotChannelRevision;
    private static long channelRevision;
    private static String browserSnapshot = "";

    private MEBridgeChannelManager() {}

    /**
     * 注册 / 刷新一个频道。若频道名已存在,覆盖其坐标信息并刷新发起端 tile 引用
     * (区块重新加载时 Sender 会再次调用,以更新弱引用)。
     *
     * @return true 表示注册成功;false 表示频道名被另一个不同坐标的发起端占用(冲突)。
     */
    public static boolean register(MEBridgeChannelInfo info, TileMEBridgeSender tile) {
        if (info == null || info.name == null || info.name.isEmpty() || !MEBridgeChannelName.isValid(info.name)) {
            return false;
        }
        MEBridgeChannelInfo existing = CHANNELS.get(info.name);
        if (existing != null && !sameLocation(existing, info)) {
            // 频道名已被另一个发起端占用
            return false;
        }
        info.setSenderTile(tile);
        CHANNELS.put(info.name, info);
        invalidateBrowserSnapshot();
        MEBridgeWorldSavedData.markDirtyIfPresent();
        return true;
    }

    /**
     * Replaces a sender's registered channel after validating the destination. A conflict leaves the old
     * registration untouched, so existing receivers do not disconnect after a failed rename.
     */
    public static MEBridgeChannelChangeResult replaceSenderChannel(String oldName, MEBridgeChannelInfo newInfo,
        TileMEBridgeSender tile) {
        if (newInfo == null || newInfo.name == null || !MEBridgeChannelName.isValid(newInfo.name)) {
            return MEBridgeChannelChangeResult.INVALID_NAME;
        }

        String newName = newInfo.name;
        MEBridgeChannelInfo existing = newName.isEmpty() ? null : CHANNELS.get(newName);
        if (existing != null && !sameLocation(existing, newInfo)) {
            return MEBridgeChannelChangeResult.CHANNEL_OCCUPIED;
        }

        if (oldName != null && !oldName.equals(newName)) {
            unregister(oldName, newInfo.x, newInfo.y, newInfo.z, newInfo.dim);
        }
        if (!newName.isEmpty()) {
            newInfo.setSenderTile(tile);
            CHANNELS.put(newName, newInfo);
            invalidateBrowserSnapshot();
            MEBridgeWorldSavedData.markDirtyIfPresent();
        }
        return MEBridgeChannelChangeResult.SUCCESS;
    }

    /** 注销频道(发起端被拆除时调用)。只有当前占用该频道名的坐标匹配才移除,避免误删。 */
    public static void unregister(String name, int x, int y, int z, int dim) {
        if (name == null) return;
        MEBridgeChannelInfo info = CHANNELS.get(name);
        if (info == null) return;
        if (info.x == x && info.y == y && info.z == z && info.dim == dim) {
            CHANNELS.remove(name);
            invalidateBrowserSnapshot();
            MEBridgeWorldSavedData.markDirtyIfPresent();
        }
    }

    public static MEBridgeChannelInfo get(String name) {
        return name == null ? null : CHANNELS.get(name);
    }

    public static boolean exists(String name) {
        return name != null && CHANNELS.containsKey(name);
    }

    /** 频道列表快照(GUI 用,避免并发修改)。 */
    public static List<MEBridgeChannelInfo> snapshot() {
        return new ArrayList<>(CHANNELS.values());
    }

    public static List<MEBridgeChannelInfo> snapshotForBrowser() {
        List<MEBridgeChannelInfo> channels = snapshot();
        channels.sort(
            Comparator.comparingInt((MEBridgeChannelInfo channel) -> channel.dim)
                .thenComparing(MEBridgeChannelInfo::isOnline, Comparator.reverseOrder())
                .thenComparing(channel -> channel.name, String.CASE_INSENSITIVE_ORDER));
        return channels;
    }

    public static String browserSnapshot(long worldTick) {
        long receiverRevision = MEBridgeReceiverRegistry.getRevision();
        if (browserSnapshotTick == Long.MIN_VALUE || worldTick < browserSnapshotTick
            || worldTick - browserSnapshotTick >= BROWSER_SNAPSHOT_INTERVAL
            || browserSnapshotChannelRevision != channelRevision
            || browserSnapshotReceiverRevision != receiverRevision) {
            browserSnapshot = MEBridgeChannelListCodec.encode(snapshotForBrowser());
            browserSnapshotTick = worldTick;
            browserSnapshotChannelRevision = channelRevision;
            browserSnapshotReceiverRevision = MEBridgeReceiverRegistry.getRevision();
        }
        return browserSnapshot;
    }

    public static void clear() {
        CHANNELS.clear();
        invalidateBrowserSnapshot();
    }

    /** 供持久化读取后批量填充(不触发发起端 tile 引用,tile 由各自 onReady 时补登记)。 */
    public static void loadEntry(MEBridgeChannelInfo info) {
        if (info == null || info.name == null || info.name.isEmpty() || !MEBridgeChannelName.isValid(info.name)) {
            return;
        }
        CHANNELS.put(info.name, info);
        invalidateBrowserSnapshot();
    }

    public static Map<String, MEBridgeChannelInfo> view() {
        return Collections.unmodifiableMap(CHANNELS);
    }

    private static boolean sameLocation(MEBridgeChannelInfo a, MEBridgeChannelInfo b) {
        return a.x == b.x && a.y == b.y && a.z == b.z && a.dim == b.dim;
    }

    private static void invalidateBrowserSnapshot() {
        channelRevision++;
        browserSnapshotTick = Long.MIN_VALUE;
    }
}
