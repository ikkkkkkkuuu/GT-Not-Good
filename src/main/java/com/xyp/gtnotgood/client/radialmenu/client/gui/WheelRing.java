package com.xyp.gtnotgood.client.radialmenu.client.gui;

import com.xyp.gtnotgood.client.radialmenu.config.WheelConfig;
import com.xyp.gtnotgood.client.radialmenu.core.geometry.RadialGeometry;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;

public final class WheelRing {

    public final MenuNode menu;

    public final int level;

    public final double startAngle;

    public final double span;

    public final double inner;

    public final double outer;

    public final int slotCount;

    public final int parent;

    public final int parentSlot;

    public WheelRing(MenuNode menu, int level, double startAngle, double span, int slotCount, int parent,
        int parentSlot) {
        this.menu = menu;
        this.level = level;
        this.startAngle = startAngle;
        this.span = span;
        this.slotCount = slotCount;
        this.parent = parent;
        this.parentSlot = parentSlot;
        this.inner = innerRadius(level);
        this.outer = innerRadius(level) + thickness();
    }

    public static WheelRing root(MenuNode menu, int slotCount) {
        return new WheelRing(menu, 0, -RadialGeometry.sectorSpan(slotCount) / 2.0, 360.0, slotCount, -1, -1);
    }

    public static double thickness() {
        return WheelConfig.outerRadius - WheelConfig.effectiveInnerRadius();
    }

    public static double innerRadius(int level) {
        return WheelConfig.effectiveInnerRadius() + level * (thickness() + WheelConfig.inlineRingSpacing);
    }

    public static double outerRadius(int level) {
        return innerRadius(level) + thickness();
    }

    public double midRadius() {
        return (inner + outer) / 2.0;
    }

    public double slotSpan() {
        return RadialGeometry.arcSlotSpan(span, slotCount);
    }

    public double slotStart(int slot) {
        return RadialGeometry.arcSlotStart(slot, slotCount, startAngle, span);
    }

    public double slotCenter(int slot) {
        return RadialGeometry.arcSlotCenter(slot, slotCount, startAngle, span);
    }

    public int slotAt(double angle) {
        return RadialGeometry.slotInArc(angle, startAngle, span, slotCount);
    }

    public MenuNode childAt(int slot) {
        return menu.childAt(slot);
    }
}
