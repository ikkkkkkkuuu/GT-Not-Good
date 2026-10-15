package com.xyp.gtnotgood.client.radialmenu.client.icon;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;

public final class UserIconLoader {

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private static final Map<String, Boolean> FAILED = new HashMap<>();

    private UserIconLoader() {}

    public static ResourceLocation texture(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        ResourceLocation cached = TEXTURES.get(fileName);
        if (cached != null) {
            return cached;
        }
        if (FAILED.containsKey(fileName)) {
            return null;
        }

        File file = new File(ProfileStorage.iconsDir(), fileName);
        if (!file.isFile()) {
            FAILED.put(fileName, Boolean.TRUE);
            RadialMenuMod.LOG.warn("Icon file not found: " + file.getAbsolutePath());
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IllegalArgumentException("not a readable image");
            }
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation(RadialMenuMod.MODID + "_icon", new DynamicTexture(image));
            TEXTURES.put(fileName, location);
            return location;
        } catch (Exception e) {
            FAILED.put(fileName, Boolean.TRUE);
            RadialMenuMod.LOG.error("Could not load icon " + fileName, e);
            return null;
        }
    }

    public static List<String> listFiles() {
        List<String> names = new ArrayList<>();
        File[] files = ProfileStorage.iconsDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
                    names.add(file.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    public static void refresh() {
        TEXTURES.clear();
        FAILED.clear();
    }
}
