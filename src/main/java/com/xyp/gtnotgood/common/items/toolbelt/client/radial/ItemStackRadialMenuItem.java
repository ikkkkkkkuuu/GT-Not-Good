package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class ItemStackRadialMenuItem extends RadialMenuItem {

    private static final RenderItem itemRenderer = new RenderItem();

    private final ItemStack stack;

    public ItemStack getStack() {
        return stack;
    }

    public ItemStackRadialMenuItem(GenericRadialMenu menu, ItemStack stack) {
        super(menu);
        this.stack = stack;
    }

    @Override
    public void draw(DrawingContext context) {
        if (stack == null || stack.getItem() == null) {
            return;
        }

        // Draw item
        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();

        float x = context.x - 8;
        float y = context.y - 8;
        itemRenderer.renderItemAndEffectIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            (int) x,
            (int) y);

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();

        // Draw stack size
        if (stack.stackSize > 1) {
            String sizeStr = String.valueOf(stack.stackSize);
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 300.0F);
            Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
                sizeStr,
                (int) (context.x + 8 - Minecraft.getMinecraft().fontRenderer.getStringWidth(sizeStr)),
                (int) (context.y + 8),
                0xFFFFFF);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void drawTooltip(DrawingContext context) {
        if (stack != null && stack.getItem() != null) {
            String name = stack.getDisplayName();
            context.drawingHelper.drawString(
                Minecraft.getMinecraft().fontRenderer,
                name,
                (int) context.x - Minecraft.getMinecraft().fontRenderer.getStringWidth(name) / 2,
                (int) context.y - 12,
                0xFFFFFF);
        }
    }

    @Override
    public boolean onClick() {
        // Handled by anonymous subclass
        return false;
    }

    private boolean visible = true;

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
