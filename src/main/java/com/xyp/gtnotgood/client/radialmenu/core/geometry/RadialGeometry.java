package com.xyp.gtnotgood.client.radialmenu.core.geometry;

public final class RadialGeometry {

    public static final int NO_SLOT = -1;

    private RadialGeometry() {}

    public static double normalizeDegrees(double degrees) {
        double d = degrees % 360.0;
        return d < 0.0 ? d + 360.0 : d;
    }

    public static double angleTo(double centerX, double centerY, double x, double y) {
        return normalizeDegrees(Math.toDegrees(Math.atan2(x - centerX, -(y - centerY))));
    }

    public static int slotAtAngle(double angleDegrees, int slotCount, double rotationDegrees) {
        if (slotCount <= 0) {
            return NO_SLOT;
        }
        double step = 360.0 / slotCount;
        double shifted = normalizeDegrees(angleDegrees - rotationDegrees + step / 2.0);
        return (int) (shifted / step) % slotCount;
    }

    public static int slotAtPoint(double centerX, double centerY, double x, double y, int slotCount,
        double rotationDegrees, double deadZoneRadius) {
        double dx = x - centerX;
        double dy = y - centerY;
        if (dx * dx + dy * dy < deadZoneRadius * deadZoneRadius) {
            return NO_SLOT;
        }
        return slotAtAngle(angleTo(centerX, centerY, x, y), slotCount, rotationDegrees);
    }

    public static double slotCenterAngle(int slotIndex, int slotCount, double rotationDegrees) {
        if (slotCount <= 0) {
            return 0.0;
        }
        return normalizeDegrees(rotationDegrees + slotIndex * (360.0 / slotCount));
    }

    public static double sectorSpan(int slotCount) {
        return slotCount <= 0 ? 0.0 : 360.0 / slotCount;
    }

    public static double arcSlotSpan(double arcSpan, int slotCount) {
        return slotCount <= 0 ? 0.0 : arcSpan / slotCount;
    }

    public static double arcSlotStart(int slot, int slotCount, double startAngle, double arcSpan) {
        return startAngle + slot * arcSlotSpan(arcSpan, slotCount);
    }

    public static double arcSlotCenter(int slot, int slotCount, double startAngle, double arcSpan) {
        return startAngle + (slot + 0.5) * arcSlotSpan(arcSpan, slotCount);
    }

    public static int slotInArc(double angleDegrees, double startAngle, double arcSpan, int slotCount) {
        if (slotCount <= 0 || arcSpan <= 0.0) {
            return NO_SLOT;
        }
        double fromStart = normalizeDegrees(angleDegrees - startAngle);
        if (arcSpan < 360.0 && fromStart > arcSpan) {
            return NO_SLOT;
        }
        int slot = (int) (fromStart / arcSlotSpan(arcSpan, slotCount));

        return Math.min(slot, slotCount - 1);
    }

    public static double offsetX(double angleDegrees, double radius) {
        return Math.sin(Math.toRadians(angleDegrees)) * radius;
    }

    public static double offsetY(double angleDegrees, double radius) {
        return -Math.cos(Math.toRadians(angleDegrees)) * radius;
    }

    public static double gapInsetDegrees(double radius, double gapPixels) {
        if (gapPixels <= 0.0 || radius <= 0.0) {
            return 0.0;
        }
        double half = gapPixels / 2.0;
        if (half >= radius) {
            return 90.0;
        }
        return Math.toDegrees(Math.asin(half / radius));
    }

    public static int arcSegments(double spanDegrees) {
        return Math.max(8, (int) Math.ceil(spanDegrees / 4.0));
    }
}
