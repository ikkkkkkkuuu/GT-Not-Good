// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.client.packaged;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

/**
 * LGPL-3.0 adaptation of AE2LTPP PackagedCoreItemRenderer: original core base plus a 0.45-scale target item.
 * Target art is rendered from installed TC4 or GT5U items, not copied into GTNG.
 * Registration for the optional Thaumcraft target remains guarded on the client.
 */
public final class PackagedCoreRenderer implements IItemRenderer {

    private final RenderItem renderer = new RenderItem();
    private final java.util.function.Supplier<ItemStack> targetIcon;

    /** Uses the installed mod's controller icon without copying any of its textures. */
    public PackagedCoreRenderer(java.util.function.Supplier<ItemStack> targetIcon) {
        this.targetIcon = targetIcon;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack base = GTNGItemList.BasicPackagedCore.get(1);
        ItemStack target = targetIcon.get();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1, 1, 1, 1);
            if (type == ItemRenderType.INVENTORY) {
                renderer.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), base, 0, 0);
                GL11.glTranslatef(8.88f, 8.88f, 40);
                GL11.glScalef(.45f, .45f, .02f);
            } else {
                if (type == ItemRenderType.ENTITY) GL11.glTranslatef(-.5f, -.5f, 0);
                mc.getTextureManager()
                    .bindTexture(TextureMap.locationItemsTexture);
                IIcon icon = base.getIconIndex();
                ItemRenderer.renderItemIn2D(
                    Tessellator.instance,
                    icon.getMaxU(),
                    icon.getMinV(),
                    icon.getMinU(),
                    icon.getMaxV(),
                    icon.getIconWidth(),
                    icon.getIconHeight(),
                    .0625f);
                GL11.glTranslatef(.78f, .22f, .07f);
                GL11.glScalef(.45f / 16, -.45f / 16, .45f * .02f / 16);
                GL11.glTranslatef(-8, -8, 0);
            }
            renderer.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), target, 0, 0);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
