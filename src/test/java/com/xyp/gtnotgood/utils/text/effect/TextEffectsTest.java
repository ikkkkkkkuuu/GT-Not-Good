package com.xyp.gtnotgood.utils.text.effect;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import org.junit.Test;

/** Checks persisted text syntax, nested scopes and malformed user input without an OpenGL context. */
public class TextEffectsTest {

    @Test
    public void compactSyntaxAndPaletteRoundTrip() {
        TextEffectStyle style = TextEffectFormat.readInline("ba;#fc0;2");
        assertEquals(TextEffects.BURNISHED_AURIC.rendererId(), style.rendererId());
        assertEquals(Arrays.asList(0xFFCC00), style.colors());
        assertEquals(2, style.speed(), 0);
        assertEquals(
            style,
            TextEffectFormat.readInline(
                TextEffects.format(style)
                    .substring(2)
                    .replace("}", "")));
        assertEquals("中文", TextEffects.plainText(TextEffects.apply("中文", style)));
    }

    @Test
    public void nestedSpansRestoreOuterEffect() {
        String text = TextEffects
            .apply("A" + TextEffects.apply("B", TextEffects.CALAMITY_RED) + "C", TextEffects.EXOTIC_RAINBOW);
        EffectTextParser.Parsed parsed = EffectTextParser.parse(text);
        assertEquals("ABC", parsed.plainText());
        assertEquals(
            TextEffects.EXOTIC_RAINBOW,
            parsed.runs()
                .get(0)
                .style());
        assertEquals(
            TextEffects.CALAMITY_RED,
            parsed.runs()
                .get(1)
                .style());
        assertEquals(
            TextEffects.EXOTIC_RAINBOW,
            parsed.runs()
                .get(2)
                .style());
    }

    @Test
    public void rejectsInvalidOrDuplicateOptions() {
        for (String header : Arrays.asList(
            "ba;s=-1",
            "ba;s=NaN",
            "ba;s=Infinity",
            "ba;s=1;s=2",
            "ba;c=#xyz",
            "ba;unknown=1",
            "ba;c=fff;c=000")) {
            assertNull(header, TextEffectFormat.readInline(header));
        }
    }

    @Test
    public void unknownRendererRemainsEditableLiteral() {
        String input = "&{missing}PLAIN";
        EffectTextParser.Cursor cursor = new EffectTextParser.Cursor(
            input,
            UnaryOperator.identity(),
            false,
            id -> false);
        StringBuilder output = new StringBuilder();
        for (EffectTextParser.Token token; (token = cursor.next()) != null;) output.append(token.text());
        assertEquals(input, output.toString());
    }

    @Test
    public void resetAndNativeColorPreserveExpectedEffectScope() {
        EffectTextParser.Parsed parsed = EffectTextParser.parse("&{ba}A&cB&rC");
        assertEquals(
            TextEffects.BURNISHED_AURIC,
            parsed.runs()
                .get(0)
                .style());
        assertEquals(
            Arrays.asList(0xFF5555),
            parsed.runs()
                .get(1)
                .style()
                .colors());
        assertNull(
            parsed.runs()
                .get(2)
                .style());
    }

    @Test
    public void paletteIsImmutableAndLiteralMarkersCanBeEscaped() {
        ArrayList<Integer> colors = new ArrayList<>(Arrays.asList(0x123456));
        TextEffectStyle style = new TextEffectStyle(TextEffects.EXOTIC_RAINBOW.rendererId(), colors, 0);
        colors.set(0, 0);
        assertEquals(Arrays.asList(0x123456), style.colors());
        String literal = "A\u2063B";
        assertEquals(literal, TextEffects.plainText(TextEffects.escapeLiteral(literal)));
    }
}
