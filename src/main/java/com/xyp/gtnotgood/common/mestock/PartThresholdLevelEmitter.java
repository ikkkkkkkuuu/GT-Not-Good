// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.mestock;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import com.xyp.gtnotgood.client.mestock.StockModelRenderer;

import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStackWatcherHost;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.parts.PartBasicState;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Exact item/fluid hysteresis emitter. Storage notifications are coalesced before neighbor updates. */
public final class PartThresholdLevelEmitter extends PartBasicState
    implements StockHost, IStackWatcherHost, IGridTickable {

    private final StockConfig config = new StockConfig(1, this::stockChanged);
    private final StockWatcher watcher = new StockWatcher(this);
    private long upper;
    private boolean lowSignal = true;
    private boolean highLatch;
    private boolean output;
    private boolean queued;
    public long evaluations;

    public PartThresholdLevelEmitter(ItemStack stack) {
        super(stack);
        getProxy().setIdlePowerUsage(0.5);
    }

    @Override
    public StockConfig stockConfig() {
        return config;
    }

    @Override
    public String stockTitle() {
        return "item.threshold_level_emitter.name";
    }

    public long upper() {
        return upper;
    }

    public void setUpper(long value) {
        upper = Math.max(0, value);
        stockChanged();
    }

    public boolean lowSignal() {
        return lowSignal;
    }

    public void setLowSignal(boolean value) {
        lowSignal = value;
        stockChanged();
    }

    public boolean output() {
        return getTile() != null && getTile().getWorldObj().isRemote ? (getClientFlags() & 8) != 0 : output;
    }

    /** At equality the upper threshold enters the high state; dropping below the lower threshold leaves it. */
    public static boolean highState(boolean previous, long stored, long lower, long upper) {
        return previous ? stored >= lower : stored >= upper;
    }

    @Override
    public void stockChanged() {
        if (watcher == null) return;
        watcher.rebuild();
        if (getHost() != null) getHost().markForSave();
        wake();
    }

    private void wake() {
        if (getHost() == null || queued) return;
        try {
            queued = getProxy().getTick()
                .alertDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {
            queued = false;
        }
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
        if (!getProxy().isActive()) setOutput(false);
        wake();
    }

    @MENetworkEventSubscribe
    public void stockChannelChanged(MENetworkChannelsChanged event) {
        if (!getProxy().isActive()) setOutput(false);
        wake();
    }

    @Override
    protected int populateFlags(int flags) {
        return flags | (output ? 8 : 0);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(2, 100, false, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        queued = false;
        evaluations++;
        if (!getProxy().isActive() || config.key(0) == null || config.amount(0) > upper) {
            setOutput(false);
            return TickRateModulation.SLEEP;
        }
        try {
            boolean next = highState(
                highLatch,
                StockResources.count(getProxy().getStorage(), config.key(0)),
                config.amount(0),
                upper);
            if (next != highLatch) {
                highLatch = next;
                getHost().markForSave();
            }
            setOutput(lowSignal != highLatch);
        } catch (GridAccessException ignored) {
            setOutput(false);
        }
        return TickRateModulation.SLEEP;
    }

    private void setOutput(boolean value) {
        if (output == value || getHost() == null) return;
        output = value;
        getHost().markForUpdate();
        var tile = getTile();
        Platform.notifyBlocksOfNeighbors(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
        Platform.notifyBlocksOfNeighbors(
            tile.getWorldObj(),
            tile.xCoord + getSide().offsetX,
            tile.yCoord + getSide().offsetY,
            tile.zCoord + getSide().offsetZ);
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public int isProvidingStrongPower() {
        return output() ? 15 : 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return output() ? 15 : 0;
    }

    @Override
    public int cableConnectionRenderTo() {
        return 16;
    }

    @Override
    public void getBoxes(IPartCollisionHelper boxes) {
        boxes.addBox(7, 7, 11, 9, 9, 16);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(IPartRenderHelper helper, RenderBlocks renderer) {
        StockModelRenderer.inventory("extendedae:item/threshold_level_emitter");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(int x, int y, int z, IPartRenderHelper helper, RenderBlocks renderer) {
        StockModelRenderer.part(
            output() ? "extendedae:part/threshold_level_emitter_base_on"
                : "extendedae:part/threshold_level_emitter_base_off",
            x,
            y,
            z,
            helper,
            renderer,
            getColor());
        StockModelRenderer.part(
            StockModelRenderer.indicator("level_emitter_status", getClientFlags()),
            x,
            y,
            z,
            helper,
            renderer,
            getColor());
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
        config.write(tag);
        tag.setLong("upper", upper);
        tag.setBoolean("lowSignal", lowSignal);
        tag.setBoolean("highLatch", highLatch);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        config.read(tag);
        upper = Math.max(0, tag.getLong("upper"));
        lowSignal = !tag.hasKey("lowSignal") || tag.getBoolean("lowSignal");
        highLatch = tag.getBoolean("highLatch");
        output = false;
        stockChanged();
    }

    @Override
    public NBTTagCompound downloadSettings(SettingsFrom from) {
        NBTTagCompound tag = super.downloadSettings(from);
        config.write(tag);
        tag.setLong("upper", upper);
        tag.setBoolean("lowSignal", lowSignal);
        return tag;
    }

    @Override
    public void uploadSettings(SettingsFrom from, NBTTagCompound tag) {
        super.uploadSettings(from, tag);
        config.read(tag);
        upper = Math.max(0, tag.getLong("upper"));
        lowSignal = !tag.hasKey("lowSignal") || tag.getBoolean("lowSignal");
        stockChanged();
    }
}
