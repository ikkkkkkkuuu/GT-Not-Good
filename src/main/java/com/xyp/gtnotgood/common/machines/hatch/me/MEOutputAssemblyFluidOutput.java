package com.xyp.gtnotgood.common.machines.hatch.me;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.util.item.AEFluidStack;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.IOutputHatchTransaction;
import gregtech.api.interfaces.IOutputTransaction;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.util.GTUtility;
import gregtech.common.tileentities.machines.outputme.base.MTEHatchOutputMEBase;
import gregtech.common.tileentities.machines.outputme.util.AECacheCounter;

public final class MEOutputAssemblyFluidOutput extends MTEHatchOutput {

    private static final String NBT_KEY = "assemblyFluidOutput";
    private static final Field BASE_TILE = baseTileField();
    private final MEOutputAssembly owner;
    private final Provider provider;

    public MEOutputAssemblyFluidOutput(MEOutputAssembly owner) {
        super("me_output_assembly_fluid_view", 4, 0, new String[0], null);
        this.owner = owner;
        provider = new Provider(new Environment());
        attach();
    }

    /** The ordinary setter would replace the real assembly on its base tile. */
    public void attach() {
        if (getBaseMetaTileEntity() == owner.getBaseMetaTileEntity()) return;
        try {
            BASE_TILE.set(this, owner.getBaseMetaTileEntity());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot attach assembly fluid output", e);
        }
    }

    private static Field baseTileField() {
        try {
            Field field = MetaTileEntity.class.getDeclaredField("mBaseMetaTileEntity");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Missing GT meta-tile base reference", e);
        }
    }

    @Override
    public boolean isValid() {
        return owner != null && owner.isValid();
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public boolean isFiltered() {
        return owner.getFilters().hasFluids();
    }

    @Override
    public boolean isFluidLocked() {
        return isFiltered();
    }

    @Override
    public boolean isEmptyAndAcceptsAnyFluid() {
        return !isFiltered() && !owner.getProvider().getCheckMode();
    }

    @Override
    public boolean canStoreFluid(@Nonnull FluidStack stack) {
        return owner.getFilters().accepts(stack);
    }

    @Override
    public boolean isFilteredToFluid(GTUtility.FluidId id) {
        return canStoreFluid(id.getFluidStack());
    }

    @Override
    public OutputHatchType getHatchType() {
        if (owner.getProvider().getCacheMode())
            return isFiltered() ? OutputHatchType.MECacheFiltered : OutputHatchType.MECacheUnfiltered;
        return isFiltered() ? OutputHatchType.MEFiltered : OutputHatchType.MEUnfiltered;
    }

    public MTEHatchOutputMEBase<IAEFluidStack> getProvider() {
        return provider;
    }

    public void updateSettings() {
        attach();
        MTEHatchOutputMEBase<?> source = owner.getProvider();
        if (provider.getCheckMode() != source.getCheckMode()) provider.setCheckMode(source.getCheckMode());
        if (provider.getCacheMode() != source.getCacheMode()) provider.setCacheMode(source.getCacheMode());
        if (provider.getPriority() != source.getPriority()) provider.setPriority(source.getPriority());
        provider.forceMaxCapacity();
    }

    public void tick(long timer) {
        updateSettings();
        provider.onPostTick(owner.getBaseMetaTileEntity(), timer);
    }

    public void save(NBTTagCompound tag) {
        provider.saveCache(tag);
    }

    public void load(NBTTagCompound tag) {
        provider.loadCache(tag.getCompoundTag(NBT_KEY).getTagList("cache", 10));
        updateSettings();
    }

    @Override
    public void setItemNBT(NBTTagCompound tag) {
        tag.removeTag(NBT_KEY);
    }

    @Override
    public int fill(FluidStack stack, boolean doFill) {
        if (stack == null || stack.amount <= 0 || !canStoreFluid(stack)) return 0;
        updateSettings();
        int accepted = (int) Math.min(stack.amount, Math.max(0, provider.getPhysicalSpace()));
        if (accepted == 0) return 0;
        IAEFluidStack input = AEFluidStack.create(stack).setStackSize(accepted);
        provider.storePartial(input, !doFill);
        return accepted - (int) input.getStackSize();
    }

    @Override
    public boolean storePartial(FluidStack stack, boolean simulate) {
        if (stack == null) return false;
        stack.amount -= fill(stack, !simulate);
        return stack.amount == 0;
    }

    public boolean hasAvailableSpace() {
        return provider.hasAvailableSpace();
    }

    @Override
    public IOutputHatchTransaction createTransaction() {
        updateSettings();
        return new Transaction();
    }

    public void notifyOutputSpaceChanged() {
        notifyWatchers();
        owner.notifyOutputSpaceChanged();
    }

    public void filtersChanged() {
        notifyWatchers();
    }

    private final class Environment implements MTEHatchOutputMEBase.Environment<IAEFluidStack> {

        public IGregTechTileEntity getBaseMetaTileEntity() {
            return owner.getBaseMetaTileEntity();
        }

        public IGridProxyable getIGridProxyable() {
            return owner;
        }

        public StorageChannel getChannel() {
            return StorageChannel.FLUIDS;
        }

        public ItemStack getCellStack() {
            return null;
        }

        public ISaveProvider getISaveProvider() {
            return inventory -> owner.markDirty();
        }

        public BaseActionSource getActionSource() {
            return owner.getActionSource();
        }

        public EntityPlayer getLastClickedPlayer() {
            return null;
        }

        public IMEInventory<IAEFluidStack> getNetworkInvtory() throws GridAccessException {
            return owner.getProxy().getStorage().getFluidInventory();
        }

        public NBTTagCompound saveStackToNBT(IAEFluidStack stack) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("stack", stack.copy().setStackSize(1).getFluidStack().writeToNBT(new NBTTagCompound()));
            tag.setLong("amount", stack.getStackSize());
            return tag;
        }

        public IAEFluidStack loadStackFromNBT(NBTTagCompound tag) {
            IAEFluidStack stack = AEFluidStack.create(FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("stack")));
            return stack == null ? null : stack.setStackSize(tag.getLong("amount"));
        }

        public byte getColor() {
            return owner.getColor();
        }

        public String getCopiedDataIdentifier(EntityPlayer player) {
            return owner.getCopiedDataIdentifier(player);
        }

        public ItemStack getVisual() {
            return owner.getVisual();
        }

        public void dispatchMarkDirty() {
            owner.markDirty();
        }

        public void notifyOutputSpaceChanged() {
            MEOutputAssemblyFluidOutput.this.notifyOutputSpaceChanged();
        }

        public MTEHatchOutputMEBase<IAEFluidStack> getProvider() {
            return provider;
        }

        public String getEnableKey() {
            return "GT5U.hatch.fluid.filter.enable";
        }

        public String getDisableKey() {
            return "GT5U.hatch.fluid.filter.disable";
        }
    }

    private final class Provider extends MTEHatchOutputMEBase<IAEFluidStack> {

        private final MEOutputAssemblyFluidOutput.Environment environment;

        Provider(MEOutputAssemblyFluidOutput.Environment environment) {
            super(environment, 128_000);
            this.environment = environment;
            forceMaxCapacity();
        }

        @Override
        public AENetworkProxy getProxy() {
            return owner.getProxy();
        }

        void forceMaxCapacity() {
            baseCapacity = Long.MAX_VALUE;
            cacheCapacity = Long.MAX_VALUE;
        }

        void saveCache(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            cache.iterateAll(
                (stack, amount) -> list.appendTag(environment.saveStackToNBT(stack.copy().setStackSize(amount))));
            if (list.tagCount() == 0) {
                tag.removeTag(NBT_KEY);
            } else {
                NBTTagCompound fluidTag = new NBTTagCompound();
                fluidTag.setTag("cache", list);
                tag.setTag(NBT_KEY, fluidTag);
            }
        }

        void loadCache(NBTTagList list) {
            cache.updateAll((stack, amount) -> 0);
            for (int i = 0; i < list.tagCount(); i++) {
                IAEFluidStack stack = environment.loadStackFromNBT(list.getCompoundTagAt(i));
                if (stack != null && stack.getStackSize() > 0) addToCache(stack);
            }
            forceMaxCapacity();
        }
    }

    private final class Transaction implements IOutputHatchTransaction, IOutputTransaction.IRecipeCheckAware,
        IOutputTransaction.IProtectOutputAware {

        private final AECacheCounter<GTUtility.FluidId> pending = new AECacheCounter<>();
        private final long availableSpace = Math.max(0, provider.getPhysicalSpace());
        private boolean recipeCheck;
        private boolean protectOutput = true;
        private boolean committed;

        public IOutputHatch getHatch() {
            return MEOutputAssemblyFluidOutput.this;
        }

        public void setRecipeCheck(boolean checking) {
            recipeCheck = checking;
        }

        public void setProtectOutput(boolean protect) {
            protectOutput = protect;
        }

        public boolean needsTotalParallelData() {
            return recipeCheck && protectOutput && provider.getCheckMode();
        }

        public boolean hasAvailableSpace() {
            return pending.getTotal() < availableSpace;
        }

        public boolean storePartial(GTUtility.FluidId id, @Nonnull FluidStack stack, long totalPerParallel,
            long perParallel) {
            if (committed) throw new IllegalStateException("Output transaction already committed");
            if (stack.amount <= 0 || !canStoreFluid(stack)) return false;
            long amount = Math.min(stack.amount, availableSpace - pending.getTotal());
            if (needsTotalParallelData() && totalPerParallel > 0 && perParallel > 0) {
                long parallels = Math.max(1, Math.min(Integer.MAX_VALUE, availableSpace / totalPerParallel));
                long budget = perParallel > Long.MAX_VALUE / parallels ? Long.MAX_VALUE : perParallel * parallels;
                amount = Math.min(amount, budget);
            }
            if (amount <= 0) return false;
            pending.insert(id, amount);
            stack.amount -= (int) amount;
            return true;
        }

        public void complete(GTUtility.FluidId id) {}

        public void commit() {
            if (committed) throw new IllegalStateException("Output transaction already committed");
            committed = true;
            if (pending.getTotal() == 0) return;
            pending.iterateAll(
                (id, amount) -> provider.addToCache(AEFluidStack.create(id.getFluidStack()).setStackSize(amount)));
            provider.updateLastInputTick();
            owner.markDirty();
        }
    }
}
