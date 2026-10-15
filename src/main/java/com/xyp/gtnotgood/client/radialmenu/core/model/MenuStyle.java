package com.xyp.gtnotgood.client.radialmenu.core.model;

public class MenuStyle {

    public String ringColor;

    public String highlightColor;

    public String iconColor;

    public String borderColor;

    public String highlightBorderColor;

    public String backgroundColor;

    public boolean isEmpty() {
        return isBlank(ringColor) && isBlank(highlightColor)
            && isBlank(iconColor)
            && isBlank(borderColor)
            && isBlank(highlightBorderColor)
            && isBlank(backgroundColor);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static MenuStyle of(String ringColor, String highlightColor, String iconColor, String borderColor,
        String highlightBorderColor, String backgroundColor) {
        MenuStyle style = new MenuStyle();
        style.ringColor = isBlank(ringColor) ? null : ringColor;
        style.highlightColor = isBlank(highlightColor) ? null : highlightColor;
        style.iconColor = isBlank(iconColor) ? null : iconColor;
        style.borderColor = isBlank(borderColor) ? null : borderColor;
        style.highlightBorderColor = isBlank(highlightBorderColor) ? null : highlightBorderColor;
        style.backgroundColor = isBlank(backgroundColor) ? null : backgroundColor;
        return style.isEmpty() ? null : style;
    }
}
