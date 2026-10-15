package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;

import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionField;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuStyle;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;

final class SubmenuFields {

    private SubmenuFields() {}

    static ActionSpec toSpec(MenuNode node) {
        SlotLayout layout = node.layoutOrDefault();
        MenuStyle style = node.style;

        ActionSpec spec = new ActionSpec(ActionTypes.SUBMENU);
        SlotLayout.Opening opening = layout.opening == null ? SlotLayout.Opening.Replace : layout.opening;
        spec.set(ActionTypes.PARAM_OPENING, opening.name().toLowerCase());
        spec.set(ActionTypes.PARAM_SLOT_MODE, ToolBeltMenu.isMenu(node) ? "fixed" : layout.mode.name().toLowerCase());
        spec.set(ActionTypes.PARAM_SLOT_COUNT, Integer.toString(layout.slots));
        spec.set(ActionTypes.PARAM_RING_COLOR, orBlank(style == null ? null : style.ringColor));
        spec.set(ActionTypes.PARAM_HIGHLIGHT_COLOR, orBlank(style == null ? null : style.highlightColor));
        spec.set(ActionTypes.PARAM_BORDER_COLOR, orBlank(style == null ? null : style.borderColor));
        spec.set(ActionTypes.PARAM_HIGHLIGHT_BORDER_COLOR, orBlank(style == null ? null : style.highlightBorderColor));
        spec.set(ActionTypes.PARAM_BACKGROUND_COLOR, orBlank(style == null ? null : style.backgroundColor));
        return spec;
    }

    static List<ActionField> fields(MenuNode node) {
        List<ActionField> fields = ActionTypes.get(ActionTypes.SUBMENU).fields;
        if (!ToolBeltMenu.isMenu(node)) {
            return fields;
        }
        List<ActionField> editable = new ArrayList<>(fields);
        editable.removeIf(field -> ActionTypes.PARAM_SLOT_MODE.equals(field.key));
        return editable;
    }

    private static String orBlank(String value) {
        return value == null ? "" : value;
    }

    /** Writes the edited spec back onto the node, turning it into a category if it was not one already. */
    static void applyToNode(MenuNode node) {
        ActionSpec spec = node.action;

        SlotLayout layout = new SlotLayout();
        layout.opening = spec.getEnum(ActionTypes.PARAM_OPENING, SlotLayout.Opening.class, SlotLayout.Opening.Replace);
        layout.mode = spec.getEnum(ActionTypes.PARAM_SLOT_MODE, SlotLayout.Mode.class, SlotLayout.Mode.Fixed);
        layout.slots = SlotLayout.clampSlots(spec.getInt(ActionTypes.PARAM_SLOT_COUNT, SlotLayout.DEFAULT_SLOTS));

        node.layout = layout;
        // A submenu has no icon tint of its own - that belongs to the entry, not to the wheel it opens.
        node.style = MenuStyle.of(spec.getString(ActionTypes.PARAM_RING_COLOR, ""),
            spec.getString(ActionTypes.PARAM_HIGHLIGHT_COLOR, ""), null,
            spec.getString(ActionTypes.PARAM_BORDER_COLOR, ""),
            spec.getString(ActionTypes.PARAM_HIGHLIGHT_BORDER_COLOR, ""),
            spec.getString(ActionTypes.PARAM_BACKGROUND_COLOR, ""));

        if (node.children == null) {
            node.children = new ArrayList<>();
        }
        // The spec was only ever a view onto the node; a category carries no action.
        node.action = null;
        if (!ToolBeltMenu.isMenu(node)) {
            node.ensureSlotCapacity();
        }
    }
}
