package com.xyp.gtnotgood.client.radialmenu.core.model;

import com.xyp.gtnotgood.client.radialmenu.core.Colors;

public class WheelColors {

    public final int ring;
    public final int highlight;
    public final int border;

    public final int highlightBorder;

    public final int background;

    public final int icon;

    public WheelColors(int ring, int highlight, int border, int highlightBorder, int background, int icon) {
        this.ring = ring;
        this.highlight = highlight;
        this.border = border;
        this.highlightBorder = highlightBorder;
        this.background = background;
        this.icon = icon;
    }

    public WheelColors fromAccent(int accentRgb, AccentCoefficients coefficients) {
        return new WheelColors(tone(accentRgb, coefficients.ring), tone(accentRgb, coefficients.highlight),
            tone(accentRgb, coefficients.border), tone(accentRgb, coefficients.highlightBorder),
            tone(accentRgb, coefficients.background), icon);
    }

    private static int tone(int accentRgb, AccentCoefficients.Tone tone) {
        return Colors.withAlpha(Colors.scale(accentRgb, tone.brightness), tone.alpha);
    }
}
