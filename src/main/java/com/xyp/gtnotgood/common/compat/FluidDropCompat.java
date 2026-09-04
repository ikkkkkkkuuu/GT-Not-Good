package com.xyp.gtnotgood.common.compat;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;

/**
 * AE2FC「流体液滴」(ItemFluidDrop) 兼容封装层。
 *
 * <p>
 * 背景：AE2 977 的自动合成引擎 (CraftingJobV2) 只认 {@link IAEItemStack}，无法直接对
 * {@link IAEFluidStack} 下单。因此 AE2FC 一直用 {@link ItemFluidDrop} 把流体伪装成物品塞进
 * 物品合成通道——这就是「液滴」机制。上游 (AE2FC 1.5.x / GTNH 2.9 线) 已把存储/注入层迁到
 * AE2 原生流体通道 (StorageChannel.FLUIDS)，并明确表达了将来删除液滴物品的意向，但合成请求层
 * 目前仍绕不开 ItemFluidDrop（引擎不支持流体下单）。
 *
 * <p>
 * 为了在上游真正删除/改造 ItemFluidDrop 时把改动锁在一个文件里，本项目所有对 ItemFluidDrop
 * 的直接调用都应收敛到这里。语义与 ItemFluidDrop 的对应静态方法完全一致（含 null / amount<=0
 * 的边界行为），当前实现只是薄转发，不改变任何行为。
 *
 * <p>
 * 迁移路线：
 * <ul>
 * <li>A（当前）：收敛调用点到本类，纯重构零行为变化。</li>
 * <li>C：把每个调用点标注「必须留液滴（合成请求）」或「可迁原生（存储/显示）」。</li>
 * <li>B：把「可迁原生」的显示/查询改走 getFluidInventory() 原生流体通道；合成请求层保留液滴，
 * 直到 AE2 977 合成引擎支持 IAEFluidStack 下单为止。</li>
 * </ul>
 */
public final class FluidDropCompat {

    private FluidDropCompat() {}

    /**
     * 判断一个物品是否是 AE2FC 的流体液滴物品。替代散落的 {@code instanceof ItemFluidDrop}。
     * 将来上游若改用别的载体类型，只需改这里。
     */
    public static boolean isFluidDrop(@Nullable Item item) {
        return item instanceof ItemFluidDrop;
    }

    /**
     * 判断一个 ItemStack 是否是流体液滴物品。
     */
    public static boolean isFluidDrop(@Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFluidDrop;
    }

    /**
     * 判断一个 AE 物品堆是否是流体液滴。
     */
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
     * 这是「液滴 → 原生流体」的桥，B 阶段迁移存储/显示层时会大量用到。
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
