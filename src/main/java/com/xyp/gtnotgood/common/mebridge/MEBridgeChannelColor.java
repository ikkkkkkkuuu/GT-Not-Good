package com.xyp.gtnotgood.common.mebridge;

/** Shared sender-defined colours for channel UIs and wireless-node visualisation. */
final class MEBridgeChannelColor {

    private static final int[] PALETTE = { 0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA,
        0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0xFF55FF, 0x55FFFF };

    private MEBridgeChannelColor() {}

    static int[] palette() {
        return PALETTE.clone();
    }

    static int sanitize(int rgb) {
        return rgb & 0xFFFFFF;
    }

    static int defaultFor(String channelName) {
        String channel = channelName == null ? "" : channelName;
        return PALETTE[Math.floorMod(channel.hashCode(), PALETTE.length)];
    }

    static int textColor(int rgb) {
        int color = sanitize(rgb);
        int luminance = ((color >> 16) & 0xFF) * 299 + ((color >> 8) & 0xFF) * 587 + (color & 0xFF) * 114;
        return luminance >= 140_000 ? 0xFF202020 : 0xFFFFFFFF;
    }
}
