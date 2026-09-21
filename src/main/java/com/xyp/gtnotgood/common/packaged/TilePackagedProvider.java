// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeBase;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.MachineSource;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

/**
 * 36-pattern wireless provider with real altar lanes and a persistent nine-slot return inventory.
 * Ingredients reside in the target after acceptance; only actual completed outputs are ever imported.
 * A missing chunk pauses a lane rather than deleting its binding or loading the chunk.
 */
public final class TilePackagedProvider extends TileMEBridgeBase
    implements IInventory, ICraftingProvider, IGuiHolder<PosGuiData>, appeng.api.util.IInterfaceViewable {

    public static final int PATTERNS = 36;
    public static final int CORE = 45;
    public static final int MAX_TARGETS = 1024;
    private final ItemStack[] inventory = new ItemStack[46];
    final List<PackagedTarget> targets = new ArrayList<>();
    private final List<PackagedTarget> visibleTargets = Collections.unmodifiableList(targets);
    private final List<Job> jobs = new ArrayList<>();
    private final List<ICraftingPatternDetails> patterns = new ArrayList<>();
    private boolean patternsDirty = true;
    boolean autoReturn;
    boolean networkEssentia;
    int essentiaSpeed = 8;
    private String essentiaIdentity = java.util.UUID.randomUUID()
        .toString();
    AltarStatus altarStatus = AltarStatus.IDLE;
    int priority;
    boolean terminalVisible = true;
    PackagedCraftingLock craftingLock = PackagedCraftingLock.NONE;
    private boolean pulseLocked;
    private boolean previousRedstone;
    private ItemStack unlockResult;
    private int cursor;
    private int collectionCursor;
    private long nextDispatch;
    private int failures;
    private static final int[] RETRY = { 1, 2, 3, 4, 5, 8, 10, 20, 40 };

    @Override
    protected ItemStack getVisualRepresentation() {
        return GTNGItemList.WirelessPackagedPatternProvider.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        boolean creating = gridProxy == null;
        AENetworkProxy proxy = super.getProxy();
        if (creating) {
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setIdlePowerUsage(10);
        }
        return proxy;
    }

    /** Configuration is owner-controlled; the client never decides authorization. */
    public boolean canConfigure(EntityPlayer player) {
        return player != null
            && (worldObj != null && worldObj.isRemote || ownerName.equals(player.getCommandSenderName()));
    }

    public boolean bind(PackagedTarget target) {
        if (!isServerSide() || target == null
            || target.dimension != worldObj.provider.dimensionId
            || target.resolve(worldObj) == null
            || target.resolve(worldObj) == this
            || targets.size() >= MAX_TARGETS) return false;
        for (PackagedTarget existing : targets) if (existing.sameBlock(target)) return false;
        targets.add(target);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        nextDispatch = 0;
        getProxy().setIdlePowerUsage(10 + targets.size());
        markDirty();
        return true;
    }

    boolean removeTarget(int index) {
        if (!isServerSide() || index < 0 || index >= targets.size()) return false;
        PackagedTarget target = targets.get(index);
        if (busy(target)) return false;
        targets.remove(index);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        getProxy().setIdlePowerUsage(10 + targets.size());
        markDirty();
        return true;
    }

    public int queuedJobs() {
        return jobs.size();
    }

    /** Read-only live binding view for the connector overlay; never resolves or loads target chunks. */
    public List<PackagedTarget> connections() {
        return visibleTargets;
    }

    /** Sends only overlay coordinates to chunk watchers, never inventories, jobs or AE proxy state. */
    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList connections = new NBTTagList();
        for (PackagedTarget target : targets) connections.appendTag(target.write());
        tag.setTag("WirelessConnections", connections);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    /** Applies the bounded server snapshot without invoking persistent/server-side NBT restoration. */
    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
        if (worldObj == null || !worldObj.isRemote) return;
        NBTTagList connections = packet.func_148857_g()
            .getTagList("WirelessConnections", 10);
        targets.clear();
        for (int i = 0; i < Math.min(connections.tagCount(), MAX_TARGETS); i++) {
            PackagedTarget target = PackagedTarget.read(connections.getCompoundTagAt(i));
            if (target != null && target.dimension == worldObj.provider.dimensionId) targets.add(target);
        }
    }

    /** Stable identity prevents a replacement block at the same coordinates from paying for an old altar job. */
    public String essentiaIdentity() {
        return essentiaIdentity;
    }

    /** Changes the source only while idle; existing worlds keep their original altar-supply behavior. */
    public boolean setNetworkEssentia(boolean enabled) {
        if (!isServerSide() || !jobs.isEmpty()) return false;
        if (enabled && !com.xyp.gtnotgood.utils.enums.ModList.ThaumicEnergistics.isModLoaded()) return false;
        networkEssentia = enabled;
        markDirty();
        return true;
    }

    /** Bounded per-cycle source throughput; changing an active transaction's rate is intentionally disallowed. */
    public boolean cycleEssentiaSpeed() {
        if (!isServerSide() || !jobs.isEmpty()) return false;
        essentiaSpeed = essentiaSpeed >= 32 ? 1 : essentiaSpeed * 2;
        markDirty();
        return true;
    }

    public boolean busy(PackagedTarget target) {
        for (Job job : jobs) if (job.target.sameBlock(target)) return true;
        return false;
    }

    /**
     * Allows bounded prefetch only for the exact same encoded recipe and core. Legacy receipts lack a pattern
     * snapshot and must finish before prefetch resumes. The adapter still checks every real inventory atomically.
     */
    boolean canQueue(PackagedTarget target, PackagedCoreRegistry.Adapter adapter, ItemStack pattern) {
        int count = 0;
        for (Job job : jobs) {
            if (!job.target.sameBlock(target)) continue;
            if (PackagedCoreRegistry.entries()
                .get(job.core) != adapter || job.pattern == null
                || !sameItem(job.pattern, pattern)) return false;
            count++;
        }
        return count < adapter.maxInFlight();
    }

    /**
     * Explicitly interrupts a loaded lane through its adapter, then releases all of its prefetched receipts.
     * Does not fabricate refunds or remove buffered materials. Non-machine adapters retain stopped-job recovery.
     * The user must cancel the corresponding unfinished AE CPU request separately.
     * 
     * @param index current target row on the server
     * @return whether an interrupted receipt was removed
     */
    boolean releaseInterrupted(int index) {
        if (!isServerSide() || index < 0 || index >= targets.size()) return false;
        PackagedTarget target = targets.get(index);
        if (!worldObj.getChunkProvider()
            .chunkExists(target.x >> 4, target.z >> 4)) return false;
        for (int i = 0; i < jobs.size(); i++) {
            Job job = jobs.get(i);
            if (!job.target.sameBlock(target)) continue;
            var adapter = PackagedCoreRegistry.entries()
                .get(job.core);
            if (adapter == null || jobs.stream()
                .anyMatch(other -> other.target.sameBlock(target) && !other.core.equals(job.core))) return false;
            if (!adapter.interrupt(this, target.resolve(worldObj), job.expected)) return false;
            jobs.removeIf(other -> other.target.sameBlock(target));
            nextDispatch = 0;
            failures = 0;
            resetCraftingLock();
            if (jobs.isEmpty()) altarStatus = AltarStatus.IDLE;
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Changes the user setting and drops the previous waiting condition, while keeping every accepted altar job.
     * 
     * @param mode one of the five upstream lock modes
     */
    public void setCraftingLock(PackagedCraftingLock mode) {
        craftingLock = mode;
        resetCraftingLock();
    }

    /** Resets the waiting condition, as the upstream lock-reason button does; never cancels altar jobs. */
    public void resetCraftingLock() {
        pulseLocked = false;
        unlockResult = null;
        markDirty();
    }

    public boolean craftingLocked() {
        boolean powered = worldObj != null && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
        return switch (craftingLock) {
            case NONE -> false;
            case HIGH -> powered;
            case LOW -> !powered;
            case PULSE -> pulseLocked;
            case RESULT -> unlockResult != null;
        };
    }

    /** Samples an edge, not a sustained signal; keeping this state in NBT prevents reloads from inventing pulses. */
    void updateCraftingLockPower() {
        if (craftingLock != PackagedCraftingLock.PULSE) return;
        boolean powered = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
        if (powered && !previousRedstone && pulseLocked) resetCraftingLock();
        if (powered != previousRedstone && pulseLocked) markDirty();
        previousRedstone = powered;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!isServerSide()) return;
        updateCraftingLockPower();
        if (patternsDirty) {
            try {
                getProxy().getGrid()
                    .postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
                patternsDirty = false;
            } catch (GridAccessException ignored) {}
        }
        var adapter = PackagedCoreRegistry.get(inventory[CORE]);
        boolean fast = adapter != null && adapter.maxInFlight() > 1 && !jobs.isEmpty();
        if ((fast || worldObj.getTotalWorldTime() % 20 == 0) && getProxy().isActive()) {
            flushReturns();
            if (autoReturn) collectCompleted();
            flushReturns();
        }
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper helper) {
        patterns.clear();
        if (PackagedCoreRegistry.get(inventory[CORE]) == null) return;
        for (int slot = 0; slot < PATTERNS; slot++) {
            ItemStack stack = inventory[slot];
            if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem item)) continue;
            ICraftingPatternDetails details = item.getPatternForItem(stack, worldObj);
            if (details == null || details.isCraftable()) continue;
            details.setPriority(priority);
            patterns.add(details);
            helper.addCraftingOption(this, details);
        }
    }

    @Override
    public boolean isBusy() {
        return !isServerSide() || !getProxy().isActive()
            || craftingLocked()
            || targets.isEmpty()
            || PackagedCoreRegistry.get(inventory[CORE]) == null
            || jobs.size() >= targets.size() * PackagedCoreRegistry.get(inventory[CORE])
                .maxInFlight()
            || worldObj.getTotalWorldTime() < nextDispatch;
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting table) {
        if (isBusy() || !patterns.contains(details)) return false;
        PackagedCoreRegistry.Adapter adapter = PackagedCoreRegistry.get(inventory[CORE]);
        int count = targets.size();
        // Bound work even with 1024 remote targets; successive retries continue from the next lane.
        for (int i = 0; i < Math.min(count, 16); i++) {
            PackagedTarget target = targets.get(Math.floorMod(cursor++, count));
            if (!canQueue(target, adapter, details.getPattern()) || !adapter.accepts(target.resolve(worldObj)))
                continue;
            ItemStack expected = adapter.dispatch(this, target, details, table);
            if (expected == null) continue;
            jobs.add(
                new Job(
                    target,
                    ((ItemPackagedCore) inventory[CORE].getItem()).adapterId,
                    expected.copy(),
                    details.getPattern()));
            altarStatus = AltarStatus.RUNNING;
            if (craftingLock == PackagedCraftingLock.PULSE) {
                pulseLocked = true;
                previousRedstone = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
            }
            if (craftingLock == PackagedCraftingLock.RESULT) unlockResult = expected.copy();
            failures = 0;
            nextDispatch = 0;
            markDirty();
            return true;
        }
        nextDispatch = worldObj.getTotalWorldTime()
            + (adapter.maxInFlight() > 1 && !jobs.isEmpty() ? 1 : RETRY[failures]);
        failures = Math.min(failures + 1, RETRY.length - 1);
        return false;
    }

    /** Imports only the expected result of a lane accepted by this provider, never arbitrary altar contents. */
    private void collectCompleted() {
        // Keep fast polling bounded even with hundreds of connected lines; rotate to avoid starvation.
        int checks = Math.min(jobs.size(), 64);
        for (int check = 0; check < checks && !jobs.isEmpty(); check++) {
            int index = Math.floorMod(collectionCursor--, jobs.size());
            Job job = jobs.get(index);
            PackagedCoreRegistry.Adapter adapter = PackagedCoreRegistry.entries()
                .get(job.core);
            if (adapter == null) continue;
            var targetTile = job.target.resolve(worldObj);
            IInventory source = adapter.output(targetTile);
            if (source == null) continue;
            int sourceSlot = -1;
            for (int slot = 0; slot < source.getSizeInventory(); slot++) {
                ItemStack actual = source.getStackInSlot(slot);
                if (sameItem(actual, job.expected) && actual.stackSize >= job.expected.stackSize) {
                    sourceSlot = slot;
                    break;
                }
            }
            if (sourceSlot < 0) {
                if (adapter.canRelease(targetTile, job.expected)) altarStatus = AltarStatus.INTERRUPTED;
                continue;
            }
            int destination = -1;
            for (int slot = PATTERNS; slot < CORE; slot++) {
                if (inventory[slot] == null) {
                    destination = slot;
                    break;
                }
            }
            if (destination < 0) {
                altarStatus = AltarStatus.RETURNS_FULL;
                return;
            }
            // Vanilla/TC inventory calls are synchronous on the server thread. Retain rejected network output locally.
            ItemStack extracted = source.decrStackSize(sourceSlot, job.expected.stackSize);
            if (extracted == null) continue;
            inventory[destination] = extracted;
            source.markDirty();
            if (source instanceof net.minecraft.tileentity.TileEntity pedestal) {
                worldObj.markBlockForUpdate(pedestal.xCoord, pedestal.yCoord, pedestal.zCoord);
            }
            jobs.remove(index);
            nextDispatch = 0;
            failures = 0;
            if (jobs.isEmpty()) altarStatus = AltarStatus.IDLE;
            markDirty();
        }
    }

    private void flushReturns() {
        try {
            for (int slot = PATTERNS; slot < CORE; slot++) {
                if (inventory[slot] == null) continue;
                var left = Platform.poweredInsert(
                    getProxy().getEnergy(),
                    getProxy().getStorage()
                        .getItemInventory(),
                    AEItemStack.create(inventory[slot]),
                    new MachineSource(this));
                ItemStack remainder = left == null ? null : left.getItemStack();
                if (remainder == null || remainder.stackSize != inventory[slot].stackSize) {
                    if (unlockResult != null && sameItem(unlockResult, inventory[slot])) {
                        unlockResult.stackSize -= inventory[slot].stackSize
                            - (remainder == null ? 0 : remainder.stackSize);
                        if (unlockResult.stackSize <= 0) unlockResult = null;
                    }
                    inventory[slot] = remainder;
                    markDirty();
                }
            }
        } catch (GridAccessException ignored) {}
    }

    public static boolean sameItem(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    public void setPriority(int value) {
        priority = value;
        patternsDirty = true;
        markDirty();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        settings.canInteractWith(this::isUseableByPlayer);
        return PackagedProviderGui.build(this, sync);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new com.cleanroommc.modularui.screen.ModularScreen(
            com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD,
            panel);
    }

    @Override
    public void securityBreak() {
        if (isServerSide()) worldObj.func_147480_a(xCoord, yCoord, zCoord, true);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        Arrays.fill(inventory, null);
        NBTTagList items = tag.getTagList("Inventory", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound item = items.getCompoundTagAt(i);
            int slot = item.getInteger("Slot");
            if (slot >= 0 && slot < inventory.length) inventory[slot] = ItemStack.loadItemStackFromNBT(item);
        }
        targets.clear();
        NBTTagList connections = tag.getTagList("WirelessConnections", 10);
        for (int i = 0; i < Math.min(MAX_TARGETS, connections.tagCount()); i++) {
            PackagedTarget target = PackagedTarget.read(connections.getCompoundTagAt(i));
            if (target != null && targets.stream()
                .noneMatch(target::sameBlock)) targets.add(target);
        }
        jobs.clear();
        NBTTagList savedJobs = tag.getTagList("GTNGJobs", 10);
        for (int i = 0; i < Math.min(MAX_TARGETS * 17, savedJobs.tagCount()); i++) {
            NBTTagCompound entry = savedJobs.getCompoundTagAt(i);
            PackagedTarget target = PackagedTarget.read(entry);
            ItemStack expected = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Expected"));
            if (target != null && expected != null && expected.stackSize > 0) {
                jobs.add(
                    new Job(
                        target,
                        entry.getString("Core"),
                        expected,
                        ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Pattern"))));
            }
        }
        autoReturn = tag.getBoolean("AutoReturn");
        networkEssentia = tag.getBoolean("NetworkEssentia");
        essentiaSpeed = tag.hasKey("EssentiaSpeed")
            ? Integer.highestOneBit(Math.max(1, Math.min(32, tag.getInteger("EssentiaSpeed"))))
            : 8;
        if (tag.hasKey("EssentiaIdentity")) essentiaIdentity = tag.getString("EssentiaIdentity");
        priority = tag.getInteger("Priority");
        terminalVisible = !tag.hasKey("TerminalVisible") || tag.getBoolean("TerminalVisible");
        craftingLock = PackagedCraftingLock.read(tag.getInteger("CraftingLock"));
        pulseLocked = tag.getBoolean("PulseLocked");
        previousRedstone = tag.getBoolean("PreviousRedstone");
        unlockResult = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("UnlockResult"));
        altarStatus = jobs.isEmpty() ? AltarStatus.IDLE : AltarStatus.RUNNING;
        cursor = 0;
        nextDispatch = 0;
        failures = 0;
        patternsDirty = true;
        getProxy().setIdlePowerUsage(10 + targets.size());
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < inventory.length; slot++) {
            if (inventory[slot] == null) continue;
            NBTTagCompound item = inventory[slot].writeToNBT(new NBTTagCompound());
            item.setInteger("Slot", slot);
            items.appendTag(item);
        }
        tag.setTag("Inventory", items);
        NBTTagList connections = new NBTTagList();
        for (PackagedTarget target : targets) connections.appendTag(target.write());
        tag.setTag("WirelessConnections", connections);
        NBTTagList savedJobs = new NBTTagList();
        for (Job job : jobs) {
            NBTTagCompound entry = job.target.write();
            entry.setString("Core", job.core);
            entry.setTag("Expected", job.expected.writeToNBT(new NBTTagCompound()));
            if (job.pattern != null) entry.setTag("Pattern", job.pattern.writeToNBT(new NBTTagCompound()));
            savedJobs.appendTag(entry);
        }
        tag.setTag("GTNGJobs", savedJobs);
        tag.setBoolean("AutoReturn", autoReturn);
        tag.setBoolean("NetworkEssentia", networkEssentia);
        tag.setInteger("EssentiaSpeed", essentiaSpeed);
        tag.setString("EssentiaIdentity", essentiaIdentity);
        tag.setInteger("Priority", priority);
        tag.setBoolean("TerminalVisible", terminalVisible);
        tag.setInteger("CraftingLock", craftingLock.ordinal());
        tag.setBoolean("PulseLocked", pulseLocked);
        tag.setBoolean("PreviousRedstone", previousRedstone);
        if (unlockResult != null) tag.setTag("UnlockResult", unlockResult.writeToNBT(new NBTTagCompound()));
        else tag.removeTag("UnlockResult");
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (slot == CORE && !jobs.isEmpty()) return null;
        if (inventory[slot] == null || count <= 0) return null;
        ItemStack taken = inventory[slot].splitStack(Math.min(count, inventory[slot].stackSize));
        if (inventory[slot].stackSize <= 0) inventory[slot] = null;
        if (slot < PATTERNS || slot == CORE) patternsDirty = true;
        markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack taken = inventory[slot];
        inventory[slot] = null;
        if (slot < PATTERNS || slot == CORE) patternsDirty = true;
        markDirty();
        return taken;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (slot < PATTERNS || slot == CORE) patternsDirty = true;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "tile.wireless_packaged_pattern_provider.name";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && canConfigure(player)
            && player.getDistanceSq(xCoord + .5, yCoord + .5, zCoord + .5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null) return false;
        return slot < PATTERNS ? stack.getItem() instanceof ICraftingPatternItem
            : slot == CORE && stack.getItem() instanceof ItemPackagedCore && jobs.isEmpty();
    }

    @Override
    public int rows() {
        return 4;
    }

    @Override
    public int rowSize() {
        return 9;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getName() {
        return getInventoryName();
    }

    @Override
    public net.minecraft.tileentity.TileEntity getTileEntity() {
        return this;
    }

    @Override
    public boolean shouldDisplay() {
        return terminalVisible;
    }

    @Override
    public ItemStack getSelfRep() {
        return getVisualRepresentation();
    }

    /** A strict 36-slot view prevents a remote terminal from reaching the return inventory or core. */
    @Override
    public IInventory getPatterns() {
        return new net.minecraft.inventory.InventoryBasic(getInventoryName(), false, PATTERNS) {

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot >= 0 && slot < PATTERNS ? inventory[slot] : null;
            }

            @Override
            public ItemStack decrStackSize(int slot, int count) {
                return slot >= 0 && slot < PATTERNS ? TilePackagedProvider.this.decrStackSize(slot, count) : null;
            }

            @Override
            public void setInventorySlotContents(int slot, ItemStack stack) {
                if (slot >= 0 && slot < PATTERNS && (stack == null || isItemValidForSlot(slot, stack))) {
                    TilePackagedProvider.this.setInventorySlotContents(slot, stack);
                }
            }

            @Override
            public int getInventoryStackLimit() {
                return 1;
            }

            @Override
            public boolean isItemValidForSlot(int slot, ItemStack stack) {
                return slot >= 0 && slot < PATTERNS
                    && stack != null
                    && stack.stackSize == 1
                    && stack.getItem() instanceof ICraftingPatternItem;
            }

            @Override
            public void markDirty() {
                patternsDirty = true;
                TilePackagedProvider.this.markDirty();
            }
        };
    }

    /** Durable receipt for ingredients already accepted by a real altar. */
    private static final class Job {

        final PackagedTarget target;
        final String core;
        final ItemStack expected;
        final ItemStack pattern;

        Job(PackagedTarget target, String core, ItemStack expected, ItemStack pattern) {
            this.target = target;
            this.core = core;
            this.expected = expected;
            this.pattern = pattern == null ? null : pattern.copy();
        }
    }
}
