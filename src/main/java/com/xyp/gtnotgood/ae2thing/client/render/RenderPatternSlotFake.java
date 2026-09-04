package com.xyp.gtnotgood.ae2thing.client.render;

import static codechicken.nei.guihook.GuiContainerManager.drawItem;

import java.util.function.Predicate;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.ae2thing.client.gui.IGuiDrawSlot;
import com.xyp.gtnotgood.ae2thing.client.gui.container.slot.SlotPatternFake;
import com.xyp.gtnotgood.ae2thing.util.Ae2ReflectClient;
import com.xyp.gtnotgood.ae2thing.util.Util;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.me.ItemRepo;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.util.item.AEItemStack;

public class RenderPatternSlotFake implements ISlotRender {

    private static final ItemStack PATTERN = AEApi.instance()
        .definitions()
        .items()
        .encodedPattern()
        .maybeStack(1)
        .orNull();

    @Override
    public Predicate<Slot> get() {
        return slot -> (slot instanceof SlotPatternFake || slot instanceof SlotFakeCraftingMatrix);
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {
        return true;
    }

    @Override
    public void drawCallback(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {
        if ((slot instanceof SlotPatternFake fake && !fake.isHidden()) || (slot instanceof SlotFakeCraftingMatrix)) {
            IDisplayRepo iDisplayRepo = Util.getDisplayRepo(draw.getAEBaseGui());
            if (iDisplayRepo instanceof ItemRepo repo) {
                IItemList<IAEStack<?>> list = Ae2ReflectClient.getList(repo);
                IAEItemStack what = stack;
                // [液滴分类] 可迁原生：仅用于在展示列表查找该流体以决定是否绘制可合成叠加图标,属显示
                if (FluidDropCompat.isFluidDrop(stack.getItem())) {
                    what = stack;
                } else if (stack.getItem() instanceof ItemFluidPacket) {
                    // [液滴分类] 可迁原生：构造展示液滴仅供 findPrecise 匹配展示列表画角标,不参与合成
                    what = AEItemStack.create(FluidDropCompat.newDisplayStack(ItemFluidPacket.getFluidStack(stack)));
                }
                IAEStack<?> storedItem = list.findPrecise(what);
                if (storedItem != null && storedItem.isCraftable()) {
                    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
                    GL11.glPushMatrix();
                    GL11.glTranslatef(0, 0, 200f);
                    GL11.glScalef(0.4f, 0.4f, 0.4f);
                    drawItem((int) ((slot.xDisplayPosition + 10) * 2.5), (int) (slot.yDisplayPosition * 2.5), PATTERN);
                    GL11.glTranslatef(0, 0, -200f);
                    RenderHelper.disableStandardItemLighting();
                    GL11.glPopMatrix();
                    GL11.glPopAttrib();
                }
            }
        }
    }
}
