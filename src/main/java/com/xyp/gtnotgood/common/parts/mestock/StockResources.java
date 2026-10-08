package com.xyp.gtnotgood.common.parts.mestock;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.NetworkMonitor;
import appeng.util.IterationCounter;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.util.GTUtility;

public final class StockResources {

    private StockResources() {}

    public static IAEStack<?> sample(ItemStack cursor) {
        if (cursor == null) return null;
        FluidStack fluid = ItemFluidPacket.getFluidStack(cursor);
        if (fluid == null) fluid = GTUtility.getFluidFromDisplayStack(cursor);
        if (fluid == null && FluidDropCompat.isFluidDrop(cursor)) fluid = FluidDropCompat.getFluidStack(cursor);
        if (fluid == null) fluid = FluidContainerRegistry.getFluidForFilledItem(cursor);
        if (fluid == null && cursor.getItem() instanceof IFluidContainerItem container)
            fluid = container.getFluid(cursor);
        return fluid == null ? AEItemStack.create(cursor) : AEFluidStack.create(fluid);
    }

    public static ItemStack display(IAEStack<?> key) {
        if (key instanceof IAEFluidStack fluid) return ItemFluidPacket.newDisplayStack(fluid.getFluidStack());
        if (key instanceof IAEItemStack item) {
            ItemStack result = item.getItemStack();
            result.stackSize = 1;
            return result;
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static long count(IStorageGrid storage, IAEStack<?> key) {
        if (key == null) return 0;
        IMEMonitor monitor = key.isFluid() ? storage.getFluidInventory() : storage.getItemInventory();
        IAEStack stored = monitor instanceof NetworkMonitor network
            ? network.getHandler().getAvailableItem(key, IterationCounter.fetchNewId())
            : monitor.getAvailableItem(key, IterationCounter.fetchNewId());
        return stored == null ? 0 : Math.max(0, stored.getStackSize());
    }

    public static long add(long a, long b) {
        return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b;
    }

    /** Subtraction is staged so very large reserves and in-flight counts cannot overflow. */
    public static long missing(long target, long stored, long pending) {
        return Math.max(0, Math.max(0, target - stored) - pending);
    }
}
