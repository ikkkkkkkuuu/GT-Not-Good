package com.xyp.gtnotgood.client.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.packet.ServerConfigMessage;
import com.xyp.gtnotgood.config.ServerConfigOptions;
import com.xyp.gtnotgood.config.ServerConfigOptions.Option;
import com.xyp.gtnotgood.config.ServerConfigService;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;

/** Edits detached server values and only reports success after the server acknowledges persistence. */
public final class ServerSettingsScreen extends GuiConfig {

    private final Map<String, String> baseline;
    private final Map<String, Property> edits;

    private ServerSettingsScreen(GuiScreen parent, ServerConfigMessage reply, Map<String, Property> edits) {
        super(parent, elements(edits), ModList.GTNotGood.getID(), false, false, ModList.GTNotGood.getDisplayName());
        baseline = new LinkedHashMap<>(reply.values);
        this.edits = edits;
        titleLine2 = statusText(reply.status);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id != 2000) {
            super.actionPerformed(button);
            return;
        }
        entryList.saveConfigElements();
        Map<String, String> changes = new LinkedHashMap<>();
        edits.forEach(
            (id, property) -> {
                if (!property.getString()
                    .equals(baseline.get(id))) changes.put(id, property.getString());
            });
        if (changes.isEmpty()) {
            mc.displayGuiScreen(parentScreen);
        } else {
            ServerConfigMessage request = new ServerConfigMessage(0, ServerConfigService.APPLY, changes);
            request.expected.putAll(baseline);
            mc.displayGuiScreen(new WaitingScreen(parentScreen, request));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static List<IConfigElement> elements(Map<String, Property> edits) {
        Map<String, ConfigCategory> categories = new LinkedHashMap<>();
        Map<String, Option> options = ServerConfigOptions.options();
        edits.forEach((id, property) -> {
            Option option = options.get(id);
            categories.computeIfAbsent(option.category, name -> {
                ConfigCategory category = new ConfigCategory(name);
                category.setComment(
                    option.configuration()
                        .getCategory(name.toLowerCase(Locale.ENGLISH))
                        .getComment());
                return category;
            })
                .put(option.key, property);
        });
        List<IConfigElement> elements = new ArrayList<>();
        categories.values()
            .forEach(category -> elements.add(new ConfigElement(category)));
        return elements;
    }

    /**
     * Handles only the visible waiting screen's reply; stale replies after leaving or reconnecting are ignored.
     *
     * @param reply server acknowledgement delivered on the client thread
     */
    public static void receive(ServerConfigMessage reply) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof WaitingScreen)) return;
        WaitingScreen waiting = (WaitingScreen) mc.currentScreen;
        if (waiting.request.requestId != reply.requestId || mc.getNetHandler() != waiting.connection) return;
        waiting.awaiting = false;
        if (reply.status == ServerConfigService.DENIED) {
            waiting.message = statusText(reply.status);
            return;
        }
        Map<String, Property> edits = new LinkedHashMap<>();
        Map<String, Option> options = ServerConfigOptions.options();
        if (!options.keySet()
            .equals(reply.values.keySet())) {
            waiting.message = statusText(ServerConfigService.INVALID);
            return;
        }
        reply.values.forEach((id, value) -> {
            Option option = options.get(id);
            Property source = option.property();
            Property copy = new Property(option.key, value, source.getType());
            copy.setDefaultValue(source.getDefault());
            if (source.getType() == Property.Type.INTEGER) {
                copy.setMinValue(Integer.parseInt(source.getMinValue()));
                copy.setMaxValue(Integer.parseInt(source.getMaxValue()));
            } else if (source.getType() == Property.Type.DOUBLE) {
                copy.setMinValue(Double.parseDouble(source.getMinValue()));
                copy.setMaxValue(Double.parseDouble(source.getMaxValue()));
            }
            // #tr gui.gtnotgood.server_live
            // # Takes effect on subsequent server processing; existing results are not undone.
            // # zh_CN 后续服务器处理生效；已有结果不会撤销。
            String timing = I18n.format("gui.gtnotgood.server_live");
            if (!option.live) {
                // #tr gui.gtnotgood.server_restart
                // # Saved for the next server restart; clients may also need matching settings and a restart.
                // # zh_CN 保存后重启服务器生效；客户端可能也需同步设置并重启。
                timing = I18n.format("gui.gtnotgood.server_restart");
            }
            copy.comment = source.comment + "\n\n" + timing;
            edits.put(id, copy);
        });
        mc.displayGuiScreen(new ServerSettingsScreen(waiting.parent, reply, edits));
    }

    private static String statusText(int status) {
        switch (status) {
            case ServerConfigService.SAVED:
                // #tr gui.gtnotgood.server_saved
                // # Server settings saved and applied.
                // # zh_CN 服务器设置已保存并生效。
                return I18n.format("gui.gtnotgood.server_saved");
            case ServerConfigService.RESTART:
                // #tr gui.gtnotgood.server_saved_restart
                // # Saved. Live options applied; startup options require a server restart.
                // # zh_CN 已保存；热更新项已生效，启动项需重启服务器。
                return I18n.format("gui.gtnotgood.server_saved_restart");
            case ServerConfigService.DENIED:
                // #tr gui.gtnotgood.server_denied
                // # Requires server operator permission level 2 or higher.
                // # zh_CN 需要服务器 OP 权限（等级 2 或以上）。
                return I18n.format("gui.gtnotgood.server_denied");
            case ServerConfigService.CONFLICT:
                // #tr gui.gtnotgood.server_conflict
                // # Settings changed elsewhere. Latest values loaded; please edit again.
                // # zh_CN 设置已被其他人修改，已刷新最新值，请重新编辑。
                return I18n.format("gui.gtnotgood.server_conflict");
            case ServerConfigService.INVALID:
                // #tr gui.gtnotgood.server_invalid
                // # Invalid values or mismatched mod versions. Nothing was applied.
                // # zh_CN 数值无效或模组版本不一致，未应用修改。
                return I18n.format("gui.gtnotgood.server_invalid");
            case ServerConfigService.FAILED:
                // #tr gui.gtnotgood.server_failed
                // # Save failed. Check the server log; runtime settings were not changed.
                // # zh_CN 保存失败，请检查服务器日志；运行中的设置未改变。
                return I18n.format("gui.gtnotgood.server_failed");
            default:
                // #tr gui.gtnotgood.server_editing
                // # Server settings - hover over an option to see when it takes effect.
                // # zh_CN 服务器设置：悬停查看生效时机，点击完成提交。
                return I18n.format("gui.gtnotgood.server_editing");
        }
    }

    /** Root menu category that fetches a fresh server snapshot whenever it is opened. */
    public static final class ServerEntry extends GuiConfigEntries.CategoryEntry {

        @SuppressWarnings("rawtypes")
        public ServerEntry(GuiConfig screen, GuiConfigEntries entries, IConfigElement element) {
            super(screen, entries, element);
            // #tr gui.gtnotgood.server_settings_description
            // # Edit this server's settings. Requires OP level 2. Hover over each option for its effect and timing.
            // # zh_CN 修改当前服务器的设置，需要 OP 2 级权限。悬停各选项可查看功能和生效时机。
            String description = I18n.format("gui.gtnotgood.server_settings_description");
            toolTip = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(description, 300);
        }

        @Override
        public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
            childScreen = buildChildScreen();
            return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
        }

        @Override
        protected GuiScreen buildChildScreen() {
            return new WaitingScreen(
                owningScreen,
                new ServerConfigMessage(0, ServerConfigService.READ, Collections.emptyMap()));
        }
    }

    /** Holds a single request with a timeout and allows leaving without accepting late replies. */
    private static final class WaitingScreen extends GuiScreen {

        private final GuiScreen parent;
        private final ServerConfigMessage request;
        private Object connection;
        private String message;
        private long started;
        private boolean awaiting = true;

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        private WaitingScreen(GuiScreen parent, ServerConfigMessage request) {
            this.parent = parent;
            this.request = request;
        }

        @Override
        public void initGui() {
            buttonList.clear();
            buttonList.add(new GuiButton(0, width / 2 - 100, height - 35, I18n.format("gui.back")));
            if (started != 0) return;
            connection = mc.getNetHandler();
            request.requestId = ThreadLocalRandom.current()
                .nextLong();
            started = System.currentTimeMillis();
            // #tr gui.gtnotgood.server_waiting
            // # Waiting for the server...
            // # zh_CN 等待服务器确认……
            message = I18n.format("gui.gtnotgood.server_waiting");
            if (connection != null) GTNotGood.channel.sendToServer(request);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            if (awaiting && System.currentTimeMillis() - started > 10000) {
                // #tr gui.gtnotgood.server_timeout
                // # No reply. Reopen settings to verify; the server must have the same mod version.
                // # zh_CN 未收到回复，请重新打开核实；服务器需安装相同版本模组。
                message = I18n.format("gui.gtnotgood.server_timeout");
            }
            drawCenteredString(fontRendererObj, message, width / 2, height / 2, 0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            mc.displayGuiScreen(parent);
        }

        @Override
        protected void keyTyped(char character, int keyCode) {
            if (keyCode == 1) mc.displayGuiScreen(parent);
        }
    }
}
