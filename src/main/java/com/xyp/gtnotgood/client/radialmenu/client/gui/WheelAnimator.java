package com.xyp.gtnotgood.client.radialmenu.client.gui;

import com.xyp.gtnotgood.client.radialmenu.config.AnimationConfig;
import com.xyp.gtnotgood.client.radialmenu.core.animation.RevealTiming;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;

public class WheelAnimator {

    private static final int MAX_SLOTS = SlotLayout.MAX_SLOTS + 1;

    private final float[] push = new float[MAX_SLOTS];

    private long lastNano;

    private double revealElapsed;

    public void reset() {
        lastNano = 0L;
        revealElapsed = 0.0;
        for (int i = 0; i < push.length; i++) {
            push[i] = 0f;
        }
    }

    public void advance(int slotCount, int hoveredSlot) {
        long now = System.nanoTime();
        if (lastNano == 0L) {
            lastNano = now;
        }

        double delta = Math.min((now - lastNano) / 1.0e9, 0.1);
        lastNano = now;
        revealElapsed += delta;

        double duration = AnimationConfig.hoverDurationMs / 1000.0;
        double step = duration <= 0.0 ? 1.0 : delta / duration;
        for (int slot = 0; slot < push.length; slot++) {
            float target = slot == hoveredSlot && slot < slotCount ? 1f : 0f;
            push[slot] = (float) RevealTiming.approach(push[slot], target, step);
        }
    }

    public float reveal(int slot, int slotCount) {
        return (float) RevealTiming.easeOutQuint(RevealTiming.slotProgress(AnimationConfig.revealAnimation, slot,
            slotCount, revealElapsed, AnimationConfig.revealDurationMs / 1000.0));
    }

    public float push(int slot) {
        if (slot < 0 || slot >= push.length) {
            return 0f;
        }
        return push[slot] * AnimationConfig.hoverPush;
    }
}
