package com.xyp.ldlib.integration.modularui;

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;

/**
 * Single-line selector caption: short text is centered, long text scrolls between its ends without wrapping.
 * Uses MUI's clipping implementation so floating menus and scroll-list transforms remain aligned.
 * Each caption owns its renderer and timing; changing its value resets the initial reading pause.
 */
public final class ScrollingTextDrawable implements IDrawable {

    private final Supplier<String> text;
    private final TextRenderer renderer = new TextRenderer();
    private String previous;
    private long started;

    public ScrollingTextDrawable(Supplier<String> text, int color, boolean shadow) {
        this.text = Objects.requireNonNull(text);
        renderer.setColor(color);
        renderer.setShadow(shadow);
        renderer.setHardWrapOnBorder(false);
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
        if (width <= 4 || height <= 0) return;
        String supplied = text.get();
        String value = supplied == null ? ""
            : supplied.replace('\n', ' ')
                .replace('\r', ' ');
        long now = Minecraft.getSystemTime();
        if (!value.equals(previous)) {
            previous = value;
            started = now;
        }
        int available = width - 4;
        TextRenderer.Line line = renderer.line(value);
        renderer.setPos(x + 2, y);
        boolean overflow = line.getWidth() > available;
        renderer.setAlignment(overflow ? Alignment.CenterLeft : Alignment.Center, available, height);
        if (overflow) {
            long travel = Math.max(1000, (long) ((line.getWidth() - available) * 50));
            long phase = Math.max(0, now - started) % (travel * 2 + 1600);
            float progress = phase < 800 ? 0
                : phase < 800 + travel ? (phase - 800) / (float) travel
                    : phase < 1600 + travel ? 1 : 1 - (phase - 1600 - travel) / (float) travel;
            renderer.drawScrolling(line, progress, null, context);
        } else {
            renderer.drawSimple(value);
        }
    }
}
