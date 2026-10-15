package com.xyp.gtnotgood.client.radialmenu.core.model;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.google.gson.annotations.SerializedName;

public class IconSpec {

    public enum Kind {

        @SerializedName("item")
        Item,

        @SerializedName("sprite")
        Sprite,

        @SerializedName("file")
        File,

        @SerializedName("effect")
        Effect,

        @SerializedName("player")
        Player
    }

    public Kind kind = Kind.Item;

    public String id;

    public int meta;

    public String color;

    public transient ItemStack runtimeStack;

    public static IconSpec fromStack(ItemStack stack) {
        IconSpec icon = item(Item.itemRegistry.getNameForObject(stack.getItem()), stack.getItemDamage());
        icon.runtimeStack = stack.copy();
        return icon;
    }

    public static IconSpec item(String registryName, int meta) {
        IconSpec icon = new IconSpec();
        icon.kind = Kind.Item;
        icon.id = registryName;
        icon.meta = meta;
        return icon;
    }

    public static IconSpec sprite(String spriteName, String color) {
        IconSpec icon = new IconSpec();
        icon.kind = Kind.Sprite;
        icon.id = spriteName;
        icon.color = color;
        return icon;
    }

    public static IconSpec file(String fileName) {
        IconSpec icon = new IconSpec();
        icon.kind = Kind.File;
        icon.id = fileName;
        return icon;
    }

    public static IconSpec player(String playerName) {
        IconSpec icon = new IconSpec();
        icon.kind = Kind.Player;
        icon.id = playerName;
        return icon;
    }

    public static IconSpec effect(String effectName) {
        IconSpec icon = new IconSpec();
        icon.kind = Kind.Effect;
        icon.id = effectName;
        return icon;
    }

    public static IconSpec parse(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (hasPrefix(value, "sprite")) {
            return afterPrefix(value).isEmpty() ? null : sprite(afterPrefix(value), null);
        }
        if (hasPrefix(value, "file")) {
            return afterPrefix(value).isEmpty() ? null : file(afterPrefix(value));
        }
        if (hasPrefix(value, "effect")) {
            return afterPrefix(value).isEmpty() ? null : effect(afterPrefix(value));
        }
        if (hasPrefix(value, "player")) {
            return afterPrefix(value).isEmpty() ? null : player(afterPrefix(value));
        }
        if (hasPrefix(value, "item")) {
            value = afterPrefix(value);
            if (value.isEmpty()) {
                return null;
            }
        }

        int split = value.lastIndexOf(':');
        if (split > 0 && split < value.length() - 1) {
            String tail = value.substring(split + 1);
            if (isDigits(tail)) {
                try {
                    return item(value.substring(0, split), Integer.parseInt(tail));
                } catch (NumberFormatException ignored) {

                }
            }
        }
        return item(value, 0);
    }

    private static boolean hasPrefix(String value, String prefix) {
        return value.regionMatches(true, 0, prefix + ":", 0, prefix.length() + 1);
    }

    private static String afterPrefix(String value) {
        return value.substring(value.indexOf(':') + 1).trim();
    }

    private static boolean isDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                return false;
            }
        }
        return !text.isEmpty();
    }

    public IconSpec copy() {
        IconSpec copy = new IconSpec();
        copy.kind = kind;
        copy.id = id;
        copy.meta = meta;
        copy.color = color;
        copy.runtimeStack = runtimeStack == null ? null : runtimeStack.copy();
        return copy;
    }

    public int rgbOrWhite() {
        return parseRgb(color, 0xFFFFFF);
    }

    public static int parseRgb(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) {
            return fallback;
        }
        return com.xyp.gtnotgood.client.radialmenu.core.Colors.parseArgb(hex, fallback) & 0x00FFFFFF;
    }

    public void normalize() {
        if (kind == null) {
            kind = Kind.Item;
        }
    }
}
