package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.text.preview.TextEffectPreview;
import com.xyp.gtnotgood.client.wireless.WirelessMonitorHud;
import com.xyp.gtnotgood.client.wireless.WirelessMonitorPositionScreen;
import com.xyp.gtnotgood.client.wireless.WirelessMonitorPreferences;
import com.xyp.gtnotgood.common.wireless.monitor.WirelessEnergyHistory;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;
import com.xyp.gtnotgood.utils.text.AnimatedText;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(
    modid = "wirelessmonitorqa",
    name = "Wireless monitor QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WirelessMonitorClientChecks {

    private boolean started;
    private boolean finished;
    private volatile boolean discharging;
    private volatile boolean showUpstreamMonitor;
    private boolean upstreamMonitorAdded;
    private boolean initializedServer;
    private int stage;
    private int ticks;
    private int totalTicks;
    private long requestWhenDisabled;
    private String capture;
    private TextEffectStyle machineStyle;
    private int savedX;
    private int savedY;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.wirelessMonitor.qa")) return;
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (showUpstreamMonitor && !upstreamMonitorAdded) {
            Item item = GameRegistry.findItem("gtswn", "PortableWirelessNetworkMonitor_GTswn");
            require(item != null, "Upstream monitor still registered");
            ItemStack monitor = new ItemStack(item);
            monitor.setTagCompound(new NBTTagCompound());
            monitor.getTagCompound()
                .setBoolean("Initialized", true);
            monitor.getTagCompound()
                .setString(
                    "OwnerUUID",
                    player.getUniqueID()
                        .toString());
            monitor.getTagCompound()
                .setInteger("HUDMode", 1);
            player.inventory.setInventorySlotContents(1, monitor);
            upstreamMonitorAdded = true;
        }
        if (!initializedServer) {
            initializedServer = true;
            WirelessNetworkManager.strongCheckOrAddUser(player.getUniqueID());
            WirelessNetworkManager.setUserEU(player.getUniqueID(), BigInteger.valueOf(29_500_080));
            for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++)
                player.inventory.setInventorySlotContents(slot, null);
            player.setPositionAndUpdate(0, 6, 0);
        } else {
            WirelessNetworkManager.addEUToGlobalEnergyMap(player.getUniqueID(), discharging ? -4838 : 3200);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer(
                    "wireless-monitor-" + System.currentTimeMillis(),
                    "Wireless monitor QA",
                    new WorldSettings(193L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.theWorld == null || mc.thePlayer == null) return;
            if (++totalTicks > 1500) throw new AssertionError("HUD QA timed out at stage " + stage);
            if (capture != null) return;
            ticks++;
            if (stage == 0) {
                require(
                    Loader.isModLoaded("gtswn") == Boolean.getBoolean("gtng.wirelessMonitor.upstream"),
                    "Upstream presence");
                machineStyle = AnimatedText.creditStyle();
                WirelessMonitorPreferences.setEnabled(false);
                toggle();
                require(WirelessMonitorPreferences.enabled, "Toggle key enables HUD without an item");
                mc.displayGuiScreen(null);
                stage = 1;
                ticks = 0;
            } else if (stage == 1 && ticks > 150) {
                WirelessEnergyHistory history = history();
                require(
                    history.energy() != null && history.energy()
                        .signum() > 0,
                    "Network reply arrived");
                require(
                    history.realtime() != null && history.realtime()
                        .signum() > 0,
                    "Positive real-time power");
                require(
                    history.average()
                        .signum() > 0,
                    "Positive average power");
                require(mc.thePlayer.inventory.getCurrentItem() == null, "No monitor item required");
                capture = "wireless-hud-charging.png";
                stage = 2;
                ticks = 0;
            } else if (stage == 2 && ticks > 30) {
                capture = "wireless-hud-animation.png";
                stage = 3;
                ticks = 0;
            } else if (stage == 3) {
                toggle();
                require(
                    !WirelessMonitorPreferences.enabled && history().energy() == null,
                    "Key closes HUD and clears history");
                requestWhenDisabled = numberField("requestId");
                stage = 4;
                ticks = 0;
            } else if (stage == 4 && ticks > 120) {
                require(numberField("requestId") == requestWhenDisabled, "Disabled HUD sends no requests");
                capture = "wireless-hud-disabled.png";
                stage = 5;
                ticks = 0;
            } else if (stage == 5) {
                discharging = true;
                toggle();
                stage = 6;
                ticks = 0;
            } else if (stage == 6 && ticks > 150) {
                require(
                    history().realtime() != null && history().realtime()
                        .signum() < 0,
                    "Negative power after reopening");
                WirelessMonitorPreferences.animateValues = false;
                capture = "wireless-hud-discharging.png";
                stage = 7;
                ticks = 0;
            } else if (stage == 7 && ticks > 20) {
                WirelessMonitorPreferences.apply(TextEffects.PULSE_UPWARDS.rendererId(), true, true, false);
                require(
                    AnimatedText.creditStyle()
                        .equals(machineStyle),
                    "HUD style does not change machine credits");
                WirelessMonitorPreferences.animateValues = true;
                capture = "wireless-hud-custom-palette.png";
                stage = 8;
                ticks = 0;
            } else if (stage == 8 && ticks > 20) {
                WirelessMonitorPreferences.load();
                require(
                    WirelessMonitorPreferences.enabled && WirelessMonitorPreferences.customPalette
                        && WirelessMonitorPreferences.bold,
                    "HUD settings survive configuration reload");
                require(
                    WirelessMonitorPreferences.style()
                        .rendererId()
                        .equals(TextEffects.PULSE_UPWARDS.rendererId()),
                    "Effect saved");
                mc.displayGuiScreen(new TextEffectPreview(true));
                stage = 9;
                ticks = 0;
            } else if (stage == 9 && ticks > 30) {
                require(mc.currentScreen instanceof TextEffectPreview, "Wireless effect preview opens");
                ScreenShotHelper.saveScreenshot(
                    output(),
                    "wireless-hud-preview.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                if (!Boolean.getBoolean("gtng.wirelessMonitor.upstream")) {
                    mc.displayGuiScreen(null);
                    stage = 13;
                    ticks = 0;
                } else {
                    mc.displayGuiScreen(null);
                    showUpstreamMonitor = true;
                    stage = 10;
                    ticks = 0;
                }
            } else if (stage == 10 && ticks > 150) {
                require(mc.thePlayer.inventory.getStackInSlot(1) != null, "Upstream monitor synchronized");
                capture = "wireless-hud-coexist-both.png";
                stage = 11;
                ticks = 0;
            } else if (stage == 11) {
                toggle();
                require(!WirelessMonitorPreferences.enabled, "GTNG HUD disabled independently");
                require(
                    mc.thePlayer.inventory.getStackInSlot(1)
                        .getTagCompound()
                        .getInteger("HUDMode") == 1,
                    "GTNG toggle does not change upstream item settings");
                stage = 12;
                ticks = 0;
            } else if (stage == 12 && ticks > 110) {
                capture = "wireless-hud-coexist-upstream-only.png";
                stage = 13;
                ticks = 0;
            } else if (stage == 13) {
                WirelessMonitorPreferences.xOffset = 0;
                WirelessMonitorPreferences.yOffset = 0;
                KeyBinding.onTick(KeyBindManager.moveWirelessMonitor.getKeyCode());
                FMLCommonHandler.instance()
                    .bus()
                    .post(new InputEvent.KeyInputEvent());
                require(mc.currentScreen instanceof WirelessMonitorPositionScreen, "Edit key opens position screen");
                WirelessMonitorPositionScreen editor = (WirelessMonitorPositionScreen) mc.currentScreen;
                int startX = 12;
                int startY = editor.height - 70;
                screenInput(
                    editor,
                    "mouseClicked",
                    new Class<?>[] { int.class, int.class, int.class },
                    startX,
                    startY,
                    0);
                screenInput(
                    editor,
                    "mouseClickMove",
                    new Class<?>[] { int.class, int.class, int.class, long.class },
                    startX + 80,
                    startY - 100,
                    0,
                    1L);
                screenInput(
                    editor,
                    "mouseMovedOrUp",
                    new Class<?>[] { int.class, int.class, int.class },
                    startX + 80,
                    startY - 100,
                    0);
                require(
                    WirelessMonitorPreferences.xOffset == 80 && WirelessMonitorPreferences.yOffset == 100,
                    "Dragging preserves the mouse grab offset");
                savedX = WirelessMonitorPreferences.xOffset;
                savedY = WirelessMonitorPreferences.yOffset;
                stage = 14;
                ticks = 0;
            } else if (stage == 14 && ticks > 15) {
                ScreenShotHelper.saveScreenshot(
                    output(),
                    "wireless-hud-drag-editor.png",
                    mc.displayWidth,
                    mc.displayHeight,
                    mc.getFramebuffer());
                WirelessMonitorPositionScreen editor = (WirelessMonitorPositionScreen) mc.currentScreen;
                screenInput(
                    editor,
                    "keyTyped",
                    new Class<?>[] { char.class, int.class },
                    '\0',
                    KeyBindManager.moveWirelessMonitor.getKeyCode());
                require(mc.currentScreen == null, "Pressing edit key again closes editor");
                WirelessMonitorPreferences.xOffset = 0;
                WirelessMonitorPreferences.yOffset = 0;
                WirelessMonitorPreferences.load();
                require(
                    WirelessMonitorPreferences.xOffset == savedX && WirelessMonitorPreferences.yOffset == savedY,
                    "Dragged position survives configuration reload");
                if (!WirelessMonitorPreferences.enabled) toggle();
                capture = "wireless-hud-drag-saved.png";
                stage = 15;
                ticks = 0;
            } else if (stage == 15 && ticks > 15) {
                KeyBinding.onTick(KeyBindManager.moveWirelessMonitor.getKeyCode());
                FMLCommonHandler.instance()
                    .bus()
                    .post(new InputEvent.KeyInputEvent());
                WirelessMonitorPositionScreen editor = (WirelessMonitorPositionScreen) mc.currentScreen;
                int x = 12 + savedX;
                int y = editor.height - 70 - savedY;
                screenInput(editor, "mouseClicked", new Class<?>[] { int.class, int.class, int.class }, x, y, 0);
                screenInput(
                    editor,
                    "mouseClickMove",
                    new Class<?>[] { int.class, int.class, int.class, long.class },
                    -1000,
                    -1000,
                    0,
                    1L);
                require(WirelessMonitorPreferences.xOffset == 0, "Dragging beyond left edge clamps the position");
                require(
                    editor.height - 78 - WirelessMonitorPreferences.yOffset == 4,
                    "Dragging beyond top edge remains visible");
                screenInput(editor, "mouseMovedOrUp", new Class<?>[] { int.class, int.class, int.class }, 0, 0, 0);
                screenInput(editor, "actionPerformed", new Class<?>[] { GuiButton.class }, new GuiButton(0, 0, 0, ""));
                require(
                    WirelessMonitorPreferences.xOffset == 0 && WirelessMonitorPreferences.yOffset == 0,
                    "Reset restores the default location");
                screenInput(editor, "keyTyped", new Class<?>[] { char.class, int.class }, '\0', Keyboard.KEY_ESCAPE);
                require(mc.currentScreen == null, "Esc closes editor");
                WirelessMonitorPreferences.load();
                require(
                    WirelessMonitorPreferences.xOffset == 0 && WirelessMonitorPreferences.yOffset == 0,
                    "Reset position is saved on exit");
                finish("PASS");
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(failure.toString());
        }
    }

    private static void screenInput(WirelessMonitorPositionScreen screen, String name, Class<?>[] types,
        Object... values) throws Exception {
        Method method = WirelessMonitorPositionScreen.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(screen, values);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || capture == null || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), capture, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        capture = null;
    }

    private static void toggle() {
        KeyBinding.onTick(KeyBindManager.toggleWirelessMonitor.getKeyCode());
        FMLCommonHandler.instance()
            .bus()
            .post(new InputEvent.KeyInputEvent());
    }

    private static WirelessEnergyHistory history() throws Exception {
        Field field = WirelessMonitorHud.class.getDeclaredField("history");
        field.setAccessible(true);
        return (WirelessEnergyHistory) field.get(WirelessMonitorHud.INSTANCE);
    }

    private static long numberField(String name) throws Exception {
        Field field = WirelessMonitorHud.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(WirelessMonitorHud.INSTANCE);
    }

    private static File output() {
        return new File(System.getProperty("gtng.wirelessMonitor.output"));
    }

    private void finish(String result) {
        finished = true;
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("WIRELESS_MONITOR_QA: " + result);
        FMLCommonHandler.instance()
            .exitJava(result.equals("PASS") ? 0 : 1, true);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
