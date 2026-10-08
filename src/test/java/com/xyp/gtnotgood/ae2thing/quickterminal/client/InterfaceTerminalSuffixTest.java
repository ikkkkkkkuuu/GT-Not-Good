package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.util.IChatComponent;

import org.junit.Test;

/** Regression coverage for suffix packets containing JSON arrays, translation arguments and legacy plain text. */
public class InterfaceTerminalSuffixTest {

    @Test
    public void preservesCircuitAndMoldSiblings() {
        assertSuffix(" 2 32", "{\"text\":\" [2]\",\"extra\":[{\"text\":\" [32]\"}]}");
    }

    @Test
    public void resolvesTranslationArgumentsBeforeRemovingBrackets() {
        assertSuffix(" 2 32", "{\"translate\":\" [%s] [%s]\",\"with\":[\"2\",\"32\"]}");
    }

    @Test
    public void acceptsLegacyTextAndPreservesEscapedCharacters() {
        assertSuffix(" 2 32", " [2]  [32] ");
        assertSuffix(" Mold \"A\"", "{\"text\":\" [Mold \\\"A\\\"]\"}");
    }

    @Test
    public void handlesMissingAndEmptySuffixes() {
        assertNull(InterfaceTerminalSuffix.normalize(null));
        assertEquals("", InterfaceTerminalSuffix.normalize(""));
        assertSuffix("", "{\"text\":\" [] \"}");
    }

    private static void assertSuffix(String expected, String packetSuffix) {
        assertEquals(expected, IChatComponent.Serializer.func_150699_a(InterfaceTerminalSuffix.normalize(packetSuffix))
            .getUnformattedText());
    }
}
