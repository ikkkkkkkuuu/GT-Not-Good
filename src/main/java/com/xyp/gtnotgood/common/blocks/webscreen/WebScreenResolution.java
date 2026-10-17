package com.xyp.gtnotgood.common.blocks.webscreen;

import java.util.Locale;

public enum WebScreenResolution {

    FullHd("1080p", 1920, 1080),
    QuadHd("2k", 2560, 1440),
    UltraHd("4k", 3840, 2160);

    public final String option;
    public final int width;
    public final int height;

    WebScreenResolution(String option, int width, int height) {
        this.option = option;
        this.width = width;
        this.height = height;
    }

    public static WebScreenResolution parse(String value) {
        if (value == null) return FullHd;
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "2k", "1440p", "2560x1440" -> QuadHd;
            case "4k", "2160p", "3840x2160" -> UltraHd;
            default -> FullHd;
        };
    }
}
