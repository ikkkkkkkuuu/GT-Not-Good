package com.xyp.gtnotgood.client.radialmenu.core.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Reordering a chain. The order of the steps is the whole point of a chain, so it is worth testing on its own. */
public class ActionStepsTest {

    private static List<ActionSpec> chain(String... types) {
        List<ActionSpec> steps = new ArrayList<>();
        for (String type : types) {
            steps.add(new ActionSpec(type));
        }
        return steps;
    }

    private static String order(List<ActionSpec> steps) {
        StringBuilder text = new StringBuilder();
        for (ActionSpec step : steps) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(step.type);
        }
        return text.toString();
    }

    @Test
    public void movesAStepEarlier() {
        List<ActionSpec> steps = chain("a", "b", "c");
        assertTrue(ActionSteps.move(steps, 2, -1));
        assertEquals("a,c,b", order(steps));
    }

    @Test
    public void movesAStepLater() {
        List<ActionSpec> steps = chain("a", "b", "c");
        assertTrue(ActionSteps.move(steps, 0, 1));
        assertEquals("b,a,c", order(steps));
    }

    @Test
    public void refusesToMovePastTheEnds() {
        List<ActionSpec> steps = chain("a", "b");
        assertFalse(ActionSteps.move(steps, 0, -1));
        assertFalse(ActionSteps.move(steps, 1, 1));
        assertEquals("a,b", order(steps));
    }

    @Test
    public void refusesAnIndexThatIsNotThere() {
        List<ActionSpec> steps = chain("a");
        assertFalse(ActionSteps.move(steps, 5, -1));
        assertFalse(ActionSteps.move(steps, -1, 1));
        assertFalse(ActionSteps.move(null, 0, 1));
        assertFalse(ActionSteps.move(steps, 0, 0));
        assertEquals("a", order(steps));
    }

    @Test
    public void movingBackAndForthRestoresTheOrder() {
        List<ActionSpec> steps = chain("a", "b", "c", "d");
        ActionSteps.move(steps, 1, 1);
        ActionSteps.move(steps, 2, -1);
        assertEquals("a,b,c,d", order(steps));
    }
}
