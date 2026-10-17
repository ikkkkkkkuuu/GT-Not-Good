package com.xyp.gtnotgood.client.webscreen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.common.blocks.webscreen.TileWebScreen;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenAddress;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuiWebScreen extends GuiScreen {

    private final TileWebScreen tile;
    private final Map<Integer, String> pressedKeys = new LinkedHashMap<>();
    private GuiTextField address;
    private GuiButton zoomOut;
    private GuiButton zoomReset;
    private GuiButton zoomIn;
    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewHeight;
    private int buttons;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private String error;

    public GuiWebScreen(TileWebScreen tile) {
        this.tile = tile;
    }

    @Override
    public void initGui() {
        releaseKeys();
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        boolean compact = width < 640;
        int controlsY = compact ? 32 : 8;
        int zoomX = compact ? 8 : width - 270;
        buttonList.add(new GuiButton(0, 8, 8, 24, 20, "<"));
        buttonList.add(new GuiButton(1, 34, 8, 28, 20, "R"));
        buttonList.add(new GuiButton(2, 64, 8, 40, 20, "Bili"));
        // #tr gui.web_screen.go
        // # Go
        // # zh_CN 前往
        buttonList.add(new GuiButton(3, width - (compact ? 50 : 316), 8, 42, 20, I18n.format("gui.web_screen.go")));
        // #tr gui.web_screen.stop
        // # Stop
        // # zh_CN 停止
        buttonList.add(new GuiButton(4, width - 104, controlsY, 48, 20, I18n.format("gui.web_screen.stop")));
        // #tr gui.web_screen.close
        // # Close
        // # zh_CN 关闭
        buttonList.add(new GuiButton(5, width - 52, controlsY, 44, 20, I18n.format("gui.web_screen.close")));
        zoomOut = new GuiButton(6, zoomX, controlsY, 24, 20, "-");
        zoomReset = new GuiButton(7, zoomX + 26, controlsY, 48, 20, "100%");
        zoomIn = new GuiButton(8, zoomX + 76, controlsY, 24, 20, "+");
        buttonList.add(zoomOut);
        buttonList.add(zoomReset);
        buttonList.add(zoomIn);
        // #tr gui.web_screen.settings
        // # Settings
        // # zh_CN 设置
        buttonList.add(
            new GuiButton(9, compact ? 114 : width - 168, controlsY, 60, 20, I18n.format("gui.web_screen.settings")));
        String addressText = address == null ? null : address.getText();
        boolean addressFocused = address != null && address.isFocused();
        address = new GuiTextField(fontRendererObj, 110, 9, Math.max(20, width - (compact ? 166 : 432)), 18);
        address.setMaxStringLength(2048);
        ChromiumSession session = ClientWebScreens.getSession(tile);
        address.setText(
            addressText != null ? addressText : session == null ? WebScreenAddress.DEFAULT_HOME : session.getUrl());
        address.setFocused(addressFocused);
        int browserWidth = session == null ? ChromiumSession.DEFAULT_WIDTH : session.getWidth();
        int browserHeight = session == null ? ChromiumSession.DEFAULT_HEIGHT : session.getHeight();
        int contentTop = compact ? 60 : 36;
        int contentHeight = Math.max(1, height - contentTop - 28);
        double scale = Math.min((width - 16) / (double) browserWidth, contentHeight / (double) browserHeight);
        viewWidth = Math.max(1, (int) (browserWidth * scale));
        viewHeight = Math.max(1, (int) (browserHeight * scale));
        viewX = (width - viewWidth) / 2;
        viewY = contentTop + (contentHeight - viewHeight) / 2;
        updateZoomControls();
    }

    @Override
    public void updateScreen() {
        address.updateCursorCounter();
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (!address.isFocused() && session != null && !session.getUrl().equals(address.getText())) {
            address.setText(session.getUrl());
        }
        updateZoomControls();
    }

    private void updateZoomControls() {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        int percent = session == null ? 100 : session.getZoomPercent();
        zoomOut.enabled = session != null && percent > 25;
        zoomReset.enabled = session != null;
        String label = percent + "%";
        if (!label.equals(zoomReset.displayString)) {
            lastMouseX = -1;
            lastMouseY = -1;
        }
        zoomReset.displayString = label;
        zoomIn.enabled = session != null && percent < 300;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawRect(0, 0, width, height, 0xF013171C);
        drawRect(viewX - 1, viewY - 1, viewX + viewWidth + 1, viewY + viewHeight + 1, 0xFF58616B);
        drawRect(viewX, viewY, viewX + viewWidth, viewY + viewHeight, 0xFF080A0C);
        ResourceLocation texture = ClientWebScreens.getTexture(tile);
        if (texture != null) {
            GL11.glColor4f(1, 1, 1, 1);
            mc.getTextureManager().bindTexture(texture);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(viewX, viewY + viewHeight, 0, 0, 1);
            tessellator.addVertexWithUV(viewX + viewWidth, viewY + viewHeight, 0, 1, 1);
            tessellator.addVertexWithUV(viewX + viewWidth, viewY, 0, 1, 0);
            tessellator.addVertexWithUV(viewX, viewY, 0, 0, 0);
            tessellator.draw();
        } else {
            List<String> lines = fontRendererObj.listFormattedStringToWidth(status(), Math.max(30, viewWidth - 24));
            int lineY = viewY + (viewHeight - lines.size() * fontRendererObj.FONT_HEIGHT) / 2;
            for (String line : lines) {
                drawCenteredString(fontRendererObj, line, width / 2, lineY, 0xE0E5EB);
                lineY += fontRendererObj.FONT_HEIGHT;
            }
        }
        address.drawTextBox();
        // #tr gui.web_screen.hint
        // # Esc: return to screen | Ctrl+wheel: zoom | Ctrl+0: reset
        // # zh_CN Esc 返回屏幕 | Ctrl+滚轮缩放 | Ctrl+0 还原
        String hint = error == null ? I18n.format("gui.web_screen.hint") : error;
        drawString(fontRendererObj, fontRendererObj.trimStringToWidth(hint, width - 16), 8, height - 16,
            error == null ? 0xBBC6D2 : 0xFF7777);
        super.drawScreen(mouseX, mouseY, partialTick);
        if (inside(mouseX, mouseY) || buttons != 0) move(mouseX, mouseY);
        if (
            mouseX >= zoomOut.xPosition && mouseX < zoomIn.xPosition + zoomIn.width
                && mouseY >= zoomOut.yPosition
                && mouseY < zoomOut.yPosition + zoomOut.height
        ) {
            // #tr gui.web_screen.zoom_hint
            // # Page zoom: Ctrl+wheel or Ctrl +/-; click the percentage to reset
            // # zh_CN 网页缩放：Ctrl+滚轮或 Ctrl +/-；点击百分比还原
            String zoomHint = I18n.format("gui.web_screen.zoom_hint");
            drawHoveringText(fontRendererObj.listFormattedStringToWidth(zoomHint, Math.min(260, width - 24)), mouseX,
                mouseY, fontRendererObj);
        }
    }

    private String status() {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (session == null) {
            // #tr gui.web_screen.stopped
            // # Browser stopped or disabled in client settings
            // # zh_CN 浏览器已停止或已在客户端设置中禁用
            return I18n.format("gui.web_screen.stopped");
        }
        switch (session.getStatus()) {
            case "missingBrowser":
                // #tr gui.web_screen.missing_browser
                // # Install Edge or Chrome on Windows, or set the browser path in client settings
                // # zh_CN 请在 Windows 安装 Edge 或 Chrome，或在客户端设置中填写浏览器路径
                return I18n.format("gui.web_screen.missing_browser");
            case "failed":
                // #tr gui.web_screen.failed
                // # Browser failed to start; check the game log
                // # zh_CN 浏览器启动失败，请查看游戏日志
                return I18n.format("gui.web_screen.failed");
            case "ready":
                // #tr gui.web_screen.loading
                // # Loading page...
                // # zh_CN 正在加载网页……
                return I18n.format("gui.web_screen.loading");
            default:
                // #tr gui.web_screen.starting
                // # Starting browser...
                // # zh_CN 正在启动浏览器……
                return I18n.format("gui.web_screen.starting");
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (button.id == 5) mc.displayGuiScreen(null);
        else if (button.id == 4) {
            ClientWebScreens.stop(tile);
            mc.displayGuiScreen(null);
        } else if (button.id == 9) mc.displayGuiScreen(new GuiWebScreenSettings(this, tile));
        else if (session != null) {
            if (button.id == 0) session.back();
            else if (button.id == 1) session.reload();
            else if (button.id == 2) session.navigate(WebScreenAddress.DEFAULT_HOME);
            else if (button.id == 3) navigate();
            else if (button.id == 6) session.adjustZoom(-25);
            else if (button.id == 7) session.resetZoom();
            else if (button.id == 8) session.adjustZoom(25);
            updateZoomControls();
        }
    }

    private void navigate() {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (session == null) return;
        try {
            String url = WebScreenAddress.normalize(address.getText());
            address.setText(url);
            address.setFocused(false);
            error = null;
            session.navigate(url);
        } catch (IllegalArgumentException ignored) {
            // #tr gui.web_screen.invalid_address
            // # Enter an HTTP or HTTPS address without a username or password
            // # zh_CN 请输入不含用户名和密码的 HTTP 或 HTTPS 网址
            error = I18n.format("gui.web_screen.invalid_address");
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        boolean wasFocused = address.isFocused();
        address.mouseClicked(mouseX, mouseY, button);
        if (!wasFocused && address.isFocused()) releaseKeys();
        if (inside(mouseX, mouseY) && button >= 0 && button <= 2) {
            address.setFocused(false);
            buttons |= mouseMask(button);
            sendMouse("mousePressed", mouseX, mouseY, buttonName(button), 0);
        } else super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (button >= 0 && button <= 2 && (buttons & mouseMask(button)) != 0) {
            buttons &= ~mouseMask(button);
            sendMouse("mouseReleased", mouseX, mouseY, buttonName(button), 0);
        }
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int x = Mouse.getEventX() * width / mc.displayWidth;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        handleWheel(wheel, isCtrlKeyDown(), x, y);
    }

    private void handleWheel(int wheel, boolean control, int x, int y) {
        if (wheel != 0 && inside(x, y)) {
            if (control) {
                ChromiumSession session = ClientWebScreens.getSession(tile);
                if (session == null || address.isFocused()) return;
                session.adjustZoom(wheel > 0 ? 25 : -25);
                updateZoomControls();
            } else sendMouse("mouseWheel", x, y, "none", -Math.signum(wheel) * 120);
        }
    }

    private void move(int x, int y) {
        if (x == lastMouseX && y == lastMouseY) return;
        lastMouseX = x;
        lastMouseY = y;
        sendMouse("mouseMoved", x, y, "none", 0);
    }

    private void sendMouse(String type, int x, int y, String button, double delta) {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (session == null) return;
        int viewportWidth = session.getViewportWidth();
        int viewportHeight = session.getViewportHeight();
        session.mouse(type, Math.max(0, Math.min(viewportWidth - 1, (x - viewX) * viewportWidth / viewWidth)),
            Math.max(0, Math.min(viewportHeight - 1, (y - viewY) * viewportHeight / viewHeight)), button, buttons,
            delta, modifiers());
    }

    private boolean inside(int x, int y) {
        return x >= viewX && x < viewX + viewWidth && y >= viewY && y < viewY + viewHeight;
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (key == Keyboard.KEY_F6 || (isCtrlKeyDown() && key == Keyboard.KEY_L)) {
            releaseKeys();
            address.setFocused(true);
            return;
        }
        if (address.isFocused()) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) navigate();
            else address.textboxKeyTyped(character, key);
            return;
        }
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (session == null) return;
        if (handleZoomKey(key, isCtrlKeyDown())) return;
        if (isCtrlKeyDown() && key == Keyboard.KEY_V) {
            session.insertText(getClipboardString());
            return;
        }
        int virtual = virtualKey(key, character);
        String name = keyName(key, character);
        pressedKeys.put(virtual, name);
        session.key("rawKeyDown", name, virtual, modifiers());
        if (character >= 32 && character != 127 && !isCtrlKeyDown() && !altDown()) {
            session.insertText(String.valueOf(character));
        }
    }

    private boolean handleZoomKey(int key, boolean control) {
        if (!control || address.isFocused()) return false;
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (session == null) return false;
        if (key == Keyboard.KEY_ADD || key == Keyboard.KEY_EQUALS) session.adjustZoom(25);
        else if (key == Keyboard.KEY_SUBTRACT || key == Keyboard.KEY_MINUS) session.adjustZoom(-25);
        else if (key == Keyboard.KEY_0 || key == Keyboard.KEY_NUMPAD0) session.resetZoom();
        else return false;
        updateZoomControls();
        return true;
    }

    @Override
    public void handleKeyboardInput() {
        super.handleKeyboardInput();
        if (!Keyboard.getEventKeyState() && !address.isFocused()) {
            ChromiumSession session = ClientWebScreens.getSession(tile);
            int virtual = virtualKey(Keyboard.getEventKey(), Keyboard.getEventCharacter());
            String name = pressedKeys.remove(virtual);
            if (session != null && name != null) session.key("keyUp", name, virtual, modifiers());
        }
    }

    private void releaseKeys() {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        if (session != null) pressedKeys.forEach((key, name) -> session.key("keyUp", name, key, 0));
        pressedKeys.clear();
    }

    private static String keyName(int key, char character) {
        return switch (key) {
            case Keyboard.KEY_BACK -> "Backspace";
            case Keyboard.KEY_TAB -> "Tab";
            case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> "Enter";
            case Keyboard.KEY_DELETE -> "Delete";
            case Keyboard.KEY_LEFT -> "ArrowLeft";
            case Keyboard.KEY_RIGHT -> "ArrowRight";
            case Keyboard.KEY_UP -> "ArrowUp";
            case Keyboard.KEY_DOWN -> "ArrowDown";
            case Keyboard.KEY_HOME -> "Home";
            case Keyboard.KEY_END -> "End";
            case Keyboard.KEY_PRIOR -> "PageUp";
            case Keyboard.KEY_NEXT -> "PageDown";
            case Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT -> "Shift";
            case Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL -> "Control";
            case Keyboard.KEY_LMENU, Keyboard.KEY_RMENU -> "Alt";
            default -> character >= 32 && character != 127 ? String.valueOf(character) : Keyboard.getKeyName(key);
        };
    }

    private static int virtualKey(int key, char character) {
        return switch (key) {
            case Keyboard.KEY_BACK -> 8;
            case Keyboard.KEY_TAB -> 9;
            case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> 13;
            case Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT -> 16;
            case Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL -> 17;
            case Keyboard.KEY_LMENU, Keyboard.KEY_RMENU -> 18;
            case Keyboard.KEY_PRIOR -> 33;
            case Keyboard.KEY_NEXT -> 34;
            case Keyboard.KEY_END -> 35;
            case Keyboard.KEY_HOME -> 36;
            case Keyboard.KEY_LEFT -> 37;
            case Keyboard.KEY_UP -> 38;
            case Keyboard.KEY_RIGHT -> 39;
            case Keyboard.KEY_DOWN -> 40;
            case Keyboard.KEY_INSERT -> 45;
            case Keyboard.KEY_DELETE -> 46;
            default -> {
                String name = Keyboard.getKeyName(key);
                yield name != null && name.length() == 1 ? name.charAt(0) : Character.toUpperCase(character);
            }
        };
    }

    private static int modifiers() {
        return (altDown() ? 1 : 0) | (isCtrlKeyDown() ? 2 : 0) | (isShiftKeyDown() ? 8 : 0);
    }

    private static boolean altDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    private static int mouseMask(int button) {
        return button == 0 ? 1 : button == 1 ? 2 : 4;
    }

    private static String buttonName(int button) {
        return button == 0 ? "left" : button == 1 ? "right" : "middle";
    }

    @Override
    public void onGuiClosed() {
        releaseKeys();
        for (int button = 0; button < 3; button++) {
            if ((buttons & mouseMask(button)) == 0) continue;
            buttons &= ~mouseMask(button);
            sendMouse("mouseReleased", lastMouseX, lastMouseY, buttonName(button), 0);
        }
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
