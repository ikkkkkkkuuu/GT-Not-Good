package com.xyp.gtnotgood.client.radialmenu.client.icon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.config.ColorConfig;
import com.xyp.gtnotgood.client.radialmenu.core.Colors;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.StyleResolver;
import com.xyp.gtnotgood.client.radialmenu.core.model.WheelColors;

public final class IconRenderer {

    public static final int ICON_SIZE = 16;

    private static final Map<String, ItemStack> ITEM_CACHE = new HashMap<>();

    private static final ItemStack MISSING = new ItemStack(Item.getItemById(0));

    private static final Set<String> UNRENDERABLE = new HashSet<>();

    private IconRenderer() {}

    public static void draw(IconSpec icon, int x, int y) {
        draw(icon, x, y, ICON_SIZE);
    }

    public static void draw(IconSpec icon, int x, int y, int size) {
        if (icon == null || icon.id == null) {
            return;
        }
        switch (icon.kind) {
            case Item:
                drawItem(icon, x, y, size);
                break;
            case Sprite:
                drawSprite(icon, x, y, size);
                break;
            case File:
                drawFile(icon, x, y, size);
                break;
            case Effect:
                drawEffect(icon, x, y, size);
                break;
            case Player:
                drawPlayerHead(icon, x, y, size);
                break;
            default:
                break;
        }
    }

    private static void drawItem(IconSpec icon, int x, int y, int size) {
        if (UNRENDERABLE.contains(key(icon))) {
            return;
        }
        ItemStack stack = resolveItem(icon);
        if (stack == null || stack == MISSING) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        float scale = size / (float) ICON_SIZE;
        if (scale != 1.0F) {
            GL11.glTranslatef(x, y, 0.0F);
            GL11.glScalef(scale, scale, scale);
            x = 0;
            y = 0;
        }
        RenderHelper.enableGUIStandardItemLighting();

        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        try {
            RenderItem.getInstance().renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            if (icon.runtimeStack != null) {
                RenderItem.getInstance().renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            }
        } catch (Throwable failure) {

            UNRENDERABLE.add(key(icon));
            RadialMenuMod.LOG.warn("Item '" + icon.id + "' cannot be drawn as an icon", failure);
        } finally {

            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPopMatrix();
        }
    }

    private static int tintFor(IconSpec icon) {
        return icon.color == null ? 0xFFFFFF : resolveTint(icon.color);
    }

    public static int resolveTint(String override) {
        WheelColors colors = StyleResolver.resolve(ColorConfig.defaultColors(), ProfileManager.active().style, null);
        return Colors.over(override, colors.icon) & 0x00FFFFFF;
    }

    private static void drawSprite(IconSpec icon, int x, int y, int size) {
        SpriteAtlas.Sprite sprite = SpriteAtlas.find(icon.id);
        if (sprite == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(SpriteAtlas.TEXTURE);

        drawTexturedQuad(x, y, size, tintFor(icon), SpriteAtlas.minU(sprite), SpriteAtlas.minV(sprite),
            SpriteAtlas.maxU(sprite), SpriteAtlas.maxV(sprite));
    }

    private static void drawEffect(IconSpec icon, int x, int y, int size) {
        Potion potion = PotionIcons.find(icon.id);
        if (potion == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(PotionIcons.TEXTURE);
        int index = potion.getStatusIconIndex();
        if (index < 0) {
            return;
        }

        drawTexturedQuad(x, y, size, 0xFFFFFF, PotionIcons.minU(index), PotionIcons.minV(index),
            PotionIcons.maxU(index), PotionIcons.maxV(index));
    }

    private static void drawPlayerHead(IconSpec icon, int x, int y, int size) {
        ResourceLocation head = PlayerHeadIcons.texture(icon.id);
        if (head == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(head);

        drawTexturedQuad(x, y, size, 0xFFFFFF, 0f, 0f, 1f, 1f);
    }

    private static void drawFile(IconSpec icon, int x, int y, int size) {
        ResourceLocation texture = UserIconLoader.texture(icon.id);
        if (texture == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        drawTexturedQuad(x, y, size, tintFor(icon), 0f, 0f, 1f, 1f);
    }

    private static void drawTexturedQuad(int x, int y, int size, int rgb, float minU, float minV, float maxU,
        float maxV) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + size, 0.0, minU, maxV);
        tessellator.addVertexWithUV(x + size, y + size, 0.0, maxU, maxV);
        tessellator.addVertexWithUV(x + size, y, 0.0, maxU, minV);
        tessellator.addVertexWithUV(x, y, 0.0, minU, minV);
        tessellator.draw();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static String key(IconSpec icon) {
        return icon.id + "#" + icon.meta;
    }

    public static ItemStack resolveItem(IconSpec icon) {
        if (icon.runtimeStack != null) return icon.runtimeStack;
        String key = key(icon);
        ItemStack cached = ITEM_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        ItemStack stack = MISSING;
        Object entry = Item.itemRegistry.getObject(icon.id);
        if (entry instanceof Item) {
            stack = new ItemStack((Item) entry, 1, Math.max(0, icon.meta));
        }
        ITEM_CACHE.put(key, stack);
        return stack;
    }

    public static void clearCache() {
        ITEM_CACHE.clear();
        UserIconLoader.refresh();
        PlayerHeadIcons.refresh();
    }
}
