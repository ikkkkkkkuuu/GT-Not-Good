package com.xyp.gtnotgood.client.radialmenu.client.action;

import java.util.ArrayList;
import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;

public class SequenceExecutor implements IActionExecutor {

    private static final int MAX_DEPTH = 8;

    private static int depth;

    @Override
    public String typeId() {
        return ActionTypes.SEQUENCE;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        if (depth >= MAX_DEPTH) {
            return false;
        }

        List<ActionSpec> steps = runnableSteps(spec);
        if (steps.isEmpty()) {
            return false;
        }

        int delay = Math.max(0, spec.getInt(ActionTypes.PARAM_DELAY_TICKS, 0));

        DelayedActions.cancel(spec);

        depth++;
        try {
            for (int i = 0; i < steps.size(); i++) {
                if (i == 0 || delay == 0) {
                    ActionExecutors.runNow(steps.get(i));
                } else {
                    DelayedActions.scheduleAction(spec, steps.get(i), delay * i);
                }
            }
        } finally {
            depth--;
        }
        return true;
    }

    private static List<ActionSpec> runnableSteps(ActionSpec spec) {
        List<ActionSpec> runnable = new ArrayList<>();
        if (spec.steps == null) {
            return runnable;
        }
        for (ActionSpec step : spec.steps) {
            if (step != null && step.type != null && ActionTypes.isComplete(step)) {
                runnable.add(step);
            }
        }
        return runnable;
    }
}
