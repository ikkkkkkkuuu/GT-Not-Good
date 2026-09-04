package com.xyp.gtnotgood.ae2thing.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.texture.ExtraBlockTextures;

/** A 16x16 AE2 control-column button that renders an ItemStack as its icon. */
public final class CompactItemTabButton extends GuiTabButton {

    private static final int GL_CLIENT_ALL_ATTRIB_BITS = 0xFFFFFFFF;
    private static final float ICON_SCALE = 0.75F;

    private final ItemStack icon;
    private final RenderItem itemRenderer;

    public CompactItemTabButton(ItemStack icon, String message, RenderItem itemRenderer) {
        super(0, 0, icon, message, itemRenderer);
        this.icon = icon;
        this.itemRenderer = itemRenderer;
        width = 16;
        height = 16;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);
        try {
            field_146123_n = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
            ScreenColor.setGuiColor();
            minecraft.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
            drawTexturedModalRect(xPosition, yPosition, 240, 240, 16, 16);
            ScreenColor.resetGuiColor();

            GL11.glPushMatrix();
            try {
                zLevel = 100.0F;
                itemRenderer.zLevel = 100.0F;
                RenderHelper.enableGUIStandardItemLighting();
                float centerX = xPosition + 8.0F;
                float centerY = yPosition + 8.0F;
                GL11.glTranslatef(centerX, centerY, 0.0F);
                GL11.glScalef(ICON_SCALE, ICON_SCALE, 1.0F);
                GL11.glTranslatef(-centerX, -centerY, 0.0F);
                itemRenderer.renderItemAndEffectIntoGUI(
                    minecraft.fontRenderer,
                    minecraft.renderEngine,
                    icon,
                    xPosition,
                    yPosition);
            } finally {
                GL11.glPopMatrix();
            }
            mouseDragged(minecraft, mouseX, mouseY);
        } finally {
            RenderHelper.disableStandardItemLighting();
            itemRenderer.zLevel = 0.0F;
            zLevel = 0.0F;
            ScreenColor.resetGuiColor();
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
        }
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }
}
