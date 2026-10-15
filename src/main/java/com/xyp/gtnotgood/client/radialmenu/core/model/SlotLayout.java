package com.xyp.gtnotgood.client.radialmenu.core.model;

import com.google.gson.annotations.SerializedName;

public class SlotLayout {

    public enum Mode {
        @SerializedName("fixed")
        Fixed,

        @SerializedName("dynamic")
        Dynamic
    }

    public enum Opening {
        @SerializedName("replace")
        Replace,

        @SerializedName("inline")
        Inline
    }

    public static final int MIN_SLOTS = 2;
    public static final int MAX_SLOTS = 24;
    public static final int DEFAULT_SLOTS = 8;

    public Mode mode = Mode.Dynamic;

    public int slots = DEFAULT_SLOTS;

    public Opening opening = Opening.Replace;

    public static SlotLayout fixed(int slots) {
        SlotLayout layout = new SlotLayout();
        layout.mode = Mode.Fixed;
        layout.slots = slots;
        return layout;
    }

    public static SlotLayout dynamic() {
        return new SlotLayout();
    }

    public int slotCount(int childCount) {
        return mode == Mode.Fixed ? clampSlots(slots) : childCount;
    }

    public static int clampSlots(int value) {
        return Math.max(MIN_SLOTS, Math.min(MAX_SLOTS, value));
    }

    public boolean isInline() {
        return opening == Opening.Inline;
    }

    public void normalize() {
        if (mode == null) {
            mode = Mode.Dynamic;
        }
        if (opening == null) {
            opening = Opening.Replace;
        }
        slots = clampSlots(slots);
    }
}
