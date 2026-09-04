package com.xyp.gtnotgood.ae2thing.client.render;

import static appeng.client.gui.AEBaseGui.aeRenderItem;

import java.util.function.Predicate;

import net.minecraft.inventory.Slot;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.client.gui.IGuiDrawSlot;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.storage.data.IAEItemStack;

public class RenderFluidDrop implements ISlotRender {

    @Override
    public Predicate<Slot> get() {
        // [液滴分类] 可迁原生：仅判定槽位是否为流体以决定是否走本渲染器，属图标渲染
        return slot -> FluidDropCompat.isFluidDrop(
            slot.getStack()
                .getItem());
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {
        // [液滴分类] 可迁原生：取流体仅用于绘制流体图标，属渲染显示
        FluidStack fluidStack = FluidDropCompat.getFluidStack(slot.getStack());
        if (fluidStack == null || fluidStack.getFluid() == null) return true;
        draw.drawWidget(slot.xDisplayPosition, slot.yDisplayPosition, fluidStack.getFluid());
        aeRenderItem.setAeStack(stack);
        draw.renderStackSize(display, stack, slot);
        return false;
    }
}
