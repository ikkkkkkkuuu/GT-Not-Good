package com.xyp.gtnotgood.client.radialmenu.core.animation;

public final class RevealTiming {

    private static final double SPREAD = 0.5;

    private RevealTiming() {}

    public static double slotProgress(RevealAnimation animation, int slot, int slotCount, double elapsedSeconds,
        double durationSeconds) {
        if (animation == null || animation == RevealAnimation.NONE || durationSeconds <= 0.0 || slotCount <= 0) {
            return 1.0;
        }
        if (elapsedSeconds >= durationSeconds) {
            return 1.0;
        }

        double delay = delayFraction(animation, slot, slotCount) * durationSeconds * SPREAD;
        double span = durationSeconds - delay;
        if (span <= 0.0) {
            return elapsedSeconds >= delay ? 1.0 : 0.0;
        }
        return clamp((elapsedSeconds - delay) / span);
    }

    private static double delayFraction(RevealAnimation animation, int slot, int slotCount) {
        if (slotCount <= 1) {
            return 0.0;
        }
        switch (animation) {
            case STAGGER_CW:
                return (double) slot / (slotCount - 1);
            case STAGGER_CCW:
                return (double) ((slotCount - slot) % slotCount) / (slotCount - 1);
            case STAGGER_BOTH:

                int distance = Math.min(slot, slotCount - slot);
                return (double) distance / (slotCount / 2);
            default:
                return 0.0;
        }
    }

    public static double easeOutQuint(double progress) {
        double t = 1.0 - clamp(progress);
        return 1.0 - t * t * t * t * t;
    }

    public static double approach(double current, double target, double step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
    }
}
