// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from pedroksl/AdvancedAE StockExportBusPart and AdvancedIOBusPart.
// Exact revision, changes and asset provenance: META-INF/advancedio-port/NOTICE.md.
package com.xyp.gtnotgood.common.advancedio;

import java.util.List;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartExportBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Maintains exact item/fluid stock and imports unlisted products through one cable-mounted machine face.
 * The two directions have independent upstream-style 8x operation budgets. Unexpected transfer remainders
 * are persisted in escrow and block new transfers until returned to ME, including across save/reload.
 * Unlike recipe providers this bus never bypasses GT sided inventory or fluid access rules.
 */
public final class PartAdvancedIOBus extends PartExportBus {

    public static final int CONFIG_SLOTS = 63;
    private final IAEStackInventory stockConfig = new IAEStackInventory(this, CONFIG_SLOTS, StorageName.CONFIG);
    private IAEStack<?> pending;
    private boolean regulate = true;
    private boolean transferring;
    private int cursor;

    public PartAdvancedIOBus(ItemStack stack) {
        super(stack);
        getProxy().setIdlePowerUsage(1);
    }

    @Override
    protected int getUpgradeSlots() {
        return 8;
    }

    @Override
    public int availableSlots() {
        return Math.min(CONFIG_SLOTS, 18 + 9 * getInstalledUpgrades(Upgrades.CAPACITY));
    }

    /** Retains GTNH's native additive upgrade tiers before applying AdvancedAE's eightfold budget. */
    @Override
    public int calculateAmountToSend() {
        return 8 * super.calculateAmountToSend();
    }

    @Override
    public IAEStackInventory getAEInventoryByName(StorageName name) {
        return name == StorageName.CONFIG ? stockConfig : super.getAEInventoryByName(name);
    }

    public IAEStack<?> filter(int slot) {
        return stockConfig.getAEStackInSlot(slot);
    }

    /** Sets a ghost identity and target; duplicate identities have one authoritative target, not competing loops. */
    public void setFilter(int slot, IAEStack<?> stack) {
        if (slot < 0 || slot >= availableSlots()) return;
        if (stack != null) {
            stack = stack.copy()
                .setStackSize(Math.max(1, Math.min(Integer.MAX_VALUE - 1L, stack.getStackSize())));
            for (int i = 0; i < CONFIG_SLOTS; i++) {
                if (i != slot && filter(i) != null && filter(i).isSameType(stack))
                    stockConfig.putAEStackInSlot(i, null);
            }
        }
        stockConfig.putAEStackInSlot(slot, stack);
        changed();
    }

    public boolean regulate() {
        return regulate;
    }

    public void setRegulate(boolean value) {
        regulate = value;
        changed();
    }

    public void changed() {
        if (getHost() == null) return;
        getHost().markForSave();
        try {
            getProxy().getTick()
                .alertDevice(getProxy().getNode());
        } catch (GridAccessException ignored) {}
    }

    @Override
    public void saveAEStackInv() {
        changed();
    }

    @Override
    public void upgradesChanged() {
        super.upgradesChanged();
        changed();
    }

    @Override
    protected boolean isSleeping() {
        return false; // Poll at AE's slow idle rate: tank contents can change without neighbor notifications.
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (transferring || getTile() == null
            || getTile().getWorldObj().isRemote
            || !getProxy().isActive()
            || !canDoBusWork()
            || !redstoneAllows()) return TickRateModulation.IDLE;
        transferring = true;
        try {
            if (!flushPending()) return TickRateModulation.SLOWER;
            var self = getTile();
            var side = getSide();
            var tile = self.getWorldObj()
                .getTileEntity(self.xCoord + side.offsetX, self.yCoord + side.offsetY, self.zCoord + side.offsetZ);
            if (tile == null || tile.isInvalid()) return TickRateModulation.SLOWER;
            var target = new BusTarget(tile, side.getOpposite());
            if (!target.available()) return TickRateModulation.SLOWER;
            boolean worked = false;
            int remaining = calculateAmountToSend();
            var initialStock = target.stock();
            var mode = (SchedulingMode) getConfigManager().getSetting(Settings.SCHEDULING_MODE);
            int start = mode == SchedulingMode.ROUNDROBIN ? cursor % availableSlots() : 0;
            for (int i = 0; i < availableSlots() && remaining > 0 && pending == null; i++) {
                int slot = mode == SchedulingMode.RANDOM ? self.getWorldObj().rand.nextInt(availableSlots())
                    : (start + i) % availableSlots();
                var filter = filter(slot);
                if (filter == null) continue;
                long unit = filter.isFluid() ? 1000 : 1;
                long amount = StockPolicy
                    .exportAmount(BusTarget.count(initialStock, filter), filter.getStackSize(), remaining * unit);
                long moved = export(target, filter, amount);
                if (moved > 0) {
                    BusTarget.add(
                        initialStock,
                        filter.copy()
                            .setStackSize(moved));
                    remaining -= (int) ((moved + unit - 1) / unit);
                    worked = true;
                    cursor = (slot + 1) % availableSlots();
                }
            }
            remaining = calculateAmountToSend();
            for (var stack : target.stock()) {
                if (remaining <= 0 || pending != null) break;
                var filter = matchingFilter(stack);
                long unit = stack.isFluid() ? 1000 : 1;
                long amount = StockPolicy.importAmount(
                    stack.getStackSize(),
                    filter == null ? 0 : filter.getStackSize(),
                    filter != null,
                    regulate,
                    remaining * unit);
                long moved = importStack(target, stack, amount);
                if (moved > 0) {
                    remaining -= (int) ((moved + unit - 1) / unit);
                    worked = true;
                }
            }
            return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            transferring = false;
        }
    }

    private boolean redstoneAllows() {
        if (getInstalledUpgrades(Upgrades.REDSTONE) == 0) return true;
        RedstoneMode mode = (RedstoneMode) getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
        return mode == RedstoneMode.IGNORE || mode == RedstoneMode.HIGH_SIGNAL && getHost().hasRedstone(getSide())
            || mode == RedstoneMode.LOW_SIGNAL && !getHost().hasRedstone(getSide());
    }

    private IAEStack<?> matchingFilter(IAEStack<?> key) {
        // Disabled rows remain protected from product import when a capacity card is removed.
        for (int i = 0; i < CONFIG_SLOTS; i++) {
            var filter = filter(i);
            if (filter != null && filter.isSameType(key)) {
                return i < availableSlots() ? filter
                    : key.copy()
                        .setStackSize(Long.MAX_VALUE);
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private IMEInventory inventory(IAEStack<?> key) throws GridAccessException {
        return key.isFluid() ? getProxy().getStorage()
            .getFluidInventory()
            : getProxy().getStorage()
                .getItemInventory();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private long export(BusTarget target, IAEStack<?> key, long amount) throws GridAccessException {
        if (amount <= 0) return 0;
        IAEStack request = key.copy()
            .setStackSize(amount);
        long accepted = target.insert(request, true);
        if (accepted <= 0) return 0;
        IMEInventory network = inventory(key);
        IAEStack extracted = Platform
            .poweredExtraction(getProxy().getEnergy(), network, request.setStackSize(accepted), mySrc);
        if (extracted == null) return 0;
        pending = extracted;
        changed();
        long inserted = target.insert(extracted, false);
        pending = inserted >= extracted.getStackSize() ? null
            : extracted.copy()
                .setStackSize(extracted.getStackSize() - inserted);
        // Returning extracted stock is a rollback, not a second powered transfer.
        if (pending != null) pending = network.injectItems(pending, Actionable.MODULATE, mySrc);
        changed();
        return inserted;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private long importStack(BusTarget target, IAEStack<?> key, long amount) throws GridAccessException {
        if (amount <= 0) return 0;
        IAEStack simulated = target.extract(
            key.copy()
                .setStackSize(amount),
            true);
        if (simulated == null || simulated.getStackSize() <= 0) return 0;
        IMEInventory network = inventory(key);
        IAEStack rest = Platform
            .poweredInsert(getProxy().getEnergy(), network, simulated.copy(), mySrc, Actionable.SIMULATE);
        long accepted = simulated.getStackSize() - (rest == null ? 0 : rest.getStackSize());
        if (accepted <= 0) return 0;
        IAEStack extracted = target.extract(
            simulated.copy()
                .setStackSize(accepted),
            false);
        if (extracted == null) return 0;
        pending = extracted;
        changed();
        pending = Platform.poweredInsert(getProxy().getEnergy(), network, extracted.copy(), mySrc);
        changed();
        return extracted.getStackSize() - (pending == null ? 0 : pending.getStackSize());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean flushPending() throws GridAccessException {
        if (pending == null) return true;
        pending = Platform.poweredInsert(getProxy().getEnergy(), inventory(pending), (IAEStack) pending.copy(), mySrc);
        changed();
        return pending == null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < CONFIG_SLOTS; i++) stockConfig.putAEStackInSlot(i, null);
        stockConfig.readFromNBT(tag, "advancedStock");
        normalizeFilters();
        regulate = !tag.hasKey("regulateStock") || tag.getBoolean("regulateStock");
        pending = tag.hasKey("advancedEscrow") ? Platform.readStackNBT(tag.getCompoundTag("advancedEscrow"), false)
            : null;
        cursor = Math.max(0, tag.getInteger("stockCursor")) % CONFIG_SLOTS;
    }

    /** Memory-card settings copy only ghost targets/settings, never real escrow or installed upgrades. */
    @Override
    public NBTTagCompound downloadSettings(appeng.util.SettingsFrom from) {
        NBTTagCompound tag = super.downloadSettings(from);
        tag.setBoolean("regulateStock", regulate);
        return tag;
    }

    @Override
    public void uploadSettings(appeng.util.SettingsFrom from, NBTTagCompound tag) {
        for (int i = 0; i < CONFIG_SLOTS; i++) stockConfig.putAEStackInSlot(i, null);
        super.uploadSettings(from, tag);
        normalizeFilters();
        regulate = !tag.hasKey("regulateStock") || tag.getBoolean("regulateStock");
        changed();
    }

    /** Bounds loaded amounts and discards duplicate identities from older or externally edited settings. */
    private void normalizeFilters() {
        for (int i = 0; i < CONFIG_SLOTS; i++) {
            var filter = filter(i);
            if (filter == null) continue;
            filter.setStackSize(Math.max(1, Math.min(Integer.MAX_VALUE - 1L, filter.getStackSize())));
            for (int previous = 0; previous < i; previous++) {
                if (filter(previous) != null && filter(previous).isSameType(filter)) {
                    stockConfig.putAEStackInSlot(i, null);
                    break;
                }
            }
        }
        if (getRSMode() == RedstoneMode.SIGNAL_PULSE) {
            getConfigManager().putSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        stockConfig.writeToNBT(tag, "advancedStock");
        tag.setBoolean("regulateStock", regulate);
        tag.setInteger("stockCursor", cursor);
        if (pending != null) {
            NBTTagCompound escrow = new NBTTagCompound();
            Platform.writeStackNBT(pending, escrow, true);
            tag.setTag("advancedEscrow", escrow);
        } else tag.removeTag("advancedEscrow");
    }

    @Override
    public void getDrops(List<ItemStack> drops, boolean wrenched) {
        super.getDrops(drops, wrenched);
        if (pending instanceof IAEItemStack item) {
            long left = item.getStackSize();
            while (left > 0) {
                ItemStack stack = item.getItemStack();
                stack.stackSize = (int) Math.min(left, stack.getMaxStackSize());
                drops.add(stack);
                left -= stack.stackSize;
            }
        } else if (pending instanceof IAEFluidStack fluid) drops.add(ItemFluidPacket.newStack(fluid.getFluidStack()));
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, Vec3 pos) {
        if (player.isSneaking()) return false;
        if (!player.worldObj.isRemote) AdvancedIOGuiFactory.INSTANCE.open(player, this);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(IPartRenderHelper helper, RenderBlocks renderer) {
        renderBody(helper, renderer, 0, 0, 0, true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(int x, int y, int z, IPartRenderHelper helper, RenderBlocks renderer) {
        setRenderCache(helper.useSimplifiedRendering(x, y, z, this, getRenderCache()));
        renderBody(helper, renderer, x, y, z, false);
    }

    /** Uses upstream textures with the GTNH export-bus geometry and cable status light. */
    @SideOnly(Side.CLIENT)
    private void renderBody(IPartRenderHelper helper, RenderBlocks renderer, int x, int y, int z, boolean inventory) {
        ItemAdvancedIOBus item = (ItemAdvancedIOBus) getItemStack().getItem();
        helper.setTexture(item.sides, item.sides, item.back, getFaceIcon(), item.sides, item.sides);
        int[][] boxes = { { 4, 4, 12, 12, 12, 14 }, { 5, 5, 14, 11, 11, 15 }, { 6, 6, 15, 10, 10, 16 },
            { 6, 6, 11, 10, 10, 12 } };
        for (int[] box : boxes) {
            helper.setBounds(box[0], box[1], box[2], box[3], box[4], box[5]);
            if (inventory) helper.renderInventoryBox(renderer);
            else helper.renderBlock(x, y, z, renderer);
        }
        if (!inventory) renderLights(x, y, z, helper, renderer);
    }
}
