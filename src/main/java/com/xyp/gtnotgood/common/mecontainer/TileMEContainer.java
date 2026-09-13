package com.xyp.gtnotgood.common.mecontainer;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeBase;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/**
 * Thirty-six item and fluid export selections. Fluid access is a live, powered AE transaction, not a tank.
 * Item slots retain real stacks for vanilla inventory read/modify/write and hopper rollback compatibility.
 * Legacy fluid buffers remain drainable after upgrading, including when their old filter has been cleared.
 */
public class TileMEContainer extends TileMEBridgeBase
    implements ISidedInventory, IFluidHandler, IGuiHolder<PosGuiData> {

    static final int SLOT_COUNT = 36;
    final ItemStack[] itemFilters = new ItemStack[SLOT_COUNT];
    final FluidStack[] fluidFilters = new FluidStack[SLOT_COUNT];
    private final ItemStack[] items = new ItemStack[SLOT_COUNT * 2];
    /** Only used to preserve already extracted fluid from the old buffered implementation. */
    private FluidStack fluid;
    final long[] networkItems = new long[SLOT_COUNT];
    final long[] networkFluid = new long[SLOT_COUNT];
    boolean online;
    private boolean transferring;

    @Override
    protected ItemStack getVisualRepresentation() {
        return GTNGItemList.MEContainer.get(1);
    }

    @Override
    public AENetworkProxy getProxy() {
        boolean creating = gridProxy == null;
        AENetworkProxy proxy = super.getProxy();
        if (creating) {
            proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            proxy.setIdlePowerUsage(1);
        }
        return proxy;
    }

    @Override
    public void securityBreak() {
        if (isServerSide() && !isInvalid()) worldObj.func_147480_a(xCoord, yCoord, zCoord, true);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!isServerSide() || transferring) return;
        online = getProxy().isActive();
        Arrays.fill(networkItems, 0);
        Arrays.fill(networkFluid, 0);
        if (!online) return;
        transferring = true;
        try {
            refill();
        } catch (GridAccessException ignored) {
            online = false;
        } finally {
            transferring = false;
        }
    }

    /** Fills real item slots and refreshes long network counts without reserving any fluid. */
    private void refill() throws GridAccessException {
        MachineSource source = new MachineSource(this);
        var itemInventory = getProxy().getStorage()
            .getItemInventory();
        var fluidInventory = getProxy().getStorage()
            .getFluidInventory();
        flushInputs();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack filter = itemFilters[slot];
            if (filter != null) {
                var request = AEItemStack.create(filter);
                if (items[slot] == null || sameItem(items[slot], filter)) {
                    int missing = Math.min(64, filter.getMaxStackSize())
                        - (items[slot] == null ? 0 : items[slot].stackSize);
                    if (missing > 0) {
                        request.setStackSize(missing);
                        IAEItemStack extracted = Platform
                            .poweredExtraction(getProxy().getEnergy(), itemInventory, request, source);
                        if (extracted != null && extracted.getStackSize() > 0) {
                            if (items[slot] == null) items[slot] = extracted.getItemStack();
                            else items[slot].stackSize += (int) extracted.getStackSize();
                            markDirty();
                        }
                    }
                }
            }
        }
        var storedItems = itemInventory.getStorageList();
        var storedFluids = fluidInventory.getStorageList();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (itemFilters[slot] != null) {
                IAEItemStack stored = storedItems.findPrecise(AEItemStack.create(itemFilters[slot]));
                networkItems[slot] = stored == null ? 0 : stored.getStackSize();
            }
            if (fluidFilters[slot] != null) {
                IAEFluidStack stored = storedFluids.findPrecise(AEFluidStack.create(fluidFilters[slot]));
                networkFluid[slot] = stored == null ? 0 : stored.getStackSize();
            }
        }
    }

    /** Flushes only real ingress stacks; rejected remainders stay durable in the block. */
    void flushInputs() throws GridAccessException {
        for (int slot = SLOT_COUNT; slot < items.length; slot++) {
            if (items[slot] == null) continue;
            ItemStack remainder = insertNetworkItem(items[slot].copy());
            int left = remainder == null ? 0 : remainder.stackSize;
            if (left != items[slot].stackSize) {
                items[slot] = left == 0 ? null : remainder;
                markDirty();
            }
        }
    }

    /**
     * Commits a buffered item insertion, accounting for ME energy and machine permissions.
     *
     * @param offered owned copy of the ingress stack
     * @return rejected remainder, or null when fully accepted
     * @throws GridAccessException when the grid becomes unavailable
     */
    protected ItemStack insertNetworkItem(ItemStack offered) throws GridAccessException {
        IAEItemStack remainder = Platform.poweredInsert(
            getProxy().getEnergy(),
            getProxy().getStorage()
                .getItemInventory(),
            AEItemStack.create(offered),
            new MachineSource(this));
        return remainder == null ? null : remainder.getItemStack();
    }

    static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    void setItemFilter(int slot, ItemStack stack) {
        if (!validSlot(slot)) return;
        itemFilters[slot] = stack == null ? null : stack.copy();
        if (itemFilters[slot] != null) itemFilters[slot].stackSize = 1;
        networkItems[slot] = 0;
        markDirty();
    }

    /** A fluid appears once in the external tank list, even if a sample is moved between slots. */
    void setFluidFilter(int slot, FluidStack stack) {
        if (!validSlot(slot)) return;
        if (stack != null) {
            for (int other = 0; other < SLOT_COUNT; other++) {
                if (other != slot && fluidFilters[other] != null && fluidFilters[other].isFluidEqual(stack)) {
                    fluidFilters[other] = null;
                    networkFluid[other] = 0;
                }
            }
        }
        fluidFilters[slot] = stack == null ? null : stack.copy();
        if (fluidFilters[slot] != null) fluidFilters[slot].amount = 1;
        networkFluid[slot] = 0;
        markDirty();
    }

    static boolean sameItem(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    FluidStack bufferedFluid() {
        return fluid == null ? null : fluid.copy();
    }

    @Override
    public int getSizeInventory() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < items.length ? items[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (!isServerSide() || !validSlot(slot) || amount <= 0 || items[slot] == null || transferring) return null;
        ItemStack result = items[slot].splitStack(Math.min(amount, items[slot].stackSize));
        if (items[slot].stackSize <= 0) items[slot] = null;
        markDirty();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    /** Vanilla hoppers also restore a removed stack when insertion fails; the setter must support rollback. */
    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (!isServerSide() || slot < 0 || slot >= items.length || transferring) return;
        items[slot] = stack;
        if (items[slot] != null && items[slot].stackSize <= 0) items[slot] = null;
        if (items[slot] != null)
            items[slot].stackSize = Math.min(items[slot].stackSize, Math.min(64, items[slot].getMaxStackSize()));
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "tile.me_container.name";
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
        return !isInvalid() && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= SLOT_COUNT && slot < items.length && stack != null;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int[] slots = new int[items.length];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return !transferring && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return validSlot(slot) && !transferring && sameItem(items[slot], stack);
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (!isServerSide() || transferring || resource == null || resource.amount <= 0) return 0;
        transferring = true;
        try {
            return insertNetworkFluid(resource, doFill);
        } finally {
            transferring = false;
        }
    }

    /**
     * Inserts any supplied fluid, independently of extraction filters. Returns only the amount accepted by AE.
     * No local tank is allocated, and callers retain the unaccepted remainder.
     *
     * @param resource offered fluid, never mutated
     * @param modulate whether to commit the transaction
     * @return accepted mB, bounded by the original offer
     */
    protected int insertNetworkFluid(FluidStack resource, boolean modulate) {
        if (!getProxy().isActive()) return 0;
        try {
            IAEFluidStack remainder = Platform.poweredInsert(
                getProxy().getEnergy(),
                getProxy().getStorage()
                    .getFluidInventory(),
                AEFluidStack.create(resource),
                new MachineSource(this),
                modulate ? Actionable.MODULATE : Actionable.SIMULATE);
            return resource.amount - (remainder == null ? 0 : (int) remainder.getStackSize());
        } catch (GridAccessException ignored) {
            return 0;
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (!isServerSide() || transferring || resource == null || resource.amount <= 0) return null;
        transferring = true;
        try {
            int local = fluid != null && fluid.isFluidEqual(resource) ? Math.min(fluid.amount, resource.amount) : 0;
            int remote = 0;
            if (local < resource.amount && isFluidConfigured(resource)) {
                FluidStack extracted = extractNetworkFluid(resource, resource.amount - local, doDrain);
                if (extracted != null) remote = extracted.amount;
            }
            if (local + remote <= 0) return null;
            if (doDrain && local > 0) {
                fluid.amount -= local;
                if (fluid.amount <= 0) fluid = null;
                markDirty();
            }
            FluidStack result = resource.copy();
            result.amount = local + remote;
            return result;
        } finally {
            transferring = false;
        }
    }

    private boolean isFluidConfigured(FluidStack resource) {
        for (FluidStack sample : fluidFilters) {
            if (sample != null && sample.isFluidEqual(resource)) return true;
        }
        return false;
    }

    /**
     * Performs one AE transaction using the caller's requested amount. Simulation consumes neither fluid nor power.
     * Forge represents a single transfer with an int; this is an API bound, not a stored-fluid capacity.
     *
     * @param sample   exact fluid identity, including NBT
     * @param amount   positive requested amount in mB
     * @param modulate true to commit, false to simulate
     * @return actually extractable fluid, or null when the network is unavailable
     */
    protected FluidStack extractNetworkFluid(FluidStack sample, int amount, boolean modulate) {
        if (!isServerSide() || !getProxy().isActive()) return null;
        try {
            var request = AEFluidStack.create(sample);
            request.setStackSize(amount);
            IAEFluidStack extracted = Platform.poweredExtraction(
                getProxy().getEnergy(),
                getProxy().getStorage()
                    .getFluidInventory(),
                request,
                new MachineSource(this),
                modulate ? Actionable.MODULATE : Actionable.SIMULATE);
            return extracted == null ? null : extracted.getFluidStack();
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (!isServerSide() || transferring || maxDrain <= 0) return null;
        if (fluid != null) {
            FluidStack request = fluid.copy();
            request.amount = maxDrain;
            return drain(from, request, doDrain);
        }
        for (FluidStack sample : fluidFilters) {
            if (sample == null) continue;
            FluidStack request = sample.copy();
            request.amount = maxDrain;
            FluidStack result = drain(from, request, doDrain);
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, net.minecraftforge.fluids.Fluid type) {
        return type != null && fill(from, new FluidStack(type, 1), false) > 0;
    }

    @Override
    public boolean canDrain(ForgeDirection from, net.minecraftforge.fluids.Fluid type) {
        if (!isServerSide() || transferring) return false;
        if (fluid != null && (type == null || fluid.getFluid() == type)) return true;
        for (FluidStack sample : fluidFilters) {
            if (sample != null && (type == null || sample.getFluid() == type)) {
                FluidStack request = sample.copy();
                request.amount = 1;
                if (drain(from, request, false) != null) return true;
            }
        }
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        ArrayList<FluidTankInfo> tanks = new ArrayList<>();
        for (FluidStack sample : fluidFilters) {
            FluidStack available = null;
            if (sample != null) {
                FluidStack request = sample.copy();
                request.amount = Integer.MAX_VALUE;
                available = drain(from, request, false);
            }
            tanks.add(new FluidTankInfo(available, Integer.MAX_VALUE));
        }
        if (fluid != null && !isFluidConfigured(fluid))
            tanks.add(new FluidTankInfo(bufferedFluid(), Integer.MAX_VALUE));
        return tanks.toArray(new FluidTankInfo[0]);
    }

    /** Serializes portable resources only; AE identity and ownership must not be cloned into dropped blocks. */
    void writeContents(NBTTagCompound tag) {
        NBTTagList slots = new NBTTagList();
        for (int i = 0; i < SLOT_COUNT; i++) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("slot", i);
            if (items[i + SLOT_COUNT] != null)
                entry.setTag("input", items[i + SLOT_COUNT].writeToNBT(new NBTTagCompound()));
            if (items[i] != null) entry.setTag("items", items[i].writeToNBT(new NBTTagCompound()));
            if (itemFilters[i] != null) entry.setTag("itemFilter", itemFilters[i].writeToNBT(new NBTTagCompound()));
            if (fluidFilters[i] != null) entry.setTag("fluidFilter", fluidFilters[i].writeToNBT(new NBTTagCompound()));
            slots.appendTag(entry);
        }
        tag.setTag("slots", slots);
        tag.removeTag("items");
        tag.removeTag("itemFilter");
        tag.removeTag("fluidFilter");
        tag.removeTag("fluid");
        if (fluid != null) tag.setTag("fluid", fluid.writeToNBT(new NBTTagCompound()));
    }

    /** Loads old single-slot blocks into slot zero without discarding their already extracted resources. */
    void readContents(NBTTagCompound tag) {
        Arrays.fill(items, null);
        Arrays.fill(itemFilters, null);
        Arrays.fill(fluidFilters, null);
        Arrays.fill(networkItems, 0);
        Arrays.fill(networkFluid, 0);
        fluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid"));
        if (fluid != null && fluid.amount <= 0) fluid = null;
        if (tag.hasKey("slots")) {
            NBTTagList slots = tag.getTagList("slots", 10);
            for (int i = 0; i < slots.tagCount(); i++) {
                NBTTagCompound entry = slots.getCompoundTagAt(i);
                int slot = entry.getInteger("slot");
                if (validSlot(slot)) readSlot(slot, entry);
            }
        } else readSlot(0, tag);
    }

    private void readSlot(int slot, NBTTagCompound tag) {
        items[slot + SLOT_COUNT] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("input"));
        items[slot] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("items"));
        if (items[slot] != null) {
            items[slot].stackSize = Math.min(items[slot].stackSize, Math.min(64, items[slot].getMaxStackSize()));
            if (items[slot].stackSize <= 0) items[slot] = null;
        }
        itemFilters[slot] = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("itemFilter"));
        fluidFilters[slot] = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluidFilter"));
        if (itemFilters[slot] != null) itemFilters[slot].stackSize = 1;
        if (fluidFilters[slot] != null) {
            fluidFilters[slot].amount = 1;
            for (int other = 0; other < SLOT_COUNT; other++) {
                if (other != slot && fluidFilters[other] != null
                    && fluidFilters[other].isFluidEqual(fluidFilters[slot])) fluidFilters[other] = null;
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeContents(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readContents(tag);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return MEContainerGui.build(this, sync);
    }

    /** Actual block GUI entry point: applies the library font to layout, drawing and input together. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new com.xyp.ldlib.integration.modularui.PixelFontModularScreen(
            com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD,
            mainPanel);
    }
}
