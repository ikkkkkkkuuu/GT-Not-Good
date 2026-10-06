package com.xyp.gtnotgood.client.wireless;

import java.math.BigDecimal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.text.preview.TextEffectPreview;
import com.xyp.gtnotgood.common.packet.WirelessMonitorRequest;
import com.xyp.gtnotgood.common.packet.WirelessMonitorSnapshot;
import com.xyp.gtnotgood.common.wireless.monitor.WirelessEnergyHistory;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Personal, item-free wireless HUD. Networking runs on client ticks; rendering only reads cached snapshots.
 * Connection changes and toggling discard history and invalidate outstanding replies.
 */
public final class WirelessMonitorHud extends Gui {

    public static final WirelessMonitorHud INSTANCE = new WirelessMonitorHud();
    private final WirelessEnergyHistory history = new WirelessEnergyHistory();
    private NetHandlerPlayClient connection;
    private long requestId;
    private long pendingRequest = -1;
    private int ticksUntilRequest;
    private int ticksSinceSnapshot;

    private WirelessMonitorHud() {}

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        while (KeyBindManager.toggleWirelessMonitor.isPressed()) {
            WirelessMonitorPreferences.setEnabled(!WirelessMonitorPreferences.enabled);
            reset();
        }
        if (KeyBindManager.configureWirelessMonitor.isPressed()) {
            mc.displayGuiScreen(new TextEffectPreview(true));
        }
        if (KeyBindManager.moveWirelessMonitor.isPressed()) {
            mc.displayGuiScreen(new WirelessMonitorPositionScreen());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient current = mc.getNetHandler();
        if (connection != current || mc.thePlayer == null || mc.theWorld == null) {
            reset();
            connection = current;
        }
        if (!WirelessMonitorPreferences.enabled || mc.thePlayer == null || mc.theWorld == null || mc.isGamePaused())
            return;
        if (++ticksSinceSnapshot > 300) history.clear();
        if (ticksUntilRequest-- > 0) return;
        ticksUntilRequest = WirelessEnergyHistory.SAMPLE_INTERVAL - 1;
        pendingRequest = ++requestId;
        GTNotGood.channel.sendToServer(new WirelessMonitorRequest(pendingRequest));
    }

    /** Accepts only the current connection's outstanding reply, on the Minecraft client thread. */
    public void receive(WirelessMonitorSnapshot message, NetHandlerPlayClient source) {
        if (!WirelessMonitorPreferences.enabled || source != connection || pendingRequest != message.requestId) return;
        pendingRequest = -1;
        ticksSinceSnapshot = 0;
        history.add(message.owner, message.tick, message.energy);
    }

    private void reset() {
        history.clear();
        pendingRequest = -1;
        ticksUntilRequest = 0;
        ticksSinceSnapshot = 0;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !WirelessMonitorPreferences.enabled
            || mc.thePlayer == null
            || mc.theWorld == null
            || mc.gameSettings.hideGUI
            || mc.gameSettings.showDebugInfo
            || mc.currentScreen instanceof WirelessMonitorPositionScreen) return;
        ScaledResolution resolution = event.resolution;
        draw(layout(resolution.getScaledWidth(), resolution.getScaledHeight()));
    }

    /** Shared geometry keeps the editor's drag target aligned with the actual scaled and clamped HUD. */
    Layout layout(int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        String[] lines = lines();
        int width = 0;
        for (String line : lines) width = Math.max(width, mc.fontRenderer.getStringWidth(line));
        float scale = Math.min(WirelessMonitorPreferences.scale, (screenWidth - 8F) / (width + 4F));
        scale = Math.min(scale, (screenHeight - 8F) / 36F);
        int x = Math
            .max(4, Math.min(4 + WirelessMonitorPreferences.xOffset, screenWidth - (int) ((width + 4) * scale) - 4));
        int y = Math.max(
            4,
            Math.min(screenHeight - 78 - WirelessMonitorPreferences.yOffset, screenHeight - (int) (36 * scale) - 4));
        return new Layout(lines, x, y, width, scale);
    }

    void draw(Layout layout) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(layout.x, layout.y, 0);
            GL11.glScalef(layout.scale, layout.scale, 1);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            for (int i = 0; i < layout.lines.length; i++) {
                drawRect(-2, i * 12 - 2, mc.fontRenderer.getStringWidth(layout.lines[i]) + 2, i * 12 + 10, 0x80000000);
                mc.fontRenderer.drawStringWithShadow(layout.lines[i], 0, i * 12, 0xFFFFFF);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    static final class Layout {

        final String[] lines;
        final int x;
        final int y;
        final float scale;
        final int width;
        final int height;

        private Layout(String[] lines, int x, int y, int textWidth, float scale) {
            this.lines = lines;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.width = (int) Math.ceil((textWidth + 4) * scale);
            this.height = (int) Math.ceil(36 * scale);
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x - 2 * scale && mouseX <= x + width && mouseY >= y - 2 * scale && mouseY <= y + height;
        }
    }

    private String[] lines() {
        // #tr hud.gtnotgood.wireless.realtime
        // # Real-time grid status:
        // # zh_CN 实时电网状态：
        String realtime = StatCollector.translateToLocal("hud.gtnotgood.wireless.realtime");
        // #tr hud.gtnotgood.wireless.average
        // # Average grid status:
        // # zh_CN 平均电网状态：
        String average = StatCollector.translateToLocal("hud.gtnotgood.wireless.average");
        // #tr hud.gtnotgood.wireless.energy
        // # Wireless network:
        // # zh_CN 无线电网：
        String energy = StatCollector.translateToLocal("hud.gtnotgood.wireless.energy");
        // #tr hud.gtnotgood.wireless.syncing
        // # Synchronizing...
        // # zh_CN 同步中…
        String syncing = EnumChatFormatting.GRAY + StatCollector.translateToLocal("hud.gtnotgood.wireless.syncing");
        // #tr hud.gtnotgood.wireless.measuring
        // # Measuring...
        // # zh_CN 采样中…
        String measuring = EnumChatFormatting.GRAY + StatCollector.translateToLocal("hud.gtnotgood.wireless.measuring");
        boolean scientific = WirelessMonitorPreferences.scientific;
        String balance = history.energy() == null ? syncing
            : EnumChatFormatting.WHITE
                + WirelessMonitorFormat.number(new BigDecimal(history.energy()), scientific, false)
                + EnumChatFormatting.AQUA
                + " EU";
        String[] result = new String[] {
            EnumChatFormatting.AQUA + realtime
                + " "
                + (history.realtime() == null ? measuring : WirelessMonitorFormat.rate(history.realtime(), scientific)),
            EnumChatFormatting.AQUA + average
                + " "
                + (history.average() == null ? measuring : WirelessMonitorFormat.rate(history.average(), scientific)),
            EnumChatFormatting.AQUA + energy + " " + balance };
        if (WirelessMonitorPreferences.animatedColors) {
            if (WirelessMonitorPreferences.animateValues) {
                for (int i = 0; i < result.length; i++) result[i] = WirelessMonitorPreferences
                    .decorate(EnumChatFormatting.getTextWithoutFormattingCodes(result[i]));
            } else {
                result[0] = WirelessMonitorPreferences.decorate(realtime) + " "
                    + (history.realtime() == null ? measuring
                        : WirelessMonitorFormat.rate(history.realtime(), scientific));
                result[1] = WirelessMonitorPreferences.decorate(average) + " "
                    + (history.average() == null ? measuring
                        : WirelessMonitorFormat.rate(history.average(), scientific));
                result[2] = WirelessMonitorPreferences.decorate(energy) + " " + balance;
            }
        }
        return result;
    }
}
