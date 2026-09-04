package com.xyp.gtnotgood.common.items.wildcard.model.io;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;

import gregtech.api.enums.Materials;

/**
 * 固定组件：忽略当前材料，始终返回一个固定物品/流体 stack。
 * 用于在样板里放与通配材料无关的辅料（催化剂、固定的酸、熔融橡胶等）。
 * amount 精确控制数量：普通物品 → stackSize；流体（ItemFluidDrop）→ 流体 mB。
 */
public final class SimpleIOComponent implements IWildcardIOComponent {

    public static final String TYPE = "simple";

    private static final String KEY_STACK = "Stack";
    private static final String KEY_AMOUNT = "Amount";

    private ItemStack stack;
    private long amount;

    public SimpleIOComponent(ItemStack stack, long amount) {
        this.stack = stack == null ? null : stack.copy();
        this.amount = Math.max(1L, amount);
    }

    public static SimpleIOComponent empty() {
        return new SimpleIOComponent(null, 1L);
    }

    public static SimpleIOComponent readData(NBTTagCompound data) {
        ItemStack stack = data.hasKey(KEY_STACK) ? ItemStack.loadItemStackFromNBT(data.getCompoundTag(KEY_STACK))
            : null;
        long amount = Math.max(1L, data.getLong(KEY_AMOUNT));
        return new SimpleIOComponent(stack, amount);
    }

    /** 放入的物品（GT 流体显示物品应在放入前转成 ItemFluidDrop）。设置时用其自带数量初始化 amount。 */
    public void setStack(ItemStack stack) {
        this.stack = stack == null ? null : stack.copy();
        if (stack != null) {
            // [液滴分类] 可迁原生：仅读出流体量初始化 amount，不构造合成请求
            FluidStack fluid = FluidDropCompat.getFluidStack(stack);
            if (fluid != null) {
                this.amount = Math.max(1L, fluid.amount);
            } else {
                this.amount = Math.max(1L, stack.stackSize);
            }
        }
    }

    public ItemStack getStack() {
        return stack == null ? null : stack.copy();
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = Math.max(1L, amount);
    }

    /** 是否为流体（ItemFluidDrop）。 */
    public boolean isFluid() {
        // [液滴分类] 可迁原生：仅判定该组件是否为流体，纯查询，不参与合成
        return stack != null && FluidDropCompat.getFluidStack(stack) != null;
    }

    @Override
    public ItemStack apply(Materials material) {
        if (stack == null) return null;
        // [液滴分类] 必须留液滴：apply() 产出的堆用于展开样板参与合成，读流体是为重建下单用的 ItemFluidDrop
        FluidStack fluid = FluidDropCompat.getFluidStack(stack);
        if (fluid != null) {
            FluidStack copy = fluid.copy();
            copy.amount = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
            // [液滴分类] 必须留液滴：重建 ItemFluidDrop 作为样板展开后的流体请求，CPU 靠它识别下单
            return FluidDropCompat.newStack(copy);
        }
        ItemStack result = stack.copy();
        result.stackSize = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amount));
        return result;
    }

    @Override
    public ItemStack getDisplayStack() {
        return stack == null ? null : stack.copy();
    }

    @Override
    public boolean isEmpty() {
        return stack == null;
    }

    @Override
    public String typeKey() {
        return TYPE;
    }

    @Override
    public NBTTagCompound writeData() {
        NBTTagCompound data = new NBTTagCompound();
        if (stack != null) {
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBT(stackTag);
            data.setTag(KEY_STACK, stackTag);
        }
        data.setLong(KEY_AMOUNT, amount);
        return data;
    }
}
