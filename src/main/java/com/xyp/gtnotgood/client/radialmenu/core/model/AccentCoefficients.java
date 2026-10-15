package com.xyp.gtnotgood.client.radialmenu.core.model;

public class AccentCoefficients {

    public static class Tone {

        public final int alpha;

        public final int brightness;

        public Tone(int alpha, int brightness) {
            this.alpha = clamp(alpha, 0, 255);
            this.brightness = clamp(brightness, 0, 200);
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    public final Tone ring;
    public final Tone highlight;
    public final Tone border;
    public final Tone highlightBorder;
    public final Tone background;

    public AccentCoefficients(Tone ring, Tone highlight, Tone border, Tone highlightBorder, Tone background) {
        this.ring = ring;
        this.highlight = highlight;
        this.border = border;
        this.highlightBorder = highlightBorder;
        this.background = background;
    }
}
