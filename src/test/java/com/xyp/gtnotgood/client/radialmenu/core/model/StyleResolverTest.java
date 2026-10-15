package com.xyp.gtnotgood.client.radialmenu.core.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The colour chain: mod config, then profile, then the menu on screen.
 *
 * <p>
 * Accents are not part of it - they are how the editor fills a level in, and by the time a wheel is drawn there is
 * nothing left to derive. What is worth pinning down here is that each level speaks only for what it names, and that
 * a six-digit colour keeps the opacity of whatever it replaces.
 */
public class StyleResolverTest {

    private static final int RING = 0x99101010;
    private static final int HIGHLIGHT = 0xCC4A90D9;
    private static final int BORDER = 0x60FFFFFF;
    private static final int HIGHLIGHT_BORDER = 0xCCFFFFFF;
    private static final int BACKGROUND = 0x80101010;
    private static final int ICON = 0xFFFFFFFF;

    private static WheelColors defaults() {
        return new WheelColors(RING, HIGHLIGHT, BORDER, HIGHLIGHT_BORDER, BACKGROUND, ICON);
    }

    /** Deliberately lopsided, so a test cannot pass by reading the wrong tone. */
    private static AccentCoefficients coefficients() {
        return new AccentCoefficients(new AccentCoefficients.Tone(0x99, 50), new AccentCoefficients.Tone(0xCC, 100),
            new AccentCoefficients.Tone(0x60, 150), new AccentCoefficients.Tone(0xAA, 200),
            new AccentCoefficients.Tone(0x80, 0));
    }

    @Test
    public void nothingOverriddenLeavesTheConfigAlone() {
        WheelColors colors = StyleResolver.resolve(defaults(), null, null);

        assertEquals(RING, colors.ring);
        assertEquals(HIGHLIGHT, colors.highlight);
        assertEquals(BORDER, colors.border);
        assertEquals(HIGHLIGHT_BORDER, colors.highlightBorder);
        assertEquals(BACKGROUND, colors.background);
        assertEquals(ICON, colors.icon);
    }

    @Test
    public void aLevelSpeaksOnlyForWhatItNames() {
        MenuStyle profile = new MenuStyle();
        profile.ringColor = "0x40000000";

        WheelColors colors = StyleResolver.resolve(defaults(), profile, null);

        assertEquals(0x40000000, colors.ring);
        assertEquals("the rest is still the config's", HIGHLIGHT, colors.highlight);
        assertEquals(BORDER, colors.border);
        assertEquals(HIGHLIGHT_BORDER, colors.highlightBorder);
        assertEquals(BACKGROUND, colors.background);
    }

    @Test
    public void aMenuIsNearerThanItsProfile() {
        MenuStyle profile = new MenuStyle();
        profile.highlightColor = "0xCC00FF00";
        MenuStyle menu = new MenuStyle();
        menu.highlightColor = "0xCCFF0000";

        assertEquals(0xCCFF0000, StyleResolver.resolve(defaults(), profile, menu).highlight);
        assertEquals(0xCC00FF00, StyleResolver.resolve(defaults(), profile, null).highlight);
    }

    @Test
    public void aSixDigitColourKeepsTheOpacityItReplaces() {
        // The hazard this mod keeps running into: read as eight digits, a six-digit value means alpha zero and the
        // ring vanishes.
        MenuStyle profile = new MenuStyle();
        profile.ringColor = "#920A0A";

        assertEquals(0x99920A0A, StyleResolver.resolve(defaults(), profile, null).ring);
    }

    @Test
    public void anAccentFillsInEveryColourItWrites() {
        // What the editor puts into a profile when a colour is picked as an accent. The wheel never sees an accent:
        // by the time it draws, these are ordinary colours in the file.
        WheelColors colors = defaults().fromAccent(0x8040C0, coefficients());

        assertEquals("half brightness", 0x99402060, colors.ring);
        assertEquals("the accent as picked", 0xCC8040C0, colors.highlight);
        assertEquals("halfway to white", 0x60C0A0E0, colors.border);
        assertEquals("white", 0xAAFFFFFF, colors.highlightBorder);
        assertEquals("black", 0x80000000, colors.background);
    }

    @Test
    public void anAccentLeavesTheIconTintAlone() {
        // An icon that changes hue with the ring stops saying what it is, which is the one thing it is for.
        assertEquals(ICON, defaults().fromAccent(0x8040C0, coefficients()).icon);
    }
}
