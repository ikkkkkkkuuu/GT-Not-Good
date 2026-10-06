// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.mestock;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.common.advancedio.BusTarget;

import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartExportBus;
import appeng.util.Platform;
import appeng.util.SettingsFrom;

/**
 * Exports resources according to their network stock, preserving the exact reserve in above-threshold mode.
 * Dormant filters use exact AE watchers. Active transfers only query configured identities and the adjacent face.
 * Rejected delivery is persisted until the ME network can accept its rollback.
 */
public final class PartThresholdExportBus extends PartExportBus implements StockHost, IStackWatcherHost {

    private final StockConfig config = new StockConfig(63, this::stockChanged);
    private final StockWatcher watcher = new StockWatcher(this);
    private boolean above = true;
    private boolean wakeQueued;
    private int cursor;
    private IAEStack<?> escrow;
    private TileEntity cachedTile;
    private BusTarget cachedTarget;
    private int[] rows = new int[0];
    public long stockLookups;

    public PartThresholdExportBus(ItemStack stack) {
        super(stack);
    }

    @Override
    protected int getUpgradeSlots() {
        return 8;
    }

    @Override
    public int availableSlots() {
        return Math.min(63, 9 + 18 * getInstalledUpgrades(Upgrades.CAPACITY));
    }

    @Override
    public int stockSlots() {
        return availableSlots();
    }

    @Override
    public StockConfig stockConfig() {
        return config;
    }

    @Override
    public String stockTitle() {
        return "item.threshold_export_bus.name";
    }

    public boolean above() {
        return above;
    }

    public void setAbove(boolean value) {
        above = value;
        stockChanged();
    }

    @Override
    public void stockChanged() {
        if (watcher == null) return;
        watcher.rebuild();
        int[] next = new int[stockSlots()];
        int count = 0;
        for (int i = 0; i < stockSlots(); i++) if (config.key(i) != null) next[count++] = i;
        rows = Arrays.copyOf(next, count);
        if (getHost() != null) getHost().markForSave();
        wake();
    }

    private void wake() {
        if (getHost() == null || wakeQueued) return;
        try {
            wakeQueued = getProxy().getTick()
                .alertDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
            wakeQueued = false;
        }
    }

    @Override
    public void updateWatcher(IStackWatcher watcher) {
        this.watcher.bind(watcher);
        stockChanged();
    }

    @Override
    public void onStackChange(IItemList list, IAEStack full, IAEStack diff, BaseActionSource source,
        StorageChannel channel) {
        wake();
    }

    @Override
    public void upgradesChanged() {
        super.upgradesChanged();
        stockChanged();
    }

    @Override
    public void onNeighborChanged() {
        cachedTile = null;
        cachedTarget = null;
        super.onNeighborChanged();
        wake();
    }

    @Override
    protected boolean isSleeping() {
        return rows.length == 0 && escrow == null;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 100, isSleeping(), true);
    }

    /** The lower mode is an export condition; it does not restock the network or cap the destination. */
    public static long exportLimit(boolean above, long stored, long threshold) {
        if (above) return Math.max(0, stored - threshold);
        return stored <= threshold ? stored : 0;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TickRateModulation doBusWork() {
        wakeQueued = false;
        if (!getProxy().isActive() || !canDoBusWork()) return TickRateModulation.IDLE;
        if (getInstalledUpgrades(Upgrades.REDSTONE) > 0 && getRSMode() != RedstoneMode.IGNORE) {
            boolean powered = getHost().hasRedstone(getSide());
            if (getRSMode() == RedstoneMode.SIGNAL_PULSE || powered != (getRSMode() == RedstoneMode.HIGH_SIGNAL))
                return TickRateModulation.IDLE;
        }
        try {
            if (escrow != null) {
                IMEInventory network = escrow.isFluid() ? getProxy().getStorage()
                    .getFluidInventory()
                    : getProxy().getStorage()
                        .getItemInventory();
                escrow = network.injectItems(escrow, Actionable.MODULATE, mySrc);
                getHost().markForSave();
                if (escrow != null) return TickRateModulation.SLOWER;
            }
            if (rows.length == 0) return TickRateModulation.SLEEP;
            TileEntity self = getTile();
            int x = self.xCoord + getSide().offsetX, y = self.yCoord + getSide().offsetY,
                z = self.zCoord + getSide().offsetZ;
            if (!self.getWorldObj()
                .blockExists(x, y, z)) return TickRateModulation.IDLE;
            TileEntity tile = self.getWorldObj()
                .getTileEntity(x, y, z);
            if (tile != cachedTile || cachedTarget == null) {
                cachedTile = tile;
                cachedTarget = tile == null ? null : new BusTarget(tile, getSide().getOpposite());
            }
            if (cachedTarget == null || !cachedTarget.available()) return TickRateModulation.SLEEP;
            // Inventory notifications can invalidate the neighbor cache during this transfer.
            BusTarget target = cachedTarget;
            long budget = calculateAmountToSend();
            boolean eligible = false, moved = false;
            SchedulingMode mode = (SchedulingMode) getConfigManager().getSetting(Settings.SCHEDULING_MODE);
            for (int n = 0; n < rows.length && budget > 0; n++) {
                int offset = mode == SchedulingMode.RANDOM ? Platform.getRandom()
                    .nextInt(rows.length) : mode == SchedulingMode.ROUNDROBIN ? (cursor + n) % rows.length : n;
                int slot = rows[offset];
                IAEStack<?> key = config.key(slot);
                stockLookups++;
                long stored = StockResources.count(getProxy().getStorage(), key);
                long limit = exportLimit(above, stored, config.amount(slot));
                if (limit <= 0) continue;
                eligible = true;
                long transfer = Math.min(Integer.MAX_VALUE, Math.min(limit, budget * (key.isFluid() ? 1000L : 1L)));
                IAEStack offer = key.copy()
                    .setStackSize(transfer);
                long accepted = target.insert(offer, true);
                if (accepted <= 0) continue;
                IMEInventory network = key.isFluid() ? getProxy().getStorage()
                    .getFluidInventory()
                    : getProxy().getStorage()
                        .getItemInventory();
                IAEStack extracted = Platform
                    .poweredExtraction(getProxy().getEnergy(), network, offer.setStackSize(accepted), mySrc);
                if (extracted == null) continue;
                escrow = extracted;
                getHost().markForSave();
                long inserted = target.insert(extracted, false);
                escrow = inserted >= extracted.getStackSize() ? null
                    : extracted.copy()
                        .setStackSize(extracted.getStackSize() - inserted);
                if (escrow != null) escrow = network.injectItems(escrow, Actionable.MODULATE, mySrc);
                getHost().markForSave();
                budget -= Math.max(1, (inserted + (key.isFluid() ? 999 : 0)) / (key.isFluid() ? 1000 : 1));
                moved |= inserted > 0;
                if (escrow != null || cachedTarget != target) break;
            }
            if (moved) cursor = (cursor + 1) % rows.length;
            return moved ? TickRateModulation.FASTER : eligible ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        }
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, Vec3 pos) {
        if (player.isSneaking()) return false;
        if (!player.worldObj.isRemote) StockGuiFactory.instance.open(player, this);
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagCompound settings = new NBTTagCompound();
        config.write(settings);
        tag.setTag("stockSettings", settings);
        tag.setBoolean("stockAbove", above);
        if (escrow != null) {
            NBTTagCompound pending = new NBTTagCompound();
            Platform.writeStackNBT(escrow, pending, true);
            tag.setTag("stockEscrow", pending);
        } else tag.removeTag("stockEscrow");
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        config.read(tag.getCompoundTag("stockSettings"));
        above = !tag.hasKey("stockAbove") || tag.getBoolean("stockAbove");
        escrow = tag.hasKey("stockEscrow") ? Platform.readStackNBT(tag.getCompoundTag("stockEscrow"), false) : null;
        stockChanged();
    }

    @Override
    public NBTTagCompound downloadSettings(SettingsFrom from) {
        NBTTagCompound tag = super.downloadSettings(from);
        NBTTagCompound settings = new NBTTagCompound();
        config.write(settings);
        tag.setTag("stockSettings", settings);
        tag.setBoolean("stockAbove", above);
        return tag;
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound tag) {
        super.uploadSettings(from, tag);
        if (tag.hasKey("stockSettings")) config.read(tag.getCompoundTag("stockSettings"));
        if (tag.hasKey("stockAbove")) above = tag.getBoolean("stockAbove");
        stockChanged();
    }

    @Override
    public void getDrops(List<ItemStack> drops, boolean wrenched) {
        super.getDrops(drops, wrenched);
        if (escrow instanceof IAEItemStack item) {
            long left = item.getStackSize();
            while (left > 0) {
                ItemStack stack = item.getItemStack();
                stack.stackSize = (int) Math.min(left, stack.getMaxStackSize());
                drops.add(stack);
                left -= stack.stackSize;
            }
        } else if (escrow instanceof IAEFluidStack fluid) drops.add(ItemFluidPacket.newStack(fluid.getFluidStack()));
    }
}
