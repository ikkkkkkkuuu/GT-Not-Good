package com.xyp.gtnotgood.common.machines.hatch.me;

import static org.junit.Assert.assertEquals;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import org.junit.Test;

public class ChatComponentInterfaceNameSuffixTest {

    @Test
    public void roundTripPreservesBothPoliciesAndTranslationArguments() {
        var standard = new ChatComponentText(" [2]").appendSibling(new ChatComponentTranslation(" - %s", "Assembler"))
            .appendSibling(new ChatComponentText(" [32] {Mold \"A\"}"));
        var suffix = new ChatComponentInterfaceNameSuffix(standard, new ChatComponentText(" [2] [32]"));
        var restored = new ChatComponentInterfaceNameSuffix();
        restored.deserialize(suffix.serialize());
        assertEquals(" [2] - Assembler [32] {Mold \"A\"}", restored.getText(false));
        assertEquals(" [2] [32]", restored.getText(true));
        assertEquals(suffix.serialize(), restored.serialize());
    }

    @Test
    public void selectingOneClientPolicyDoesNotChangeTheOther() {
        var suffix = new ChatComponentInterfaceNameSuffix(new ChatComponentText(" [5] - Assembler"),
            new ChatComponentText(" [5]"));
        var copied = (ChatComponentInterfaceNameSuffix) suffix.createCopy();
        assertEquals(" [5]", copied.getText(true));
        assertEquals(" [5] - Assembler", suffix.getText(false));
        assertEquals(" [5]", suffix.getText(true));
        assertEquals(" [5] - Assembler", copied.getText(false));
    }

    @Test
    public void emptyOwnSuffixSuppressesTheForeignCategory() {
        var suffix = new ChatComponentInterfaceNameSuffix(new ChatComponentText(" - Assembler"), null);
        var restored = new ChatComponentInterfaceNameSuffix();
        restored.deserialize(suffix.serialize());
        assertEquals(" - Assembler", restored.getText(false));
        assertEquals("", restored.getText(true));
    }

    @Test
    public void acceptsMissingStandardAndPreferredSuffixes() {
        var suffix = new ChatComponentInterfaceNameSuffix();
        var restored = new ChatComponentInterfaceNameSuffix();
        restored.deserialize(suffix.serialize());
        assertEquals("", restored.getText(false));
        assertEquals("", restored.getText(true));
    }
}
