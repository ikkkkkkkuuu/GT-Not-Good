package com.xyp.gtnotgood.common.compat;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;

/**
 * 集中封装 AE2FC {@link ItemFluidDrop}，保留对应静态方法的 null 和非正数量边界行为。
 */
public final class FluidDropCompat {

    private FluidDropCompat() {}

    public static boolean isFluidDrop(@Nullable Item item) {
        return item instanceof ItemFluidDrop;
    }

    public static boolean isFluidDrop(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidDrop;
    }

    public static boolean isFluidDrop(@Nullable IAEItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidDrop;
    }

    /**
     * 用 FluidStack 造一个液滴物品（合成请求用，CPU 通过 instanceof 识别为流体请求）。
     * fluid 为 null 或 amount<=0 返回 null。
     */
    @Nullable
    public static ItemStack newStack(@Nullable FluidStack fluid) {
        return ItemFluidDrop.newStack(fluid);
    }

    /**
     * 造一个「仅显示」的液滴物品（NBT 带 DisplayOnly 标记，不参与实际合成/存取）。
     */
    @Nullable
    public static ItemStack newDisplayStack(@Nullable FluidStack fluid) {
        return ItemFluidDrop.newDisplayStack(fluid);
    }

    /**
     * 判断 ItemStack 是否携带合法流体（即能取出 FluidStack）。
     */
    public static boolean isFluidStack(@Nullable ItemStack stack) {
        return ItemFluidDrop.isFluidStack(stack);
    }

    /**
     * 判断 AE 物品堆是否携带合法流体。
     */
    public static boolean isFluidStack(@Nullable IAEItemStack stack) {
        return ItemFluidDrop.isFluidStack(stack);
    }

    /**
     * 从液滴物品中取出 FluidStack（数量取自 stackSize），非液滴返回 null。
     */
    @Nullable
    public static FluidStack getFluidStack(@Nullable ItemStack stack) {
        return ItemFluidDrop.getFluidStack(stack);
    }

    /**
     * 从液滴 AE 物品堆中取出原生 {@link IAEFluidStack}（数量对齐 AE 堆的 stackSize）。
     */
    @Nullable
    public static IAEFluidStack getAeFluidStack(@Nullable IAEItemStack stack) {
        return ItemFluidDrop.getAeFluidStack(stack);
    }

    /**
     * 用 FluidStack 造一个液滴 AE 物品堆。
     */
    @Nullable
    public static IAEItemStack newAeStack(@Nullable FluidStack fluid) {
        return ItemFluidDrop.newAeStack(fluid);
    }

    /**
     * 用原生 {@link IAEFluidStack} 造一个液滴 AE 物品堆（原生流体 → 液滴，与 getAeFluidStack 互为反向）。
     */
    @Nullable
    public static IAEItemStack newAeStack(@Nullable IAEFluidStack fluid) {
        return ItemFluidDrop.newAeStack(fluid);
    }
}
