package com.xyp.gtnotgood.client.radialmenu.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The one-string icon form a script writes, where a profile writes an object. */
public class IconSpecParseTest {

    @Test
    public void noPrefixMeansAnItem() {
        IconSpec icon = IconSpec.parse("minecraft:stone");
        assertEquals(IconSpec.Kind.Item, icon.kind);
        assertEquals("minecraft:stone", icon.id);
        assertEquals(0, icon.meta);
    }

    @Test
    public void metadataIsReadFromTheEnd() {
        // Not from the second colon: a registry name already has one, and this one has two of its own.
        IconSpec icon = IconSpec.parse("gregtech:gt.metaitem.01:32000");
        assertEquals("gregtech:gt.metaitem.01", icon.id);
        assertEquals(32000, icon.meta);
    }

    @Test
    public void aTrailingSegmentThatIsNotANumberStaysPartOfTheName() {
        IconSpec icon = IconSpec.parse("minecraft:stained_hardened_clay");
        assertEquals("minecraft:stained_hardened_clay", icon.id);
        assertEquals(0, icon.meta);
    }

    @Test
    public void readsTheOtherThreeKinds() {
        IconSpec sprite = IconSpec.parse("sprite:phosphor:house");
        assertEquals(IconSpec.Kind.Sprite, sprite.kind);
        assertEquals("phosphor:house", sprite.id);

        IconSpec file = IconSpec.parse("file:backpack.png");
        assertEquals(IconSpec.Kind.File, file.kind);
        assertEquals("backpack.png", file.id);

        IconSpec effect = IconSpec.parse("effect:potion.moveSpeed");
        assertEquals(IconSpec.Kind.Effect, effect.kind);
        assertEquals("potion.moveSpeed", effect.id);
    }

    @Test
    public void anItemMayNameItsKindOutright() {
        IconSpec icon = IconSpec.parse("item:minecraft:wool:14");
        assertEquals(IconSpec.Kind.Item, icon.kind);
        assertEquals("minecraft:wool", icon.id);
        assertEquals(14, icon.meta);
    }

    @Test
    public void prefixesAreReadWhateverTheirCase() {
        assertEquals(IconSpec.Kind.Sprite, IconSpec.parse("Sprite:phosphor:gear").kind);
    }

    @Test
    public void nothingToDrawIsNullRatherThanAnEmptyIcon() {
        assertNull(IconSpec.parse(null));
        assertNull(IconSpec.parse(""));
        assertNull(IconSpec.parse("   "));
        assertNull(IconSpec.parse("sprite:"));
        assertNull(IconSpec.parse("file:  "));
    }

    @Test
    public void metadataTooLargeForAnIntLeavesTheNameWhole() {
        // Guessing at a truncated metadata value would draw the wrong item with complete confidence.
        IconSpec icon = IconSpec.parse("mod:item:99999999999999");
        assertEquals("mod:item:99999999999999", icon.id);
        assertEquals(0, icon.meta);
    }

    @Test
    public void surroundingSpaceIsIgnored() {
        IconSpec icon = IconSpec.parse("  minecraft:torch  ");
        assertEquals("minecraft:torch", icon.id);
    }
}
