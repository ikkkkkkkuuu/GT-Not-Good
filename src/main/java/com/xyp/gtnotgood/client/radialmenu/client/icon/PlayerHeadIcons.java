package com.xyp.gtnotgood.client.radialmenu.client.icon;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;

public final class PlayerHeadIcons {

    private static final int FACE = 8;

    private static final int FACE_U = 8;

    private static final int FACE_V = 8;

    private static final int HAT_U = 40;

    private static final int HAT_V = 8;

    private static final int SKIN_WIDTH = 64;

    private static final long REFRESH_MS = 10_000L;

    private static final Map<String, Head> HEADS = new HashMap<>();

    private PlayerHeadIcons() {}

    private static final class Head {

        ResourceLocation location;

        DynamicTexture texture;

        int size;

        long lastAttempt;

        boolean saved;
    }

    public static ResourceLocation texture(String name) {
        String key = key(name);
        if (key == null) {
            return null;
        }

        Head head = HEADS.get(key);
        if (head == null) {
            head = new Head();
            HEADS.put(key, head);
            loadFromDisk(key, head);
        }

        AbstractClientPlayer player = findLoaded(key);
        if (player != null && System.currentTimeMillis() - head.lastAttempt >= REFRESH_MS) {
            head.lastAttempt = System.currentTimeMillis();
            capture(key, player.getLocationSkin(), head);
        }
        return head.location;
    }

    public static List<String> listCached() {
        List<String> names = new ArrayList<>();
        File[] files = headsDir().listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile() && fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
                    names.add(fileName.substring(0, fileName.length() - 4));
                }
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static List<String> listLoaded() {
        List<String> names = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return names;
        }
        for (Object entity : mc.theWorld.playerEntities) {
            if (entity instanceof EntityPlayer) {
                String name = ((EntityPlayer) entity).getCommandSenderName();
                if (name != null && !name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void refresh() {
        for (Head head : HEADS.values()) {
            release(head);
        }
        HEADS.clear();
    }

    private static void release(Head head) {
        if (head.location != null) {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(head.location);
        }
        head.location = null;
        head.texture = null;
        head.size = 0;
    }

    private static void capture(String key, ResourceLocation skin, Head head) {
        if (skin == null) {
            return;
        }
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(skin);

            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            if (width < SKIN_WIDTH || height < width / 2) {
                return;
            }
            int scale = width / SKIN_WIDTH;

            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);

            int size = FACE * scale;
            int[] face = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int skinPixel = read(pixels, width, FACE_U * scale + x, FACE_V * scale + y);
                    int hatPixel = read(pixels, width, HAT_U * scale + x, HAT_V * scale + y);
                    face[y * size + x] = over(hatPixel, skinPixel);
                }
            }

            store(key, head, face, size);
        } catch (Throwable failure) {

            RadialMenuMod.LOG.warn("Could not read the skin of " + key, failure);
        }
    }

    private static int read(ByteBuffer pixels, int width, int x, int y) {
        int index = (y * width + x) * 4;
        int r = pixels.get(index) & 0xFF;
        int g = pixels.get(index + 1) & 0xFF;
        int b = pixels.get(index + 2) & 0xFF;
        int a = pixels.get(index + 3) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int over(int top, int bottom) {
        int topAlpha = top >>> 24;
        if (topAlpha == 0) {
            return bottom;
        }
        if (topAlpha == 255) {
            return top;
        }
        float alpha = topAlpha / 255.0F;
        int r = Math.round(((top >> 16) & 0xFF) * alpha + ((bottom >> 16) & 0xFF) * (1 - alpha));
        int g = Math.round(((top >> 8) & 0xFF) * alpha + ((bottom >> 8) & 0xFF) * (1 - alpha));
        int b = Math.round((top & 0xFF) * alpha + (bottom & 0xFF) * (1 - alpha));
        int a = Math.max(bottom >>> 24, topAlpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void store(String key, Head head, int[] face, int size) {
        if (head.texture == null || head.size != size) {
            release(head);
            head.texture = new DynamicTexture(size, size);
            head.size = size;
            head.location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation(RadialMenuMod.MODID + "_head", head.texture);
            head.saved = false;
        }

        int[] data = head.texture.getTextureData();
        boolean changed = false;
        for (int i = 0; i < face.length && i < data.length; i++) {
            if (data[i] != face[i]) {
                data[i] = face[i];
                changed = true;
            }
        }
        if (changed || !head.saved) {
            head.texture.updateDynamicTexture();
            head.saved = save(key, face, size);
        }
    }

    private static boolean save(String key, int[] face, int size) {
        File file = headFile(key);
        if (file == null) {
            return false;
        }
        try {
            File dir = file.getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return false;
            }
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, size, size, face, 0, size);
            ImageIO.write(image, "png", file);
            return true;
        } catch (Exception failure) {
            RadialMenuMod.LOG.warn("Could not cache the head of " + key, failure);
            return false;
        }
    }

    private static void loadFromDisk(String key, Head head) {
        File file = headFile(key);
        if (file == null || !file.isFile()) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() <= 0) {
                return;
            }
            int size = image.getWidth();
            int[] face = new int[size * size];
            image.getRGB(0, 0, size, Math.min(size, image.getHeight()), face, 0, size);
            store(key, head, face, size);
            head.saved = true;
        } catch (Exception failure) {
            RadialMenuMod.LOG.warn("Could not read the cached head of " + key, failure);
        }
    }

    private static AbstractClientPlayer findLoaded(String key) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return null;
        }
        for (Object entity : mc.theWorld.playerEntities) {
            if (!(entity instanceof AbstractClientPlayer)) {
                continue;
            }
            AbstractClientPlayer player = (AbstractClientPlayer) entity;
            if (key.equals(key(player.getCommandSenderName()))) {
                return player;
            }
        }
        return null;
    }

    private static String key(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            builder.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' ? c : '_');
        }
        return builder.toString();
    }

    private static File headsDir() {
        return new File(new File(ProfileStorage.rootDir(), "cache"), "heads");
    }

    private static File headFile(String key) {
        return key == null || key.isEmpty() ? null : new File(headsDir(), key + ".png");
    }
}
