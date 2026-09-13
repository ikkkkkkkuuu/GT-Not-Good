package com.xyp.ldlib.gui.texture;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

/** LDLib2-style item icon adapted to the 1.7.10 renderer; contains no inventory slot or transfer logic. */
public final class ItemStackTexture implements IGuiTexture {

    private static final RenderItem RENDERER = new RenderItem();
    private final Supplier<ItemStack> stack;

    public ItemStackTexture(Supplier<ItemStack> stack) {
        this.stack = stack;
    }

    public ItemStackTexture(ItemStack stack) {
        ItemStack snapshot = stack == null ? null : stack.copy();
        this.stack = () -> snapshot;
    }

    @Override
    public void draw(int mx, int my, int x, int y, int width, int height) {
        ItemStack item = stack.get();
        if (item == null || item.getItem() == null || width <= 0 || height <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float oldZ = RENDERER.zLevel;
        try {
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(width / 16f, height / 16f, 1);
            GL11.glColor4f(1, 1, 1, 1);
            RenderHelper.enableGUIStandardItemLighting();
            RENDERER.zLevel = 0;
            RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), item, 0, 0);
            RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), item, 0, 0);
        } finally {
            RENDERER.zLevel = oldZ;
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
