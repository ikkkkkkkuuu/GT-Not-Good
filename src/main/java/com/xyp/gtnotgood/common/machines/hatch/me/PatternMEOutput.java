package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.enums.OutputBusType;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputBusTransaction;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;

/**
 * Composes the same native GT output providers used by the maximum-capacity ME bus and hatch.
 * Product caches are independent of pattern ingredients, persist with the master, and use its existing ME channel.
 * Mirrors return these same adapters so one controller cannot count the same cache twice.
 */
public final class PatternMEOutput {

    private final SuperMTEHatchCraftingInputME owner;
    private final Runnable markDirty;
    private final Provider<IAEItemStack> items;
    private final Provider<IAEFluidStack> fluids;
    public final IOutputBus itemOutput = new ItemOutput();
    public final IOutputHatch fluidOutput = new FluidOutput();

    public PatternMEOutput(SuperMTEHatchCraftingInputME owner) {
        this(owner, owner::markDirty);
    }

    /** Allows cache persistence and transaction checks without constructing a world or AE network. */
    PatternMEOutput(SuperMTEHatchCraftingInputME owner, Runnable markDirty) {
        this.owner = owner;
        this.markDirty = markDirty;
        items = new Provider<>(new Environment<IAEItemStack>() {

            public StorageChannel getChannel() {
                return StorageChannel.ITEMS;
            }

            public IMEInventory<IAEItemStack> getNetworkInvtory() throws GridAccessException {
                return owner.getProxy()
                    .getStorage()
                    .getItemInventory();
            }

            public NBTTagCompound saveStackToNBT(IAEItemStack stack) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag(
                    "stack",
                    GTUtility.saveItem(
                        stack.copy()
                            .setStackSize(1)
                            .getItemStack()));
                tag.setLong("amount", stack.getStackSize());
                return tag;
            }

            public IAEItemStack loadStackFromNBT(NBTTagCompound tag) {
                IAEItemStack stack = AEItemStack.create(GTUtility.loadItem(tag.getCompoundTag("stack")));
                return stack == null ? null : stack.setStackSize(tag.getLong("amount"));
            }

            public MTEHatchOutputMEBase<IAEItemStack> getProvider() {
                return items;
            }
        });
        fluids = new Provider<>(new Environment<IAEFluidStack>() {

            public StorageChannel getChannel() {
                return StorageChannel.FLUIDS;
            }

            public IMEInventory<IAEFluidStack> getNetworkInvtory() throws GridAccessException {
                return owner.getProxy()
                    .getStorage()
                    .getFluidInventory();
            }

            public NBTTagCompound saveStackToNBT(IAEFluidStack stack) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag(
                    "stack",
                    stack.copy()
                        .setStackSize(1)
                        .getFluidStack()
                        .writeToNBT(new NBTTagCompound()));
                tag.setLong("amount", stack.getStackSize());
                return tag;
            }

            public IAEFluidStack loadStackFromNBT(NBTTagCompound tag) {
                IAEFluidStack stack = AEFluidStack
                    .create(FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("stack")));
                return stack == null ? null : stack.setStackSize(tag.getLong("amount"));
            }

            public MTEHatchOutputMEBase<IAEFluidStack> getProvider() {
                return fluids;
            }
        });
    }

    /** Runs the native cache flush cadence and capacity-change notifications on the server. */
    public void tick(long timer) {
        items.onPostTick(owner.getBaseMetaTileEntity(), timer);
        fluids.onPostTick(owner.getBaseMetaTileEntity(), timer);
    }

    /** Explicitly retries exporting products, using the same powered insertion as native ME outputs. */
    public void flush() {
        items.flushCachedStack();
        fluids.flushCachedStack();
    }

    /** Persists native long-sized cache entries without duplicating the master's AE node data. */
    public void save(NBTTagCompound tag) {
        tag.setTag("patternMEItemOutput", items.saveCache());
        tag.setTag("patternMEFluidOutput", fluids.saveCache());
    }

    /** Replaces cache contents, including when loading an older hatch without output caches. */
    public void load(NBTTagCompound tag) {
        items.loadCache(tag.getTagList("patternMEItemOutput", 10));
        fluids.loadCache(tag.getTagList("patternMEFluidOutput", 10));
    }

    /** Appends the shared product-output behavior to each input variant's existing tooltip. */
    public static String[] describe(String[] original) {
        List<String> lines = new ArrayList<>(Arrays.asList(original));
        // #tr tooltip.gtnotgood.pattern_me_output
        // # Also outputs items and fluids to ME
        // # zh_CN 同时支持物品和流体ME输出
        lines.add(StatCollector.translateToLocal("tooltip.gtnotgood.pattern_me_output"));
        // #tr tooltip.gtnotgood.pattern_me_output_cache
        // # Separate maximum-capacity ME output caches
        // # zh_CN 物品和流体输出各有独立最大容量缓存
        lines.add(StatCollector.translateToLocal("tooltip.gtnotgood.pattern_me_output_cache"));
        return lines.toArray(new String[0]);
    }

    /** Supplies the real hatch environment; output providers never create additional tile entities or ME nodes. */
    private abstract class Environment<T extends IAEStack<T>> implements MTEHatchOutputMEBase.Environment<T> {

        public IGregTechTileEntity getBaseMetaTileEntity() {
            return owner.getBaseMetaTileEntity();
        }

        public IGridProxyable getIGridProxyable() {
            return owner;
        }

        public ItemStack getCellStack() {
            return null;
        }

        public ISaveProvider getISaveProvider() {
            return inventory -> markDirty.run();
        }

        public BaseActionSource getActionSource() {
            return owner.getMEOutputActionSource();
        }

        public EntityPlayer getLastClickedPlayer() {
            return null;
        }

        public byte getColor() {
            return owner.getColor();
        }

        public String getCopiedDataIdentifier(EntityPlayer player) {
            return "";
        }

        public ItemStack getVisual() {
            return owner.getSelfRep();
        }

        public void dispatchMarkDirty() {
            markDirty.run();
        }

        public void notifyOutputSpaceChanged() {
            owner.notifyMEOutputSpaceChanged();
        }

        public String getEnableKey() {
            return "GT5U.hatch.item.filter.enable";
        }

        public String getDisableKey() {
            return "GT5U.hatch.item.filter.disable";
        }
    }

    /**
     * Uses GT's native long counter, acceptance policy and network flushing, with the project's maximum capacity.
     * Only the existing master's proxy and cache-only persistence differ from a standalone output hatch.
     */
    private final class Provider<T extends IAEStack<T>> extends MTEHatchOutputMEBase<T> {

        private final Environment<T> environment;

        Provider(Environment<T> environment) {
            super(environment, 1600);
            this.environment = environment;
            MaxCapacityMEOutputCapacity.forceMaxCapacity(this);
        }

        @Override
        public AENetworkProxy getProxy() {
            return owner.getProxy();
        }

        NBTTagList saveCache() {
            NBTTagList list = new NBTTagList();
            cache.iterateAll(
                (stack, amount) -> list.appendTag(
                    environment.saveStackToNBT(
                        stack.copy()
                            .setStackSize(amount))));
            return list;
        }

        void loadCache(NBTTagList list) {
            cache.updateAll((stack, amount) -> 0);
            for (int i = 0; i < list.tagCount(); i++) {
                T stack = environment.loadStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null && stack.getStackSize() > 0) addToCache(stack);
            }
            MaxCapacityMEOutputCapacity.forceMaxCapacity(this);
        }
    }

    /**
     * Stages native AE stacks without changing the provider until commit, as GT's native output transactions do.
     * These unfiltered providers use the native default check mode (off); no cell or extra channel is required.
     */
    private final class Pending<T extends IAEStack<T>> {

        private final Provider<T> provider;
        private final List<T> stacks = new ArrayList<>();
        private boolean committed;

        Pending(Provider<T> provider) {
            this.provider = provider;
        }

        boolean hasSpace() {
            return provider.hasAvailableSpace();
        }

        boolean insert(T stack) {
            if (committed) throw new IllegalStateException("Output transaction already committed");
            if (!hasSpace()) return false;
            stacks.add(stack.copy());
            return true;
        }

        void commit() {
            if (committed) throw new IllegalStateException("Output transaction already committed");
            committed = true;
            for (T stack : stacks) provider.addToCache(stack);
            if (!stacks.isEmpty()) {
                provider.updateLastInputTick();
                markDirty.run();
            }
        }
    }

    /** Item output adapter for GT ejection and void-protection calculations. */
    private final class ItemOutput implements IOutputBus {

        public boolean isFiltered() {
            return false;
        }

        public boolean isFilteredToItem(GTUtility.ItemId id) {
            return true;
        }

        public OutputBusType getBusType() {
            return OutputBusType.MEUnfiltered;
        }

        public IOutputBusTransaction createTransaction() {
            return new ItemTransaction();
        }

        public boolean storePartial(ItemStack stack, boolean simulate) {
            IAEItemStack input = AEItemStack.create(stack);
            items.storePartial(input, simulate);
            stack.stackSize = (int) input.getStackSize();
            return stack.stackSize == 0;
        }
    }

    /** Item transaction keeps recipe simulations isolated from the persistent product cache. */
    private final class ItemTransaction implements IOutputBusTransaction {

        private final Pending<IAEItemStack> pending = new Pending<>(items);

        public IOutputBus getBus() {
            return itemOutput;
        }

        public boolean hasAvailableSpace() {
            return pending.hasSpace();
        }

        public boolean storePartial(GTUtility.ItemId id, ItemStack stack, long total, long perParallel) {
            if (!pending.insert(AEItemStack.create(stack))) return false;
            stack.stackSize = 0;
            return true;
        }

        public void complete(GTUtility.ItemId id) {}

        public void commit() {
            pending.commit();
        }
    }

    /** Fluid output adapter for GT ejection and void-protection calculations. */
    private final class FluidOutput implements IOutputHatch {

        public boolean isFiltered() {
            return false;
        }

        public boolean isFilteredToFluid(GTUtility.FluidId id) {
            return true;
        }

        public OutputHatchType getHatchType() {
            return OutputHatchType.MEUnfiltered;
        }

        public IOutputHatchTransaction createTransaction() {
            return new FluidTransaction();
        }

        public boolean storePartial(FluidStack stack, boolean simulate) {
            IAEFluidStack input = AEFluidStack.create(stack);
            fluids.storePartial(input, simulate);
            stack.amount = (int) input.getStackSize();
            return stack.amount == 0;
        }
    }

    /** Fluid transaction keeps recipe simulations isolated from the persistent product cache. */
    private final class FluidTransaction implements IOutputHatchTransaction {

        private final Pending<IAEFluidStack> pending = new Pending<>(fluids);

        public IOutputHatch getHatch() {
            return fluidOutput;
        }

        public boolean hasAvailableSpace() {
            return pending.hasSpace();
        }

        public boolean storePartial(GTUtility.FluidId id, FluidStack stack, long total, long perParallel) {
            if (!pending.insert(AEFluidStack.create(stack))) return false;
            stack.amount = 0;
            return true;
        }

        public void complete(GTUtility.FluidId id) {}

        public void commit() {
            pending.commit();
        }
    }
}
