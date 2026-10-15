package com.xyp.gtnotgood.client.radialmenu.core;

public final class Colors {

    private Colors() {}

    public static boolean hasAlpha(String value) {
        return digits(value).length() == 8;
    }

    public static int parseArgb(String value, int fallback) {
        String digits = digits(value);
        if (digits.length() != 6 && digits.length() != 8) {
            return fallback;
        }
        try {
            return (int) Long.parseLong(digits, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static int resolve(String override, String inherited, int fallback) {
        return over(override, parseArgb(inherited, fallback));
    }

    public static int over(String override, int base) {
        if (override == null || override.trim().isEmpty()) {
            return base;
        }
        int picked = parseArgb(override, base);
        if (hasAlpha(override)) {
            return picked;
        }
        return (base & 0xFF000000) | (picked & 0x00FFFFFF);
    }

    public static int scale(int rgb, int percent) {
        if (percent == 100) {
            return rgb & 0x00FFFFFF;
        }
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        if (percent < 100) {
            float factor = Math.max(0, percent) / 100f;
            red = Math.round(red * factor);
            green = Math.round(green * factor);
            blue = Math.round(blue * factor);
        } else {
            float towardsWhite = Math.min(100, percent - 100) / 100f;
            red = Math.round(red + (255 - red) * towardsWhite);
            green = Math.round(green + (255 - green) * towardsWhite);
            blue = Math.round(blue + (255 - blue) * towardsWhite);
        }
        return (red << 16) | (green << 8) | blue;
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((Math.max(0, Math.min(255, alpha))) << 24) | (rgb & 0x00FFFFFF);
    }

    public static String toHex8(int argb) {
        return String.format("0x%08X", argb);
    }

    private static String digits(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            return cleaned.substring(2);
        }
        if (cleaned.startsWith("#")) {
            return cleaned.substring(1);
        }
        return cleaned;
    }
}
