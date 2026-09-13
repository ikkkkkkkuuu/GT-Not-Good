package com.xyp.ldlib.integration.modularui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.AbstractMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.xyp.ldlib.gui.texture.ColorBorderTexture;

/**
 * Wildcard Pattern's LDLib selector interaction adapted to MUI2 floating menus.
 * The button has dark text, while the translucent popup has white 15-pixel rows, at most five visible.
 * Programmatic updates are silent; a user selection invokes the callback once and closes the menu.
 * MUI2 owns the popup layer, outside-click dismissal and scroll clipping.
 */
public final class SelectorWidget extends AbstractMenuButton<SelectorWidget> {

    /** MUI caches panel handlers by name for the screen lifetime, including removed filter rows. */
    private static final AtomicLong NEXT_MENU_ID = new AtomicLong();
    private final List<String> candidates = new ArrayList<>();
    private String value = "";
    private Consumer<String> onChanged = ignored -> {};
    private int maxCount = 5;
    private static final IDrawable SELECTED = ModernThemeAdapter.drawable(new ColorBorderTexture(1, 0xFFFFFFFF));

    public SelectorWidget(String name, IDrawable button, List<String> candidates) {
        super(name + "_menu_" + NEXT_MENU_ID.getAndIncrement());
        name(name);
        openOnHover = false;
        size(80, 15);
        padding(0);
        background(button);
        disableHoverBackground();
        overlay(new ScrollingTextDrawable(() -> value, 0xFF444444, false));
        tooltip().setAutoUpdate(true)
            .tooltipBuilder(tooltip -> { if (!value.isEmpty()) tooltip.addLine(IKey.str(value)); });
        setCandidates(candidates);
    }

    /** Replaces options and invalidates the popup; never selects or writes a model value implicitly. */
    public SelectorWidget setCandidates(List<String> values) {
        List<String> copy = new ArrayList<>(Objects.requireNonNull(values));
        copy.forEach(Objects::requireNonNull);
        if (isOpen()) closeMenu(false);
        candidates.clear();
        candidates.addAll(copy);
        setMenu(null);
        return this;
    }

    public List<String> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    public SelectorWidget setValue(String value) {
        this.value = Objects.requireNonNull(value);
        return this;
    }

    public String getSelectedValue() {
        return value;
    }

    public SelectorWidget setOnChanged(Consumer<String> callback) {
        onChanged = Objects.requireNonNull(callback);
        return this;
    }

    public SelectorWidget setMaxCount(int count) {
        if (count < 1) throw new IllegalArgumentException("Positive row count required");
        maxCount = count;
        setCandidates(new ArrayList<>(candidates));
        return this;
    }

    @Override
    public void openMenu(boolean soft) {
        if (isValid() && areAncestorsEnabled() && !candidates.isEmpty()) super.openMenu(soft);
    }

    /**
     * A menu may still receive close events while its panel is finishing its closing animation.
     * Never walk a removed source's parent chain; also retire menus whose source page is hidden.
     */
    @Override
    protected void checkClose(boolean soft, boolean requireNoHover) {
        if (!isValid() || !areAncestorsEnabled()) {
            closeMenu(false);
            return;
        }
        super.checkClose(soft, requireNoHover);
    }

    /** Closes and drops the floating menu before MUI invalidates this row's parent and context. */
    @Override
    public void dispose() {
        try {
            closeMenu(false);
            setMenu(null);
        } finally {
            super.dispose();
        }
    }

    @Override
    protected Menu<?> createMenu() {
        ListWidget<com.cleanroommc.modularui.api.widget.IWidget, ?> list = new ListWidget<>().widthRel(1f)
            .maxSize(maxCount * 15)
            .scrollDirection(
                new com.cleanroommc.modularui.widget.scroll.VerticalScrollData(false, 4)
                    .texture(new Rectangle().color(0xFFFFFFFF)));
        for (String candidate : candidates) {
            list.child(
                new ButtonWidget<>().widthRel(1f)
                    .height(15)
                    .padding(0)
                    .background(new DynamicDrawable(() -> candidate.equals(value) ? SELECTED : null))
                    .disableHoverBackground()
                    .overlay(new ScrollingTextDrawable(() -> candidate, 0xFFFFFFFF, true))
                    .tooltipBuilder(tooltip -> tooltip.addLine(IKey.str(candidate)))
                    .onMousePressed(mouse -> {
                        if (mouse != 0) return false;
                        if (!isValid() || !areAncestorsEnabled()) {
                            closeMenu(false);
                            return true;
                        }
                        if (candidates.contains(candidate)) {
                            setValue(candidate);
                            closeMenu(false);
                            onChanged.accept(candidate);
                        }
                        return true;
                    }));
        }
        return new Menu<>().widthRel(1f)
            .coverChildrenHeight()
            .padding(0)
            .background(new Rectangle().color(0xAA000000))
            .child(list);
    }
}
