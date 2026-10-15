package com.xyp.gtnotgood.client.radialmenu.core.script;

import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;

public final class ScriptLimits {

    public final int instructionsPerResume;

    public final long nanosPerResume;

    public final int maxMenuItems;

    public static final ScriptLimits DEFAULT = new ScriptLimits(200_000, 20_000_000L, SlotLayout.MAX_SLOTS);

    public ScriptLimits(int instructionsPerResume, long nanosPerResume, int maxMenuItems) {
        this.instructionsPerResume = instructionsPerResume;
        this.nanosPerResume = nanosPerResume;
        this.maxMenuItems = maxMenuItems;
    }
}
