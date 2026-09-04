package com.xyp.gtnotgood.ae2thing.client.render;

import static appeng.client.gui.AEBaseGui.aeRenderItem;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.ae2thing.client.gui.IGuiDrawSlot;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.storage.data.IAEItemStack;

public class RenderFluidPacketPatternSlot implements ISlotRender {

    private static final Set<Integer> renderingSlots = new HashSet<>();

    @Override
    public Predicate<Slot> get() {
        return slot -> {
            ItemStack stack = slot.getStack();
            // [液滴分类] 可迁原生：判定样板槽是否为流体以决定渲染方式，属图标渲染
            return stack != null
                && (stack.getItem() instanceof ItemFluidPacket || FluidDropCompat.isFluidDrop(stack.getItem()));
        };
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {
        FluidStack fluidStack = null;
        ItemStack itemStack = stack.getItemStack();
        // [液滴分类] 可迁原生：取流体仅用于绘制样板槽流体图标与数量,属渲染显示
        if (FluidDropCompat.isFluidDrop(itemStack.getItem())) {
            fluidStack = FluidDropCompat.getFluidStack(itemStack);
        } else if (itemStack.getItem() instanceof ItemFluidPacket) {
            fluidStack = ItemFluidPacket.getFluidStack(itemStack);
        }
        if (fluidStack != null && fluidStack.amount > 0) {
            int key = System.identityHashCode(slot);
            if (renderingSlots.add(key)) {
                try {
                    draw.getAEBaseGui()
                        .func_146977_a(slot);
                } finally {
                    renderingSlots.remove(key);
                }
            }
            IAEItemStack fake = stack.copy();
            fake.setStackSize(fluidStack.amount);
            aeRenderItem.setAeStack(fake);
            draw.renderStackSize(display, stack, slot);
            return false;
        }
        return true;
    }
}
