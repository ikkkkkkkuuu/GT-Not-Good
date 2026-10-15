package com.xyp.gtnotgood.client.radialmenu.core.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Which actions have enough to run.
 *
 * <p>
 * A chain passes over the steps that do not, so what counts as "enough" decides what silently does nothing - worth
 * pinning down rather than leaving to each executor.
 */
public class ActionCompletenessTest {

    @Before
    public void registerTypes() {
        ActionTypes.clear();
        ActionTypes.registerDefaults();
    }

    @Test
    public void aKeybindActionNeedsAKeybinding() {
        ActionSpec spec = ActionTypes.get(ActionTypes.KEYBIND).newSpec();
        // Exactly what a step added to a chain and left alone looks like.
        assertFalse(ActionTypes.isComplete(spec));

        spec.set(ActionTypes.PARAM_BINDING, "key.inventory");
        assertTrue(ActionTypes.isComplete(spec));
    }

    @Test
    public void blankCountsAsMissing() {
        ActionSpec spec = new ActionSpec(ActionTypes.KEYBIND).set(ActionTypes.PARAM_BINDING, "   ");
        assertFalse(ActionTypes.isComplete(spec));
    }

    @Test
    public void aCommandActionNeedsText() {
        ActionSpec spec = ActionTypes.get(ActionTypes.COMMAND).newSpec();
        assertFalse(ActionTypes.isComplete(spec));

        spec.set(ActionTypes.PARAM_COMMAND, "/home");
        assertTrue(ActionTypes.isComplete(spec));
    }

    @Test
    public void anOptionalBlankIsStillComplete() {
        // A profile switch with no profile named cycles to the next one, which is a choice and not an omission.
        ActionSpec spec = ActionTypes.get(ActionTypes.PROFILE_SWITCH).newSpec();
        assertTrue(ActionTypes.isComplete(spec));
    }

    @Test
    public void aChainWithNoDelayIsComplete() {
        // A chain's steps are not parameters; an empty one is caught by the executor, not by this.
        assertTrue(ActionTypes.isComplete(ActionTypes.get(ActionTypes.SEQUENCE).newSpec()));
    }

    @Test
    public void anUnknownTypeIsLeftToTheExecutorLookup() {
        assertTrue(ActionTypes.isComplete(new ActionSpec("noSuchType")));
    }

    @Test
    public void nothingAtAllIsNotComplete() {
        assertFalse(ActionTypes.isComplete(null));
        assertFalse(ActionTypes.isComplete(new ActionSpec(null)));
    }
}
