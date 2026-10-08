package com.xyp.gtnotgood.common.blocks.stockio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.parts.advancedio.BusTarget;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridHost;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Shared cable-part/block storage policy and recipe transaction. Marks stay in ME until an accepted
 * recipe commits. Unexpected refunds and product-import remainders survive save/load and block new
 * transactions until ME accepts them, so changing or removing a target never discards real resources.
 */
public final class StockIOLogic {

    public static final int SLOT_COUNT = 900;
    private static final long ITEM_IMPORT_LIMIT = 1_048_584;
    private static final long FLUID_IMPORT_LIMIT = 1_048_584_000;

    public final ItemStack[] itemFilters = new ItemStack[SLOT_COUNT];
    public final FluidStack[] fluidFilters = new FluidStack[SLOT_COUNT];
    public final long[] networkItems = new long[SLOT_COUNT];
    public final long[] networkFluids = new long[SLOT_COUNT];
    public final long[] offeredItems = new long[SLOT_COUNT];
    public final long[] offeredFluids = new long[SLOT_COUNT];
    private final StockIOPolicy[] itemPolicies = new StockIOPolicy[SLOT_COUNT];
    private final StockIOPolicy[] fluidPolicies = new StockIOPolicy[SLOT_COUNT];
    private final List<Pending> pending = new ArrayList<>();
    private final Host host;
    private boolean enabled = true;
    private boolean limitedMode;
    private boolean fixedMode;
    private boolean recycle = true;
    private boolean regulate;
    private boolean autoPullItems;
    private boolean autoPullFluids;
    private boolean online;
    private boolean targetConnected;
    private String targetNameKey = "";
    private int refreshTime = 20;
    private int minItemAutoPull = 64;
    private int minFluidAutoPull = 1000;
    private boolean transferring;
    private boolean refreshRequested = true;
    private long lastWorkTick = Long.MIN_VALUE;
    private long lastRefreshTick = Long.MIN_VALUE;
    private StockIOSnapshot activeRecipe;

    public StockIOLogic(Host host) {
        this.host = host;
    }

    /** Coordinates point at the adjacent machine; its API receives the opposite face. */
    public interface Host {

        boolean isServerSide();

        AENetworkProxy getProxy();

        BaseActionSource getActionSource();

        TileEntity getTarget();

        ForgeDirection getTargetSide();

        void setTargetSide(ForgeDirection side);

        void markDirty();

        long getTimer();

        default boolean canSelectTargetSide() {
            return true;
        }
    }

    public boolean isServerSide() {
        return host.isServerSide();
    }

    public boolean isOnline() {
        return isServerSide() && host.getProxy().isActive();
    }

    public boolean isTargetConnected() {
        return targetConnected;
    }

    public String targetNameKey() {
        return targetNameKey;
    }

    public boolean canSelectTargetSide() {
        return host.canSelectTargetSide();
    }

    public ForgeDirection getTargetSide() {
        return host.getTargetSide();
    }

    public ForgeDirection getTargetFace() {
        return getTargetSide().getOpposite();
    }

    public void setTargetSide(ForgeDirection side) {
        if (!canChange() || side == null || side == ForgeDirection.UNKNOWN || !canSelectTargetSide()) return;
        host.setTargetSide(side);
        policyChanged();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (!canChange()) return;
        enabled = value;
        policyChanged();
    }

    public boolean isLimitedMode() {
        return limitedMode;
    }

    public void setLimitedMode(boolean value) {
        if (!canChange()) return;
        limitedMode = value;
        policyChanged();
    }

    public boolean isFixedMode() {
        return fixedMode;
    }

    public void setFixedMode(boolean value) {
        if (!canChange()) return;
        fixedMode = value;
        policyChanged();
    }

    public boolean isRecycle() {
        return recycle;
    }

    public void setRecycle(boolean value) {
        if (!canChange()) return;
        recycle = value;
        policyChanged();
    }

    public boolean isRegulate() {
        return regulate;
    }

    public void setRegulate(boolean value) {
        if (!canChange()) return;
        regulate = value;
        policyChanged();
    }

    public boolean isAutoPullItems() {
        return autoPullItems;
    }

    public void setAutoPullItems(boolean value) {
        if (!canChange()) return;
        autoPullItems = value;
        policyChanged();
    }

    public boolean isAutoPullFluids() {
        return autoPullFluids;
    }

    public void setAutoPullFluids(boolean value) {
        if (!canChange()) return;
        autoPullFluids = value;
        policyChanged();
    }

    public int getRefreshTime() {
        return refreshTime;
    }

    public void setRefreshTime(int value) {
        if (!canChange()) return;
        refreshTime = Math.max(1, value);
        policyChanged();
    }

    public int getMinItemAutoPull() {
        return minItemAutoPull;
    }

    public void setMinItemAutoPull(int value) {
        if (!canChange()) return;
        minItemAutoPull = Math.max(1, value);
        policyChanged();
    }

    public int getMinFluidAutoPull() {
        return minFluidAutoPull;
    }

    public void setMinFluidAutoPull(int value) {
        if (!canChange()) return;
        minFluidAutoPull = Math.max(1, value);
        policyChanged();
    }

    public StockIOPolicy getPolicy(boolean fluid, int index) {
        checkSlot(index);
        StockIOPolicy[] policies = fluid ? fluidPolicies : itemPolicies;
        if (policies[index] == null) policies[index] = new StockIOPolicy(fluid);
        return policies[index];
    }

    public void setItemFilter(int index, ItemStack filter) {
        if (!canChange() || index < 0 || index >= SLOT_COUNT) return;
        if (filter != null) {
            filter = filter.copy();
            filter.stackSize = 1;
        }
        if (filter != null && matchingSlot(AEItemStack.create(filter), index) >= 0) return;
        if (sameItem(itemFilters[index], filter)) return;
        itemFilters[index] = filter;
        itemPolicies[index] = null;
        policyChanged();
    }

    public void setFluidFilter(int index, FluidStack filter) {
        if (!canChange() || index < 0 || index >= SLOT_COUNT) return;
        if (filter != null) {
            filter = filter.copy();
            filter.amount = 1;
        }
        if (filter != null && matchingSlot(AEFluidStack.create(filter), index) >= 0) return;
        if (sameFluid(fluidFilters[index], filter)) return;
        fluidFilters[index] = filter;
        fluidPolicies[index] = null;
        policyChanged();
    }

    public void policyChanged() {
        if (!isServerSide()) return;
        refreshRequested = true;
        host.markDirty();
    }

    private boolean canChange() {
        return isServerSide() && activeRecipe == null && !transferring;
    }

    private static void checkSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) throw new IndexOutOfBoundsException("Stock IO slot " + index);
    }

    /** Never writes configured inputs into the target: only product recycling runs periodically. */
    public void tick() {
        if (!isServerSide() || transferring || activeRecipe != null) return;
        online = isOnline();
        if (!online) {
            clearAmounts();
            return;
        }
        long timer = host.getTimer();
        boolean workTick = elapsed(timer, lastWorkTick, 5);
        boolean refresh = refreshRequested || elapsed(timer, lastRefreshTick, refreshTime);
        if (!workTick && !refresh) return;
        transferring = true;
        try {
            if (workTick) {
                lastWorkTick = timer;
                if (!flushPending()) return;
            }
            if (refresh) {
                if (enabled) refreshAutomaticMarks();
                refreshNetworkAmounts();
                refreshRequested = false;
                lastRefreshTick = timer;
            }
            if (!workTick) return;
            TileEntity targetTile = host.getTarget();
            targetConnected = targetTile != null && !targetTile.isInvalid() && !sameGrid(targetTile);
            if (targetConnected && targetTile instanceof BaseMetaTileEntity base && base.getMetaTileEntity() != null) {
                targetNameKey = base.getMetaTileEntity().getLocalNameKey();
            } else {
                targetNameKey = targetConnected && targetTile.getBlockType() != null
                    ? targetTile.getBlockType().getUnlocalizedName() + ".name"
                    : "";
            }
            if (!enabled || !recycle || !targetConnected || !pending.isEmpty()) return;
            BusTarget target = new BusTarget(targetTile, getTargetFace(), true);
            if (target.available()) recycle(target, targetTile);
        } catch (GridAccessException ignored) {
            online = false;
            clearAmounts();
        } finally {
            transferring = false;
        }
    }

    private static boolean elapsed(long timer, long previous, int interval) {
        return previous == Long.MIN_VALUE || timer < previous || timer - previous >= interval;
    }

    private void clearAmounts() {
        Arrays.fill(networkItems, 0);
        Arrays.fill(networkFluids, 0);
        Arrays.fill(offeredItems, 0);
        Arrays.fill(offeredFluids, 0);
    }

    private void refreshNetworkAmounts() throws GridAccessException {
        clearAmounts();
        if (!enabled) return;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (itemFilters[slot] != null) {
                long stored = available(AEItemStack.create(itemFilters[slot]));
                networkItems[slot] = stored;
                offeredItems[slot] = getPolicy(false, slot).offered(stored, limitedMode, fixedMode);
            }
            if (fluidFilters[slot] != null) {
                long stored = available(AEFluidStack.create(fluidFilters[slot]));
                networkFluids[slot] = stored;
                offeredFluids[slot] = getPolicy(true, slot).offered(stored, limitedMode, fixedMode);
            }
        }
    }

    private void refreshAutomaticMarks() throws GridAccessException {
        if (autoPullItems) {
            int free = freeSlot(false);
            for (IAEItemStack item : host.getProxy().getStorage().getItemInventory().getStorageList()) {
                if (free < 0) break;
                if (item.getStackSize() < minItemAutoPull || matchingSlot(item, -1) >= 0) continue;
                itemFilters[free] = item.getItemStack();
                itemFilters[free].stackSize = 1;
                itemPolicies[free] = null;
                host.markDirty();
                free = freeSlot(false);
            }
        }
        if (autoPullFluids) {
            int free = freeSlot(true);
            for (IAEFluidStack fluid : host.getProxy().getStorage().getFluidInventory().getStorageList()) {
                if (free < 0) break;
                if (fluid.getStackSize() < minFluidAutoPull || matchingSlot(fluid, -1) >= 0) continue;
                fluidFilters[free] = fluid.getFluidStack();
                fluidFilters[free].amount = 1;
                fluidPolicies[free] = null;
                host.markDirty();
                free = freeSlot(true);
            }
        }
    }

    private int freeSlot(boolean fluid) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if ((fluid ? fluidFilters[i] : itemFilters[i]) == null) return i;
        }
        return -1;
    }

    private int matchingSlot(IAEStack<?> key, int skip) {
        if (key == null) return -1;
        if (key instanceof IAEItemStack item) {
            ItemStack sample = item.getItemStack();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (slot != skip && itemFilters[slot] != null && sameItem(itemFilters[slot], sample)) return slot;
            }
        } else if (key instanceof IAEFluidStack fluid) {
            FluidStack sample = fluid.getFluidStack();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (slot != skip && fluidFilters[slot] != null && sameFluid(fluidFilters[slot], sample)) return slot;
            }
        }
        return -1;
    }

    private boolean sameGrid(TileEntity target) {
        if (!(target instanceof IGridHost gridHost)) return false;
        var node = host.getProxy().getNode();
        if (node == null) return false;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            var targetNode = gridHost.getGridNode(side);
            if (targetNode != null && targetNode.getGrid() == node.getGrid()) return true;
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    private IMEInventory inventory(IAEStack<?> key) throws GridAccessException {
        return key.isFluid() ? host.getProxy().getStorage().getFluidInventory()
            : host.getProxy().getStorage().getItemInventory();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private long available(IAEStack<?> key) throws GridAccessException {
        IAEStack found = inventory(key).extractItems(key.copy().setStackSize(Long.MAX_VALUE), Actionable.SIMULATE,
            host.getActionSource());
        return found == null ? 0 : Math.max(0, found.getStackSize());
    }

    /** Begins one recipe check with mutable virtual inputs; no resource leaves ME at this stage. */
    public StockIOSnapshot startRecipe() {
        return prepareRecipe(-1, false);
    }

    /** Begins a fuel check for one admitted resource without querying the other configured marks. */
    public StockIOSnapshot startRecipe(boolean fluid, int slot) {
        checkSlot(slot);
        return prepareRecipe(slot, fluid);
    }

    private StockIOSnapshot prepareRecipe(int selectedSlot, boolean fluidOnly) {
        if (!isServerSide() || !enabled || !isOnline() || transferring || activeRecipe != null || !pending.isEmpty())
            return null;
        StockIOSnapshot snapshot = new StockIOSnapshot(this);
        try {
            int firstSlot = Math.max(0, selectedSlot);
            int endSlot = selectedSlot < 0 ? SLOT_COUNT : selectedSlot + 1;
            for (int slot = firstSlot; slot < endSlot; slot++) {
                if ((selectedSlot < 0 || !fluidOnly) && itemFilters[slot] != null) {
                    int offered = getPolicy(false, slot).offered(available(AEItemStack.create(itemFilters[slot])),
                        limitedMode, fixedMode);
                    if (offered > 0) {
                        snapshot.items[slot] = itemFilters[slot].copy();
                        snapshot.items[slot].stackSize = offered;
                        snapshot.originalItems[slot] = snapshot.items[slot].copy();
                    }
                }
                if ((selectedSlot < 0 || fluidOnly) && fluidFilters[slot] != null) {
                    int offered = getPolicy(true, slot).offered(available(AEFluidStack.create(fluidFilters[slot])),
                        limitedMode, fixedMode);
                    if (offered > 0) {
                        snapshot.fluids[slot] = fluidFilters[slot].copy();
                        snapshot.fluids[slot].amount = offered;
                        snapshot.originalFluids[slot] = snapshot.fluids[slot].copy();
                    }
                }
            }
            activeRecipe = snapshot;
            return snapshot;
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    /** Releases a rejected recipe check without consuming anything. */
    public void abortRecipe(StockIOSnapshot snapshot) {
        if (snapshot != null && snapshot == activeRecipe && snapshot.owner == this) {
            snapshot.closed = true;
            activeRecipe = null;
        }
    }

    public void cancelRecipe(StockIOSnapshot snapshot) {
        abortRecipe(snapshot);
    }

    /**
     * Commits consumed differences after acceptance. Every identity/reserve is rechecked; if any powered
     * extraction falls short, all quantities extracted by this transaction are returned or held in escrow.
     *
     * @return whether all recipe consumption reached ME; the caller must reject outputs on false
     */
    public boolean endRecipe(StockIOSnapshot snapshot) {
        return endRecipe(snapshot, () -> true);
    }

    /**
     * Extracts the complete consumption ledger before granting the caller's result.
     *
     * @param snapshot     mutable inputs owned by the current transaction
     * @param commitEffect returns true once the result is granted and fuel cannot be refunded; on false,
     *                     the caller must have undone every result mutation before the ledger is refunded
     * @return whether both extraction and the result commit succeeded; refused refunds remain in durable escrow
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean endRecipe(StockIOSnapshot snapshot, BooleanSupplier commitEffect) {
        Objects.requireNonNull(commitEffect, "commitEffect");
        if (
            !isServerSide() || snapshot == null || snapshot.closed || snapshot != activeRecipe || snapshot.owner != this
        ) return false;
        List<IAEStack<?>> requests = new ArrayList<>();
        List<IAEStack<?>> extracted = new ArrayList<>();
        transferring = true;
        try {
            if (!enabled || !isOnline() || !pending.isEmpty()) return false;
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (
                    !consumption(requests, snapshot.originalItems[slot], snapshot.items[slot], slot)
                        || !consumption(requests, snapshot.originalFluids[slot], snapshot.fluids[slot], slot)
                ) return false;
            }
            for (IAEStack<?> request : requests) {
                IAEStack actual = Platform.poweredExtraction(host.getProxy().getEnergy(), inventory(request),
                    (IAEStack) request.copy(), host.getActionSource());
                if (actual != null && actual.getStackSize() > 0) extracted.add(actual);
                if (actual == null || actual.getStackSize() != request.getStackSize()) {
                    rollback(extracted);
                    return false;
                }
            }
            if (!commitEffect.getAsBoolean()) {
                rollback(extracted);
                return false;
            }
            refreshRequested = true;
            return true;
        } catch (GridAccessException ignored) {
            rollback(extracted);
            return false;
        } finally {
            snapshot.closed = true;
            activeRecipe = null;
            transferring = false;
        }
    }

    private boolean consumption(List<IAEStack<?>> requests, ItemStack original, ItemStack remaining, int slot)
        throws GridAccessException {
        if (original == null) return remaining == null;
        if (
            remaining != null && (!sameItem(original, remaining) || remaining.stackSize < 0
                || remaining.stackSize > original.stackSize)
        ) return false;
        int used = original.stackSize - (remaining == null ? 0 : remaining.stackSize);
        if (used <= 0) return true;
        IAEItemStack request = AEItemStack.create(original);
        if (getPolicy(false, slot).offered(available(request), limitedMode, fixedMode) < used) return false;
        requests.add(request.setStackSize(used));
        return true;
    }

    private boolean consumption(List<IAEStack<?>> requests, FluidStack original, FluidStack remaining, int slot)
        throws GridAccessException {
        if (original == null) return remaining == null;
        if (
            remaining != null
                && (!sameFluid(original, remaining) || remaining.amount < 0 || remaining.amount > original.amount)
        ) return false;
        int used = original.amount - (remaining == null ? 0 : remaining.amount);
        if (used <= 0) return true;
        IAEFluidStack request = AEFluidStack.create(original);
        if (getPolicy(true, slot).offered(available(request), limitedMode, fixedMode) < used) return false;
        requests.add(request.setStackSize(used));
        return true;
    }

    private void rollback(List<IAEStack<?>> extracted) {
        for (IAEStack<?> stack : extracted) pending.add(new Pending(stack.copy(), true));
        if (extracted.isEmpty()) return;
        host.markDirty();
        try {
            flushPending();
        } catch (GridAccessException ignored) {}
        refreshRequested = true;
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        checkSlot(slot);
        IAEStack<?> extracted = extract(AEItemStack.create(itemFilters[slot]), slot, amount, simulate);
        return extracted instanceof IAEItemStack item ? item.getItemStack() : null;
    }

    public FluidStack extractFluid(int slot, int amount, boolean simulate) {
        checkSlot(slot);
        IAEStack<?> extracted = extract(AEFluidStack.create(fluidFilters[slot]), slot, amount, simulate);
        return extracted instanceof IAEFluidStack fluid ? fluid.getFluidStack() : null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IAEStack<?> extract(IAEStack<?> key, int slot, int amount, boolean simulate) {
        if (
            key == null || amount <= 0
                || !enabled
                || !isOnline()
                || activeRecipe != null
                || transferring
                || !pending.isEmpty()
        ) return null;
        try {
            int offered = getPolicy(key.isFluid(), slot).offered(available(key), limitedMode, fixedMode);
            if (offered <= 0) return null;
            IAEStack request = key.copy().setStackSize(Math.min(amount, offered));
            if (simulate) return inventory(key).extractItems(request, Actionable.SIMULATE, host.getActionSource());
            transferring = true;
            IAEStack actual = Platform.poweredExtraction(host.getProxy().getEnergy(), inventory(key), request,
                host.getActionSource());
            refreshRequested = true;
            return actual;
        } catch (GridAccessException ignored) {
            return null;
        } finally {
            if (!simulate) transferring = false;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void recycle(BusTarget target, TileEntity targetTile) throws GridAccessException {
        long itemBudget = ITEM_IMPORT_LIMIT;
        long fluidBudget = FLUID_IMPORT_LIMIT;
        for (IAEStack<?> stack : target.stock()) {
            if (!pending.isEmpty()) break;
            long budget = stack.isFluid() ? fluidBudget : itemBudget;
            if (budget <= 0) continue;
            long quantity = Math.min(budget,
                target.hasNativeOutputs() ? stack.getStackSize() : recyclableAmount(stack));
            if (quantity <= 0) continue;
            IAEStack simulated = target.extract(stack.copy().setStackSize(quantity), true);
            if (simulated == null || simulated.getStackSize() <= 0) continue;
            IMEInventory network = inventory(stack);
            IAEStack remainder = Platform.poweredInsert(host.getProxy().getEnergy(), network, simulated.copy(),
                host.getActionSource(), Actionable.SIMULATE);
            long accepted = simulated.getStackSize() - (remainder == null ? 0 : remainder.getStackSize());
            if (accepted <= 0) continue;
            IAEStack taken = target.extract(simulated.copy().setStackSize(accepted), false);
            if (taken == null || taken.getStackSize() <= 0) continue;
            pending.add(new Pending(taken, false));
            host.markDirty();
            targetTile.markDirty();
            if (stack.isFluid()) fluidBudget -= taken.getStackSize();
            else itemBudget -= taken.getStackSize();
            flushPending();
            refreshRequested = true;
        }
    }

    /** Marked inputs remain protected; explicit fixed regulation can return stock above the configured amount. */
    public long recyclableAmount(IAEStack<?> stack) {
        if (!recycle || stack == null) return 0;
        int marked = matchingSlot(stack, -1);
        long keep = marked < 0 ? 0
            : fixedMode && regulate ? Math.max(1, getPolicy(stack.isFluid(), marked).batch) : Long.MAX_VALUE;
        return Math.max(0, stack.getStackSize() - keep);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean flushPending() throws GridAccessException {
        for (int index = 0; index < pending.size();) {
            Pending transfer = pending.get(index);
            IMEInventory network = inventory(transfer.stack);
            IAEStack remainder = transfer.rollback
                ? network.injectItems((IAEStack) transfer.stack.copy(), Actionable.MODULATE, host.getActionSource())
                : Platform.poweredInsert(host.getProxy().getEnergy(), network, (IAEStack) transfer.stack.copy(),
                    host.getActionSource());
            if (remainder == null || remainder.getStackSize() <= 0) pending.remove(index);
            else {
                transfer.stack = remainder;
                index++;
            }
            host.markDirty();
        }
        return pending.isEmpty();
    }

    public boolean hasPending() {
        return !pending.isEmpty();
    }

    /** Ghost configuration may be copied independently; escrow is only saved in real block/part data. */
    public void writeSettings(NBTTagCompound tag) {
        NBTTagList items = new NBTTagList();
        NBTTagList fluids = new NBTTagList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (itemFilters[slot] != null) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setInteger("slot", slot);
                entry.setTag("item", itemFilters[slot].writeToNBT(new NBTTagCompound()));
                getPolicy(false, slot).write(entry);
                items.appendTag(entry);
            }
            if (fluidFilters[slot] != null) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setInteger("slot", slot);
                entry.setTag("fluid", fluidFilters[slot].writeToNBT(new NBTTagCompound()));
                getPolicy(true, slot).write(entry);
                fluids.appendTag(entry);
            }
        }
        tag.setTag("stockIOItems", items);
        tag.setTag("stockIOFluids", fluids);
        tag.setBoolean("stockIOEnabled", enabled);
        tag.setBoolean("stockIOLimited", limitedMode);
        tag.setBoolean("stockIOFixed", fixedMode);
        tag.setBoolean("stockIORecycle", recycle);
        tag.setBoolean("stockIORegulate", regulate);
        tag.setBoolean("stockIOAutoItems", autoPullItems);
        tag.setBoolean("stockIOAutoFluids", autoPullFluids);
        tag.setInteger("stockIORefresh", refreshTime);
        tag.setInteger("stockIOMinItems", minItemAutoPull);
        tag.setInteger("stockIOMinFluids", minFluidAutoPull);
        tag.setInteger("stockIOTargetSide", getTargetSide().ordinal());
    }

    public void readSettings(NBTTagCompound tag) {
        if (activeRecipe != null || transferring) return;
        Arrays.fill(itemFilters, null);
        Arrays.fill(fluidFilters, null);
        Arrays.fill(itemPolicies, null);
        Arrays.fill(fluidPolicies, null);
        NBTTagList items = tag.getTagList("stockIOItems", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound entry = items.getCompoundTagAt(i);
            int slot = entry.getInteger("slot");
            ItemStack item = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("item"));
            if (item != null) item.stackSize = 1;
            if (slot < 0 || slot >= SLOT_COUNT || item == null || matchingSlot(AEItemStack.create(item), slot) >= 0)
                continue;
            itemFilters[slot] = item;
            item.stackSize = 1;
            getPolicy(false, slot).read(entry);
        }
        NBTTagList fluids = tag.getTagList("stockIOFluids", 10);
        for (int i = 0; i < fluids.tagCount(); i++) {
            NBTTagCompound entry = fluids.getCompoundTagAt(i);
            int slot = entry.getInteger("slot");
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(entry.getCompoundTag("fluid"));
            if (fluid != null) fluid.amount = 1;
            if (slot < 0 || slot >= SLOT_COUNT || fluid == null || matchingSlot(AEFluidStack.create(fluid), slot) >= 0)
                continue;
            fluidFilters[slot] = fluid;
            fluid.amount = 1;
            getPolicy(true, slot).read(entry);
        }
        enabled = !tag.hasKey("stockIOEnabled") || tag.getBoolean("stockIOEnabled");
        limitedMode = tag.getBoolean("stockIOLimited");
        fixedMode = tag.getBoolean("stockIOFixed");
        recycle = !tag.hasKey("stockIORecycle") || tag.getBoolean("stockIORecycle");
        regulate = tag.getBoolean("stockIORegulate");
        autoPullItems = tag.getBoolean("stockIOAutoItems");
        autoPullFluids = tag.getBoolean("stockIOAutoFluids");
        refreshTime = tag.hasKey("stockIORefresh") ? Math.max(1, tag.getInteger("stockIORefresh")) : 20;
        minItemAutoPull = tag.hasKey("stockIOMinItems") ? Math.max(1, tag.getInteger("stockIOMinItems")) : 64;
        minFluidAutoPull = tag.hasKey("stockIOMinFluids") ? Math.max(1, tag.getInteger("stockIOMinFluids")) : 1000;
        int side = tag.hasKey("stockIOTargetSide") ? tag.getInteger("stockIOTargetSide")
            : ForgeDirection.NORTH.ordinal();
        if (side >= 0 && side < ForgeDirection.VALID_DIRECTIONS.length)
            host.setTargetSide(ForgeDirection.getOrientation(side));
        clearAmounts();
        refreshRequested = true;
        lastWorkTick = Long.MIN_VALUE;
        lastRefreshTick = Long.MIN_VALUE;
    }

    public void writeContents(NBTTagCompound tag) {
        writeSettings(tag);
        tag.setTag("stockIOEscrow", writeEscrow());
    }

    /**
     * Returns only owned resources for a real removal, leaving configuration behind. Drop previews never
     * transfer ownership or clear the pending queue; ordinary interfaces return null so their items stack.
     *
     * @return an independent escrow-only tag, or null when no actual resource remains
     */
    public NBTTagCompound getRemovalContents() {
        NBTTagList escrow = writeEscrow();
        if (escrow.tagCount() == 0) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("stockIOEscrow", escrow);
        return tag;
    }

    private NBTTagList writeEscrow() {
        NBTTagList escrow = new NBTTagList();
        for (Pending transfer : pending) {
            if (transfer.stack == null || transfer.stack.getStackSize() <= 0) continue;
            NBTTagCompound entry = new NBTTagCompound();
            Platform.writeStackNBT(transfer.stack, entry, true);
            entry.setBoolean("rollback", transfer.rollback);
            escrow.appendTag(entry.copy());
        }
        return escrow;
    }

    public void readContents(NBTTagCompound tag) {
        readSettings(tag);
        pending.clear();
        NBTTagList escrow = tag.getTagList("stockIOEscrow", 10);
        for (int index = 0; index < escrow.tagCount(); index++) {
            NBTTagCompound entry = escrow.getCompoundTagAt(index);
            IAEStack<?> stack = Platform.readStackNBT(entry, false);
            if (stack != null && stack.getStackSize() > 0)
                pending.add(new Pending(stack, entry.getBoolean("rollback")));
        }
    }

    static boolean sameItem(ItemStack a, ItemStack b) {
        return a == null ? b == null : b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    static boolean sameFluid(FluidStack a, FluidStack b) {
        return a == null ? b == null : b != null && a.isFluidEqual(b);
    }

    private static final class Pending {

        private IAEStack<?> stack;
        private final boolean rollback;

        private Pending(IAEStack<?> stack, boolean rollback) {
            this.stack = stack;
            this.rollback = rollback;
        }
    }
}
