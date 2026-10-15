package com.xyp.gtnotgood.client.radialmenu.core.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** What a keybind action asks to be held alongside it. */
public class ModifiersTest {

    private static ActionSpec keybind() {
        return new ActionSpec(ActionTypes.KEYBIND);
    }

    @Test
    public void nothingIsAskedForByDefault() {
        Modifiers modifiers = Modifiers.of(keybind());
        assertFalse(modifiers.any());
        assertFalse(modifiers.shift);
        assertFalse(modifiers.ctrl);
        assertFalse(modifiers.alt);
    }

    @Test
    public void readsEachOne() {
        assertTrue(Modifiers.of(keybind().set(ActionTypes.PARAM_SHIFT, "true")).shift);
        assertTrue(Modifiers.of(keybind().set(ActionTypes.PARAM_CTRL, "true")).ctrl);
        assertTrue(Modifiers.of(keybind().set(ActionTypes.PARAM_ALT, "true")).alt);
    }

    @Test
    public void theyCombine() {
        // The reason there are three booleans rather than one setting: a binding can want two at once.
        Modifiers modifiers = Modifiers
            .of(keybind().set(ActionTypes.PARAM_SHIFT, "true").set(ActionTypes.PARAM_CTRL, "true"));
        assertTrue(modifiers.shift);
        assertTrue(modifiers.ctrl);
        assertFalse(modifiers.alt);
        assertEquals("shift+ctrl", modifiers.toString());
    }

    @Test
    public void aMissingSpecIsNoModifiersRatherThanACrash() {
        assertFalse(Modifiers.of(null).any());
    }

    @Test
    public void aHandEditedValueThatIsNotTrueReadsAsFalse() {
        // The file format is all strings, and "yes" is not one of them. Reading it as false costs a modifier; reading
        // it as true would hold one down that nobody asked for.
        assertFalse(Modifiers.of(keybind().set(ActionTypes.PARAM_SHIFT, "yes")).shift);
    }
}
