package com.xyp.gtnotgood.common.flux;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.mebridge.MEBridgeChannelManager;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeReceiver;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.MachineSource;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/**
 * Wireless stock keeper and automatic output collector on an existing ME bridge channel.
 * Export uses filters and targets (zero means fill); import collects accessible outputs without filters.
 * Extraction uses AE power/security. Rejected deliveries are saved before retrying or returning them to ME.
 * No external inventory is exposed, preventing a second hopper or pipe from taking reserved cargo.
 */
public final class TileFluxLogistics extends TileMEBridgeReceiver {

    public static final int SLOTS = 9;
    /** Forge inventory/fluid operations accept int quantities; this is an API bound, not a rate setting. */
    private static final int MAX_API_AMOUNT = Integer.MAX_VALUE;
    final ItemStack[] itemFilters = new ItemStack[SLOTS];
    final FluidStack[] fluidFilters = new FluidStack[SLOTS];
    final long[] itemTargets = new long[SLOTS];
    final long[] fluidTargets = new long[SLOTS];
    private ItemStack pendingItem;
    private FluidStack pendingFluid;
    private UUID owner;
    private boolean transferring;
    boolean online;
    private int cursor;
    private int importCursor;
    private boolean importing;

    public boolean importing() {
        return importing;
    }

    public void toggleDirection() {
        if (!isServerSide()) return;
        importing = !importing;
        markDirty();
    }

    /** A resource has one target row; replacing a duplicate removes its previous sample. */
    void setItemFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) return;
        for (int i = 0; i < SLOTS; i++) {
            if (i != slot && LogisticsStock.same(itemFilters[i], stack)) itemFilters[i] = null;
        }
        itemFilters[slot] = stack == null ? null : stack.copy();
        if (itemFilters[slot] != null) itemFilters[slot].stackSize = 1;
        markDirty();
    }

    void setFluidFilter(int slot, FluidStack stack) {
        if (slot < 0 || slot >= SLOTS) return;
        for (int i = 0; i < SLOTS; i++) {
            if (i != slot && stack != null && stack.isFluidEqual(fluidFilters[i])) fluidFilters[i] = null;
        }
        fluidFilters[slot] = stack == null ? null : stack.copy();
        if (fluidFilters[slot] != null) fluidFilters[slot].amount = 1;
        markDirty();
    }

    @Override
    protected ItemStack getVisualRepresentation() {
        return GTNGItemList.FluxLogisticsPlug.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        boolean creating = gridProxy == null;
        AENetworkProxy proxy = super.getProxy();
        if (creating) {
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setIdlePowerUsage(1);
            proxy.setValidSides(EnumSet.noneOf(ForgeDirection.class));
        }
        return proxy;
    }

    public void placedBy(EntityPlayer player) {
        owner = player.getUniqueID();
        setOwnerName(player.getCommandSenderName());
    }

    public boolean canEdit(EntityPlayer player) {
        return player != null && owner != null && owner.equals(player.getUniqueID());
    }

    public ForgeDirection outputSide() {
        return ForgeDirection.getOrientation(getBlockMetadata() % 6);
    }

    public TileEntity destination() {
        if (worldObj == null) return null;
        ForgeDirection side = outputSide();
        int x = xCoord + side.offsetX, y = yCoord + side.offsetY, z = zCoord + side.offsetZ;
        return worldObj.blockExists(x, y, z) ? worldObj.getTileEntity(x, y, z) : null;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!isServerSide() || transferring) return;
        var channel = MEBridgeChannelManager.get(getChannelName());
        var sender = channel == null ? null : channel.getSenderTile();
        var node = getGridNode(ForgeDirection.UNKNOWN);
        var remote = sender == null ? null : sender.getGridNode(ForgeDirection.UNKNOWN);
        online = isConnected() && getProxy().isActive()
            && node != null
            && remote != null
            && node.getGrid() == remote.getGrid();
        if (!online) return;
        transferring = true;
        try {
            returnPending();
            TileEntity target = destination();
            if (target == null || target instanceof TileFluxLogistics) return;
            ForgeDirection input = outputSide().getOpposite();
            IInventory inventory = target instanceof IInventory slots ? slots : null;
            if (worldObj.getBlock(
                target.xCoord,
                target.yCoord,
                target.zCoord) instanceof net.minecraft.block.BlockChest chest) {
                inventory = chest.func_149951_m(worldObj, target.xCoord, target.yCoord, target.zCoord);
            }
            if (importing) {
                if (pendingItem == null && inventory != null) importItems(inventory, input);
                if (pendingFluid == null && target instanceof IFluidHandler tank) importFluids(tank, input);
                return;
            }
            for (int n = 0; n < SLOTS; n++) {
                int slot = (cursor + n) % SLOTS;
                if (pendingItem == null && inventory != null) exportItem(inventory, input, slot);
                if (pendingFluid == null && target instanceof IFluidHandler tank) exportFluid(tank, input, slot);
            }
            cursor = (cursor + 1) % SLOTS;
        } catch (GridAccessException ignored) {
            online = false;
        } finally {
            transferring = false;
        }
    }

    /** Refund remainders first; if AE cannot accept them, keep durable cargo and suspend that resource type. */
    private void returnPending() throws GridAccessException {
        MachineSource source = new MachineSource(this);
        if (pendingItem != null) {
            var remainder = Platform.poweredInsert(
                getProxy().getEnergy(),
                getProxy().getStorage()
                    .getItemInventory(),
                AEItemStack.create(pendingItem),
                source);
            pendingItem = remainder == null ? null : remainder.getItemStack();
            markDirty();
        }
        if (pendingFluid != null) {
            var remainder = Platform.poweredInsert(
                getProxy().getEnergy(),
                getProxy().getStorage()
                    .getFluidInventory(),
                AEFluidStack.create(pendingFluid),
                source);
            pendingFluid = remainder == null ? null : remainder.getFluidStack();
            markDirty();
        }
    }

    private void exportItem(IInventory inventory, ForgeDirection side, int slot) throws GridAccessException {
        ItemStack filter = itemFilters[slot];
        if (filter == null) return;
        long present = itemTargets[slot] == 0 ? 0 : LogisticsStock.count(inventory, filter);
        int missing = LogisticsStock.missing(itemTargets[slot], present, MAX_API_AMOUNT);
        if (missing == 0) return;
        ItemStack offer = filter.copy();
        offer.stackSize = missing;
        int capacity = LogisticsStock.insert(inventory, side.ordinal(), offer, true);
        if (capacity == 0) return;
        var request = AEItemStack.create(filter);
        request.setStackSize(capacity);
        var extracted = Platform.poweredExtraction(
            getProxy().getEnergy(),
            getProxy().getStorage()
                .getItemInventory(),
            request,
            new MachineSource(this));
        if (extracted == null || extracted.getStackSize() <= 0) return;
        pendingItem = extracted.getItemStack();
        markDirty();
        int accepted = LogisticsStock.insert(inventory, side.ordinal(), pendingItem, false);
        pendingItem.stackSize -= accepted;
        if (pendingItem.stackSize <= 0) pendingItem = null;
        markDirty();
    }

    /** Auto-collect only slots advertised for this face and approved by the container's extraction predicate. */
    private void importItems(IInventory inventory, ForgeDirection side) throws GridAccessException {
        int[] slots = LogisticsStock.accessibleSlots(inventory, side.ordinal());
        if (slots == null || slots.length == 0) return;
        int start = importCursor;
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        for (int i = 0; i < slots.length && pendingItem == null; i++) {
            int index = Math.floorMod(start + i, slots.length);
            int slot = slots[index];
            if (!visited.add(slot)) continue;
            ItemStack stack = LogisticsStock.extractable(inventory, slot, side.ordinal());
            if (stack == null) continue;
            var offer = AEItemStack.create(stack);
            var remainder = Platform.poweredInsert(
                getProxy().getEnergy(),
                getProxy().getStorage()
                    .getItemInventory(),
                offer,
                new MachineSource(this),
                Actionable.SIMULATE);
            int accepted = (int) (offer.getStackSize() - (remainder == null ? 0 : remainder.getStackSize()));
            if (accepted <= 0) continue;
            pendingItem = inventory.decrStackSize(slot, accepted);
            inventory.markDirty();
            markDirty();
            importCursor = (index + 1) % slots.length;
            returnPending();
        }
    }

    /** Auto-collect drainable tank contents; simulate network capacity before removing any source fluid. */
    private void importFluids(IFluidHandler tank, ForgeDirection side) throws GridAccessException {
        FluidTankInfo[] tanks = tank.getTankInfo(side);
        if (tanks == null || tanks.length == 0) {
            importFluid(tank, side, tank.drain(side, MAX_API_AMOUNT, false));
            return;
        }
        int start = cursor;
        for (int i = 0; i < tanks.length && pendingFluid == null; i++) {
            FluidTankInfo info = tanks[(start + i) % tanks.length];
            if (info != null && importFluid(tank, side, info.fluid)) {
                cursor = (start + i + 1) % tanks.length;
            }
        }
    }

    private boolean importFluid(IFluidHandler tank, ForgeDirection side, FluidStack sample) throws GridAccessException {
        if (sample == null || sample.amount <= 0 || !tank.canDrain(side, sample.getFluid())) return false;
        FluidStack wanted = sample.copy();
        wanted.amount = MAX_API_AMOUNT;
        FluidStack available = tank.drain(side, wanted, false);
        if (available == null || available.amount <= 0 || !sample.isFluidEqual(available)) return false;
        var offer = AEFluidStack.create(available);
        var remainder = Platform.poweredInsert(
            getProxy().getEnergy(),
            getProxy().getStorage()
                .getFluidInventory(),
            offer,
            new MachineSource(this),
            Actionable.SIMULATE);
        int accepted = (int) (offer.getStackSize() - (remainder == null ? 0 : remainder.getStackSize()));
        if (accepted <= 0) return false;
        wanted.amount = accepted;
        pendingFluid = tank.drain(side, wanted, true);
        markDirty();
        returnPending();
        return true;
    }

    private void exportFluid(IFluidHandler tank, ForgeDirection side, int slot) throws GridAccessException {
        FluidStack filter = fluidFilters[slot];
        if (filter == null) return;
        long present = 0;
        FluidTankInfo[] tanks = tank.getTankInfo(side);
        // A positive target needs observable stock; never guess that an opaque tank is empty.
        if (fluidTargets[slot] > 0 && (tanks == null || tanks.length == 0)) return;
        if (tanks != null) for (FluidTankInfo info : tanks) {
            if (info != null && info.fluid != null && filter.isFluidEqual(info.fluid)) present += info.fluid.amount;
        }
        int missing = LogisticsStock.missing(fluidTargets[slot], present, MAX_API_AMOUNT);
        if (missing == 0) return;
        FluidStack offer = filter.copy();
        offer.amount = missing;
        // GT canFill probes with 1 mB, but steam power accepts pairs of mB. Simulate the actual offer instead;
        // sided fill still enforces the machine's input face, covers and available capacity.
        int capacity = Math.max(0, Math.min(missing, tank.fill(side, offer, false)));
        if (capacity == 0) return;
        var request = AEFluidStack.create(filter);
        request.setStackSize(capacity);
        var extracted = Platform.poweredExtraction(
            getProxy().getEnergy(),
            getProxy().getStorage()
                .getFluidInventory(),
            request,
            new MachineSource(this));
        if (extracted == null || extracted.getStackSize() <= 0) return;
        pendingFluid = extracted.getFluidStack();
        markDirty();
        int accepted = Math.max(0, Math.min(pendingFluid.amount, tank.fill(side, pendingFluid.copy(), true)));
        pendingFluid.amount -= accepted;
        if (pendingFluid.amount == 0) pendingFluid = null;
        markDirty();
    }

    /** Stores portable configuration and real undelivered cargo; player identity is deliberately excluded. */
    public void writeContents(NBTTagCompound tag) {
        tag.setString("logisticsChannel", getChannelName());
        tag.setBoolean("logisticsImport", importing);
        for (int i = 0; i < SLOTS; i++) {
            if (itemFilters[i] != null) tag.setTag("item" + i, itemFilters[i].writeToNBT(new NBTTagCompound()));
            if (fluidFilters[i] != null) tag.setTag("fluid" + i, fluidFilters[i].writeToNBT(new NBTTagCompound()));
            tag.setLong("items" + i, itemTargets[i]);
            tag.setLong("fluids" + i, fluidTargets[i]);
        }
        if (pendingItem != null) {
            tag.setTag("pendingItem", pendingItem.writeToNBT(new NBTTagCompound()));
            // Vanilla's ItemStack Count is a byte and cannot preserve a bulk remainder.
            tag.setInteger("pendingItemCount", pendingItem.stackSize);
        }
        if (pendingFluid != null) tag.setTag("pendingFluid", pendingFluid.writeToNBT(new NBTTagCompound()));
    }

    public void readContents(NBTTagCompound tag) {
        setChannelName(tag.getString("logisticsChannel"));
        importing = tag.getBoolean("logisticsImport");
        for (int i = 0; i < SLOTS; i++) {
            itemFilters[i] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item" + i));
            fluidFilters[i] = tag.hasKey("fluid" + i)
                ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid" + i))
                : null;
            if (itemFilters[i] != null) itemFilters[i].stackSize = 1;
            if (fluidFilters[i] != null) fluidFilters[i].amount = 1;
            itemTargets[i] = Math.max(0, tag.getLong("items" + i));
            fluidTargets[i] = Math.max(0, tag.getLong("fluids" + i));
        }
        pendingItem = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("pendingItem"));
        pendingItem = LogisticsStock.restoreCargoCount(pendingItem, tag);
        pendingFluid = tag.hasKey("pendingFluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("pendingFluid"))
            : null;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeContents(tag);
        if (owner != null) tag.setString("logisticsOwner", owner.toString());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readContents(tag);
        owner = null;
        try {
            owner = UUID.fromString(tag.getString("logisticsOwner"));
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return FluxLogisticsGui.build(this, sync);
    }
}
