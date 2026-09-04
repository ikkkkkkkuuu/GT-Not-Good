package com.xyp.gtnotgood.common.mebridge;

import java.util.function.Supplier;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;

final class MEBridgeGuiTheme {

    static final int TEXT = 0xFFE7E9F2;
    static final int MUTED = 0xFFAEB3C4;
    static final int ACCENT = 0xFF74C7FF;
    static final int SUCCESS = 0xFF6CFFA0;
    static final int WARNING = 0xFFFFC857;
    static final int DANGER = 0xFFFF7188;
    static final int PANEL = 0xFF252936;
    static final int PANEL_DARK = 0xFF171A22;
    static final int PANEL_ROW = 0xFF303646;

    private MEBridgeGuiTheme() {}

    static ParentWidget<?> section(int x, int y, int width, int height) {
        ParentWidget<?> section = new ParentWidget<>().pos(x, y)
            .size(width, height)
            .background(GTNGGuiTextures.MODERN_PANEL_BORDER);
        section.child(
            new ParentWidget<>().pos(2, 2)
                .size(width - 4, height - 4)
                .background(new Rectangle().color(PANEL)));
        return section;
    }

    static ParentWidget<?> inset(int x, int y, int width, int height) {
        ParentWidget<?> inset = new ParentWidget<>().pos(x, y)
            .size(width, height)
            .background(new Rectangle().color(0xFF9099B0));
        inset.child(
            new ParentWidget<>().pos(1, 1)
                .size(width - 2, height - 2)
                .background(new Rectangle().color(PANEL_DARK)));
        return inset;
    }

    static IWidget text(IKey key, int x, int y, int width, int color, Alignment alignment) {
        return key.asWidget()
            .pos(x, y)
            .size(width, 12)
            .color(color)
            .textAlign(alignment);
    }

    static IWidget dynamic(Supplier<String> value, int x, int y, int width, int color, Alignment alignment) {
        return text(IKey.dynamic(value), x, y, width, color, alignment);
    }

    static String dimensionLabel(int dimension) {
        if (dimension == 0) {
            // #tr gui.mebridge.dimension.overworld
            // # Overworld
            // # zh_CN 主世界
            return net.minecraft.util.StatCollector.translateToLocal("gui.mebridge.dimension.overworld");
        }
        if (dimension == -1) {
            // #tr gui.mebridge.dimension.nether
            // # Nether
            // # zh_CN 下界
            return net.minecraft.util.StatCollector.translateToLocal("gui.mebridge.dimension.nether");
        }
        if (dimension == 1) {
            // #tr gui.mebridge.dimension.end
            // # The End
            // # zh_CN 末地
            return net.minecraft.util.StatCollector.translateToLocal("gui.mebridge.dimension.end");
        }
        // #tr gui.mebridge.dimension.other
        // # Dimension %s
        // # zh_CN 维度 %s
        return net.minecraft.util.StatCollector.translateToLocalFormatted("gui.mebridge.dimension.other", dimension);
    }
}
