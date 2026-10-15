package com.xyp.gtnotgood.client.radialmenu.core.action;

public final class Modifiers {

    public final boolean shift;
    public final boolean ctrl;
    public final boolean alt;

    public static final Modifiers NONE = new Modifiers(false, false, false);

    private Modifiers(boolean shift, boolean ctrl, boolean alt) {
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
    }

    public static Modifiers of(ActionSpec spec) {
        if (spec == null) {
            return NONE;
        }
        return new Modifiers(spec.getBoolean(ActionTypes.PARAM_SHIFT, false),
            spec.getBoolean(ActionTypes.PARAM_CTRL, false), spec.getBoolean(ActionTypes.PARAM_ALT, false));
    }

    public boolean any() {
        return shift || ctrl || alt;
    }

    @Override
    public String toString() {
        if (!any()) {
            return "none";
        }
        StringBuilder text = new StringBuilder();
        if (shift) {
            text.append("shift");
        }
        if (ctrl) {
            text.append(text.length() > 0 ? "+ctrl" : "ctrl");
        }
        if (alt) {
            text.append(text.length() > 0 ? "+alt" : "alt");
        }
        return text.toString();
    }
}
