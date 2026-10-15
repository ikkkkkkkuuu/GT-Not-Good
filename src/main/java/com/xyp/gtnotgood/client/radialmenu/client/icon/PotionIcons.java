package com.xyp.gtnotgood.client.radialmenu.client.icon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

public final class PotionIcons {

    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    private static final int CELL = 18;
    private static final int COLUMNS = 8;
    private static final int SHEET = 256;
    private static final int TOP = 198;

    private static final Map<String, Potion> BY_NAME = new HashMap<>();

    private static List<String> names;

    private PotionIcons() {}

    public static List<String> list() {
        load();
        return names;
    }

    public static Potion find(String name) {
        load();
        return BY_NAME.get(name);
    }

    public static float minU(int index) {
        return (index % COLUMNS) * CELL / (float) SHEET;
    }

    public static float minV(int index) {
        return (TOP + (index / COLUMNS) * CELL) / (float) SHEET;
    }

    public static float maxU(int index) {
        return minU(index) + CELL / (float) SHEET;
    }

    public static float maxV(int index) {
        return minV(index) + CELL / (float) SHEET;
    }

    private static void load() {
        if (names != null) {
            return;
        }
        names = new ArrayList<>();
        for (Potion potion : Potion.potionTypes) {

            if (potion == null || !potion.hasStatusIcon()) {
                continue;
            }
            names.add(potion.getName());
            BY_NAME.put(potion.getName(), potion);
        }
    }
}
