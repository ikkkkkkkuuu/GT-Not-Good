package com.xyp.gtnotgood.ae2thing.loader;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.api.AE2ThingAPI;
import com.xyp.gtnotgood.ae2thing.api.Constants;
import com.xyp.gtnotgood.ae2thing.api.adapter.pattern.FCPatternTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.pattern.IRecipeHandler;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.Platform;

public class PatternTerminalMouseWheelLoader implements Runnable {

    /**
     * 把滚轮换料应用到样板编辑槽：把持有 {@code in} 的槽整体替换成 {@code out}。
     * {@code in}/{@code out} 可能是物品（{@link ItemStack}）也可能是流体（{@link FluidStack}，
     * 由客户端流体切换分支下发）。流体分支按流体类型比对槽内的 AE2FC 液滴，命中后换成目标流体液滴。
     */
    private static void applySwap(IInventory inv, OrderStack<?> inOrder, OrderStack<?> outOrder) {
        Object inStack = inOrder.getStack();
        Object outStack = outOrder.getStack();
        if (inStack instanceof FluidStack inFluid && outStack instanceof FluidStack outFluid) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                FluidStack slotFluid = FluidDropCompat.getFluidStack(inv.getStackInSlot(i));
                if (slotFluid != null && slotFluid.getFluid() == inFluid.getFluid()) {
                    // [液滴分类] 可迁原生：样板内容装载（滚轮换料切等价流体），不参与合成计算
                    FluidStack replacement = new FluidStack(outFluid.getFluid(), slotFluid.amount);
                    inv.setInventorySlotContents(i, FluidDropCompat.newStack(replacement));
                }
            }
        } else if (inStack instanceof ItemStack in && outStack instanceof ItemStack out) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (Platform.isSameItemPrecise(inv.getStackInSlot(i), in)) {
                    inv.setInventorySlotContents(i, out);
                }
            }
        }
    }

    @Override
    public void run() {
        IRecipeHandler handler = (container, inputs, outputs, identifier, adapter, message) -> {
            if (container instanceof IAEAppEngInventory inventory) {
                IInventory inv = adapter.getInventoryByName(container, adapter.getCraftingInvName());
                applySwap(inv, inputs.get(0), outputs.get(0));
                container.onCraftMatrixChanged(inv);
                inventory.saveChanges();
            }
        };

        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new FCPatternTerminal(ContainerPatternTerm.class))
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, handler);
        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new FCPatternTerminal(ContainerPatternTermEx.class))
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, handler);
    }

}
