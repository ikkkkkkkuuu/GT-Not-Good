package com.xyp.gtnotgood.common.mestock;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

import com.xyp.gtnotgood.common.mebridge.TileMEBridgeBase;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.networking.GridFlags;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;

/** Five auto-stock requests, serviced by a shared bounded network scheduler instead of individual tick polling. */
public final class TileMERequester extends TileMEBridgeBase implements StockHost, IStackWatcherHost {

    public static final int empty = 0, ready = 1, waiting = 2, calculating = 3, crafting = 4, missingMaterials = 5,
        waitingCPU = 6, offline = 7, disabled = 8, noPattern = 9, failed = 10;
    private final StockConfig config = new StockConfig(5, this::stockChanged);
    private final StockWatcher watcher = new StockWatcher(this);
    private final int[] statuses = new int[5];
    private final long[] stored = new long[5];
    private final long[] pending = new long[5];
    private final long[] retryAfter = new long[5];
    private StockGridCache scheduler;
    private String name = "";
    private int redstoneMode;
    private int cursor;
    private boolean renderActive;
    public long stockLookups;

    @Override
    public StockConfig stockConfig() {
        return config;
    }

    @Override
    public String stockTitle() {
        return "tile.me_requester.name";
    }

    @Override
    protected ItemStack getVisualRepresentation() {
        return GTNGItemList.MERequester.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        boolean creating = gridProxy == null;
        AENetworkProxy result = super.getProxy();
        if (creating) {
            result.setFlags(GridFlags.REQUIRE_CHANNEL);
            result.setIdlePowerUsage(1);
        }
        return result;
    }

    public String name() {
        return name;
    }

    public void setName(String value) {
        name = value == null ? "" : value.substring(0, Math.min(48, value.length()));
        markDirty();
        if (scheduler != null) scheduler.directoryChanged();
    }

    public int redstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(int value) {
        redstoneMode = Math.floorMod(value, 3);
        stockChanged();
    }

    public int status(int slot) {
        return statuses[slot];
    }

    public long stored(int slot) {
        return stored[slot];
    }

    public long pending(int slot) {
        return pending[slot];
    }

    void setStatus(int row, int status) {
        statuses[row] = status;
    }

    void delayRow(int row, long until) {
        retryAfter[row] = until;
    }

    void attach(StockGridCache cache) {
        scheduler = cache;
    }

    void detach(StockGridCache cache) {
        if (scheduler == cache) scheduler = null;
        Arrays.fill(statuses, offline);
    }

    public boolean canRequest() {
        if (!isServerSide() || isInvalid() || !getProxy().isActive()) return false;
        if (redstoneMode == 0) return true;
        return worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) == (redstoneMode == 1);
    }

    @Override
    public void stockChanged() {
        if (watcher == null) return;
        watcher.rebuild();
        Arrays.fill(retryAfter, 0);
        markDirty();
        wake();
        if (scheduler != null) scheduler.directoryChanged();
    }

    private void wake() {
        if (scheduler != null) scheduler.schedule(this, 0);
    }

    @Override
    public void updateWatcher(IStackWatcher watcher) {
        this.watcher.bind(watcher);
        wake();
    }

    @Override
    public void onStackChange(IItemList list, IAEStack full, IAEStack diff, BaseActionSource source,
        StorageChannel channel) {
        wake();
    }

    @MENetworkEventSubscribe
    public void stockPowerChanged(MENetworkPowerStatusChange event) {
        updateRenderState();
        wake();
    }

    @MENetworkEventSubscribe
    public void stockChannelChanged(MENetworkChannelsChanged event) {
        updateRenderState();
        wake();
    }

    public void neighborChanged() {
        wake();
    }

    @Override
    protected void onProxyReady() {
        updateRenderState();
    }

    public boolean renderActive() {
        return isServerSide() ? getProxy().isActive() : renderActive;
    }

    /** Sends only a visual flag when power/channel state changes; no inventory or per-tick packets. */
    private void updateRenderState() {
        if (!isServerSide()) return;
        boolean active = getProxy().isActive();
        if (active == renderActive) return;
        renderActive = active;
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("active", renderActive());
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, data);
    }

    @Override
    public void onDataPacket(NetworkManager manager, S35PacketUpdateTileEntity packet) {
        renderActive = packet.func_148857_g()
            .getBoolean("active");
        worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }

    void service(StockGridCache cache) {
        if (!canRequest()) {
            Arrays.fill(statuses, offline);
            cache.schedule(this, 200);
            return;
        }
        int retry = 100;
        try {
            var storage = getProxy().getStorage();
            var craftingGrid = getProxy().getCrafting();
            for (int n = 0; n < config.size(); n++) {
                int row = (cursor + n) % config.size();
                IAEStack<?> key = config.key(row);
                if (key == null) {
                    statuses[row] = empty;
                    continue;
                }
                if (!config.enabled(row) || config.amount(row) == 0) {
                    statuses[row] = disabled;
                    continue;
                }
                if (retryAfter[row] > cache.now()) continue;
                stockLookups++;
                stored[row] = StockResources.count(storage, key);
                pending[row] = cache.pending(key);
                long missing = StockResources.missing(config.amount(row), stored[row], pending[row]);
                if (cache.calculating(this, row)) {
                    statuses[row] = calculating;
                    retry = 20;
                    continue;
                }
                if (missing == 0) {
                    statuses[row] = pending[row] > 0 ? crafting : ready;
                    continue;
                }
                if (!craftingGrid.getCraftingMultiPatterns()
                    .containsKey(key) && !craftingGrid.canEmitFor(key)) {
                    statuses[row] = noPattern;
                    continue;
                }
                long batch = config.batch(row) == 0 ? missing : Math.min(missing, config.batch(row));
                if (cache.start(this, row, batch)) {
                    statuses[row] = calculating;
                    retry = 20;
                    cursor = (row + 1) % config.size();
                    break;
                }
                statuses[row] = waiting;
                retry = 20;
            }
        } catch (GridAccessException | RuntimeException error) {
            Arrays.fill(statuses, offline);
        }
        cache.schedule(this, retry);
    }

    public void writeSettings(NBTTagCompound tag) {
        config.write(tag);
        tag.setString("requesterName", name);
        tag.setInteger("requesterRedstone", redstoneMode);
    }

    public void readSettings(NBTTagCompound tag) {
        config.read(tag);
        setName(tag.getString("requesterName"));
        redstoneMode = Math.max(0, Math.min(2, tag.getInteger("requesterRedstone")));
        stockChanged();
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeSettings(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSettings(tag);
    }
}
