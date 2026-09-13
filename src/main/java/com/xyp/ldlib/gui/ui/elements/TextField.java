package com.xyp.ldlib.gui.ui.elements;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.render.ScissorScope;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/**
 * LDLib2 text-field contract adapted onto 1.7.10's native editing engine for selection,
 * clipboard shortcuts, cursor navigation and horizontal scrolling. Single-line client state only.
 * setText is silent; accepted user edits notify onChange once.
 * Right-click clears all text as a built-in editing action, including numeric fields.
 * Empty text is a permitted intermediate state for this action, independent of final-value validation.
 */
public final class TextField extends UIElement {

    private GuiTextField editor;
    private String value = "";
    private int maxLength = 256;
    private boolean focused;
    private IGuiTexture normalTexture, focusedTexture, disabledTexture;

    /** Replaces vanilla decoration while retaining its editing and clipboard engine. */
    public TextField setTextures(IGuiTexture normal, IGuiTexture focused, IGuiTexture disabled) {
        normalTexture = Objects.requireNonNull(normal);
        focusedTexture = Objects.requireNonNull(focused);
        disabledTexture = Objects.requireNonNull(disabled);
        return this;
    }

    private Predicate<String> validator = text -> true;
    private Consumer<String> onChange = text -> {};
    private Consumer<String> onSubmit = text -> {};

    public TextField(int x, int y, int width, int height) {
        super(x, y, width, height);
        setFocusable(true);
        addEventListener(UIEvents.FOCUS, e -> {
            focused = true;
            if (editor != null) editor.setFocused(true);
        });
        addEventListener(UIEvents.BLUR, e -> {
            focused = false;
            if (editor != null) editor.setFocused(false);
        });
        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 1) {
                if (!value.isEmpty()) {
                    value = "";
                    if (editor != null) editor.setText("");
                    onChange.accept(value);
                }
                e.preventDefault();
                e.stopPropagation();
                return;
            }
            if (e.button != 0) return;
            prepare();
            editor.mouseClicked(e.x, e.y, e.button);
            e.preventDefault();
            e.stopPropagation();
        });
        addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == Keyboard.KEY_ESCAPE) return;
            if (e.keyCode == Keyboard.KEY_RETURN || e.keyCode == Keyboard.KEY_NUMPADENTER) {
                onSubmit.accept(value);
                e.preventDefault();
                return;
            }
            prepare();
            int cursor = editor.getCursorPosition(), selection = editor.getSelectionEnd();
            if (editor.textboxKeyTyped(e.codePoint, e.keyCode)) {
                String candidate = editor.getText();
                if (!validator.test(candidate)) {
                    editor.setText(value);
                    editor.setCursorPosition(cursor);
                    editor.setSelectionPos(selection);
                } else if (!candidate.equals(value)) {
                    value = candidate;
                    onChange.accept(value);
                }
            }
            e.preventDefault();
            e.stopPropagation();
        });
    }

    private void prepare() {
        if (editor == null) {
            editor = new GuiTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, width, height);
            editor.setMaxStringLength(maxLength);
            editor.setText(value);
            editor.setCanLoseFocus(false);
        }
        boolean themed = normalTexture != null;
        editor.setEnableBackgroundDrawing(!themed);
        editor.xPosition = getScreenX() + (themed ? 3 : 0);
        editor.yPosition = getScreenY() + (themed ? (height - 8) / 2 : 0);
        editor.width = Math.max(1, width - (themed ? 6 : 0));
        editor.height = themed ? 8 : height;
        editor.setFocused(focused);
        editor.setEnabled(isInteractive());
    }

    public String getText() {
        return value;
    }

    public TextField setText(String text) {
        Objects.requireNonNull(text);
        if (text.length() > maxLength || !validator.test(text))
            throw new IllegalArgumentException("Invalid text value");
        value = text;
        if (editor != null) editor.setText(text);
        return this;
    }

    public TextField setMaxLength(int limit) {
        if (limit < 0 || value.length() > limit) throw new IllegalArgumentException("Invalid text limit");
        maxLength = limit;
        if (editor != null) editor.setMaxStringLength(limit);
        return this;
    }

    /** Validators should permit intermediate edits such as an empty numeric field. */
    public TextField setValidator(Predicate<String> validator) {
        Objects.requireNonNull(validator);
        if (!validator.test(value)) throw new IllegalArgumentException("Current value fails validator");
        this.validator = validator;
        return this;
    }

    public TextField setOnChange(Consumer<String> callback) {
        onChange = Objects.requireNonNull(callback);
        return this;
    }

    public TextField setOnSubmit(Consumer<String> callback) {
        onSubmit = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (editor != null) editor.updateCursorCounter();
    }

    @Override
    protected void drawBackground(int mx, int my, int px, int py) {
        if (normalTexture == null) super.drawBackground(mx, my, px, py);
        else(!isInteractive() ? disabledTexture : focused ? focusedTexture : normalTexture)
            .draw(mx, my, px + x, py + y, width, height);
    }

    @Override
    protected void drawForeground(int mouseX, int mouseY, int parentX, int parentY) {
        if (width < 8 || height < 12) return;
        prepare();
        try (ScissorScope ignored = new ScissorScope(parentX + x, parentY + y, width, height)) {
            editor.drawTextBox();
        }
    }
}
