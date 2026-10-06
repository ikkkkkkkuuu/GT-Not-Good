package com.xyp.gtnotgood.utils.text;

import static org.junit.Assert.*;

import org.junit.Test;

import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

public class TextEffectsCompatTest {

    @Test
    public void upstreamPresetKeepsPaletteSpeedAndSavedIdentifier() {
        TextEffectStyle stored = TextEffects.EXOTIC_RAINBOW.withColors(0x33CCFF, 0xFFAA33)
            .withSpeed(2.5f);
        TextEffectStyle rendered = TextEffectsCompat.renderingStyle(stored, true);
        assertEquals(ModList.GTNotLeisure.getID() + ":exotic_rainbow", rendered.rendererId());
        assertEquals(stored.colors(), rendered.colors());
        assertEquals(stored.speed(), rendered.speed(), 0);
        assertEquals(TextEffects.EXOTIC_RAINBOW.rendererId(), stored.rendererId());
        assertSame(stored, TextEffectsCompat.renderingStyle(stored, false));
    }

    @Test
    public void foreignPresetsKeepTheirOwnNamespace() {
        TextEffectStyle foreign = new TextEffectStyle("othermod:custom", TextEffects.EXOTIC_RAINBOW.colors(), 1);
        assertSame(foreign, TextEffectsCompat.renderingStyle(foreign, true));
    }
}
