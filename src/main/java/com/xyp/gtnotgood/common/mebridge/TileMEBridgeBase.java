package com.xyp.gtnotgood.common.mebridge;

import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;

/**
 * 跨维度 ME 网桥方块的公共基类。
 * <p>
 * 一个普通 {@link TileEntity},通过 {@link AENetworkProxy} 接入相邻的本地 ME 网络(像一段线缆节点)。
 * 发起端 / 接收端各自扩展它:发起端注册频道,接收端匹配频道后用 {@code createGridConnection} 把两端并成一张网。
 * <p>
 * 节点生命周期严格管理(onChunkUnload / invalidate 都销毁节点),避免残留连接污染 AE 网络。
 */
public abstract class TileMEBridgeBase extends TileEntity implements IGridProxyable, IActionHost {

    protected AENetworkProxy gridProxy = null;
    private boolean readyCalled = false;
    /** 放置后头几 tick 内重复重扫节点 + 通知邻居，兜住线缆邻接扫描的时机竞态。 */
    private int connectRetryTicks = 0;
    private static final int CONNECT_RETRY_LIMIT = 5;
    /** 放置者用户名。设 proxy owner 用——连接带安保终端的主网时安保检查要靠它。 */
    protected String ownerName = "";
    private boolean ownerApplied = false;

    /** 子类提供用于 AE 视觉表现的物品(方块本身的 ItemStack)。可返回 null。 */
    protected abstract net.minecraft.item.ItemStack getVisualRepresentation();

    /** 方块放置时记录放置者(见 BlockMEBridgeBase.onBlockPlacedBy),用于 proxy owner + 安保。 */
    public void setOwnerName(String name) {
        this.ownerName = name == null ? "" : name;
        this.ownerApplied = false;
        markDirty();
    }

    @Override
    public AENetworkProxy getProxy() {
        if (gridProxy == null) {
            gridProxy = new AENetworkProxy(this, "mebridge_proxy", getVisualRepresentation(), true);
            // 桥接节点像量子网桥一样传导通道(32 通道、路径优先),这样接收端的样板总成等设备
            // 能通过桥拿到通道。CANNOT_CARRY 会阻断通道传导,导致"缺少频道",故不能用。
            gridProxy.setFlags(GridFlags.DENSE_CAPACITY);
            gridProxy.setIdlePowerUsage(0);
            gridProxy.setValidSides(EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN)));
        }
        applyOwnerIfPossible();
        return gridProxy;
    }

    /**
     * 把放置者解析成在线 EntityPlayer 并设为 proxy owner。安保检查(Platform.securityCheck)靠 owner 的
     * playerID 判权限;不设 owner 则连接带安保终端的主网会被拒。放置者刚放下方块时在线,能解析成功。
     */
    private void applyOwnerIfPossible() {
        if (ownerApplied || gridProxy == null || worldObj == null || worldObj.isRemote) return;
        if (ownerName.isEmpty()) return;
        EntityPlayer player = worldObj.getPlayerEntityByName(ownerName);
        if (player != null) {
            gridProxy.setOwner(player);
            ownerApplied = true;
        }
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {}

    @Override
    public IGridNode getGridNode(ForgeDirection dir) {
        AENetworkProxy proxy = getProxy();
        return proxy != null ? proxy.getNode() : null;
    }

    @Override
    public IGridNode getActionableNode() {
        AENetworkProxy proxy = getProxy();
        return proxy != null ? proxy.getNode() : null;
    }

    @Override
    public AECableType getCableConnectionType(ForgeDirection dir) {
        return AECableType.SMART;
    }

    /** 该桥接方块是否已接入一个 AE 网络(节点存在且已归属某个 grid)。仅服务端有意义。 */
    public boolean isInNetwork() {
        if (worldObj == null || worldObj.isRemote || gridProxy == null) return false;
        IGridNode node = gridProxy.getNode();
        // AE2 里孤立节点也有自己的单节点 grid,故不能用 getGrid()!=null 判定。
        // 用实际连接数:接了线缆 / 连上别的节点才算"在网"。
        return node != null && !node.getConnections()
            .isEmpty();
    }

    @Override
    public void securityBreak() {}

    // region tile lifecycle

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!worldObj.isRemote) {
            if (!readyCalled) {
                getProxy().onReady();
                readyCalled = true;
                onProxyReady();
                connectRetryTicks = CONNECT_RETRY_LIMIT;
            }
            // The AE node only exists after onReady (first server tick). A cable placed before us already
            // finished its adjacency scan while our node was still null. Re-scan our own node and nudge
            // neighbours for a few ticks to cover any placement/creation timing race (both orders work).
            if (connectRetryTicks > 0) {
                connectRetryTicks--;
                AENetworkProxy proxy = getProxy();
                IGridNode node = proxy != null ? proxy.getNode() : null;
                if (node != null) node.updateState();
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
            }
        }
    }

    /** proxy 节点就绪后调用一次(子类做频道注册 / 首次连接)。 */
    protected void onProxyReady() {}

    @Override
    public void invalidate() {
        super.invalidate();
        onBridgeInvalidate();
        if (gridProxy != null) gridProxy.invalidate();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        onBridgeInvalidate();
        if (gridProxy != null) gridProxy.onChunkUnload();
    }

    /** 方块失效前的清理(子类断开连接 / 注销频道)。 */
    protected void onBridgeInvalidate() {}

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        ownerName = nbt.getString("mebridge_owner");
        ownerApplied = false;
        if (nbt.hasKey("mebridge_proxy")) {
            getProxy().readFromNBT(nbt);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("mebridge_owner", ownerName == null ? "" : ownerName);
        getProxy().writeToNBT(nbt);
    }

    // endregion

    public boolean isServerSide() {
        return worldObj != null && !worldObj.isRemote;
    }

    public EntityPlayer getOwnerPlayer() {
        if (worldObj == null || ownerName == null || ownerName.isEmpty()) return null;
        return worldObj.getPlayerEntityByName(ownerName);
    }
}
