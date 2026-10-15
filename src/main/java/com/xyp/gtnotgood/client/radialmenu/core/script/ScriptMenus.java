package com.xyp.gtnotgood.client.radialmenu.core.script;

import org.luaj.vm2.LuaValue;

import com.xyp.gtnotgood.client.radialmenu.core.Colors;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.AccentCoefficients;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuStyle;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;
import com.xyp.gtnotgood.client.radialmenu.core.model.WheelColors;

final class ScriptMenus {

    private ScriptMenus() {}

    static MenuNode build(LuaValue items, LuaValue opts, String token, ScriptContext context, ScriptLimits limits) {
        LuaValue options = opts == null || !opts.istable() ? LuaValue.tableOf() : opts;

        boolean keepOpen = options.get("keepOpen").toboolean();

        MenuNode menu = MenuNode.category(blankToNull(options.get("title")), null,
            layoutOf(options, countOf(items, limits)));
        menu.style = styleOf(options, context, optString(options, "icon"));
        fill(menu, items, keepOpen, token, context, limits);

        return menu;
    }

    private static void fill(MenuNode parent, LuaValue items, boolean keepOpen, String token, ScriptContext context,
        ScriptLimits limits) {
        int count = countOf(items, limits);
        for (int i = 1; i <= count; i++) {
            LuaValue item = items.get(i);
            String label = blankToNull(item.get("label"));
            LuaValue nested = item.get("items");

            if (nested.istable() && nested.length() > 0) {
                MenuNode submenu = MenuNode.category(label, iconOf(item), layoutOf(item, countOf(nested, limits)));

                submenu.style = styleOf(item, context, null);
                fill(submenu, nested, keepOpen, token, context, limits);
                parent.children.add(submenu);
                continue;
            }

            MenuNode leaf = MenuNode.leaf(label, iconOf(item), resumeAction(token, item.get("n").optint(i)));
            leaf.keepOpen = keepOpen;
            parent.children.add(leaf);
        }
    }

    private static int countOf(LuaValue items, ScriptLimits limits) {
        return Math.min(items.length(), limits.maxMenuItems);
    }

    private static ActionSpec resumeAction(String token, int choice) {
        return new ActionSpec(ActionTypes.SCRIPT_RESUME).set(ActionTypes.PARAM_TOKEN, token)
            .set(ActionTypes.PARAM_CHOICE, Integer.toString(choice));
    }

    private static SlotLayout layoutOf(LuaValue options, int count) {
        int slots = options.get("slots").optint(0);
        SlotLayout layout = slots <= 0 ? SlotLayout.dynamic()
            : SlotLayout.fixed(SlotLayout.clampSlots(Math.max(slots, count)));
        layout.opening = openingOf(options);
        return layout;
    }

    private static SlotLayout.Opening openingOf(LuaValue options) {
        return "inline".equalsIgnoreCase(optString(options, "opening")) ? SlotLayout.Opening.Inline
            : SlotLayout.Opening.Replace;
    }

    private static MenuStyle styleOf(LuaValue options, ScriptContext context, String iconColor) {
        String ring = optString(options, "ring");
        String highlight = optString(options, "highlight");
        String border = optString(options, "border");
        String highlightBorder = optString(options, "highlightBorder");
        String background = optString(options, "background");

        String accent = optString(options, "accent");
        AccentCoefficients coefficients = context == null ? null : context.accentCoefficients();
        if (accent != null && coefficients != null) {
            WheelColors derived = new WheelColors(0, 0, 0, 0, 0, 0)
                .fromAccent(Colors.parseArgb(accent, 0xFFFFFF) & 0x00FFFFFF, coefficients);
            ring = ring == null ? Colors.toHex8(derived.ring) : ring;
            highlight = highlight == null ? Colors.toHex8(derived.highlight) : highlight;
            border = border == null ? Colors.toHex8(derived.border) : border;
            highlightBorder = highlightBorder == null ? Colors.toHex8(derived.highlightBorder) : highlightBorder;
            background = background == null ? Colors.toHex8(derived.background) : background;
        }

        return MenuStyle.of(ring, highlight, iconColor, border, highlightBorder, background);
    }

    private static IconSpec iconOf(LuaValue item) {
        LuaValue value = item.get("icon");
        String color = optString(item, "color");

        IconSpec icon = null;
        if (value.isstring()) {
            icon = IconSpec.parse(value.tojstring());
        } else if (value.istable()) {
            icon = fromTable(value);
        }

        if (
            icon != null && icon.color == null && (icon.kind == IconSpec.Kind.Sprite || icon.kind == IconSpec.Kind.File)
        ) {
            icon.color = color;
        }
        return icon;
    }

    private static IconSpec fromTable(LuaValue value) {
        String id = optString(value, "id");
        if (id == null) {
            return null;
        }
        String kind = optString(value, "kind");
        String color = optString(value, "color");
        int meta = value.get("meta").optint(0);

        if ("sprite".equalsIgnoreCase(kind)) {
            return IconSpec.sprite(id, color);
        }
        if ("file".equalsIgnoreCase(kind)) {
            IconSpec icon = IconSpec.file(id);
            icon.color = color;
            return icon;
        }
        if ("effect".equalsIgnoreCase(kind)) {
            return IconSpec.effect(id);
        }
        if ("player".equalsIgnoreCase(kind)) {
            return IconSpec.player(id);
        }

        return IconSpec.item(id, meta);
    }

    private static String optString(LuaValue table, String key) {
        return blankToNull(table.get(key));
    }

    private static String blankToNull(LuaValue value) {
        if (value == null || !value.isstring()) {
            return null;
        }
        String text = value.tojstring().trim();
        return text.isEmpty() ? null : text;
    }
}
