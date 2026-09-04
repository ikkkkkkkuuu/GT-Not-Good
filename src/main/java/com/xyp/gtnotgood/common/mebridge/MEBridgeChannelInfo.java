package com.xyp.gtnotgood.common.mebridge;

import java.lang.ref.WeakReference;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 一个跨维度 ME 网桥频道的信息。
 * <p>
 * 由发起端(Sender)方块注册到 {@link MEBridgeChannelManager}。频道名为玩家写入的字符串(支持中文)。
 * 记录发起端方块的世界坐标 + 维度 + owner,供接收端(Receiver)列表显示与建立连接使用。
 * {@link #senderTile} 是对发起端 tile 的弱引用,仅在其所在区块加载时有效,用于取 {@code IGridNode} 建立连接。
 */
public class MEBridgeChannelInfo {

    /** 频道名(玩家写入,可为中文)。同时作为全局注册表的 key。 */
    public final String name;
    public final int x;
    public final int y;
    public final int z;
    public final int dim;
    /** RGB colour chosen by the sender for visualising every wireless node on this channel. */
    public final int color;
    /** 发起端 owner UUID(可能为 null)。 */
    public final UUID owner;

    /** 对发起端 tile 的弱引用;区块卸载后为 null,重新加载时由 Sender 重新注册刷新。 */
    private WeakReference<TileMEBridgeSender> senderTile = new WeakReference<>(null);

    public MEBridgeChannelInfo(String name, int x, int y, int z, int dim, int color, UUID owner) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.color = MEBridgeChannelColor.sanitize(color);
        this.owner = owner;
    }

    public void setSenderTile(TileMEBridgeSender tile) {
        this.senderTile = new WeakReference<>(tile);
    }

    /** 取发起端 tile;若区块未加载 / tile 已失效则返回 null。 */
    public TileMEBridgeSender getSenderTile() {
        TileMEBridgeSender tile = senderTile.get();
        if (tile == null || tile.isInvalid()) return null;
        return tile;
    }

    /** 发起端当前是否在线(区块已加载 + tile 有效 + 已接入 AE 网络)。 */
    public boolean isOnline() {
        TileMEBridgeSender tile = getSenderTile();
        return tile != null && tile.isInNetwork();
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", name);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setInteger("dim", dim);
        tag.setInteger("color", color);
        if (owner != null) tag.setString("owner", owner.toString());
        return tag;
    }

    public static MEBridgeChannelInfo readFromNBT(NBTTagCompound tag) {
        UUID owner = null;
        if (tag.hasKey("owner")) {
            try {
                owner = UUID.fromString(tag.getString("owner"));
            } catch (RuntimeException ignored) {}
        }
        String name = tag.getString("name");
        return new MEBridgeChannelInfo(
            name,
            tag.getInteger("x"),
            tag.getInteger("y"),
            tag.getInteger("z"),
            tag.getInteger("dim"),
            tag.hasKey("color") ? tag.getInteger("color") : MEBridgeChannelColor.defaultFor(name),
            owner);
    }
}
