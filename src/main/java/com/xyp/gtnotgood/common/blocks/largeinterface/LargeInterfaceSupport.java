package com.xyp.gtnotgood.common.blocks.largeinterface;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.inventory.AEFluidInventory;

import appeng.api.storage.data.IAEFluidStack;
import appeng.tile.inventory.AppEngInternalAEInventory;
import io.netty.buffer.ByteBuf;

/** Empty configuration inventories keep the native interface in its ME pass-through mode. */
public final class LargeInterfaceSupport {

    private LargeInterfaceSupport() {}

    /**
     * Creates a permanently empty stock configuration, including when restored from NBT or a memory card.
     *
     * @param slots native configuration size expected by AE inventory loops
     * @return an inventory that rejects stock marks
     */
    public static AppEngInternalAEInventory emptyItemConfig(int slots) {
        return new EmptyItemConfig(slots);
    }

    public static AEFluidInventory emptyFluidConfig(int slots) {
        return new EmptyFluidConfig(slots);
    }

    /** Removes stock marks while retaining native settings, priority, patterns, and any returned products. */
    public static NBTTagCompound withoutStockConfig(NBTTagCompound source) {
        NBTTagCompound data = source == null ? new NBTTagCompound() : (NBTTagCompound) source.copy();
        data.removeTag("config");
        data.removeTag("ConfigInv");
        data.removeTag("fluidConfig");
        return data;
    }

    private static final class EmptyItemConfig extends AppEngInternalAEInventory {

        private EmptyItemConfig(int slots) {
            super(null, slots);
            setMaxStackSize(0);
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {}

        @Override
        public void readFromNBT(NBTTagCompound data, String name) {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return false;
        }
    }

    private static final class EmptyFluidConfig extends AEFluidInventory {

        private EmptyFluidConfig(int slots) {
            super(null, slots, 0);
        }

        @Override
        public void setFluidInSlot(int slot, IAEFluidStack fluid) {}

        @Override
        public void readFromNBT(NBTTagCompound data, String name) {}

        @Override
        public boolean readFromBuf(ByteBuf data) throws IOException {
            new AEFluidInventory(null, getSlots(), 0).readFromBuf(data);
            return false;
        }

        @Override
        public int fill(int slot, FluidStack fluid, boolean doFill) {
            return 0;
        }

        @Override
        public long fill(int slot, IAEFluidStack fluid, boolean doFill) {
            return 0;
        }
    }
}
