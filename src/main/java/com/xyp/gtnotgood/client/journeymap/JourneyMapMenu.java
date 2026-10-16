package com.xyp.gtnotgood.client.journeymap;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.client.journeymap.JourneyMapLanding.Result;
import com.xyp.gtnotgood.client.journeymap.JourneyMapLanding.Status;
import com.xyp.gtnotgood.config.Config;

public final class JourneyMapMenu extends Gui {

    private static final int TITLE_HEIGHT = 26;
    private static final int ITEM_HEIGHT = 18;
    private final GuiScreen screen;
    private boolean open;
    private JourneyMapTarget target;
    private WorldClient world;
    private EntityClientPlayerMP player;
    private int screenWidth;
    private int screenHeight;
    private int[] bounds;
    private String title;
    private String status;
    private String[] labels;
    private boolean teleportEnabled;

    public JourneyMapMenu(GuiScreen screen) {
        this.screen = screen;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button, int rawX, int rawY) {
        if (open) {
            int index = button == 0 && validContext() && screen.width == screenWidth && screen.height == screenHeight
                ? hit(mouseX, mouseY)
                : -1;
            close();
            if (index == 0 && teleportEnabled) teleport();
            else if (index == 1) newWaypoint();
            return true;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (
            !Config.enableJourneyMapContextMenu || button != 1
                || mc.currentScreen != screen
                || mc.thePlayer == null
                || mc.theWorld == null
                || mouseX < 0
                || mouseY < 0
                || mouseX >= screen.width
                || mouseY >= screen.height
        ) return false;
        JourneyMapBridge bridge = JourneyMapBridge.get();
        if (bridge == null) return false;
        try {
            if (!bridge.accepts(screen, mouseX, mouseY)) return false;
            JourneyMapTarget selected = bridge.target(screen, rawX, rawY);
            if (selected == null) return false;
            target = selected;
            world = mc.theWorld;
            player = mc.thePlayer;
            screenWidth = screen.width;
            screenHeight = screen.height;
            Result landing = landing();
            boolean permitted = bridge.isPermitted();
            teleportEnabled = target.dimension == player.dimension && permitted && landing.status != Status.Blocked;
            status = target.dimension != player.dimension ? JourneyMapText.dimension()
                : !permitted ? JourneyMapText.permission()
                    : landing.status == Status.Blocked ? JourneyMapText.blocked()
                        : landing.status == Status.Unknown ? JourneyMapText.unknown()
                            : JourneyMapText.height(landing.y);
            String coordinates = String.format(Locale.ROOT, "%d, %s, %d", target.x,
                landing.status == Status.Unknown ? "?" : landing.y, target.z);
            String name = target.name == null ? ""
                : EnumChatFormatting.getTextWithoutFormattingCodes(target.name).replace('\n', ' ').replace('\r', ' ');
            title = name.isEmpty() ? coordinates : String.format(Locale.ROOT, "%s (%s)", name, coordinates);
            labels = new String[] { JourneyMapText.teleport(), JourneyMapText.newWaypoint() };
            FontRenderer font = mc.fontRenderer;
            int width = Math.max(140, Math.max(font.getStringWidth(title), font.getStringWidth(status)) + 12);
            for (String label : labels) width = Math.max(width, font.getStringWidth(label) + 12);
            width = Math.min(width, screen.width - 4);
            int height = TITLE_HEIGHT + labels.length * ITEM_HEIGHT + 2;
            if (width < 30 || height > screen.height - 4) return false;
            bounds = new int[] { Math.max(2, Math.min(mouseX, screen.width - width - 2)),
                Math.max(2, Math.min(mouseY, screen.height - height - 2)), width, height };
            open = true;
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            fail(error);
            return false;
        }
    }

    public boolean keyTyped(int key) {
        if (!open) return false;
        close();
        return key == Keyboard.KEY_ESCAPE;
    }

    public void draw(int mouseX, int mouseY) {
        if (!open) return;
        if (!validContext() || screen.width != screenWidth || screen.height != screenHeight) {
            close();
            return;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int x = bounds[0], y = bounds[1], width = bounds[2], height = bounds[3];
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glTranslatef(0, 0, 500);
            drawRect(x, y, x + width, y + height, 0xF0101010);
            drawRect(x, y, x + width, y + 1, 0xFF707070);
            drawRect(x, y + height - 1, x + width, y + height, 0xFF707070);
            drawRect(x, y, x + 1, y + height, 0xFF707070);
            drawRect(x + width - 1, y, x + width, y + height, 0xFF707070);
            font.drawStringWithShadow(fit(font, title, width - 12), x + 6, y + 4, 0xFFFFFF);
            font.drawString(fit(font, status, width - 12), x + 6, y + 15, teleportEnabled ? 0xAAAAAA : 0xFFAA55);
            drawRect(x + 1, y + TITLE_HEIGHT - 1, x + width - 1, y + TITLE_HEIGHT, 0xFF505050);
            int hovered = hit(mouseX, mouseY);
            for (int i = 0; i < labels.length; i++) {
                boolean enabled = i != 0 || teleportEnabled;
                int top = y + TITLE_HEIGHT + i * ITEM_HEIGHT;
                if (enabled && i == hovered) drawRect(x + 1, top, x + width - 1, top + ITEM_HEIGHT, 0xFF404040);
                font.drawStringWithShadow(fit(font, labels[i], width - 12), x + 6, top + 5,
                    !enabled ? 0x777777 : i == hovered ? 0xFFFFA0 : 0xEEEEEE);
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    public void close() {
        open = false;
    }

    private int hit(int x, int y) {
        if (x <= bounds[0] || x >= bounds[0] + bounds[2] - 1) return -1;
        int offset = y - bounds[1] - TITLE_HEIGHT;
        return offset >= 0 && offset < labels.length * ITEM_HEIGHT ? offset / ITEM_HEIGHT : -1;
    }

    private boolean validContext() {
        Minecraft mc = Minecraft.getMinecraft();
        return Config.enableJourneyMapContextMenu && mc.theWorld == world
            && mc.thePlayer == player
            && player != null
            && world != null;
    }

    private Result landing() {
        return JourneyMapLanding.resolve(target.dimension == player.dimension ? world : null, target.x, target.z,
            target.y, target.cave || target.waypoint != null);
    }

    private void newWaypoint() {
        JourneyMapBridge bridge = JourneyMapBridge.get();
        if (bridge == null || !validContext()) return;
        try {
            Result destination = landing();
            bridge.newWaypoint(target, destination.y, screen);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            fail(error);
        }
    }

    private void teleport() {
        JourneyMapBridge bridge = JourneyMapBridge.get();
        if (bridge == null || !validContext() || target.dimension != player.dimension) return;
        try {
            if (!bridge.isPermitted()) {
                player.addChatMessage(new ChatComponentText(JourneyMapText.permission()));
                return;
            }
            Result destination = landing();
            if (destination.status == Status.Blocked) {
                player.addChatMessage(new ChatComponentText(JourneyMapText.blocked()));
                return;
            }
            if (destination.status == Status.Unknown) {
                JourneyMapTarget selected = target;
                WorldClient sourceWorld = world;
                EntityClientPlayerMP sourcePlayer = player;
                Minecraft.getMinecraft().displayGuiScreen(new JourneyMapConfirmScreen((confirmed, id) -> {
                    if (!validContext() || world != sourceWorld || player != sourcePlayer) return;
                    if (confirmed) sendTeleport(selected, destination.y, true);
                    else Minecraft.getMinecraft().displayGuiScreen(screen);
                }, JourneyMapText.confirm(), JourneyMapText.destination(selected.x, destination.y, selected.z)));
            } else {
                sendTeleport(target, destination.y, false);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            fail(error);
        }
    }

    private void sendTeleport(JourneyMapTarget selected, int y, boolean unknownConfirmed) {
        JourneyMapBridge bridge = JourneyMapBridge.get();
        if (bridge == null || !validContext() || selected.dimension != player.dimension) return;
        try {
            if (!bridge.isPermitted()) return;
            Result checked = JourneyMapLanding.resolve(world, selected.x, selected.z, y, true);
            if (checked.status == Status.Blocked) {
                player.addChatMessage(new ChatComponentText(JourneyMapText.blocked()));
                Minecraft.getMinecraft().displayGuiScreen(screen);
                return;
            }
            if (checked.status == Status.Unknown && !unknownConfirmed) return;
            int height = checked.status == Status.Safe ? checked.y : y;
            // Integer coordinates also add half a block to Y in Minecraft 1.7.10's /tp parser.
            player.sendChatMessage(String.format(Locale.ROOT, "/tp %s %.1f %.1f %.1f", player.getCommandSenderName(),
                selected.x + 0.5, (double) height, selected.z + 0.5));
            bridge.closeMap();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            fail(error);
        }
    }

    private void fail(Throwable error) {
        close();
        JourneyMapBridge.disable(error);
    }

    private static String fit(FontRenderer font, String text, int width) {
        return font.getStringWidth(text) <= width ? text
            : font.trimStringToWidth(text, Math.max(0, width - 12)) + "...";
    }
}
